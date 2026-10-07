# Luma Studio

Editor pessoal Android em Kotlin + Jetpack Compose, versão 1.5.0: fotos, efeitos, Stories, colagens, biblioteca pessoal, máscaras, enquadramento inteligente e melhoria opcional com IA no PC. Android 10 ou superior. Sem conta ou anúncios; a edição funciona localmente, e o botão de melhoria conecta ao servidor pessoal escolhido.

## Disponível na fase 1

- Photo Picker do Android; cópia privada imutável da imagem original, inclusive orientação EXIF.
- Preview GPU OpenGL ES com resolução reduzida, mesmo shader na exportação.
- Crop livre por arraste dos cantos/retângulo, proporções 9:16, 4:5, 1:1, 1.91:1; girar 90° e espelhar nos dois eixos.
- 19 ajustes: exposição, brilho, contraste, saturação, vibrance, temperatura, tint, highlights, sombras, brancos, pretos, gamma, fade, vinheta, clareza, textura, nitidez, desembaçar e redução de ruído.
- 320 presets originais JSON em 21 coleções; curvas RGB e por canal, HSL seletivo, gradação e textura. Os 120 filtros anteriores permanecem disponíveis.
- Miniaturas calculadas na própria foto e carregadas sob demanda. Paletas da biblioteca são rotuladas e não simulam previews de fotos.
- Comparação ao segurar e dividida com posição ajustável.
- Undo/redo de parâmetros; um gesto gera uma entrada no histórico.
- Projetos locais editáveis, nome, thumbnail e confirmação para salvar/descartar ao sair.
- JPEG, PNG, WebP; resolução original após transformação ou tamanho exato do maior lado escolhido, qualidade, metadados opcionais de câmera/data para JPEG e compartilhamento pelo Android.

## Efeitos e Stories (fases 2 e 3)

- 93 presets de efeitos: Flash, Film, Lens, Digital, Light, Blur, Texture, Overlay e Frame. Até 16 efeitos em sequência, com intensidade, parâmetros, visibilidade, remoção e reordenação.
- Story Maker com canvas 9:16, feed 4:5, quadrado ou paisagem; fundo com transparência e guias de composição opcionais.
- Até 80 camadas de fotos/PNGs, textos, stickers e formas. Selecionar, mover, redimensionar/girar com gestos, duplicar, ocultar, bloquear e reordenar; opacidade e mistura normal/multiply/screen/overlay.
- Fotos com enquadramento Preencher/Foto inteira, zoom e posição ajustáveis. Importação múltipla preenche os espaços do template; exportação exige preencher todas as fotos visíveis.
- Texto multilinha com cinco famílias do Android, peso/tamanho, alinhamento, tracking, entrelinhas, contorno, sombra, cor e fundo. 12 configurações tipográficas iniciais e stickers de símbolos/emojis; stickers PNG próprios entram pelo seletor de fotos.
- 90 templates JSON originais, preview renderizado, busca, categorias e favoritos. Nenhuma composição fica achatada: fotos/textos/formas continuam editáveis.
- Mesmos ajustes, filtros e efeitos de fotos no Story. Templates e Stories manuais compartilham formato, composição e exportação. Projetos de versões anteriores permanecem compatíveis.

## Biblioteca pessoal e colagens (fase 4)

