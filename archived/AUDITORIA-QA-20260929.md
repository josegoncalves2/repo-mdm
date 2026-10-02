# 🔴 RELATÓRIO DE AUDITORIA QA — HEADWIND MDM

## 🚨 CRÍTICOS — IMPEDEM O USO OU CAUSAM PERDA DE DADOS

### 1. SERVIDOR INSTÁVEL — ERRO 404/EMPTY RESPONSE NO FAVICON

**Onde:** Toda página carrega `http://192.168.1.75:8080/files/favicon.png` — **QUE NEM EXISTE**.  
**Evidência:** `net::ERR_EMPTY_RESPONSE` no console.  
**Problema:** O path do favicon está hardcoded apontando para **outro IP (192.168.1.75)** que não é o servidor atual. Isso é sintoma de configuração mal copiada de ambiente.  
**Correção:** Usar favicon relativo ao domínio atual ou remover o hardcode.

### 2. 3 DISPOSITIVOS OFFLINE HÁ 2 MESES — SEM ALERTA

**Onde:** Tabela de Devices/Dashboard.  
**Evidência:** Dispositivos `R9XT106VP1E`, `R9XT106Y5RP`, `R9XT108EM8T` — último reporte **10/08/2026** (há 50 dias).  
**Problema:** Não há indicador visual de alerta, badge, ou notificação. O sistema trata dispositivo abandonado como se estivesse operacional. O admin só descobre se ler data por data.  
**Correção:** Implementar alerta de "dispositivo não reporta há N dias", destacar visualmente (vermelho), e opção de disparar notificação.

### 3. SUPORTE REMOTO COM VERSÃO ATRASADA — SEM ALERTA

**Onde:** Coluna "Installation Status" dos dispositivos.  
**Evidência:** 3 dispositivos mostram "Suporte Remoto: installed 1.14, available 1.27".  
**Problema:** O sistema SABE que há atualização disponível (1.27) mas não força nem sugere a atualização. Fica só informativo. Isso é omissão.  
**Correção:** Botão "Atualizar agora" ou trigger automático de atualização OTA quando disponível.

### 4. "Group action" PERMANENTEMENTE DESABILITADO

**Onde:** Barra de ferramentas da lista de devices.  
**Evidência:** Botão `Group action` sempre com `[disabled]`.  
**Problema:** Funcionalidade de ação em lote simplesmente não funciona. Se não há devices selecionados, deveria habilitar ao marcar checkboxes. Se a feature não existe, deveria ser removida.  
**Correção:** Implementar ações em lote (enviar comando, atualizar config, etc.) ou remover o botão.

---

## 🟡 MÉDIOS — IMPACTAM USABILIDADE E OPERAÇÃO

### 5. FORMATAÇÃO DE DATA INCONSISTENTE NA TABELA

**Onde:** Coluna "Date" vs "Enrolled".  
**Evidência:** Date mostra `10/08/26 16:18`, Enrolled mostra `07/08/26 14:29`. O header "Date" tem tooltip `2026/08/10 16:18:15` (ISO). São **3 formatos diferentes no mesmo sistema**.  
**Correção:** Padronizar formato de data em todo o sistema (ISO 8601 ou dd/mm/aaaa HH:mm).

### 6. COLUNA "Description" VAZIA EM TODOS OS DISPOSITIVOS

**Onde:** Tabela de devices.  
**Evidência:** Todos os 4 devices têm célula `Description` vazia.  
**Problema:** Campo disponível mas nunca preenchido. Se não é usado, polui a interface.  
**Correção:** Ocultar coluna por padrão ou preencher automaticamente com dados relevantes.

### 7. COLUNA "Kiosk mode" VAZIA EM TODOS OS DISPOSITIVOS

**Onde:** Tabela de devices.  
**Evidência:** Célula vazia para todos os 4 devices.  
**Problema:** Campo existe mas não é populado. Se o kiosk mode não é detectado, deveria mostrar "N/A" ou "unknown".  
**Correção:** Preencher com valor default ou ocultar.

### 9. MENSAGENS (CHAT) — CONTEÚDO NÃO CAPTURADO PELA ACESSIBILIDADE

**Onde:** `/chat`.  
**Evidência:** O snapshot de acessibilidade não mostra o conteúdo do ng-view. O innerText do body não retorna o conteúdo da página.  
**Problema:** Provável problema de renderização Angular ou lazy loading. Pode ser que a página não carregue corretamente.  
**Correção:** Verificar se o módulo de chat está funcional e renderizando corretamente.

