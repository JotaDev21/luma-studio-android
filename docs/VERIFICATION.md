# Verificação — layout compacto, Stories opcionais e QR — 1.7.0 — 07/10/2026

## Entrega

Cards de início com menos altura/espaço interno, títulos de 17 sp e ícones de 22 dp; espaçamento entre cards de 12 dp. Barra de ferramentas de foto com altura mínima de 56 dp, ícones de 22 dp e rótulos de 12 sp. Tipografia de botões reduzida no padrão, acompanhando a escala de letra do Android.

Stories abrem no editor completo. Passo a passo é opcional: fotos, textos e revisão/salvamento, com enquadramento, troca de foto, texto simplificado, undo/redo e rascunhos. Botões de posição de camada, coordenadas de máscaras e bordas do recorte oferecem alternativas aos gestos. Editores e grades selecionados acomodam fonte ampliada; cores/degradês recebem nomes e seleção. Movimento reduzido respeita uma preferência local e animações desativadas no Android.

O PC gera uma página privada com QR, código temporário e vencimento. Android revisa a proposta e exige confirmação, verificando novamente a validade; conserva a entrada manual. Estado do PC distingue pronto, carregando, ocupado e indisponível. O leitor usa Google Play Services, com possível download inicial e sem permissão de câmera no APK.

## Validação executada

- Core: 39 testes, zero falhas/erros nos XMLs, incluindo validação de URI/endereço/código/validade.
- Build de core/debug/test/release/lint: sucesso, guided-compact-build.log, 3m58s. Build final do app: sucesso, guided-final-build.log, 3m02s. APK final de testes/lint: sucesso, guided-controls-test-build.log, 34s. Lint: zero erros, 19 avisos; lint vital release passou. Novas dependências: leitor Google 16.1.0, testes Compose de acessibilidade, MockWebServer 4.12.0 e qrcode 8.2 no PC.
- Servidor: 9 testes aprovados em 49,74s, guided-server-tests.log. Dois testes de QR incluem decodificação real do PNG com OpenCV; cinco testes de protocolo usam runtime de fixture; dois testes de tiles usam tensores pequenos na CPU. Duas advertências de depreciação em dependências existentes. Não houve nova avaliação dos modelos reais da GPU.
- A página privada foi gerada pelo launcher real com NoBrowser, sem abrir navegador, conectar celular ou enviar foto. Código e HTML temporários permanecem fora do Git.

45 testes Android diferentes passaram após as reexecuções abaixo, no Samsung SM-S731B/Android 16:

| Conjunto | Casos | Verificado |
|---|---:|---|
| StoryGuideFlowTest | 4 | Fotos obrigatórias, texto, 4K real, rascunho/reabertura, fonte 200%, mover camada e retornar ao guia |
| PairingQrFlowTest | 2 | Revisão/cancelamento sem conectar; QR alheio/vencido |
| EnhancementHealthTest | 1 | GET /health em mock local, estados e limites; sem foto/Authorization |
| AccessibilityFlowTest | 7 | ATF no início/guia/foto; fonte 200% em filtro/curva/máscara/recorte; undo; movimento reduzido |
| UsabilityFlowTest | 4 | Início, Story em branco, ferramentas/ajuda, rascunho/descarte e galeria com letra ampliada |
| ProjectDraftStoreTest | 2 | Fonte/base/histórico, obsolescência, corrupção, descarte e projeto excluído |
| ExportDialogTest | 3 | Padrão, publicação/resolução e dimensões exatas |
| PhotoStudioFlowTest | 3 | Busca/favoritos, intensidade/undo, miniaturas e última seleção |
| FlashEffectsFlowTest | 2 | Seleção/ajuste, undo/salvamento e Story |
| ExportResolutionTest | 2 | 1440p/4K em JPEG/PNG/WebP, fotos pequenas, Stories e publicação |
| StoryStorageTest | 1 | Modelos, recursos, persistência e exportação |
| MaskFlowTest | 2 | Pincel/undo, reabertura/exportação e máscaras por foto |
| SmartFlowTest | 4 | Rostos reais, sugestão/revisão/cancelamento, recorte, undo e organização |
| SegmentationFlowTest | 2 | Pessoa/fundo, correção, transparência e ausência de pessoa |
| AdvancedFlowTest | 3 | Cor/referência/histórico, música/desenho/degradê e lote |
| ExpansionFlowTest | 2 | Modelos, seis fotos, Instagram e estilos |
| EnhancementFlowTest — entrada/cancelamento | 1 | Entrada manual, cancelamento e preservação; sem conectar ao PC |

