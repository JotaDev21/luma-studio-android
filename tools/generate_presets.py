"""Build the original, data-only starter collection. No external assets or LUTs."""
import json
from pathlib import Path

# id, name, category, exposure, contrast, saturation, temperature, tint, fade,
# shadows, highlights, vignette, shadow RGB, highlight RGB, grading
rows = [
 ("amber-35","Amber 35","Film",.12,1.12,.90,.28,.02,.22,.12,-.20,.12,[.12,.02,-.12],[.20,.10,-.08],.35),
 ("soft-negative","Soft Negative","Film",.08,.90,.82,.08,-.08,.32,.22,-.28,.05,[.02,.12,.10],[.12,.04,-.02],.28),
 ("expired-summer","Expired Summer","Film",.18,.94,.78,.40,.14,.38,.24,-.16,.18,[.18,-.08,.12],[.28,.12,-.16],.38),
 ("instant-cream","Instant Cream","Film",.20,.88,.76,.30,.02,.25,.12,-.38,.10,[.14,.10,.04],[.14,.10,-.06],.30),
 ("slide-day","Slide Day","Film",-.04,1.28,1.16,.10,-.04,.04,-.12,-.12,.14,[-.08,.08,.14],[.12,.06,-.04],.20),
 ("olive-analog","Olive Analog","Film",-.10,1.08,.75,.18,-.22,.24,.14,-.26,.18,[.04,.18,-.12],[.10,.08,-.10],.32),
 ("ccd-night","CCD Night","CCD",-.08,1.22,.84,-.32,.08,.06,-.12,-.08,.24,[-.12,.10,.30],[.16,.08,-.12],.42),
 ("compact-flash","Compact Flash","CCD",.25,1.30,.92,-.06,-.04,.02,-.22,.16,.16,[-.06,.08,.10],[.04,.02,.02],.18),
 ("vacation-07","Vacation 07","CCD",.16,1.16,1.14,.26,-.10,.08,.08,-.12,.08,[-.04,.12,.18],[.18,.10,-.10],.24),
 ("blue-hour","Blue Hour","CCD",-.12,1.08,.80,-.42,.08,.12,.12,-.16,.18,[-.08,.02,.26],[.08,.04,.16],.36),
 ("party-pocket","Party Pocket","CCD",.18,1.36,1.12,-.14,.18,.06,-.20,.04,.22,[.12,-.08,.22],[.14,.08,-.02],.30),
 ("teal-story","Teal Story","Cinema",-.06,1.20,.84,.12,-.02,.08,.08,-.24,.24,[-.20,.18,.22],[.28,.10,-.16],.62),
 ("neo-night","Neo Night","Cinema",-.24,1.32,.72,-.26,.22,.10,-.08,-.22,.30,[-.10,.08,.28],[.30,-.10,.08],.58),
 ("warm-screen","Warm Screen","Cinema",.06,1.10,.82,.34,.04,.16,.16,-.30,.16,[-.06,.08,.10],[.28,.14,-.14],.42),
 ("silver-bleach","Silver Bleach","Cinema",-.08,1.42,.42,-.10,-.02,.06,-.12,-.12,.18,[-.06,.08,.12],[.04,.06,.08],.20),
 ("quiet-cinema","Quiet Cinema","Cinema",.10,.96,.66,.04,-.06,.26,.22,-.28,.18,[-.06,.08,.08],[.16,.12,.04],.28),
 ("clean-day","Clean Day","IG",.18,1.04,.92,.06,0,.04,.08,-.14,0,[0,0,0],[0,0,0],0),
 ("golden-note","Golden Note","IG",.16,1.12,1.06,.42,.04,.10,.08,-.24,.08,[.10,.04,-.10],[.28,.16,-.16],.36),
 ("pastel-cloud","Pastel Cloud","IG",.28,.82,.68,.10,.10,.28,.22,-.32,.04,[.08,.10,.16],[.16,.08,.10],.26),
 ("urban-moody","Urban Moody","IG",-.22,1.22,.70,-.14,-.08,.12,-.02,-.24,.24,[-.04,.12,.14],[.10,.04,-.04],.30),
 ("coastal-air","Coastal Air","IG",.14,1.02,.94,-.18,-.08,.10,.16,-.14,.04,[-.10,.12,.20],[.12,.10,-.04],.24),
 ("pink-signal","Pink Signal","Y2K",.16,1.14,1.10,-.10,.32,.14,.04,-.12,.12,[.22,-.12,.24],[.24,.04,.10],.50),
 ("chrome-blue","Chrome Blue","Y2K",.08,1.30,.66,-.38,.02,.08,-.12,-.08,.14,[-.10,.12,.32],[.10,.08,.20],.40),
 ("purple-after","Purple After","Y2K",-.12,1.26,.92,-.22,.34,.14,.10,-.20,.26,[.12,-.12,.32],[.22,.08,.16],.44),
 ("natural-face","Natural Face","Portrait",.12,1.02,.90,.12,.02,.04,.14,-.22,.06,[.04,.02,0],[.08,.04,-.02],.12),
 ("editorial-skin","Editorial Skin","Portrait",.08,1.18,.76,.08,-.02,.08,.08,-.18,.12,[-.04,.04,.06],[.12,.06,-.04],.20),
 ("sunlit-portrait","Sunlit Portrait","Portrait",.22,.98,.88,.32,.06,.12,.18,-.28,.08,[.08,.02,-.06],[.20,.10,-.08],.28),
 ("mono-classic","Mono Classic","BW",.04,1.18,0,0,0,.06,.12,-.16,.12,[0,0,0],[0,0,0],0),
 ("mono-paper","Mono Paper","BW",.14,.90,0,0,0,.34,.24,-.28,.12,[0,0,0],[0,0,0],0),
 ("mono-noir","Mono Noir","BW",-.14,1.52,0,0,0,.04,-.14,-.06,.28,[0,0,0],[0,0,0],0),
]
presets = []
for i,row in enumerate(rows):
    key,name,cat,e,c,s,t,ti,f,sh,hi,v,shadow,highlight,grade = row
    # Original tonal response per preset, not just a colored overlay.
    lift = round(f * .12, 4)
    toe = round(max(.01, .18 + sh * .04 - (c-1)*.10), 4)
    shoulder = round(min(.98, .80 + hi * .06 + (c-1)*.06), 4)
    curve = [{"x":0,"y":lift},{"x":.2,"y":toe},{"x":.5,"y":round(.5+e*.025,4)},{"x":.8,"y":shoulder},{"x":1,"y":round(1-f*.04,4)}]
    tags = [cat.lower(), "warm" if t>.1 else "cold" if t<-.1 else "neutral"]
    if "night" in key or "after" in key or "noir" in key: tags += ["night","dark"]
    if cat == "Film": tags += ["retro","analog"]
    if cat == "CCD": tags += ["2000s","digicam"]
    presets.append({"schemaVersion":1,"meta":{"id":key,"name":name,"category":cat,"version":1,"tags":tags},
        "adjustments":{"exposure":e,"contrast":c,"saturation":s,"temperature":t,"tint":ti,"fade":f,"shadows":sh,"highlights":hi,"vignette":v},
        "curves":{"rgb":curve},"shadowTint":shadow,"highlightTint":highlight,"grading":grade})
