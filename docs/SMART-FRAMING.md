# Recorte e organização inteligente — 0.7.0

## Como usar

Foto simples: Recorte → Recorte inteligente → formato desejado. Story/template/colagem: aba Inteligente → Enquadrar foto selecionada ou Organizar fotos. A aba Colagem também oferece Organizar fotos. Cada ação apresenta uma prévia com Ver antes, Aplicar sugestão e Cancelar. Cancelar não altera parâmetros nem o histórico. Se um ajuste ainda está renderizando, o Antes é recomposto com seus parâmetros mais recentes. Aplicar registra o estado manual anterior e gera uma entrada para a sugestão; um desfazer retorna exatamente a esse estado.

O recorte usa novamente a fonte original, conservando rotação, espelhamento e retoques/máscaras. Zoom e posição da foto são redefinidos no enquadramento sugerido. Após aplicar, continuam ajustáveis manualmente.

## Análise local

FocusAnalyzer decodifica a fonte privada com orientação EXIF em até 1280 px. O ML Kit Face Detection 16.1.7 incorporado usa modo preciso, sem reconhecimento de identidade, tracking, classificação ou contornos. Quando não encontra rostos, reutiliza o mapa de pessoa existente ou executa o segmentador incorporado. O centro ponderado dos pixels de confiança alta orienta o recorte. Sem pessoa/rosto, a sugestão é central e informa isso.

Não há serviço externo nem download no telefone. INTERNET e ACCESS_NETWORK_STATE continuam removidas do manifesto. Somente dimensões, retângulos dos rostos e ponto de interesse ficam no cache transitório do editor, limitado a 32 fontes. Nenhuma foto/resultado é enviado. O projeto guarda apenas os parâmetros finais, sem precisar analisar novamente para reabrir ou exportar.

## Enquadramento seguro

SmartFraming calcula o maior retângulo possível na proporção pedida, orientado pelo centro de interesse e limitado por todos os rostos detectados, com margem de 12% de cada caixa. A proporção é invertida para rotações de 90/270 graus; crop permanece nas coordenadas da fonte, mantendo máscaras ancoradas.

Se todos os rostos não couberem, uma foto de Story recebe photoFit=contain: a imagem inteira fica centralizada com margens do fundo/camadas inferiores. Para foto simples, a sugestão mantém a fonte inteira e explica que outro formato é necessário. Não força o formato cortando um rosto detectado. A escolha Foto inteira/Preencher também está em Ajustar foto; zoom manual pode voltar a cortar a imagem, por decisão do usuário.

## Organização

O comando considera 2–20 molduras de foto visíveis e desbloqueadas, inclusive vazias, e analisa as proporções verticais/horizontais/quadradas. Usa atribuição global de custo mínimo (algoritmo húngaro), comparando proporções reais e penalizando molduras que exigem margens para proteger rostos. O conteúdo útil desconta bordas e a faixa inferior de Polaroid como no renderer.

Troca a foto de moldura junto com fonte, rotação/espelhamento, máscaras e seu enquadramento. IDs, geometria, aparência, mistura e opacidade pertencem à moldura e ficam preservados. Camadas bloqueadas/ocultas e elementos gráficos não participam. Fontes repetidas são analisadas uma vez; nenhum asset é criado, duplicado ou apagado pelo comando. O formato/layout atual é preservado: este comando distribui fotos nos espaços existentes, sem gerar um template novo.

## Persistência e limites

Projetos novos/salvos usam schema 7. Versões 1–6 continuam legíveis, com photoFit=cover como padrão. Templates/packs preservam contain em fotos fixas; placeholders removem enquadramento/máscaras e voltam a cover. Prévia e exportação usam o mesmo StoryRenderer.

A detecção pode falhar em rostos pequenos, desfocados, parcialmente ocultos ou em orientações difíceis. A proteção aplica-se aos rostos efetivamente detectados; a prévia exige revisão. O segmento de pessoa ajuda a posicionar a foto e não constitui identificação de rostos. A organização não garante a melhor composição estética nem substitui ajustes manuais.

Fonte da integração: [documentação oficial do ML Kit](https://developers.google.com/ml-kit/vision/face-detection/android). Os testes usam o retrato NASA de diagnóstico descrito em SEGMENTATION.md, somente no APK de testes.
