## Context

Motivacao e escopo: ver `proposal.md`. Estado atual verificado no codigo em 2026-09-15:

## Itens que precisam de citacao arquivo:linha antes de executar

O FO nao emite OS de implementacao para os itens abaixo antes de anexar citacao `arquivo:linha` do codigo real. Se a citacao nao existir ou contradisser este design, a tarefa fica BLOQUEADA e o design deve ser revisado.

| Item | Por que bloqueia | Tarefas afetadas |
|---|---|---|
| Formato exato de `locked_packages` e `unlocked_packages` | O sync deve mesclar no formato que o launcher atual realmente interpreta. | 4.3, 6.3, 6.4 |
| Origem do `mainApp`/app principal de quiosque no perfil ou sync | PRD-08 protege quiosque, mas a origem precisa estar no codigo real antes de implementar. | 4.3, 6.3, 9.2 |
| API de push `configUpdated` e definicao de perfis afetados | PRD-12 exige push ao salvar dominio, politica, listas e categorizacao. | 6.2 |
| Caminho real de persistencia de settings do launcher | Necessario para provar retry de DNS privado sem gravar host recusado. | 7.2 |
| Caminho real de publicacao do APK em homologacao | Necessario para ensaiar sem tocar producao. | 10.3 |

**Launcher**
- `com.hmdm.launcher` e Device Owner; `minSdkVersion 26`, `targetSdkVersion 34` (`android-source/app/build.gradle`).
- Ja bloqueia aplicativos: `ConfigUpdater.lockRestrictions()` le as configuracoes `locked_packages` e `unlocked_packages` do proprio pacote do launcher e chama `Utils.lockPackages`, que usa `setPackagesSuspended` e `setApplicationHidden`. Essas configuracoes vem do `applicationSettings` do sync (`SettingsHelper.updateAppSettingsMap`).
- Ja aplica o campo `restrictions` do perfil via `addUserRestriction` (`Utils.lockUserRestrictions`). O manifesto ja declara `MANAGE_DEVICE_POLICY_RESTRICT_PRIVATE_DNS`, sem uso.
- Versao open source: `ProUtils` e stub. Nao ha deteccao em tempo real de app nao listado no perfil.
- Verifica `X-Response-Signature` sobre o texto bruto de `data` e desserializa `ServerConfig` com `@JsonIgnoreProperties(ignoreUnknown = true)` (`GetServerConfigTask`). Campo novo no sync nao quebra launcher antigo.

**Servidor**
- `SyncResource` executa todo `SyncResponseHook` registrado no injector, sob `SecurityContext.init(customerId)`, antes de calcular a assinatura (`SyncResource.java:508`). Nenhum plugin do repositorio usa o hook hoje. `SyncApplicationSettingInt` expoe `packageId`, `name`, `type`, `value`, `readonly`, `lastUpdate`, `variable`.
- Padrao de plugin: `plugins/deviceinfo`.
  - `PluginConfigurationImpl` e modulos Guice (Liquibase, Persistence, Rest, Task).
  - Rotas `/rest/plugins/<id>/<recurso>/private/*` com `JWTFilter`, `AuthFilter`, `PluginAccessFilter`, `PrivateIPFilter`, e `/public/*` com `PublicIPFilter`.
  - Tabelas com `customerId`; registro em `plugins` e `permissions` pelo changelog do plugin; configuracao propria em `plugin_<id>_settings`.
- Plugin so entra no WAR com a linha de dependencia em `server/pom.xml`.
- Parametros do `context.xml` viram `@Named` (`AbstractPersistenceModule`). O WebFilter so le parametros ja existentes (`base.url`, `plugins.files.directory`) e nao cria parametro novo. `plugins.files.directory=/usr/local/tomcat/work/plugins` e montado de `source/volumes/work`.
- Dispositivo pertence a um perfil (`devices.configurationId`) e a N grupos (`deviceGroups`).
- Nao existe `src/test` no servidor nem nos plugins. Android tem `app/src/test` com JUnit 4. Playwright em `tests/playwright`; testes Python em `tests/`.

