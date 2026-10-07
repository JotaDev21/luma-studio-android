import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'app/src/main/assets/effects/presets.json'
values=json.loads(root.read_text(encoding='utf-8'))
values=[e for e in values if not e['meta']['id'].startswith(('pro-','flash-effect-'))]
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
# These illuminate existing pixels; they do not invent scene depth or cast shadows.
# amount, radius, hardness, falloff, warmth, tint, grain, x, y
flash_rows=[
 ('bolso','Flash de bolso',.55,.55,.75,.42,.50,.50,.02,.50,.52),
 ('direto','Flash direto',.78,.48,.93,.65,.50,.50,.03,.50,.50),
 ('duro','Flash duro',.88,.37,1.0,.78,.48,.49,.05,.50,.50),
 ('estourado','Flash estourado',1.0,.60,.95,.40,.52,.50,.06,.50,.52),
 ('ccd-2003','CCD 2003',.72,.58,.85,.40,.39,.45,.16,.50,.50),
 ('ccd-frio','CCD frio',.66,.52,.82,.55,.22,.46,.20,.50,.52),
 ('ccd-festa','CCD festa',.84,.62,.93,.32,.62,.55,.24,.48,.52),
 ('ccd-suave','CCD suave',.52,.72,.32,.25,.45,.50,.12,.50,.50),
 ('descartavel-89','Descartável 89',.67,.48,.78,.65,.69,.37,.34,.50,.50),
 ('descartavel-quente','Descartável quente',.72,.58,.67,.55,.85,.43,.29,.50,.52),
 ('descartavel-grao','Descartável ISO 800',.78,.44,.84,.68,.63,.34,.58,.50,.50),
 ('descartavel-lavado','Descartável lavado',.50,.68,.35,.30,.71,.41,.30,.50,.50),
 ('verde-90','Flash verde 90',.72,.52,.86,.64,.52,.13,.28,.50,.50),
 ('oliva','Flash oliva',.64,.60,.72,.58,.76,.20,.32,.50,.52),
 ('verde-duro','Verde intenso',.88,.40,.97,.76,.48,.05,.40,.50,.50),
 ('verde-suave','Verde suave',.49,.72,.30,.28,.56,.28,.16,.50,.50),
 ('azul-noturno','Flash azul noturno',.78,.46,.86,.72,.13,.50,.25,.50,.50),
 ('gelo','Flash gelo',.65,.65,.74,.43,.02,.53,.10,.50,.52),
 ('violeta-95','Flash violeta 95',.69,.57,.67,.57,.26,.88,.31,.50,.50),
 ('azul-grao','Azul ISO 1600',.86,.40,.95,.82,.16,.57,.62,.50,.50),
 ('editorial','Flash editorial',.62,.72,.72,.32,.54,.50,.01,.50,.50),
 ('beauty','Flash beauty',.48,.82,.12,.18,.57,.54,.00,.50,.50),
 ('lateral','Flash lateral',.72,.52,.66,.57,.61,.50,.04,.32,.52),
 ('spot','Flash spotlight',.83,.26,.88,.94,.47,.50,.07,.50,.54),
]
family_tags=[['direto','celular','festa'],['ccd','compacta','digital','anos 2000'],['descartavel','filme','retro','anos 80','anos 90'],['verde','oliva','retro','anos 90','japao'],['azul','violeta','noite','retro'],['editorial','beauty','lateral','spotlight']]
for index,(slug,name,*parameters) in enumerate(flash_rows):
 values.append({'schemaVersion':1,'meta':{'id':'flash-effect-'+slug,'name':name,'category':'Flash','tags':['flash','retrato','selfie','luz','camera',slug.replace('-',' ')]+family_tags[index//4]},'type':'flash','parameters':dict(zip(('amount','radius','hardness','falloff','warmth','tint','grain','x','y'),parameters))})
root.write_text(json.dumps(values,ensure_ascii=False,indent=2),encoding='utf-8')
print('Created',len(values),'effects')
