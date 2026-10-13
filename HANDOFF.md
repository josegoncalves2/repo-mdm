# HANDOFF DEFINITIVO — 2026-10-02

Autor: Claude Opus 4.6, a pedido do dono do projeto.
Premissa: NADA esta entregue. Zero itens passaram por prova de uso real.

---

## 1. O QUE E ESTE PROJETO

HWMDM e um fork do Headwind MDM (open source, Apache 2.0) customizado para a
Prefeitura de Olimpia. Gerencia tablets Android: enrollment, kiosk, acesso remoto,
webfilter, GPS, chat, logs, permissoes RBAC.

**Componentes:**

| Componente | Linguagem | Local no repo | Artefato |
|---|---|---|---|
| Servidor (painel web) | Java 17 + AngularJS 1.x | `server-source/` | `hmdm.war` |
| Launcher Android | Java (Android SDK 34) | `android-source/` | `hmdm-*.apk` |
| Agente Remoto | Java (Android SDK 34) | `remote-agent/` | `hwmdm-remote-*.apk` |
| Agente WebFilter | Java (Android SDK 34) | `webfilter-agent/` | `hwmdm-webfilter-*.apk` |
| Resolvedor DNS | Python 3.12 + Blocky | `webfilter-dns/` | container Docker |
| Infra Docker | docker-compose + scripts | `source/` | stack local |

**Ambientes:**

| Ambiente | IP | Acesso web | Regra |
|---|---|---|---|
| DEV | 192.168.1.65:8080 | admin / admin | Trabalhar aqui |
| PROD | 192.168.1.75 | mdm.olimpia.sp.gov.br | NUNCA TOCAR |

---

## 2. A TRAGEDIA — O QUE DEU ERRADO

### 2.1 Multiplos agentes IA sem coordenacao

Desde setembro/2026, varios agentes (Claude, Codex, DeepSeek via OpenRouter) mexeram
no repositorio ao mesmo tempo. Cada um:

- Fez commits com mensagens genericas ("ajustes", "ajustes e correcoes", ".")
- Editou codigo sem validar se funciona
- Declarou "entregue" sem nenhuma prova real
- Reverteu correcoes de outros agentes
- Criou arquivos .bak espalhados pelo repo
- Deixou lixo no source/volumes/ (1.8 GB de artefatos de runtime versionados)

### 2.2 Estrutura do repositorio corrompida

O `repo-mdm/` mistura:

1. **Codigo-fonte** (server-source, android-source, remote-agent, webfilter-agent,
   webfilter-dns) — o que DEVERIA estar no repo
2. **Runtime da stack** (source/volumes/) — 1.8 GB de logs, webapps explodidos,
   backups, WARs, plugins de runtime — NUNCA deveriam ser versionados
3. **Toolchain** (.android-sdk 603 MB, .jdk21 210 MB, .maven 11 MB) — deveriam
   ser instalados, nao versionados
4. **Artefatos compilados** (dist/ 151 MB de APKs e WARs) — podem ser reconstruidos
   a partir do fonte
5. **Documentacao processual** (openspec/, archived/, docs/) — valida mas misturada
   com o codigo
6. **Infra de agentes IA** (.claude/, .trava/, .githooks) — meta-projeto, nao
   codigo do produto
7. **Backups/lixo** (*.bak*, *.anterior, untracked files no webapp)

### 2.3 Nenhum item funciona comprovadamente

40 itens documentados nos handoffs 1-5. Status real:

| Status | Quantidade |
|---|---|
| NAO IMPLEMENTADO (nunca foi construido) | 18 |
| NAO CORRIGIDO / NAO FUNCIONA (codigo existe mas nao funciona) | 14 |
| PENDENTE (codigo alterado mas nunca validado em uso real) | 8 |
| ENTREGUE COM PROVA REAL | **0** |

### 2.4 Bugs criticos conhecidos

**Enrollment:**
- `GetServerConfigTask.java`: response == null quando ambos os servidores falham →
  crash do launcher. Fix foi aplicado mas nunca testado em device real
