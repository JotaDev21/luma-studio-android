# Verificação dos efeitos de flash — 1.4.0 — 07/10/2026

## Entrega

24 efeitos originais de flash; total 93 efeitos. Filtros (320), templates (90) e estilos (72) permanecem. Os 69 efeitos anteriores foram comparados por igualdade de JSON com o commit anterior. Gerador de efeitos verificado como idempotente.

Nova passagem GPU para presets flash com hardness, falloff, tint, grain e posição. O flash legado conserva sua passagem. Controles de luz no topo; seleção de camada e intensidade fixas abaixo do catálogo. Busca sem acentos e rolagem apenas no catálogo. Detalhes em FLASH-EFFECTS.md.

## Validação executada

- 31 testes JVM/core, sem falhas ou erros, executados novamente no build final. Validação dos 93 presets e 24 respostas distintas de parâmetros.
- FlashEffectsTest no aparelho Samsung SM-S731B/Android 16: 3/3 passaram, 5,351s, registro local flash-device-render.log. Todos os 24 flashes renderizaram retratos distintos e determinísticos; intensidade zero neutra e fonte preservada. Os 69 presets anteriores também renderizaram sem erro com seus parâmetros reais.
- Fixtures uniformes verticais/horizontais verificaram centro iluminado, bordas escuras, luz circular, posição ajustável, mistura proporcional da intensidade e preservação do alfa, inclusive transparência total.
- Story com flash salvo/reaberto e exportado em PNG em outra resolução. Cores centrais entre preview/exportação dentro de dois níveis por canal; fonte intacta. Exportação de QA removida pelo próprio teste.
- flash-effects-portrait.png: original + 24 flashes, renderizados no aparelho e inspecionados visualmente. Retrato oficial NASA de Christina Koch; crédito e origem em app/src/androidTest/assets/README.md. O retrato de teste não integra o release.
- Build inicial flash-final-build.log: BUILD SUCCESSFUL em 2m08s; core:test, assembleDebug, assembleDebugAndroidTest, assembleRelease e lintDebug. Lint: zero erros, 16 avisos existentes. Lint vital release passou.

## Interface e uso do aparelho

A rodada inicial FlashEffectsFlowTest + PhotoStudioFlowTest falhou com Activity has been destroyed already durante acesso à atividade. O usuário pediu para não abrir aplicativos enquanto usava o celular; a instrumentação foi encerrada. Houve uma continuação apenas no computador e, depois, nova autorização explícita para testar e instalar no aparelho.

Não iniciar testes, instalações ou navegação no aparelho sem novo pedido explícito do usuário. Continuar verificações no computador.

### Continuação apenas no computador

- Assinatura do APK conferida por apksigner 36.0.0: v2 válida, um assinante.
- tools/test_emulator.ps1 aceita somente identificadores emulator-NNNN, direciona todas as operações ao identificador informado e não enumera dispositivos. Parser PowerShell sem erros; rejeição de identificador físico verificada antes de qualquer operação Android.
- Tentativa local com Android Emulator 37.2.12 e imagem API 35: o modo acelerado recusou iniciar por falta do driver de hypervisor. Tentativa sem aceleração/sem janela também não concluiu o boot; processo encerrou e a espera local terminou por timeout. Nenhum teste de interface executou nessa tentativa. Não foram alterados recursos do Windows ou instalado driver de kernel.
- Não houve acesso, abertura de aplicativos ou instalação no celular pessoal durante a continuação restrita ao computador. A validação no aparelho só foi retomada após nova autorização.

### Interface concluída no aparelho

- PhotoStudioFlowTest passou 3/3, 12,69s, em flash-ui-regression-resume.log: busca/favoritos/intensidade/undo, seleção rápida de looks e invalidação de miniaturas com crop/ajustes/filtros pessoais.
- FlashEffectsFlowTest passou 2/2, 5,976s, no APK QA final, em flash-pinned-controls-device.log. Foto: adicionar flash, ajustar área, selecionar camada anterior, adicionar outro flash, mudar intensidade/dureza sem alterar a camada anterior, desfazer e salvar. Story: preencher uma foto, abrir controles sem rolar até o botão, mudar posição e intensidade, salvar/reabrir e preservar camada/bytes da fonte.
- Revisão visual encontrou o botão de luz fora da área inicial e, numa etapa intermediária, intensidade parcialmente coberta pela exportação do Story. Botão fixado no topo; camadas/intensidade fixas abaixo do catálogo; cards compactados. O teste agora compara as coordenadas da intensidade com o botão Exportar Story para impedir sobreposição, além da inspeção das capturas finais.
- Uma execução intermediária encontrou um nó de teste obsoleto ao rolar logo após fechar o diálogo. O teste aguarda a renderização e interage diretamente com a intensidade visível. A rodada final passou ambos os fluxos; não foi necessário mudar o renderer.
- flash-effects-editor.png e flash-effects-story.png: capturas finais do QA, inspecionadas com o botão de luz e a intensidade legíveis. Retrato NASA, sem fotos pessoais do usuário.
- 8 testes únicos de aparelho verificados nesta entrega: FlashEffectsTest (3), PhotoStudioFlowTest (3), FlashEffectsFlowTest (2), além dos 31 JVM/core. Não foram contadas repetições como testes distintos.
- Build final de produção flash-pinned-controls-build.log: BUILD SUCCESSFUL em 1m54s, assembleDebug/AndroidTest, assembleRelease e lintDebug. APK de testes recompilado em flash-pinned-geometry-test-build.log para incluir a asserção de sobreposição; 6s, sucesso.

## Artefato

- Release studio.luma.personal, versionCode 13 / versionName 1.4.0; min29/target36. Compilado com R8 e recursos reduzidos.
- app/build/outputs/apk/release/app-release.apk: 82.542.969 bytes.
- SHA256: 47079ebc9b68ad56dce70c9fb5d281193be5b20bfe03ebd10d98af5f3e64d3fa.
- Os quatro catálogos dentro do APK são idênticos aos atuais; fixtures de instrumentação ausentes.
- Cópia de entrega em dist/Luma-Studio-1.4.0.apk, com o mesmo hash. install -r retornou Success; versão instalada 1.4.0 / código 13 conferida no aparelho, preservando dados/projetos pessoais.
- Início do release: Status ok, COLD, TotalTime 1159ms. Só studio.luma.personal no usuário principal; QA e instrumentação removidos após os testes. Não foi usado uninstall no aplicativo pessoal.

## Limites

Tratamento da luz existente, sem sombras físicas ou estimativa de profundidade. Grão é procedural/determinístico, referência espacial de 1080 px. Fotos já claras podem saturar; intensidade e controles permitem suavizar o efeito. Testes não abrangem todas as condições de iluminação, pele ou desempenho. Verificação anterior em VERIFICATION-1.3.0.md.
