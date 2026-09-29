# 🔴 RELATÓRIO DE AUDITORIA QA — HEADWIND MDM

**Data:** 29/09/2026 | **Versão:** HWMDM 1.0.0 build 1 · 76f2734
**Ambiente:** mdm.pmeto.local:8080 | **4 dispositivos:** SM-T225 (Samsung Galaxy Tab)
**Analista:** QA (modo cuzão, sem dó)

---

## 🚨 CRÍTICOS — Requer ação imediata

### 1. 3 dispositivos OFFLINE há 50 DIAS sem alerta/notificação
| Dispositivo | Último reporte | Dias offline |
|-------------|----------------|--------------|
| R9XT106VP1E | 10/08/2026 | ~50 |
| R9XT106Y5RP | 10/08/2026 | ~50 |
| R9XT108EM8T | 10/08/2026 | ~50 |

- O sistema mostra "No report for more than 7 days" mas **não dispara alerta, email, push ou ação corretiva**
- Existe um banner "3 device(s) have not reported for more than 7 days" mas é fácil de ignorar
- **Correção:** Implementar notificação proativa (email/push) e destacar visualmente em vermelho com badge

### 2. TODOS os dispositivos com app desatualizada — sem atualização automática
| Dispositivo | Instalado | Disponível |
|-------------|-----------|------------|
| R9XT106VP1E | 1.14 | 1.29 |
| R9XT106Y5RP | 1.14 | 1.29 |
| R9XT108EM8T | 1.14 | 1.29 |
| R9XT200AMYY | 1.28 | 1.29 |

- Botão "Update now" existe mas **depende de ação manual do admin**
- 100% dos devices com app desatualizada — falha sistêmica
- **Correção:** Política de atualização automática OTA para apps críticas (Suporte Remoto)

### 3. Launcher version 6.36 — configuração Kiosk Total é 6.37.3
- Todos os devices rodando launcher **6.36**, mas a config atribuída é "Kiosk Total (6.37.3)"
- A versão do launcher **não bate com a configuração** — ninguém percebeu
- **Correção:** Verificar pipeline de deploy do launcher — ou força atualização OTA ou corrige a configuração

---

## 🟡 MÉDIOS — Requer planejamento

### 4. Configuração "teste" em produção
- Filtro de configurações ainda mostra opção `teste` ao lado de configs reais
- Polui o seletor e pode causar confusão
- **Correção:** Remover configuração "teste"

### 5. "Group action" permanentemente desabilitado
- Botão nunca habilita, mesmo com checkboxes marcados
- Funcionalidade de ação em lote simplesmente não funciona
- **Correção:** Implementar ações em lote ou remover o botão

### 6. Kiosk mode = "no" para TODOS os dispositivos
- Coluna Kiosk mode mostra "no" em todos — se o dispositivo está em kiosk, deveria mostrar "yes"
- Indica que o report do kiosk mode pode estar quebrado
- **Correção:** Verificar se o campo kiosk mode está sendo populado corretamente

### 7. Files status e Description sempre vazios
- Colunas ocupam espaço na tabela mas **nenhum dispositivo tem dado**
- Description: vazio em 4/4 devices
- Files status: vazio em 4/4 devices
- **Correção:** Ocultar colunas por default ou preencher automaticamente

### 8. Bateria entre 44%-78% sem alerta configurável
- R9XT106VP1E com **44%** — não crítico hoje, mas não há threshold configurável
- **Melhoria:** Implementar alerta configurável (< 20%, < 10%)

### 9. Location map — mapa sem devices visíveis
- Nenhum dispositivo aparece no mapa GPS
- Pode ser que GPS não esteja sendo reportado ou módulo desabilitado
- **Correção:** Verificar se o módulo de GPS está funcionando e os devices reportando

### 10. Messages (Chat) — conteúdo não acessível via snapshot
- O Angular não expõe o conteúdo do chat no DOM acessível
- **Correção:** Garantir acessibilidade no módulo de chat

---

## 🔧 MELHORIAS — Sugestões

### 11. Botões de ação inconsistentes entre dispositivos
- Devices online mostram: Edit, QR code, Remote access, More ...
- **Sugestão:** Padronizar ações visíveis e mover ações secundárias para dentro do "More"