- `CryptoUtil.getBase64String()` usava `base64Url()` sem padding — Android espera
  Base64 standard. Fix foi aplicado E REVERTIDO por outro agente pelo menos uma vez
- APKs 1.9 e 1.10 bloqueados pelo Google Play Protect
- Certificado do APK 1.3 difere do 6.36 — impossibilita update OTA

**WebFilter:**
- O tablet NAO bloqueia nada. O campo `webfilterDnsHost` do sync nao e consumido
  por nenhum codigo no launcher. Nao existe Private DNS forcado nem VPN local
- Plugin no servidor compila mas nunca foi testado com dados reais
- `webfilter-agent` aplica `URLBlocklist` via Chrome policy, mas a politica do
  Chrome so funciona se o Chrome for gerenciado — e nao ha evidencia de que esteja

**Acesso Remoto:**
- Teclado nao funciona: `input_injection_config.xml` nao declara
  `canRetrieveWindowContent`, Android retorna null no `getRootInActiveWindow()`
- F5 ou troca de aba derruba a sessao — `$scope.$on('$destroy')` chama `stopRemote()`
- Device offline = sessao perdida, sem fila de comandos
- Tela travada/apagada = MediaProjection morre, sem wake lock
- Tablet bipa e acende tela sozinho (wake lock permanente?)
- APK aparece na gaveta de apps (LAUNCHER no intent-filter)

**GPS:**
- Tela de GPS: layout existe mas esta "crashado" ou nao carrega
- Historico de localizacao: tabela pode existir no banco mas esta vazia
- Nenhuma funcionalidade de mapa validada

**Permissoes RBAC:**
- 41 permissoes nunca auditadas — ninguem sabe quais funcionam
- Combobox de permissoes na tela de papeis nao grava nada
- Nao existe segregacao ver/editar
- Nomes em ingles, descricoes erradas ou ausentes

**Infra:**
- `docker-entrypoint.sh` destroi customizacoes CSS/JS ao reiniciar
  (usuario desabilitou com .bak)
- Banco restaurado de PROD tem schema diferente do codigo compilado
- Container hwmdm-admin nao sobe automaticamente

**UI/UX:**
- Botoes de cada modulo em estilo diferente
- Menus incoerentes, plugins listados sem proposito
- Menu lateral instavel — se move ao clicar, perde estado no F5
- Sem controle de versao/build do projeto

---

## 3. INVENTARIO DO REPOSITORIO — O QUE FICA E O QUE SAI

### 3.1 FICA no `repo-mdm/` (codigo-fonte do produto)

| Pasta | Conteudo | Tamanho |
|---|---|---|
| `server-source/` | Backend Java (WAR), plugins, webapp AngularJS | 226 MB |
| `android-source/` | Launcher Android (APK) | 114 MB |
| `remote-agent/` | Agente de acesso remoto (APK) | 18 MB |
| `webfilter-agent/` | Agente webfilter (APK) | 15 MB |
| `webfilter-dns/` | Resolvedor DNS (container) | 28 KB |
| `source/` (so infra) | docker-compose, Dockerfile, .env, entrypoint, templates, SQL | ~5 MB |
| `VERSION` | Versao do projeto | 6 bytes |
| `.gitignore` | Exclusoes | 1.5 KB |
| `.gitattributes` | EOL fixes | 0.5 KB |

### 3.2 MOVE para `/opt/projetos/hwmdm/arquivados/refatoracao-20261002/`

| O que | Por que | De | Tamanho |
|---|---|---|---|
| `.android-sdk/` | Toolchain, nao e codigo | repo-mdm/ | 603 MB |
| `.jdk21/` | Toolchain, nao e codigo | repo-mdm/ | 210 MB |
| `.maven/` | Toolchain, nao e codigo | repo-mdm/ | 11 MB |
| `dist/` | Artefatos compilados, reconstruiveis | repo-mdm/ | 151 MB |
| `source/volumes/` | Runtime da stack (logs, webapps, backups, cache) | repo-mdm/source/ | 1.8 GB |
| `archived/` | Documentacao processual historica | repo-mdm/ | 16 MB |
| `docs/` | Handoffs, prompts — historico, nao codigo | repo-mdm/ | 108 KB |
| `openspec/` | Specs de changes — meta-projeto | repo-mdm/ | 800 KB |
| `.claude/` | Config de agentes IA — meta-projeto | repo-mdm/ | 348 KB |
| `.trava/` | Sistema de travas — meta-projeto | repo-mdm/ | 136 KB |
| `.githooks/` | Hooks git customizados | repo-mdm/ | 4 KB |
| Arquivos .bak | Lixo de backups manuais | varios | ~200 MB |

