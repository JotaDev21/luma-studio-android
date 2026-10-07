"""Fetch only the official pretrained weights; store outside version control."""
from pathlib import Path
from urllib.request import urlopen
import hashlib
import json

WEIGHTS=Path(".tools/luma-ai/weights")
MODELS={
    "RealESRGAN_x4plus.pth":"https://github.com/xinntao/Real-ESRGAN/releases/download/v0.1.0/RealESRGAN_x4plus.pth",
    "GFPGANv1.4.pth":"https://github.com/TencentARC/GFPGAN/releases/download/v1.3.0/GFPGANv1.4.pth",
    "detection_Resnet50_Final.pth":"https://github.com/xinntao/facexlib/releases/download/v0.1.0/detection_Resnet50_Final.pth",
    "parsing_parsenet.pth":"https://github.com/xinntao/facexlib/releases/download/v0.2.2/parsing_parsenet.pth",
}
EXPECTED_SHA256={
    "RealESRGAN_x4plus.pth":"4fa0d38905f75ac06eb49a7951b426670021be3018265fd191d2125df9d682f1",
    "GFPGANv1.4.pth":"e2cd4703ab14f4d01fd1383a8a8b266f9a5833dacee8e6a79d3bf21a1b6be5ad",
    "detection_Resnet50_Final.pth":"6d1de9c2944f2ccddca5f5e010ea5ae64a39845a86311af6fdf30841b0a5a16d",
    "parsing_parsenet.pth":"3d558d8d0e42c20224f13cf5a29c79eba2d59913419f945545d8cf7b72920de2",
}

if __name__=="__main__":
    WEIGHTS.mkdir(parents=True,exist_ok=True)
    hashes={}
    for name,url in MODELS.items():
        target=WEIGHTS/name
        if not target.exists():
            temporary=target.with_suffix(".download")
            try:
                with urlopen(url,timeout=60) as response,temporary.open("wb") as output:
                    while chunk:=response.read(1024*1024):
                        output.write(chunk)
                temporary.replace(target)
            finally:
                temporary.unlink(missing_ok=True)
        hashes[name]=hashlib.sha256(target.read_bytes()).hexdigest()
        if hashes[name]!=EXPECTED_SHA256[name]:
            raise RuntimeError(f"Unexpected model checksum: {name}")
        print(name,target.stat().st_size,"bytes",hashes[name])
    (WEIGHTS/"sha256.json").write_text(json.dumps(hashes,indent=2),encoding="utf-8")
