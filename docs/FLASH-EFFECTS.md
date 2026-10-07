# Efeitos de flash — 1.4.0

24 presets originais na categoria Flash, além dos 69 efeitos existentes. Os filtros Flash 80/90 continuam disponíveis nos Looks; os novos efeitos podem ser combinados com qualquer look e usados em fotos, Stories e ajustes locais.

| Família | Efeitos |
| --- | --- |
| Direto | Flash de bolso, Flash direto, Flash duro, Flash estourado |
| Compacta | CCD 2003, CCD frio, CCD festa, CCD suave |
| Descartável | Descartável 89, Descartável quente, Descartável ISO 800, Descartável lavado |
| Verde retrô | Flash verde 90, Flash oliva, Verde intenso, Verde suave |
| Noturno | Flash azul noturno, Flash gelo, Flash violeta 95, Azul ISO 1600 |
| Editorial | Flash editorial, Flash beauty, Flash lateral, Flash spotlight |

## Uso

No editor, abra Efeitos → Flash e toque em um card. Intensidade mistura a luz, cor e textura com a edição anterior. Ajustar luz do flash fica no topo e permite mudar área, dureza, bordas escuras, temperatura, tonalidade, grão e posição. Seleção de camada e intensidade ficam fixas na parte inferior; apenas o catálogo rola em painéis menores. Camadas conserva visibilidade, ordenação, remoção e parâmetros. Novos efeitos passam a ser a seleção corrente; tocar numa camada anterior permite editá-la novamente. Busca e favoritos também funcionam com esses presets.

Os cards identificam presets; a foto principal apresenta o resultado real. O painel rola em alturas menores, inclusive Stories. Os nomes ISO são referências estéticas de grão, sem alteração de metadados ou simulação de sensor físico.

## Renderização e compatibilidade

Tipo existente `flash`, com parâmetros adicionais `hardness`, `falloff`, `tint`, `grain`, `x`, `y`. A presença de hardness seleciona a nova passagem GPU; o preset pro-flash e projetos antigos continuam usando a implementação anterior. A lista original de 69 efeitos foi comparada por igualdade de dados antes da entrega. Não há mudança de schema de projeto.

A nova passagem trata RGB sem premultiplicação, normaliza a distância pelo lado menor do canvas e mistura iluminação, temperatura, tonalidade, escurecimento periférico e grão determinístico. O alfa original é preservado. Intensidade zero ignora a passagem. Preview e exportação usam o mesmo shader. Parâmetros são valores finitos entre 0 e 1, validados também na importação de packs.

É um tratamento da luz existente, sem reconstrução de profundidade ou sombras físicas. Fotos já estouradas não recuperam detalhes; revise intensidade e temperatura em cada retrato. Todo processamento é local, sem recursos externos novos ou permissões de rede.

## Prévia e testes

[Original e 24 efeitos em retrato](flash-effects-portrait.png), renderizados pelo aplicativo. Retrato oficial de Christina Koch, crédito NASA; origem em app/src/androidTest/assets/README.md. Essa imagem de teste não integra o release.

Os testes de renderização, transparência, intensidade, persistência, exportação e controles pela interface em fotos/Stories passaram. [Editor de fotos](flash-effects-editor.png) e [editor de Stories](flash-effects-story.png) mostram os controles finais; o teste verifica que a intensidade não sobrepõe o botão de exportar Story. A versão 1.4.0 foi instalada após autorização explícita do usuário.

Para uma máquina com Android virtual disponível, use tools/test_emulator.ps1 conforme o README; o script recusa identificadores de celulares físicos. Evidências e limites em VERIFICATION.md.
