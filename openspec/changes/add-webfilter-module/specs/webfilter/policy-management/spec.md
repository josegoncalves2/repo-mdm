## Purpose

Define, por perfil de dispositivos do MDM, o controle de acesso a sites e aplicativos: categorias bloqueadas, allowlist e blocklist de domínios e de aplicativos, a ordem de decisão e a proteção do próprio MDM, com isolamento entre clientes.

## ADDED Requirements

### Requirement: Política de filtro por perfil

Origem: PRD-01, PRD-06

O sistema SHALL permitir, para cada perfil de dispositivos (configuration) do cliente, no máximo uma política contendo `enabled`, categorias bloqueadas, allowlist e blocklist de domínios, e allowlist e blocklist de aplicativos. Toda categoria SHALL pertencer ao catálogo definido em `webfilter/classification`. Salvar a política de um perfil que já possui política SHALL substituir a anterior.

#### Scenario: Ativar filtro com categorias e listas
- **WHEN** administrador salva para o perfil "Tablets Loja": `enabled=true`, categorias `adult` e `social_media`, allowlist de domínios `*.empresa.com.br`, blocklist de domínios `jogos.example.com`, blocklist de aplicativos `com.king.candycrushsaga`
- **THEN** a leitura da política retorna exatamente esses valores, com `doh` entre as categorias bloqueadas

#### Scenario: Categoria fora do catálogo é rejeitada
- **WHEN** administrador salva uma política com a categoria `education`
- **THEN** o sistema responde HTTP 400 identificando `education` como categoria inválida
- **THEN** a política anterior do perfil permanece inalterada

#### Scenario: Salvar novamente substitui a política
- **WHEN** administrador salva a política do perfil com a categoria `gambling` e depois salva a do mesmo perfil com a categoria `games`
- **THEN** o perfil possui uma única política, que bloqueia `games` e `doh` e não bloqueia `gambling`

#### Scenario: Desativar filtro
- **WHEN** administrador salva a política do perfil com `enabled=false`
- **THEN** a política é mantida com categorias e listas e `enabled=false`
- **THEN** nenhum site nem aplicativo do perfil é bloqueado pelo filtro

### Requirement: Categoria doh sempre bloqueada com filtro ativo

Origem: PRD-05

Quando `enabled=true`, o sistema SHALL tratar a categoria `doh` como bloqueada, independentemente das categorias enviadas. Não SHALL existir forma de ativar o filtro sem bloquear `doh`.

#### Scenario: Filtro ativo sem categorias escolhidas
- **WHEN** administrador salva `enabled=true` sem nenhuma categoria
- **THEN** a política do perfil bloqueia apenas a categoria `doh`

#### Scenario: Lista enviada sem doh
- **WHEN** administrador salva `enabled=true` com as categorias `adult` e `games`, sem `doh`
- **THEN** a política do perfil bloqueia `adult`, `games` e `doh`

### Requirement: Entradas de allowlist e blocklist são validadas

Origem: PRD-06

Entrada de domínio SHALL ser um nome de domínio válido ou `*.` seguido de nome de domínio válido com pelo menos dois rótulos. Entrada de aplicativo SHALL ser um nome de pacote Android válido. A mesma entrada SHALL NOT constar ao mesmo tempo na allowlist e na blocklist do mesmo tipo no perfil. Entrada de domínio SHALL valer para o domínio e todos os seus subdomínios.

#### Scenario: Domínio inválido
- **WHEN** administrador inclui `exemplo com` na blocklist de domínios
- **THEN** o sistema responde HTTP 400 identificando a entrada inválida

#### Scenario: Curinga amplo demais
- **WHEN** administrador inclui `*.com` na blocklist de domínios
- **THEN** o sistema responde HTTP 400 identificando a entrada inválida

#### Scenario: Pacote inválido
- **WHEN** administrador inclui `candy crush` na blocklist de aplicativos
- **THEN** o sistema responde HTTP 400 identificando a entrada inválida