- Colagens com 2–20 fotos em Grid, Livre, Photo Dump, Magazine, Polaroid, Scrapbook ou Film Strip. Controles de espaçamento, borda, cantos arredondados, sombra, inclinação e fundo. Mudar o layout preserva fotos/crops/textos e respeita camadas bloqueadas.
- Salvar como template: escolher quais fotos viram placeholders e quais ficam fixas, marcar textos editáveis e guardar também filtro/ajustes/efeitos. Os templates aparecem em Meus templates e continuam editáveis ao reutilizar.
- Style Lab com 72 combinações originais; salvar a combinação completa de filtro/intensidade, ajustes e stack de efeitos com sua ordem, parâmetros e visibilidade. Textos/stickers/formas opcionais são proporcionados ao canvas de destino. Trocar de estilo substitui os elementos adicionados pelo estilo anterior e preserva elementos manuais.
- Filtros Custom guardam o tratamento e a intensidade escolhida mais os ajustes base, sem achatar os pixels. Estilos guardam também efeitos/elementos. Fotos com elementos gráficos são convertidas para o mesmo formato Story, preservando sua fonte e crop/rotação/flip por camada.
- Importar e exportar packs próprios por arquivos escolhidos no Android. Conteúdo validado, IDs remapeados e commit atômico do catálogo; importação inválida não modifica a biblioteca. Busca/favoritos para filtros, templates, estilos e efeitos.
- Exportar biblioteca inclui apenas conteúdo pessoal/importado, com imagens fixas dos templates. Fotos dos projetos e placeholders não são incluídos. A tela informa essa diferença antes de escolher o destino.

## Máscaras e ajustes locais (início da fase 5)

- Até oito máscaras independentes por foto: pincel com borracha e fluxo, área circular/elíptica e degradê linear. Cada máscara tem nome, intensidade, suavização, inversão, visibilidade e ordem de aplicação.
- Os mesmos 19 ajustes globais podem ser aplicados somente à área da máscara. Os controles de área e cor ficam separados; a área rosa é uma guia opcional do editor, ausente do arquivo exportado.
- Máscaras usam coordenadas da fonte orientada pelo EXIF: acompanham recortes, espelhamento e rotação, preservando a imagem original. Traços são vetores serializados; um traço/gesto equivale a uma entrada no histórico.
- No editor de foto: Máscaras. Em Stories/colagens: selecione uma foto e abra Máscaras da foto. A edição ocorre na foto isolada e Concluir máscaras retorna à composição. Cada foto guarda seus próprios ajustes locais.
- Projetos antigos continuam abrindo; novos salvamentos usam schema 8. Converter foto em Story preserva máscaras. Trocar a foto remove os retoques da fonte anterior. Templates com foto fixa conservam suas máscaras; placeholders descartam os retoques específicos da foto original.
- Filtros e estilos pessoais conservam o tratamento global; máscaras ficam com a foto do projeto. Limites por máscara: 100 traços, 10.000 pontos totais e 2.048 por traço. Exportação com máscaras usa o mesmo pipeline da prévia e respeita o orçamento de memória.

## Pessoa e fundo (0.6.0)

- Em Máscaras, abra Pessoa / fundo: Selecionar pessoa, Selecionar fundo ou Remover fundo. Funciona em fotos simples e nas fotos de Stories/colagens. A seleção pode incluir várias pessoas e objetos próximos; confira as bordas.
- Modelo ML Kit Selfie Segmentation 16.0.0-beta6 incorporado ao APK, em modo de foto única. A inferência é local e não exige download no celular. Desde 1.5.0, o app possui permissão de internet para a melhoria opcional no PC; a segmentação continua local.
- A confiança por pixel é salva junto com o projeto, em mapa de até 256 × 256, quantizado em 8 bits e comprimido com zlib/Base64. Reabrir/exportar não executa novamente o modelo. Pessoa e fundo da mesma fonte reutilizam o resultado inicial e mantêm correções manuais independentes.
- Controles de limite, suavização, inversão, intensidade, pintura e borracha. Para máscaras de fundo, Pintar e Apagar atuam sobre a área selecionada. Limpar correções restaura o resultado automático; Comparar original ajuda a revisar o recorte.
- Remover fundo preserva os pixels da fonte e aplica transparência pelo renderer. PNG/WebP preservam transparência; JPEG preenche com branco. Em Story, a área removida mostra o fundo e as camadas inferiores do canvas. O quadriculado e a marcação rosa existem só na UI.
- O modelo é experimental e focado em retratos; não oferece seleção automática de objetos/animais nem garantia de recorte preciso de cabelo, objetos próximos, oclusões ou cenas difíceis. Fotos sem pessoa com confiança suficiente exibem orientação para usar o pincel, sem adicionar máscara vazia.

