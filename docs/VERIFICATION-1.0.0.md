# Verificação da atualização completa 1.0.0 — 05/10/2026

## Entrega confirmada

- 71 testes aprovados, zero falhas/erros/skips: 31 JVM no core, 21 de renderer e 19 de aplicativo. Execuções finais em full-final-checks.log (core/renderer) e full-delivery-checks.log (app/lint/release). Última execução de app/lint/release: BUILD SUCCESSFUL em 1m37s.
- Samsung SM-S731B, Android 16, conectado por depuração sem fio. QA isolado em studio.luma.personal.qa, sem abrir os projetos pessoais. Testes serializados no aparelho.
- Core: parsing/validação e interpolação trilinear de .cube, domains e ordem de canais, limites/NaN/formatos inválidos; round-trip de cor/HSL/LUT/detalhe/histórico com redo após reinício; recursos aninhados e packs; snapping e bloqueio no alinhamento. Catálogos validados: 120 filtros, 69 efeitos, 42 templates e 24 estilos. Testes anteriores de modelos/máscaras/crops/packs/colagens preservados.
- GPU: todos os 58 operadores de catálogo renderizados sobre uma fixture com textura/cor; resultado determinístico, diferente do neutro, intensidade zero neutra e alpha preservado. Texture foi testada com arquivo importado. LUT troca R/B conforme esperado; curvas levantam meios-tons; HSL pode remover saturação; detalhe muda pixels; movimento limitado pela máscara deixa a região não coberta igual ao baseline.
- Composição: gradiente, desenho vetorial e fonte importada em texto curvo/gradiente renderizados na prévia e exportação. Os testes anteriores de transparência, pessoa/fundo, recortes, máscaras e fotointeira continuaram aprovados.
- Armazenamento: fontes/LUT/textura importados, template com fonte/fundo/LUT, exportação e reimportação de pack com novos nomes; exclusão de recursos/packs deixou os projetos existentes renderizáveis com cópias privadas. Exportação em PNG com dimensões explícitas e margens transparentes passou.
- App: Modo Pro → Avançado → Cor Pro, HSL com prévia; sugestão de referência com comparar/cancelar sem alterar estado; salvar/reabrir com histórico e undo. Card de música e desenho mantiveram camadas e undo/redo. Lote de duas fotos com estilo exportou ambos e guardou fontes imutáveis/projetos/histórico.
- Capturas reais inspecionadas e copiadas para docs/pro-hsl.png, docs/reference-proposal.png e docs/music-and-drawing.png. Os controles Pro mostram prévia atualizada. Capturas anteriores de Stories/máscaras/recorte também foram produzidas nesta rodada.
- Lint: zero erros, 16 avisos (atualizações de dependências, backup e sugestões KTX). A leitura de assinatura de fonte foi corrigida para API 29, sem uso de readNBytes da API 33. Lint vital de release passou.
- APK release com R8/redução de recursos: 82.319.949 bytes; versão 1.0.0/versionCode 8, min29/target36, assinatura v2 validada, um assinante. Nenhuma permissão INTERNET/NETWORK; fixture NASA apenas em testes, ausente do release; modelos incorporados.
- Atualização instalada com install -r: Success, sem desinstalar o aplicativo pessoal. dumpsys confirmou versionCode 8/versionName 1.0.0. Início a frio Status ok, TotalTime 288 ms. Apenas studio.luma.personal permaneceu instalado no usuário principal.
- SHA256: 2b4da98b76b94dd7e3fdb8c0c598e0503e7a7a1bd43b87eb522c229be1171589.

## Correções encontradas durante a integração

Bindings de textura na inicialização dos novos controles de cor podiam sobrescrever a textura usada pelas máscaras. A inicialização agora seleciona sua própria unidade; os testes antigos e novos de máscaras passaram após a correção.

Dois problemas de fixtures foram corrigidos: a imagem para teste de textura precisava ser mutável, e uma rampa linear não permite provar desfoque em um pixel arbitrário; o teste passou a avaliar uma região com variação real, mantendo a exigência de região externa intacta. O teste de lote completava seus asserts, mas a limpeza usava display_name em vez de MediaStore.DISPLAY_NAME; foi corrigido. Dois arquivos de diagnóstico 128×128 dessa tentativa foram identificados e removidos; consulta final não encontrou resíduos desse lote. Testes não alteraram fontes/exportações pessoais.

Instrumentação mostra somente a própria Activity QA sobre a tela bloqueada, sem desbloquear/remover PIN. As flags não existem no app pessoal. A versão compacta foi compilada, inspecionada, instalada e iniciada; os testes de interação usam o pacote QA sem redução. Projetos precisam de salvamento explícito.

## Limites declarados

SDR sRGB/8 bits, decode até 32 MP, orçamento de memória e capacidade GPU; sem HDR/RAW/tiles. LUT 3D .cube 2–32 pontos por eixo e saída 0–1, sem LUT 1D. Referência aproxima média/desvio de cores, não recupera preset nem composição/textura. Gate weave/flicker/datamosh são interpretações estáticas para fotos; sem vídeo. Fontes devem renderizar no Android; texto curvo é de linha única. Cards de música são gráficos manuais, sem serviço de streaming. Lote aceita até 20 fotos e estilos apenas de cor/efeitos; parar conclui a foto atual. IA generativa opcional e vídeo permanecem futuros. Regras e detalhes em ADVANCED-EDITOR.md, README.md e PACKS.md.

Verificação anterior preservada em VERIFICATION-0.7.0.md.
