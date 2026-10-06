"""Original editable compositions, all geometry lives in JSON assets."""
import json
from pathlib import Path

rows=[
 ("couple-memories","Couple Memories","Couple","OUR MEMORIES",0,0xFFEDE5D8,0xFF32332D),
 ("romantic-note","Romantic Note","Romantic","always, you.",1,0xFFF0DDE2,0xFF754856),
 ("travel-journal","Travel Journal","Travel","somewhere new",2,0xFFE4E8DC,0xFF344A3F),
 ("photo-dump","Photo Dump","Photo Dump","LIFE LATELY",3,0xFF202123,0xFFF1EFE7),
 ("gym-focus","Gym Focus","Gym","ONE MORE REP.",4,0xFF151719,0xFFD9F99D),
 ("music-moment","Music Moment","Music","ON REPEAT",5,0xFF272637,0xFFE0D8F5),
 ("birthday-wish","Birthday Wish","Birthday","MAKE A WISH",1,0xFFF0E7D1,0xFF66543C),
 ("minimal-day","Minimal Day","Minimal","a quieter kind of day",4,0xFFF2F0EB,0xFF252525),
 ("film-contact","Film Contact","Film","CONTACT SHEET / 01",3,0xFF232521,0xFFE0D3B5),
 ("instant-diary","Instant Diary","Polaroid","little moments",0,0xFFCBC5B8,0xFF36312B),
 ("magazine-cover","Magazine Cover","Magazine","THE EVERYDAY ISSUE",4,0xFFEEE7DC,0xFF393831),
 ("y2k-signal","Y2K Signal","Y2K","DIGITAL DAYDREAM",2,0xFF322641,0xFFE9AFF2),
 ("dark-frames","Dark Frames","Dark","AFTER HOURS",1,0xFF121719,0xFFCBD9DD),
 ("scrapbook-page","Scrapbook Page","Scrapbook","collected memories",0,0xFFE4D6BA,0xFF745638),
 ("night-notes","Night Notes","Night","WHEN THE CITY SLEEPS",2,0xFF122631,0xFFBCCFDD),
 ("memory-strip","Memory Strip","Memories","THINGS TO REMEMBER",3,0xFF3B4038,0xFFE8E8D8),
 ("editorial-two","Editorial Two","Editorial","IN THE FRAME",1,0xFFEEEDE8,0xFF1D2320),
 ("aesthetic-grid","Aesthetic Grid","Aesthetic","a few favorites",3,0xFFCDC3B5,0xFF4C4942),
 ("party-pages","Party Pages","Party","A NIGHT TO KEEP",0,0xFF32253A,0xFFEAD6E9),
 ("gaming-status","Gaming Status","Gaming","PLAYER ONE",5,0xFF141E24,0xFFAEF0D6),
]
def signed(n):return n if n<2**31 else n-2**32
def text(key,content,x,y,w,h,color,size=66,family="sans-serif",weight=400):
 return {"id":key,"type":"text","content":content,"transform":{"x":x,"y":y,"width":w,"height":h},"typography":{"size":size,"family":family,"weight":weight,"color":signed(color),"alignment":"center"}}
def shape(key,x,y,w,h,color,kind="rect"):
 return {"id":key,"type":"shape","shape":kind,"color":signed(color),"transform":{"x":x,"y":y,"width":w,"height":h}}
def photo(key,x,y,w,h,r=0):
 return {"id":key,"type":"photo","placeholder":True,"transform":{"x":x,"y":y,"width":w,"height":h,"rotation":r}}