import copy
base=list(presets)
for original in base:
    for scene,exposure,temp,contrast in [('Window',.12,-.07,-.08),('Sunset',-.08,.15,.07),('Night',.2,-.12,.12)]:
        value=copy.deepcopy(original);key=original['meta']['id']+'-'+scene.lower()
        value['meta'].update(id=key,name=original['meta']['name']+' / '+scene,tags=original['meta']['tags']+[scene.lower(),'lighting'])
        a=value['adjustments'];a.update(exposure=round(a['exposure']+exposure,3),temperature=round(a['temperature']+temp,3),contrast=round(a['contrast']+contrast,3),clarity=.08 if scene=='Night' else -.05,denoise=.24 if scene=='Night' else 0,texture=.07 if scene=='Sunset' else -.06)
        value['color']={'rolloff':.25 if scene=='Sunset' else .12,'shadows':[0,.06,.12] if scene=='Night' else [.05,.02,-.03], 'hsl':[{'hue':0,'saturation':-.12 if i in (2,3) else 0,'luminance':.08 if i==1 else 0} for i in range(8)]}
        value['curves']['rgb'][2]['y']=round(.5+exposure*.12,4)
        presets.append(value)
# Twelve authored color families. Channel curves, selective HSL and tonal grading
# distinguish these from the lighting variants above; no downloaded LUTs/assets.
# family, contrast, saturation, warmth, black lift, shadow RGB, highlight RGB, names
families = [
 ('Editorial',1.12,.83,.05,.012,[-.03,.01,.04],[.06,.02,-.02],['Capa','Atelier','Linho','Galeria','Minimal','Pele de Luz','Chá Branco','Museu','Estúdio','Alabastro']),
 ('Vintage',.98,.76,.18,.055,[.07,.02,-.04],[.13,.06,-.07],['Memória 76','Postal','Arquivo','Veludo','Tarde Antiga','Café de Rua','Caramelo','Álbum','Polaroid Rosa','Domingo 89']),
 ('Nature',1.08,1.08,.04,.005,[-.03,.05,.01],[.03,.05,-.04],['Bosque','Folhagem','Jardim','Serra','Orvalho','Mata Atlântica','Eucalipto','Chuva Verde','Trilha','Campo Aberto']),
 ('Neon',1.23,1.13,-.15,.009,[-.08,.04,.15],[.13,-.04,.09],['Neon Rosa','Arcade','Tóquio Azul','Laser','Roxo Elétrico','Cyber Noite','Sinal Vermelho','Pista','Clube 03','Metrô']),
 ('Matte',.94,.73,.01,.09,[.03,.03,.05],[.04,.03,.01],['Papel Cru','Fumaça','Algodão','Giz','Cinza Quente','Areia Fosca','Tinta Seca','Suede','Porcelana','Silêncio']),
 ('Soft',.93,.83,.12,.024,[.03,.02,.05],[.08,.05,.03],['Nuvem Rosa','Brisa','Pétala','Seda','Sonho','Amanhecer','Pêssego','Leve','Luz de Janela','Creme']),
 ('Golden',1.06,.96,.31,.012,[.07,.02,-.06],[.15,.08,-.10],['Sol de Mel','Duna','Âmbar','Hora Dourada','Cobre','Girassol','Verão Quente','Açafrão','Luz de Outubro','Último Sol']),
 ('Cool',1.09,.85,-.27,.016,[-.06,.04,.13],[.01,.03,.07],['Gelo','Aurora Azul','Inverno','Névoa','Cristal','Azul Mineral','Ar Frio','Geada','Sombra Azul','Montanha']),
 ('Noir',1.31,.12,-.04,.003,[-.03,.02,.04],[.02,.02,.03],['Noir 01','Prata Escura','Chuva na Cidade','Grafite','Cimento','Meia-noite','Janela Sombria','Carvão','Sombra Densa','Última Cena']),
 ('Coastal',1.01,.98,-.08,.018,[-.05,.05,.10],[.08,.06,-.03],['Maré','Sal','Oceano','Praia Clara','Água Verde','Espuma','Céu de Sal','Coral','Ilha','Azul de Verão']),
 ('Urban',1.18,.71,-.07,.019,[-.03,.03,.06],[.06,.03,-.02],['Asfalto','Concreto','Rua 24','Vidro','Passagem','Muro','Terminal','Chuva Urbana','Arquitetura','Centro']),
 ('Nordic',1.02,.68,-.10,.037,[-.02,.04,.06],[.04,.04,.02],['Luz Nórdica','Madeira Clara','Fjord','Céu Pálido','Casa Branca','Sálvia','Pedra','Lago Quieto','Vento Norte','Manhã Fria']),
]
# Each look has its own tonal silhouette and color relationships, not just EV.
variants = [
 (0,0,0,0,0),(.07,-.05,.09,.04,.018),(-.09,.10,-.08,-.04,-.003),
 (.02,.04,.13,-.08,.01),(-.03,-.07,-.16,.08,.03),(.11,.02,-.05,-.02,.006),
 (-.08,.07,.06,.11,.015),(.04,-.03,-.12,-.10,.024),(-.12,.15,.02,.05,-.009),(.09,-.10,.10,-.05,.034),
]
def number(v): return round(v,4)
def curve(points): return [{'x':x,'y':number(max(0,min(1,y)))} for x,y in zip([0,.2,.5,.8,1],points)]
for family_index,(cat,c,s,t,lift,shadow,highlight,names) in enumerate(families):
    for i,(name,(ev,dc,ds,dt,dl)) in enumerate(zip(names,variants)):
        contrast=c+dc; warmth=t+dt; black=max(0,lift+dl)
        hue=(i-4.5)*.022; chroma=.016+(i%4)*.008
        rgb=curve([black,.19-(contrast-1)*.13+black*.4,.5+ev*.12,.81+(contrast-1)*.08,1-black*.5])
        red=curve([max(0,black*.4+warmth*.015),.2+warmth*.018+chroma,.5+warmth*.033,.8+warmth*.012,1-max(0,-warmth)*.015])
        green=curve([black*.2,.2+(.5-i/9)*.018,.5+(i%3-1)*.013,.8+(.5-i/9)*.02,1-black*.12])
        blue=curve([max(0,black*.5-warmth*.025),.2-warmth*.025,.5-warmth*.04,.8-warmth*.026,1-max(0,warmth)*.023])
        hsl=[{'hue':0,'saturation':0,'luminance':0} for _ in range(8)]
        hsl[1]={'hue':number(hue*.18),'saturation':number(-.07+ds*.3),'luminance':number(.025+ev*.08)} # skin
        hsl[2]={'hue':number(hue),'saturation':number(-.08+ds*.6),'luminance':number(-.02+ev*.12)}
        hsl[3]={'hue':number(hue+(.10 if cat in ('Nature','Coastal') else -.04)), 'saturation':number((.14 if cat=='Nature' else -.16)+ds*.8),'luminance':number(-.04+ev*.2)}
        hsl[4]={'hue':number(-hue*.7),'saturation':number(.12 if cat in ('Neon','Coastal','Cool') else -.10),'luminance':number(.025-ev*.1)}
        hsl[5]={'hue':number(hue*.65),'saturation':number((.15 if cat in ('Neon','Cool') else -.09)+ds*.5),'luminance':number(-.025+ev*.1)}
        hsl[6]={'hue':number(hue*.8),'saturation':number(.16 if cat=='Neon' else -.12),'luminance':number(ev*.08)}
        key=f'studio-{cat.lower()}-{i+1:02d}'
        value={'schemaVersion':1,'meta':{'id':key,'name':name,'category':cat,'version':1,'tags':[cat.lower(),'coleção','quente' if warmth>.1 else 'frio' if warmth<-.1 else 'neutro','retrato' if cat in ('Editorial','Soft') else 'natureza' if cat=='Nature' else 'cidade' if cat in ('Neon','Urban','Noir') else 'luz']},
         'adjustments':{'exposure':ev,'contrast':number(contrast),'saturation':number(max(0,s+ds)),'temperature':number(warmth),'tint':number(hue*.6),'highlights':number(-.18-(i%3)*.06),'shadows':number(.04+black*.8),'vignette':number(.04+(i%4)*.025),'clarity':-.12 if cat=='Soft' else .10 if cat in ('Urban','Nature') else .02,'texture':-.08 if cat=='Soft' else .07 if cat in ('Urban','Vintage') else 0,'sharpness':.08 if cat in ('Editorial','Nature','Cool') else 0},
         'curves':{'rgb':rgb,'red':red,'green':green,'blue':blue},'grading':.35,'shadowTint':shadow,'highlightTint':highlight,
         'color':{'hsl':hsl,'shadows':[number(v*(.6+i*.035)) for v in shadow],'midtones':[number(warmth*.035),number(hue*.02),number(-warmth*.035)],'highlights':[number(v*.6) for v in highlight],'rolloff':number(.16+(i%4)*.07),'luminance':number(ev*.08)}}
        if cat=='Vintage' or (cat=='Noir' and i in (2,5,8)):
            value['effects']=[{'id':key+'-grain','preset':{'meta':{'id':key+'-grain','name':'Grão fino','category':cat},'type':'grain','parameters':{'amount':.16,'size':.13,'seed':number(.1+i*.06)}},'intensity':.3}]
        presets.append(value)

