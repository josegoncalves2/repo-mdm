# HANDOFF CONSOLIDADO — 2026-10-02

**Premissa:** NADA está entregue. Tudo listado abaixo é PENDENTE até prova real
(uso humano no browser/tablet, prints, vídeo). Qualquer marcação anterior de
"deployado" ou "entregue" é desconsiderada.

**Ambiente DEV:** 192.168.1.65:8080 — user web: admin / admin
**PROD (NUNCA TOCAR):** 192.168.1.75

---

## BLOCO 1 — ENROLLMENT E APK (handoff-1, handoff-2, handoff-3)

### 1.1 Correção do enrollment (null response)
- `GetServerConfigTask.java`: enrollPlain e enrollSecure devem tratar `response == null`
  quando ambos os servidores falham, sem crash
- STATUS: **PENDENTE** — código foi alterado mas nunca validado em dispositivo real

### 1.2 APK compatível com Play Protect
- APK 1.9 e 1.10 foram bloqueados pelo Play Protect
- Perfil `modelo-kiosk` (ID 57) ficou no APK 6.36 como bootstrap
- O APK 1.3 tem certificado diferente do 6.36 — não faz update over-the-air
- STATUS: **PENDENTE** — nenhum APK recompilado passou no Play Protect em device real
- PRÓXIMO PASSO: enrollment real em tablet limpo, confirmar que Play Protect aceita,
  validar sync, confirmar versão correta no servidor

### 1.3 Checksum SHA-256 Base64 no QR
- Bug: `CryptoUtil.getBase64String()` usava `base64Url()` (sem padding `=`)
- Android espera Base64 standard com padding
- Fix: `BaseEncoding.base64Url()` → `BaseEncoding.base64()` em `CryptoUtil.java`
- STATUS: **PENDENTE** — código alterado, WAR compilou, mas enrollment real via QR
  nunca foi testado em dispositivo físico
- O fix foi revertido por agente externo pelo menos uma vez — confirmar estado atual

### 1.4 QR do perfil 57
- Perfil 57 configurado com mainAppId=10128 (APK 1.2) como bootstrap
- Launcher 1.3 como instalação gerenciada (action=install)
- STATUS: **PENDENTE** — QR gerado e decodificado via endpoint mas nunca usado em device

---

## BLOCO 2 — WEBFILTER (openspec/add-webfilter-module)

### 2.1 Plugin WebFilter no servidor
- Plugin Maven `webfilter` em `server-source/plugins/webfilter/`
- Liquibase, MyBatis, Guice, rotas private/public
- Permissão `plugin_webfilter_access`
- Catálogo fixo de 14 categorias (sites + apps)
- Fontes: IPFire DBL, Block List Project, UT1
- Allowlist > blocklist > categoria > permitido
- Proteção de domínios/pacotes do próprio MDM
- STATUS: **PENDENTE** — plugin existe no código mas nunca validado em uso real

### 2.2 Resolvedor DNS (webfilter-dns)
- Container `webfilter-dns` com Blocky
- `resolver.py` como supervisor
- DNS-over-TLS na porta 853
- Aplica categorias/allowlist/blocklist por perfil
- STATUS: **PENDENTE** — container sobe e responde DNS, Blocky roda, mas bloqueio
  nunca validado em tablet real

### 2.3 WebFilter NO DISPOSITIVO (item 1 dos 12 itens)
- **O tablet acessa TUDO** (Facebook, Instagram, pornô) mesmo com webfilter "ativado"
- CAUSA RAIZ: não existe NADA no dispositivo que consuma o campo `webfilterDnsHost`
  do sync. O launcher `com.hmdm.launcher` não conhece esse campo. Não há Private DNS
  forçado nem VPN local
- STATUS: **NÃO IMPLEMENTADO** — imposição no dispositivo nunca foi construída
- NECESSÁRIO: alterar launcher para aplicar DNS privado via `DevicePolicyManager`
  (requer Device Owner), ou VPN local, ou companion app

### 2.4 Tela WebFilter no console
- Tela para gerenciar DNS, categorias, allowlist/blocklist de domínios e apps
- Atribuição de fontes/licenças (CC BY-SA 4.0)
- STATUS: **PENDENTE** — tela existe mas precisa validação visual/funcional real

