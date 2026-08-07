# Acesso remoto a tela dos tablets

Este documento existe porque a pergunta "por que nao consigo ver a tela do tablet?"
ja custou tempo varias vezes, e a resposta nao esta em lugar nenhum do codigo do painel.

## Situacao atual: nao ha visualizacao de tela, e nunca houve

O agente em producao e o `hmdm-6.36-os.apk` oficial. Ele **nao possui nenhum mecanismo de
leitura de tela**. Isto foi verificado extraindo o dex do proprio binario:

| Recurso | 6.36 oficial |
|---|---|
| push `screenshot` | ausente |
| `createScreenCaptureIntent` | ausente |
| permissao `MEDIA_PROJECTION` | ausente |
| `BIND_ACCESSIBILITY_SERVICE` | ausente |
| push `textMessage` | ausente |

Como reproduzir a verificacao:

```bash
python3 - <<'PY'
import zipfile
z = zipfile.ZipFile('source/volumes/work/files/hmdm-6.36-os.apk')
blob = b''.join(z.read(n) for n in z.namelist() if n.endswith('.dex'))
for s in ["screenshot", "createScreenCaptureIntent", "MEDIA_PROJECTION", "textMessage"]:
    print(f"  {'SIM' if s.encode() in blob else 'NAO'}  {s}")
PY
```

Por isso o painel exibia "Nenhuma captura de tela voltou" para sempre: o comando era
enfileirado e entregue, e o agente simplesmente nao sabia o que era. O painel agora diz
isso na cara, em vez de oferecer um botao que nao tem como funcionar.

**Consequencia pratica:** `textMessage` tambem esta ausente, entao "Mensagem rapida" e
"Exibir mensagem" no painel tambem nao chegam ao dispositivo hoje.

## Por que a solucao nao pode ser modificar o launcher

Regra do projeto, em `scripts/verificar-apk.sh` linha 3: o `hmdm-6.36-os.apk` e "o unico
permitido e comprovadamente funcional". Builds proprios do launcher sao bloqueados pelo
Google Play Protect na matricula por QR desde 2026 — o proprio `scripts/publicar-apk.sh`
avisa isso no final.

E o launcher nunca foi o lugar certo: a Headwind pos tela e gesto num **aplicativo
separado**, e deixou o hook para ele dentro do launcher:

```java
// android-source/.../launcher/Const.java
public static final String APUPPET_PACKAGE_NAME       = "com.hmdm.control";
public static final String APUPPET_SERVICE_CLASS_NAME = "com.hmdm.control.GestureDispatchService";
```

## O caminho que resolve: aPuppet