#### Scenario: Mesma entrada nas duas listas
- **WHEN** administrador inclui `facebook.com` na allowlist e na blocklist de domínios do mesmo perfil
- **THEN** o sistema responde HTTP 400 identificando o conflito
- **THEN** a política anterior do perfil permanece inalterada

### Requirement: Ordem de decisão única para sites e aplicativos

Origem: PRD-06

Para um domínio ou um aplicativo, o sistema SHALL decidir nesta ordem: (1) presente na allowlist do perfil → permitido; (2) presente na blocklist do perfil → bloqueado; (3) pertence a categoria bloqueada no perfil → bloqueado; (4) demais → permitido.

#### Scenario: Allowlist libera site de categoria bloqueada
- **WHEN** o perfil bloqueia `social_media` e tem `linkedin.com` na allowlist de domínios
- **THEN** `linkedin.com` é permitido e `facebook.com` é bloqueado

#### Scenario: Blocklist bloqueia site fora das categorias
- **WHEN** o perfil bloqueia apenas `gambling` e tem `noticias.example.com` na blocklist de domínios
- **THEN** `noticias.example.com` é bloqueado

#### Scenario: Allowlist libera aplicativo de categoria bloqueada
- **WHEN** o perfil bloqueia `social_media` e tem `com.linkedin.android` na allowlist de aplicativos
- **THEN** `com.linkedin.android` é permitido e os demais aplicativos de `social_media` são bloqueados

#### Scenario: Blocklist bloqueia aplicativo sem categoria
- **WHEN** o perfil não bloqueia `games` e tem `com.king.candycrushsaga` na blocklist de aplicativos
- **THEN** `com.king.candycrushsaga` é bloqueado

### Requirement: O filtro nunca bloqueia o próprio MDM

Origem: PRD-07, PRD-08

O sistema SHALL sempre permitir o domínio do servidor MDM e os hostnames do domínio DNS do filtro, mesmo que constem em lista de categoria. O sistema SHALL recusar em blocklist, e nunca bloquear por categoria, os agentes do MDM e o aplicativo principal de quiosque do perfil.

#### Scenario: Domínio do MDM em blocklist por curinga
- **WHEN** o domínio do MDM é `mdm.empresa.com.br` e o administrador inclui `*.empresa.com.br` na blocklist de domínios
- **THEN** `outro.empresa.com.br` é bloqueado
- **THEN** `mdm.empresa.com.br` continua permitido

#### Scenario: Agente do MDM na blocklist de aplicativos
- **WHEN** administrador inclui `com.hmdm.launcher` na blocklist de aplicativos
- **THEN** o sistema responde HTTP 400 informando que o pacote é protegido

#### Scenario: App de quiosque em categoria bloqueada
- **WHEN** o app principal de quiosque do perfil pertence a uma categoria bloqueada no perfil
- **THEN** esse app não é bloqueado

### Requirement: Isolamento de políticas por cliente

Origem: PRD-01

O sistema SHALL permitir ler e salvar políticas apenas de perfis do mesmo cliente do usuário autenticado. Perfil de outro cliente SHALL ser tratado como inexistente.

#### Scenario: Ler política de perfil de outro cliente
- **WHEN** usuário do cliente A solicita a política de um perfil do cliente B
- **THEN** o sistema responde HTTP 404

#### Scenario: Salvar política em perfil de outro cliente
- **WHEN** usuário do cliente A salva política para um perfil do cliente B
- **THEN** o sistema responde HTTP 404
- **THEN** nenhuma política do cliente B é criada ou alterada

### Requirement: Exclusão do perfil remove a política

Origem: PRD-01

O sistema SHALL remover a política, suas categorias, listas e histórico quando o perfil for excluído.

#### Scenario: Perfil excluído
- **WHEN** um perfil com política ativa é excluído do MDM
- **THEN** não resta política, categoria, entrada de lista nem histórico associado a esse perfil
