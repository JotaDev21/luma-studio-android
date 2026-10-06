# Flash 80/90 — 1.2.1

32 filtros para fotos pessoais, selfies e retratos, ajustados às quatro imagens fornecidas pelo usuário: verde clássico de filme, negativo oliva quente, flash azul e violeta lavado, com grão e brilho suave. A referência de carros japoneses à noite define a aparência desejada, não o assunto da foto. Na versão 1.2.1 o catálogo total tinha 272 filtros. A expansão para 320 em 1.3.0 está descrita em EXPANDED-CATALOG.md; esta coleção conserva seus 32 looks. A coleção aparece como Flash 80/90 logo depois de Favoritos; JDM é apenas a chave interna estável.

## Variações

- Flash: Flash Verde 89, Tokyo Flash, Posto 93, Pocket 90, Estacionamento 88, Garage Flash, Concreto Verde e Compact 95.
- Negativo: Oliva 90, Negativo Verde, Verde Lavado, Foto Revelada, Asfalto 90, Neblina 87, Papel Antigo e Musgo 94.
- Noite: Flash Azul 91, Daikoku Night, Osaka Midnight, Teal Garage, Chuva em Tóquio, Kansai Petrol, Midnight Run e Neon Verde.
- Violeta: Flash 95, Câmera 95, Azul Lavado, Arquivo Violeta, Flash de Bolso, CCD 94, Papel Azul e Lilás 90.

As quatro direções aparecem intercaladas no começo da coleção. IDs permanecem estáveis; os presets internos passaram à versão 2. Curvas RGB e por canal, HSL seletivo, verde nas sombras/meios-tons, altas luzes mais neutras, vinheta, bloom e grão determinístico variam por look. Os negativos e violetas incluem marcas de filme sutis (dust procedural); não utilizam textura externa. A intensidade inicial é 75% para suavizar o tratamento em retratos; o controle permite 0–100%. A aparência de flash depende da luz existente na foto: não cria iluminação física nova. O HSL trata faixas de cor, não reconhece ou mascara automaticamente a pele. Os 240 filtros anteriores não foram alterados.

## Referências do usuário

| Imagem enviada | Características visuais | Direção do filtro |
| --- | --- | --- |
| 1, flash de 1995 | Sombras azul-violeta, pretos levantados, contraste suave, textura de papel/filme | Flash 95 |
| 2, mirante à noite | Sombras oliva, luz de flash amarelada, desbotamento e poeira | Oliva 90 |
| 3, garagem | Verde clássico, cores de objetos ainda visíveis, altas luzes de flash | Flash Verde 89 |
| 4, rua à noite | Preto mais profundo, flash azul/ciano, grão evidente e luzes brilhantes | Flash Azul 91 |

As fotos enviadas são referências visuais, não assets distribuídos. Não há transferência de estilo por IA nem garantia de reprodução idêntica: a iluminação e as cores da foto original influenciam o resultado. O carimbo de data da primeira referência não foi embutido nos filtros; o efeito timestamp existente continua disponível separadamente.

## Imagens de verificação

Retrato: portrait-nasa.jpg, retrato oficial NASA de Christina Koch, jsc2018e095073 (03/12/2018), com crédito e origem em app/src/androidTest/assets/README.md. A comparação é produzida pelo renderer real do aplicativo no aparelho, a 75%, e não por geração de imagem. Arquivo de comparação: docs/retro-reference-portrait.png. Capturas: docs/retro-reference-editor.png e docs/retro-reference-gallery.png.

A cena de carro foi uma fixture complementar anterior à clarificação de uso em retratos. Arquivo final: app/src/androidTest/assets/jdm-night-car.png. Gerada em 06/10/2026 com o **image_gen integrado**, sem CLI. Imagem fictícia, sem representar um lugar/evento real. Ambos os arquivos estão somente no APK de instrumentação, fora do APK pessoal. Não são usados como branding do aplicativo.

### Prompt final da fixture sintética

Use case: photorealistic-natural. Asset type: synthetic reference photograph for testing a photo editor's filters, NOT a filtered final image. Create a candid realistic vertical 4:5 photo of a silver 1990s Japanese two-door sports coupe parked in a plain Japanese street-side parking area at night. Show the entire car at a front three-quarter angle, low but ordinary camera position. Modest white headlights, small warm parking lamps, some dim building windows, wet dark pavement reflecting lamps. The near side of the silver car is illuminated by direct on-camera compact flash, brighter than the dark background, with crisp shadows and authentic photographic texture. Neutral natural white balance, neutral silver/grey bodywork and concrete, a few dark red accents to evaluate selective color. Keep midtone greys visible rather than pitch black. Clean ungraded photo with restrained saturation. No green/cyan tint, no vintage filter, no film grain, no fake lens flare, no exaggerated cinematic neon, no rendered glossy CGI appearance. No people, readable logos, captions, watermark or collage. This is a fictional scene, not a documentary of a real location.
