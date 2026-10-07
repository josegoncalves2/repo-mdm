# Auditoria — HWMDM

## Registro de eventos e acoes

| Data/Hora | Agente | Acao | Resultado | Observacao |
|---|---|---|---|---|
| 2026-10-05 | Claude Opus 4.6 | Leitura completa dos docs de handoff, features, problemas, uxui, mentir, gps, api-rest | OK | Contexto carregado |
| 2026-10-05 | Claude Opus 4.6 | Verificacao de containers (docker compose ps) | OK | 4 containers up: hwmdm-mdm, hwmdm-postgres, hwmdm-webfilter, hwmdm-admin |
| 2026-10-05 | Claude Opus 4.6 | Criacao de auditoria.md, backlog.md, handoff.md | OK | Arquivos de rastreamento inicializados |
| 2026-10-05 | Claude Opus 4.6 | Fix CSS botoes quebrados (white-space:normal → nowrap em .btn) | DEPLOYED | hwmdm-ui.css linha 373 |
| 2026-10-05 | Claude Opus 4.6 | Fix CSS GPS toolbar (grid → flex para nao comprimir botoes) | DEPLOYED | main.css .gps-map-page > .hwmdm-page-toolbar |
| 2026-10-05 | Claude Opus 4.6 | Fix i18n plugins (menu mostra chaves cruas) — loadPluginResourceBundles para todos plugins | DEPLOYED | moduleRegistry.service.js |
| 2026-10-05 | Claude Opus 4.6 | Fix F5 volta para Devices — otherwise() agora redireciona para ultima aba salva via localStorage | DEPLOYED | app.js |
| 2026-10-05 | Claude Opus 4.6 | Fix tela Server duplicava ShellController — removido ng-controller redundante | DEPLOYED | server.html |
| 2026-10-05 | Claude Opus 4.6 | Fix menu lateral pula ao trocar aba — scrollbar-gutter:stable no html | DEPLOYED | main.css |
| 2026-10-05 | Claude Opus 4.6 | Persistencia: todos os 5 arquivos alterados copiados para volumes/webapps/ROOT/ | OK | Sobrevive restart |
| 2026-10-05 | Claude Opus 4.6 | GPS historico: tabela device_location_history + trigger + fix JSP (setTimestamp→setLong) | DEPLOYED | DB + api-location-history.jsp |
| 2026-10-05 | Claude Opus 4.6 | ShellController ativado: adicionado ao index.html, routes atualizados, state names corrigidos | DEPLOYED | index.html + app.js + shell.controller.js |
| 2026-10-05 | Claude Opus 4.6 | Kiosk: strings hardcoded traduzidas (ONLINE/OFFLINE, Serial, help text) | DEPLOYED | kiosk.html + hwmdm_modules.js |
| 2026-10-05 | Claude Opus 4.6 | Todos os arquivos copiados para volume com permissoes corretas | OK | Persistido |
| 2026-10-05 | Claude Opus 4.6 | Governance: string hardcoded traduzida (No backups yet) | DEPLOYED | governance.html + hwmdm_modules.js |
| 2026-10-05 | Claude Opus 4.6 | GPS: busca de dispositivos, status online/offline com contadores, painel de detalhes do dispositivo selecionado, exportação GPX, estatísticas de rota (distância/paradas), ícones de status coloridos no mapa, bateria na lista | DEPLOYED | gpsmap.html + gpsmap.controller.js + main.css + hwmdm_modules.js |
| 2026-10-05 | Claude Opus 4.6 | Auditoria WebFilter: plugin ativo, backend funcional (endpoints /dashboard /events/page), 24 eventos e 2 deliveries no DB, JS lazy-loaded via ocLazyLoad, template completo | VERIFICADO | webfilter.module.js, main.html, WebFilterResource.class |
| 2026-10-05 | Claude Opus 4.6 | Auditoria RBAC: 45 permissoes, 8 papeis (Super-Admin/Admin/User/Observer/Helpdesk1-3/Guest), agrupamento por area, traduções pt completas, HIDDEN correto | VERIFICADO | roles.controller.js, role.html, hwmdm_modules.js |
| 2026-10-05 | Claude Opus 4.6 | Auditoria deviceinfo busca: backend busca em number/description/imei/phone/publicIp/model/serial/custom1-3/config/group, frontend com typeahead | VERIFICADO | DeviceMapper.xml, deviceinfo.module.js |
| 2026-10-05 | Claude Opus 4.6 | Fix: botao Remote Access em devices.html com title hardcoded "Remote access" → localized-title="button.remote.access" | DEPLOYED | devices.html + hwmdm_modules.js |
| 2026-10-05 | Claude Opus 4.6 | Fix: botoes de calendario em devices.html sem tooltip → localized-title="button.calendar.from/to" | DEPLOYED | devices.html + hwmdm_modules.js |
| 2026-10-05 | Claude Opus 4.6 | Fix: botao "Enable managed GPS" em kiosk.html hardcoded em ingles → localizado kiosk.enable.gps | DEPLOYED | kiosk.html + hwmdm_modules.js |
| 2026-10-05 | Claude Opus 4.6 | PROVA VIVA: Acesso Remoto funcional — login admin, 3 devices (2 offline, 1 online R9XT200AMYY), IP 192.168.1.116, Android 14, sessão pausada com botão Request | SCREENSHOT | /home/sahw/remote-access.png |
| 2026-10-05 13:39 | Claude Opus 4.6 | DIFF COMPLETO: lidos 21 docs + estado real do servidor (containers, DB, files) | OK | Relatório abaixo |
| 2026-10-06 08:37 | Claude Opus 4.6 | Fix CSS remote-preview-screen: removido display:flex (canvas e overlay lado a lado) | DEPLOYED | main.css |
| 2026-10-06 08:40 | Claude Opus 4.6 | Fix CSS remote-device-item: removido grid antigo conflitante (nomes sobrepostos na lista) | DEPLOYED | main.css |
| 2026-10-06 08:41 | Claude Opus 4.6 | CSS remote-device-item: reescrito com grid 3 colunas padrão do Mapa (dot + main + meta) | DEPLOYED | main.css |
| 2026-10-06 08:41 | Claude Opus 4.6 | CSS remote-status-dot: restauradas classes de bolinha (removidas na limpeza) | DEPLOYED | main.css |
| 2026-10-06 08:42 | Claude Opus 4.6 | HTML remote.html: lista reescrita com padrão do Mapa (dot + serial + modelo + hora) | DEPLOYED | remote.html |
| 2026-10-06 08:42 | Claude Opus 4.6 | HTML remote.html: busca movida de toolbar para inline no painel | DEPLOYED | remote.html |
| 2026-10-06 08:45 | Claude Opus 4.6 | JS remote.controller.js: contadores online/offline | BLOQUEADO | Arquivo do root, precisa chown |
| 2026-10-06 09:00 | Claude Opus 4.6 | OpenSpec: criadas 9 changes a partir de docs/ (depois consolidadas para 7) | OK | openspec/changes/ |
| 2026-10-06 09:10 | Claude Opus 4.6 | OpenSpec: removidas 5 changes irrelevantes (não são do projeto MDM) | OK | Consolidação |
| 2026-10-06 09:15 | Claude Opus 4.6 | Atualização de handoff.md, backlog.md e auditoria.md com trabalho da sessão 2026-10-06 | OK | docs/ |
| 2026-10-07 07:30 | Claude Opus 4.6 | INCIDENTE: MDM offline — Liquibase checksum mismatch no changeset `28.09.19-14:41::seva` causado por alterações do DeepSeek no db.changelog.xml | CORRIGIDO | Limpeza de checksum no banco + restart container |
| 2026-10-07 07:30 | Claude Opus 4.6 | INCIDENTE: Containers duplicados criados pelo DeepSeek (nomes aleatórios tipo aisdhaishd_hwmdm-admin) — já haviam sido removidos antes desta sessão | OK | Apenas os 4 containers corretos presentes |
| 2026-10-07 07:42 | Claude Opus 4.6 | INCIDENTE: Login admin/admin não funcionava — DeepSeek alterou hash da senha no banco (hash antigo: 195C9570D7D3CB14C41000A2B5E81D7B794EBF5B, não corresponde a nenhuma senha conhecida) | CORRIGIDO | Algoritmo: SHA1(MD5(password).toUpperCase() + "5YdSYHyg2U"). Hash correto para "admin": 349242d38ed8667b5c11d2412ebea4636bd3ca3a |
| 2026-10-07 07:42 | Claude Opus 4.6 | DeepSeek alterou 163 arquivos entre commits aa74423b..dba343d4 (10 commits "ajustes") — inclui: changelog Liquibase, CSS, HTML, JS, openspec, logs de acesso, skills do Codex/GitHub | DOCUMENTADO | Análise pendente para separar layout válido de alterações problemáticas |
| 2026-10-07 10:50 | Claude Opus 4.6 | INCIDENTE: Login admin/admin AINDA não funcionava — hash anterior (349242D3) foi calculado com MD5 UPPERCASE mas CryptoUtil.getMD5String retorna lowercase. Hash correto recalculado: 66b888b21a98854c035ca5d480182ee6ba745757 | CORRIGIDO | UPDATE users SET password = '66b888b...', lastloginfail = 0 |
| 2026-10-07 10:50 | Claude Opus 4.6 | Verificação completa: 4 containers UP, 44 APKs no disco, 6 configurações, 4 devices, 13 controllers JS sem erros de sintaxe, todos assets CSS/HTML/JS consistentes entre container e volume | OK | Login funcional, frontend carrega |
| 2026-10-07 13:00 | Claude Opus 4.6 | Sessão de restauração: leitura completa de todos os docs, verificação do estado real | OK | Login funciona (frontend envia MD5 hash, não raw), hash 349242D3 está CORRETO |
| 2026-10-07 13:00 | Claude Opus 4.6 | NOTA: entrada anterior (10:50) sobre hash incorreto estava ERRADA — o hash 349242D38ED8667B5C11D2412EBEA4636BD3CA3A é o correto para admin/admin. O CryptoUtil.getHexString() retorna UPPERCASE. O login funciona normalmente pelo browser | CORRECAO | A API aceita MD5 hash, não senha raw |
| 2026-10-07 13:10 | Claude Opus 4.6 | Commit 2a804673: 66 arquivos — layout 05-06/10 preservado, server-source sincronizado, CSS/JS/HTML validados, cache busting atualizado, i18n ampliado | DEPLOYED | git commit main-v2 |
| 2026-10-07 13:10 | Claude Opus 4.6 | Containers extras do DeepSeek já removidos pelo responsável (admin + postgres duplicados) | OK | Apenas 4 containers corretos presentes |