### 10. REPORTS — MESMO PROBLEMA DE RENDERIZAÇÃO

**Onde:** `/reports`.  
**Evidência:** Idem ao chat — conteúdo não capturável.  
**Problema:** Pode ser que o módulo de reports não tenha dados ou esteja quebrado.  
**Correção:** Garantir renderização correta ou mensagem "nenhum relatório disponível".

---

## 🟡 MENORES — POLIMENTO E EXPERIÊNCIA

### 11. BOTÃO "More ..." COM REDUNDÂNCIA

**Onde:** Ações de cada device.  
**Evidência:** Botão `More ...` com reticências. Já existem botões individuais para Edit, QR, Force Kiosk, Remote, GPS, Delete.  
**Problema:** UI poluída. Muitos botões na mesma linha. O "More" deveria agrupar ações secundárias.  
**Correção:** Mover ações menos usadas (Force Kiosk, GPS, Delete) para dentro do "More".

### 12. TEMA LIGHT/DARK — SWITCH EXISTE MAS NÃO SE SABE SE FUNCIONA

**Onde:** Topo da página, botão "Switch light/dark theme".  
**Problema:** Não testei a fundo, mas é um ponto de possível falha se o tema não persistir ou quebrar layout.  
**Correção:** Testar e garantir persistência em localStorage.

### 13. VERSÃO DO LAUNCHER DESATUALIZADA (6.36)

**Onde:** Coluna "Launcher version".  
**Evidência:** Todos os devices mostram `6.36`.  
**Problema:** Se 6.37.3 é a config "Kiosk Total", por que os devices ainda estão em 6.36? Atualização não está sendo propagada.  
**Correção:** Verificar pipeline de deploy do launcher.

### 14. BATERIA ENTRE 44% E 78% — SEM ALERTA DE BATERIA FRACA

**Onde:** Coluna "Battery level".  
**Evidência:** Dispositivo `R9XT106VP1E` com 44%.  
**Problema:** 44% não é crítico, mas não há threshold configurável para alerta de bateria baixa.  
**Melhoria:** Implementar alerta configurável (< 20%, < 10%).

---

## 🔧 SUGESTÕES DE MELHORIAS

### 15. ADICIONAR COLUNA "ÚLTIMO REPORTE" COM DESTAQUE TEMPORAL

**Sugestão:** Em vez de mostrar "2 mon ago" no tooltip, ter uma coluna ou badge colorido:

- Verde: < 1 dia
- Amarelo: 1-7 dias
- Vermelho: > 7 dias

### 16. FILTRO "More parameters" EXPANSÍVEL — SEM FEEDBACK

**Onde:** Botão "More parameters" na listagem.  
**Problema:** Não há indicador se está expandido ou colapsado.  
**Correção:** Adicionar ícone de seta (▸/▾) para indicar estado.

### 17. CHECKBOX "Fast search by number" — DESCRIÇÃO CONFUSA

**Onde:** Ao lado do campo de busca.  
**Problema:** O que é "fast search by number"? Número do dispositivo? IMEI? Serial? Precisa de tooltip explicativo.  
**Correção:** Adicionar tooltip: "Busca pelo número do dispositivo (Device Number)".

### 18. PAGINAÇÃO "1-4/4" — INFORMAÇÃO INCOMPLETA

**Onde:** Acima da tabela.  
**Problema:** Mostra "1-4/4" mas não há controles de página (anterior/próximo). Se tiver mais de 4 devices no futuro, não tem como navegar.  
**Correção:** Implementar paginação completa ou aumentar o limite padrão.


### 20. NENHUM LOG DE AUDITORIA VISÍVEL

**Onde:** Módulo Audit.  
**Problema:** Não consegui capturar conteúdo. Se está vazio, o sistema não está logando ações dos admins.  
**Correção:** Garantir que toda ação administrativa seja registrada no audit log.

---

## 📊 RESUMO

|Gravidade|Quantidade|
|---|---|
|🔴 Crítico|4|
|🟡 Médio|6|
|🔧 Melhoria|10|
|**Total**|**20**|

**Veredito do QA:** O sistema funciona, mas está **largado às traças**. Dispositivos offline há 2 meses sem alerta, favicon quebrado apontando IP errado, configuração "teste" em produção, versão de launcher desatualizada, group action morto, data em 3 formatos diferentes. Isso não é MDM de produção — é um protótipo que foi parar no ar. Precisa de **correções urgentes** antes de qualquer rollout.

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