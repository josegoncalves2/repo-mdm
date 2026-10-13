
A seguir está o compilado técnico de requisitos para **Navegação Segura em Dispositivos Gerenciados por MDM**, extraído das soluções líderes de mercado (Palo Alto Networks, Zscaler, Cisco, Fortinet e Check Point). Os itens foram normalizados para eliminar duplicidades funcionais e classificados por nível de criticidade para uma implementação partindo do zero.

---

## NÍVEL 1 — ESSENCIAL

**Define a fundação arquitetural. Sem qualquer um destes itens, a solução de navegação segura em dispositivos MDM simplesmente não opera.**

| #   | Requisito Técnico                              | Descrição Técnica                                                                                                                                                                                                                                                                       |
| --- | ---------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | **Integração MDM via API/AppConfig**           | O agente de segurança deve ser implantável através do MDM corporativo (Intune, Jamf, Workspace ONE) utilizando o padrão AppConfig ou Managed Configurations. O agente precisa ler o status de enrollment do dispositivo diretamente do MDM para decidir se permite ou nega a navegação. |
| 2   | **Enrollment obrigatório do dispositivo**      | A política de navegação só é aplicada se o dispositivo estiver enrolled e compliant no MDM. Dispositivos não gerenciados ou não compliant são bloqueados de acessar qualquer recurso web corporativo. O agente consulta o MDM em tempo real para validar o estado.                      |
| 3   | **Instalação silenciosa do agente via MDM**    | O cliente de segurança (Zscaler Client Connector, FortiClient, Harmony SASE Agent, etc.) deve ser distribuído pelo MDM sem interação do usuário. Requer suporte a instalação silenciosa em Windows, macOS, iOS, Android e Linux.                                                        |
| 4   | **Configuração de perfil de VPN/Proxy no MDM** | O MDM deve empurrar um perfil de VPN ou proxy (Global HTTP Proxy) para o dispositivo, roteando todo o tráfego web pelo gateway de segurança. Em iOS/iPadOS, isso exige dispositivo **supervisionado**.                                                                                  |
| 5   | **Suporte a múltiplos sistemas operacionais**  | O agente deve cobrir Windows, macOS, iOS, iPadOS, Android, Linux (CentOS, Ubuntu 20.04+). Sem essa cobertura, a política de navegação segura fica fragmentada.                                                                                                                          |
| 6   | **Certificado raiz da CA instalado via MDM**   | O certificado da Autoridade Certificadora (CA) do gateway de segurança deve ser instalado como Trusted Root CA no dispositivo via perfil de MDM. Sem isso, a inspeção SSL/TLS falha e o usuário recebe erros de certificado em vez da página de bloqueio.                               |
| 7   | **Mecanismo anti-tampering no agente**         | O agente deve impedir que o usuário final desinstale, desative ou encerre o processo de segurança. Requer código de administrador para desinstalação e proteção contra kill de processo.                                                                                                |
| 8   | **Autenticação do usuário no agente**          | Após a instalação, o usuário deve se autenticar no agente (SAML, MFA, IdP corporativo) para que a política de navegação seja vinculada à identidade. O agente suporta login único e multifator.                                                                                         |
| 9   | **Bloqueio de navegadores não autorizados**    | Via MDM, o administrador deve conseguir bloquear a execução de navegadores não gerenciados (Chrome, Edge, Firefox, Safari) por nome de processo, caminho de arquivo ou assinatura de certificado, forçando o uso exclusivo do navegador seguro ou do agente de filtragem.               |
| 10  | **Política de Always-On / Auto-Start**         | O agente deve iniciar automaticamente com o dispositivo e permanecer ativo sem intervenção do usuário. Se o agente parar, o acesso à internet deve ser bloqueado (fail-closed).                                                                                                         |


## NÍVEL 2 — BÁSICO

**Adiciona camadas de controle e visibilidade sobre o tráfego. Sem estes, a solução funciona, mas sem granularidade ou governança mínima.**