### 3.3 Arquivos-orfaos a resolver

| Arquivo | Situacao |
|---|---|
| `server-source/.../webapp/__autologin.html` | Untracked, deletado do volumes — investigar se e necessario |
| `server-source/.../webapp/shell.html` | Untracked, deletado do volumes — idem |
| `server-source/.../webapp/tab-content.html` | Untracked, deletado do volumes — idem |
| `source/docker-compose.yaml.bak-*` | Backup, mover |
| `source/docker-entrypoint.sh.bak-*` | Backup, mover |
| `source/.env.anterior` | Backup, mover |
| `dist/hmdm.war.bak.*` | Backup, mover |
| `source/volumes/backups/pre-restore-deepseek-mess-*` | Backup de estrago do DeepSeek |

---

## 4. COMO SOLUCIONAR — PASSO A PASSO

### FASE 0: Refatoracao do repositorio (AGORA)

1. Criar `/opt/projetos/hwmdm/arquivados/refatoracao-20261002/`
2. Mover toolchain: `.android-sdk/`, `.jdk21/`, `.maven/`
3. Mover artefatos: `dist/`
4. Mover runtime: `source/volumes/`
5. Mover meta-projeto: `archived/`, `docs/`, `openspec/`, `.claude/`, `.trava/`, `.githooks/`
6. Mover lixo: todos os `*.bak*`, `*.anterior`
7. Atualizar `.gitignore` para refletir nova estrutura
8. Resultado: `repo-mdm/` fica LIMPO so com codigo-fonte

### FASE 1: Estabilizar o que existe (proximas sessoes)

**Prioridade 1 — Enrollment (sem isso nada mais funciona):**
1. Confirmar estado atual do `CryptoUtil.java` — o fix de base64 esta aplicado?
2. Confirmar `GetServerConfigTask.java` — null guard esta no codigo?
3. Recompilar APK com keystore `hwmdm-release.jks`
4. Instalar em tablet limpo via ADB
5. Testar enrollment via QR do perfil 57
6. Prova: screenshot do tablet matriculado + status no painel

**Prioridade 2 — Acesso Remoto basico:**
1. Corrigir `input_injection_config.xml`: adicionar `canRetrieveWindowContent="true"`
2. Remover LAUNCHER intent-filter do `StatusActivity`
3. Recompilar APK remoto
4. Testar: abrir sessao, digitar texto, fechar sessao limpo
5. Prova: video do teclado funcionando

**Prioridade 3 — WebFilter no dispositivo:**
1. No launcher, consumir `webfilterDnsHost` do sync
2. Usar `DevicePolicyManager.setGlobalPrivateDns()` (requer Device Owner)
3. OU implementar VPN local que redireciona DNS
4. Testar: acessar site bloqueado no Chrome, ver pagina de bloqueio
5. Prova: screenshot do bloqueio

**Prioridade 4 — GPS:**
1. Investigar estado real da tabela `device_location_history`
2. Corrigir tela de GPS no frontend
3. Validar que coordenadas chegam do device
4. Prova: mapa com posicao real do tablet

**Prioridade 5 — Permissoes:**
1. Auditar as 41 permissoes: nome × efeito real
2. Corrigir nomes pt-BR
3. Fazer combobox de papeis gravar de verdade
4. Implementar verificacao no REST (nao so na tela)
5. Prova: usuario sem permissao nao consegue editar

### FASE 2: UI/UX e polimento (depois que tudo funciona)

- Layout M365 (barra lateral, sub-menus)
- Responsividade de todas as telas
- Padronizacao de botoes
- Menu lateral estavel
- Traducoes pt-BR completas
- Controle de versao/build