### 12. Tooltip "Fast search by number" existe mas nome confuso
- Tooltip explica bem, mas o nome é ambíguo
- **Sugestão:** Renomear para "Busca exata por número (final)"

### 13. Paginação 1-4/4 sem controles de navegação
- Mostra "1-4/4" e seletor "Per page" mas não tem botões de página anterior/próxima
- **Sugestão:** Implementar navegação completa de paginação

### 14. Versão do Android fixa em 14 para todos
- Todos SM-T225 com Android 14 — correto, mas sem destaque para versões diferentes
- **Sugestão:** Destacar visualmente versões desatualizadas (< 13)

### 15. Navegação com data em formato inconsistente
- Header da coluna mostra "10/08/2026 16:18" mas tooltip mostra "2 mon ago"
- **Sugestão:** Padronizar formato de data em todo o sistema

---

## 📊 RESUMO FINAL

| Gravidade | Qtd | Prioridade |
|-----------|-----|------------|
| 🔴 Crítico | 3 | Imediata |
| 🟡 Médio | 7 | Curto prazo |
| 🔧 Melhoria | 5 | Médio prazo |
| **Total** | **15** | |

**Veredito do QA:** O sistema funciona, mas está largado às traças. 75% dos dispositivos estão abandonados há 50 dias, apps desatualizadas em 100% dos devices, configuração "teste" em produção, launcher com versão errada, kiosk mode reportando "no" em kiosks. Isso não é MDM de produção — é um sistema que ninguém está monitorando. Precisa de correções urgentes antes de qualquer rollout.

---

## 📊 COMPARATIVO: Upstream (h-mdm) vs Nosso MDM (hwmdm/repo-mdm)

### Repositórios do Upstream (h-mdm)

| Repositório | Descrição | Versão |
|-------------|-----------|--------|
| `hmdm-server` | Server v5 (Java/JS, Tomcat 9) | v5.41.1 |
| `hmdm-server-v7` | Server v7 (Java/Angular, Tomcat 11, JDK21) | v7.02.4 |
| `hmdm-android` | Agente/Launcher Android | - |
| `hmdm-docker` | Imagem Docker oficial | - |
| `hmdm-plugin-wifimanager` | Gerenciamento WiFi | - |
| `hmdm-android-plugin-pager` | Push notifications | - |
| `launcherrestarter` | Monitor/restart do launcher | - |
| `hmdm-plugin-apn` | Configuração APN | - |
| `hmdm-openvpn` | VPN (fork) | - |
| `AospProvision` | Provisionamento AOSP | - |
| `AospProvisionStudio` | Provisionamento AOSP (Studio) | - |
| `AospHmdm` | Makefile para preinstalar em AOSP | - |
| `grapheneos-setup-wizard` | Provisionamento GrapheneOS | - |

### Nossos Plugins (repo-mdm/server-source/plugins)

| Plugin | Função |
|--------|--------|
| `audit` | Auditoria de ações dos usuários |
| `deviceinfo` | Informações detalhadas dos devices |
| `devicelog` | Coleta de logs dos devices |
| `messaging` | Mensagens para dispositivos |
| `moduleregistry` | Registro de módulos |
| `platform` | Funcionalidades de plataforma |
| `push` | Push messages autônomo |
| `webfilter` | Filtro de URL / bloqueio de sites |
| `xtra` | Funcionalidades extras |

### Features: Upstream vs Nosso MDM

