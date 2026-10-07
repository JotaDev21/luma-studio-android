from concurrent.futures import ThreadPoolExecutor
from io import BytesIO
import json
import threading

import cv2
from fastapi.testclient import TestClient
import numpy as np
from PIL import Image
import pytest

from server.app import create_app


class FixtureRuntime:
    """Deliberately different pixels to verify transport/blending, not model quality."""
    calls=0
    fail=False
    def enhance(self,image,scale,faces):
        self.calls+=1
        if self.fail:
            raise RuntimeError("failure")
        output=cv2.resize(image,None,fx=scale,fy=scale)
        output[:,:,0]=255
        return output,1 if faces else 0


def photo(alpha=False):
    pixels=np.zeros((40,30,4 if alpha else 3),dtype=np.uint8)
    pixels[:,:,:3]=(40,80,120)
    if alpha:
        pixels[:,:,3]=128
    image=Image.fromarray(pixels)
    output=BytesIO();image.save(output,format="PNG")
    return output.getvalue()


@pytest.fixture
def service(tmp_path):
    config=tmp_path/"connection.json";runtime=FixtureRuntime()
    with TestClient(create_app(config,runtime)) as client:
        settings=json.loads(config.read_text())
        yield client,runtime,settings


def test_pairing_is_one_time_and_requires_the_correct_code(service):
    client,_,settings=service
    bad_code="000000" if settings["pair_code"]!="000000" else "111111"
    assert client.post("/pair",json={"code":bad_code}).status_code==401
    reply=client.post("/pair",json={"code":settings["pair_code"]})
    assert reply.status_code==200 and reply.json()["token"]==settings["token"]
    assert client.post("/pair",json={"code":settings["pair_code"]}).status_code==403
    assert client.post("/pair/renew").status_code==401
    renewed=client.post("/pair/renew",headers={"Authorization":"Bearer "+settings["token"]})
    assert renewed.status_code==200 and renewed.json()["expires_in"]==600
    assert client.post("/pair",json={"code":renewed.json()["code"]}).json()["token"]==settings["token"]


def test_unauthorized_and_invalid_images_never_reach_the_model(service):
    client,runtime,settings=service
    assert client.post("/enhance",content=photo()).status_code==401
    auth={"Authorization":"Bearer "+settings["token"]}
    assert client.post("/enhance",content=b"not an image",headers=auth).status_code==422
    assert client.post("/enhance?scale=32",content=photo(),headers=auth).status_code==422
    assert client.post("/enhance?strength=nan",content=photo(),headers=auth).status_code==422
    assert runtime.calls==0


def test_output_dimensions_blending_and_transparency_are_real_png_pixels(service):
    client,runtime,settings=service
    auth={"Authorization":"Bearer "+settings["token"]}
    for scale in (2,4):
        reply=client.post(f"/enhance?scale={scale}&faces=true&strength=0.5",content=photo(True),headers=auth)
        assert reply.status_code==200
        assert reply.headers["cache-control"]=="no-store"
        assert reply.headers["x-luma-faces"]=="1"
        with Image.open(BytesIO(reply.content)) as result:
            assert result.size==(30*scale,40*scale)
            assert result.mode=="RGBA"
            assert result.getpixel((10,10))==(40,80,188,128)
            assert "exif" not in result.info
    assert runtime.calls==2


def test_model_failure_returns_an_error_and_releases_the_gpu(service):
    client,runtime,settings=service
    auth={"Authorization":"Bearer "+settings["token"]}
    runtime.fail=True
    assert client.post("/enhance",content=photo(),headers=auth).status_code==500
    runtime.fail=False
    assert client.post("/enhance",content=photo(),headers=auth).status_code==200


def test_a_second_job_does_not_run_on_the_gpu_concurrently(service):
    client,runtime,settings=service
    started=threading.Event();release=threading.Event()
    original=runtime.enhance
    def slow(*args):
        started.set();assert release.wait(5)
        return original(*args)
    runtime.enhance=slow
    auth={"Authorization":"Bearer "+settings["token"]}
    with ThreadPoolExecutor() as executor:
        first=executor.submit(client.post,"/enhance",content=photo(),headers=auth)
        assert started.wait(5)
        try:
            assert client.post("/enhance",content=photo(),headers=auth).status_code==409
        finally:
            release.set()
        assert first.result().status_code==200
    assert runtime.calls==1
