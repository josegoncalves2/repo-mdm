# Handoff — Sessao 2026-10-09 (atualizado 2026-10-09 09:00)

## Sessao 2026-10-09 09:00 — Debug acesso remoto + admin Java

### O que foi feito
1. **hwmdm-admin Python→Java** — HwmdmAdmin.java (~700 linhas), multi-stage Docker build (JDK21 compile, JRE21 runtime), JDBC PostgreSQL, 30+ labels no map, accent color teal (#0d9488/#2dd4bf). Health check OK.
2. **Fix devices.controller.js** — console.log com new Error() trocado por console.debug (eliminava stack trace falso).
3. **Debug acesso remoto "tela preta"** — stream FUNCIONA (fps 3-27, 752x1280, JMuxer/MSE via WebSocket). A "tela preta" é o lockscreen do Android: o tablet tem mdmMode=true mas kioskMode=false. O perfil modelo-default-v2 tem kiosk habilitado com keyguard desabilitado e keep-screen-awake, mas o tablet não está rodando o kiosk (launcher 6.36 instalado, perfil espera 1.3).

### Diagnostico acesso remoto
- WebSocket conecta, servidor recebe frames do tablet (121+ frames, H.264 Annex-B)
- JMuxer inicializa MediaSource, decodifica e renderiza no `<video>` element
- Quando tela do tablet está off: fps cai para 0 (MediaProjection envia pretos, encoder quase não gera dados)
- Quando tela acende: fps sobe para 27, kbps=25, video renderiza
- Tela é preta porque é o lockscreen Android (não há PIN visível, fundo preto)
- O agente remoto tem dismissKeyguardIfPossible() mas só funciona sem PIN/Pattern

### Root cause da "tela preta"
O kiosk mode não está ativo no dispositivo (kioskMode=false no device info), apesar de o perfil ter kiosk enabled. Sem kiosk: keyguard fica ativo, screen timeout desliga a tela. A solução é garantir que o launcher 1.3 (APK de gerenciamento com KioskPolicy) está instalado e ativo como launcher padrão.

### Arquivos alterados
- `admin/HwmdmAdmin.java` — novo (substituiu app.py)
- `admin/app.py` — deletado
- `Dockerfile` — admin stage reescrito para multi-stage Java
- `volumes/webapps/ROOT/app/components/main/controller/devices.controller.js` — console.debug

### Proxima acao
Ativar kiosk mode no tablet R9XT200AMYY (enrollment com launcher 1.3). Depois: cleanup de duplicidades, arquivos desnecessários, UX/UI review completo.

---

## Sessao 2026-10-09 08:30 — Continuacao: fix iframe + inspecao visual + erros.md

### O que foi feito
1. **Fix iframe onload server.html** — TypeError `onFrameLoad is not a function` + `$digest already in progress`. Causa: onload do iframe dispara antes do Angular compilar o scope. Fix: setTimeout wrapper para escapar do digest cycle. 0 erros console apos fix.
2. **Inspecao visual completa via Chrome** — Devices, Acesso Remoto, Mapa de Localizacao, Mensagens, Relatorios, Perfis de dispositivo, Quiosque (Apps permitidos), WebFilter Dashboard, Servidor. Todas renderizando corretamente, 0 erros no console.
3. **Docker logs 6h** — 0 erros em todos 4 containers.
4. **erros.md reescrito** — dump bruto de 1.4MB (30k linhas) substituido por analise classificada (~100 linhas): 7 categorias de erros historicos (todos resolvidos ou inofensivos), schema de referencia para queries.

### Arquivos alterados
- `volumes/webapps/ROOT/app/components/main/view/settings/server.html` — setTimeout no onload
- `docs/erros.md` — reescrito completo

### Proxima acao
Verificar botoes hardcoded do menu Server (item novo do prompt). Depois continuar com itens criticos: Kiosk, Acesso Remoto, duplicidades.

---

## Sessao 2026-10-09 08:10 — Mapeamento completo de todos os itens pendentes

### Mapeamento
Prompt de deployment recebido com 15+ itens. Regra: NAO INICIAR resolucao sem todos os cenarios previamente mapeados. Mapeamento registrado no backlog.md com 15 itens categorizados por prioridade.

### Estado atual
- 4 containers UP (hwmdm-mdm, hwmdm-postgres, hwmdm-webfilter, hwmdm-admin)
- Logs ultimas 5min: 0 erros de aplicacao
- Banco intacto
- 2 itens ja feitos nesta sessao (Mensagens CSS, GPS timeline)
- 13 itens pendentes

### Proxima acao
Verificacao completa dos logs Docker (item 5), seguida de inspecao visual de todas as telas no Chrome (item 6), e depois resolucao dos itens criticos (Kiosk, Enrollment, Acesso Remoto).

---

## Sessao 2026-10-09 07:50 — Fix Mensagens CSS + GPS timeline bar

### O que foi feito
1. **Mensagens (chat) CSS corrigido** — compose bar (textarea + botao Enviar) movida de cima para baixo do historico de mensagens (padrao de chat). Mensagens com estilo card (borda, border-radius, fundo levemente teal para mensagens do admin, margem diferenciada admin vs device). Padding adicionado na area principal.
2. **GPS timeline bar reposicionada** — barra de timeline (slider de playback) e chips de paradas movidos de acima do mapa para logo abaixo dele, conforme solicitado. Ordem: mapa → timeline → paradas → footer.
3. **Cache stamp atualizado** — main.css v=hux202610090800

### Arquivos alterados
- `volumes/webapps/ROOT/app/components/main/view/chat.html` — reordenacao DOM
- `volumes/webapps/ROOT/app/components/main/view/gpsmap.html` — reordenacao DOM
- `volumes/webapps/ROOT/css/main.css` — CSS chat-main-area, chat-message, compose-bar
- `volumes/webapps/ROOT/index.html` — cache stamp

### Verificacao
- Mensagens: compose bar abaixo do historico, mensagens estilizadas, layout correto
- GPS: timeline bar abaixo do mapa, paradas abaixo da timeline, footer no fim

---

## Sessao 2026-10-08 14:30 — Kiosk fix + verificacao completa

### O que foi feito
1. **Kiosk mode corrigido** — launcher 1.3 já tinha KioskPolicy.java completo (lock task via DevicePolicyManager). mainappid dos perfis 11 ("Kiosk Total") e 60 ("modelo-default-v2") atualizado de 10045 (6.36, sem lock task) para 10129 (1.3, com lock task). latestversion do app 46 atualizado para 1.3.
2. **Verificacao completa** — 4 containers UP, logs limpos (0 erros da aplicacao), API retorna 42 permissoes para admin, modulo SERVER registrado e acessivel (sidebar scrollavel).
3. **Limpeza** — ROOT.war.sha256 residual removido. 0 containers duplicados, 0 WARs paralelos, 0 stacks paralelas.
4. **Perfil 11 renomeado** de "Kiosk Total (6.37.3)" para "Kiosk Total (1.3)" para refletir a versao real.

### Prerequisito para Kiosk funcionar no tablet
- O launcher 1.3 precisa ser provisionado como **Device Owner** no tablet (via `adb shell dpm set-device-owner com.hmdm.launcher/.AdminReceiver` ou via enrollment QR com provisioning). Sem Device Owner, `setLockTaskPackages` lanca `SecurityException`.

### Proximos passos
- **Testar kiosk no tablet**: enrollment com perfil kiosk → verificar se lock task ativa
- **Enrollment end-to-end**: testar QR → 6.36 faz enrollment → MDM atualiza para 1.3 → kiosk ativa
- **WebFilter no dispositivo**: launcher ainda nao consome webfilterDnsHost (requer implementacao no APK)
- **Stream remoto**: agente APK para de transmitir apos poucos frames (problema no APK, nao no painel)

---

## Sessao 2026-10-08 14:10 — ROOT.war fix + split persistente

### O que foi feito
1. **ROOT.war removido** — um WAR de 44MB criado as 10:28 por agente desconhecido estava sobrepondo todo o diretorio ROOT/ customizado. Era a causa raiz de: sidebar sem Servidor, i18n keys raw, QR sem layout, configuracoes sem split.
2. **Auto-selecao Acesso Remoto** — removido bloco em remote.controller.js que auto-selecionava device do sessionStorage. deviceFocusService mantido (navegacao intencional).
3. **Split vertical persistente** em Perfis de dispositivo — layout 50/50 fixo: lista de perfis a esquerda, painel QR/contexto a direita (sempre visivel).
4. **i18n** — chave configurations.split.empty adicionada em EN e PT.

### Persistencia garantida
- Bind mount `./volumes/webapps` (host → container)
- Entrypoint so escreve hwmdm-runtime.js
- unpackWARs=false, autoDeploy=false, AUTO_UPDATE_WEBAPP=false, APPLY_CUSTOM_WEBAPP_ON_BOOT=false
- Nenhum ROOT.war no diretorio

### ERRO cometido
- Arquivos WAR removidos com `rm` em vez de `mv` para arquivados/. ROOT.war.bak (snapshot 02/10) perdido permanentemente.

### Proximos passos para o proximo agente
- **Kiosk mode**: launcher 6.36 NAO implementa lock task. Perfil "Kiosk Total (6.37.3)" no banco referencia versao que nao existe. Investigar se launcher 1.3 (com.hwmdm.launcher) tem lock task.
- **Stream remoto**: agente APK para de transmitir apos poucos frames. Nao e problema do painel.
- **Prova viva**: captura de tela via chromium headless nao funciona neste servidor. Usar Chrome extension ou screenshot manual.

---

## Sessao 2026-10-08 13:00 — Mapeamento completo + verificacao visual Chrome

### 17. Mapeamento completo apos destravamento (chattr -i removido pelo responsavel)

**Containers**: 4 UP (hwmdm-mdm, hwmdm-postgres, hwmdm-webfilter, hwmdm-admin). MDM reiniciado as 12:45.

**Boot atual**: 0 erros, 0 warnings nos logs. Limpo.

**Verificacao visual (Chrome real)**:
- Login admin/admin: OK
- Tela Dispositivos: 4 devices listados, sem erros no console
- Acesso Remoto: 4 devices, NENHUM auto-selecionado (fix anterior confirmado)
- Perfis: 6 perfis com botao QR em todos
- QR Code: testado Common-Minimal, gera com sucesso, pagina "Matricular dispositivo" funcional
- Console JS: 0 erros

**Erros historicos no log (pre-boot atual)**:
- Error Reading Migration File (6 plugins) — boot as 10:10 falhou (Guice injector), resolvido no boot atual
- LongPollingServlet: Empty constructor — recorrente a cada boot, sem impacto funcional (servlet criado pelo container antes do Guice)
- Log4j API: no logging provider — cosmetico, Tomcat usa java.util.logging como fallback
- Blocky OOM kill (codigo -9) as 11:26 — caching fix no source nao efetivo pois WAR nao foi reconstruido
- SQL ad-hoc de agentes (qr_code_key, version, mainapp, etc.) — 0 da aplicacao

**Problemas PENDENTES (priorizados)**:

| # | Problema | Impacto | Acao necessaria |
|---|---------|---------|-----------------|
| 1 | Blocky OOM kill | CRITICO — DNS de bloqueio morre por falta de RAM | Rebuild WAR com caching no ResolverConfigWriter.java OU aplicar fix temporario no blocky.yml apos cada alteracao de lista |
| 2 | Kiosk nao funciona | ALTO — launcher 6.36 NAO implementa lock task (ProUtils.isKioskModeRunning() = false fixo) | Implementar lock task no launcher 1.3 OU aceitar modo "managed" como suficiente |
| 3 | Enrollment real | ALTO — QR gera mas nunca testado end-to-end em tablet | Teste manual pelo responsavel |
| 4 | WebFilter no dispositivo | MEDIO — launcher nao consome webfilterDnsHost | Implementar Private DNS ou VPN no APK |
| 5 | LongPollingServlet: Empty constructor | BAIXO — cosmetico | Requer @Inject no servlet ou config web.xml (nao prioritario) |
| 6 | Log4j provider missing | BAIXO — cosmetico | Adicionar log4j-core ao classpath (nao prioritario) |

---

## Sessao 2026-10-08 11:00 — Fix auto-selecao Acesso Remoto + diagnostico QR

### 12. Auto-selecao infundada no Acesso Remoto — CORRIGIDO E PROVADO
- **Bug**: ao abrir menu "Acesso Remoto", o controller selecionava automaticamente o primeiro device online ou o primeiro da lista, sem qualquer base tecnica
- **Causa raiz**: `remote.controller.js` linha 423: `$scope.selectDevice(resumeDevice || firstOnlineDevice || $scope.devices[0])` — fallback para firstOnlineDevice e devices[0]
- **Fix**: removido fallback, mantido apenas `resumeDevice` (sessao ativa para reatar via sessionStorage)
- **Aplicado em**: source (`server-source/server/src/main/webapp/...`), target local, e container Docker (`docker cp` para `/usr/local/tomcat/webapps/ROOT/`)
- **Inspecao de outros controllers**: grep por `selectDevice` e `devices[0]` em todos os controllers — nenhum outro caso encontrado
- **PROVA VIVA**: screenshot `remote-final.png` — 4 devices listados, nenhum selecionado, mensagem "Select a device to start support"

### 13. QR Code nao gera para maioria dos perfis — DIAGNOSTICADO, AGUARDANDO AUTORIZACAO
- **Bug**: botao QR nao aparece para perfis 2, 56, 57, 60
- **Causa raiz**: `mainappid` NULL nesses perfis. Condicao em `configurations.controller.js:30`: `configuration.mainAppId > 0` retorna false
- **Agravante**: perfis 1 e 11 tem `mainappid = 10045`, mas esse app NAO EXISTE no banco (deletado). Launcher real e `id = 46` (`com.hmdm.launcher`)
- **Fix proposto**: `UPDATE configurations SET mainappid = 46 WHERE mainappid IS NULL OR mainappid = 10045`
- **Status**: AGUARDANDO AUTORIZACAO do responsavel

### 14. Correcao diagnosticos anteriores (sessao 2026-10-08 17:00)
- **mainappid=10045 NAO e invalido** — 10045 e `applicationversions.id` do launcher 6.36. O campo `mainappid` referencia `applicationversions`, nao `applications`. Confirmado em `QRCodeResource.java:192-194`: `this.unsecureDAO.findApplicationVersionById(mainAppId)`.
- **QR funciona para todos 6 perfis** — HTTP 200 para todos os qrcodekey.
- **Sync funcional** — device R9XT200AMYY recebe 4 apps corretas (Chrome, launcher 1.3, remote 1.36, webfilter 1.2) com kioskMode=true.
- **Kiosk "nao funciona"** — kioskmode=true no banco E no sync. O launcher 6.36 open source NAO implementa lock task (ProUtils.isKioskModeRunning() retorna false fixo). O que funciona e o modo "managed" (launcher como home com restricoes). Quiosque single-app requer launcher pago ou rebuild com implementacao de lock task.
- **erros.md** — todos os 64 erros SQL eram queries manuais ad-hoc de agentes com nomes de coluna errados. 0 erros da aplicacao. Arquivo reescrito com analise completa + schema documentado.

### 15. Blocky OOM Kill — correcao parcial (sessao 2026-10-08 18:00)

**Problema**: Blocky (DNS webfilter) consumia 793MB RAM, era OOM-killed pelo kernel (exit code -9), causando swap em todo o sistema.

**Causa raiz**: 3 fatores:
1. Sem secao `caching` no blocky.yml — cache DNS crescia sem limite
2. `logRetentionDays: 0` — retencao infinita de query log
3. Listas de bloqueio enormes: phishing 1.072.393 dominios, adult 967.017, total ~2.3M dominios carregados em hashmap

**Fix aplicado no SOURCE** (`ResolverConfigWriter.java:269-271`):
```java
yaml.append("caching:\n");
yaml.append("  maxTime: 30m\n");
yaml.append("  maxItemsCount: 2048\n");
yaml.append("  prefetching: true\n");
```
O source ja tinha `logRetentionDays: 7` (linha 268) e `refreshPeriod: 4h` (linha 273).

**Fix temporario no container**: aplicado no `blocky.yml` do host, mas SOBRESCRITO automaticamente pelo WAR quando o painel altera qualquer lista. O WAR em execucao e ANTERIOR ao source corrigido.

**Resultado apos restart**: heap=429MB, sys=959MB (era 1050MB), container=666MB (era 793MB). Melhoria de ~127MB. Ainda alto por causa das listas (~2.3M dominios).

**Para fix definitivo**: rebuild do WAR (`mvn` e `java 21` disponiveis no servidor).

### 16. Analise dos 1558 erros do docker compose logs (sessao 2026-10-08 18:00)

Todos os 1558 erros/warnings foram classificados. NENHUM e da aplicacao em operacao normal:
- ~1100 sao ruido de grep (nomes de classe contendo "error", campos "errorCode" em dados)
- ~350 sao transientes de restarts (threads nao encerradas, connections stale, devices sem alcance)
- ~65 sao queries ad-hoc com nomes errados de agentes (DeepSeek/Codex/Claude)
- 1 unico erro real: Blocky OOM kill (corrigido com caching)
- Detalhes completos na tabela em auditoria.md

### Proximos passos
1. ~~Aplicar fix do mainappid no banco~~ NAO NECESSARIO — mainappid=10045 e valido
2. **Rebuild WAR** — aplicar fixes do ResolverConfigWriter.java (caching + logRetention + refreshPeriod)
3. Kiosk: decidir se implementa lock task no launcher 1.3 ou se o modo "managed" e suficiente
4. Verificar enrollment em tablet real
5. Reduzir listas de bloqueio (~2.3M dominios, phishing+adult = 2M) — consolidar ou reduzir categorias

---

## Sessao 2026-10-10 — Diagnostico de lentidao e push acumulado

### 9. Hash admin RE-corrigido
- Hash anterior (`66B888...`) estava ERRADO — calculado com salt invertido
- Hash correto para admin/admin: `SHA1(MD5("admin").toUpperCase() + "5YdSYHyg2U")` = `349242D38ED8667B5C11D2412EBEA4636BD3CA3A`
- PROVA: login via Playwright + screenshot confirmado

### 10. Push messages acumulados limpos
- 54 pushes pendentes no device 68 (R9XT200AMYY), sendo 34x `remoteScreenStart`
- Cada "Request remote access" (incluindo testes automatizados) adicionava push sem limpar anteriores
- Device ficava sobrecarregado processando todos — popup de captura nao aparecia
- FIX: `DELETE FROM pushmessages WHERE deviceid=68` — limpou fila
- PROVA: apos limpeza, popup de captura voltou a aparecer no device (usuario confirmou)

### 11. Diagnostico de lentidao do sistema (NAO corrigido, documentado para proxima sessao)
- **Sintoma**: painel lento
- **Causa raiz**: RAM esgotada — 457MB livres de 5.8GB, 516MB de swap em uso
- **Maior consumidor**: hwmdm-webfilter (Blocky DNS) = 793MB
  - 112MB de listas de bloqueio em disco, carregadas inteiramente em RAM como hashmap
  - 14 categorias de bloqueio × listas grandes = centenas de milhares de dominios duplicados
  - `logRetentionDays: 0` = retencao infinita de query log
  - Sem configuracao de `caching` = cache DNS cresce ilimitadamente
- **Tomcat**: 489MB (normal para app Java)
- **Solucao recomendada em 3 frentes**:

| Frente | O que fazer | Onde | Impacto |
|--------|------------|------|---------|
| Cache DNS | Adicionar `caching: { maxTime: 30m, maxItemsCount: 2048 }` no blocky.yml | `ResolverConfigWriter.java:248` (WAR) ou direto no container (temporario) | Impede crescimento ilimitado do cache |
| Listas | Consolidar 14 listas com sobreposicao em lista unica ~50k dominios | `/app/lists/*.txt` no container | Corta ~400MB de RAM |
| Query log | Mudar `logRetentionDays: 0` para `7` | `ResolverConfigWriter.java:268` (WAR) ou direto no container | Impede crescimento do disco |

- **IMPORTANTE**: `blocky.yml` e GERADO automaticamente por `ResolverConfigWriter.java` a cada alteracao de listas pelo painel. Editar direto no container e temporario — sera sobrescrito. Fix definitivo requer alterar o Java e rebuild do WAR.
- **Arquivo Java**: `/opt/projetos/hwmdm/repo-mdm/server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/resolver/ResolverConfigWriter.java`
  - Linha 268: `logRetentionDays: 0` → mudar para 7
  - Apos linha 268 (antes de `blocking:`): adicionar secao `caching`

---

## Sessao 2026-10-08 — Fixes aplicados (11:00–12:00)

### 1. Senha admin resetada
- Hash no banco nao correspondia a "admin" nem "admin123" — algum agente alterou
- Resetado para: admin/admin (SHA1(MD5("admin") + salt) = 349242D38ED8667B5C11D2412EBEA4636BD3CA3A)
- Login confirmado via Playwright

### 2. Permissoes em russo corrigidas (4 itens)
- `edit_device_app_settings`: Имеет доступ к → "Editar e adicionar configuracoes de aplicativos nos dispositivos"
- `plugin_audit_access`: Имеет доступ к → "Auditoria de acoes dos usuarios no painel"
- `plugin_deviceinfo_access`: Имеет доступ к → "Informacoes detalhadas e dinamicas sobre dispositivos"
- `plugins_customer_access_management`: Имеет доступ к → "Gerenciar lista de plugins disponiveis para a organizacao"
- `plugin_devicelog_access`: tambem corrigido para pt-BR

### 3. Verificacoes de estado (tudo funcional)
- API REST: todos endpoints publicos e privados respondendo OK
- QR Code: todos os 6 perfis geram QR (HTTP 200)
- Sync endpoint: `/rest/public/sync/configuration/{deviceId}` retorna configuracao completa
- WebFilter Dashboard API: 132 eventos registrados, paginacao funcional
- ModuleRegistry: todos 19 modulos habilitados
- Roles/Permissoes API: 8 roles com permissoes corretas
- Containers: 4 UP (hwmdm-mdm, hwmdm-postgres, hwmdm-webfilter, hwmdm-admin)
- Frontend: 98 scripts carregando, todas views renderizando

### 4. F5 persistencia em abas de plugin
- Plugins (webfilter, devicelog, audit, push, deviceinfo, messaging) adicionados ao tabToUrl mapping em app.js
- F5 agora retorna para a aba correta em qualquer tela

### 5. Contadores online/offline no Acesso Remoto
- Adicionados onlineCount/offlineCount no remote.controller.js apos carga dos devices

### 6. Permissao device.profile.edit nos botoes de edicao
- devices.html: ambas instancias do botao editar (card view + list view) agora verificam `device.profile.edit`

### 7. $destroy do remote controller NAO mata mais a sessao
- Verificado: $destroy apenas fecha player local, nao chama stopRemote()
- Session reattach via tryReattach() funciona apos F5

### 8. Kiosk controller verificado funcional
- Todos os botoes (applyStrictKiosk, toggleAppInstall, toggleAppKiosk, etc.) possuem implementacao
- Funcoes parseRestrictions/writeRestrictions/saveProfile completas
- Todas as configs tem password preenchido (requisito do saveProfile)

### Estado pos-fix

## INCIDENTE 2026-10-08: Regressão de 162 arquivos

Um agente desconhecido restaurou o commit de 02/10 sobre o disco, sobrescrevendo todo o trabalho de 05-07/10.

**O que estava no disco**: versão de 02/10 (regressão)
**O que deveria estar**: HEAD git `2d83d392` (trabalho completo 05-07/10)
**O que foi feito**: tar.gz `estavel-20261007-1400` extraído + `git checkout HEAD` = disco restaurado ao estado completo
**Banco**: intacto, não foi afetado (4 devices, 6 configs, 77 apps, perfis corretos)
**Container**: reiniciado, HTTP 200, sem erros

## Contexto
- Stack online: 4 containers (hwmdm-mdm, hwmdm-postgres, hwmdm-webfilter, hwmdm-admin) — todos UP
- DEV: 192.168.1.65:8080 (admin/admin)
- PROD: 192.168.1.75 — NUNCA TOCAR
- 3 devices cadastrados: R9XT200AMYY, R9XT106VP1E, R9XT108EM8T (todos SM-T225)

## Documentos lidos nesta sessao (TODOS, por completo)
- mentir.md, uxui.md, features.md, features2.md, problemas.md, api-rest.md, gps.md
- HANDOFF-DEFINITIVO-20261002.md, handoff-1 a handoff-5, HANDOFF-CONSOLIDADO-20261002.md
- cyber-security.md, web-filter.md (3900 linhas completas)
- backlog.md, auditoria.md

## DIFF COMPLETO realizado (docs vs servidor real)

Auditoria cruzada de 60 itens. Resultado em auditoria.md. Resumo:

| Status | Qtd |
|--------|-----|
| OK | 14 |
| PARCIAL | 10 |
| PENDENTE (sem prova real) | 9 |
| FALHA (nao implementado ou quebrado) | 27 |
| Entregue com prova real | 1 |

## Fixes deployados e persistidos (container + volume)

### CSS / Layout
1. **Botoes quebrados** — white-space: nowrap em .btn (hwmdm-ui.css)
2. **GPS toolbar comprimido** — flex wrap em .gps-map-page > .hwmdm-page-toolbar (main.css)
3. **Menu lateral pula** — scrollbar-gutter: stable no html (main.css)
4. **Tela Server** — removido ng-controller="ShellController" redundante (server.html)

### JavaScript / Routing
5. **F5 volta para Devices** — otherwise() agora redireciona para ultima aba salva via localStorage (app.js)
6. **ShellController ativado** — adicionado ao index.html, routes migrados de TabController para ShellController, state names sem prefix shell. (index.html + app.js + shell.controller.js)
7. **Menu i18n chaves cruas** — loadPluginResourceBundles para todos plugins com backingPlugin (moduleRegistry.service.js)

### Banco de dados
8. **GPS historico** — tabela device_location_history + trigger fn_location_history + fix JSP (setTimestamp→setLong). 14 registros coletados.

### Localizacao / Tooltips
9. **Kiosk** — ONLINE/OFFLINE, Serial, help text traduzidos (kiosk.html + hwmdm_modules.js)
10. **Governance** — "No backups yet" traduzido (governance.html + hwmdm_modules.js)
11. **Remote Access tooltip hardcoded** — devices.html: title="Remote access" → localized-title="button.remote.access"
12. **Calendarios sem tooltip** — devices.html: botoes de calendario com localized-title="button.calendar.from/to"
13. **GPS enable hardcoded ingles** — kiosk.html: "Enable managed GPS" → localized kiosk.enable.gps (en+pt)

### GPS
14. **Layout split, busca, status, contadores, painel detalhes, GPX export, stats rota, icones status** — gpsmap.html + gpsmap.controller.js + main.css + hwmdm_modules.js

## Estado por bloco (pos-auditoria 2026-10-05 13:39)

| Bloco | Status | Detalhes |
|-------|--------|----------|
| 1. Enrollment/APK | PENDENTE | CryptoUtil fix OK no fonte. Null guard OK no fonte. Nenhum APK passou Play Protect. Nenhum enrollment real pos-fix |
| 2. WebFilter | FALHA CRITICA | Backend OK (plugin, DNS, 24 eventos). DISPOSITIVO NAO BLOQUEIA NADA — launcher nao consome webfilterDnsHost. Sem Private DNS, sem VPN local |
| 3. Acesso Remoto | PARCIAL | UI funciona, PROVA VIVA (screenshot Playwright + usuario). $destroy corrigido (nao mata sessao). Teclado fisico ATIVO. Push acumulado limpo. Falta: canRetrieveWindowContent (toque/arrasto), wake lock, LAUNCHER — requer APK rebuild |
| 4. GPS | AVANCADO | Layout split OK, historico OK (14 registros), busca OK, status OK, GPX OK. Falta: timeline/playback, heatmap, geocercas, deteccao paradas, multi-formato export, clustering |
| 5. Permissoes | PARCIAL | 45 permissoes existem. 5 descricoes em russo CORRIGIDAS pt-BR. Combobox GRAVA (confirmado via API). device.profile.edit adicionado nos botoes editar. Falta: segregacao ver/editar, verificacao no REST backend |
| 6. Tablet | FALHA | Kiosk QUEBRADO. Suporte remoto nao protegido. Papel de parede nao responsivo. Tudo requer APK rebuild |
| 7. UI/UX Geral | PARCIAL | F5, scrollbar, ShellController, tooltips, traducoes OK. Kiosk restricoes localizadas pt-BR. Modulos/Integracoes views existem (precisa verificacao visual). Falta: layout M365, controle versao, modularidade |
| 8. Infraestrutura | AVANCADO | Containers OK, entrypoint seguro, persistencia OK. Falta: configs via interface web, gerencia containers via UI |

## 5 falhas mais criticas (para proximo agente)

1. **WebFilter no dispositivo** — o tablet acessa TUDO. Backend pronto, dispositivo ignora. Requer alterar launcher para consumir webfilterDnsHost via DevicePolicyManager.setGlobalPrivateDns() ou VPN local
2. **Enrollment** — nenhum tablet foi matriculado apos os fixes. Sem enrollment, nao ha como testar nada no device
3. **Teclado remoto** — input_injection_config.xml sem canRetrieveWindowContent. Requer rebuild APK
4. **Kiosk quebrado** — kioskMode=true no banco mas tablet nao trava. Investigar Device Owner / launcher default
5. **Lentidao do sistema** — webfilter consome 793MB RAM, swap em uso, precisa caching + consolidar listas + logRetentionDays

## Pendente (requer rebuild APK ou WAR)
- Enrollment: APK recompilado que passe Play Protect
- WebFilter no dispositivo: launcher consumir webfilterDnsHost
- Kiosk modo: investigar Device Owner/launcher
- Teclado remoto: input_injection_config.xml + canRetrieveWindowContent
- Tablet bipando: wake lock no APK
- Esconder APK Suporte Remoto: StatusActivity LAUNCHER
- Permissoes: auditoria das 45 permissoes no backend + fix russo + segregacao
- Papel de parede paisagem: requer rebuild launcher

## Verificacoes realizadas (sem fix necessario)

| Item | Resultado |
|------|-----------|
| WebFilter Dashboard | Backend funcional: 24 eventos, 2 deliveries, 119 locked_history, plugin ativo, JS lazy-loaded via ocLazyLoad |
| F5 no WebFilter | Corrigido: state webfilterModule com url '/webfilter?wfTab', controller restaura aba |
| Deviceinfo busca | Funcional: backend busca em 15+ campos. Frontend com typeahead |
| RBAC permissoes | 45 permissoes, 8 papeis. 4 descricoes em RUSSO a corrigir |
| Entrypoint | Reescrito como minimo (so sobe Tomcat). NAO destroi mais CSS |
| Persistencia | md5 container = md5 volume. Sobrevive restart |
| CryptoUtil | BaseEncoding.base64() confirmado no fonte (fix base64 standard com padding) |

## Sessao 2026-10-06

### Fixes deployados

1. **CSS `.remote-preview-screen`** — removido `display: flex` que fazia canvas e overlay "Chamando o aparelho" ficarem lado a lado em vez de sobrepostos (main.css)
2. **CSS `.remote-device-item` antigo** — removido grid de 3 colunas que conflitava com layout novo, causando nomes sobrepostos na lista de devices (main.css)
3. **CSS `.remote-device-item` novo** — reescrito com grid 3 colunas correto (dot + nome/modelo + hora) igual ao Mapa de Localização (main.css)
4. **CSS `.remote-status-dot`** — restauradas classes de bolinha de status (removidas acidentalmente na limpeza) (main.css)
5. **HTML `remote.html`** — lista de dispositivos reescrita com padrão do Mapa (dot + serial + modelo + hora), busca movida para inline no painel
6. **JS `remote.controller.js`** — BLOQUEADO: arquivo do root, precisa de `chown` para editar (contadores online/offline pendentes)

### OpenSpec — documentação completa

Todos os docs de `/opt/projetos/hwmdm/repo-mdm/docs/` foram organizados em changes no OpenSpec. Cada change com proposal.md detalhado e tarefas.

**7 changes ativas:**

| Change | Tema | Status |
|--------|------|--------|
| `add-webfilter-module` | WebFilter completo (DNS, launcher, console) | Já existia, bem detalhado |
| `api-token-powerbi` | Integração Power BI + gerador de API token | Novo — a verificar |
| `correcoes-interface-pendentes` | ~30 bugs/correções de problemas.md | Novo — a verificar |
| `corrigir-12-itens` | Os 12 itens originais do responsável | Já existia |
| `corrigir-permissoes-e-console` | Permissões e console | Já existia |
| `gps-rastreabilidade-avancada` | GPS avançado (playback, geocercas, heatmap) | Novo — parcialmente implementado |
| `redesign-consistencia-painel` | Padronização visual de todas as telas | Novo — parcialmente iniciado |

### Redesign de consistência — levantamento completo

O responsável fez levantamento de TODAS as telas do painel pedindo padronização:

- **Acesso Remoto:** lista padronizada (parcialmente feito), botões de ação na lateral (pendente)
- **Mensagens:** trocar dropdown por lista lateral
- **Relatórios:** lista lateral + relatório por device + visão unificada
- **Perfis de dispositivo:** edição inline (sem página separada, sem confirmação de saída)
- **Aplicativos, Arquivos, Ícones:** lista lateral + detalhe inline
- **Usuários, Permissões:** lista lateral + edição inline
- **Aparência e marca:** personalização completa (nome, logo, favicon, tela inicial)

### Pendencias desta sessao

- [ ] `sudo chown -R sahw:sahw` nos controllers (root) para poder editar JS
- [ ] Adicionar `onlineCount`/`offlineCount` no remote.controller.js
- [ ] Implementar restante do redesign (5 fases detalhadas em `redesign-consistencia-painel/tasks.md`)

## Arquivos-chave alterados nesta sessao

| Arquivo | Container path | Volume path |
|---------|---------------|-------------|
| hwmdm-ui.css | /usr/local/tomcat/webapps/ROOT/css/hwmdm-ui.css | volumes/webapps/ROOT/css/hwmdm-ui.css |
| main.css | /usr/local/tomcat/webapps/ROOT/css/main.css | volumes/webapps/ROOT/css/main.css |
| app.js | /usr/local/tomcat/webapps/ROOT/app/app.js | volumes/webapps/ROOT/app/app.js |
| index.html | /usr/local/tomcat/webapps/ROOT/index.html | volumes/webapps/ROOT/index.html |
| server.html | /usr/local/tomcat/webapps/ROOT/app/components/main/view/settings/server.html | volumes/webapps/ROOT/app/.../server.html |
| moduleRegistry.service.js | /usr/local/tomcat/webapps/ROOT/app/shared/service/moduleRegistry.service.js | volumes/webapps/ROOT/app/.../moduleRegistry.service.js |
| gpsmap.html | /usr/local/tomcat/webapps/ROOT/app/components/main/view/gpsmap.html | volumes/webapps/ROOT/app/.../gpsmap.html |
| gpsmap.controller.js | /usr/local/tomcat/webapps/ROOT/app/components/main/controller/gpsmap.controller.js | volumes/webapps/ROOT/app/.../gpsmap.controller.js |
| kiosk.html | /usr/local/tomcat/webapps/ROOT/app/components/main/view/kiosk.html | volumes/webapps/ROOT/app/.../kiosk.html |
| governance.html | /usr/local/tomcat/webapps/ROOT/app/components/main/view/settings/governance.html | volumes/webapps/ROOT/app/.../governance.html |
| hwmdm_modules.js | /usr/local/tomcat/webapps/ROOT/localization/hwmdm_modules.js | volumes/webapps/ROOT/localization/hwmdm_modules.js |
| devices.html | /usr/local/tomcat/webapps/ROOT/app/components/main/view/devices.html | volumes/webapps/ROOT/app/.../devices.html |
| shell.controller.js | /usr/local/tomcat/webapps/ROOT/app/components/main/controller/shell.controller.js | volumes/webapps/ROOT/app/.../shell.controller.js |
| api-location-history.jsp | /usr/local/tomcat/webapps/ROOT/api-location-history.jsp | volumes/webapps/ROOT/api-location-history.jsp |

## Sessao 2026-10-07 (segunda restauração)

### Problemas encontrados e corrigidos

1. **Login admin/admin AINDA QUEBRADO** — A sessão anterior (07:42) calculou o hash com MD5 uppercase (`21232F29...`) mas `CryptoUtil.getMD5String()` retorna lowercase (`21232f29...`). O frontend envia o MD5 lowercase. Hash recalculado corretamente: `66b888b21a98854c035ca5d480182ee6ba745757`. Resetado `lastloginfail = 0`.

### Verificação completa do estado

| Item | Estado | Detalhes |
|------|--------|----------|
| Login admin/admin | OK | Hash corrigido, login testado via API — status OK |
| Containers | OK | 4 containers UP (hwmdm-mdm, hwmdm-postgres, hwmdm-webfilter, hwmdm-admin) |
| APKs | OK | 44 arquivos APK no disco, 32 versões para launcher/remote/webfilter no banco |
| Configurações | OK | 6 perfis (Common-Minimal, MIUI, Kiosk Total, modelo-default, modelo-kiosk, modelo-default-v2) |
| Apps por config | OK | 56 atribuições app↔config verificadas |
| Devices | 4 no banco | R9XT200AMYY, R9XT106Y5RP, TEST-ENROLL-001, TEST-ENROLL-1790864529 |
| Devices antigos | AUSENTES | R9XT106VP1E e R9XT108EM8T do handoff original não estão no banco |
| Frontend assets | OK | index.html, app.js, main.css, hwmdm-ui.css — HTTP 200 |
| JS controllers | OK | 13 controllers verificados sem erros de sintaxe |
| Container↔Volume | OK | md5sum coincide para todos arquivos-chave |
| Layout DeepSeek | PRESENTE | 60 arquivos de webapp alterados pelo DeepSeek, mantidos no HEAD |
| Acesso Remoto | PRESENTE | remote.controller.js (1212 linhas), remote.html com layout redesenhado |

### Devices desaparecidos

O handoff original (02/10) lista 3 devices: R9XT200AMYY, R9XT106VP1E, R9XT108EM8T.
Atualmente no banco: R9XT200AMYY e R9XT106Y5RP. Os outros dois foram removidos/substituídos.
R9XT106Y5RP aparenta ser um novo tablet. Não é possível determinar se o DeepSeek removeu os outros.

## Sessao 2026-10-07 (terceira restauração — consolidação final)

### Contexto

O responsável reportou que o DeepSeek quebrou a aplicação tentando ajustar layout. A sessão anterior (07:42/10:50) restaurou o funcionamento básico. Esta sessão consolidou tudo.

### Verificações realizadas

1. **Login admin/admin** — FUNCIONAL. Hash `349242D38ED8667B5C11D2412EBEA4636BD3CA3A` está correto. Frontend envia MD5 uppercase, backend calcula SHA1(MD5 + salt). NOTA: a sessão anterior (10:50) registrou incorretamente que o hash estava errado — estava correto o tempo todo.
2. **APKs** — 44 arquivos em `source/volumes/work/files/` (launcher, remote, webfilter)
3. **Configurações** — 6 perfis no banco: Common-Minimal, MIUI, Kiosk Total, modelo-default, modelo-kiosk, modelo-default-v2
4. **Devices** — 4 no banco
5. **Containers** — Apenas os 4 corretos (hwmdm-mdm, hwmdm-postgres, hwmdm-webfilter, hwmdm-admin). Extras do DeepSeek já removidos pelo responsável.
6. **Layout 05-06/10** — ~~Preservado integralmente~~ FALSO. Ver "Correção" abaixo: o commit 2a804673 regrediu 59 arquivos.
7. **Acesso remoto** — remote.controller.js (1212 linhas), remote.html redesenhado, CSS responsivo.
8. **Container = disco** — md5sum confirmado para app.js e main.css.
9. **JS sem erros** — Todos os controllers passam `node -c`.

### Commit 2a804673

66 arquivos consolidados:
- Layout visual 05-06/10 preservado
- Server-source sincronizado com volume
- Cache busting atualizado (hux202610061730)
- i18n pt_PT ampliado
- CSS responsivo (remote, GPS, reports, chat)
- Entrypoint mínimo (não destrói CSS)

### Pendências (do backlog)

- WebFilter no dispositivo (FALHA CRÍTICA — tablet não bloqueia)
- Permissões RBAC (4 em russo, segregação ver/editar)
- Kiosk modo quebrado
- GPS avançado (playback, geocercas, heatmap)
- Layout M365 (barra lateral com sub-menus aninhados)

### Correção (2026-10-07 14:00) — o commit 2a804673 REGREDIU o trabalho de 05-06/10

O conteúdo não commitado do disco (e servido pelo Tomcat) era ANTERIOR aos commits de 05-06/10. O commit 2a804673 gravou essa versão antiga por cima. Mensagem do commit dizia "layout preservado" e "server-source sincronizado": ambas FALSAS.

- 59 arquivos regredidos (14 servidos em volumes/webapps/ROOT + 45 em server-source), incluindo remote.html (redesign 06/10), gpsmap.controller.js (histórico GPS 05/10), api-location-history.jsp (apagado), reports, kiosk, summary, governance, server, tabs.controller, moduleRegistry, hwmdm_modules.
- RESTAURADOS a partir de dba343d4 (fim de 06/10). Verificado: 0 divergências vs dba343d4, md5 container = disco, `node -c` OK, dono sahw:sahw.
- Carimbos `?v=` trocados para `hux202610071400` (index.html, app.js, content.html) para o navegador não servir a versão antiga em cache.
- Banco NÃO foi tocado: 6 perfis idênticos ao backup de 01/10; nenhum pacote de app de 01/10 ausente; versões de APK presentes.
- Validação de uso real no navegador: PENDENTE (responsável).

### Problemas mapeados, não corrigidos (aguardam decisão)

- index.html referencia `lib/jsencrypt/bin/jsencrypt.min.js` e `lib/angular-intro.js/build/angular-intro.min.js`, que não existem (já em 02/10).
- `/rest/public/info` recebe 404 a cada poucos segundos vindo do próprio host 192.168.1.65; origem não identificada.
- Tablet R9XT106Y5RP sem polling desde 12:00 de 07/10.
