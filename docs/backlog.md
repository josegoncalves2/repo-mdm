# Backlog — HWMDM (atualizado 2026-10-09 09:10)

## Status geral: atualizado 2026-10-09 09:10

### Mapeamento sessao 2026-10-09 — TODOS os itens do prompt de deployment

| # | Item | Status | Prioridade | Observacao |
|---|------|--------|-----------|------------|
| 1 | Kiosk mode 100% funcional | PENDENTE | CRITICO | NAO FUNCIONA MAIS; 6.36 faz enroll, 1.3 gerencia; requer Device Owner no tablet |
| 2 | Enrollment: 6.36 enroll → MDM lanca 1.3 + suporte remoto + webfilter | PENDENTE | CRITICO | Fluxo combinado nunca testado end-to-end |
| 3 | Acesso remoto — tela preta, parou de funcionar | DIAGNOSTICADO | CRITICO | Stream FUNCIONA (752x1280, JMuxer/MSE). Tela preta = lockscreen Android (kioskMode=false). Resolvido quando kiosk ativar |
| 4 | Analise e correcao /docs/erros.md | FEITO | ALTO | Reescrito de 1.4MB para ~100 linhas classificadas |
| 5 | Zero erros/warnings/not found nos logs Docker | FEITO | ALTO | 0 erros em 4 containers (6h de logs) |
| 6 | Inspecao completa interface web — todos bugs e erros | FEITO | ALTO | 9 rotas percorridas no Chrome, 0 erros console |
| 7 | Conectar no Chrome e resolver problemas encontrados | FEITO | ALTO | console.debug fix em devices.controller.js |
| 8 | Mensagens CSS quebrado | FEITO | MEDIO | Sessao 07:50 — compose bar, card-style msgs |
| 9 | GPS timeline bar abaixo do mapa | FEITO | MEDIO | Sessao 07:50 — reordenacao DOM |
| 10 | Remover duplicidade de codigo/diretorios/arquivos/pastas inuteis | PARCIAL | MEDIO | Removidos: 14 APKs antigos dist/, .old.claude/, WAR backup (93MB). Deploy dirs root-owned pendentes (699MB, precisa sudo) |
| 11 | Manter apenas arquivos essenciais — mover demais | PARCIAL | MEDIO | dist/ limpo (7 arquivos essenciais). Deploy backups root-owned pendentes |
| 12 | Proibido hardcoded — usar variaveis | FEITO | MEDIO | 20+ cores hardcoded convertidas para var(--hwmdm-*) com fallback. 4 novas CSS vars adicionadas. 0 hardcoded restantes em views |
| 13 | Layout UX/UI com boas praticas, design system, design patterns | PENDENTE | MEDIO | Validacao em todas as telas |
| 14 | Persistencia e compliance de permissoes | PENDENTE | MEDIO | chown, bind mounts, sobrevive reboot |
| 15 | Nao aplicar correcao em arquivo desatualizado | REGRA | — | Verificar estado antes de cada edicao |

### Fixes aplicados 2026-10-08 sessao 13:50-14:10
- [x] ROOT.war removido — causa raiz de sidebar sumida, i18n raw, QR sem layout, servidor desaparecido
- [x] Auto-selecao Acesso Remoto — removido bloco sessionStorage que auto-selecionava device (remote.controller.js)
- [x] Split vertical persistente em Perfis de dispositivo — col 50/50 fixa, painel QR/contexto sempre visivel
- [x] i18n configurations.split.empty (EN+PT)
- [x] Documentacao auditoria.md atualizada

### Fixes aplicados 2026-10-08 sessao 10:30-11:00
- [x] Auto-selecao infundada no menu Acesso Remoto — removido fallback firstOnlineDevice/devices[0], agora so seleciona se houver sessao ativa para reatar (PROVADO com screenshot)
- [x] QR — FALSO ALARME: mainappid=10045 e valido (applicationversions.id do launcher 6.36). Todos 6 perfis geram QR (HTTP 200). O campo mainappid referencia applicationversions, nao applications

### Fixes aplicados 2026-10-08
- [x] Senha admin resetada (hash estava alterado por agente desconhecido)
- [x] 5 permissoes com descricao em russo/ingles corrigidas para pt-BR
- [x] API REST verificada: todos endpoints publicos e privados funcionais
- [x] QR Code: 6/6 perfis geram QR funcional
- [x] WebFilter Dashboard: 132 eventos registrados, paginacao funcional
- [x] ModuleRegistry: 19 modulos habilitados
- [x] Perfil 11 (Kiosk Total 6.36) pronto para enrollment

### OpenSpec changes ativas (documentacao completa em openspec/changes/)

| Change | Tema |
|--------|------|
| `add-webfilter-module` | WebFilter completo |
| `api-token-powerbi` | Power BI + API token |
| `correcoes-interface-pendentes` | ~30 bugs de interface |
| `corrigir-12-itens` | 12 itens do responsável |
| `corrigir-permissoes-e-console` | Permissões e console |
| `gps-rastreabilidade-avancada` | GPS avançado |
| `redesign-consistencia-painel` | Padronização visual de todas as telas |

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
- [x] F5/troca de aba: $destroy JA NAO chama stopRemote — CORRIGIDO (closePlayer apenas)
- [ ] Tela bloqueada/apagada mata MediaProjection — requer wake lock no APK
- [ ] Tablet bipando sem sessao ativa — investigar RemoteAgentService
- [ ] Device offline: implementar fila de comandos pendentes
- [ ] Prova: video do teclado funcionando + sessao que sobrevive F5