Detalhes e fontes oficiais em [docs/SEGMENTATION.md](docs/SEGMENTATION.md).

## Recorte e organização inteligente (0.7.0)

- Foto → Recorte → Recorte inteligente: escolher formato e revisar uma sugestão orientada por rostos/pessoas detectados localmente. Sem detecção, informa o enquadramento central. Ver antes, aplicar ou cancelar; aplicação pode ser desfeita em um passo.
- Story/template/colagem → Inteligente: enquadrar a foto selecionada ou organizar fotos em 2–20 molduras visíveis e desbloqueadas. A organização escolhe os espaços mais compatíveis com fotos verticais, horizontais e quadradas, preservando a geometria do layout e as outras camadas.
- Rostos detectados recebem margem de segurança. Quando todos não cabem na moldura, mostra a foto inteira com margens; em foto simples, conserva o formato original e explica a impossibilidade do recorte solicitado.
- Fonte, rotação/espelhamento e máscaras acompanham a foto ao trocar de espaço. O enquadramento sugerido redefine crop/zoom/posição usando o original, sem alterar seus pixels. Tudo permanece editável.
- Face Detection incorporado; sem download/upload. Projetos schema 8 mantêm compatibilidade com versões anteriores. Templates e packs conservam Foto inteira nas fotos fixas.

Pipeline e limites em [docs/SMART-FRAMING.md](docs/SMART-FRAMING.md).

## Novidades da atualização completa (1.0.0)

- Modo rápido e modo Pro no mesmo editor. Pro → Avançado abre curvas RGB e canais, HSL de oito faixas, balanço, sombras/meios-tons/luzes, luminância, transição de altas luzes e intensidade de LUT.
- Cor Pro, filtros, efeitos e preenchimento em degradê também funcionam em máscaras. Pessoa normal com desfoque de movimento só no fundo, por exemplo.
- Referência: escolha uma foto para sugerir correspondência estatística de cor/contraste. Compare antes, sugestão e referência; aplicar registra um passo de undo. Análise local, sem upload, não recupera o preset usado na referência.
- Importar fontes TTF/OTF, LUT .cube 3D e imagens como stickers, texturas e frames. Os recursos entram em packs próprios e os projetos/templates guardam cópias independentes. Biblioteca permite duplicar/remover presets e remover packs/recursos; Projetos permite exclusão com confirmação.
- Stories: degradê e foto desfocada no fundo; texto com fonte importada, gradiente, brilho, desfoque e curvatura; desenho/borracha em camada própria; card de música com capa da galeria, título/artista/trecho ou tempo/progresso editáveis.
- Alinhamento horizontal/vertical e aproximação magnética ao soltar as camadas; canvas personalizado, perfil e wallpaper.
- Exportação em lote de até 20 fotos, original ou estilo sem elementos gráficos. Cada foto vira projeto editável. Dimensões de Story/feed/quadrado/perfil/wallpaper ou personalizadas, com imagem inteira e margens quando necessário.
- Até 20 estados de desfazer/refazer persistem ao salvar/reabrir. Estilos recentes e Surpreenda-me usam combinações do catálogo original.

## Novo editor de fotos (1.1.0)

