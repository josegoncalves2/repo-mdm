# Backlog — HWMDM (atualizado 2026-10-05 13:39)

## Status geral: 27 FALHAS, 10 PARCIAIS, 9 PENDENTES, 14 OK

---

### Prioridade 1 — Enrollment (BLOQUEADOR: sem isso nada funciona no device)
- [ ] Confirmar CryptoUtil.java — fix base64 APLICADO no fonte (OK)
- [ ] Confirmar GetServerConfigTask.java — null guard APLICADO no fonte, NUNCA testado em device
- [ ] Recompilar APK com keystore hwmdm-release.jks SEM disparar Play Protect
- [ ] Testar enrollment em tablet real
- [ ] Prova: screenshot tablet matriculado + status no painel

### Prioridade 2 — WebFilter NO DISPOSITIVO (FALHA CRITICA)
- [ ] Launcher nao consome webfilterDnsHost do sync — NUNCA implementado
- [ ] Implementar Private DNS via DevicePolicyManager OU VPN local
- [ ] Testar bloqueio real: acessar site bloqueado no Chrome, ver pagina de bloqueio
- [ ] Pagina de bloqueio customizada (mdm.pmeto.local DEV / mdm.olimpia.sp.gov.br PROD) configuravel via interface
- [ ] Paginacao e limpeza de historico de bloqueios
- [ ] Corrigir comportamento "macro" (seleciona URL, apaga, digita URL de bloqueio)
- [ ] Prova: screenshot/video do bloqueio no tablet

### Prioridade 3 — Acesso Remoto
- [ ] Fix input_injection_config.xml (canRetrieveWindowContent) — requer rebuild APK
- [ ] Remover LAUNCHER intent-filter do StatusActivity — requer rebuild APK
- [ ] F5/troca de aba: $destroy ainda chama stopRemote — corrigir controller
- [ ] Tela bloqueada/apagada mata MediaProjection — requer wake lock no APK
- [ ] Tablet bipando sem sessao ativa — investigar RemoteAgentService
- [ ] Device offline: implementar fila de comandos pendentes
- [ ] Prova: video do teclado funcionando + sessao que sobrevive F5

### Prioridade 4 — Permissoes RBAC
- [ ] Auditar 45 permissoes: nome x efeito real x onde verificada
- [ ] Corrigir 4 descricoes em RUSSO → pt-BR (edit_device_app_settings, plugin_audit_access, plugin_deviceinfo_access, plugins_customer_access_management)
- [ ] Verificar se combobox de permissoes GRAVA de verdade
- [ ] Implementar segregacao ver/editar
- [ ] Verificacao no REST (nao so na tela)
- [ ] Prova: usuario sem permissao nao consegue editar (via tela E via REST)

### Prioridade 5 — GPS (funcionalidades avancadas)
- [x] Layout split (lista esquerda, mapa centro) — DEPLOYED
- [x] Historico de localizacao (tabela + trigger + API) — DEPLOYED, 14 registros
- [x] Busca de dispositivos com typeahead — DEPLOYED
- [x] Status online/offline com contadores — DEPLOYED
- [x] Export GPX — DEPLOYED
- [ ] Polyline com timeline/playback/animacao
- [ ] Heatmap
- [ ] Deteccao de paradas
- [ ] Geocercas (geofencing) com alertas
- [ ] Exportacao multi-formato (CSV, XLSX, PDF, KML, JSON) — so GPX existe
- [ ] Marker clustering
- [ ] Circulo de precisao GPS
- [ ] Comparacao de historico entre devices

### Prioridade 6 — Tablet / APK
- [ ] Kiosk modo QUEBRADO — nao trava mais. Investigar Device Owner/launcher
- [ ] Suporte remoto nao desativavel/desinstalavel pelo usuario
- [ ] Kiosk + Proteger Config automaticos apos permissoes
- [ ] Papel de parede responsivo em modo paisagem — requer APK rebuild

### Prioridade 7 — UI/UX Geral
- [x] F5 volta para mesma tela (localStorage) — DEPLOYED
- [x] Traducao Kiosk (ONLINE/OFFLINE, Serial) — DEPLOYED
- [x] Traducao Governance (No backups yet) — DEPLOYED
- [x] GPS toolbar flex — DEPLOYED
- [x] Scrollbar-gutter stable — DEPLOYED
- [x] ShellController ativado — DEPLOYED
- [x] Tooltips localizados em devices.html — DEPLOYED
- [ ] Menu Modulos — erros parsing/css — NAO verificado
- [ ] Menu Integracoes — erros parsing/css — NAO verificado
- [ ] Quiosque > Politica de bloqueio — botoes nao funcionam
- [ ] Quiosque > Apps permitidos — botoes estourados css
- [ ] Layout estilo M365 (barra lateral com sub-menus aninhados) — NAO implementado
- [ ] Controle de versao/build — NAO implementado
- [ ] Modularidade (desativar modulo sem parar tudo) — NAO implementado
- [ ] Validacao visual de TODAS as telas por UX/UI

### Prioridade 8 — Infra
- [x] docker-compose.yaml unico — OK
- [x] Containers nomeados hwmdm-* — OK
- [x] Entrypoint nao destrutivo — OK (reescrito)
- [x] Persistencia no volume — OK (md5 match)
- [ ] Nada hardcoded — PARCIAL (muitas configs nao estao na interface web)
- [ ] Gerencia de containers via interface web — NAO implementado
