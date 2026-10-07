"""Real image restoration; no sharpening-only fallback."""
from pathlib import Path
from urllib.parse import urlparse
import sys
import cv2
import numpy as np
import torch
import torchvision.transforms.functional as functional

# BasicSR 1.4.2 references the old torchvision module, removed in newer releases.
# The supported public functions it uses are still in transforms.functional.
sys.modules.setdefault("torchvision.transforms.functional_tensor", functional)

from basicsr.archs.rrdbnet_arch import RRDBNet
from gfpgan import GFPGANer
from realesrgan import RealESRGANer
import facexlib.detection
import facexlib.parsing


class StrictRealESRGANer(RealESRGANer):
    """Propagate failed tiles instead of reusing an earlier tile's pixels."""
    def tile_process(self):
        batch,channels,height,width=self.img.shape
        self.output=self.img.new_zeros((batch,channels,height*self.scale,width*self.scale))
        for top in range(0,height,self.tile_size):
            for left in range(0,width,self.tile_size):
                bottom=min(top+self.tile_size,height);right=min(left+self.tile_size,width)
                padded_top=max(0,top-self.tile_pad);padded_left=max(0,left-self.tile_pad)
                padded_bottom=min(height,bottom+self.tile_pad);padded_right=min(width,right+self.tile_pad)
                tile=self.img[:,:,padded_top:padded_bottom,padded_left:padded_right]
                restored=self.model(tile)
                offset_top=(top-padded_top)*self.scale;offset_left=(left-padded_left)*self.scale
                self.output[:,:,top*self.scale:bottom*self.scale,left*self.scale:right*self.scale]=restored[
                    :,:,offset_top:offset_top+(bottom-top)*self.scale,offset_left:offset_left+(right-left)*self.scale]


class RestorationRuntime:
    def __init__(self, weights: Path):
        if not torch.cuda.is_available():
            raise RuntimeError("CUDA unavailable: install the matching GPU PyTorch build.")
        self.device = torch.device("cuda")
        weights=weights.resolve()
        def local_weight(url,**kwargs):
            name=Path(urlparse(url).path).name
            if name not in ("detection_Resnet50_Final.pth","parsing_parsenet.pth") or not (weights/name).is_file():
                raise RuntimeError("Required face model is missing; run download_models.py.")
            return str(weights/name)
        facexlib.detection.load_file_from_url=local_weight
        facexlib.parsing.load_file_from_url=local_weight
        self.background = StrictRealESRGANer(
            scale=4,
            model_path=str(weights / "RealESRGAN_x4plus.pth"),
            model=RRDBNet(num_in_ch=3, num_out_ch=3, num_feat=64, num_block=23, num_grow_ch=32, scale=4),
            tile=192, tile_pad=16, pre_pad=0, half=True, device=self.device,
        )
        self.face = GFPGANer(
            model_path=str(weights / "GFPGANv1.4.pth"), upscale=2,
            arch="clean", channel_multiplier=2, bg_upsampler=self.background, device=self.device,
        )

    @torch.inference_mode()
    def enhance(self, image, scale: float, faces: bool):
        try:
            return self._restore(image,scale,faces)
        finally:
            self.face.face_helper.clean_all()
            self.face.face_helper.input_img=None
            self.background.img=None
            self.background.output=None
            torch.cuda.empty_cache()

    def _restore(self,image,scale,faces):
        background=self.background.enhance(image,outscale=scale)[0]
        if not faces:
            return background,0
        helper=self.face.face_helper
        helper.clean_all();helper.set_upscale_factor(scale);helper.read_image(image)
        helper.get_face_landmarks_5(only_center_face=False,eye_dist_threshold=5)
        helper.all_landmarks_5=helper.all_landmarks_5[:20]
        helper.det_faces=helper.det_faces[:20]
        helper.align_warp_face()
        for crop in helper.cropped_faces:
            rgb=cv2.cvtColor(crop,cv2.COLOR_BGR2RGB)
            tensor=torch.from_numpy(np.ascontiguousarray(rgb.transpose(2,0,1))).float().div(127.5).sub(1).unsqueeze(0).to(self.device)
            result=self.face.gfpgan(tensor,return_rgb=False,weight=.5)[0]
            restored=result.squeeze(0).float().cpu().clamp(-1,1).add(1).mul(127.5).round().byte().permute(1,2,0).numpy()
            helper.add_restored_face(cv2.cvtColor(restored,cv2.COLOR_RGB2BGR))
        helper.get_inverse_affine(None)
        output=helper.paste_faces_to_input_image(upsample_img=background)
        if output is None:
            raise RuntimeError("Face restoration returned no image.")
        return output,len(helper.restored_faces)