| # | Requisito Técnico | Descrição Técnica |
|---|---|---|
| 11 | **Filtragem de URL por lista de permissão/negação** | O agente/gateway deve permitir ou negar acesso a URLs específicas, domínios inteiros ou wildcards. A política é configurada no console central e empurrada para o dispositivo. Formato de correspondência: `[scheme://][.]host[:port][/path][@query]`. |
| 12 | **Filtragem por categoria de conteúdo** | Bloqueio automático baseado em categorias pré-definidas (malware, phishing, adulto, streaming, redes sociais, jogos, etc.). A categorização é feita na nuvem e aplicada no dispositivo, independentemente da rede a que ele se conecta. |
| 13 | **Inspeção SSL/TLS (HTTPS Decryption)** | O gateway deve descriptografar tráfego HTTPS, inspecionar o conteúdo em busca de ameaças e violações de política, e re-criptografar antes de entregar ao dispositivo. Requer CA confiável instalada via MDM. Suporta HTTP/2 e TLS 1.3. |
| 14 | **DNS Security / DNS Layer Enforcement** | Filtragem de resolução DNS no nível do dispositivo ou do gateway, bloqueando domínios maliciosos antes mesmo de a conexão TCP ser estabelecida. Reduz latência e cobre aplicações que não usam HTTP.  |
| 15 | **Bloqueio de downloads de arquivos maliciosos** | Inspeção de arquivos baixados via web, com bloqueio baseado em reputação, assinatura de malware e análise sandbox. Aplica-se a uploads e downloads. |
| 16 | **Isolamento de sessão de navegação (Remote Browser Isolation)** | Sessões de risco elevado (sites desconhecidos, categorias de risco) são executadas em um contêiner remoto isolado. Apenas pixels são renderizados para o dispositivo, neutralizando exploits de browser. Suporta desktop e mobile (Android, iOS, iPadOS). |
| 17 | **Controle de clipboard entre navegador isolado e dispositivo** | No modo de isolamento, o administrador deve poder bloquear ou permitir cópia/colagem de dados entre o navegador remoto e o sistema local. Reduz exfiltração de dados via área de transferência. |
| 18 | **Bloqueio de conteúdo externo não aprovado** | Impedir que páginas web carreguem recursos de domínios fora da allowlist corporativa (scripts, iframes, CDNs não aprovados). Reduz superfície de ataque de supply chain web. |
| 19 | **Forçar SafeSearch em buscadores** | Aplicar configuração de pesquisa segura obrigatória no Google, Bing e outros buscadores, independentemente da configuração do usuário. Implementado via política de MDM ou agente. |
| 20 | **Log de eventos de navegação permitidos e bloqueados** | O agente deve gerar e enviar logs de cada decisão de política (allow/block) para o console central. Inclui URL, categoria, identidade do usuário, timestamp, veredito e ID do dispositivo.  |


## NÍVEL 3 — AVANÇADO

**Introduz postura de dispositivo, identidade contextual e controle granular de aplicações. Essencial para ambientes com BYOD, acesso a SaaS e conformidade regulatória.**

| # | Requisito Técnico | Descrição Técnica |
|---|---|---|
| 21 | **Verificação de postura do dispositivo (Device Posture Check)** | Antes de conceder acesso, o gateway consulta o MDM para validar: versão de OS, patch level, status de criptografia de disco, presença de EDR, jailbreak/root, senha de desbloqueio. Dispositivo não compliant é redirecionado para página de remediação. |
| 22 | **Certificado de cliente (mTLS) para autenticação de dispositivo** | O MDM provisiona um certificado de cliente no dispositivo (via SCEP/PKCS). O gateway valida a autenticidade do certificado, validade, e correspondência de chave privada antes de permitir a conexão. Elimina roubo de token de sessão. |
| 23 | **Acesso condicional baseado em IP conhecido** | O tráfego de autenticação do navegador seguro sai por um range de IPs conhecidos do proxy. O administrador usa esses IPs como condição em políticas de Conditional Access (Azure AD, Okta) para restringir acesso a aplicações SaaS. |
| 24 | **Filtragem baseada em identidade do usuário** | Políticas de navegação vinculadas ao usuário autenticado (via SAML/IdP), não apenas ao dispositivo. Permite perfis diferenciados: executivos, financeiro, TI, estagiários. |
| 25 | **Controle de tenant (Tenant Restrictions)** | Restringir acesso a tenants específicos de serviços cloud (Microsoft 365, Google Workspace, Slack, Dropbox). Impede acesso a tenants pessoais ou não autorizados a partir do dispositivo corporativo. |
| 26 | **DLP (Data Loss Prevention) em uploads web** | Inspeção de conteúdo em uploads para a web (Box, Google Drive, Dropbox) com bloqueio baseado em classificação de dados (PII, PHI, PCI). Requer DLP engine integrada ao gateway. |
| 27 | **Controle granular de aplicações web** | Bloquear ações específicas em aplicações SaaS: upload, download, print, copy/paste. Ex.: permitir visualizar Google Drive mas bloquear upload de arquivos. |
| 28 | **Túnel dividido (Split Tunnel) por FQDN** | Encaminhar apenas tráfego corporativo pelo túnel de segurança; tráfego de internet geral sai direto. Reduz latência e carga no gateway. Configurável por FQDN e por aplicação. |
| 29 | **Detecção de dispositivo ocioso com auto sign-out** | O agente detecta quando o dispositivo está unattended e desconecta automaticamente a sessão do usuário, protegendo recursos corporativos. |
| 30 | **Watermark em sessões sensíveis** | Aplicação de marca d'água dinâmica (com identidade do usuário, timestamp) em sessões de acesso a dados sensíveis, dissuadindo vazamento por screenshot. |