### 2.5 Push configUpdated
- Ao salvar domínio/política/listas, enviar push `configUpdated` aos devices afetados
- STATUS: **PENDENTE**

---

## BLOCO 3 — ACESSO REMOTO (itens 2, 3, 4, 5 dos 12 itens + handoff-4/5)

### 3.1 Teclado não funciona (item 2)
- `InputInjectionService.type()` usa `getRootInActiveWindow()` mas
  `input_injection_config.xml` NÃO declara `canRetrieveWindowContent`
- Android devolve `null`, digitação nunca acontece
- STATUS: **NÃO FUNCIONA** — fix nunca implementado

### 3.2 Device offline perde sessão (item 3)
- `startRemote()` desiste quando `!device.online`
- Push de início não é enfileirado para quando o device voltar
- STATUS: **NÃO IMPLEMENTADO** — fila de comandos pendentes não existe

### 3.3 F5 / troca de tela derruba sessão (item 4)
- `remote.controller.js` não persiste estado
- `$scope.$on('$destroy')` chama `stopRemote()` que manda `stop` ao servidor
- STATUS: **NÃO CORRIGIDO** — qualquer F5 ou navegação encerra a sessão

### 3.4 Tela travada/apagada encerra acesso remoto (item 5)
- MediaProjection morre quando a tela desliga
- Não há wake lock nem dispensa de keyguard
- STATUS: **NÃO IMPLEMENTADO**

### 3.5 Tablet bipando/acendendo tela sozinho (handoff-4 bug A)
- Sem sessão remota ativa, tablet fica bipando e acendendo tela
- Hipótese: `remote-agent` segura wake lock permanente, ou a sessão web nunca é
  realmente encerrada (UI se perde ao navegar)
- STATUS: **NÃO INVESTIGADO/CORRIGIDO**

### 3.6 Esconder tela do APK Suporte Remoto (handoff-4 bug C)
- `StatusActivity` tem LAUNCHER no intent-filter → aparece na gaveta de apps
- Deveria ser invisível
- STATUS: **NÃO CORRIGIDO** — requer rebuild do APK

### 3.7 Refinar visual do Remote Access (handoff-4 bug D + handoff-5)
- Layout estilo Microsoft 365
- Barra lateral com sub-menus aninhados
- Console de helpdesk enterprise (conforme prompt TELA ACESSO REMOTO):
  - Fila de comandos pendentes para offline
  - Abas: Tela e Sessão, Ciclo de Vida, Aplicativos, Arquivos, Rede, Avançado
  - Chat integrado, transferência de arquivos, gravação de sessão
  - Indicadores de latência
  - RBAC nos botões
  - Ações em massa
- STATUS: **NÃO IMPLEMENTADO**

---

## BLOCO 4 — GPS (handoff-4 + handoff-5)

### 4.1 GPS lista de dispositivos à ESQUERDA
- Layout split: painel esquerdo com lista de devices, painel central com mapa
- Lista antes do mapa no DOM
- Layout responsivo: 2 colunas até 768px, depois coluna única
- STATUS: **PENDENTE** — handoff-4 diz que fez mas marcado como "ERRO, CRASHADO OU
  NAO EXISTE" pelo próprio handoff

### 4.2 GPS Histórico e Rastreabilidade
- Tabela `device_location_history` com trigger em `devices`
- API REST `api-location-history.jsp`
- Polyline no mapa, marcadores início/fim
- Exportação CSV e KML
- Conforme prompt TELA GPS, a visão completa inclui:
  - Painel de filtros e busca avançada
  - Timeline/slider de tempo
  - Painel de detalhes do evento
  - Reprodução de rota (play/pause/avançar/retroceder)
  - Detecção de paradas
  - Heatmap
  - Comparação de histórico entre devices
  - Exportação multi-formato (CSV, XLSX, PDF, KML/KMZ/GPX, JSON/XML)
  - Geocercas (geofencing) com alertas
  - Alertas baseados em localização
  - Relatórios de produtividade
  - API RESTful documentada
  - Marker clustering, círculo de precisão, ícones de status
  - RBAC, modo privacidade, log de auditoria, retenção de dados
