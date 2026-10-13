## Purpose

Define os catálogos de categorias de sites e de aplicativos, a atualização das listas abertas de domínios e a decisão de bloqueio de sites aplicada pelo resolvedor DNS do filtro.

## ADDED Requirements

### Requirement: Catálogo fixo de categorias

Origem: PRD-02, PRD-03, PRD-05

O sistema SHALL oferecer exatamente as categorias `adult`, `gambling`, `games`, `social_media`, `streaming`, `shopping`, `dating`, `violence`, `drugs`, `piracy`, `malware`, `phishing`, `vpn_proxy` e `doh`, as mesmas para sites e aplicativos. Para sites, cada categoria SHALL ser a união das listas de domínios mapeadas para ela nas fontes IPFire DBL, Block List Project e UT1. O catálogo SHALL identificar `doh` como obrigatória.

#### Scenario: Consultar catálogo
- **WHEN** administrador autorizado consulta o catálogo de categorias
- **THEN** o sistema retorna as 14 categorias, cada uma com suas fontes de domínios e a quantidade de aplicativos catalogados
- **THEN** somente `doh` vem marcada como obrigatória

### Requirement: Catálogo de aplicativos por categoria

Origem: PRD-02, PRD-04, PRD-07

O sistema SHALL manter um catálogo inicial de pacotes Android por categoria, em que todo pacote existe na Google Play. Cada cliente SHALL poder incluir e remover pacotes próprios em categorias do catálogo. Pacotes incluídos por um cliente SHALL valer somente para os perfis desse cliente. Pacotes protegidos do MDM SHALL NOT ser aceitos no catálogo.

#### Scenario: Cliente categoriza pacote próprio
- **WHEN** administrador do cliente A inclui `br.com.empresa.jogo` na categoria `games`
- **THEN** perfis do cliente A que bloqueiam `games` bloqueiam `br.com.empresa.jogo`
- **THEN** perfis do cliente B que bloqueiam `games` não bloqueiam `br.com.empresa.jogo`

#### Scenario: Remover pacote próprio
- **WHEN** administrador remove `br.com.empresa.jogo` da categoria `games`
- **THEN** perfis do cliente deixam de bloquear esse pacote pela categoria `games`

#### Scenario: Pacote protegido no catálogo
- **WHEN** administrador tenta incluir `com.hmdm.launcher` em qualquer categoria
- **THEN** o sistema responde HTTP 400 informando que o pacote é protegido

#### Scenario: Catálogo inicial válido
- **WHEN** o catálogo inicial de aplicativos é verificado
- **THEN** todo pacote do catálogo inicial tem página ativa na Google Play
- **THEN** nenhum pacote do catálogo inicial é protegido

### Requirement: Listas de domínios atualizadas sem perder a última versão válida

Origem: PRD-03, PRD-09

O sistema SHALL baixar cada lista de origem pelo menos uma vez a cada 24 horas e no máximo uma vez por hora por URL. Falha de download ou conteúdo sem nenhum domínio SHALL manter em uso a última versão válida da lista. Lista de origem dividida em partes SHALL ser usada com todas as partes.

#### Scenario: Fonte indisponível
- **WHEN** o download de uma lista de origem falha por erro HTTP ou timeout
- **THEN** o resolvedor continua usando a versão anterior dessa lista
- **THEN** o resolvedor registra no seu log a URL e o motivo da falha

#### Scenario: Fonte responde vazia
- **WHEN** o download de uma lista de origem retorna HTTP 200 sem nenhuma linha de domínio
- **THEN** o resolvedor continua usando a versão anterior dessa lista

#### Scenario: Lista em partes
- **WHEN** uma categoria de origem é publicada em partes `domains.0`, `domains.1` e `domains.2`
- **THEN** domínios de todas as partes são bloqueados na categoria correspondente

#### Scenario: Intervalo mínimo por URL
- **WHEN** uma lista de origem foi baixada com sucesso há menos de 1 hora
- **THEN** o resolvedor não baixa essa URL novamente

### Requirement: Resolvedor aplica a decisão de sites do perfil

Origem: PRD-06, PRD-09

