# Facilidade de uso e acessibilidade — avaliação de 07/10/2026

Escopo: código das telas da versão 1.5.0 e capturas existentes do aparelho. Esta avaliação não incluiu sessões com usuários iniciantes, TalkBack ativo, fonte ampliada ou testes com pessoas com deficiência. Testes funcionais aprovados não demonstram, por si só, facilidade de uso ou acessibilidade.

O app já oferece prévias reais de filtros, favoritos/busca, comparação, desfazer/refazer e preservação da fonte. O principal trabalho identificado foi tornar essas capacidades fáceis de encontrar e entender. As propostas abaixo registram a avaliação inicial da versão 1.5.0; parte delas foi implementada na 1.6.0, conforme o acompanhamento a seguir.

## Acompanhamento — 1.6.0

- Implementados: início com três ações; modo simples com quatro ferramentas; acesso ao catálogo completo e ferramentas avançadas; rótulos de ajustes; salvar na galeria com opções avançadas recolhidas; rascunhos privados recuperáveis; ajuda curta.
- Melhorados: textos e áreas de toque nos fluxos principais, estado de rascunho, categorias, efeitos e recorte. O editor de foto oferece rolagem com fonte ampliada ou janela baixa.
- Verificado no aparelho: ações iniciais, troca entre modo simples/completo, acesso à ajuda, recuperação em um novo ViewModel, descarte preservando projeto/fonte e ajuste/salvamento na galeria com fonte de 200%.
- Pendentes: revisão completa de telas avançadas e Stories com fonte ampliada, TalkBack, movimento reduzido, wizard de Stories, QR da IA e avaliação com pessoas. A recuperação em novo ViewModel verifica a leitura do disco; não simula encerramento abrupto do processo pelo Android.

Detalhes da implementação em [USABILITY.md](USABILITY.md) e resultados desta entrega em [VERIFICATION.md](VERIFICATION.md).

## Prioridade 1: começar e concluir sem ajuda

### Início com ações claras

Observado em StudioApp.kt: uma entrada grande de foto, outra entrada Photo editor, e cards de Story Maker, Templates, Style Lab, colagens, lote e packs. Mistura de português/inglês e explicações voltadas às ferramentas.

Proposta: destacar Editar foto, Criar Story e Fazer colagem; projetos recentes em seguida. Agrupar biblioteca e operações avançadas em uma entrada secundária. Traduzir os rótulos funcionais; nomes próprios de filtros podem continuar. Criar Story deve oferecer modelos prontos e Story em branco no mesmo caminho, sem exigir que a pessoa entenda a diferença entre as duas entradas atuais.

Critério: uma pessoa que abre o app pela primeira vez consegue começar cada uma dessas três tarefas sem instrução verbal.

### Modo simples com menos decisões

Observado em PhotoStudio.kt: o Modo rápido mantém Filtros, Ajustes, Recorte, Máscaras, Efeitos e Estilos; Pro acrescenta Avançado. Parte da barra fica fora da largura da tela.

Proposta: no modo simples, priorizar Filtros, Luz e cor, Recortar e Efeitos; deixar ferramentas detalhadas em Mais ferramentas, com os mesmos projetos e capacidade de edição. Destacar favoritos e coleções populares antes do catálogo completo. Um ajuste inicial automático, reversível, pode ser uma feature posterior.

Critério: editar a foto com filtro e brilho não exige percorrer ferramentas avançadas. O acesso à barra inteira deve ser evidente e funcionar com fonte ampliada.

### Salvar com significado claro

Observado: Salvar projeto é um ícone de marcador; Exportar abre escolhas de formato, dimensões, resolução, qualidade e metadados. A edição do projeto depende de salvamento explícito.

Proposta: ação principal Salvar foto, com qualidade recomendada e opções avançadas recolhidas; mostrar claramente Salvar projeto editável como outra ação. Criar salvamento automático de rascunhos, com estado visível e recuperação após interrupção. Exportação maior continua disponível e deve informar os pixels finais.

Critério: o usuário consegue salvar e encontrar a foto na galeria; entende quando o projeto está salvo; encerrar e reabrir recupera o rascunho sem alterar a fonte original.

## Prioridade 2: leitura, toque e orientação

Observado: textos funcionais de 8–11 sp e metadados de filtros de 6 sp; controles visuais de 20–40 dp em várias telas, além de painéis com altura limitada. Compose pode ampliar a área de toque automaticamente: dimensões visuais pequenas são um risco a verificar, não prova de que todos os alvos tenham essa dimensão efetiva.

Proposta: aumentar textos importantes, reservar áreas de interação de pelo menos 48 × 48 dp com espaçamento, acomodar a fonte do Android sem cortar ações e verificar contraste, foco e descrições de controles no TalkBack. Campos e sliders precisam anunciar o ajuste e seu valor. Não depender apenas de cor ou gestos para indicar seleção ou executar uma tarefa.

Critério: avaliar telas pequenas e fonte até 200%; concluir tarefas essenciais com TalkBack, sem controles sobrepostos, textos essenciais cortados ou ações inacessíveis. Oferecer alternativas por botões para ações de gesto, especialmente em Stories.

Ajuda: explicações curtas perto da ação, como Clarear foto e Adicionar efeito, instrução inicial dispensável e acessível novamente por Ajuda. Mostrar passos e próximo botão em tarefas com várias etapas. Evitar tutoriais longos obrigatórios. Revisar animações e oferecer comportamento com movimento reduzido.

Referências: [Android: acessibilidade padrão no Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [W3C: propósito claro da página](https://www.w3.org/WAI/WCAG2/supplemental/patterns/o1p01-clear-purpose/).

## Prioridade 3: Stories e IA com menos configuração

- Stories guiados: escolher modelo → adicionar fotos → editar texto → salvar. Já existem modelos, importação e edição; falta um caminho guiado que destaque a tarefa atual antes das opções de camadas, composição e cor avançada.
- IA: a configuração atual exige endereço do PC e código. Implementar conexão por QR com confirmação do servidor ou descoberta local confirmada; mostrar PC conectado, PC desligado ou Conectar PC antes de começar. Manter a confirmação do envio e a comparação do resultado. A descoberta não deve enviar fotos automaticamente.
- Falhas: mensagens com ação concreta, como Reconectar PC, Tentar novamente e Voltar à foto; preservar o trabalho anterior. Progresso real por etapa é preferível a percentuais inventados.

## Verificação com pessoas

Testar escolher foto, aplicar filtro, desfazer, recortar, usar modelo de Story e salvar na galeria. Observar conclusão sem ajuda, toques incorretos, telas em que a pessoa se perde e compreensão de onde a foto foi salva. Incluir iniciantes e pessoas com necessidades diferentes, consentimento e adaptações individuais; não inferir capacidade a partir de um diagnóstico. Ajustar o produto a partir dos resultados, sem prometer que uma interface servirá igualmente para todas as pessoas.

Ordem recomendada: início e rótulos → modo simples → salvar/rascunhos → tamanho e acessibilidade dos controles → Stories guiados → conexão facilitada com IA. Filtros, efeitos e ferramentas atuais devem continuar disponíveis.