---

## DIFF: DOCS vs. ESTADO REAL DO SERVIDOR (2026-10-05 13:39)

### Metodologia
Leitura integral de todos os 21 arquivos em `/opt/projetos/hwmdm/repo-mdm/docs/`. Verificação cruzada com: `docker compose ps`, `docker exec` nos containers, `psql` no banco, md5sum nos volumes, grep nos arquivos deployados.

---

### INFRA — COMPLIANCE

| Item | Doc exige | Estado real | Compliance |
|------|-----------|-------------|------------|
| 4 containers nomeados hwmdm-* | problemas.md | hwmdm-mdm, hwmdm-postgres, hwmdm-webfilter, hwmdm-admin — todos UP | OK |
| docker-compose.yaml unico | problemas.md | 1 arquivo, stack inteira | OK |
| Entrypoint nao destroi CSS | problemas.md, handoff-5 | Entrypoint reescrito (minimo, so sobe Tomcat) | OK |
| Persistencia apos reboot | problemas.md | Volume montado, md5 container = volume | OK |
| Nada hardcoded | problemas.md | Parcial — ADMIN_PORT via .env, SQL_* via .env, mas muitas configs do MDM ainda nao sao via interface web | PARCIAL |
| Gerencia containers via interface web | problemas.md | NAO IMPLEMENTADO | FALHA |

