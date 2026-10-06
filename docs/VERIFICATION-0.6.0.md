# Verificação da entrega 0.6.0 — 05/10/2026

## Confirmado

- 47 testes aprovados: 20 JVM, 17 de renderer e 10 de aplicativo, sem falhas/erros/skips. Relatórios em core/build/reports/tests/test, renderer/build/reports/androidTests e app/build/reports/androidTests.
- JVM: confidence maps comprimidos, round-trip de seleção/correções/recorte no JSON, versões anteriores e rejeição de base64/zlib inválido, truncamento, tamanho excessivo e bytes excedentes. Máscaras automáticas exigem raster; recorte não pode ser aplicado a um tipo incompatível. Catálogos, histórico, layouts e packs anteriores também passaram.
- Renderer no Samsung SM-S731B/Android 16: cobertura automática, ajustes só na área, correções por pincel/borracha, inversão, corte de alpha com intensidade parcial, limite da confiança e ancoragem após crop/flip/rotação entre preview e resolução completa. Foto recortada mostra o fundo do Story sem alterar outra foto da composição. Os 14 testes anteriores também passaram.
- App: inferência real do modelo incorporado sobre retrato de diagnóstico da NASA; face classificada como pessoa e canto da bandeira como fundo. O teste verificou ausência da permissão INTERNET no aplicativo alvo. Seleção de pessoa/fundo reutilizou confiança e manteve correções independentes.
- UI: escolher pessoa/fundo/remover fundo, corrigir com pincel sobre a comparação original, desfazer o traço, ocultar guia, concluir e salvar/reabrir. PNG saiu com pixels iguais à prévia; PNG e WebP mantiveram alpha zero no fundo; JPEG preencheu essa área com branco. Bytes da fonte original permaneceram iguais.
- Foto preta sem pessoa: orientação exibida, busy encerrado e nenhuma máscara vazia criada. Undo da troca da foto enquanto o editor de máscaras estava aberto voltou à composição e restaurou a seleção anterior.
- Templates/packs preservaram a seleção automática da foto fixa e removeram máscaras de placeholders. Testes anteriores de fotos/Stories, estilos, colagens, catálogo, biblioteca e JPEG/PNG/WebP também passaram.
- Captura real da UI em docs/person-cutout-editor.png foi inspecionada. Fixture: retrato oficial de Christina Koch, crédito NASA, presente somente no APK de testes; não está no aplicativo pessoal nem é foto privada do usuário.
- Lint: zero erros, 13 avisos (dependências fixadas, backup legado e sugestões KTX). APK compacto 0.6.0/versionCode 6: 44.515.216 bytes, aproximadamente 42,45 MiB, incluindo modelo e bibliotecas nativas para as arquiteturas suportadas. R8/redução de recursos concluídos; assinatura v2 validada, um assinante.
- APK final inspecionado: asset selfiesegmentation_mlkit-256x256-2021_01_19-v1215.f16.tflite incorporado; nenhuma permissão de internet/rede e nenhum portrait fixture. Única uses-permission é a assinatura interna para receptores AndroidX.
- Atualização no celular via install -r: Success. Pacote studio.luma.personal confirmado em 0.6.0/versionCode 6; início a frio com Status: ok, TotalTime 483 ms. O app pessoal não foi desinstalado; seus arquivos privados foram preservados pela atualização. Apenas o pacote pessoal permaneceu instalado.

## Condições e limites

Instrumentação foi executada no aplicativo separado studio.luma.personal.qa, sem ler imagens/projetos do app pessoal. Arquivos de diagnóstico/exportações foram apagados e a biblioteca QA restaurada. A Activity de teste mostrou só a própria UI acima do bloqueio, sem remover/desbloquear PIN. Flags de teste não estão no aplicativo pessoal. Apps instrumentados foram removidos pelo Android depois da verificação.

O teste inicial da composição usou export=true com o limite padrão 1280 e mediu coordenadas da composição de 200 px sem aplicar a escala. Foi corrigido para exportar na resolução original e verificar as dimensões antes de medir áreas. A execução final completa passou; não houve mudança no comportamento da composição para contornar o teste.

Os testes automatizados usam a versão QA sem redução. A versão compacta foi compilada, assinada, inspecionada, instalada e iniciada; a inferência real não foi instrumentada no pacote pessoal. Não há garantia de qualidade em todos os retratos/cabelos/cenas ou de desempenho em todos os aparelhos. O SDK é beta, dedicado a pessoas. Resultado pode incluir objetos próximos e precisa ser revisado/corrigido. Smart layout/crop, referência estética e demais controles avançados permanecem pendentes.

## Evidência e fontes

phase6-final-checks.log registra a execução final completa. SEGMENTATION.md contém o pipeline, persistência, limites e links para documentação oficial do ML Kit/NASA. Captura de diagnóstico: person-cutout-editor.png. Registros anteriores: VERIFICATION-0.5.0.md, VERIFICATION-0.4.0.md e VERIFICATION-0.3.0.md.
