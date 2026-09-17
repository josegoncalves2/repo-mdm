# Fatia G - Launcher aplica DNS privado

## Resultado visivel

Tablet com launcher novo passa a usar DNS privado do perfil sem usuario tocar nas configuracoes.

## Tarefas

### 7.1 - Campo e decisao pura

**ET:** `WebFilterDnsDecisionTest`.

**EI:** altera `ServerConfig` e cria classe pura em `launcher/webfilter/**`.

**FQ exige:**

- Android 9 retorna unsupported;
- sem Device Owner retorna unsupported;
- host novo aplica;
- host ausente remove;
- restricao `no_config_private_dns` preservada;
- registro unico por hostname unsupported.

### 7.2 - Aplicacao no ConfigUpdater

**ET:** `WebFilterDnsApplierTest`.

**EI:** chama decisao apos restricoes, com DPM atras de interface e persistencia do ultimo host.

**FQ exige:**

- chamada em thread de fundo;
- erro do Android gera log remoto e retry no proximo sync;
- `assembleRelease` passa;
- bloqueio de aplicativos existente continua funcionando;
- mudanca de restricoes nao desfaz filtro ativo.

## Jornada de aceite

J07. FQ nao aceita por teste unitario apenas: precisa ver tablet com launcher novo aplicar e remover DNS privado.