# Flash 80/90: authored against the user's violet disposable flash, warm olive
# negative, classic green garage film and cyan night flash references. Bright areas stay closer to white; the cast lives mostly in
# shadows/midtones. Flash is a tonal treatment of the existing photo, not relighting.
jdm_profiles = [
 ('flash',1.17,.88,-.06,.025,-.12,.12,.040,.006,.38,.15,
  ['Flash Verde 89','Tokyo Flash','Posto 93','Pocket 90','Estacionamento 88','Garage Flash','Concreto Verde','Compact 95']),
 ('negative',1.05,.72,.13,.075,-.015,-.04,.022,-.065,.50,.09,
  ['Oliva 90','Negativo Verde','Verde Lavado','Foto Revelada','Asfalto 90','Neblina 87','Papel Antigo','Musgo 94']),
 ('night',1.22,.75,-.25,.018,-.19,.16,.014,.075,.58,.23,
  ['Flash Azul 91','Daikoku Night','Osaka Midnight','Teal Garage','Chuva em Tóquio','Kansai Petrol','Midnight Run','Neon Verde']),
 ('chrome',1.03,.62,-.19,.070,.075,.075,-.015,.070,.45,.16,
  ['Flash 95','Câmera 95','Azul Lavado','Arquivo Violeta','Flash de Bolso','CCD 94','Papel Azul','Lilás 90']),
]
# exposure, contrast, green bias, blue bias, black lift, highlight shoulder, grain
jdm_shapes = [(0,0,0,0,0,0,0),(.06,-.07,.011,-.007,.012,-.018,.03),
 (-.08,.05,.017,.004,-.006,.022,.07),(.12,.02,-.005,-.011,.004,.035,-.02),
 (-.04,.10,.009,.015,-.009,-.025,.04),(.02,-.04,.022,-.004,.018,-.012,.06),
 (-.11,-.02,.005,.010,.010,.013,-.04),(.09,.06,.015,.007,-.003,.018,.02)]