- STATUS: **NÃO IMPLEMENTADO** — tabela pode existir no banco mas está vazia;
  frontend não validado

---

## BLOCO 5 — PERMISSÕES E CONSOLE (openspec/corrigir-permissoes-e-console)

### 5.1 Tabela de dispositivos responsiva (PRD-06)
- `.table-responsive` trava largura da tabela na tela Dispositivos
- STATUS: **NÃO CORRIGIDO**

### 5.2 Auditoria de permissões (PRD-14)
- Matriz das 41 permissões: nome × descrição × onde verificada × efeito real
- Classificação: CORRETA | NOME_ERRADO | NAO_FAZ_NADA | FAZ_OUTRA_COISA | SO_NA_TELA
- STATUS: **NÃO FEITO**

### 5.3 Corrigir nomes/descrições pt-BR (PRD-14, PRD-15)
- Permissões com nome errado ou que fazem outra coisa
- STATUS: **NÃO FEITO** — depende de 5.2

### 5.4 Fazer permissões funcionarem de verdade (PRD-15, PRD-03)
- Permissões que não fazem nada ou só checam na tela (não no servidor)
- Verificação no REST + na tela
- STATUS: **NÃO FEITO** — depende de 5.2

### 5.5 Segregação ver/editar (PRD-07, PRD-08)
- Matriz de áreas × níveis (sem acesso | ver | editar)
- Começando pelo perfil de dispositivo
- Novas permissões via Liquibase, sem apagar existentes
- STATUS: **NÃO FEITO** — depende de 5.2

### 5.6 Combobox de permissões real (PRD-10)
- O combobox da tela de papéis é placeholder — não grava nada
- Substituir por seletor real que grava e é respeitado
- STATUS: **NÃO FEITO**

### 5.7 Usuário sem permissão não pode editar perfil (PRD-10)
- Nem pela tela nem por chamada REST direta
- STATUS: **NÃO FEITO**

---

## BLOCO 6 — TABLET (openspec/corrigir-permissoes-e-console, seção 4)

### 6.1 Papel de parede responsivo em paisagem (PRD-09)
- Hoje só está correto em retrato
- STATUS: **NÃO FEITO**

### 6.2 Suporte remoto não desativável/desinstalável (PRD-12)
- Usuário do tablet não pode forçar parada, desativar ou desinstalar o suporte remoto
- STATUS: **NÃO FEITO** — requer rebuild APK

### 6.3 Kiosk e Proteger Configurações automáticos (PRD-13)
- Após concessão de permissões, kiosk e "Proteger Configurações" armam sozinhos
- STATUS: **NÃO FEITO**

### 6.4 Modo Kiosk parou de funcionar (handoff-4 bug B, handoff-5)
- Ontem funcionava, hoje não — tablet fica normal mesmo com kioskMode=true
- Pode ser: perfil mudou, Device Owner perdido, launcher não é mais default
- STATUS: **QUEBRADO** — investigar e corrigir

---

## BLOCO 7 — UI/UX GERAL (12 itens + handoff-4 + handoff-5)

### 7.1 Botões não padronizados (item 6)
- Cada módulo mostra botões de um jeito, alguns carregam depois
- STATUS: **NÃO CORRIGIDO**

### 7.2 Modularidade (item 7)
- Não dá para desativar 1 módulo para manutenção sem parar tudo
- Tudo amarrado
- STATUS: **NÃO IMPLEMENTADO**

### 7.3 Lógica dos menus (item 8)
- Menus são uma salada de opções
- STATUS: **NÃO CORRIGIDO**

### 7.4 Menu Plugins / Extensions (item 9)
- "More plugins" sem propósito, opções repetidas, inúteis
- STATUS: **NÃO CORRIGIDO**

### 7.5 Controle de versão/build/compilação (item 10)
- Não existe mecanismo de versão no projeto
- STATUS: **NÃO IMPLEMENTADO**

### 7.6 Traduções Dashboard (handoff-4)
- Strings hardcoded em inglês no `summary.html`
- STATUS: **PENDENTE** — pode ter sido feito parcialmente mas não validado

### 7.7 Focus-visible global (handoff-4)
- `:focus-visible` no CSS para navegação por teclado
- STATUS: **PENDENTE** — não validado

