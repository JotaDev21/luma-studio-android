# Uso mais simples — 1.6.0

O início organiza o app pelas tarefas: Editar foto, Criar Story e Fazer colagem. Projetos recentes vêm em seguida. Estilos prontos, edição em lote e recursos próprios continuam abaixo. Criar Story abre a mesma biblioteca da aba Stories, com modelos e uma opção em branco; o editor de camadas ainda não possui um assistente por etapas.

## Editar uma foto

1. Toque em Editar foto e escolha a imagem no seletor do Android.
2. Toque numa miniatura de filtro. Original remove o filtro; Força ajusta sua intensidade. Ver todos abre busca, favoritos e as coleções completas.
3. Luz e cor oferece Clarear foto, Contraste, Força das cores, Cor quente ou fria e Áreas escuras. Recortar e Efeitos ficam ao lado. Desfazer/refazer preservam os passos da edição.
4. Toque em Salvar foto e depois Salvar na galeria. O arquivo vai para a pasta Luma Studio da galeria, com opção de compartilhamento.

O modo simples usa um carrossel menor, com favoritos, Flash 80/90, Instagram, retratos e o filtro atual. Não remove filtros do catálogo. Mais ferramentas abre máscaras, estilos, cor avançada, ajuda e todas as ferramentas; Usar modo simples retorna aos quatro controles principais.

## Salvar foto e salvar projeto

Salvar foto gera uma imagem na galeria. O padrão é JPEG, qualidade 95, resolução Original e sem metadados opcionais. Original considera a transformação da foto ou o canvas do Story; não significa desfazer a edição. 1440p e 4K usam, respectivamente, 2560 e 3840 pixels no maior lado. Ampliar pixels sozinho não recupera detalhes ausentes.

Opções avançadas mostra formatos JPEG/PNG/WebP, qualidade, dimensões de publicação ou personalizadas e metadados de câmera/data para JPEG. O cálculo de resolução e os limites do renderer permanecem iguais à correção da versão 1.4.1. A opção personalizada determina dimensões exatas.

Salvar projeto editável, em Mais ferramentas, confirma nome, edição, histórico e thumbnail no projeto privado. O rascunho automático é separado desse salvamento. Um arquivo na galeria e um projeto editável são dois resultados distintos, identificados pelos rótulos das ações.

## Rascunhos automáticos

- Cada projeto possui um draft.json privado na própria pasta. A gravação usa AtomicFile e um mutex compartilhado entre instâncias do armazenamento; o original e o project.json não são alterados pela gravação do rascunho.
- Alterações reiniciam uma espera de 900 ms. O app tenta gravar imediatamente ao entrar em segundo plano, quando a edição não está ocupada. Voltar ao início aguarda a gravação antes de fechar a edição; uma falha mantém a edição aberta e informa o erro.
- Projeto salvo, Salvando rascunho…, Rascunho salvo e Rascunho não salvo indicam o estado. Espere Rascunho salvo antes de encerrar abruptamente o app; não existe garantia de persistência antes dessa confirmação.
- Reabrir recupera a edição e até 20 estados persistidos de cada direção do histórico. Uma alteração de slider ainda não concluída é incluída no histórico da recuperação sem modificar o undo da sessão atual.
- O rascunho referencia o timestamp do último salvamento explícito. Versão base diferente, ID diferente ou conteúdo inválido descartam o rascunho, preservando o projeto salvo. A gravação atrasada não pode recriar um projeto excluído.
- Salvar projeto editável confirma a edição e limpa o rascunho. Descartar rascunho exige confirmação e retorna ao último projeto salvo, sem apagar a fonte ou exportações da galeria.
- Fotos e Stories usam o mesmo mecanismo. Projetos recentes consideram a última gravação de rascunho na ordenação; o thumbnail continua representando o último salvamento explícito, enquanto as alterações são recuperadas ao abrir.

## Leitura e interação

Foram aumentados os textos funcionais do início, editor de foto, filtros, ajustes, efeitos, categorias e salvamento. Controles essenciais usam áreas mínimas de 48 dp; sliders recebem descrições do ajuste e o estado do rascunho usa anúncio sem interromper a navegação. O modo simples mostra quatro ferramentas sem exigir rolagem horizontal da barra.

Fonte a partir de 130% ou janela abaixo de 640 dp ativa rolagem vertical no editor de foto e uma ação de salvar em linha própria. O teste de 200% usa uma substituição de densidade dentro do app QA: não altera a preferência de fonte do celular. Ajustar luz e salvar uma imagem na galeria foram verificados nessa configuração, com captura conferida visualmente. Isso não comprova a acessibilidade de cada tela ou de cada ferramenta avançada.

Ajuda curta está no início e em Mais ferramentas; explica foto, filtro, ajustes, galeria, rascunhos, Stories e IA. Não há tutorial obrigatório.

## Próximos pontos de usabilidade

Stories guiados por tarefa, conexão da IA por QR, revisão completa de fonte ampliada/contraste/foco nas telas avançadas, testes com TalkBack e comportamento com movimento reduzido continuam pendentes. Também falta observar usuários iniciantes e pessoas com diferentes necessidades concluindo tarefas sem ajuda; não é possível garantir facilidade a partir de um diagnóstico.

A recuperação foi testada com um novo ViewModel lendo o rascunho do disco e com fechamento/reabertura da edição. Não foi testado o encerramento forçado do processo durante uma gravação. A IA não foi alterada nesta entrega: a entrada e o cancelamento foram testados, sem conexão ou envio de foto ao PC. Limitações anteriores estão em [VERIFICATION-1.5.0.md](VERIFICATION-1.5.0.md).

## Capturas do aparelho

![Início com três tarefas](easy-home.png)

![Editor no modo simples](easy-photo-editor.png)

![Editor rolável com fonte em 200%](easy-font-200.png)

O retrato é uma fixture pública NASA, usada somente na instrumentação; crédito e origem em app/src/androidTest/assets/README.md. As capturas não incluem fotos pessoais e a fixture não entra no APK release.