for family,c,s,t,black,sh,wh,g,b,grain,glow,names in jdm_profiles:
    for i,(name,(ev,dc,dg,db,dl,dh,dn)) in enumerate(zip(names,jdm_shapes)):
        key=f'jdm-{family}-{i+1:02d}';lift=max(.003,black+dl)
        green=g+dg;blue=b+db
        shadows={'flash':[-.08,.16,.035],'negative':[.018,.105,-.11],
          'night':[-.09,.09,.19],'chrome':[.07,-.01,.17]}[family]
        tone_shadows={'flash':[-.045,.07,.015],'negative':[.022,.055,-.05],
          'night':[-.05,.038,.08],'chrome':[.035,-.01,.07]}[family]
        tone_midtones={'flash':[-.028,.066,.006],'negative':[.026,.04,-.035],
          'night':[-.028,.025,.045],'chrome':[.025,-.02,.065]}[family]
        tone_highlights={'flash':[.025,.025,.012],'negative':[.025,.02,-.012],
          'night':[-.006,.018,.035],'chrome':[.022,.007,.042]}[family]
        balance={'flash':[-.01,.016,-.005],'negative':[.012,.015,-.022],
          'night':[-.016,.008,.025],'chrome':[.01,-.008,.024]}[family]
        rgb=curve([lift,.18-(c+dc-1)*.20+lift*.30,.49+ev*.07,.84+wh*.12+dh,1-lift*.16])
        channels={
          'red':curve([lift*.45,.175-green*.30,.50-green*.70,.81+dh*.30,1]),
          'green':curve([lift*.55+.006,.20+green*.55,.50+green,.80+green*.24+dh*.18,1]),
          'blue':curve([lift*.50+max(0,blue)*.45,.19+blue*.35,.50+blue*.55,.80+blue*.20, .985 if family=='negative' else 1]),
        }
        hsl=[{'hue':0,'saturation':0,'luminance':0} for _ in range(8)]
        hsl[0]={'hue':-.018,'saturation':.04 if family=='flash' else -.06,'luminance':.015}
        hsl[1]={'hue':-.06,'saturation':.035,'luminance':.04} # orange/skin remains usable
        hsl[2]={'hue':number(.16+i*.012),'saturation':-.12,'luminance':-.025}
        hsl[3]={'hue':number(.12+(i%3)*.03),'saturation':number(.06+dg),'luminance':-.04}
        hsl[4]={'hue':-.10,'saturation':.03,'luminance':-.025}
        hsl[5]={'hue':-.12,'saturation':-.17 if family=='chrome' else -.08,'luminance':-.05}
        hsl[6]={'hue':.02,'saturation':-.22,'luminance':-.02}
        value={'schemaVersion':1,'meta':{'id':key,'name':name,'category':'JDM','version':2,
          'tags':['jdm','japão','japones','carro','carros','retrato','retratos','selfie','pessoas','retro','vintage','anos 80','anos 90','1980','1990','flash','verde','filme','camera antiga','descartavel','analógico',{'flash':'verde clássico','negative':'oliva quente','night':'flash azulado','chrome':'flash violeta'}[family],family,'noite' if family=='night' else 'compacta']},
          'adjustments':{'exposure':number(ev+(.10 if family=='flash' else -.06 if family=='night' else 0)),
          'contrast':number(c+dc),'saturation':s,'temperature':t,'tint':.065 if family=='chrome' else -.10 if family=='negative' else -.09 if family=='night' else -.16,
          'shadows':sh,'highlights':-.10 if family=='negative' else .04 if family=='flash' else -.06,
          'whites':number(wh+dh*.6),'blacks':-.055 if family in ('flash','night') else .015,
          'fade':.13 if family=='chrome' else .08 if family=='negative' else .025,'vignette':number(.18+(i%3)*.035),
          'clarity':.025 if family=='flash' else -.025,'texture':-.035 if family=='chrome' else .015,'sharpness':.035 if family=='flash' else 0},
          'savedIntensity':.75,'curves':{'rgb':rgb,**channels},'shadowTint':shadows,'highlightTint':tone_highlights, 'grading':.62,
          'color':{'hsl':hsl,'shadows':tone_shadows,'midtones':tone_midtones,'highlights':tone_highlights,
          'balance':balance,'rolloff':.09 if family in ('flash','night') else .18},
          'effects':[
            {'id':key+'-bloom','preset':{'meta':{'id':key+'-bloom','name':'Luz de flash','category':'JDM'},'type':'bloom','parameters':{'amount':number(glow),'threshold':.70 if family=='flash' else .56 if family=='night' else .74,'radius':.13}},'intensity':.65},
            {'id':key+'-grain','preset':{'meta':{'id':key+'-grain','name':'Grão compacto','category':'JDM'},'type':'grain','parameters':{'amount':number(grain+dn),'size':.27 if family in ('negative','chrome') else .21 if family=='night' else .15,'seed':number(.13+i*.071)}},'intensity':.70},
          ]}
        if family in ('negative','chrome'):
            value['effects'].append({'id':key+'-dust','preset':{'meta':{'id':key+'-dust','name':'Marcas de filme','category':'JDM'},'type':'dust','parameters':{'amount':.24 if family=='negative' else .17,'size':.14,'seed':number(.17+i*.053)}},'intensity':.35})
        presets.append(value)

