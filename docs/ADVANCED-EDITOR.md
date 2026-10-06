# Editor completo 1.0.0

## Dados e processamento

Schema de projeto 8 lê versões 1–8 com defaults. Parâmetros `ColorTreatment`, `HslBand`, `CubeLut`, `GradientSpec`, `DrawingStroke` e `HistorySnapshot` são JSON. Fontes, imagens e LUTs originais recebem nomes internos validados, sem caminhos fornecidos pelo pack. Filtros, estilos, templates e máscaras preservam os novos tratamentos.

GPU ES2: ajustes de detalhe, ajustes básicos, curvas manuais, HSL e gradação/matriz/LUT, filtro e curvas legadas, máscaras, efeitos. Texturas de curvas têm 256 amostras; LUT trilinear usa uma textura 2D com fatias azuis horizontais, verde nas linhas e vermelho no eixo rápido. Fonte conceitual da ordem/formato: [implementação oficial OpenColorIO para IRIDAS Cube](https://github.com/AcademySoftwareFoundation/OpenColorIO/blob/main/src/OpenColorIO/fileformats/FileFormatIridasCube.cpp). Este app tem implementação própria, SDR/8 bits; não incorpora esse código.

HSL usa faixas centradas em vermelho, laranja, amarelo, verde, ciano, azul, violeta e magenta, interpoladas suavemente. Nitidez/textura usam diferenças espaciais; clareza usa escala mais ampla; redução de ruído usa média bilateral aproximada; dehaze usa aproximação de transmissão atmosférica. São operadores fotográficos locais, sem modelo de restauração por IA. Máscaras processam o tratamento da região e misturam na cobertura; recorte de pessoa conserva alpha. Exportação usa o mesmo renderer e orçamento de memória, com efeito definido em escala lógica e semente persistida.

69 presets originais, 59 operadores (58 de catálogo e texture disponível mediante importação); controles mostram somente parâmetros suportados. Gate weave, exposição/flicker e deslocamento de blocos são fotos estáticas; não prometem datamosh temporal nem vídeo. Texturas e overlays são gerados matematicamente. Timestamp captura a data ao inserir, fica editável e não muda ao exportar. Stack respeita ordem, visibilidade e intensidade. Sem extração de recursos proprietários.

## Referência

Amostras limitadas de pixels visíveis produzem média/desvio RGB. Ganhos e offsets limitados sugerem correspondência de cor/contraste, sem identificação de filtro ou análise semântica. Comparação antes/sugestão/referência, aplicar/cancelar, um passo de undo. Parâmetros permanecem editáveis. Não estima fielmente grain/bloom, composição ou iluminação física; esses recursos podem ser escolhidos no editor. Funciona sobre a criação aberta e não envia fotos.

## Recursos e composição

Meus recursos importa `.cube` 3D 2–32, TTF/OTF até 5 MB e imagens até 16 MB/32 MP. Packs ZIP contêm manifesto e somente arquivos referenciados, limitados a 64 MB e 80 arquivos. Fontes são validadas e carregadas pelo [Typeface do Android](https://developer.android.com/reference/android/graphics/Typeface#createFromFile(java.io.File)). Templates/styles incluem fontes, fundo e texturas usadas, remapeadas na importação. Fontes de projetos continuam disponíveis após excluir recurso/pack. Remoção só coleta arquivos globais sem referência no catálogo; dados da galeria permanecem intactos.

Degradês, fundos desfocados, tipografia com gradiente/brilho/desfoque/curva, desenhos vetoriais e cards de música seguem o mesmo formato de camada. Borracha afeta apenas a camada de desenho. Alinhamento respeita bloqueio. Snapping ao soltar usa bordas, centro e margens de 12%; ajustes manuais continuam disponíveis. Texto curvo desenha uma linha, substituindo quebras por espaços; a altura da caixa limita o desenho.

## Exportação e histórico

Dimensões explícitas mantêm a imagem inteira com margens transparentes (brancas em JPEG). Qualidade e EXIF opcional preservam as regras anteriores. Lote de até 20 fotos processa sequencialmente, com projeto por fonte, opção de original ou estilo apenas fotográfico; se um item falhar, informa quantos já concluíram. Pode parar após a foto em processamento. Sem compartilhar automaticamente.

Projetos guardam últimos 20 estados de cada direção do histórico quando salvos; sessão tem até 80. Fonte, imagens/fonte tipográfica e máscara permanecem independentes dos pixels exportados. Catálogo inicial: 120 filtros (30 bases e tratamentos de janela/pôr-do-sol/noite), 69 efeitos, 42 templates, 24 estilos. Bibliotecas podem crescer por JSON e packs pessoais.