## NÍVEL 4 — EXPERT

**Recursos de arquitetura distribuída, resiliência, integração profunda e automação de resposta a incidentes.**

| # | Requisito Técnico | Descrição Técnica |
|---|---|---|
| 31 | **Arquitetura multi-tenant com isolamento de dados por cliente** | O gateway é multi-tenant na nuvem, com instância de dashboard e políticas dedicada por organização. Nenhum dado de usuário é processado ou armazenado compartilhadamente. |
| 32 | **Integração com EPP/EDR para correlação de telemetria** | O agente de navegação segura envia eventos para o EDR/EPP corporativo (CrowdStrike, Defender, SentinelOne) e recebe sinais de postura em tempo real. Correlação de eventos web com alertas de endpoint. |
| 33 | **API de automação de políticas** | Endpoint REST para criação, leitura, atualização e exclusão de políticas de navegação programaticamente. Permite integração com CI/CD de segurança, SOAR e scripts de provisionamento. RBAC na API com auditoria de todas as chamadas. |
| 34 | **SCIM-driven provisioning** | Sincronização automática de usuários e grupos do IdP corporativo (Okta, Azure AD) para o console de políticas. Provisionamento e desprovisionamento em tempo real. |
| 35 | **Inspeção de tráfego de aplicações nativas (não-browser)** | O agente deve inspecionar e aplicar políticas em tráfego HTTP/HTTPS gerado por aplicações nativas no dispositivo, não apenas no navegador. Cobre apps móveis, clientes desktop e agentes de atualização. |
| 36 | **Suporte a IPv6** | O agente e o gateway devem operar nativamente em ambientes dual-stack e IPv6-only, incluindo tunelamento e inspeção SSL/TLS sobre IPv6. |
| 37 | **Isolamento de dispositivo da LAN (Device Isolation)** | Capacidade de isolar o dispositivo da rede local (LAN) enquanto mantém o acesso à internet via túnel seguro. Previne movimento lateral em caso de comprometimento. |
| 38 | **Política de bypass de inspeção SSL para sites sensíveis** | Lista curada de domínios (banking, health, governo) que são isentos de descriptografia TLS, respeitando regulação e privacidade. A lista é gerenciada centralmente e empurrada via MDM. |
| 39 | **Failover e HA do gateway** | Múltiplos data centers do gateway com failover automático. O agente detecta indisponibilidade do nó primário e reconecta ao secundário sem intervenção do usuário.  |
| 40 | **Retenção e exportação de logs para SIEM** | Logs de navegação exportáveis via syslog, API ou conectores nativos para Splunk, QRadar, Sentinel. Retenção configurável com conformidade a GDPR, HIPAA, SOC 2. |


## NÍVEL 5 — PROFESSIONAL

**Recursos de vanguarda, customização profunda e integração com ecossistemas especializados. Para ambientes de alta segurança ou requisitos regulatórios extremos.**