### Prioridade 4 — Permissoes RBAC
- [ ] Auditar 45 permissoes: nome x efeito real x onde verificada
- [x] Corrigir 5 descricoes em RUSSO → pt-BR — CORRIGIDO via SQL (edit_device_app_settings, plugin_audit_access, plugin_deviceinfo_access, plugins_customer_access_management, plugin_devicelog_access)
- [x] Verificar se combobox de permissoes GRAVA de verdade — CONFIRMADO via API (add/remove persiste no DB)
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
- [x] Kiosk modo: launcher 1.3 tem KioskPolicy.java completo (startLockTask/setLockTaskPackages/setLockTaskFeatures). mainappid dos perfis 11 e 60 atualizado para 10129 (1.3). Requer Device Owner provisioning no tablet para funcionar
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
- [x] Quiosque > Politica de bloqueio — restricoes localizadas pt-BR (antes mostravam IDs crus como no_factory_reset)
- [ ] Quiosque > Apps permitidos — botoes estourados css
- [ ] Layout estilo M365 (barra lateral com sub-menus aninhados) — NAO implementado
- [ ] Controle de versao/build — NAO implementado
- [ ] Modularidade (desativar modulo sem parar tudo) — NAO implementado
- [ ] Validacao visual de TODAS as telas por UX/UI

### Prioridade 9 — Redesign de consistência do painel (change: redesign-consistencia-painel)
- [x] CSS remote-preview-screen: removido display:flex (canvas estourava) — DEPLOYED 2026-10-06
- [x] CSS remote-device-item: removido grid antigo conflitante — DEPLOYED 2026-10-06
- [x] CSS remote-device-item: reescrito com grid padrão do Mapa — DEPLOYED 2026-10-06
- [x] HTML remote.html: lista padronizada (dot + serial + modelo + hora) — DEPLOYED 2026-10-06
- [x] HTML remote.html: busca movida para inline no painel — DEPLOYED 2026-10-06
- [x] JS remote.controller.js: contadores online/offline — CORRIGIDO 2026-10-08 (ownership OK, contadores adicionados)
- [ ] Mensagens: trocar dropdown por lista lateral padrão
- [ ] Relatórios: lista lateral + relatório por device + visão unificada
- [ ] Perfis de dispositivo: edição inline (sem página separada)
- [ ] Aplicativos, Arquivos, Ícones: lista lateral + detalhe inline
- [ ] Usuários, Permissões: lista lateral + edição inline
- [ ] Acesso Remoto: botões de ação na coluna lateral (não embaixo esticados)
- [ ] Aparência e marca: personalização completa (nome, logo, favicon)

### Prioridade 10 — Integração Power BI e API Token (change: api-token-powerbi)
- [ ] Endpoint/tela para gerar token de longa duração (read-only)
- [ ] UserRole de leitura (API reader)
- [ ] Documentação Swagger dos endpoints

### Prioridade 8 — Infra
- [x] docker-compose.yaml unico — OK
- [x] Containers nomeados hwmdm-* — OK
- [x] Entrypoint nao destrutivo — OK (reescrito)
- [x] Persistencia no volume — OK (md5 match)
- [ ] Nada hardcoded — PARCIAL (muitas configs nao estao na interface web)
- [ ] Gerencia de containers via interface web — NAO implementado

### Prioridade URGENTE — Performance / Lentidao (diagnosticado 2026-10-10, corrigido 2026-10-08)
- [x] Caching DNS — adicionado `caching: { maxTime: 30m, maxItemsCount: 2048, prefetching: true }` em ResolverConfigWriter.java:269-271 (SOURCE corrigido)
- [x] logRetentionDays: 0 → 7 — ja estava corrigido no source (linha 268), so faltava o rebuild do WAR
- [x] refreshPeriod: 0m → 4h — ja estava corrigido no source (linha 273), so faltava o rebuild do WAR
- [x] Fix temporario aplicado: blocky.yml editado diretamente no host (volumes/work/plugins/webfilter/dns/blocky.yml), container reiniciado — DEPLOYED 08/10 13:02
- [ ] **REBUILD WAR** necessario para fix PERMANENTE — WAR em execucao e anterior ao source e regenera blocky.yml com valores antigos quando painel altera listas
- [ ] Listas de bloqueio: 14 categorias = 2.3M dominios (phishing 1M, adult 967k) = ~430MB heap
  - FIX: consolidar/reduzir categorias OU usar listas menores (decisao de negocio)
- [x] Push messages acumulados — DELETE manual aplicado. FIX DEFINITIVO: limpar pushes antigos automaticamente
- [ ] Swap em uso (877MB) — sintoma da RAM esgotada pelas listas do webfilter
- **Arquivo chave**: `server-source/plugins/webfilter/src/main/java/com/hmdm/plugins/webfilter/resolver/ResolverConfigWriter.java`
- **ATENCAO**: blocky.yml e regenerado a cada alteracao de listas pelo painel — editar direto no container e TEMPORARIO, sobrescrito pelo WAR