Para consultas DNS-over-TLS ao hostname de um perfil com filtro ativo, o resolvedor SHALL aplicar a ordem de decisão de `webfilter/policy-management` e responder NXDOMAIN para domínio bloqueado. Toda entrada de lista, das fontes ou do perfil, SHALL valer para o domínio e seus subdomínios. Domínio permitido SHALL ser resolvido normalmente. Allowlist do perfil SHALL NOT restringir a resolução de domínios fora dela. Hostname de perfil sem filtro ativo SHALL resolver sem bloqueio.

#### Scenario: Domínio de categoria bloqueada
- **WHEN** o perfil bloqueia `social_media` e um dispositivo consulta `facebook.com` pelo hostname do perfil
- **THEN** o resolvedor responde NXDOMAIN

#### Scenario: Subdomínio de domínio listado
- **WHEN** o perfil está ativo, a lista `doh` contém `cloudflare-dns.com` e um dispositivo consulta `mozilla.cloudflare-dns.com`
- **THEN** o resolvedor responde NXDOMAIN

#### Scenario: Allowlist do perfil vence a categoria
- **WHEN** o perfil bloqueia `social_media`, tem `linkedin.com` na allowlist e um dispositivo consulta `www.linkedin.com`
- **THEN** o resolvedor responde com o endereço real de `www.linkedin.com`

#### Scenario: Blocklist do perfil
- **WHEN** o perfil tem `noticias.example.com` na blocklist e um dispositivo consulta esse domínio
- **THEN** o resolvedor responde NXDOMAIN

#### Scenario: Allowlist não restringe o restante
- **WHEN** o perfil tem apenas `linkedin.com` na allowlist, nenhuma categoria além de `doh` e nenhuma blocklist, e um dispositivo consulta `wikipedia.org`
- **THEN** o resolvedor responde com o endereço real de `wikipedia.org`

#### Scenario: Domínio fora das regras do perfil
- **WHEN** o perfil bloqueia apenas `gambling` e um dispositivo consulta `facebook.com`
- **THEN** o resolvedor responde com o endereço real de `facebook.com`

#### Scenario: Isolamento entre perfis
- **WHEN** o perfil A bloqueia `social_media`, o perfil B não, e um dispositivo consulta `facebook.com` pelo hostname do perfil B
- **THEN** o resolvedor responde com o endereço real de `facebook.com`

#### Scenario: Perfil sem filtro ativo
- **WHEN** um dispositivo consulta `facebook.com` pelo hostname de um perfil com `enabled=false`
- **THEN** o resolvedor responde com o endereço real de `facebook.com`

### Requirement: Política salva é aplicada pelo resolvedor sem intervenção manual

Origem: PRD-09, PRD-12

O resolvedor SHALL aplicar em até 5 minutos, sem ação manual de operação, uma política, lista ou domínio salvo no MDM.

#### Scenario: Categoria adicionada ao perfil
- **WHEN** administrador adiciona `games` à política ativa de um perfil
- **THEN** em até 5 minutos consultas a domínios de `games` pelo hostname do perfil recebem NXDOMAIN

#### Scenario: Domínio adicionado à blocklist
- **WHEN** administrador adiciona `noticias.example.com` à blocklist de um perfil ativo
- **THEN** em até 5 minutos consultas a `noticias.example.com` pelo hostname do perfil recebem NXDOMAIN, sem reinício do resolvedor

#### Scenario: Configuração inválida não derruba o resolvedor
- **WHEN** a configuração nova do resolvedor impede o serviço de iniciar
- **THEN** o resolvedor volta a operar com a última configuração válida
- **THEN** o resolvedor registra no seu log a falha da configuração nova

### Requirement: Resolvedor exposto somente por DNS-over-TLS

Origem: PRD-09, PRD-14

O resolvedor SHALL aceitar consultas de dispositivos somente por DNS-over-TLS na porta TCP 853, com certificado válido publicamente para os hostnames dos perfis. A porta DNS 53 do resolvedor SHALL NOT ficar acessível fora do host.

#### Scenario: Consulta DNS-over-TLS com hostname de perfil
- **WHEN** um cliente abre TLS na porta 853 indicando o hostname de um perfil
- **THEN** o handshake conclui com certificado válido para esse hostname emitido por autoridade pública

#### Scenario: Consulta DNS sem TLS
- **WHEN** um cliente externo envia consulta DNS sem TLS à porta 53 do servidor
- **THEN** a consulta não é respondida pelo resolvedor
