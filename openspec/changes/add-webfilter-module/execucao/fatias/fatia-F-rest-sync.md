# Fatia F - REST, push e sync

## Resultado visivel

Console e dispositivos recebem a politica pelo caminho real do MDM: endpoints privados, push `configUpdated` e sync assinado.

## Tarefas

### 6.1 - REST privado

**ET:** `WebFilterResourceTest`.

**EI:** implementa resources e filtros.

**FQ exige:**

- 403 sem permissao;
- 404 para perfil de outro cliente;
- 400 para entradas invalidas;
- 200 para fluxos validos;
- `curl` so como complemento, nao aceite final.

### 6.2 - Push `configUpdated`

**ET:** `WebFilterPushTest`.

**EI:** envia notificacao aos dispositivos afetados.

**FQ exige:**

- todos os dispositivos do perfil afetado recebem;
- dispositivos de outro perfil nao recebem;
- categorizacao de app dispara push para perfis ativos relevantes;
- nenhum tipo novo inventado quando `configUpdated` existente resolve.

### 6.3 - SyncResponseHook

**ET:** `WebFilterSyncResponseHookTest` e `tests/test_webfilter_sync.py`.

**EI:** implementa hook com arvore JSON, `webfilterDnsHost` e mesclagem de `locked_packages`/`unlocked_packages`.

**FQ exige:**

- demais campos do sync preservados campo a campo;
- assinatura compativel com launcher;
- hostname segue `id-c<customerId>-p<configurationId>.<dominio>`;
- sem politica ou sem dominio omite campo;
- aplicacoes do administrador preservadas.

### 6.4 - Compatibilidade launcher atual

**Papel:** EJ/FQ com sondas temporarias.

**FQ exige:**

- launcher de producao ignora `webfilterDnsHost` sem quebrar sync;
- `locked_packages` funciona;
- DNS privado nao muda no launcher antigo;
- evidencia principal por tela/tablet (app some/volta e DNS privado nao muda); logcat, `dumpsys` e `settings get` sao complemento.

## Jornada de aceite

J02, J05A/J05B e parte de J07. Sync assinado precisa ser provado contra servidor rodando, mas uso real exige tablet.
