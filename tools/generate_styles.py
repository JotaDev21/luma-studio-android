import json
import copy
from pathlib import Path
root=Path('app/src/main/assets')
filters=json.loads((root/'filters/presets.json').read_text(encoding='utf-8'))
effects=json.loads((root/'effects/presets.json').read_text(encoding='utf-8'))
rows=[('Film Sunday','Film','grain-fine','bloom-soft'),('Night Digicam','CCD','bloom-night','chromatic-subtle'),('Cinema Diary','Cinema','grain-35','frame-cinema'),('Y2K Party','Y2K','chromatic-prism','bloom-dream'),('Portrait Glow','Portrait','bloom-soft','grain-fine'),('Monochrome Print','BW','grain-35','frame-minimal')]
styles=[]
for i,(name,category,a,b) in enumerate(rows):
    preset=next(p for p in filters if p['meta']['category']==category)
    stack=[{'id':f'style_effect_{j}','preset':next(e for e in effects if e['meta']['id']==key),'intensity':.65,'enabled':True} for j,key in enumerate([a,b])]
    styles.append({'meta':{'id':f'luma-style-{i}','name':name,'category':category,'tags':[category.lower(),'style']},'filter':{'preset':preset,'intensity':.85},'effectLayers':stack})
extra=[('Window Film','Film','pro-halation','grain-fine'),('Pocket VHS','CCD','pro-vhs','pro-digicam'),('Golden Cinema','Cinema','pro-flare','frame-cinema'),('Prism Party','Y2K','pro-prism','pro-glow'),('Soft Portrait','Portrait','pro-softlight','grain-fine'),('Paper Noir','BW','pro-paper','pro-photocopy'),('Rainy Night','CCD','pro-rain','pro-glow'),('Neon Street','Cinema','pro-neon','pro-rgbshift'),('Instant Summer','Film','pro-polaroid','grain-fine'),('Chrome Memories','Y2K','pro-metal','pro-flash'),('Window Skin','Portrait','pro-softlight','grain-fine'),('Print Contact','BW','pro-filmstrip','grain-35'),('Coastal Paper','IG','pro-paperframe','pro-paper'),('Star Diary','IG','pro-stars','pro-glow'),('Dream Hearts','IG','pro-hearts','bloom-soft'),('City Motion','Cinema','pro-directionalblur','grain-fine'),('Analog Canvas','Film','pro-canvas','pro-halation'),('Digital Recall','CCD','pro-compression','pro-digitalnoise')]
for i,(name,category,a,b) in enumerate(extra):
 preset=next(p for p in filters if p['meta']['category']==category and p['meta']['id'].endswith('-'+('night' if i%3==0 else 'window' if i%3==1 else 'sunset')))
 # Preserve the original embedded Pro effect defaults, independently of the newer effect catalog.
 stack=[]
 for j,key in enumerate((a,b)):
  effect=copy.deepcopy(next(e for e in effects if e['meta']['id']==key))
  if key.startswith('pro-'):
   effect['parameters']={'amount':1 if effect['meta']['category']=='Frame' else .65,'size':.4,'seed':.35,'radius':.45,'angle':.15,'warmth':.8}
  stack.append({'id':'style_effect_'+str(j),'preset':effect,'intensity':.24 if key=='pro-directionalblur' else .48,'enabled':True})
 styles.append({'meta':{'id':'luma-style-pro-'+str(i),'name':name,'category':category,'tags':[category.lower(),'style','night' if i%3==0 else 'day']},'filter':{'preset':preset,'intensity':.8},'effectLayers':stack})
# Twenty-four photo looks and twenty-four editable graphic styles.
groups=[
 ('Social','Social','social-oslo-classic','social-paris-soft','bloom-soft','grain-fine',0xFFF3EEE5,0xFFF0AB83),
 ('Golden','Hora dourada','social-rio-classic','social-valencia-soft','pro-halation','grain-fine',0xFFFAE8C9,0xFFD9A66E),
 ('Editorial','Editorial','social-ludwig-classic','studio-editorial-02','pro-softlight','grain-fine',0xFFF5F0E5,0xFFB9AA93),
 ('Romantic','Romance','social-aden-soft','studio-soft-03','bloom-soft','pro-hearts',0xFFF3DDE4,0xFFE7A8BD),
 ('Travel','Viagem','social-lark-classic','social-rio-pop','pro-halation','grain-fine',0xFFEEEADB,0xFFCBAE7A),
 ('Night','Noite','jdm-night-01','social-clarendon-pop','bloom-night','grain-35',0xFFD9EBF4,0xFF93C6D9),
 ('Minimal','Minimal','social-oslo-soft','social-moon-classic','pro-softlight','grain-fine',0xFFF0EBE1,0xFFC8C4B9),
 ('Magazine','Revista','social-ludwig-pop','social-tokyo-classic','frame-minimal','grain-fine',0xFFF7F0DF,0xFFE2A774),
 ('Y2K','Y2K','studio-neon-01','social-aden-pop','chromatic-subtle','pro-prism',0xFFF1DFFD,0xFFC8A0EF),
 ('Scrapbook','Papel','social-gingham-matte','social-valencia-matte','pro-paper','grain-fine',0xFFF2E2C7,0xFFD2B589),
 ('Music','Playlist','social-clarendon-soft','studio-cool-02','bloom-soft','grain-fine',0xFFE6E0F4,0xFFB7A9F5),
 ('Polaroid','Instantâneo','social-gingham-classic','jdm-chrome-01','pro-polaroid','grain-fine',0xFFF7F2E8,0xFFCFB399),
]
def signed(n):return n if n<2**31 else n-2**32
def graphic(key,kind,x,y,w,h,color,content='',size=38,family='sans-serif',weight=400):
 layer={'id':key,'type':kind,'content':content,'color':signed(color),'transform':{'x':x,'y':y,'width':w,'height':h}}
 if kind=='text':layer['typography']={'size':size,'family':family,'color':signed(color),'weight':weight,'alignment':'center'}
 return layer
