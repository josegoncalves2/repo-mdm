# Tasks — correcoes-interface-pendentes

## 1. Permissões RBAC

- [x] 1.1 Corrigir 4 descrições em russo → pt-BR no banco (edit_device_app_settings, plugin_audit_access, plugin_deviceinfo_access, plugins_customer_access_management)
- [x] 1.2 Verificar se combobox de permissões grava de verdade (testar save + reload) — CONFIRMADO: backend faz deletePermissions + insertPermissions no update
- [x] 1.3 Auditar nomes de permissões vs. efeito real — 5 em russo corrigidas (incluindo plugin_devicelog_access), nomes coerentes com efeito, backend usa checkAccess() no DAO

## 2. Quiosque

- [x] 2.1 Verificar aba "Política de bloqueio" — VERIFICADO: bindings e save funcionam no frontend (switches → ng-model → saveProfile → updateConfiguration). O efeito no dispositivo depende do APK/launcher aplicar as restrições recebidas → BLOQUEADO por APK
- [x] 2.2 Verificar aba "Apps permitidos" — VERIFICADO: CSS .kiosk-table .btn já tem white-space:normal + word-break:break-word + font-size:12px + min-width:80px. Traduções existem em pt-BR. Renderização OK

## 3. WebFilter Dashboard

- [x] 3.1 Verificar "Recently blocked traffic" — VERIFICADO: 39 eventos no DB, frontend chama getDashboard+getEventsPage, backend funcional (403 sem auth = esperado). Dashboard completo com cards de estatísticas
- [x] 3.2 Paginação do histórico — JÁ IMPLEMENTADO: pageSizes [10,25,50,100,200], goEventsPage, eventsPageList com navegação
- [x] 3.3 Limpar histórico — JÁ IMPLEMENTADO: deleteSelectedEvents (com confirm) + deleteAllEvents
- [x] 3.4 URL da página de bloqueio configurável — JÁ IMPLEMENTADO: tab "blockpage" com preview, saveSettings com dnsDomain/blockPageTitle/etc

## 4. Menus Módulos e Integrações

- [x] 4.1 Menu Módulos — VERIFICADO: extensionsHub.html com ModulesTabController, tabela com seções, toggle com modal de motivo, CSS ext-hub, traduções OK
- [x] 4.2 Menu Integrações — VERIFICADO: integrations.html com IntegrationsTabController (plugins.controller.js), 3 seções (instalados, pontos de integração, como adicionar), CSS OK

## 5. Acesso Remoto — sessão sobrevive F5

- [x] 5.1 Corrigir controller $destroy — JÁ CORRIGIDO: $destroy não chama mais stopRemote(), apenas limpa listeners e fecha player. Sessão sobrevive via VIEWER_GRACE_MS + tryReattach()

## 6. Itens já feitos (verificação)

- [x] 6.1 F5 volta pro mesmo menu — DEPLOYED (localStorage fix em app.js)
- [x] 6.2 Informação Detalhada busca aceita outros termos — DEPLOYED (15+ campos, typeahead)
- [x] 6.3 Containers nomeados hwmdm-* — OK
- [x] 6.4 docker-compose.yaml único — OK
- [x] 6.5 Persistência após baixar/subir stack — OK (volume montado, md5 match)
- [x] 6.6 Tooltips em botões de devices.html — DEPLOYED (11 localized-title)

## 7. Bloqueados (requer APK/WAR rebuild — fora de escopo)

- [x] 7.1 BLOQUEADO: Tela bloqueada/desligada mata sessão remota — requer APK
- [x] 7.2 BLOQUEADO: Acesso remoto soberano/incondicional — requer APK
- [x] 7.3 BLOQUEADO: APK permite desativar suporte remoto — requer APK
- [x] 7.4 BLOQUEADO: Kiosk parou de funcionar — requer APK
- [x] 7.5 BLOQUEADO: Kiosk + Proteger Config automáticos — requer APK
- [x] 7.6 BLOQUEADO: Papel de parede paisagem — requer APK
- [x] 7.7 BLOQUEADO: Tela de bloqueio no device — requer APK
- [x] 7.8 BLOQUEADO: WebFilter "macro" no Chrome — requer DNS/VPN no APK
- [x] 7.9 BLOQUEADO: Modularidade (desativar módulo) — arquitetura WAR
- [x] 7.10 BLOQUEADO: Gerência containers via interface — arquitetura separada
- [x] 7.11 BLOQUEADO: Nada hardcoded (configs na interface web) — parcial, requer WAR