**Console (customizado neste repositorio)**
- Menu lateral proprio (`hwmdm-sidebar` em `app/components/main/view/content.html`). A entrada de um plugin e gerada a partir de `functionsViewTemplate` da sua linha na tabela `plugins` e so aparece com o plugin ativo para o cliente na tela de Plugins (`tabs.controller.js`, `plugins.controller.js`). O titulo usa `namelocalizationkey`.
- Tema claro/escuro por `data-theme` (`theme.service.js`).
- `pt_BR` usa o pacote `pt_PT` (`app.js`). Plugin carrega `app/components/plugins/<id>/i18n/<idioma>.json`; `en_US` e o pacote de reserva e plugin nao precisa trazer todos os idiomas (`locale.service.js`).
- Defeitos de UI ja corrigidos no console e que a tela nova nao pode reintroduzir (`condicao.md`): titulo fora do padrao do menu lateral, voltar sempre para Devices em vez da tela de origem, contraste ruim no modo escuro, estouro horizontal.
- Testes de UI existentes medem estouro horizontal (`scrollWidth` vs `innerWidth`) em 360/768/1280 px e exigem `HWMDM_BASE_URL` sem fallback (`hwmdm-ui-overhaul.spec.js`, `hwmdm-evidencia-responsivo.spec.js`).

**Producao e publicacao**
- Producao roda o mesmo compose (`source/docker-compose.yaml`) em `/opt/projetos/hwmdm` (`scripts/rollback-para-producao.sh`).
- APK do agente e publicado por `scripts/publicar-apk.sh`, que grava arquivo, hash, versao no perfil e QR numa transacao e reconfere o hash.
- O rollback atual assume que a WAR nao muda schema; esta mudanca muda (tabelas novas do plugin).
- Dump de referencia em `source/sql/hmdm-dump.sql.gz`.

**Fontes de listas de sites (download real verificado)**
- IPFire DBL: `https://dbl.ipfire.org/lists/<slug>/domains.txt`, 14 categorias, CC BY-SA 4.0, pede no maximo 1 download por hora.
- Block List Project: `https://blocklistproject.github.io/Lists/alt-version/<nome>-nl.txt`, Unlicense, diaria, linhas de comentario `#`.
- UT1 (espelho diario da Universite Toulouse Capitole): `https://raw.githubusercontent.com/olbat/ut1-blacklists/master/blacklists/<categoria>/domains`, 80 categorias, CC BY-SA 4.0. Categorias grandes vem partidas (`adult`: `domains.0..2`, ~124 MB).
- As listas registram dominios-pai (ex.: `cloudflare-dns.com`, `quad9.net`) e pressupoem bloqueio dos subdominios.

**Blocky (documentacao oficial)**
- Suporta DoT, cliente por subdominio `id-<nome>`, `clientGroupsBlock`, listas de arquivo local e `POST /api/lists/refresh`.
- `*.example.com` bloqueia o dominio e todos os subdominios. Entrada simples sem curinga nao tem bloqueio de subdominio documentado.
- Allowlist tem precedencia sobre denylist em todos os grupos do cliente.
- Cliente cujos grupos tenham todos so allowlist entra em modo exclusivo (bloqueia todo o resto).

## Goals / Non-Goals

**Goals:**

- Bloqueio de sites por categoria, allowlist e blocklist valido para todos os apps do tablet, com nova versao do launcher publicada pelo fluxo existente.
- Bloqueio de aplicativos por categoria, allowlist e blocklist usando o mecanismo que o launcher em producao ja tem.
- MDM existente alterado somente nos 3 pontos de integracao do `proposal.md` (Impact).
- Politica salva no console aplicada no tablet sem acao manual.
- Caminho de publicacao em producao ensaiado e reversivel.

**Non-Goals:**

- Alterar `source/docker-compose.yaml`, `context.xml`/`context_template.xml`, classes do core do servidor ou comportamento do launcher alem do DNS privado.
- Log de consultas DNS e metricas do resolvedor (desativados).
- Politica diferente por usuario ou por grupo.

## Decisions

### D1: Sites por DNS privado do Android forcado pelo Device Owner

O launcher chama `DevicePolicyManager.setGlobalPrivateDnsModeSpecifiedHost` (API 29) com o hostname do perfil e aplica `UserManager.DISALLOW_CONFIG_PRIVATE_DNS` (API 29). Todo app do tablet passa a resolver nomes por DNS-over-TLS no resolvedor do filtro.

Alternativas descartadas:
- `VpnService` com APK companion: APK novo, tunel, e risco de o lockdown do Always-on derrubar a conexao do proprio MDM.
- DNS por DHCP/Wi-Fi: nao cobre dados moveis.
- Politicas do Chrome: so valem para o Chrome.

Consequencia: o filtro de sites e por dominio. Acesso por IP literal nao e filtrado (non-goal).

### D2: Aplicativos pelo mecanismo `locked_packages` do launcher

