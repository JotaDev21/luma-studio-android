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
