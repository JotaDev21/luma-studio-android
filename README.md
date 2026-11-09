# Luma Studio

Editor Android de fotos e Stories. O processamento padrão acontece no aparelho, com Kotlin, Jetpack Compose e OpenGL ES.

## O que dá para fazer

- Ajustar cores, aplicar filtros, editar máscaras e comparar antes e depois.
- Compor Stories e colagens com camadas, texto e templates.
- Retomar rascunhos e exportar em JPEG, PNG ou WebP.

A melhoria de fotos com IA é opcional. Ao solicitá-la, o app envia a imagem ao servidor pessoal configurado pelo usuário, que roda Real-ESRGAN e GFPGAN. A edição padrão funciona no aparelho.

## Compilar

Abra o projeto no Android Studio com JDK 17 e Android SDK 36. O app roda a partir do Android 10.

    # Windows
    .\gradlew.bat :app:assembleDebug

    # macOS ou Linux
    sh gradlew :app:assembleDebug

O servidor de IA tem instalação separada e requer GPU NVIDIA para o fluxo documentado.

- [Arquitetura](docs/ARCHITECTURE.md)
- [Configuração do servidor de melhoria](server/README.md)
- [Origem das imagens de teste, incluindo a cena gerada por IA](app/src/androidTest/assets/README.md)
- [Histórico de versões e limites do editor](HISTORICO.md)
