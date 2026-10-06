# Verificação da entrega 0.7.0 — 05/10/2026

## Confirmado

- 59 testes aprovados na execução final: 27 JVM, 18 de renderer e 14 de aplicativo, sem falhas/erros/skips. Relatórios em core/build/reports/tests/test, renderer/build/reports/androidTests e app/build/reports/androidTests.
- Core: margem em rosto fora do centro; rostos separados exigem foto inteira; rotações/espelhamentos; fallback pessoa/centro; atribuição global de custo mínimo com até 20 fontes; molduras vazias; máscaras junto ao asset; preservação de geometria/aparência, bloqueios, camadas ocultas e gráficos. Schema 1–6 lê como cover; contain persiste no schema 7, com undo/redo.
- Renderer no Samsung SM-S731B/Android 16: Foto inteira mantém ambas as bordas da fonte e mostra margens na prévia/exportação; Preencher corta essas bordas como esperado. Os 17 testes anteriores de GPU, máscaras, transparência, composição e exportação passaram.
- App: modelo incorporado detectou rosto no retrato NASA e ambos os rostos na imagem dupla, com INTERNET negada no aplicativo alvo. Sugestão em paisagem preservou rosto/cabeça e retoques, sem alterar parâmetros antes de aplicar. Comparar/cancelar/aplicar, undo/redo, salvar/reabrir schema 7 e PNG idêntico à prévia passaram, mantendo os bytes originais.
- App: imagem dupla em moldura vertical recebeu contain, mantendo margens e ambos os rostos visíveis. Salvar/compor na resolução original conservou a prévia. Organização trocou fotos horizontal/vertical, levou máscaras com sua fonte e manteve gráficos, com aceitar/desfazer/refazer/salvar, sem alterar arquivos.
- Edição rápida: sugestão aberta durante a renderização de um ajuste mostrou o Antes atualizado. Cancelar conservou parâmetros/histórico; aplicar/desfazer voltou ao ajuste manual anterior. Fechar/reabrir rapidamente também passou com a transição protegida.
- Templates/packs mantiveram contain/máscaras em fotos fixas; placeholders descartaram retoques e voltaram a cover. Os dez testes anteriores de aplicativo passaram.
- Capturas reais inspecionadas em docs/smart-crop-proposal.png, docs/smart-whole-photo-proposal.png e docs/smart-layout-proposal.png. Retrato NASA somente no APK de testes, com crédito em SEGMENTATION.md.
- Lint: zero erros, 13 avisos existentes. APK compacto 0.7.0/versionCode 7: 82.067.050 bytes (78,27 MiB), com modelos e bibliotecas das arquiteturas suportadas. R8/redução de recursos concluídos. Assinatura v2 validada, um assinante.
- APK inspecionado: modelos em assets/models_bundled e segmentação incorporada; nenhum portrait-nasa fixture. Sem permissões de internet/rede; única uses-permission é a assinatura interna AndroidX.
- Atualização no celular via install -r: Success. studio.luma.personal confirmado em 0.7.0/versionCode 7; início a frio Status: ok, TotalTime 510 ms. App pessoal não desinstalado. Apenas esse pacote permaneceu instalado no usuário principal.
- SHA256 do APK: 381a8fee0d7a327ac9cd5b7002421b1ffd64ad3c2bad7808963bb287b33bb560.

## Correções e condições

A nova aba exigiu rolar a barra no teste antigo de Efeitos/Criar, como na interação normal. O teste de edição rápida passou a medir a diferença de brilho na escala real do controle, sem mudar o shader. Uma execução registrou ActivityScenario destruída; os quatro cenários isolados e a execução final completa passaram.

Fechar/reabrir revelou uma corrida: o fechamento podia terminar a leitura de projetos depois que uma nova abertura já começara, limpando o novo editor. closeEditor agora mantém busy durante a transição, lê a lista antes de atualizar o estado e encerra o indicador de renderização ao fechar. Sugestões recompõem o Antes quando necessário. Cancelar restaura a renderização normal sem registrar parâmetros; aplicar guarda o estado manual anterior e a sugestão.

Instrumentação no pacote separado studio.luma.personal.qa, sem ler fotos/projetos pessoais. Arquivos de diagnóstico/exportações removidos e biblioteca QA restaurada. A Activity de testes mostrou somente a própria UI acima do bloqueio, sem remover/desbloquear PIN; essas flags não estão no aplicativo pessoal. Pacotes instrumentados removidos pelo Android.

Testes usam QA sem redução. Versão compacta compilada, assinada, inspecionada, instalada e iniciada; inferência/edição não instrumentadas no pacote pessoal. Proteção considera rostos efetivamente detectados e requer revisão na prévia, sem garantia em todas as cenas. Organização distribui fotos nas molduras existentes, preservando o layout. Referência estética e controles avançados seguem pendentes.

## Evidência

phase7-final-checks.log: execução completa bem-sucedida, 1m52s. phase7-smart-check.log: quatro cenários novos isolados. SMART-FRAMING.md descreve algoritmo/persistência/limites e integração oficial do ML Kit. Entrega anterior preservada em VERIFICATION-0.6.0.md.
