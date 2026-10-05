# Handoff — Sessao 2026-10-05 (atualizado 13:39)

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