O plugin calcula os pacotes bloqueados do perfil e os entrega no sync como configuracoes do pacote `com.hmdm.launcher`:

- `locked_packages`: pacotes a bloquear (suspensos e ocultos).
- `unlocked_packages`: pacotes que o WebFilter ja bloqueou neste perfil e agora libera.

Regras da mesclagem:
- Valores que o administrador ja tenha definido nessas configuracoes no perfil sao preservados: `locked_packages` final = do administrador ∪ WebFilter.
- `unlocked_packages` nunca contem pacote que o administrador bloqueou.
- O historico `plugin_webfilter_locked_history` guarda cada pacote que o WebFilter bloqueou no perfil. Liberados = historico − bloqueados agora − bloqueados pelo administrador. Com isso a liberacao chega tambem a dispositivo que estava offline no sync anterior.

Por que: funciona no launcher ja instalado nos tablets de producao, sem atualizacao. `setApplicationHidden` preserva os dados do app; liberar o pacote o devolve intacto.

Alternativa descartada: nova logica de bloqueio no launcher (mais codigo no MDM sem ganho funcional).

### D3: Resolvedor Blocky no container `webfilter-dns`

Blocky, com versao fixada por digest da imagem, porque atende o necessario (ver Context).

Alternativas descartadas:
- AdGuard Home: nao tem listas por cliente, so regras com `$client`, inviavel com milhoes de dominios.
- Unbound/CoreDNS: views por IP de origem, inuteis com NAT e dados moveis.
- Servico gerenciado: consultas dos tablets saem para terceiro.

Grupos configurados:
- **Um grupo por categoria**, compartilhado entre perfis, para cada lista existir uma unica vez na memoria.
- **Um grupo por perfil** `c<customerId>-p<configurationId>`: denylist = blocklist de dominios do perfil; allowlist = allowlist do perfil + dominios protegidos do MDM (D6).
- `clientGroupsBlock` associa o cliente do perfil ao seu grupo e aos grupos das categorias bloqueadas. `doh` sempre presente garante denylist no conjunto e evita o modo exclusivo de allowlist.

Normalizacao: toda entrada de lista (fontes e listas do perfil) e gravada como `*.<dominio>`, para bloquear subdominios de forma documentada.

A identificacao por SNI, a normalizacao, a precedencia da allowlist, a ausencia de modo exclusivo e o custo de reinicio sao provados no grupo 1 de `tasks.md` antes de qualquer outra tarefa.

### D4: Hostname DNS por perfil

Formato: `id-c<customerId>-p<configurationId>.<dominio DNS do filtro>`.

O dominio DNS do filtro e configuracao do plugin (`plugin_webfilter_settings.dnsDomain`, um por cliente), editada na tela do plugin. Vazio = `webfilterDnsHost` nunca enviado; o bloqueio de aplicativos continua funcionando.

Requer registro DNS curinga `*.<dominio>` e certificado curinga valido publicamente (o Android valida a cadeia no modo estrito). Emissao curinga no Let's Encrypt exige desafio DNS-01; o `server-source/letsencrypt-ssl.sh` atual usa `--standalone` (HTTP-01) e nao serve.

### D5: Catalogos de categorias versionados no plugin

**Sites:** recurso `webfilter-catalog.json`, fonte unica para API, console e geracao do resolvedor.

| Categoria MDM | IPFire DBL | Block List Project | UT1 |
|---|---|---|---|
| `adult` | `porn` | `porn` | `adult`, `porn` |
| `gambling` | `gambling` | `gambling` | `gambling` |
| `games` | `games` | - | `games` |
| `social_media` | `social` | `facebook`, `tiktok`, `twitter` | `social_networks` |
| `streaming` | `streaming` | `youtube` | `audio-video` |
| `shopping` | `shopping` | - | `shopping` |
| `dating` | `dating` | - | `dating` |
| `violence` | `violence` | - | `violence`, `aggressive` |
| `drugs` | - | `drugs` | `drugs` |
| `piracy` | `piracy` | `piracy`, `torrent` | `warez` |
| `malware` | `malware` | `malware`, `ransomware` | `malware` |
| `phishing` | `phishing` | `phishing`, `scam`, `fraud` | `phishing` |
| `vpn_proxy` | - | - | `vpn`, `proxy` |
| `doh` (obrigatoria) | `doh` | - | `doh` |