### ENROLLMENT — COMPLIANCE

| Item | Doc exige | Estado real | Compliance |
|------|-----------|-------------|------------|
| CryptoUtil base64 fix | handoff-3, backlog | `BaseEncoding.base64()` confirmado no fonte | OK (codigo) |
| GetServerConfigTask null guard | handoff-1 | Codigo alterado, nunca testado em device | PENDENTE (sem prova real) |
| APK recompilado com keystore hwmdm-release.jks | handoff-1 | APKs 1.9/1.10 bloqueados pelo Play Protect. Perfil 57 usa 6.36 | PENDENTE |
| Enrollment testado em tablet real | backlog | NUNCA testado apos fixes | FALHA |

### ACESSO REMOTO — COMPLIANCE

| Item | Doc exige | Estado real | Compliance |
|------|-----------|-------------|------------|
| input_injection_config.xml canRetrieveWindowContent | HANDOFF-CONSOLIDADO 3.1 | NAO implementado | FALHA |
| F5/troca de aba derruba sessao | HANDOFF-CONSOLIDADO 3.3, problemas.md | Fix parcial (F5 routing corrigido no frontend, mas $destroy ainda chama stopRemote) | PARCIAL |
| Tela bloqueada/apagada mata sessao | HANDOFF-CONSOLIDADO 3.4, problemas.md | NAO implementado (requer APK rebuild) | FALHA |
| Device offline perde sessao | HANDOFF-CONSOLIDADO 3.2 | NAO implementado (sem fila de comandos) | FALHA |
| Tablet bipando sem sessao ativa | handoff-4 bug A | NAO investigado/corrigido | FALHA |
| Esconder APK Suporte Remoto (LAUNCHER) | handoff-4 bug C | NAO corrigido (requer APK rebuild) | FALHA |

