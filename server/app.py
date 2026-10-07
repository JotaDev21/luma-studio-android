"""Private, authenticated PC service for the Luma Android editor."""
import asyncio
from contextlib import asynccontextmanager
from io import BytesIO
import json
import math
import os
from pathlib import Path
import secrets
import time

import cv2
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import Response
import numpy as np
from PIL import Image, ImageOps, UnidentifiedImageError
from pydantic import BaseModel, Field

MAX_UPLOAD=64_000_000
MAX_INPUT_EDGE=2048
MAX_OUTPUT_EDGE=4096
MAX_PIXELS=32_000_000
Image.MAX_IMAGE_PIXELS=MAX_PIXELS


def restore_pixels(runtime, body:bytes, scale:int, faces:bool, strength:float):
    try:
        with Image.open(BytesIO(body)) as image:
            if image.width*image.height>MAX_PIXELS or max(image.size)>8192:
                raise HTTPException(413,"Image dimensions exceed limits")
            image=ImageOps.exif_transpose(image)
            image.thumbnail((MAX_INPUT_EDGE,MAX_INPUT_EDGE),Image.Resampling.LANCZOS)
            alpha=np.asarray(image.convert("RGBA"))[:,:,3].copy() if "A" in image.getbands() or "transparency" in image.info else None
            rgb=np.asarray(image.convert("RGB"))
    except (UnidentifiedImageError, OSError, Image.DecompressionBombError, Image.DecompressionBombWarning, ValueError) as error:
        raise HTTPException(422,"Invalid image") from error
    bgr=cv2.cvtColor(rgb,cv2.COLOR_RGB2BGR)
    effective_scale=min(scale,MAX_OUTPUT_EDGE/max(bgr.shape[:2]))
    width=round(bgr.shape[1]*effective_scale);height=round(bgr.shape[0]*effective_scale)
    if width*height>MAX_PIXELS:
        raise HTTPException(413,"Output dimensions exceed limits")
    restored,face_count=runtime.enhance(bgr,effective_scale,faces)
    if restored is None or not np.isfinite(restored).all():
        raise RuntimeError("Invalid model output")
    restored=cv2.resize(restored,(width,height),interpolation=cv2.INTER_LANCZOS4)
    base=cv2.resize(bgr,(width,height),interpolation=cv2.INTER_LANCZOS4)
    result=cv2.addWeighted(restored,strength,base,1-strength,0)
    if alpha is not None:
        result=np.dstack((result,cv2.resize(alpha,(width,height),interpolation=cv2.INTER_LINEAR)))
    ok,encoded=cv2.imencode(".png",result,[cv2.IMWRITE_PNG_COMPRESSION,3])
    if not ok:
        raise RuntimeError("Encoding failed")
    return encoded.tobytes(),width,height,face_count


def create_app(config_path:Path|None=None, runtime=None):
    path=config_path or Path(os.environ.get("LUMA_AI_CONFIG",".tools/luma-ai/connection.json"))
    path.parent.mkdir(parents=True,exist_ok=True)
    if path.exists():
        settings=json.loads(path.read_text(encoding="utf-8"))
    else:
        settings={"token":secrets.token_urlsafe(48)}
        path.write_text(json.dumps(settings),encoding="utf-8")
    settings["pair_code"]=f"{secrets.randbelow(1_000_000):06}"
    settings["pair_expires"]=time.time()+600
    path.write_text(json.dumps(settings),encoding="utf-8")
    pair_attempts=0
    lock=asyncio.Lock()

    @asynccontextmanager
    async def lifespan(app):
        app.state.runtime=runtime
        app.state.ready=runtime is not None
        if runtime is None:
            from server.runtime import RestorationRuntime
            weights=Path(os.environ.get("LUMA_AI_WEIGHTS",".tools/luma-ai/weights"))
            app.state.runtime=await asyncio.to_thread(RestorationRuntime,weights)
            app.state.ready=True
        yield
        app.state.runtime=None

    app=FastAPI(lifespan=lifespan,docs_url=None,redoc_url=None,openapi_url=None)

    @app.get("/health")
    async def health():
        return {"ready":getattr(app.state,"ready",False),"busy":lock.locked(),"service":"Luma AI","max_output_edge":MAX_OUTPUT_EDGE}

    class PairRequest(BaseModel):
        code:str=Field(pattern=r"^[0-9]{6}$")

    @app.post("/pair")
    async def pair(request:PairRequest):
        nonlocal pair_attempts
        if time.time()>settings["pair_expires"] or pair_attempts>=10:
            raise HTTPException(403,"Pairing expired; restart service to generate a new code")
        pair_attempts+=1
        if not secrets.compare_digest(request.code,settings["pair_code"]):
            raise HTTPException(401,"Invalid pairing code")
        settings["pair_expires"]=0
        path.write_text(json.dumps(settings),encoding="utf-8")
        return {"token":settings["token"]}

    def require_authorization(request:Request):
        bearer=request.headers.get("Authorization","").encode("utf-8")
        if not secrets.compare_digest(bearer,("Bearer "+settings["token"]).encode("ascii")):
            raise HTTPException(401,"Unauthorized")

    @app.post("/pair/renew")
    async def renew_pairing(request:Request):
        nonlocal pair_attempts
        require_authorization(request)
        pair_attempts=0
        settings["pair_code"]=f"{secrets.randbelow(1_000_000):06}"
        settings["pair_expires"]=time.time()+600
        path.write_text(json.dumps(settings),encoding="utf-8")
        return {"code":settings["pair_code"],"expires_in":600}

    @app.post("/enhance")
    async def enhance(request:Request,scale:int=2,faces:bool=True,strength:float=.7):
        require_authorization(request)
        if scale not in (2,4) or not math.isfinite(strength) or not .1<=strength<=1:
            raise HTTPException(422,"Invalid enhancement options")
        if not getattr(app.state,"ready",False):
            raise HTTPException(503,"Models are not ready")
        if lock.locked():
            raise HTTPException(409,"GPU busy")
        async with lock:
            body=bytearray()
            async for chunk in request.stream():
                body.extend(chunk)
                if len(body)>MAX_UPLOAD:
                    raise HTTPException(413,"Upload exceeds limit")
            try:
                work=asyncio.create_task(asyncio.to_thread(restore_pixels,app.state.runtime,bytes(body),scale,faces,strength))
                try:
                    png,width,height,face_count=await asyncio.shield(work)
                except asyncio.CancelledError:
                    # Keep the GPU reserved until the worker finishes, even if the client leaves.
                    await work
                    raise
            except HTTPException:
                raise
            except Exception as error:
                # Do not include images, filesystem paths or secret keys in logs/responses.
                raise HTTPException(500,"Restoration failed") from error
            return Response(png,media_type="image/png",headers={"Cache-Control":"no-store","X-Luma-Width":str(width),"X-Luma-Height":str(height),"X-Luma-Faces":str(face_count)})

    return app
