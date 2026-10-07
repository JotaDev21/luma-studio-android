# Verificação de usabilidade e rascunhos — 1.6.0 — 07/10/2026

## Entrega

Início com Editar foto, Criar Story e Fazer colagem; projetos recentes; modo simples com quatro ferramentas visíveis; ajustes com nomes claros; filtros favoritos e populares antes do catálogo completo; acesso às ferramentas detalhadas pelo menu. Salvar foto abre opções recomendadas e recolhe configurações técnicas em Opções avançadas. Ajuda curta e textos/áreas de toque maiores nos fluxos principais.

Rascunhos privados gravados com AtomicFile, separados do salvamento explícito. Recuperação de edição/histórico, estado visível, gravação após pausa de 900 ms, tentativa imediata em segundo plano e gravação aguardada ao voltar ao início. Descarte confirmado conserva projeto/fonte; rascunhos inválidos ou incompatíveis com a base são ignorados. Detalhes em [USABILITY.md](USABILITY.md) e avaliação inicial em [USABILITY-AUDIT.md](USABILITY-AUDIT.md).

## Validação executada

- 37 testes JVM/core passaram, zero falhas e zero erros nos XMLs em core/build/test-results/test. O código de resolução do renderer não foi alterado.
- Compilação atual de core/debug/test APK/release/lint: BUILD SUCCESSFUL, 2m38s, usability-current-build.log. Após corrigir a condição antiga de permissão nos testes locais, o APK de instrumentação compilou em 36s, usability-local-model-test-build.log; lint atual em 27s, usability-current-lint.log. Lint: zero erros, 19 avisos, incluindo 18 anteriores e uma recomendação para usar LocalWindowInfo em vez de screenHeightDp na escolha de layout. Lint vital release passou. Nenhuma dependência foi atualizada nesta entrega.
- Primeira verificação no Samsung SM-S731B/Android 16: UsabilityFlowTest 4/4, ProjectDraftStoreTest 2/2 e ExportDialogTest 3/3, OK (9 tests), 15,948 s, usability-first-device.log.
- Verificação com o APK debug atual: 29 casos executados em 76,304 s, usability-final-device.log. Destes, 25 passaram; os quatro casos SmartFlowTest pararam na preparação porque ainda exigiam a ausência da permissão INTERNET, adicionada para a IA na versão 1.5.0. A condição foi corrigida para conferir o pacote QA e a ausência de conexão com a IA, sem alterar o código de recorte/segmentação.
- Reexecução dos quatro testes SmartFlowTest e dos dois SegmentationFlowTest: OK (6 tests), 15,958 s, usability-local-model-device.log. Detecção real de rosto, sugestão/revisão/cancelamento/aplicação, undo, persistência, exportação PNG, organização de fotos, seleção de pessoa/fundo e tratamento de foto sem pessoa passaram. Não houve conexão configurada com a IA do PC; a rede do celular não foi desativada.

Os 31 testes Android diferentes abaixo passaram após a correção da preparação:

| Conjunto | Aprovados | Verificado |
|---|---:|---|
| UsabilityFlowTest | 4 | Início, Story em branco, modo simples/completo, ajuda, rascunho, descarte, fonte 200% e galeria |
| ProjectDraftStoreTest | 2 | Fonte/base/histórico preservados; rascunho obsoleto/corrompido; descarte e projeto excluído |
| ExportDialogTest | 3 | Padrão recomendado, publicação/resolução e dimensões personalizadas exatas |
| PhotoStudioFlowTest | 3 | Busca, favoritos, intensidade, undo, miniaturas e última seleção de filtro |
| FlashEffectsFlowTest | 2 | Ajustar outro flash, undo, salvar e flash no Story |
| ExportResolutionTest | 2 | 1440p/4K em JPEG/PNG/WebP, fotos pequenas, Stories e publicação |
| StoryStorageTest | 1 | Modelos, recursos, persistência e exportação de Story |
| MaskFlowTest | 2 | Pincel/undo, reabertura/exportação, máscaras por foto e conversão |
| SmartFlowTest | 4 | Revisão/cancelamento, rostos reais, recorte, undo e organização |
| SegmentationFlowTest | 2 | Seleção/correção de pessoa/fundo, transparência e foto sem pessoa |
| AdvancedFlowTest | 3 | Cor/referência, música/desenho/degradê e lote |
| ExpansionFlowTest | 2 | Modelos, seis fotos, Instagram e aplicação/troca de estilos |
| EnhancementFlowTest — somente entrada/cancelamento | 1 | Entrada manual acessível; cancelar conserva projeto/foto; sem conexão/envio |

## Capturas e limites

easy-home.png, easy-photo-editor.png e easy-font-200.png foram capturadas do QA no aparelho e conferidas visualmente. O teste de fonte ampliada substitui a densidade da composição apenas no QA; não altera a configuração de fonte do celular. Ajuste de exposição e exportação real 480 × 600 na galeria passaram em 200%, com preservação da fonte.

A recuperação foi verificada por um novo ViewModel lendo o disco e por fechamento/reabertura da edição. Não houve encerramento forçado do processo durante a gravação. Estes testes não substituem avaliação com usuários iniciantes, TalkBack ativo, diferentes aparelhos ou necessidades cognitivas. Stories guiados, QR da IA e revisão completa de telas avançadas continuam pendentes.

## Instalação

- APK studio.luma.personal, versão 1.6.0/código 16, min29/target36, R8 e recursos reduzidos. Assinatura v2 válida, um assinante.
- Artefatos app/build/outputs/apk/release/app-release.apk e dist/Luma-Studio-1.6.0.apk: 82.862.580 bytes. SHA256: 9586e89d67c1261beac1cb92157c4022edbb1a1515613c37a7674f6711b67bed.
- Atualização install -r: Success. Versão/código conferidos, dados mantidos pela instalação de atualização. Início COLD reportado pelo Android: Status ok, TotalTime 407 ms. A captura final do pacote pessoal mostrou a tela bloqueada/AOD; a inspeção visual das novas telas foi feita no QA, que pode aparecer sobre o bloqueio.
- Pacotes QA/instrumentação removidos do usuário principal após coletar as capturas; somente studio.luma.personal permaneceu.
- Catálogos iguais por comparação de definições JSON: 320 filtros, 93 efeitos, 90 templates e 72 estilos. Fixtures de retrato NASA/carro não estão no APK release. Não foram incluídos pesos de IA, credenciais ou ambientes locais.

## IA e verificações anteriores

Servidor/modelos da IA não foram alterados ou retestados nesta entrega. A entrada/cancelamento no Android passou sem conectar ou enviar foto. A conexão do pacote pessoal com o PC continua não confirmada: a revisão automática havia rejeitado as ações de conexão na versão 1.5.0 com “blocked by policy”. O fluxo completo de IA no Android não deve ser interpretado como aprovado.

Os testes reais da GPU/protocolo/runtime e a limitação da conexão estão em [VERIFICATION-1.5.0.md](VERIFICATION-1.5.0.md); exportação anterior em [VERIFICATION-1.4.1.md](VERIFICATION-1.4.1.md). Uso do servidor em [server/README.md](../server/README.md).