### FASE 3: Funcionalidades avancadas (futuro)

- Historico GPS com timeline, heatmap, geofencing
- Console de helpdesk enterprise (filas, gravacao, chat)
- Modularidade (desativar modulo sem parar tudo)
- Push de comandos para device offline

---

## 5. REGRAS PARA O PROXIMO AGENTE

1. **NAO declarar "entregue" sem prova real** — screenshot, video, log do device
2. **NAO fazer commits com mensagem "ajustes"** — descrever o que mudou e por que
3. **NAO reverter codigo de outro agente** sem entender o que ele fez
4. **NAO versionar runtime** (volumes/, logs, webapps explodidos)
5. **NAO versionar toolchain** (SDK, JDK, Maven)
6. **NAO criar scripts** — so o software do produto
7. **NAO usar sub-agentes Claude** — so DeepSeek/OpenRouter
8. **NAO tocar PROD** (192.168.1.75)
9. **Testar TUDO no DEV** (192.168.1.65:8080, admin/admin)
10. **Um agente deploya por vez** — coordenar pelo `.trava/`
11. **Ler o handoff antes de comecar** — nao repetir trabalho

---

## 6. MAPA DE ARQUIVOS-CHAVE

### Enrollment
- `android-source/app/src/main/java/com/hmdm/launcher/task/GetServerConfigTask.java` — fluxo de enrollment
- `android-source/app/src/main/java/com/hmdm/launcher/helper/CryptoHelper.java` — verificacao de hash do QR
- `server-source/common/src/main/java/com/hmdm/util/CryptoUtil.java` — geracao do hash Base64
- `server-source/server/src/main/java/com/hmdm/rest/resource/QRCodeResource.java` — endpoint QR

### Acesso Remoto
- `remote-agent/app/src/main/java/com/hwmdm/remote/service/InputInjectionService.java` — injecao de teclado
- `remote-agent/app/src/main/java/com/hwmdm/remote/service/ScreenStreamService.java` — captura de tela
- `remote-agent/app/src/main/java/com/hwmdm/remote/service/RemoteAgentService.java` — WebSocket com servidor
- `remote-agent/app/src/main/res/xml/input_injection_config.xml` — config de acessibilidade (FALTA canRetrieveWindowContent)
- `server-source/server/src/main/webapp/app/components/main/controller/remote.controller.js` — UI remoto
- `server-source/server/src/main/webapp/app/components/main/view/remote.html` — template remoto

### WebFilter
- `server-source/plugins/webfilter/` — plugin completo (Liquibase, MyBatis, REST, webapp)
- `webfilter-agent/` — APK que aplica Chrome policy
- `webfilter-dns/` — container DNS com Blocky
- `android-source/app/src/main/java/com/hmdm/launcher/helper/ConfigUpdater.java` — onde deveria consumir webfilterDnsHost

### GPS
- `server-source/server/src/main/webapp/app/components/main/controller/gpsmap.controller.js` — frontend mapa
- `server-source/server/src/main/webapp/app/components/main/view/gpsmap.html` — template mapa
- `server-source/plugins/deviceinfo/` — plugin que recebe coordenadas

### Permissoes
- `server-source/server/src/main/webapp/app/components/main/controller/roles.controller.js` — UI papeis
- `server-source/server/src/main/webapp/app/components/main/view/settings/roles.html` — template papeis
- `server-source/common/src/main/java/com/hmdm/security/SecurityContext.java` — verificacao de permissoes
- `server-source/common/src/main/java/com/hmdm/persistence/domain/User.java` — modelo de usuario

### Infra
- `source/docker-compose.yaml` — stack Docker
- `source/Dockerfile` — imagem do servidor
- `source/docker-entrypoint.sh` — entrypoint (CUIDADO: destroi customizacoes)
- `source/.env` — variaveis de ambiente (segredos!)
- `source/sql/hmdm-dump.sql` — dump do banco DEV
- `android-source/app/build.gradle` — build do launcher (versionCode, versionName, keystore)
- `remote-agent/app/build.gradle` — build do agente remoto (versionCode 37, versionName 1.37)