- Foto em destaque, painel de edição em grafite, tipografia serifada nos títulos e seleção em pêssego. Exportar no topo; ferramentas separadas do carrossel de looks.
- Cards maiores com a foto real, indicação do look escolhido e coração acessível para favoritos. Explorar looks abre uma galeria em grade com busca por nome/tags/coleção, favoritos e intensidade; a busca aceita nomes sem acento e termos como filme/P&B.
- 120 novos looks em Editorial, Vintage, Nature, Neon, Matte, Soft, Golden, Cool, Noir, Coastal, Urban e Nordic. As respostas de curvas/canais/HSL são próprias; os looks Vintage incluem grão fino.
- Miniaturas usam a transformação e os ajustes/cor/efeitos atuais, além da intensidade e das configurações de filtros pessoais. Mudanças nesses parâmetros invalidam o cache.
- Miniaturas têm contexto EGL separado, duas tarefas simultâneas e cache de até 60 imagens. A fila dos cards não bloqueia a fila da foto principal. A última seleção prevalece sobre filtros anteriores ainda carregando recursos.
- Sair de um ajuste ou confirmar um look conclui o gesto no histórico. Desfazer uma troca de filtro conserva o ajuste manual anterior.

## Flash 80/90 (1.2.1)

- 32 looks ajustados às quatro referências do usuário: flash verde clássico, negativo oliva quente, azul noturno e violeta lavado. Coleção Flash 80/90 aparece logo depois de Favoritos no editor e na galeria.
- Flash Verde 89, Oliva 90, Flash Azul 91 e Flash 95 aparecem juntos no começo da coleção, seguidos das variações. Para suas fotos, selfies e retratos; a referência de carros japoneses descreve a estética. Busca aceita selfie, retrato, japao, anos 80/90 e flash. Intensidade inicial de 75%, ajustável até 100%. Intensidade mistura cor, brilho e textura com a edição base.
- Curvas RGB/canais, HSL, sombras verdes, luzes mais neutras, vinheta, brilho suave nas altas luzes e grão determinístico mais evidente. Negativos/violetas incluem poeira e riscos sutis procedurais. A aparência de flash trata a luz existente; não reposiciona luzes nem cria sombras físicas.
- Os 240 filtros da versão anterior continuam com as mesmas definições. Retrato real NASA e cena sintética de carro são fixtures exclusivas de QA, ausentes do aplicativo pessoal. Detalhes e prompt em docs/JDM-FILTERS.md.

## Mais opções para Stories e fotos (1.3.0)

- 48 novos templates editáveis em Photo Dump, Viagem, Casal, Música, Aniversário, Treino, Minimal, Polaroid, Revista, Y2K, Scrapbook e Diário. Uma a seis fotos, molduras, tipografia e composições próprias. 90 templates em 23 categorias no total.
- 48 filtros locais inspirados na estética de Instagram: Clarendon, Juno, Lark, Gingham, Aden, Valencia, Ludwig, Moon, Oslo, Paris, Rio e Tokyo, com versões Classic, Soft, Pop e Matte. São tratamentos originais de cor; não executam efeitos oficiais da Meta ou filtros AR. 320 filtros em 21 coleções no total, com miniaturas na sua foto.
- 48 estilos novos: 24 combinações para foto e 24 com legendas/assinaturas/molduras editáveis. Famílias Social, Hora dourada, Editorial, Romance, Viagem, Noite, Minimal, Revista, Y2K, Papel, Playlist e Instantâneo. 72 estilos em 18 categorias no total.
- Templates e Style Lab abrem em Novos; Todos conserva acesso ao catálogo completo, favoritos e conteúdo pessoal. Busca por várias palavras sem acentos e categorias localizadas. No editor, Explorar estilos abre grade com busca/favoritos e seleções Só foto/Com elementos.
- Stories agora possuem busca e seleção de coleção nos Looks. Instagram fica acessível tanto em fotos como nas composições.
- As 272 definições de filtros, 42 templates e 24 estilos anteriores foram preservadas por igualdade de dados. Detalhes das composições, testes e capturas em docs/EXPANDED-CATALOG.md e docs/VERIFICATION.md.

## Mais flash (1.4.0)