[aPuppet](https://apuppet.org) e o motor de acesso remoto open source da propria Headwind.

- App Android `com.hmdm.control`, disponivel na Google Play (portanto sem problema de
  Play Protect) e com codigo aberto
- Servidor proprio: Janus (WebRTC) + nginx + certbot + web-admin, via docker-compose
- Espelha a tela **e replica gestos** — clique e digitacao funcionam
- Launcher 6.36 fica intocado

### O limite da versao gratuita

O README do proprio projeto e explicito. A versao **Premium**:

> * is seamlessly integrated into Headwind MDM as a module;
> * automatically starts by executing a command from the remote server;
> * doesn't require user interaction and is suitable for kiosk devices;

Ou seja, na versao gratuita **alguem precisa estar no tablet**: a sessao e iniciada no
aparelho, que exibe um Session ID e uma senha para o tecnico digitar no web-admin. Foi
confirmado no codigo: o app open source tem `MainActivity` com `action.MAIN` como unica
entrada, nenhum `BroadcastReceiver`, nada exportado alem do servico de acessibilidade.

Para tablets em quiosque, desassistidos, isso nao serve.

### As tres opcoes reais

| | Custo | Quiosque desassistido | Clique/digitacao | Esforco |
|---|---|---|---|---|
| **aPuppet Premium** | US$ 590/ano | sim | sim | baixo — integra ao painel |
| **aPuppet free + gatilho proprio** | zero | sim | sim | medio — adicionar um receiver ao app aPuppet (nao ao launcher) que escute `com.hmdm.push.*` e inicie o compartilhamento |
| **aPuppet free puro** | zero | **nao** | sim | baixo — exige alguem no tablet lendo o Session ID |

A opcao do meio e legitima: o aPuppet e open source e o launcher continua sendo o 6.36
oficial e intocado. O que se modifica e o aplicativo de acesso remoto, que e um app
gerenciado comum instalado pelo MDM — ele nao passa pela matricula por QR, que e onde o
Play Protect bloqueia.

### Nao tente compilar o aPuppet do codigo-fonte

O repositorio publico nao compila mais. A dependencia central do WebRTC so foi publicada
no JCenter, que a JFrog desativou:

```
org.webrtc:google-webrtc
  repo1.maven.org  (Maven Central) -> 404
  dl.google.com    (Google Maven)  -> 404
  jcenter.bintray                  -> 301 (desativado)
```

Alem disso o projeto e Gradle 5.6.4 / AGP 3.6.3 / compileSdk 29, e a toolchain deste
repositorio e JDK 21 / Gradle 8.13 / SDK 34. Para compilar seria preciso migrar o build
inteiro **e** trocar a biblioteca WebRTC por um fork mantido (por exemplo
`io.github.webrtc-sdk:android`, que preserva o pacote `org.webrtc`). E' um projeto, nao
um ajuste.

**Use o APK publicado pela Headwind** (Google Play, `com.hmdm.control`). Ele e assinado
por eles, o que tambem evita qualquer discussao de Play Protect.

### Pre-requisitos de infraestrutura (validos para qualquer das opcoes)

1. **Dominio dedicado** para o aPuppet (ex.: `remoto.olimpia.sp.gov.br`) resolvendo para
   um IP publico. O instalador verifica a posse do dominio e aborta sem ele.
2. **Certificado TLS.** O WebRTC no navegador exige contexto seguro. A versao gratuita
   usa LetsEncrypt via certbot; certificado proprio e recurso Premium.
3. **Portas 80 e 443** livres no host que receber o aPuppet.
4. **Permissoes no tablet, uma vez por aparelho:** acessibilidade (para os gestos) e
   sobreposicao de tela. O launcher tenta conceder sozinho, mas so consegue em build com
   `SYSTEM_PRIVILEGES`, que o 6.36 open source nao tem — o proprio codigo comenta:
   *"device owner can only grant permissions to self, not to other apps"*.

Sobre o item 1: **o dominio ja existe e ja atende em HTTPS** — `mdm.olimpia.sp.gov.br`
responde 200. E `191.5.114.162` nao e' de terceiros: e' o proprio IP publico desta rede
(confirmado com `curl https://api.ipify.org` a partir de `192.168.1.75`). O que falta e'
apenas decidir o hostname que o aPuppet vai usar e onde termina o TLS dele.

Nenhum host desta LAN escuta na 443 (varredura em `ip neigh` deu vazio), entao a
terminacao TLS acontece antes da rede — no proprio roteador (`192.168.1.254`) ou no
provedor. Quem mexer no nginx precisa chegar ate la.

### Pendencia conhecida no proxy

Independente do aPuppet: o nginx em `191.5.114.162` nao repassa upgrade de WebSocket.
Verificado com handshake em HTTP/1.1 — direto no Tomcat responde `101`, atraves do proxy
responde `404`. Se qualquer recurso de tempo real for exposto pelo dominio publico, falta
isto no bloco `location`:

```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade    $http_upgrade;
proxy_set_header Connection $connection_upgrade;
proxy_read_timeout 3600s;
```

com `map $http_upgrade $connection_upgrade { default upgrade; '' close; }` no bloco `http`.
