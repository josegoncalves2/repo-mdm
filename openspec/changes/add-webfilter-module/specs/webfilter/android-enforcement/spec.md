## Purpose

Faz o tablet gerenciado aplicar o controle de acesso do seu perfil: resolver nomes somente pelo resolvedor do filtro, sem que o usuário consiga alterar, e bloquear ou liberar aplicativos; desfaz isso quando o filtro é desativado.

## ADDED Requirements

### Requirement: Dispositivo bloqueia e libera aplicativos do perfil

Origem: PRD-10

Em dispositivo em que o MDM é Device Owner, cada pacote instalado presente em `locked_packages` SHALL ficar inacessível ao usuário (suspenso e oculto). Cada pacote presente em `unlocked_packages` SHALL voltar a ficar acessível com seus dados preservados. Isso SHALL funcionar com o launcher atualmente em produção.

#### Scenario: Aplicativo bloqueado
- **WHEN** o dispositivo com `com.king.candycrushsaga` instalado recebe esse pacote em `locked_packages`
- **THEN** o aplicativo não aparece para o usuário e não pode ser aberto

#### Scenario: Aplicativo liberado
- **WHEN** o dispositivo com `com.king.candycrushsaga` bloqueado recebe esse pacote em `unlocked_packages`
- **THEN** o aplicativo volta a aparecer e abre com os dados que tinha antes do bloqueio

#### Scenario: Pacote bloqueado não instalado
- **WHEN** o dispositivo recebe em `locked_packages` um pacote que não está instalado
- **THEN** a aplicação da configuração conclui sem erro para os demais pacotes

### Requirement: Dispositivo aplica o DNS privado do perfil

Origem: PRD-09, PRD-11

Em dispositivo com Android 10 ou superior em que o MDM é Device Owner, ao receber `webfilterDnsHost` no sync, o dispositivo SHALL configurar o DNS privado global em modo hostname com esse valor e SHALL impedir que o usuário altere o DNS privado.

#### Scenario: Aplicar hostname recebido
- **WHEN** o dispositivo recebe `webfilterDnsHost` igual a `id-c1-p44.dns.empresa.com`
- **THEN** o DNS privado do dispositivo fica em modo hostname com `id-c1-p44.dns.empresa.com`
- **THEN** a opção de DNS privado nas configurações do Android fica bloqueada para o usuário

#### Scenario: Hostname alterado
- **WHEN** o dispositivo com hostname aplicado recebe um `webfilterDnsHost` diferente
- **THEN** o DNS privado passa a usar o hostname novo

#### Scenario: Resolvedor inacessível ao aplicar
- **WHEN** o Android recusa o hostname por não conseguir usá-lo como DNS-over-TLS
- **THEN** o dispositivo registra no log remoto do MDM o hostname e o código de erro
- **THEN** o dispositivo tenta aplicar novamente no próximo sync

### Requirement: Filtro de sites vale para todos os apps

Origem: PRD-09, PRD-11

Com o DNS privado do perfil aplicado, nenhum app do dispositivo SHALL resolver domínios bloqueados no perfil.

#### Scenario: Navegador e outros apps
- **WHEN** o perfil bloqueia `social_media` e um app qualquer do dispositivo tenta acessar `facebook.com`
- **THEN** a resolução de `facebook.com` falha no dispositivo
- **THEN** a resolução de um domínio permitido funciona

### Requirement: Dispositivo remove o DNS do filtro quando o campo deixa de vir

Origem: PRD-11

Quando o dispositivo tiver hostname do filtro aplicado e o sync chegar sem `webfilterDnsHost`, o dispositivo SHALL voltar o DNS privado ao modo automático. O dispositivo SHALL liberar o bloqueio de alteração do DNS privado, exceto quando `no_config_private_dns` constar nas restrições do perfil.

#### Scenario: Filtro desativado
- **WHEN** o dispositivo com hostname aplicado recebe sync sem `webfilterDnsHost` e o perfil não restringe DNS privado
- **THEN** o DNS privado volta ao modo automático
- **THEN** o usuário volta a poder alterar o DNS privado

#### Scenario: Perfil também restringe DNS privado
- **WHEN** o dispositivo com hostname aplicado recebe sync sem `webfilterDnsHost` e as restrições do perfil contêm `no_config_private_dns`
- **THEN** o DNS privado volta ao modo automático
- **THEN** a opção de DNS privado continua bloqueada para o usuário

#### Scenario: Dispositivo que nunca aplicou filtro
- **WHEN** o dispositivo sem hostname aplicado recebe sync sem `webfilterDnsHost`
- **THEN** o dispositivo não altera o modo de DNS privado nem as restrições

### Requirement: Mudança nas restrições do perfil não desfaz o filtro

Origem: PRD-11

Enquanto o filtro de sites estiver ativo, a reaplicação das restrições do perfil SHALL NOT liberar o bloqueio de alteração do DNS privado.

#### Scenario: Restrições do perfil alteradas com filtro ativo
- **WHEN** o administrador altera as restrições do perfil e o dispositivo com filtro ativo aplica a configuração nova
- **THEN** a opção de DNS privado continua bloqueada para o usuário
- **THEN** o DNS privado continua em modo hostname com o hostname do perfil

### Requirement: Dispositivo sem suporte ao filtro de sites é sinalizado

Origem: PRD-11

Em dispositivo com Android anterior ao 10, ou em que o MDM não é Device Owner, o dispositivo SHALL NOT alterar o DNS e SHALL registrar no log remoto do MDM que o filtro de sites não é suportado, uma vez por hostname recebido.

#### Scenario: Android 9 recebe hostname
- **WHEN** um dispositivo com Android 9 recebe `webfilterDnsHost` em dois syncs seguidos com o mesmo valor
- **THEN** o DNS do dispositivo não é alterado
- **THEN** o log remoto do MDM contém um único registro de filtro de sites não suportado com a versão do Android