| Feature | Upstream v5 CE | Upstream v7 CE | Nosso MDM |
|---------|:---:|:---:|:---:|
| **QR-code provisioning** | ✅ | ✅ | ✅ |
| **Silent app install/update** | ✅ | ✅ | ✅ |
| **Device groups/configs** | ✅ | ✅ | ✅ |
| **Device status monitoring** | ✅ | ✅ | ✅ |
| **GPS, Wi-Fi, Bluetooth policies** | ✅ | ✅ | ✅ |
| **App logs collection** | ✅ | ✅ | ✅ |
| **Plugin system** | ✅ | ✅ | ✅ |
| **REST API** | ✅ | ✅ | ✅ |
| **Kiosk mode (COSU)** | ❌ (Enterprise) | ❌ (Enterprise) | ✅ |
| **Web Filter (URL blocking)** | ❌ | ❌ | ✅ |
| **Remote access/screen mirroring** | ❌ | ❌ | ✅ |
| **Audit logging** | ❌ | ❌ | ✅ |
| **Push messaging** | ❌ | ❌ | ✅ |
| **Device info plugin** | ❌ | ❌ | ✅ |
| **Device log plugin** | ❌ | ❌ | ✅ |
| **Messaging (chat)** | ❌ | ❌ | ✅ |
| **Files management** | ❌ | ❌ | ✅ |
| **Launcher icons** | ❌ | ❌ | ✅ |
| **Backup & restore** | ❌ | ❌ | ✅ |
| **Appearance & branding** | ❌ | ❌ | ✅ |
| **Location map (GPS tracking)** | ❌ | ❌ | ✅ |
| **Reports** | ❌ | ❌ | ✅ |
| **Two-factor auth** | ❌ | ❌ | ✅ |
| **Multi-tenant** | ❌ | ❌ | ✅ |
| **LDAP auth** | ❌ | ❌ | ✅ |
| **Self-signup** | ❌ | ❌ | ✅ |
| **Angular frontend (v7)** | ❌ | ✅ | ❌ (AngularJS) |
| **Java 21 + Tomcat 11** | ❌ | ✅ | ❌ (Java 11 + Tomcat 9) |
| **WebSocket alerts** | ❌ | ✅ | ❌ |
| **Plugin as standalone WAR** | ❌ | ✅ | ❌ |
| **WiFi Manager plugin** | ❌ | ❌ | ❌ |
| **APN setup plugin** | ❌ | ❌ | ❌ |
| **OpenVPN integration** | ❌ | ❌ | ❌ |
| **Launcher restarter** | ❌ | ❌ | ❌ |

### 🟢 O que NÓS TEMOS que o upstream NÃO TEM

1. **Web Filter** — Bloqueio de URLs por dispositivo/grupo
2. **Remote Access** — Acesso remoto com espelhamento de tela
3. **Audit Logging** — Registro de todas as ações dos admins
4. **Push Messages** — Sistema de push autônomo (não depende de Firebase)
5. **Device Info** — Informações detalhadas do dispositivo
6. **Device Log** — Coleta de logs dos devices
7. **Messaging (Chat)** — Comunicação bidirecional com devices
8. **Files Management** — Upload/download de arquivos
9. **Launcher Icons** — Gerenciamento de ícones do launcher
10. **Backup & Restore** — Backup integrado via interface
11. **Appearance & Branding** — Customização visual do painel
12. **Location Map** — Mapa de localização GPS
13. **Reports** — Relatórios gerenciais
14. **Two-factor auth** — Autenticação de dois fatores
15. **Multi-tenant** — Operação multi-cliente
16. **LDAP auth** — Autenticação via LDAP
17. **Self-signup** — Auto-cadastro de usuários

### 🔴 O que o UPSTREAM v7 TEM que NÓS NÃO TEMOS

1. **Angular moderno** (nós temos AngularJS 1.x — obsoleto)
2. **Java 21 + Tomcat 11** (nós temos Java 11 + Tomcat 9)
3. **WebSocket alerts** — Notificações em tempo real no painel
4. **Plugin como WAR standalone** — Plugins independentes sem rebuild total
5. **WiFi Manager plugin** — Gerenciamento de redes WiFi
6. **APN setup plugin** — Configuração de Access Point Name
7. **OpenVPN integration** — VPN integrada
8. **Launcher restarter** — Monitor que reinicia o launcher se cair

### 💡 Resumo

**Nosso MDM tem MUITO mais features que o upstream Community** — basicamente pegamos o que era Enterprise no upstream e transformamos em Community, além de adicionar plugins próprios (webfilter, audit, push, deviceinfo, devicelog, messaging, xtra).

**Mas estamos rodando uma stack técnica legada** — AngularJS 1.x (fim da vida), Java 11 (fim da vida), Tomcat 9. O upstream v7 já migrou para Angular, Java 21 e Tomcat 11.

**Plugins que não temos e seriam úteis:** WiFi Manager, APN setup, OpenVPN, Launcher restarter.