- 24 efeitos de flash em Efeitos → Flash: direto, compacto CCD, descartável, verde retrô, azul/violeta noturno e editorial. Funcionam sobre a edição atual em fotos e Stories, com intensidade independente por camada.
- Ajustar luz do flash fica no topo do painel e abre controles de área iluminada, dureza, bordas escuras, temperatura, tonalidade, grão e posição. A seleção de camada e a intensidade ficam fixas abaixo do catálogo, inclusive em Stories. A luz tem geometria circular em canvases verticais/horizontais e preserva a transparência.
- Os 69 efeitos anteriores conservam suas definições e renderização. Ao adicionar outro efeito, o editor seleciona a nova camada para o controle de intensidade. Busca de efeitos também aceita múltiplas palavras sem acentos.
- Detalhes e limites em [docs/FLASH-EFFECTS.md](docs/FLASH-EFFECTS.md).

## Melhorar com IA no PC (1.5.0)

- Botão **Melhorar com IA** no editor de foto. Real-ESRGAN x4plus recupera definição; GFPGAN v1.4 restaura rostos opcionalmente. Processamento real na GPU NVIDIA do PC, sem hospedagem paga.
- Ampliação de até 2×/4×, força ajustável, restauração de rostos e comparação deslizante antes/depois. A IA pode inventar detalhes ou alterar traços; confira o resultado antes de aceitar.
- Aceitar cria outro projeto com a imagem melhorada e conserva o projeto anterior, suas edições e o arquivo original. Descartar apaga o resultado temporário.
- O app envia somente a imagem editada, sem EXIF/GPS, ao servidor conectado. Entrada preparada em até 2048 px, saída até 4096 px no maior lado. O servidor processa em memória, uma foto por vez.
- Conexão por endereço e código temporário de seis números; credencial salva com Android Keystore. Celular e PC precisam estar na mesma rede, com o serviço ligado. As outras ferramentas continuam funcionando sem esse servidor.

Instruções de instalação, uso e limites em [server/README.md](server/README.md). Não é a tecnologia proprietária do Remini; os resultados dependem da foto e dos modelos usados.

## Compilar

Abra a pasta no Android Studio, configure SDK 36 e JDK 17. Ajuste `local.properties` para o SDK instalado (arquivo específico desta máquina, ignorado pelo Git).

Windows: `./gradlew.bat :app:assembleDebug`. macOS/Linux: `sh gradlew :app:assembleDebug`.

APK de desenvolvimento: `app/build/outputs/apk/debug/app-debug.apk`, instalado como aplicativo separado com sufixo `.qa`. APK compacto para uso pessoal: `app/build/outputs/apk/release/app-release.apk`, produzido por `:app:assembleRelease` com redução de código/recursos. Ambos usam a chave de debug local nesta etapa; mantenha a mesma chave para atualizações. Antes de distribuir, configure uma chave privada de release.

Se o cache Gradle padrão não estiver acessível, configure `GRADLE_USER_HOME` para uma pasta gravável. Exemplo no PowerShell: `$env:GRADLE_USER_HOME = Join-Path $env:LOCALAPPDATA 'LumaStudio\gradle-cache'`.

Verificação no computador: `./gradlew.bat :core:test :app:lintDebug :app:assembleRelease`.

Para verificar os flashes e a interface no Android virtual local, compile `./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest` e execute `./tools/test_emulator.ps1 -EmulatorSerial emulator-5580 -SdkPath 'CAMINHO_DO_SDK'`, usando o identificador do seu emulador. O script recusa identificadores de celulares físicos e dirige cada operação somente ao emulador informado. Logs e capturas ficam em `app/build/emulator-checks/`.

Testes ou instalações no celular pessoal exigem um pedido explícito do usuário; não iniciar a instrumentação no aparelho enquanto ele está sendo usado.

## Adicionar filtros

Edite `app/src/main/assets/filters/presets.json` seguindo o modelo `FilterPreset`. Nenhuma mudança no renderer é necessária para novos presets com as operações existentes. O projeto incorpora a definição selecionada para continuar reabrindo mesmo que o catálogo mude. Também é possível salvar filtros no editor e importar packs pelo próprio aplicativo.

