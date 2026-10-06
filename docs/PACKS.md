# Packs Luma — formato 1

Um arquivo ZIP com extensão sugerida .lumapack. Extensões .filterpack, .storypack, .stylepack ou .zip também funcionam se o conteúdo seguir este formato. O seletor lê o conteúdo, sem depender da extensão.

O ZIP contém manifest.json UTF-8 e somente arquivos referenciados, em assets/photo_UUID, assets/font_UUID ou assets/lut_UUID. Não há scripts, plugins executáveis, caminhos arbitrários, diretórios extras ou extração de recursos proprietários. Thumbnails são calculados pelo app; não é necessário incluir imagens de preview.

Exemplo mínimo:

```json
{
  "schemaVersion": 1,
  "meta": { "id": "my-pack", "name": "Meu pack", "category": "Personal", "version": 1, "tags": ["film"] },
  "filters": [
    { "meta": { "id": "my-warm", "name": "Warm", "category": "Custom" }, "adjustments": { "temperature": 0.2, "contrast": 1.1 } }
  ],
  "effects": [],
  "templates": [],
  "styles": []
}
```

Filtros, efeitos, templates e estilos usam os modelos serializáveis do core. Os IDs dos itens devem ser únicos dentro do pack. O importador atribui novos IDs ao instalar para preservar o catálogo existente e remapeia imagens, fontes e LUTs para novos nomes privados, inclusive referências aninhadas. Definições de filtros/efeitos em templates/estilos são incorporadas, sem referências mutáveis a outro catálogo.

Foto placeholder: type photo, asset null e placeholder true. Foto fixa: asset photo_UUID com arquivo assets/photo_UUID no ZIP. Todas as imagens referenciadas precisam existir e todas as imagens incluídas precisam ser referenciadas. PNG/JPEG/WebP decodificáveis no Android são úteis; o nome privado não exige extensão.

StylePreset aceita effectLayers para preservar intensidade/visibilidade/ordem ou o campo legado effects para definições com intensidade total. Seus elementos gráficos podem ser texto, sticker, shape ou drawing, com canvas width/height de referência. Fotos fixas pertencem a templates, não a estilos. FilterPreset opcionalmente contém savedAdjustments/savedIntensity/savedColor: ao aplicar um filtro pessoal, esses valores recompõem seu tratamento de cor exato.

Limites por pack: 300 itens, 80 arquivos, manifesto 2 MB, arquivo 16 MB, imagem 32 MP, fonte 5 MB e LUT 4 MB, 64 MB de entrada e de expansão. Arquivos duplicados, caminhos absolutos ou com traversal, conteúdo desconhecido, versões incompatíveis e parâmetros fora das faixas são rejeitados. Imagens, fontes e LUTs são conferidos em staging privado antes do commit do catálogo por AtomicFile. Falha remove arquivos novos e mantém a biblioteca anterior.

Exportar minha biblioteca usa somente itens pessoais/importados e inclui as fotos fixas dos templates. Placeholders e fotos dos projetos não entram no arquivo. O usuário escolhe explicitamente o destino pelo Android; não há upload pelo app. Bibliotecas maiores que os limites de um pack não podem ser exportadas integralmente nesta versão. Packs e itens podem ser removidos na biblioteca; projetos guardam suas próprias cópias. Exportação de seleção de itens é uma evolução futura.

Recursos aparecem no campo resources como LocalAsset(meta,kind,asset), com kind font/lut/sticker/texture/frame. LUT 3D pode também estar incorporado no ColorTreatment do preset. Typefaces são validadas pelo Android, `.cube` pelo parser do core. Somente os nomes gerados pelo app são permitidos; nenhuma entrada do ZIP é executada.