**Aplicativos:** recurso `webfilter-app-catalog.json` com pacotes Android por categoria. Nao existe fonte aberta equivalente as listas de dominios. O catalogo inicial e mantido no repositorio, e todo pacote precisa existir na Google Play (verificado por teste). Cada cliente amplia o catalogo com seus proprios pacotes em `plugin_webfilter_app_categories`. `doh` nao tem aplicativos.

### D6: Ordem de decisao e protecao do MDM

Para dominio (no resolvedor) e para pacote (no plugin), a mesma ordem:

1. allowlist do perfil -> permitido;
2. blocklist do perfil -> bloqueado;
3. pertence a categoria bloqueada no perfil -> bloqueado;
4. demais -> permitido.

A ordem e a mesma precedencia nativa do Blocky (allowlist vence). A mesma entrada exata nao pode estar nas duas listas do perfil (validacao).

Protecoes do MDM:
- **Dominios protegidos**, sempre na allowlist gerada de todo perfil: host de `base.url` e `*.<dominio DNS do filtro>`.
- **Pacotes protegidos**, que o sistema recusa em blocklist e nunca bloqueia por categoria:
  - os agentes do MDM `com.hmdm.launcher` e `com.hwmdm.remote`, listados no recurso do catalogo de aplicativos;
  - o app principal de quiosque do perfil (`mainApp` da resposta do sync ou outra origem real citada em `arquivo:linha` antes da OS). **Origem: PRD-08.**

### D7: Modelo de dados

- `plugin_webfilter_policies`: `id`, `customerId` (FK `customers`, cascade), `configurationId` (FK `configurations`, cascade, unico), `enabled`, `updatedAt`, `updatedBy`.
- `plugin_webfilter_policy_categories`: `policyId` (FK cascade), `category`, unico por (`policyId`, `category`).
- `plugin_webfilter_policy_entries`: `policyId` (FK cascade), `kind` (`DOMAIN`, `APP`), `list` (`ALLOW`, `BLOCK`), `value`, unico por (`policyId`, `kind`, `value`). A unicidade impede a mesma entrada nas duas listas.
- `plugin_webfilter_app_categories`: `id`, `customerId` (FK cascade), `packageName`, `category`, unico por (`customerId`, `packageName`, `category`).
- `plugin_webfilter_locked_history`: `policyId` (FK cascade), `packageName`, unico por (`policyId`, `packageName`).
- `plugin_webfilter_settings`: `id`, `customerId` (FK `customers`, cascade, unico), `dnsDomain`.

`doh` nao e gravada; e acrescentada na leitura e na geracao sempre que `enabled=true`. Valores de dominio sao gravados sem `*.`; a normalizacao acontece na geracao.

### D8: Plugin e resolvedor conversam por arquivos em volume compartilhado

O plugin grava em `<plugins.files.directory>/webfilter/dns/`, com escrita atomica (arquivo temporario + rename):

- `blocky.yml`: listener DoT, certificado, grupos de categoria (arquivos de lista locais), grupos de perfil, `clientGroupsBlock` apenas para politicas ativas, `blockType: nxDomain`, log de consultas desativado.
- `profiles/c<customerId>-p<configurationId>-deny.txt` e `profiles/c<customerId>-p<configurationId>-allow.txt`: listas do perfil ja normalizadas.
- `sources.json`: arquivo local -> URLs da fonte (partes multiplas concatenadas).

O plugin regenera ao salvar politica, listas ou dominio, na inicializacao e a cada 10 minutos (tarefa do plugin; cobre perfis removidos).

O container `webfilter-dns` tem codigo e `docker-compose.yaml` proprios em `repo-mdm/webfilter-dns/`, sem alterar o compose do MDM. Ele monta somente leitura `../source/volumes/work/plugins/webfilter/dns` e roda dois processos Python. O resolvedor so fica pronto depois de carregar uma configuracao valida e listas locais validas; durante inicializacao ou troca de configuracao ele nao pode responder sem filtro.

- **supervisor:** inicia o Blocky. Quando o sha256 de `blocky.yml` muda (perfil ativado/desativado ou categorias alteradas), valida a nova configuracao e reinicia em modo fail-closed; se o Blocky nao ficar saudavel com listas carregadas, volta para a ultima configuracao boa e nao deixa porta 853 responder por uma instancia sem filtro. Quando so arquivos de `profiles/` mudam (allowlist/blocklist), chama `POST /api/lists/refresh` sem reiniciar.
- **updater:** baixa as fontes de `sources.json` a cada 24 h (minimo 1 h por URL), normaliza para `*.<dominio>`, grava em temporario, rejeita conteudo sem dominios, troca o arquivo e chama refresh. Em falha, mantem a lista anterior.