Os catálogos são próprios. Filtros podem ser regenerados com `python tools/generate_presets.py`, templates com `python tools/generate_templates.py` e estilos com `python tools/generate_styles.py`. Efeitos podem ser regenerados com `python tools/generate_effects.py`. Efeitos ficam em `assets/effects/presets.json`; templates em `assets/templates/stories.json`. Nenhum LUT/asset premium foi extraído de outro aplicativo. Bibliotecas externas permanecem sob suas respectivas licenças.

## Limites reais desta versão

- Processamento SDR sRGB de 8 bits. Não há HDR ou wide gamut. Curvas, HSL e LUT operam em SDR. LUT .cube 3D aceita 2–32 pontos por lado, até 4 MB, entradas/saídas finitas com saída entre 0 e 1. LUT 1D não é importado.
- Limite de 32 MP no decode e limite por lado da GPU do aparelho. Exportações maiores exibem erro e exigem resolução menor. Tiles são pendentes.
- “1080p” usa maior lado de 1920 px (9:16: 1080 × 1920); “1440p” usa 2560 (1440 × 2560); “4K” usa 3840 (2160 × 3840). A seleção vale para fotos, Stories e lote, inclusive com formatos de publicação Story/feed/quadrado/perfil/wallpaper. Fotos menores são ampliadas para o tamanho solicitado; isso não recupera detalhes ausentes. Stories são compostos na resolução escolhida, mantendo a proporção do canvas. “Original” conserva o tamanho nativo no modo Livre e o tamanho base no formato de publicação. Dimensões personalizadas são exatas e substituem o seletor de resolução. O diálogo mostra os pixels finais para formatos de publicação e Stories, e o maior lado escolhido para fotos em Livre.
- PNG é sem perdas. WebP com qualidade 100 usa modo sem perdas no Android 11+; Android 10 usa o compressor disponível naquela versão.
- Histórico guarda até 80 entradas na sessão; os últimos 20 estados de cada direção são salvos com o projeto.
- Alterações precisam ser salvas explicitamente; importar a fonte já cria um projeto inicial local.
- Exclusão na biblioteca não remove cópias já usadas por projetos. Arquivos originais da galeria e exportações não são apagados ao excluir projetos.
- Packs suportam filtros/efeitos/templates/estilos, fontes, LUTs e imagens. Limites: 300 itens/80 arquivos por pack, 64 MB comprimidos/descompactados, manifesto 2 MB, arquivo 16 MB, imagem 32 MP, fonte 5 MB e LUT 4 MB. Biblioteca pessoal até 3000 itens; exportação de biblioteca respeita o limite de um pack.
- Efeitos de flicker, gate weave e datamosh são interpretações estáticas para fotos, identificadas como still; não há animação ou processamento de vídeo. Textura de jornal/papel/canvas etc. é procedural, sem assets premium. Cards de música usam conteúdo manual e não se conectam a serviços de streaming.

## Pendente

Vídeo, HDR/RAW e exportação Android em tiles ficam para versões futuras. A melhoria opcional de fotos já utiliza tiles no PC. Seleção de pessoas/rostos e aproximação estética continuam locais; o envio de fotos ao PC acontece quando o usuário solicita Melhorar com IA.

Arquitetura, armazenamento, pipeline e decisões em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). Exemplos em `docs/formats/`; especificação do pack em [docs/PACKS.md](docs/PACKS.md).

Verificação desta entrega em [docs/VERIFICATION.md](docs/VERIFICATION.md), incluindo testes de GPU, composição, persistência, formatos de exportação e interface executados no aparelho Android.

O repositório inclui os testes, fixtures de instrumentação e capturas de referência da documentação. Logs, relatórios XML gerados, caches, configurações locais, APKs e chaves de assinatura ficam fora do Git. Os logs e XMLs citados nas verificações são registros locais da entrega; novas execuções produzem os próprios relatórios nas pastas `build/` dos módulos.

