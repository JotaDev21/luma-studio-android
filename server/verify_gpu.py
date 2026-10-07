"""Exercise the real GPU service on the existing public NASA fixture."""
from io import BytesIO
import json
from pathlib import Path
import time

import numpy as np
from PIL import Image,ImageFilter,ImageDraw
import httpx

ROOT=Path(__file__).resolve().parent.parent
DATA=ROOT/".tools/luma-ai"
OUTPUT=DATA/"checks"

if __name__=="__main__":
    OUTPUT.mkdir(parents=True,exist_ok=True)
    settings=json.loads((DATA/"connection.json").read_text())
    service=json.loads((DATA/"server-settings.json").read_text(encoding="utf-8-sig"))
    endpoint=f"http://{service['listen_address']}:{service['port']}"
    source=ROOT/"app/src/androidTest/assets/portrait-nasa.jpg"
    original=source.read_bytes()
    with Image.open(source) as image:
        image=image.convert("RGB")
        image.thumbnail((240,320),Image.Resampling.LANCZOS)
        image=image.filter(ImageFilter.GaussianBlur(.5))
        encoded=BytesIO();image.save(encoded,format="JPEG",quality=40)
    degraded=Image.open(BytesIO(encoded.getvalue())).convert("RGB")
    degraded.save(OUTPUT/"input.png")
    input_png=(OUTPUT/"input.png").read_bytes()
    results=[]
    with httpx.Client(timeout=300,trust_env=False) as client:
        for faces in (False,True):
            started=time.monotonic()
            reply=client.post(endpoint+f"/enhance?scale=4&faces={str(faces).lower()}&strength=0.7",content=input_png,headers={"Authorization":"Bearer "+settings['token'],"Content-Type":"image/png"})
            reply.raise_for_status()
            result=Image.open(BytesIO(reply.content)).convert("RGB")
            assert result.size==(degraded.width*4,degraded.height*4)
            assert "exif" not in result.info
            bicubic=degraded.resize(result.size,Image.Resampling.BICUBIC)
            difference=float(np.abs(np.asarray(result,dtype=np.float32)-np.asarray(bicubic,dtype=np.float32)).mean())
            assert difference>.2,"The output must contain actual AI treatment, not a simple resize"
            if faces:
                assert int(reply.headers["x-luma-faces"])>0,"The portrait fixture needs a restored face"
            result.save(OUTPUT/("face-restored.png" if faces else "general-restored.png"))
            print("faces",faces,"size",result.size,"seconds",round(time.monotonic()-started,3),"difference_from_bicubic",round(difference,3),"restored_faces",reply.headers["x-luma-faces"])
            results.append(result)
    assert source.read_bytes()==original
    views=[degraded.resize(results[0].size,Image.Resampling.NEAREST)]+results
    labels=["Baixa resolucao (ampliada)","IA geral","IA geral + rosto"]
    sheet=Image.new("RGB",(960,490),(18,21,26));draw=ImageDraw.Draw(sheet)
    for index,(image,label) in enumerate(zip(views,labels)):
        image.thumbnail((310,440),Image.Resampling.LANCZOS)
        sheet.paste(image,(index*320+(320-image.width)//2,36))
        draw.text((index*320+12,12),label,fill=(242,235,225))
    sheet.save(OUTPUT/"comparison.png")
    print("GPU checks passed; source unchanged")
