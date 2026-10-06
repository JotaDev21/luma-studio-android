# Verificação da entrega 0.5.0 — 05/10/2026

## Confirmado

- 39 testes aprovados: 17 JVM, 14 de renderer e 8 de aplicativo, sem falhas/erros/skips. Relatórios em core/build/reports/tests/test, renderer/build/reports/androidTests e app/build/reports/androidTests.
- Novos testes JVM: máscaras/traços/borracha no JSON e no histórico, coordenadas após todas as combinações de crop/flip/rotação, compatibilidade de projetos anteriores e rejeição de máscaras inválidas/duplicadas/excessivas ou colocadas fora de uma foto do Story.
- Renderer no Samsung SM-S731B/Android 16: área radial, inversão, intensidade parcial/zero, visibilidade, alpha, pincel com buraco apagado, degradê, ancoragem após crop/flip/quatro rotações, preview/exportação e ordem de máscaras/efeitos. Testes anteriores de cor, composição e efeitos também passaram.
- App no mesmo aparelho: criação de pincel pela UI em foto girada, pintura, undo/redo do traço inteiro, exposição local, salvar/reabrir e exportar PNG com pixels iguais à prévia sem sobreposição rosa. Bytes da fonte permaneceram iguais.
- Conversão de foto para Story preservou máscaras. O editor isolado retornou à composição inteira; duplicação/edição manteve máscaras independentes e trocar a fonte limpou os retoques anteriores. Salvamento incluiu schema 5 e máscaras por camada.
- Template próprio/pack conservaram máscaras da foto fixa; placeholders tiveram máscaras removidas. Importação/exportação manteve fontes imutáveis e rejeição atômica de conteúdo inválido. Demais fluxos anteriores de catálogo, colagem, estilos, projetos e JPEG/PNG/WebP passaram.
- Captura real de UI em docs/mask-editor.png foi inspecionada. É uma imagem uniforme criada pelo teste; nenhuma foto privada do usuário foi acessada.
- Lint: zero erros, 12 avisos de dependências/backup legado/sugestões KTX. APK compacto com redução de código/recursos: 0.5.0/versionCode 5, 2.637.317 bytes (aproximadamente 2,52 MiB). Assinatura v2 validada, um assinante.
- Atualização no celular via install -r: Success. Pacote studio.luma.personal confirmado em 0.5.0/versionCode 5; inicialização a frio com Status: ok, TotalTime 327 ms. O aplicativo pessoal não foi desinstalado; atualização preserva seus arquivos privados. Ao terminar, apenas o pacote pessoal estava instalado.

## Condições e limites

Testes executados no aplicativo separado studio.luma.personal.qa, com fontes e projetos de diagnóstico. Exportações temporárias foram apagadas e a biblioteca QA restaurada. As Activities de teste mostram apenas a própria interface acima do bloqueio do aparelho; não desbloqueiam/removem PIN. Essas flags não existem no aplicativo pessoal. O Android removeu os aplicativos instrumentados após os testes.

O teste inicial da ordem de máscaras/efeitos usava a lateral como região preta de um frame cinematográfico que tem barras superior/inferior. A verificação passou a medir a barra correta, mantendo a checagem do centro com ajuste local. A execução final completa passou. Também foi conferida a pintura em foto já girada para verificar a conversão das coordenadas e o tamanho da indicação do pincel.

Instrumentação usa o aplicativo QA sem redução. A versão compacta foi compilada, assinada, instalada e iniciada, sem instrumentação da versão pessoal. Testes não equivalem a uso prolongado em todos os aparelhos ou auditoria completa de desempenho/acessibilidade. Máscaras vetoriais manuais estão disponíveis; segmentação automática, smart crop/layout, referência estética e IA continuam pendentes. Limites de resolução, memória, traços e packs constam em README.md/MASKS.md/PACKS.md.

## Evidência

phase5-final-checks.log registra a execução final completa. Modelos/pipeline estão documentados em MASKS.md e ARCHITECTURE.md. VERIFICATION-0.4.0.md e VERIFICATION-0.3.0.md preservam entregas anteriores.
