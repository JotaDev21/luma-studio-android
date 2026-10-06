# Seleção automática de pessoa e fundo — 0.6.0

PeopleSegmenter usa ML Kit Selfie Segmentation 16.0.0-beta6 em SINGLE_IMAGE_MODE, com mapa bruto de confiança. O modelo é incorporado ao APK; AndroidManifest remove INTERNET e ACCESS_NETWORK_STATE trazidos por dependências. A inferência recebe apenas o Bitmap privado, orientado pelo EXIF, com lado máximo 1280. Nenhum original é modificado. O cliente é fechado depois de terminar a tarefa, inclusive em caso de falha/cancelamento do coroutine. Resultado sem área confiável retorna orientação para usar um retrato/pincel e não cria máscara.

RasterMask é um mapa de até 256 × 256, quantizado em 8 bits e armazenado em zlib/Base64 dentro do JSON do projeto. Validação limita dimensões, tamanho codificado e expansão, exige exatamente width × height bytes e rejeita dados truncados/corrompidos ou bytes excedentes. O schema de projeto é 6, mantendo compatibilidade com 1–5. O mapa é ligado à fonte antes do crop/flip/rotação; exportação redimensiona a cobertura guardada e não executa outra inferência. A qualidade da borda é limitada pela resolução do mapa e pelo modelo.

LocalMask do tipo person requer RasterMask. Threshold e feather transformam confiança em cobertura contínua; os traços vetoriais refinam a cobertura por adição/borracha. Pessoa e fundo reutilizam a mesma confiança, mas mantêm máscaras/correções próprias. Inverted aplica a seleção do fundo depois das correções; para ela, o ViewModel converte os traços para que Pintar adicione à área escolhida e Apagar remova. Modificar a inversão depois de pintar inverte o resultado inteiro.

Cutout multiplica o alpha de entrada pela cobertura/intensidade. O mesmo passe mantém os ajustes locais; múltiplos recortes se intersectam. O alpha permanece premultiplicado. Em Stories, fotos recortadas mostram o canvas e camadas inferiores. PNG/WebP mantêm alpha; JPEG compõe sobre branco antes de comprimir, informado na escolha do formato. Quadriculado, comparação original e sobreposição rosa são apenas UI.

Histórico guarda mapa e correções como parâmetros. Desfazer/refazer, salvar/reabrir, fotos fixas de templates e packs conservam a seleção. Placeholder ou troca de fonte limpa máscaras específicas. Se undo troca/remove/bloqueia a foto enquanto seu editor isolado está aberto, ele retorna à composição para evitar pintar sobre outra fonte.

O SDK é beta e dedicado a pessoas, com melhor resultado em retratos. Não há seleção individual de várias pessoas, segmentação de animais/objetos nem promessa de recorte perfeito em cabelos finos, objetos próximos, oclusões ou fotografias pouco nítidas. Correções manuais são parte do fluxo. Smart crop/atribuição de fotos às molduras estão disponíveis em 0.7.0 (SMART-FRAMING.md). Referência estética e outros operadores de IA seguem pendentes.

## Fontes primárias

- [Integração Android, modelo incorporado, modos e mapa bruto](https://developers.google.com/ml-kit/vision/selfie-segmentation/android).
- [Descrição do modelo e limitações](https://developers.google.com/ml-kit/vision/selfie-segmentation).
- [Versões oficiais do ML Kit](https://developers.google.com/ml-kit/release-notes).
- [Termos do ML Kit](https://developers.google.com/ml-kit/terms).

O fixture natural dos testes é o [retrato oficial de Christina Koch](https://www.nasa.gov/image-article/nasa-astronaut-christina-koch/), crédito NASA, usado conforme as [diretrizes de imagens e mídia](https://www.nasa.gov/nasa-brand-center/images-and-media/). Está somente no APK de testes e não faz parte do aplicativo pessoal nem de seu branding. Detalhes em app/src/androidTest/assets/README.md.