values=[]
for idx,(key,name,cat,title,layout,bg,fg) in enumerate(rows):
 layers=[]
 if layout==0:
  layers += [shape("paper-1",78,280,820,660,0xFFF6F0E5),photo("PHOTO_1",100,310,775,545,-4),shape("paper-2",190,935,790,665,0xFFF6F0E5),photo("PHOTO_2",215,965,740,535,4)]
 elif layout==1:
  layers += [photo("PHOTO_1",90,300,900,580),photo("PHOTO_2",90,915,900,580)]
 elif layout==2:
  layers += [photo("PHOTO_1",60,260,620,1060),photo("PHOTO_2",700,550,320,480),photo("PHOTO_3",450,1340,570,320)]
 elif layout==3:
  for i in range(4): layers.append(photo(f"PHOTO_{i+1}",70+(i%2)*480,330+(i//2)*625,460,600))
 elif layout==4:
  layers += [photo("PHOTO_1",70,340,940,1180)]
 else:
  layers += [photo("PHOTO_1",150,350,780,780),text("SONG_TITLE","Song title",120,1180,840,100,fg,60,weight=700),text("ARTIST","Artist / your words",120,1290,840,80,fg,38),shape("PROGRESS",160,1430,760,12,fg,"line"),shape("PLAY",495,1510,90,90,fg,"arrow")]
 layers += [text("TITLE",title,80,135 if layout!=4 else 180,920,145,fg,62 if len(title)>17 else 76,"serif" if idx%3==0 else "sans-serif",700 if idx%2 else 400),text("DATE","05 · 10 · 2026",80,1710,920,100,fg,32,"monospace")]
 if cat in ("Couple","Romantic","Scrapbook","Party"):
  layers.append(text("STICKER","♥" if cat in ("Couple","Romantic") else "✦",880,1610,100,110,fg,85,"serif"))
 values.append({"schemaVersion":1,"meta":{"id":key,"name":name,"category":cat,"version":1,"tags":[cat.lower(),"story","minimal" if layout==4 else "memories"]},"width":1080,"height":1920,"background":signed(bg),"layers":layers})
# Twenty-two additional original layouts with purpose-specific geometry, typography and gradients.
extra=[('Cinema Letter','Cinematic'),('Pocket Diary','Digicam'),('Love Letter','Couple'),('Soft Hearts','Romantic'),('Destination Index','Travel'),('Weekend Contact','Photo Dump'),('Training Log','Gym'),('Album Notes','Music'),('Birthday Letter','Birthday'),('Quiet Border','Minimal'),('Analog Index','Film'),('Instant Stack','Polaroid'),('Cover Lines','Magazine'),('Chrome Card','Y2K'),('Dark Window','Dark'),('Torn Notes','Scrapbook'),('Night Window','Night'),('Memory Tiles','Memories'),('Editorial Columns','Editorial'),('Pastel Shapes','Aesthetic'),('Party Contact','Party'),('Player Card','Gaming')]
for i,(name,category) in enumerate(extra):
 count=1+i%4;gap=28+(i%3)*12;top=300+(i%4)*35;height=1150
 layers=[]
 if count==1: layers=[photo('PHOTO_1',100,top,880,height,(-3 if i%2 else 0))]
 elif count==2: layers=[photo('PHOTO_1',65,top,580,height),photo('PHOTO_2',670,top+height*.25,345,height*.65,4)]
 elif count==3: layers=[photo('PHOTO_1',80,top,920,610),photo('PHOTO_2',80,top+650,440,480,-2),photo('PHOTO_3',550,top+650,450,480,2)]
 else:
  layers=[photo('PHOTO_'+str(k+1),75+(k%2)*(465+gap),top+(k//2)*(525+gap),440-gap/2,500,(-2+k) if category in ('Scrapbook','Party') else 0) for k in range(4)]
 fg=0xFFF3EBDF if i%2==0 else 0xFF25232A;bg=0xFF202329 if i%2==0 else 0xFFF0E8DC
 layers += [text('TITLE',name.upper(),75,100,930,180,fg,64,'serif' if category in ('Romantic','Editorial','Magazine') else 'sans-serif-condensed',700),text('NOTE','your words / your story',85,1660,910,150,fg,36,'monospace')]
 if category in ('Y2K','Aesthetic','Romantic','Music'):
  layers[0]['appearance']={'cornerRadius':32};layers[-2]['typography']['gradient']={'colors':[signed(0xFFB7A9F5),signed(0xFFEA8EA8)],'angle':0}
 gradient={'colors':[signed(bg),signed(0xFF52685C if i%2==0 else 0xFFE6C9D4)],'angle':45+(i%3)*45}
 values.append({'meta':{'id':'pro-template-'+str(i),'name':name,'category':category,'tags':[category.lower(),'story','editable','night' if i%2==0 else 'day']},'width':1080,'height':1920,'background':signed(bg),'gradient':gradient,'layers':layers})
# Forty-eight authored Story compositions. Each row defines actual photo geometry,
# rather than changing names/colors on the same few layouts.
collections=[
 ('Photo Dump',0xFF171B20,0xFFF3EEE5,0xFFF0AB83,'sans-serif-condensed',[
  ('Dump do fim de semana','LIFE LATELY',[(72,365,590,700,-2),(690,400,310,360,3),(690,795,310,390,-3),(110,1210,850,360,0)]),
  ('Seis momentos','UM POUCO DE TUDO',[(70+(i%3)*320,375+(i//3)*575,300,540,0) for i in range(6)]),
  ('Meu rolo de câmera','COISAS QUE FICARAM',[(70,355,420,590,0),(520,355,490,360,0),(520,745,490,580,0),(70,975,420,600,0),(520,1355,490,220,0)]),
  ('Dump em tiras','DIAS ASSIM.',[(75,350+i*300,930,275,0) for i in range(4)]),]),
 ('Travel',0xFFEAE4D5,0xFF334B40,0xFFBC6A45,'serif',[
  ('Passaporte aberto','PRÓXIMA PARADA',[(90,390,560,1080,0),(690,445,300,430,-3),(690,980,300,430,3)]),
  ('Cartão de viagem','LONGE DA ROTINA',[(65,410,950,870,0),(130,1330,380,270,-3),(570,1330,380,270,3)]),
  ('Rota de três dias','VAMOS POR AÍ',[(80,380,920,370,0),(80,800,920,370,0),(80,1220,920,370,0)]),
  ('Janela para o mundo','UM LUGAR NOVO',[(135,390,810,1050,0),(675,1200,300,390,4)]),]),
 ('Couple',0xFFF0E0DE,0xFF79434A,0xFFB96371,'serif',[
  ('Nós dois','FICA MAIS UM POUCO',[(100,425,560,760,-5),(410,925,560,660,5)]),
  ('Carta para você','MEU LUGAR FAVORITO',[(155,440,770,820,0),(100,1300,360,280,-4),(610,1300,360,280,4)]),
  ('Antes e agora','A GENTE, SEMPRE',[(85,420,440,980,-2),(555,420,440,980,2)]),
  ('Quatro encontros','NOSSO PEQUENO MUNDO',[(120+(i%2)*440,410+(i//2)*580,400,540,(-3 if i%2==0 else 3)) for i in range(4)]),]),
 ('Music',0xFF191C2B,0xFFE8E1F7,0xFFB7A9F5,'sans-serif',[
  ('Minha trilha sonora','ESSA É A MINHA VIBE',[(190,400,700,700,0)]),
  ('Lado A lado B','EM REPEAT',[(85,425,440,570,0),(555,425,440,570,0)]),
  ('Disco do dia','SOM PARA ESSE DIA',[(150,390,780,780,0),(720,1290,250,250,0)]),
  ('Noite de show','EU ESTAVA LÁ',[(75,395,930,770,0),(75,1210,450,355,-2),(555,1210,450,355,2)]),]),
 ('Birthday',0xFFF7EAD1,0xFF795639,0xFFCA8E60,'serif',[
  ('Meu novo ciclo','MAIS UM ANO DE MIM',[(150,405,780,1030,0)]),
  ('Parabéns pra você','QUE SEJA LINDO',[(70,430,440,640,-4),(570,430,440,640,4),(275,1130,530,440,0)]),
  ('Festa em quadrinhos','HOJE É DIA DE FESTA',[(90,400,560,510,0),(690,400,300,510,0),(90,950,900,600,0)]),
  ('Desejos e memórias','UM BRINDE À VIDA',[(100,400,380,420,-5),(610,480,360,440,5),(270,1020,560,580,-2)]),]),
 ('Gym',0xFF14191A,0xFFDDF1C6,0xFFB5D685,'sans-serif-condensed',[
  ('Mais uma série','UM DIA DE CADA VEZ',[(75,420,930,1110,0)]),
  ('Evolução em foco','CONFIA NO PROCESSO',[(75,420,455,990,0),(550,420,455,990,0)]),
  ('Meu treino','FORÇA / FOCO / ROTINA',[(70,390,940,650,0),(70,1080,290,480,0),(395,1080,290,480,0),(720,1080,290,480,0)]),
  ('Fim do treino','EU FUI POR MIM',[(150,435,780,740,0),(90,1270,420,290,-2),(570,1270,420,290,2)]),]),
 ('Minimal',0xFFF4F1EB,0xFF272B29,0xFFAA9A85,'serif',[
  ('Respiro','SEM PRESSA.',[(175,460,730,880,0)]),
  ('Díptico calmo','ENTRE UM DIA E OUTRO',[(155,450,770,440,0),(155,985,770,440,0)]),
  ('Pequenas coisas','O QUE IMPORTA',[(140,410,800,650,0),(350,1210,380,330,0)]),
  ('Silêncio bonito','LEVE.',[(250,395,580,1020,0)]),]),
 ('Polaroid',0xFFDCD4C8,0xFF51443A,0xFFAB7F60,'cursive',[
  ('Instantâneo','GUARDA ESSE DIA',[(140,450,800,990,-3)]),
  ('Duas lembranças','NA MESMA PÁGINA',[(90,400,590,680,-6),(395,1010,590,610,5)]),
  ('Três instantes','LEMBRA DISSO?',[(100,400,420,530,-4),(565,535,410,520,5),(295,1090,490,510,-2)]),
  ('Parede de fotos','MOMENTOS SOLTOS',[(80,410,440,550,-3),(560,430,440,550,3),(130,1020,820,575,0)]),]),
 ('Magazine',0xFFECE7DC,0xFF212A29,0xFFBF6C48,'sans-serif-condensed',[
  ('Capa pessoal','THE EVERYDAY ISSUE',[(80,400,920,1180,0)]),
  ('Editorial de domingo','DOMINGO EM PÁGINAS',[(75,425,580,1100,0),(695,425,310,410,0),(695,890,310,450,0)]),
  ('Look do dia','MEU JEITO DE VESTIR',[(95,410,650,1010,0),(790,790,195,520,0),(530,1460,455,180,0)]),
  ('Índice visual','COMO EU VEJO O MUNDO',[(80,390,380,490,0),(505,390,495,690,0),(80,925,380,670,0),(505,1125,495,470,0)]),]),
 ('Y2K',0xFF25213A,0xFFEAE4FB,0xFFCE9DF2,'sans-serif',[
  ('Digital crush','ONLINE IN MY WORLD',[(150,440,780,880,0),(660,1250,310,330,4)]),
  ('Galeria 2004','MEU ARQUIVO DIGITAL',[(90,430,900,550,0),(90,1040,440,510,0),(565,1040,425,510,0)]),
  ('Modo internet','DREAM / SAVE / REPEAT',[(75,450,420,1080,0),(550,450,455,480,0),(550,995,455,535,0)]),
  ('Tela de memórias','SOFT SIGNAL',[(110,430,860,780,0),(170,1290,330,300,-3),(590,1290,330,300,3)]),]),
 ('Scrapbook',0xFFE4D6BA,0xFF6D5039,0xFFB86F4C,'cursive',[
  ('Página de lembranças','RECORTES DE MIM',[(90,420,550,700,-4),(680,470,310,410,5),(420,1180,550,410,3)]),
  ('Bilhetes e fotos','NÃO QUERO ESQUECER',[(180,445,720,750,-2),(100,1250,390,330,4),(600,1270,370,310,-4)]),
  ('Coleção de dias','ENTRE PAPÉIS E DIAS',[(75,410,390,510,4),(520,450,480,620,-4),(130,1010,340,570,-2),(550,1170,405,415,3)]),
  ('Diário em recortes','GUARDEI AQUI.',[(140,400,800,520,-2),(110,1000,530,560,3),(715,1040,270,490,-4)]),]),
 ('Diary',0xFFE8E4DA,0xFF3D4840,0xFFAA805D,'monospace',[
  ('Café e calma','MEU CANTINHO DO DIA',[(100,430,880,740,0),(100,1270,410,310,0),(570,1270,410,310,0)]),
  ('Bom dia','HOJE COMEÇA ASSIM',[(185,460,710,970,0)]),
  ('Resumo do dia','FOI UM DIA BOM',[(85,410,470,470,0),(585,410,410,740,0),(85,930,470,630,0),(585,1210,410,350,0)]),
  ('Pequeno diário','UM DIA PARA GUARDAR',[(105,445,870,470,0),(105,1000,540,560,0),(700,1040,275,440,0)]),]),
]
for group_index,(category,bg,fg,accent,family,entries) in enumerate(collections):
 for variant,(name,title,frames) in enumerate(entries):
  key=f'social-template-{group_index+1:02d}-{variant+1:02d}'
  layers=[shape('HEADER_RULE',80,345,920,8,accent,'line')]
  if category=='Y2K':
   layers += [shape('BUBBLE',900,70,105,105,accent,'circle'),shape('WINDOW_BAR',70,385,940,34,accent)]
  if category=='Scrapbook':
   layers += [shape('NOTE_PAPER',75,370,930,1270,0xFFF5EDD9)]
  for i,(x,y,w,h,rotation) in enumerate(frames):
   layer=photo(f'PHOTO_{i+1}',x,y,w,h,rotation)
   layer['appearance']={'cornerRadius':36 if category in ('Y2K','Birthday','Diary') else 0,
     'border':22 if category in ('Polaroid','Scrapbook') else 10 if category=='Couple' else 0,
     'bottomBorder':105 if category=='Polaroid' else 26 if category=='Scrapbook' else 0,
     'borderColor':signed(0xFFFAF6EC),'shadow':12 if category in ('Polaroid','Scrapbook','Couple') else 0}
   layers.append(layer)
  layers += [text('TITLE',title,80,170,920,148,fg,62 if len(title)>21 or family=='serif' and len(title)>17 else 76,family,700 if family in ('sans-serif','sans-serif-condensed') else 400),
             text('CAPTION','sua história, do seu jeito.',90,1690,900,110,fg,32,'monospace')]
  if category=='Music' and variant<3:
   layers += [text('TRACK','Nome da música',110,1200,530 if variant==2 else 860,90,fg,52,'sans-serif',700),
     text('ARTIST','Artista / sua trilha sonora',110,1310,530 if variant==2 else 860,75,fg,30),shape('TRACK_PROGRESS',140,1455,800,8,accent,'line')]
  if category=='Couple':layers.append(text('HEART','♡',480,1575,120,100,accent,70,'serif'))
  if category=='Birthday':layers.append(text('SPARK','✦',900,75,80,80,accent,60,'serif'))
  if category=='Y2K':layers[-2]['typography']['gradient']={'colors':[signed(fg),signed(accent)],'angle':0}
  tags=[category.lower(),'story','instagram','novo','editável','social',name.lower()]
  tags += {'Photo Dump':['dump','momentos','fim de semana'],'Travel':['viagem','viagens','ferias'],'Couple':['casal','amor','namoro'],
    'Music':['musica','playlist','show'],'Birthday':['aniversario','festa'],'Gym':['treino','academia','fitness'],
    'Minimal':['minimalista','limpo'],'Polaroid':['instantanea','analogico'],'Magazine':['revista','moda','editorial'],
    'Y2K':['anos 2000','digital'],'Scrapbook':['recortes','papel','diario'],'Diary':['diario','cafe','rotina']}[category]
  values.append({'schemaVersion':1,'meta':{'id':key,'name':name,'category':category,'tags':tags},
    'width':1080,'height':1920,'background':signed(bg),'layers':layers})

assert len(values)==90
target=Path(__file__).resolve().parents[1]/"app/src/main/assets/templates/stories.json"
target.parent.mkdir(parents=True,exist_ok=True)
target.write_text(json.dumps(values,ensure_ascii=False,indent=2),encoding="utf-8")
print(f"Created {len(values)} editable templates.")