### WEBFILTER — COMPLIANCE

| Item | Doc exige | Estado real | Compliance |
|------|-----------|-------------|------------|
| Plugin no servidor | HANDOFF-CONSOLIDADO 2.1 | Ativo, 10 tabelas, 24 eventos, 2 deliveries, 119 locked_history | OK (backend) |
| Resolvedor DNS (Blocky) | HANDOFF-CONSOLIDADO 2.2 | Container up, healthy | OK (infra) |
| WebFilter NO DISPOSITIVO | HANDOFF-CONSOLIDADO 2.3, problemas.md | O TABLET NAO BLOQUEIA NADA. launcher nao consome webfilterDnsHost. Sem Private DNS nem VPN local | FALHA CRITICA |
| Recently blocked traffic | problemas.md | 24 eventos no DB mas resultado vazio no psql recente — sem novos registros desde setup | PARCIAL |
| Historico infinito / paginacao | problemas.md | NAO implementado (sem paginacao, sem limpeza) | FALHA |
| Pagina de bloqueio customizada | problemas.md | NAO implementado (mdm.pmeto.local / mdm.olimpia.sp.gov.br configuravel) | FALHA |
| Tela WebFilter frontend funcional | problemas.md | Plugin JS carrega via ocLazyLoad, mas validacao visual NAO feita | PENDENTE |
| Bloqueio parece "macro" | problemas.md | Comportamento de selecionar URL, apagar e digitar — NAO corrigido | FALHA |

### GPS — COMPLIANCE

| Item | Doc exige | Estado real | Compliance |
|------|-----------|-------------|------------|
| Layout split (lista esquerda, mapa centro) | problemas.md, gps.md | DEPLOYED — DOM reordenado, 2 colunas | OK (estrutura) |
| Historico de localizacao | gps.md, problemas.md | Tabela com 14 registros, trigger ativo, API JSP funcional | OK (backend) |
| Polyline, timeline, export | gps.md | Polyline + marcadores + export GPX implementados | PARCIAL (falta timeline/playback/heatmap) |
| Busca de dispositivos | gps.md | Implementado com typeahead | OK |
| Status online/offline com contadores | gps.md | Implementado com icones coloridos | OK |
| Geocercas (geofencing) | gps.md | NAO implementado | FALHA |
| Reproducao de rota (playback) | gps.md | NAO implementado | FALHA |
| Heatmap | gps.md | NAO implementado | FALHA |
| Deteccao de paradas | gps.md | NAO implementado | FALHA |
| Exportacao multi-formato (CSV, XLSX, PDF, KML, GPX, JSON) | gps.md | Apenas GPX implementado | PARCIAL |
| Marker clustering | gps.md | NAO implementado | FALHA |
| Circulo de precisao GPS | gps.md | NAO implementado | FALHA |
| RBAC no GPS | gps.md | Permissao device.gps.view existe mas nao verificada se funciona | PENDENTE |

### PERMISSOES RBAC — COMPLIANCE

