# Verificação do catálogo expandido — 1.3.0 — 06/10/2026

## Entrega

- 320 filtros em 21 coleções, 90 templates em 23 categorias e 72 estilos em 18 categorias. Mais 48 de cada tipo; 69 efeitos permanecem. As 272 definições de filtros, 42 templates e 24 estilos anteriores foram comparadas por igualdade de JSON com o APK 1.2.1 antes do build.
- 48 filtros locais inspirados na estética de Instagram, quatro respostas por família; 48 templates com geometria própria e uma a seis fotos; 24 estilos de foto e 24 com legendas/assinaturas e formas editáveis. Nenhum asset externo novo é necessário.
- Templates/Style Lab abrem em Novos e conservam Todos, favoritos e conteúdo pessoal/importado. Busca por palavras sem acentos e categorias localizadas. Nova grade Explorar estilos no editor, com Só foto/Com elementos. Busca e categorias de filtros também nos Stories.
- A regeneração de estilos inicialmente apontou diferença nos defaults embutidos de efeitos Pro dos estilos antigos. O gerador agora mantém os parâmetros originais dos primeiros 24 estilos, independentemente do catálogo de efeitos. Igualdade com a versão anterior confirmada para os três catálogos.

## Testes

- Samsung SM-S731B, Android 16, depuração sem fio autorizada. Pacote QA separado; arquivos pessoais não usados. Retrato NASA apenas na instrumentação, com crédito em app/src/androidTest/assets/README.md.
- 31 testes JVM/core e 13 testes únicos de aplicativo verificados nesta revisão. Rodada de aparelho expansion-device-tests.log: 13/13, sem falhas/erros/skips; XML preservado em expansion-1.3.0-app-checks.xml. Classes: ExpansionStorageTest (3), ExpansionFlowTest (2), StoryStorageTest (1), StudioFlowTest (4), PhotoStudioFlowTest (3).
- Todos os 48 filtros sociais renderizaram resultados distintos na fixture de cor; Moon/Tokyo são neutros a 100%. Prévia em retrato de 12 exemplos + original em instagram-portrait-looks.png, a 85% (a intensidade mistura com a edição base).
- Todos os 48 templates foram preenchidos e renderizados pelo compositor real. Grade de seis fotos salva/reaberta e exportada em PNG de 540×960, preservando bytes da fonte. As três exportações existentes também passaram em StoryStorageTest/StudioFlowTest. Fonte repetida serve como fixture de teste; não demonstra enquadramento automático em cada layout.
- Todos os 48 estilos renderizaram resultados distintos em Story 9:16 com foto real: 24 sem camadas gráficas e 24 com gráficos. Conferidas fontes sem recursos externos e fonte fotográfica intocada.
- Fluxo pela interface: templates Novos, busca Seis momentos, favorito, preencher seis fotos, buscar/aplicar Valencia Soft em Story, salvar e reabrir. Na foto: galeria Instagram/Juno Soft, galeria de estilos, busca instantaneo sem acento, favorito, aplicar estilo fotográfico e desfazer; aplicar Legenda, conservar recorte/espelhamento, acrescentar texto manual, trocar para Assinatura e desfazer sem duplicar elementos.
- Revisão visual identificou título cortado em Quatro encontros e decoração sobre texto em Aniversário/Y2K. Ajustados tamanho/posição; assinaturas receberam contorno/sombra para contraste em fotos claras. Novos textos são medidos com StaticLayout/Typeface no aparelho para conferir altura dentro da camada, além da inspeção visual.
- Após esses ajustes, ExpansionStorageTest + ExpansionFlowTest passaram 5/5 em expansion-visual-final.log (46s); XML expansion-visual-checks.xml. Depois de reduzir a fonte do placeholder da busca de templates, ExpansionFlowTest passou novamente durante expansion-delivery.log.
- Gradle core:test agora declara os quatro JSONs como inputs para invalidar corretamente testes quando o catálogo muda. Rodada JVM final executada em expansion-delivery.log, não apenas cacheada.
- Nenhuma alteração no renderer de produção. Sua suite anterior está em VERIFICATION-1.1.0.md; não contada como execução nova. Limites de memória/decode/GPU continuam.

## Capturas

- expanded-template-gallery.png, expanded-instagram-gallery.png, expanded-style-browser.png, expanded-story-editor.png: capturas do app no aparelho, teclado fechado e controles legíveis.
- new-story-templates-1.png a new-story-templates-6.png: 48 composições preenchidas, renderizadas no aparelho. Grupos com ajustes de tipografia/decor foram inspecionados novamente.
- new-styles-portrait.png: oito exemplos de estilos fotográficos/gráficos. instagram-portrait-looks.png: 12 famílias novas a 85%. Crédito NASA nas imagens. Fixtures não fazem parte do APK pessoal.

## Build e instalação

- Build final em expansion-delivery.log: BUILD SUCCESSFUL em 1m35s. Inclui core:test, dois testes de interface, lint debug e assembleRelease. XML final da interface em expansion-delivery-checks.xml, 2/2, zero falhas/erros/skips. A rodada JVM final registra 31/31 sem falhas.
- Lint debug: zero erros e 16 avisos já existentes (dependências/KTX/backup); lint vital release passou. Assinatura v2 válida, um assinante, min29/target36. APK com R8 e recursos reduzidos.
- Catálogos conferidos dentro do APK: 320 filtros / 90 templates / 72 estilos, idênticos aos JSONs atuais. As 48 geometrias de fotos dos templates novos são distintas. Fixtures de instrumentação ausentes do release.
- APK versionCode 12 / versionName 1.3.0, 82,541,761 bytes. SHA256: e423e366e664fb211a987966d45d4030e8ca35b679158c97b216725806a1241b.
- install -r retornou Success, preservando os dados do aplicativo pessoal. Versão instalada conferida no aparelho; início Status ok, COLD confirmado. Apenas studio.luma.personal no usuário principal; QA removido pela instrumentação. Não foi usado uninstall no pacote pessoal.

## Escopo

Filtros são tratamentos originais inspirados em estética de Instagram, sem efeitos oficiais da Meta/AR. Layouts e elementos continuam editáveis, com salvamento explícito. Um estilo gráfico aplicado à foto cria um Story; trocar estilo preserva fotos, máscaras e elementos manuais. Nem todo crop central acomoda um retrato em moldura horizontal: o editor oferece posição/zoom e enquadramento inteligente. Não houve uma medição de FPS ou validação de todas as condições de iluminação/cores de pele.

Verificações anteriores em VERIFICATION-1.2.1.md e anteriores. Detalhes em EXPANDED-CATALOG.md, PHOTO-EDITOR.md e README.md.
