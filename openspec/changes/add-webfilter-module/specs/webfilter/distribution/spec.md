## Purpose

Entrega a cada dispositivo, pelo sync de configuração já existente no MDM, o hostname DNS do filtro e os aplicativos bloqueados e liberados do seu perfil, e avisa os dispositivos quando a política muda.

## ADDED Requirements

### Requirement: Sync inclui o hostname DNS do perfil com filtro ativo

Origem: PRD-10, PRD-11

A resposta do sync de configuração (`POST /rest/public/sync/configuration/{deviceId}`) SHALL incluir o campo `webfilterDnsHost` quando o perfil do dispositivo tiver política com `enabled=true` e o domínio DNS do filtro estiver configurado no plugin para o cliente do dispositivo. O valor SHALL ser um hostname exclusivo do perfil sob esse domínio. Em qualquer outro caso, o campo SHALL ser omitido.

#### Scenario: Perfil com filtro ativo
- **WHEN** o dispositivo do perfil 44 do cliente 1 faz sync, o perfil tem política ativa e o domínio DNS do filtro é `dns.empresa.com`
- **THEN** a resposta inclui `webfilterDnsHost` igual a `id-c1-p44.dns.empresa.com`

#### Scenario: Perfil sem política
- **WHEN** o dispositivo de um perfil sem política faz sync
- **THEN** a resposta não contém o campo `webfilterDnsHost`

#### Scenario: Política desativada
- **WHEN** o dispositivo de um perfil com política `enabled=false` faz sync
- **THEN** a resposta não contém o campo `webfilterDnsHost`

#### Scenario: Domínio DNS do filtro não configurado
- **WHEN** o cliente do dispositivo não tem domínio DNS do filtro configurado e o dispositivo de um perfil com política ativa faz sync
- **THEN** a resposta não contém o campo `webfilterDnsHost`

### Requirement: Sync inclui aplicativos bloqueados e liberados do perfil

Origem: PRD-10

A resposta do sync SHALL entregar ao launcher, nas configurações `locked_packages` e `unlocked_packages` do pacote `com.hmdm.launcher`, os pacotes que a ordem de decisão bloqueia no perfil e os pacotes que o filtro já bloqueou nesse perfil e não bloqueia mais.

Valores que o administrador tenha definido nessas configurações no perfil SHALL ser preservados. `unlocked_packages` SHALL NOT conter pacote bloqueado pelo administrador ou pelo filtro. Isso SHALL independer do domínio DNS do filtro.

#### Scenario: Categoria e blocklist de aplicativos
- **WHEN** o perfil ativo bloqueia `social_media`, `com.facebook.katana` está catalogado em `social_media` e `com.king.candycrushsaga` está na blocklist de aplicativos
- **THEN** `locked_packages` do launcher contém `com.facebook.katana` e `com.king.candycrushsaga`

#### Scenario: Aplicativo deixa de ser bloqueado
- **WHEN** o sync anterior bloqueou `com.king.candycrushsaga` e o administrador o remove da blocklist
- **THEN** o sync seguinte não contém `com.king.candycrushsaga` em `locked_packages`
- **THEN** o sync seguinte contém `com.king.candycrushsaga` em `unlocked_packages`

#### Scenario: Dispositivo offline durante a liberação
- **WHEN** um dispositivo fica sem sincronizar enquanto o administrador libera um aplicativo e depois volta a sincronizar
- **THEN** a resposta do seu sync contém esse aplicativo em `unlocked_packages`

#### Scenario: Configuração do administrador preservada
- **WHEN** o perfil já tem `locked_packages` igual a `com.android.vending` definido pelo administrador e o filtro bloqueia `com.facebook.katana`
- **THEN** `locked_packages` da resposta contém `com.android.vending` e `com.facebook.katana`
- **THEN** `unlocked_packages` da resposta não contém `com.android.vending`

#### Scenario: Política desativada libera aplicativos
- **WHEN** administrador desativa a política de um perfil que bloqueava `com.facebook.katana`
- **THEN** o sync seguinte contém `com.facebook.katana` em `unlocked_packages` e não em `locked_packages`

### Requirement: Resposta do sync permanece compatível e assinada

Origem: PRD-10, PRD-11

As alterações do filtro SHALL NOT mudar nenhum outro campo da resposta do sync além de `webfilterDnsHost` e das configurações `locked_packages`/`unlocked_packages` do launcher. A resposta SHALL continuar com `X-Response-Signature` válida para o corpo efetivamente enviado.

#### Scenario: Demais campos inalterados
- **WHEN** o mesmo dispositivo faz sync com e sem política ativa no perfil
- **THEN** as duas respostas são iguais campo a campo, exceto `webfilterDnsHost` e as configurações `locked_packages`/`unlocked_packages` do launcher

#### Scenario: Assinatura válida
- **WHEN** o dispositivo recebe resposta de sync com alterações do filtro
- **THEN** a assinatura calculada sobre `data` com o segredo compartilhado é igual a `X-Response-Signature`

#### Scenario: Launcher em produção sem suporte ao DNS do filtro
- **WHEN** o launcher atualmente em produção recebe resposta de sync com `webfilterDnsHost` e `locked_packages`
- **THEN** o launcher aceita a configuração, ignora `webfilterDnsHost` e bloqueia os pacotes de `locked_packages`

### Requirement: Dispositivos são avisados quando a política muda

Origem: PRD-12

Ao salvar política, listas ou categorização de aplicativos do cliente, o sistema SHALL enviar a mensagem push `configUpdated` a cada dispositivo dos perfis afetados.

#### Scenario: Salvar política de perfil com dispositivos
- **WHEN** administrador salva a política de um perfil com 3 dispositivos
- **THEN** o sistema envia `configUpdated` aos 3 dispositivos
- **THEN** nenhum dispositivo de outro perfil recebe a mensagem

#### Scenario: Categorizar aplicativo usado por perfis ativos
- **WHEN** administrador inclui um pacote na categoria `games` e dois perfis ativos do cliente bloqueiam `games`
- **THEN** o sistema envia `configUpdated` aos dispositivos desses dois perfis
