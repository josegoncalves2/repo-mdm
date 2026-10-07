# Handoff — Sessao 2026-10-06 (atualizado 09:15)

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
| 3. Acesso Remoto | PARCIAL | UI funciona, screenshot real existe. Teclado NAO funciona (falta canRetrieveWindowContent). $destroy mata sessao. Wake lock, LAUNCHER, tablet bipando — tudo pendente de APK rebuild |
| 4. GPS | AVANCADO | Layout split OK, historico OK (14 registros), busca OK, status OK, GPX OK. Falta: timeline/playback, heatmap, geocercas, deteccao paradas, multi-formato export, clustering |
| 5. Permissoes | FALHA | 45 permissoes existem. 4 descricoes em RUSSO. Combobox nao verificado se grava. Sem segregacao ver/editar. Sem auditoria nome x efeito |
| 6. Tablet | FALHA | Kiosk QUEBRADO. Suporte remoto nao protegido. Papel de parede nao responsivo. Tudo requer APK rebuild |
| 7. UI/UX Geral | PARCIAL | F5, scrollbar, ShellController, tooltips, traducoes parciais OK. Falta: layout M365, menus Modulos/Integracoes, Quiosque botoes, controle versao, modularidade |
| 8. Infraestrutura | AVANCADO | Containers OK, entrypoint seguro, persistencia OK. Falta: configs via interface web, gerencia containers via UI |

## 5 falhas mais criticas (para proximo agente)

1. **WebFilter no dispositivo** — o tablet acessa TUDO. Backend pronto, dispositivo ignora. Requer alterar launcher para consumir webfilterDnsHost via DevicePolicyManager.setGlobalPrivateDns() ou VPN local
2. **Enrollment** — nenhum tablet foi matriculado apos os fixes. Sem enrollment, nao ha como testar nada no device
3. **Teclado remoto** — input_injection_config.xml sem canRetrieveWindowContent. Requer rebuild APK
4. **Kiosk quebrado** — kioskMode=true no banco mas tablet nao trava. Investigar Device Owner / launcher default
5. **Permissoes RBAC** — 4 em russo, combobox pode ser placebo, sem segregacao, sem auditoria

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
6. **Layout 05-06/10** — Preservado integralmente. CSS, HTML, JS dos commits 2322eee8..dba343d4 no HEAD.
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
