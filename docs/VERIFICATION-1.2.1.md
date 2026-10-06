# Verificação Flash 80/90 — 1.2.1 — 06/10/2026

## Entrega

- 272 filtros em 20 coleções, 69 efeitos, 42 templates e 24 estilos. 32 filtros na coleção pública Flash 80/90, chave interna JDM. Os 240 anteriores conservaram suas definições; a intensidade inicial de 75% se aplica apenas aos 32 novos.
- Destinados a fotos pessoais, selfies e retratos. A referência de carros japoneses define a aparência verde/flash/filme antigo, não restringe o tipo de foto. Revisão das 32 definições baseada nas quatro referências enviadas: paletas verde, oliva, azul e violeta, grão maior e dust sutil nos negativos/violetas. IDs preservados, meta.version 2; primeiras quatro opções intercalam as direções. As 240 definições anteriores foram comparadas por igualdade de JSON com o APK 1.2.0 antes de recompilar.
- Samsung SM-S731B, Android 16, via depuração sem fio autorizada. Instrumentação no pacote QA separado; arquivos pessoais não foram usados nos testes.
- 31 testes JVM/core e 9 testes do aplicativo passaram em retro-reference-tests.log, zero falhas, erros ou skips. XML da rodada no aparelho preservado em retro-1.2.1-app-checks.xml. Suite selecionada: JdmFilterFlowTest (2), PhotoStudioFlowTest (3), StudioFlowTest (4).
- Todos os 32 novos looks renderizaram resultados distintos, paletas características em cinza neutro (verde clássico, oliva quente, azul e violeta), transparência preservada e intensidade zero idêntica à edição base. Imagem original imutável. A fixture de cor verifica cada look a 100%; a prévia em retrato usa 75%.
- Fluxo real de retrato: abrir, selecionar Flash 80/90, aplicação inicial a 75%, ajustar intensidade a 72%, pesquisar Flash Azul, selecionar, fechar teclado, salvar, reabrir com estado idêntico e arquivo fonte intocado. PhotoStudio também confere favoritos, desfazer, miniaturas e seleção rápida. StudioFlow inclui persistência e formatos de exportação.
- Antes da clarificação sobre uso em retratos, duas verificações JDM passaram com a cena sintética de carro em jdm-flow.log. A rodada final usa o retrato NASA. Nenhuma alteração no renderer; a verificação anterior de suas 21 funções permanece documentada em VERIFICATION-1.1.0.md, não contada como execução nova nesta versão.
- Capturas finais inspecionadas: retro-reference-editor.png e retro-reference-gallery.png. Coleção visível depois de Favoritos, cards inteiros, texto e controles legíveis. Comparação real do renderer no aparelho em retro-reference-portrait.png, a 75%, com crédito NASA. Inspeção visual de quatro exemplos não equivale a validação de todas as cores de pele ou condições de iluminação.

## Empacotamento e instalação

- Build concluído em 1m06s em retro-reference-release.log. Lint debug: zero erros e 16 avisos já existentes (dependências/KTX/backup). Lint vital release passou.
- APK com R8/recursos reduzidos, assinatura v2 válida, um assinante; min29/target36. Catálogo conferido dentro do APK: 272 filtros, 32 Flash 80/90 com meta.version 2; fixtures de teste ausentes.
- versionCode 11 / versionName 1.2.1; 82,492,785 bytes. SHA256: cd731084f31250d940c16a1a7dee4253d66ec5d98451116961574235dcd98e99.
- Instalação com install -r retornou Success, preservando dados do aplicativo. Versão instalada e abertura verificadas; apenas studio.luma.personal no usuário principal. Não foi usado uninstall no aplicativo pessoal.

## Escopo e limites

A aparência de flash depende da luz existente, não produz nova iluminação física. HSL opera em faixas de cor; não há detecção automática de pele nestes filtros. Intensidade ajustável permite suavizar o efeito conforme a foto. Novos filtros usam os recursos existentes e são locais, sem downloads.

Salvamento continua explícito. SDR sRGB de 8 bits, limites de decode/memória/GPU e demais limites do editor permanecem. As fixtures NASA e sintética são exclusivas da instrumentação, fora do APK pessoal. Detalhes e prompt da imagem sintética em JDM-FILTERS.md; editor em PHOTO-EDITOR.md. Verificações anteriores preservadas em VERIFICATION-1.2.0.md e VERIFICATION-1.1.0.md.