| # | Requisito Técnico | Descrição Técnica |
|---|---|---|
| 41 | **Browser como Policy Enforcement Point (PEP) em Zero Trust** | O navegador seguro é designado como ponto primário de enforcement de política, com autorização dinâmica, context-aware, por requisição e imediatamente revogável. Aplicação de least-privilege por design arquitetural. |
| 42 | **Remote Browser Isolation para sessões privilegiadas** | Sessões de acesso a sistemas críticos (admin panels, consoles de produção) são forçadas a executar em isolamento remoto, neutralizando tanto comprometimento de endpoint quanto ameaças web-based. |
| 43 | **Watermark e prevenção de exfiltração por token** | Prevenção de exfiltração de token de sessão e session hijacking. Marca d'água em sessões sensíveis com identidade do usuário para dissuadir vazamento por screenshot. |
| 44 | **Content Disarm and Reconstruction (CDR)** | Arquivos baixados da web são desmontados, ameaças removidas e reconstruídos antes de chegar ao endpoint. Elimina exploits de documentos zero-day. |
| 45 | **Custom scripts e automação de remediação** | Capacidade de programar scripts customizados que são executados quando um dispositivo entra em estado não-compliant. Ex.: forçar logout, bloquear acesso, iniciar varredura de EDR, notificar SOC.  |
| 46 | **Suporte a Safe Exam Browser / Kiosk Mode para avaliações** | Modo de navegador bloqueado para exames e avaliações, com restrição total de navegação, desabilitação de teclas de atalho e impossibilidade de escape sem PIN de administrador. Requer MDM com Autonomous Single App Mode (iOS) ou kiosk mode (Android). |
| 47 | **Integração com PKI corporativa para emissão dinâmica de certificados** | Emissão just-in-time de certificados de cliente via SCEP/EST integrado ao MDM. O certificado é vinculado ao serial do dispositivo e instalado no keychain, com renovação automática antes do vencimento. |
| 48 | **Análise de tráfego criptografado sem descriptografia (ETSI / encrypted traffic analysis)** | Identificação de ameaças em tráfego TLS sem descriptografia, usando metadados, fingerprints de JA3/JA4 e análise comportamental. Respeita privacidade e regulações que proíbem MITM. |
| 49 | **Controle de extensões de navegador via MDM allowlist** | Restringir a instalação de extensões de navegador apenas a uma allowlist aprovada. Extensões não aprovadas são bloqueadas via política de MDM ou GPO. |
| 50 | **Auditoria e compliance contínuo automatizado** | Verificação contínua de conformidade da configuração do navegador, do agente e do dispositivo contra benchmarks CIS, NIST, BSI. Relatórios automatizados com evidências para auditoria externa. |


## Diagrama de Dependências Entre Camadas

```
┌──────────────────────────────────────────────────────────────┐
│                    PROFESSIONAL (Nível 5)                    │
│  Zero Trust PEP · RBI Privilegiado · CDR · Custom Scripts   │
│  Safe Exam Browser · PKI Dinâmica · ETI · Allowlist Ext.    │
│  Auditoria Contínua Automatizada                             │
└──────────────────────────┬───────────────────────────────────┘
                           │ depende de
┌──────────────────────────▼───────────────────────────────────┐
│                     EXPERT (Nível 4)                          │
│  Multi-tenant · EPP/EDR Integration · API Automação          │
│  SCIM · Inspeção de Apps Nativas · IPv6 · Device Isolation   │
│  SSL Bypass Curado · Failover HA · Export SIEM               │
└──────────────────────────┬───────────────────────────────────┘
                           │ depende de
┌──────────────────────────▼───────────────────────────────────┐
│                    AVANÇADO (Nível 3)                         │
│  Device Posture · mTLS · Conditional Access por IP           │
│  Identidade do Usuário · Tenant Restriction · DLP Upload     │
│  Controle App Web · Split Tunnel FQDN · Auto Sign-Out        │
│  Watermark                                                    │
└──────────────────────────┬───────────────────────────────────┘
                           │ depende de
┌──────────────────────────▼───────────────────────────────────┐
│                      BÁSICO (Nível 2)                         │
│  URL Allow/Deny · Categorias · SSL Inspection · DNS Sec.     │
│  Bloqueio Downloads · RBI · Clipboard Control · SafeSearch   │
│  Logs de Navegação                                            │
└──────────────────────────┬───────────────────────────────────┘
                           │ depende de
┌──────────────────────────▼───────────────────────────────────┐
│                     ESSENCIAL (Nível 1)                       │
│  Integração MDM API · Enrollment Obrigatório · Instalação    │
│  Silenciosa · Perfil VPN/Proxy · Multi-OS · Certificado CA   │
│  Anti-Tampering · Autenticação · Bloqueio Navegadores        │
│  Always-On / Auto-Start                                       │
└──────────────────────────────────────────────────────────────┘
```

**Fluxo de decisão em runtime:**

```
Dispositivo inicia
  → Agente auto-start (N1)
  → Consulta MDM: enrolled? compliant? (N1, N3)
  → Se NÃO: bloqueia toda navegação (fail-closed) (N1)
  → Se SIM: estabelece túnel VPN/Proxy (N1)
  → Usuário autentica via SAML/MFA (N1)
  → Certificado mTLS validado no gateway (N3)
  → Política de navegação é aplicada:
      → URL na allowlist? → ALLOW (N2)
      → URL em categoria bloqueada? → BLOCK (N2)
      → Site desconhecido/risco? → RBI isolado (N2, N5)
      → Upload com PII? → DLP BLOCK (N3)
      → Tenant não autorizado? → BLOCK (N3)
      → Sessão privilegiada? → RBI + Watermark (N5)
  → Log de evento → SIEM (N2, N4)
```