### 7.8 Menu lateral instável (handoff-5)
- Menu lateral se move ao clicar, não fica fixo
- Ao atualizar a página, volta para outra seção em vez da atual
- STATUS: **NÃO CORRIGIDO**

### 7.9 Menu Server fora do padrão (handoff-5)
- `http://192.168.1.65:8080/#/server` não segue layout do resto
- STATUS: **NÃO CORRIGIDO**

### 7.10 Layout estilo Microsoft 365 (handoff-5)
- Todas as telas devem seguir padrão M365: barra lateral com sub-menus aninhados
- Aplica-se a: GPS, Remote Access, Permissões, e demais telas
- STATUS: **NÃO IMPLEMENTADO**

### 7.11 Todas as telas aprovadas por QA/UX/UI (handoff-5)
- Validação visual e funcional de cada tela
- STATUS: **NÃO FEITO**

---

## BLOCO 8 — INFRAESTRUTURA E DEPLOY (handoff-5)

### 8.1 Docker entrypoint destrói CSS/customizações
- O entrypoint rebuild e sobrescreve overlay do WAR
- Entrypoint foi desabilitado pelo usuário (`.bak-20261002-095114`)
- STATUS: **NÃO RESOLVIDO** — precisa de solução permanente que preserve
  customizações

### 8.2 Permissões de pastas inadequadas
- Deploy feito com permissões erradas
- STATUS: **NÃO CORRIGIDO**

### 8.3 Container hwmdm-admin não sobe automaticamente
- Não está no docker-compose ou tem problema de configuração
- STATUS: **NÃO INVESTIGADO**

### 8.4 Persistência das alterações
- Todas as alterações de CSS, HTML, JS devem sobreviver a restart do container
- STATUS: **NÃO GARANTIDO** — overlay/entrypoint pode destruir tudo

### 8.5 Erros PostgreSQL — schema mismatch
- Banco restaurado de PROD tem schema diferente do código compilado
- Colunas/tabelas inexistentes causam erros 500 em várias funcionalidades
- STATUS: **NÃO CORRIGIDO** — erros classificados como "não bloqueantes" mas
  afetam funcionalidades

---

## BLOCO 9 — 12 ITENS (openspec/corrigir-12-itens) — RESUMO CRUZADO

| # | Item | Bloco neste handoff | Status |
|---|------|---------------------|--------|
| 1 | WebFilter não funciona no tablet | 2.3 | NÃO IMPLEMENTADO |
| 2 | Teclado remoto não reconhece digitação | 3.1 | NÃO FUNCIONA |
| 3 | Acesso remoto se perde com device offline | 3.2 | NÃO IMPLEMENTADO |
| 4 | F5/troca de tela derruba acesso remoto | 3.3 | NÃO CORRIGIDO |
| 5 | Tela travada/apagada encerra acesso remoto | 3.4 | NÃO IMPLEMENTADO |
| 6 | Botões não padronizados | 7.1 | NÃO CORRIGIDO |
| 7 | Modularidade inexistente | 7.2 | NÃO IMPLEMENTADO |
| 8 | Menus sem lógica | 7.3 | NÃO CORRIGIDO |
| 9 | Plugins/Extensions inútil | 7.4 | NÃO CORRIGIDO |
| 10 | Sem controle de versão/build | 7.5 | NÃO IMPLEMENTADO |
| 11 | Trava de entrega | Ação do humano | NÃO ATIVADA |
| 12 | Token de registro | Ação do humano | NÃO IMPLEMENTADO |

---

## CONTAGEM TOTAL

| Categoria | Qtd |
|-----------|-----|
| NÃO IMPLEMENTADO | 18 |
| NÃO CORRIGIDO / NÃO FUNCIONA | 14 |
| PENDENTE (feito mas não validado em uso real) | 8 |
| **TOTAL DE ITENS** | **40** |
| ENTREGUE COM PROVA REAL | **0** |

---

## PRÓXIMO PASSO

Na próxima sessão: diff deste handoff contra o estado real da stack (código no
container, banco, APKs publicados, telas no browser) para identificar o que de
fato já existe vs o que precisa ser construído do zero.