for family_index,(category,name,a,b,fx1,fx2,paper,accent) in enumerate(groups):
 for variant in range(4):
  graphics=variant>=2;key=f'luma-style-social-{family_index+1:02d}-{variant+1:02d}'
  preset=next(p for p in filters if p['meta']['id']==(a if variant%2==0 else b))
  stack=[{'id':key+'-effect-'+str(j),'preset':next(e for e in effects if e['meta']['id']==fx),
    'intensity':(.13 if fx in ('pro-prism','chromatic-subtle','pro-hearts') else .26 if j==0 else .22)+(.025 if variant%2 else 0),'enabled':True} for j,fx in enumerate((fx1,fx2))]
  layers=[]
  if graphics:
   if variant==2:
    # A restrained caption card, always editable and scaled by the destination canvas.
    layers=[graphic('CAPTION_PAPER','shape',80,1490,920,270,paper),
      graphic('CAPTION_LINE','shape',125,1518,830,8,accent),
      graphic('CAPTION_TITLE','text',115,1550,850,80,0xFF302D2A,
        {'Romantic':'a gente.','Travel':'um lugar para guardar.','Night':'depois do pôr do sol.',
         'Music':'minha trilha sonora.','Scrapbook':'ficou na memória.','Polaroid':'um instante só nosso.'}.get(category,'dias assim.'),
        50,'serif' if category not in ('Y2K','Night','Music') else 'sans-serif-condensed'),
      graphic('CAPTION_NOTE','text',130,1655,820,65,0xFF70665C,'sua história, do seu jeito.',27,'monospace')]
   else:
    # A thin magazine frame with top signature and a category-specific symbol.
    layers=[graphic('FRAME_TOP','shape',60,125,960,8,paper),graphic('FRAME_BOTTOM','shape',60,1760,960,8,paper),
      graphic('FRAME_LEFT','shape',60,125,8,1643,paper),graphic('FRAME_RIGHT','shape',1012,125,8,1643,paper),
      graphic('SIGNATURE','text',120,155,840,110,paper,
        {'Romantic':'LOVE NOTES','Travel':'OFF THE ROUTINE','Night':'AFTER HOURS','Music':'ON REPEAT',
         'Magazine':'THE PERSONAL ISSUE','Y2K':'SOFT SIGNAL','Scrapbook':'COLLECTED DAYS'}.get(category,'VISUAL DIARY'),
        48,'sans-serif-condensed',700),
      graphic('SYMBOL','text',885,1590,100,110,accent,'♡' if category=='Romantic' else '✦',60,'serif')]
  if variant==3:
   next(layer for layer in layers if layer['id']=='SIGNATURE')['typography'].update(stroke=1.6,strokeColor=signed(0xFF17191F),shadow=3)
  suffix=('Natural','Intenso','Legenda','Assinatura')[variant]
  styles.append({'schemaVersion':1,'meta':{'id':key,'name':name+' / '+suffix,'category':category,
    'tags':[category.lower(),'novo','instagram','social','style','estilo',name.lower(),suffix.lower(),
      'moldura texto editavel' if graphics else 'so cor sem elementos foto',
      'retrato selfie' if category in ('Social','Romantic','Minimal','Editorial') else 'story feed']},
    'filter':{'preset':preset,'intensity':.78 if category=='Night' else .85},
    'adjustments':{'exposure':.015*(family_index%4-1)+(.035 if variant%2 else 0),'vignette':.025 if graphics else 0},
    'effectLayers':stack,'layers':layers,'width':1080,'height':1920})
assert len(styles)==72
(root/'styles').mkdir(exist_ok=True)
(root/'styles/presets.json').write_text(json.dumps(styles,ensure_ascii=False,indent=2),encoding='utf-8')
print(f'Created {len(styles)} original styles.')