# Put the four reference-inspired directions together at the beginning of the collection.
existing=presets[:240];retro=presets[240:]
presets=existing+[retro[family*8+variant] for variant in range(8) for family in range(4)]

# Original social-photo looks inspired by familiar Instagram color treatments.
# These are local color presets, not Meta's effects or face-changing AR filters.
# name, exposure, contrast, saturation, temperature, tint, lift, shadow, highlight
social_profiles=[
 ('Clarendon',.10,1.24,1.12,-.10,.015,.009,[-.06,.035,.10],[.06,.025,-.025]),
 ('Juno',.08,1.16,1.17,.15,-.025,.012,[.015,.035,-.04],[.09,.045,-.07]),
 ('Lark',.22,.98,.85,-.06,-.02,.032,[-.025,.04,.05],[.035,.04,.015]),
 ('Gingham',.10,.88,.72,.035,.035,.090,[.055,.03,.045],[.065,.05,.015]),
 ('Aden',.16,.91,.78,.08,.105,.055,[.065,.015,.06],[.07,.035,.04]),
 ('Valencia',.14,1.04,.87,.23,.02,.045,[.045,.025,-.045],[.12,.06,-.075]),
 ('Ludwig',.04,1.13,.78,.10,.005,.012,[.02,.015,-.005],[.065,.03,-.035]),
 ('Moon',.10,1.08,0,0,0,.045,[0,0,0],[0,0,0]),
 ('Oslo',.21,1.06,.96,.015,.01,.016,[-.015,.01,.015],[.035,.025,.02]),
 ('Paris',.19,.95,.84,.075,.045,.032,[.035,.02,.04],[.075,.04,.025]),
 ('Rio',.11,1.09,1.05,.29,-.025,.022,[.04,.035,-.06],[.13,.07,-.085]),
 ('Tokyo',-.015,1.34,0,-.03,0,.008,[0,0,0],[0,0,0]),
]
social_shapes=[('Classic',0,0,0,0,0),('Soft',.07,-.07,-.06,.02,.02),
               ('Pop',-.04,.08,.08,-.025,-.006),('Matte',.025,-.035,-.10,.035,.038)]