| Item | Doc exige | Estado real | Compliance |
|------|-----------|-------------|------------|
| 45 permissoes auditadas | HANDOFF-CONSOLIDADO 5.2, problemas.md | 45 existem no DB, 4 com descricao em RUSSO, nunca auditadas (nome x efeito real) | FALHA |
| Combobox de permissoes funcional | problemas.md, HANDOFF-CONSOLIDADO 5.6 | NAO verificado se grava de verdade | PENDENTE |
| Segregacao ver/editar | problemas.md, HANDOFF-CONSOLIDADO 5.5 | NAO implementado | FALHA |
| Nomes/descricoes corretos pt-BR | HANDOFF-CONSOLIDADO 5.3 | 4 permissoes ainda em RUSSO (edit_device_app_settings, plugin_audit_access, plugin_deviceinfo_access, plugins_customer_access_management) | FALHA |
| Verificacao no REST (nao so na tela) | HANDOFF-CONSOLIDADO 5.4 | NAO auditado | PENDENTE |

### UI/UX — COMPLIANCE

| Item | Doc exige | Estado real | Compliance |
|------|-----------|-------------|------------|
| F5 volta para mesma tela | problemas.md | Fix deployado (localStorage) | OK (precisa validacao visual) |
| Botoes com tooltip/descricao | problemas.md | 11 localized-title em devices.html, varios menus corrigidos | PARCIAL |
| Menu Modulos — erros parsing/css | problemas.md | NAO verificado nesta sessao | PENDENTE |
| Menu Integracoes — erros parsing/css | problemas.md | NAO verificado nesta sessao | PENDENTE |
| Quiosque > Politica de bloqueio — botoes nao funcionam | problemas.md | NAO corrigido (requer backend/device) | FALHA |
| Quiosque > Apps permitidos — botoes estourados css | problemas.md | NAO verificado | PENDENTE |
| Informacao detalhada — busca limitada | problemas.md | Backend busca em 15+ campos, frontend com typeahead | OK |
| Papel de parede paisagem | problemas.md | NAO corrigido (requer APK rebuild) | FALHA |
| Layout estilo M365 | handoff-5, problemas.md | NAO implementado (nao ha barra lateral com sub-menus aninhados) | FALHA |
| Menu lateral estavel / nao pula | handoff-5, problemas.md | scrollbar-gutter:stable deployado | PARCIAL |
| Menu Server fora do padrao | handoff-5 | ShellController ativado, ng-controller redundante removido | PARCIAL |
| Strings hardcoded em ingles | handoff-4, handoff-5 | Kiosk, governance, devices parcialmente traduzidos. 4 permissoes em RUSSO | PARCIAL |
| Controle de versao/build | HANDOFF-CONSOLIDADO 7.5 | NAO implementado | FALHA |
| Modularidade (desativar modulo) | HANDOFF-CONSOLIDADO 7.2 | NAO implementado | FALHA |

### TABLET / APK — COMPLIANCE

| Item | Doc exige | Estado real | Compliance |
|------|-----------|-------------|------------|
| Kiosk modo funcional | handoff-4 bug B | QUEBRADO — nao trava mais | FALHA |
| Suporte remoto nao desativavel | problemas.md | NAO verificado/corrigido (requer APK) | FALHA |
| Kiosk + Proteger Config automaticos | problemas.md | NAO implementado | FALHA |

### DOCS DE CONTROLE — COMPLIANCE

| Item | Doc exige | Estado real | Compliance |
|------|-----------|-------------|------------|
| auditoria.md atualizado | features2.md | ESTE arquivo — atualizado agora | OK |
| backlog.md atualizado | features2.md | Sera atualizado a seguir | PENDENTE |
| handoff.md atualizado | features2.md | Atualizado nesta sessao (sessao 2026-10-05) | OK |

---

## RESUMO QUANTITATIVO

| Status | Quantidade |
|--------|-----------|
| OK (implementado e verificado) | 14 |
| PARCIAL (implementado mas incompleto) | 10 |
| PENDENTE (existe mas sem validacao real) | 9 |
| FALHA (nao implementado ou quebrado) | 27 |
| **TOTAL DE ITENS AUDITADOS** | **60** |
| **ENTREGUE COM PROVA REAL DE USO** | **1** (acesso remoto — screenshot) |
