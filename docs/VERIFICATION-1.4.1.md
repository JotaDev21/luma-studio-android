# Verificação da resolução de exportação — 1.4.1 — 07/10/2026

## Correção

Fotos menores não eram ampliadas pelo renderizador, e o canvas de publicação fixo substituía a resolução escolhida no último passo. A exportação agora aplica o tamanho final solicitado; formatos de publicação mantêm a proporção e acompanham o maior lado escolhido. Em 9:16: 1080p = 1080 × 1920, 1440p = 1440 × 2560, 4K = 2160 × 3840. Personalizado usa largura/altura exatas e oculta o seletor de resolução. Original/Livre conserva o tamanho nativo.

Stories são compostos no tamanho necessário para caber no canvas final. Quando já têm as dimensões finais, a exportação evita criar outro bitmap. Fotos menores são ampliadas depois do tratamento; isso não inventa detalhes. A cópia privada da fonte não é modificada. JPEG continua preenchendo transparência com branco; PNG/WebP preservam transparência.

## Validação

- 35 testes JVM/core passaram, zero falhas/erros. Quatro novos testes cobrem ampliação de fontes pequenas, retrato/paisagem, Story/feed/quadrado, proporções diferentes de conteúdo/canvas e limites de dimensão/área.
- ExportResolutionTest passou 2/2: mediu as dimensões codificadas de JPEG/PNG/WebP, fotos menores e Stories, em 1440p/4K, Livre/publicação, rotação e tamanho personalizado. Excluiu suas próprias exportações e projetos de QA; conferiu que os bytes da foto original não mudaram.
- ExportDialogTest passou 2/2: conferiu o tamanho exibido e as opções enviadas ao exportador, com resolução escolhida antes/depois do formato Story e dimensões personalizadas após 4K.
- StoryStorageTest passou 1/1: templates, assets, persistência e exportação nos três formatos, preservando fonte e dimensões.
- Rodada Android completa: OK (5 tests), 12,482s, Samsung SM-S731B/Android 16; registro local export-resolution-device.log. Houve autorização atual explícita do usuário antes de qualquer operação no celular.
- Compilação Android/core/release/lint: BUILD SUCCESSFUL em 6m16s, registro export-resolution-build.log. Conferência final dos arquivos atuais: BUILD SUCCESSFUL em 49s, export-resolution-final-build.log. Lint: zero erros e 16 avisos existentes. APK de instrumentação compilado; lint vital release passou.

## Artefato e instalação

- studio.luma.personal, versão 1.4.1/código 14; min29/target36. R8 e recursos reduzidos; assinatura v2 válida, um assinante.
- APK: app/build/outputs/apk/release/app-release.apk e dist/Luma-Studio-1.4.1.apk; 82.542.965 bytes.
- SHA256: 846450d6267e7964bebbe5d8288f0632d1663a028f8cc974a9a3a10655dd85ea.
- Os quatro catálogos dentro do APK são idênticos aos arquivos do projeto: 320 filtros, 93 efeitos, 90 templates e 72 estilos. Fixtures de testes ausentes do release.
- Atualização instalada com preservação dos dados (install -r: Success); versão 1.4.1/código 14 conferida. Início COLD: Status ok, TotalTime 405ms. QA/instrumentação removidos; apenas o pacote pessoal no usuário principal.

## Limites

Ampliação não recupera detalhes de fontes pequenas. Resoluções muito grandes continuam sujeitas aos limites de memória, GPU e 32 MP; o aplicativo informa erro em vez de salvar silenciosamente em outra resolução. Nesta entrega não foram executados novamente os testes de filtros/flash sem mudanças; os resultados anteriores permanecem documentados abaixo.

A verificação da entrega anterior, com os efeitos de flash e os testes de aparelho daquela versão, está em [VERIFICATION-1.4.0.md](VERIFICATION-1.4.0.md).
