# Verificação da melhoria com IA — 1.5.0 — 07/10/2026

## Entrega

Botão Melhorar com IA no editor de foto, conexão manual por endereço/código temporário, credencial cifrada no Android Keystore, ampliação de até 2×/4×, força e restauração de rostos opcionais. Comparação deslizante permite aceitar como novo projeto ou descartar o resultado. O projeto anterior e sua fonte são conservados. A imagem editada é enviada em PNG sem EXIF/GPS ao servidor pessoal no PC.

Serviço FastAPI autenticado, uma inferência por vez, Real-ESRGAN x4plus e GFPGAN v1.4 reais. CUDA obrigatória, tiles de 192 px, limites de entrada/saída e tratamento explícito de falhas de tiles/rostos. Imagens e resultados passam pela memória; modelos e configurações ficam em .tools/, fora do Git. Launchers de iniciar/parar e mostrar conexão foram adicionados na raiz.

## Validação executada

- 37 testes JVM/core passaram, zero falhas/erros. Os dois novos testes de endereço aceitam LAN privada/HTTPS e recusam HTTP público, credenciais, caminhos, parâmetros e portas inválidas.
- 5 testes do protocolo passaram: código temporário de uso único/renovação autenticada, pedidos não autorizados/inválidos, dimensões/pixels/força/transparência/ausência de EXIF, falha de modelo e exclusão mútua da GPU. Registro local ai-service-tests-final.log.
- 2 testes do runtime passaram: tiles cobrem a imagem e as bordas sem emendas; falha num tile posterior é propagada, sem reaproveitar pixels do anterior. Registro ai-runtime-tests.log.
- Duas verificações com os modelos reais na RTX 5060 de 8 GB, PyTorch 2.8.0/CUDA 12.8, torchvision 0.23.0. Entrada pública NASA degradada, 240 × 300; saída PNG 960 × 1200. Real-ESRGAN: 7,562 s, diferença média 5,544 níveis em relação à ampliação bicúbica. Com GFPGAN: 18,906 s, um rosto restaurado e diferença média 5,498. Fonte inalterada. Tempos de teste incluem inicializações e variam conforme foto/carga; não são promessa de desempenho. Registro ai-gpu-verification-final.log; imagem em AI-ENHANCEMENT.md.
- Android no Samsung SM-S731B/Android 16: PhotoStudioFlowTest 3/3, FlashEffectsFlowTest 2/2, ExportResolutionTest 2/2, ExportDialogTest 2/2 e StoryStorageTest 1/1. OK (10 tests), 30,591 s, ai-regression-device.log. Filtros, efeitos, persistência e exportação 1440p/4K continuaram passando após a inclusão do botão. Capturas reais do editor conferidas visualmente.
- EnhancementFlowTest#entryShowsManualConnectionAndCancelPreservesPhoto passou no aparelho: botão acessível, campos de conexão visíveis, conectar desabilitado sem os dados, cancelar conserva o projeto e os bytes da foto. OK (1 test), 2,265 s, ai-entry-device.log. Esta verificação não faz conexão ou envio ao PC. APK de testes atualizado compilou em 1m54s, ai-entry-test-build.log.
- Compilação core/debug/test APK/release/lint: BUILD SUCCESSFUL. Conferência final dos arquivos atuais em 4m42s, ai-current-build.log. Lint: zero erros, 18 avisos (16 anteriores e duas sugestões de extensões KTX no armazenamento da conexão). Lint vital release passou.
- Os quatro pesos oficiais foram baixados e seus hashes conferidos por download_models.py. Nenhum peso, credencial ou ambiente virtual foi incluído no Git/APK.

## Instalação e configuração

- APK studio.luma.personal, versão 1.5.0/código 15, min29/target36, R8 e recursos reduzidos. Assinatura v2 válida, um assinante.
- Artefatos app/build/outputs/apk/release/app-release.apk e dist/Luma-Studio-1.5.0.apk: 82.829.812 bytes. SHA256: 44597f6646e3f50345b4bd4f9fe78cb00bf27fe4575f145d9c2ce52d326f04b1.
- Atualização install -r: Success, versão/código conferidos, dados preservados. Início COLD: Status ok, TotalTime 309 ms.
- Pacotes QA e de instrumentação removidos após a coleta das capturas; somente studio.luma.personal permaneceu no usuário principal.
- Fixtures de retrato NASA e carro de testes ausentes do APK release. Catálogos conservados: 320 filtros, 93 efeitos, 90 templates, 72 estilos.
- Servidor configurado na LAN do PC, http://192.168.100.190:8765, com firewall restrito ao programa Python, porta, endereço do PC e IP do celular 192.168.100.189. Serviço em segundo plano; sem tarefa de inicialização automática do Windows. Endereços podem mudar por DHCP; nesse caso é necessário ajustar a conexão e a regra.

## Limitações da verificação

O usuário autorizou expressamente abrir, testar, conectar e instalar no celular nesta sessão. A instalação e os testes de regressão foram executados. A revisão automática rejeitou o comando de instrumentação que conectaria ao servidor e executaria o fluxo completo de IA, e também rejeitou a conexão por Intent no pacote pessoal. O retorno informou apenas “blocked by policy”, sem justificativa adicional. Essas ações não foram executadas, e a conexão do pacote pessoal não está confirmada. O teste connectCompareDiscardAndSaveAsCopyPreserveTheOriginal foi incluído e compilado, mas não deve ser interpretado como aprovado no aparelho.

A melhoria real dos modelos foi verificada separadamente no PC. A conexão manual deve ser feita em Melhorar com IA usando o endereço do servidor e o código temporário mostrado por Ver conexão Luma IA.cmd. Não se promete equivalência com Remini nem reconstrução fiel de detalhes ausentes. A IA pode alterar traços e criar aparência artificial; a comparação e a força existem para revisar o resultado. O fluxo de IA recebe fotos simples; a cópia resultante pode ser usada depois em Stories pelo fluxo existente.

A verificação anterior da correção de exportação permanece em [VERIFICATION-1.4.1.md](VERIFICATION-1.4.1.md). Instruções e referências em [AI-ENHANCEMENT.md](AI-ENHANCEMENT.md) e [server/README.md](../server/README.md).
