# Verificação dos efeitos de flash — 1.4.0 — 07/10/2026

## Entrega

24 efeitos originais de flash; total 93 efeitos. Filtros (320), templates (90) e estilos (72) permanecem. Os 69 efeitos anteriores foram comparados por igualdade de JSON com o commit anterior. Gerador de efeitos verificado como idempotente.

Nova passagem GPU para presets flash com hardness, falloff, tint, grain e posição. O flash legado conserva sua passagem. Controles de luz, seleção da camada recém-adicionada, busca sem acentos e painel com rolagem. Detalhes em FLASH-EFFECTS.md.

## Validação executada

- 31 testes JVM/core, sem falhas ou erros, executados novamente no build final. Validação dos 93 presets e 24 respostas distintas de parâmetros.
- FlashEffectsTest no aparelho Samsung SM-S731B/Android 16: 3/3 passaram, 5,351s, registro local flash-device-render.log. Todos os 24 flashes renderizaram retratos distintos e determinísticos; intensidade zero neutra e fonte preservada. Os 69 presets anteriores também renderizaram sem erro com seus parâmetros reais.
- Fixtures uniformes verticais/horizontais verificaram centro iluminado, bordas escuras, luz circular, posição ajustável, mistura proporcional da intensidade e preservação do alfa, inclusive transparência total.
- Story com flash salvo/reaberto e exportado em PNG em outra resolução. Cores centrais entre preview/exportação dentro de dois níveis por canal; fonte intacta. Exportação de QA removida pelo próprio teste.
- flash-effects-portrait.png: original + 24 flashes, renderizados no aparelho e inspecionados visualmente. Retrato oficial NASA de Christina Koch; crédito e origem em app/src/androidTest/assets/README.md. O retrato de teste não integra o release.
- Build final flash-final-build.log: BUILD SUCCESSFUL em 2m08s; core:test, assembleDebug, assembleDebugAndroidTest, assembleRelease e lintDebug. Lint: zero erros, 16 avisos existentes. Lint vital release passou.

## Interface e uso do aparelho

A rodada FlashEffectsFlowTest + PhotoStudioFlowTest não validou a interface: os quatro testes falharam com Activity has been destroyed already durante acesso à atividade. O usuário informou que estava jogando e pediu para não abrir aplicativos no celular. A instrumentação QA foi encerrada; não houve nova tentativa, instalação do release ou abertura de aplicativo após essa correção. Os testes de interface estão compilados, mas o fluxo novo continua sem uma execução concluída.

Não iniciar testes, instalações ou navegação no aparelho sem novo pedido explícito do usuário. Continuar verificações no computador.

### Continuação apenas no computador

- Assinatura do APK conferida por apksigner 36.0.0: v2 válida, um assinante.
- tools/test_emulator.ps1 aceita somente identificadores emulator-NNNN, direciona todas as operações ao identificador informado e não enumera dispositivos. Parser PowerShell sem erros; rejeição de identificador físico verificada antes de qualquer operação Android.
- Tentativa local com Android Emulator 37.2.12 e imagem API 35: o modo acelerado recusou iniciar por falta do driver de hypervisor. Tentativa sem aceleração/sem janela também não concluiu o boot; processo encerrou e a espera local terminou por timeout. Nenhum teste de interface executou nessa tentativa. Não foram alterados recursos do Windows ou instalado driver de kernel.
- Sem acesso, abertura de aplicativos ou instalação no celular pessoal nesta continuação. O APK permanece o mesmo artefato validado abaixo. A checagem de interface continua pendente.

## Artefato

- Release studio.luma.personal, versionCode 13 / versionName 1.4.0; min29/target36. Compilado com R8 e recursos reduzidos.
- app/build/outputs/apk/release/app-release.apk: 82.542.969 bytes.
- SHA256: aed8ec6fc0dfc258041e088b891cf16da9427fc7c15ca582697c045bab67e639.
- Os quatro catálogos dentro do APK são idênticos aos atuais; fixtures de instrumentação ausentes.
- APK preparado no computador; release 1.4.0 não instalado no celular nesta entrega.

## Limites

Tratamento da luz existente, sem sombras físicas ou estimativa de profundidade. Grão é procedural/determinístico, referência espacial de 1080 px. Fotos já claras podem saturar; intensidade e controles permitem suavizar o efeito. Testes não abrangem todas as condições de iluminação, pele ou desempenho. Verificação anterior em VERIFICATION-1.3.0.md.
