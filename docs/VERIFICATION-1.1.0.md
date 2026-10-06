# Verificação do editor de fotos 1.1.0 — 06/10/2026

## Entrega

- 240 filtros originais, 69 efeitos, 42 templates e 24 estilos. Preservadas as definições dos 120 filtros anteriores. Os 120 novos usam curvas por canal/HSL/gradação próprias e renderizaram 120 resultados distintos sobre a fixture de cor.
- Samsung SM-S731B, Android 16, via depuração sem fio autorizada. Instrumentação no pacote QA separado. Projetos e arquivos pessoais não foram usados nas fixtures.
- 74 casos únicos verificados: 31 JVM/core, 21 renderer e 22 app. Core passou em looks-tests.log e foi conferido em looks-verified-delivery.log. Renderer completo: 21/21 em looks-delivery.log.
- App completo: 21/22 na rodada de looks-verified-delivery.log; abrir máscaras no retrato expirou uma vez. O mesmo teste passou sem mudança no código do aplicativo na repetição direcionada em mask-retry-and-package.log. Os registros mostram mudança de foco/pausa da Activity nessa tentativa. Não foi reproduzida uma falha de renderização. Resultado da rodada e repetição preservados em app-checks-before-mask-retry.xml e mask-retry-results.xml.
- Após acrescentar busca sem acentos e termos localizados, os três testes PhotoStudioFlowTest passaram novamente em looks-final-package.log (zero falhas, erros ou skips). Busca por filme/petala/neon rosa, favorito, fechamento do teclado, intensidade, remover filtro e dois passos de desfazer foram exercitados pela interface.
- Miniaturas conferidas por comparação de pixels: crop/rotação, exposição, balanço manual, intensidade salva, ajustes/cor de filtro pessoal e invalidação do cache. Seleções rápidas com cards carregando conservaram o último filtro e a imagem correspondente.
- Durante a criação dos novos testes, bloquear a thread de instrumentação esperando continuações do dispatcher de teste impedia avanço das miniaturas. Os testes agora aguardam pelo relógio/idle do Compose; os asserts de imagem permaneceram. Seletores também distinguem cards da galeria e do carrossel.
- Capturas reais finais inspecionadas: docs/photo-editor.png e docs/look-gallery.png. Teclado fechado, cards inteiros, seleção e favoritos legíveis; texto não se sobrepõe aos controles. Fixture NASA exclusiva do teste, ausente do APK pessoal, com crédito em app/src/androidTest/assets/README.md.
- Build final: BUILD SUCCESSFUL em 2m20s; lint debug com zero erros e 16 avisos já existentes (dependências/KTX/backup). Lint vital de release passou. APK com R8 e recursos reduzidos, assinatura v2 válida e um assinante; min29/target36. Sem INTERNET/NETWORK.
- APK: 82,485,933 bytes; versionCode 9/versionName 1.1.0. SHA256: ae41b030b00e9ade0d55e4e4deb251c78801fac42950d2278d07c4a3b4c3b2c8.
- Atualização instalada com install -r: Success, preservando os dados do aplicativo pessoal. Versão instalada e início verificados após a instalação. QA removido pela instrumentação; apenas studio.luma.personal no usuário principal.

## Escopo e limites

O novo PhotoStudio substitui a tela de fotos. Stories, máscaras e controles Pro continuam usando seus próprios espaços de edição. Miniaturas possuem contexto EGL e fila separados da foto principal, com duas tarefas e cache de 60 imagens; ambos usam a mesma GPU física. A validação funcional não é uma medição de FPS ou uma garantia de latência em qualquer aparelho/stack de efeitos.

Salvamento de projetos continua explícito. SDR sRGB de 8 bits, limites de decode/memória/GPU e os demais limites da versão anterior permanecem. Detalhes em PHOTO-EDITOR.md, ADVANCED-EDITOR.md e README.md. Verificação anterior preservada em VERIFICATION-1.0.0.md.
