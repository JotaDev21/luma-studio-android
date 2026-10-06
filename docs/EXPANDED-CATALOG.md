# Catálogo expandido — 1.3.0

48 templates, 48 filtros e 48 estilos novos. Totais: 90 templates / 320 filtros / 72 estilos. Todo o catálogo é local e editável, sem novos downloads ou recursos externos. Geradores em tools/generate_templates.py, generate_presets.py e generate_styles.py.

## Templates

48 composições com geometria própria em 12 famílias, quatro por família: Photo Dump, Viagem, Casal, Música, Aniversário, Treino, Minimal, Polaroid, Revista, Y2K, Scrapbook e Diário. Uma a seis fotos, textos em português editáveis, bordas, cantos, sombra e decoração. Cada retângulo de foto é um placeholder independente; nenhuma composição é achatada.

Exemplos: Seis momentos (grade de seis fotos), Passaporte aberto (foto vertical e dois recortes), Carta para você (carta com foto principal e dois detalhes), Minha trilha sonora (capa e texto musical), Capa pessoal (página de revista), Página de lembranças (papel e recortes inclinados).

Novos filtra os 48 itens desta atualização. Todos inclui os anteriores e os pessoais/importados. Categorias localizadas e busca por palavras sem acentos. Favoritos e Meus templates continuam acessíveis. Miniaturas são renderizadas pelo compositor do app, com placeholders; a seleção abre um projeto com camadas editáveis.

## Filtros inspirados no Instagram

12 famílias: Clarendon, Juno, Lark, Gingham, Aden, Valencia, Ludwig, Moon, Oslo, Paris, Rio e Tokyo. Cada família oferece Classic, Soft, Pop e Matte com curvas RGB/canais, HSL, gradação, resposta de luz e textura próprias. Intensidade inicial 85%, ajustável no editor.

São aproximações criativas de estética social, não cópias garantidas de presets oficiais, nem integração com Instagram/Meta. Tratam cor e textura; não modificam feições via AR. Moon e Tokyo são monocromáticos. As miniaturas usam sua foto e os controles atuais. Nos Stories, a busca e a coleção Instagram selecionam os mesmos presets que as fotos.

## Estilos

48 combinações em Social, Hora dourada, Editorial, Romance, Viagem, Noite, Minimal, Revista, Y2K, Papel, Playlist e Instantâneo. Cada família tem Natural e Intenso para foto, Legenda e Assinatura com elementos editáveis. Cada combinação guarda filtro, intensidade, ajustes e dois efeitos em ordem. Estilos gráficos incluem quatro ou seis camadas de texto/forma, escaladas ao canvas de destino.

Explorar estilos abre uma galeria com busca, favoritos, categorias e filtros Só foto / Com elementos. A aba Style Lab usa as mesmas opções antes de escolher a imagem. Só foto significa que não adiciona camadas de texto/forma: alguns efeitos ainda criam bordas ou texturas. Aplicar um estilo gráfico em uma foto cria um Story editável; trocar de estilo substitui suas próprias camadas e preserva fotos, recortes, máscaras e elementos manuais.

## Compatibilidade e imagens de teste

As definições anteriores (272 filtros, 42 templates, 24 estilos) foram comparadas por igualdade de JSON com o APK 1.2.1 antes da atualização. O gerador de estilos preserva os parâmetros originais embutidos de efeitos Pro, mesmo quando o catálogo separado de efeitos tem defaults diferentes. IDs dos conteúdos antigos e formato dos projetos permanecem.

Os testes usam o retrato NASA de Christina Koch, com origem/crédito em app/src/androidTest/assets/README.md. As imagens de comparação são renderizadas pelo app no Samsung de teste; não são imagens geradas por IA. A fixture NASA fica somente no APK de instrumentação, fora da aplicação pessoal. Verificação da execução em VERIFICATION.md.


## Prévia e verificação

- Galerias reais: expanded-template-gallery.png, expanded-instagram-gallery.png e expanded-style-browser.png.
- Story com seis fotos: expanded-story-editor.png.
- Composições preenchidas: new-story-templates-1.png a new-story-templates-6.png (todas as 48).
- Comparação em retrato: instagram-portrait-looks.png e new-styles-portrait.png.

Os 48 templates têm geometrias de foto distintas, além da tipografia e da decoração. Os 48 filtros e 48 estilos renderizaram resultados distintos no aparelho. Validação de textos mede altura real de StaticLayout nos novos presets; o resultado de uma foto em moldura depende do crop escolhido pelo usuário.
