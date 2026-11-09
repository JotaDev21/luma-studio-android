"""Tile stitching and failure propagation, using small CPU tensors."""
import pytest
import torch
from server.runtime import StrictRealESRGANer


def tiled(model):
    engine=object.__new__(StrictRealESRGANer)
    engine.img=torch.arange(7*9,dtype=torch.float32).reshape(1,1,7,9)
    engine.scale=2;engine.tile_size=3;engine.tile_pad=1;engine.model=model
    return engine


def test_tiles_cover_edges_without_seams():
    model=lambda image:torch.nn.functional.interpolate(image,scale_factor=2,mode="nearest")
    engine=tiled(model)
    engine.tile_process()
    assert torch.equal(engine.output,model(engine.img))


def test_later_failed_tile_is_not_replaced_with_previous_tile():
    calls=0
    def model(image):
        nonlocal calls
        calls+=1
        if calls==2:
            raise RuntimeError("GPU allocation failed")
        return torch.nn.functional.interpolate(image,scale_factor=2,mode="nearest")
    engine=tiled(model)
    with pytest.raises(RuntimeError,match="GPU allocation failed"):
        engine.tile_process()
    assert calls==2


def test_inference_server_disables_the_slurm_launcher(monkeypatch):
    from basicsr.utils import dist_util

    monkeypatch.setenv("SLURM_NODELIST", "fixture")
    with pytest.raises(RuntimeError, match="SLURM training is unavailable"):
        dist_util._init_dist_slurm("nccl")


def test_modified_model_is_rejected_before_loading(tmp_path, monkeypatch):
    import hashlib

    from server import download_models

    good = b"test checkpoint"
    model = tmp_path / "test.pth"
    model.write_bytes(good)
    monkeypatch.setattr(download_models, "EXPECTED_SHA256", {
        model.name: hashlib.sha256(good).hexdigest(),
    })
    download_models.verify_weights(tmp_path)

    model.write_bytes(b"changed checkpoint")
    with pytest.raises(RuntimeError, match="Unexpected model checksum"):
        download_models.verify_weights(tmp_path)
