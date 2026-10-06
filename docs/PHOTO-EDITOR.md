# Editor de fotos — 1.3.0

Depois de importar ou reabrir uma foto, o editor mostra a imagem sobre fundo grafite, comparação sobre a foto, salvar/exportar no topo e as ferramentas no painel inferior. O título do look fica junto à foto; o botão ao lado remove o filtro sem apagar ajustes pessoais. Modo Pro continua abrindo as ferramentas avançadas existentes.

Looks apresenta cards da própria foto e seleção em pêssego. Explorar looks abre uma grade com busca por palavras do nome, tags e coleção, ignorando acentos e incluindo termos como filme/P&B, coração para favoritos, intensidade e confirmação. Escolher um card aplica a seleção e fecha o teclado; escolher uma coleção também retira o foco da busca. Ao voltar à foto, o carrossel acompanha o look selecionado. Favoritos persistem na biblioteca local.

O catálogo contém 320 filtros, incluindo 48 inspirados em Instagram (EXPANDED-CATALOG.md) e 32 Flash 80/90 (detalhes em JDM-FILTERS.md). Os primeiros 120 IDs e definições foram preservados. Os 120 novos pertencem a 12 famílias: Editorial, Vintage, Nature, Neon, Matte, Soft, Golden, Cool, Noir, Coastal, Urban e Nordic. Cada look tem curvas RGB e por canal, tratamento seletivo HSL, transição de altas luzes e gradação; Vintage inclui grão fino determinístico. Os dados e o gerador ficam no projeto. Não exigem download ou recursos pagos.

## Correções de fluxo

- A seleção de um filtro sem arquivos é aplicada imediatamente ao estado. Filtros com recursos têm uma tarefa cancelável e uma sequência de pedidos: uma tarefa antiga não pode substituir uma seleção mais recente nem atingir outro projeto.
- A renderização principal cancela/debounce pedidos anteriores e confere a geração depois de produzir a foto e a comparação. A exportação continua recalculando com o mesmo shader na resolução escolhida.
- Miniaturas usam outro renderer EGL, duas tarefas simultâneas, lado de 224 px e cache de 60 imagens. A fila das miniaturas não disputa a thread que produz a foto principal; ambas continuam usando a GPU física do aparelho.
- A chave de contexto muda com fonte, transformação, ajustes, cor, máscaras e efeitos. Respostas antigas são descartadas. Escolher outro filtro ou mudar sua intensidade não invalida todas as alternativas.
- A imagem de cada alternativa considera seu tratamento, intensidade salva e eventuais ajustes/cor de um filtro pessoal, além da edição atual da foto. O card Original conserva os ajustes pessoais.
- Trocar ferramenta, trocar filtro ou concluir a galeria fecha o gesto pendente no histórico. Desfazer uma troca de filtro conserva o ajuste anterior; o passo seguinte pode desfazer esse ajuste.

## Verificação

PhotoStudioFlowTest exercita busca por duas palavras, termos localizados e nomes sem acento, favorito, saída do teclado, escolha/intensidade/desfazer; comparação de pixels de miniaturas com crop, ajustes, cor manual e filtro pessoal; invalidação do cache; seleções rápidas enquanto cards carregam; e renderização distinta dos 120 novos looks. Usa o pacote QA separado no Samsung autorizado. As capturas usam uma foto NASA exclusiva dos testes, com crédito em app/src/androidTest/assets/README.md.

O isolamento das filas reduz a competição com a foto principal. Não representa uma medição de FPS nem garante tempo constante em todos os aparelhos ou combinações de efeitos. Os limites de SDR, memória, decode e GPU continuam descritos em README.md.
