# Verificação da entrega 0.3.0 — 05/10/2026

## Confirmado

- :core:test: 8 testes, zero falhas/erros. Catálogos reais de 30 filtros, 18 efeitos e 20 templates validados; round-trip, parâmetros inválidos, histórico e compatibilidade com projetos schema 1.
- Renderer: 9 testes executados no Samsung SM-S731B, Android 16, via ADB Wi-Fi. Identidade/orientação, alpha de PNG, intensidade zero, crop/rotação, preview/exportação, stack determinística/reordenação/desativação, bloom, composição de camadas, blend/opacidade/visibilidade, escala, texto/transparência, bloqueio de fotos faltantes e preservação das fontes.
- App: 4 testes executados e aprovados no mesmo aparelho. Catálogos/persistência/favoritos, JPEG/PNG/WebP, edição de foto com desfazer/refazer/salvar/reabrir e Story por template com preenchimento de fotos, edição de texto, efeitos, histórico, thumbnail e reabertura. Exportações de diagnóstico foram apagadas após conferir dimensões; fontes pertencem apenas ao teste.
- Arraste de camada e undo no Story: teste adicional aprovado após os quatro testes completos.
- :app:lintDebug: zero erros, 12 avisos (versões de dependências, recomendação de atributo de backup legado e sugestões KTX). Backup automático já está desativado.
- :app:assembleRelease: compilação e redução de código/recursos concluídas. APK 0.3.0, versionCode 3, 2.471.713 bytes (aproximadamente 2,36 MiB).
- apksigner verify --verbose: assinatura v2 validada, um assinante. A mesma chave local da instalação anterior foi mantida.
- Manifesto: mínimo API 29, target API 36, sem permissão INTERNET nem leitura geral da galeria.
- Interface Story inspecionada em captura real do Compose no aparelho, com imagens de diagnóstico próprias: docs/story-editor.png.
- APK compacto atualizado no dispositivo com instalação sobre a versão anterior: Success. Pacote studio.luma.personal confirmado com versionCode 3/versionName 0.3.0. Inicialização a frio retornou Status: ok em 801 ms. A versão pessoal não foi desinstalada e seus arquivos privados foram preservados pelo Android.

## Condições dos testes e limites

Os testes usam o aplicativo separado studio.luma.personal.qa, sem acessar projetos/fotos do aplicativo pessoal. O aparelho estava bloqueado por PIN. Somente a Activity de teste recebeu flags para exibir a própria interface acima da tela de bloqueio e manter a tela ligada; o PIN não foi removido/desbloqueado e o app de uso pessoal não possui essas flags.

Uma repetição encontrou uma transição de tela de bloqueio antes do clique de foto. O preparo do teste passou a aguardar a hierarquia de UI e manter a tela ligada; a execução completa seguinte aprovou os quatro testes. As tarefas instrumentadas rodam com --no-parallel para não disputarem a mesma Activity/dispositivo.

A validação automatizada cobre os fluxos descritos. Não equivale a uma sessão longa com fotos de todos os tamanhos/aparelhos, perfil de desempenho ou teste de acessibilidade completo. O APK compacto foi compilado, assinado, instalado e iniciado; os fluxos interativos instrumentados usam a versão QA, sem redução de código.

## Arquivos de evidência

- phase3-app-final-tests.log: 8 testes JVM e 4 testes de app aprovados.
- phase3-final-device-tests.log: 9 testes de renderer aprovados; a falha transitória de UI desta tentativa foi resolvida/verificada no log seguinte.
- phase3-gesture-test.log: verificação adicional de arraste de camada e undo no fluxo Story.
- phase3-release.log: lint e APK compacto.
- Relatórios completos em core/build/reports/tests/test, renderer/build/reports/androidTests e app/build/reports/androidTests.
