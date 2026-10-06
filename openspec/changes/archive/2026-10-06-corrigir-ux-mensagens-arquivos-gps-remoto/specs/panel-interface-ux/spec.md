## ADDED Requirements

### Requirement: Compositor de mensagens visível e funcional
O painel SHALL manter o compositor de mensagens visível para usuários autorizados na tela Mensagens. O envio SHALL permanecer condicionado à seleção de um dispositivo, ao preenchimento de texto e à permissão de envio.

#### Scenario: Compositor disponível antes da seleção
- **WHEN** um usuário autorizado abre Mensagens sem selecionar um dispositivo
- **THEN** o campo de mensagem e o botão de envio ficam visíveis, e o envio permanece desabilitado até a seleção de um dispositivo

#### Scenario: Enviar mensagem ao dispositivo selecionado
- **WHEN** o usuário autorizado seleciona um dispositivo, informa uma mensagem não vazia e envia
- **THEN** a mensagem é enviada ao dispositivo selecionado e o histórico correspondente é atualizado

#### Scenario: Usuário sem permissão de envio
- **WHEN** um usuário sem a permissão de envio abre Mensagens
- **THEN** a interface informa a restrição e não permite enviar a mensagem

### Requirement: Identificadores de dispositivo legíveis
A lista e o seletor de dispositivos SHALL exibir identificadores e nomes somente a partir de valores primitivos válidos, sem apresentar objetos brutos, `undefined` ou a string `[object Object]`.

#### Scenario: Dispositivo com identificadores opcionais
- **WHEN** um dispositivo não possui modelo ou IMEI, ou algum campo de identificação não é textual
- **THEN** a interface exibe o identificador disponível sem converter objetos em texto bruto nem mostrar valores ausentes

### Requirement: Alternância entre cartões e lista de arquivos
A tela Arquivos SHALL oferecer controles localizados para alternar entre as apresentações em cartões e em lista, exibindo os mesmos arquivos em ambos os modos.

#### Scenario: Alternar apresentação
- **WHEN** o usuário escolhe Cartões ou Lista
- **THEN** os arquivos são apresentados no modo escolhido sem perder a busca nem os dados carregados

#### Scenario: Rótulos localizados
- **WHEN** a tela Arquivos é renderizada em português
- **THEN** os rótulos dos controles e o subtítulo são apresentados em português, sem chaves de tradução literais

### Requirement: Ações do dispositivo no contexto do mapa
As ações Histórico e Centralizar SHALL aparecer na área do mapa, à direita da lista de dispositivos, e não dentro do cartão do dispositivo selecionado.

#### Scenario: Ações do dispositivo selecionado
- **WHEN** um dispositivo é selecionado no mapa
- **THEN** Histórico e Centralizar aparecem na barra de ferramentas da área do mapa e atuam sobre o dispositivo selecionado

### Requirement: Ações de acesso remoto legíveis
Os botões de ação do Acesso remoto SHALL manter rótulos legíveis, alvos clicáveis e alinhamento à direita da visualização da tela em larguras amplas, sem sobrepor ou comprimir a imagem.

#### Scenario: Viewport amplo
- **WHEN** o Acesso remoto é exibido em viewport amplo
- **THEN** as ações ficam em uma coluna à direita da visualização, com rótulos inteiros e botões clicáveis

#### Scenario: Viewport estreito
- **WHEN** a largura disponível não comporta a visualização e a coluna de ações lado a lado
- **THEN** a coluna de ações se reorganiza abaixo da visualização sem cortar rótulos ou controles

### Requirement: Grade de relatórios equilibrada
A tela Relatórios SHALL distribuir as visualizações em uma grade responsiva com no máximo três colunas em telas amplas, evitando concentrar quatro cartões na primeira linha e deixar a linha seguinte parcialmente vazia. A apresentação SHALL preservar os dados e filtros existentes.

#### Scenario: Viewport amplo
- **WHEN** Relatórios é exibido em uma tela ampla
- **THEN** os seis painéis agregados são distribuídos em até três colunas, sem criar um quarto painel estreito na primeira linha

#### Scenario: Viewport estreito
- **WHEN** a largura disponível diminui
- **THEN** a grade reduz o número de colunas sem provocar rolagem horizontal

### Requirement: Busca digitável e selecionável de Informação Detalhada
A tela Informação Detalhada SHALL permitir digitar uma consulta, apresentar sugestões de dispositivos retornadas pela busca existente e carregar informações somente após selecionar um dispositivo válido. Os estados de carregamento, nenhum resultado e falha de solicitação SHALL ser explícitos.

#### Scenario: Pesquisar e selecionar dispositivo
- **WHEN** o usuário digita uma consulta e seleciona um resultado da lista de sugestões
- **THEN** a consulta é resolvida para o número do dispositivo selecionado e seus detalhes são carregados

#### Scenario: Nenhum dispositivo correspondente
- **WHEN** a busca termina sem dispositivos correspondentes
- **THEN** a tela informa que nenhum resultado foi encontrado e não exibe detalhes antigos como se fossem da consulta atual

#### Scenario: Falha durante a busca
- **WHEN** o serviço de pesquisa ou de detalhes falha
- **THEN** a tela apresenta uma mensagem de erro e permite ao usuário tentar novamente

### Requirement: Alternância Cartões/Lista restrita a Arquivos
A alternância entre Cartões e Lista SHALL existir exclusivamente na tela Arquivos. Nenhuma outra tela deste escopo SHALL apresentar esse seletor.

#### Scenario: Navegar para outra tela
- **WHEN** o usuário abre Mensagens, GPS, Acesso remoto, Relatórios ou Informação Detalhada
- **THEN** a interface não exibe a alternância Cartões/Lista
