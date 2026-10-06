import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'app/src/main/assets/effects/presets.json'
values=json.loads(root.read_text(encoding='utf-8'))
values=[e for e in values if not e['meta']['id'].startswith('pro-')]
groups={
'Lens': ['fisheye','wide','barrel','lensdirt','flare'],
'Film':['halation','gateweave','flicker'],
'Digital':['vhs','crt','rgbshift','pixel','glitch','scanlines','digitalnoise','compression','datamosh'],
'Light':['glow','neon','softlight','flash','overexposure','coloredlight','prism','rainbow'],
'Blur':['motionblur','zoomblur','radialblur','directionalblur','shake'],
'Texture':['paper','canvas','plastic','glass','metal','photocopy','newspaper'],
'Overlay':['rain','stars','hearts','scribble','tape','timestamp'],
'Frame':['polaroid','filmstrip','disposable','camcorder','digicam','magazineframe','paperframe','scrapbookframe']}
names={'gateweave':'Film Gate · still','flicker':'Film Exposure · still','datamosh':'Block Displacement · still','motionblur':'Motion Blur','zoomblur':'Zoom Blur','radialblur':'Radial Blur','directionalblur':'Directional Blur','lensdirt':'Lens Dirt','rgbshift':'RGB Split','digitalnoise':'Digital Noise','coloredlight':'Colored Light','paperframe':'Paper Edge','magazineframe':'Magazine Border','scrapbookframe':'Scrapbook Edge','filmstrip':'Film Strip','softlight':'Soft Light','overexposure':'Overexposure'}
for category,types in groups.items():
 for t in types:
  parameters={'amount':.65 if category!='Frame' else 1}
  if t in ('halation','glow','softlight'): parameters.update(radius=.45,threshold=.7)
  if t in ('vhs','crt','rgbshift','pixel','glitch','scanlines','digitalnoise','compression','motionblur','zoomblur','radialblur','directionalblur','shake','polaroid','filmstrip','disposable','camcorder','digicam','magazineframe','paperframe','scrapbookframe'): parameters['size']=.4
  if t in ('lensdirt','gateweave','flicker','vhs','glitch','digitalnoise','datamosh','paper','metal','photocopy','rain','stars','hearts','paperframe','scrapbookframe'): parameters['seed']=.35
  if t in ('rgbshift','directionalblur'): parameters['angle']=.15
  if t=='coloredlight': parameters['color']=.8
  values.append({'schemaVersion' :1,'meta':{'id':'pro-'+t,'name':names.get(t,t.title()),'category':category,'tags':[category.lower(),t,'photo','static']},'type':t,'parameters':parameters})
root.write_text(json.dumps(values,ensure_ascii=False,indent=2),encoding='utf-8')
print('Created',len(values),'effects')