for family_index,(name,e,c,s,t,tint,lift,shadow,light) in enumerate(social_profiles):
    for i,(variant,ev,dc,ds,dt,dl) in enumerate(social_shapes):
        key='social-'+name.lower()+'-'+variant.lower();black=max(.002,lift+dl)
        mono=name in ('Moon','Tokyo');warm=t+dt
        hsl=[{'hue':0,'saturation':0,'luminance':0} for _ in range(8)]
        hsl[0]={'hue':-.015,'saturation':.045 if name in ('Juno','Rio') else -.025,'luminance':.015}
        hsl[1]={'hue':-.025,'saturation':-.03,'luminance':.035+ev*.06}
        hsl[2]={'hue':.06 if name in ('Juno','Valencia','Rio') else -.02,'saturation':-.12 if name in ('Ludwig','Paris') else .025,'luminance':.02}
        hsl[3]={'hue':.075 if name in ('Lark','Juno') else -.025,'saturation':-.18 if name=='Ludwig' else .07 if name=='Juno' else -.025,'luminance':.035}
        hsl[5]={'hue':-.05 if name=='Clarendon' else .03,'saturation':.10 if name=='Clarendon' else -.10 if name in ('Paris','Gingham') else -.02,'luminance':-.025 if name=='Clarendon' else .025}
        value={'schemaVersion':1,'meta':{'id':key,'name':name+' '+variant,'category':'Instagram','version':1,
          'tags':['instagram','insta','social','feed','story','selfie','retrato','inspirado',name.lower(),variant.lower(),
            'preto branco pb' if mono else 'dourado quente' if warm>.13 else 'suave claro' if name in ('Lark','Oslo','Paris') else 'cor contraste']},
          'savedIntensity':.85,'adjustments':{'exposure':number(e+ev),'contrast':number(c+dc),'saturation':0 if mono else number(s+ds),
            'temperature':0 if mono else number(warm),'tint':0 if mono else tint,'fade':number(black*.9),'shadows':.12 if name in ('Lark','Gingham','Moon') else .02,
            'highlights':-.16 if name in ('Paris','Aden','Lark') else -.055,'whites':.045,'blacks':-.05 if name in ('Clarendon','Tokyo') else .01,
            'vignette':.03 if name in ('Oslo','Lark') else .10,'clarity':-.045 if variant=='Soft' else .045 if variant=='Pop' else .015,'texture':-.015,'sharpness':.04},
          'curves':{'rgb':curve([black,.19-(c+dc-1)*.16+black*.35,.5+e*.08+ev*.09,.82+(c+dc-1)*.045,1-black*.23]),
            'red':curve([black*.35,.2+warm*.02,.5+warm*.035,.81+warm*.022,1]),
            'green':curve([black*.3,.2+(family_index%3-1)*.008,.5+(i-1.5)*.006,.81+(family_index%2)*.007,1]),
            'blue':curve([black*.45,.2-warm*.025,.5-warm*.04,.81-warm*.022,1-max(0,warm)*.025])},
          'shadowTint':shadow,'highlightTint':light,'grading':0 if mono else .34,
          'color':{'hsl':hsl if not mono else [{'hue':0,'saturation':0,'luminance':0} for _ in range(8)],
            'shadows':shadow,'highlights':light,'rolloff':.20 if variant=='Soft' else .10}}
        # Monochrome uses identical channel curves to avoid reintroducing a cast.
        if mono:
            value['curves']={channel:value['curves']['rgb'] for channel in ('rgb','red','green','blue')}
        if name in ('Gingham','Valencia','Moon') or variant=='Matte':
            value['effects']=[{'id':key+'-grain','preset':{'meta':{'id':key+'-grain','name':'Grão social','category':'Instagram'},'type':'grain','parameters':{'amount':.18,'size':.13,'seed':number(.10+family_index*.057+i*.015)}},'intensity':.34}]
        presets.append(value)

target = Path(__file__).resolve().parents[1] / "app/src/main/assets/filters/presets.json"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps(presets, ensure_ascii=False, indent=2), encoding="utf-8")
assert len(presets) == 320
print(f"Created {len(presets)} original presets.")