Arquivos, e nao API, porque o Blocky nao documenta recarga de `clientGroupsBlock` sem reinicio e o container `hmdm` nao deve ter acesso ao socket do Docker nem a rede do resolvedor.

### D9: Entrega ao launcher pelo `SyncResponseHook`

O plugin registra `WebFilterSyncResponseHook`. O hook:

1. converte a resposta original em arvore JSON com o mesmo `ObjectMapper` usado por `CryptoUtil.getDataSignature`;
2. acrescenta `webfilterDnsHost` quando aplicavel (D4);
3. mescla em `applicationSettings` as configuracoes `locked_packages`/`unlocked_packages` do pacote `com.hmdm.launcher` (D2);
4. devolve implementacao de `SyncResponseInt` que serializa a arvore via `@JsonValue` e cujos getters refletem os valores mesclados.

A arvore preserva todo campo do `SyncResponse` real, inclusive os nao declarados na interface. O hook consulta perfil, politica e catalogos sob o `customerId` ja inicializado pelo `SyncResource`.

Ao salvar politica, listas ou categorizacao de aplicativos, o plugin envia `configUpdated` (tipo de push existente) aos dispositivos dos perfis afetados.

### D10: Launcher (somente DNS privado)

- Campo `webfilterDnsHost` em `ServerConfig`.
- Classe de decisao pura (sem Android) que recebe `sdkInt`, `isDeviceOwner`, host recebido, ultimo host aplicado e restricoes do perfil, e devolve `APPLY(host)`, `REMOVE(liberarRestricao)`, `UNSUPPORTED` ou `NONE`.
- Executor chamado no `ConfigUpdater` depois do passo de restricoes do perfil, em thread de fundo (a chamada ao DPM faz rede):
  - `APPLY`: `setGlobalPrivateDnsModeSpecifiedHost`; se o retorno nao for `PRIVATE_DNS_SET_NO_ERROR`, registra no log remoto, nao persiste o host como aplicado, nao trata a tentativa como sucesso e tenta novamente no proximo sync; aplica `DISALLOW_CONFIG_PRIVATE_DNS` somente depois de sucesso ou conforme decisao explicita coberta por teste.
  - `REMOVE`: `setGlobalPrivateDnsModeOpportunistic`; libera `DISALLOW_CONFIG_PRIVATE_DNS` so se `no_config_private_dns` nao estiver nas restricoes do perfil; apaga o host persistido.
  - `UNSUPPORTED`: registra no log remoto uma vez por host recebido.
- O bloqueio de aplicativos nao muda no launcher (D2).

### D11: Estrategia de testes

- **Plugin:** JUnit 5 + Mockito; PostgreSQL 12 (mesma imagem do compose) via Testcontainers para Liquibase e mappers, aplicando o changelog do core e o do plugin em banco limpo.
- **Resolvedor:** pytest para supervisor e updater (servidor HTTP local); integracao com o container real e consultas DoT via `dnspython` com SNI por perfil e CA de teste.
- **Catalogo de aplicativos:** teste que confirma cada pacote na Google Play.
- **Launcher:** JUnit 4 existente para a classe de decisao.
- **Console:** Playwright em `tests/playwright`, no padrao das specs existentes.
- **Dispositivo:** evidencia em tablet real via `adb`.

### D12: Publicacao em producao

Agentes nao publicam em producao. A publicacao e executada pelo responsavel do ambiente, com autorizacao explicita registrada, seguindo `docs/WEBFILTER.md`. O fiscal verifica a evidencia. A mesma sequencia e ensaiada antes em homologacao restaurada com copia do banco de producao:

1. Backup: `pg_dump` do banco, WAR em uso, APK do launcher em uso e commit/tag da arvore.
2. Pre-requisitos do filtro de sites: registro `*.<dominio>` e certificado curinga.
3. Deploy do WAR com o plugin; conferir no log o changelog `webfilter` aplicado e o plugin listado.
4. Subir o resolvedor: `docker compose -f webfilter-dns/docker-compose.yaml up -d`.
5. Publicar o launcher novo com `scripts/publicar-apk.sh`; conferir a atualizacao em um tablet piloto sem nova matricula.
6. Conceder `plugin_webfilter_access`, configurar dominio e ativar politica apenas no perfil piloto; validar sites e aplicativos; so entao ampliar para os demais perfis.

