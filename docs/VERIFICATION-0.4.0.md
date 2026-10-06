# Verificação da entrega 0.4.0 — 05/10/2026

## Confirmado

- 29 testes aprovados na execução final: 13 JVM, 10 de renderer e 6 de aplicativo, sem falhas/erros/skips. Relatórios em core/build/reports/tests/test, renderer/build/reports/androidTests e app/build/reports/androidTests.
- JVM: catálogos de 30 filtros, 18 efeitos, 20 templates e 6 estilos originais; compatibilidade de projetos antigos, histórico/serialização, sete layouts de colagem preservando fontes/crops/bloqueio, round-trip dos packs, referências de fotos fixas e efeitos com intensidade/visibilidade. Rejeição de caminhos absolutos/traversal/conteúdo desconhecido/versões futuras/imagens ausentes e manifesto com expansão excessiva.
- Renderer no Samsung SM-S731B/Android 16: cor/orientação/alpha, crop/rotação, intensidade, stack, bloom, preview/exportação, blend/ordem/opacidade, texto e fotos faltantes. Novo teste de borda, cantos arredondados, sombra e crop/rotação por camada passou.
- App no mesmo aparelho: projetos de foto/Story, filtros/favoritos, JPEG/PNG/WebP, salvar/reabrir. Template próprio preservou uma foto fixa e solicitou somente as duas placeholders. Estilo manteve texto e dois efeitos; aplicar em foto preservou o crop/rotação e reaplicar não duplicou elementos do estilo. Filtro pessoal recompôs pixels iguais aos do tratamento salvo. Exportar/importar pack preservou fontes e IDs existentes; imagem inválida foi rejeitada sem alterar o catálogo anterior.
- Captura real de colagem em docs/collage-editor.png foi inspecionada. Imagens são gradientes de diagnóstico do teste, não fotos do usuário.
- :app:lintDebug: zero erros, 12 avisos (dependências fixadas, backup legado e sugestões KTX).
- :app:assembleRelease: APK compacto 0.4.0/versionCode 4, 2.588.165 bytes (aproximadamente 2,47 MiB). Redução de código/recursos concluída; assinatura v2 validada por apksigner, um assinante.
- Atualização no celular: adb install -r retornou Success. Pacote studio.luma.personal confirmado com versionCode 4/versionName 0.4.0. Inicialização a frio retornou Status: ok em 472 ms. O aplicativo pessoal não foi desinstalado; seus arquivos privados foram preservados pelo Android.

## Condições e limites

Testes executados no aplicativo separado studio.luma.personal.qa, sem acessar fotos/projetos do aplicativo pessoal. Fontes e arquivos temporários foram criados pelo teste, exportações de diagnóstico apagadas, e o estado da biblioteca QA restaurado. O Android removeu os aplicativos instrumentados ao finalizar.

A Activity de teste pode mostrar somente sua interface acima da tela de bloqueio e manter a tela ligada, aguardando a hierarquia de UI. Nenhum PIN foi removido/desbloqueado. Essas flags não fazem parte do aplicativo pessoal.

A primeira execução do novo fluxo revelou um seletor de teste ambíguo: nome do projeto e aba ambos chamados Colagem. O teste passou a selecionar a aba pelo seu contêiner. A execução seguinte do fluxo passou, seguida pela execução final de todos os testes. Não houve simulação de resultado para contornar o fluxo.

Os fluxos instrumentados usam a versão QA sem redução de código. A versão compacta foi compilada, assinada, instalada e iniciada. Testes não equivalem a uso prolongado em todos os aparelhos/tamanhos de fotos ou a auditoria completa de acessibilidade/desempenho. Limites de packs e recursos ainda pendentes constam em README.md/PACKS.md.

## Evidência

phase4-final-checks.log registra a execução final completa de testes, lint e APK. phase4-library-ui-test.log registra o fluxo corrigido de biblioteca/colagem/filtro/estilo. docs/VERIFICATION-0.3.0.md preserva o registro anterior.