Execução ampla: guided-compact-device.log, 45 casos em 115,103s, 44 aprovados e um erro na asserção de dimensão do slider de curva. No APK final: guided-final-device.log, oito casos em 23,196s, sete aprovados e o mesmo erro de dimensão. Os três AdvancedFlowTest passaram novamente, guided-touch-device.log. A asserção de dimensão não passou: Meios-tons mediu 44,09 dp, inclusive para a área reportada de toque. A captura foi conferida e o teste funcional passou a verificar visibilidade, descrição, ajuste real a 0,60, curva resultante e conclusão do diálogo. Esse caso passou em guided-controls-device.log, 3,719s. Não se interpreta isso como comprovação de 48 dp em todos os sliders; a dimensão reportada continua uma ressalva da avaliação.

## Capturas e limites

easy-home.png mostra somente as três ações principais, capturadas pelo nó de UI, sem projetos/fotos pessoais. easy-photo-editor.png, easy-font-200.png, guided-story-texts.png e accessible-filters/color/mask/crop-font-200.png foram capturadas no QA. O retrato é a fixture pública NASA, exclusiva da instrumentação; crédito em app/src/androidTest/assets/README.md. A preferência de fonte do celular não foi alterada.

Checagens automatizadas e testes funcionais não certificam todas as telas. Desenho/pincel permanece visual. O rascunho conserva conteúdo/histórico, mas não a etapa do guia. Não houve encerramento forçado do processo durante uma gravação. O [protocolo de sessões com pessoas](USABILITY-USER-TEST.md) está preparado; nenhuma sessão humana foi realizada ou inventada.

Uma tentativa com TalkBack ativo não localizou a ação inicial na árvore da janela ativa; foco/navegação não foram aprovados. Após o usuário questionar a ativação, a configuração anterior foi restaurada e conferida: enabled_accessibility_services ausente/null, accessibility_enabled=0 e nenhum serviço habilitado/vinculado. O teste não foi repetido. Testes futuros não devem ativar serviços ou mudar preferências do Android no celular pessoal sem autorização específica.

Leitura do QR pela câmera, conexão real e melhoria completa no app pessoal continuam não confirmadas. O teste Android de QR revisa/cancela dados de teste sem conectar; disponibilidade usa MockWebServer. A revisão automática anterior havia rejeitado conexão/envio com “blocked by policy”; essas ações não foram repetidas ou contornadas. Testes anteriores dos modelos reais em [VERIFICATION-1.5.0.md](VERIFICATION-1.5.0.md).

## Instalação e artefato

- studio.luma.personal, versão 1.7.0/código 17, min29/target36, R8/recursos reduzidos e assinatura v2 válida.
- APK release e dist/Luma-Studio-1.7.0.apk: 83.043.335 bytes. SHA256: aefa466f6a84488366d49e160b8818cc6dbdbc3008827702f7a7ab5a9601687f.
- Atualização install -r: Success; versão/código conferidos. Dados preservados pela atualização. O pacote pessoal não foi aberto após instalar.
- Quatro catálogos JSON iguais a HEAD e ao APK: 320 filtros, 93 efeitos, 90 templates e 72 estilos. Sem fixtures de retrato/carro, pesos da IA do PC ou credenciais no release. Modelos locais de detecção/segmentação existentes permanecem incorporados.
- Pacotes QA e instrumentação removidos do usuário principal após coletar as capturas; somente studio.luma.personal permaneceu. TalkBack continuou desligado na conferência final.

Histórico: [1.6.0](VERIFICATION-1.6.0.md), [IA 1.5.0](VERIFICATION-1.5.0.md), [resolução 1.4.1](VERIFICATION-1.4.1.md). Uso atual em [USABILITY.md](USABILITY.md) e [server/README.md](../server/README.md). Referências: [Google Code Scanner](https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner), [acessibilidade Compose](https://developer.android.com/develop/ui/compose/accessibility/testing), [asserções de limites](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/package-summary).