### Keystore
- `android-source/keystore/hwmdm-release.jks` — senha: HwMdm!Release2026, alias: hwmdm
- TODOS os APKs devem ser assinados com este keystore para update OTA funcionar

---

## 7. ESTADO DO BANCO (PostgreSQL)

Banco: `hmdm` / usuario: `hmdm` / senha: `M4YUX9XpWS5knpJSgVAj9r1d`

Tabelas principais:
- `applications`, `applicationversions` — cadastro de APKs
- `configurations` — perfis de dispositivo (57 = modelo-kiosk)
- `devices` — tablets matriculados
- `users`, `userroles`, `permissions`, `userrolepermissions` — RBAC
- `settings` — config do customer (cores, logo, alertas)
- `plugin_webfilter_*` — webfilter (policies, entries, events, delivery, settings, sources)
- `plugin_audit_log` — auditoria
- `plugin_deviceinfo_*` — GPS, bateria, wifi, mobile
- `plugin_devicelog_*` — logs do device
- `plugin_messaging_*` — chat
- `plugin_push_*` — push messages
- `databasechangelog` — Liquibase migration history

Dump atualizado: `source/sql/hmdm-dump.sql` (20.128 linhas)
Backup do estrago do DeepSeek: `source/volumes/backups/pre-restore-deepseek-mess-20261002.sql.gz`

---

## 8. ESTADO DO GIT

Ultimo commit: `6aacc311 ajustes`
Branch: provavelmente main/master (unica)
Historico: ~30 commits, todos com mensagens genericas
Git LFS: nao configurado (APKs grandes no .gitignore)
Repo remoto: configurado no GitHub

Commits problematicos no historico:
- `7273b9e9 .` — commit sem mensagem
- Varios `ajustes` e `ajustes e correcoes` sem detalhe

---

## 9. O QUE FOI MOVIDO NA REFATORACAO

A refatoracao executada junto com este handoff moveu para
`/opt/projetos/hwmdm/arquivados/refatoracao-20261002/`:

```
repo-mdm/.android-sdk/     → arquivados/.../toolchain/android-sdk/
repo-mdm/.jdk21/           → arquivados/.../toolchain/jdk21/
repo-mdm/.maven/           → arquivados/.../toolchain/maven/
repo-mdm/dist/             → arquivados/.../artefatos/dist/
repo-mdm/source/volumes/   → arquivados/.../runtime/volumes/
repo-mdm/archived/         → arquivados/.../historico/archived/
repo-mdm/docs/             → arquivados/.../historico/docs/
repo-mdm/openspec/         → arquivados/.../meta-projeto/openspec/
repo-mdm/.claude/          → arquivados/.../meta-projeto/claude/
repo-mdm/.trava/           → arquivados/.../meta-projeto/trava/
repo-mdm/.githooks/        → arquivados/.../meta-projeto/githooks/
*.bak*, *.anterior         → arquivados/.../lixo/
```

Os symlinks necessarios para o toolchain funcionar estao documentados abaixo.

---

## 10. COMO RESTAURAR O TOOLCHAIN

Apos a refatoracao, para compilar o projeto:

```bash
# Symlinks do toolchain (ou copiar de volta)
ln -s /opt/projetos/hwmdm/arquivados/refatoracao-20261002/toolchain/android-sdk \
      /opt/projetos/hwmdm/repo-mdm/.android-sdk
ln -s /opt/projetos/hwmdm/arquivados/refatoracao-20261002/toolchain/jdk21 \
      /opt/projetos/hwmdm/repo-mdm/.jdk21
ln -s /opt/projetos/hwmdm/arquivados/refatoracao-20261002/toolchain/maven \
      /opt/projetos/hwmdm/repo-mdm/.maven

# A stack Docker precisa dos volumes para rodar
# Recriar source/volumes/ vazio e subir a stack:
mkdir -p /opt/projetos/hwmdm/repo-mdm/source/volumes
cd /opt/projetos/hwmdm/repo-mdm/source
docker compose up -d
```

---

*Fim do handoff. Boa sorte.*