**Rollback funcional:** desativar as politicas. O proximo sync libera os aplicativos (`unlocked_packages`) e remove `webfilterDnsHost`; o launcher volta o DNS privado ao modo automatico.

**Rollback tecnico:** somente depois do rollback funcional confirmado em todos os dispositivos.
1. Parar o resolvedor.
2. Restaurar a WAR anterior. As tabelas `plugin_webfilter_*` ficam inertes.
3. Remover as tabelas e o registro do plugin so com `pg_dump` novo e script de remocao ensaiado em homologacao.
4. O launcher novo pode permanecer: sem o campo, ele nao altera o DNS.

### D13: Interface no console existente

A gestao do modulo fica inteira no console web do MDM, como plugin:

- **Registro:** linha em `plugins` no changelog do plugin, no padrao de `deviceinfo`: `identifier = webfilter`, `javascriptModuleFile`, `functionsViewTemplate` (tela principal), `namelocalizationkey`, `functionsPermission = plugin_webfilter_access`. Isso cria a entrada "Web Filter" no menu lateral sem alterar o console.
- **Tela unica com tres abas:**
  - Politicas por perfil: categorias, allowlist/blocklist de dominios e aplicativos;
  - Aplicativos: categorizacao de pacotes do cliente;
  - Configuracao: dominio DNS do filtro e fontes das listas.
- **Padrao visual:** reusa as classes e variaveis de tema do console, sem tema proprio; funciona nos temas claro e escuro, sem estouro horizontal em 360/768/1280 px.
- **Navegacao:** voltar da edicao retorna a lista do Web Filter.
- **Idiomas:** `i18n/en_US.json` (reserva) e `i18n/pt_PT.json` (usado tambem para `pt_BR`).

Alternativa descartada: tela em `settingsViewTemplate` (Configuracoes). A gestao diaria de politicas e funcao operacional, nao configuracao do sistema.

## Risks / Trade-offs

| Risco | Mitigacao |
|---|---|
| Resolvedor fora do ar deixa tablets com filtro de sites sem internet | `restart: unless-stopped`, healthcheck; jornada 9.5 prova a recuperacao por tela; HA fora de escopo |
| Reinicio do Blocky ao mudar categorias deixa o DNS indisponivel enquanto carrega listas | listas locais; allowlist/blocklist so fazem refresh; supervisor fail-closed; tarefa 1.6 mede; acima de 60 s o fiscal para e D3/D8 sao revistos |
| Memoria com listas grandes (UT1 `adult` ~124 MB) e entradas curinga | tarefa 1.6 mede RSS; limite do compose definido pela medicao |
| Blocky nao identificar cliente por SNI ou nao respeitar curinga, precedencia ou modo exclusivo | provado no grupo 1 antes do resto; falha = parar e revisar D3 |
| Catalogo inicial de aplicativos com cobertura limitada | categorizacao propria do cliente; teste garante que todo pacote existe |
| Bloquear app essencial (ex.: navegador, loja) | decisao do administrador; pacotes do MDM protegidos; app liberado volta com dados preservados |
| Mesclagem apagar `locked_packages` configurado a mao no perfil | uniao preservando valores do administrador; testes dedicados (4.3, 6.3) |
| Falso positivo/negativo das listas abertas | allowlist e blocklist do perfil |
| IP literal, app com DoH em IP fixo e VPN de terceiros contornam o filtro de sites | fora de escopo; `doh` sempre bloqueada; categoria `vpn_proxy` bloqueia apps e dominios de VPN; restricao `no_config_vpn` ja existente |
| Portal cativo de Wi-Fi publico nao abre com DNS estrito | documentado em `docs/WEBFILTER.md` |
| Porta 853 aceita qualquer cliente | so TCP/853 exposta, sem 53/UDP publico; sem amplificacao; aceito |
| Certificado curinga exige DNS-01 | pre-requisito; sem ele o DPM retorna `HOST_NOT_SERVING`, o launcher registra erro e os apps continuam bloqueados |
| Hook alterar o JSON do sync ou a assinatura | teste campo a campo e verificacao de assinatura com a regra do launcher (6.3, 6.4) |
| Migracao de schema em producao | backup obrigatorio; script de remocao ensaiado (10.2); ensaio com copia do banco de producao (10.3) |

## Rastreabilidade PRD

Cada requisito das specs deve declarar `Origem: PRD-nn`. Uma OS que cite requisito sem origem PRD e invalida. As origens aceitas sao as listadas em `proposal.md`.
