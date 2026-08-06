# Provisionar o HWMDM numa maquina nova

Este guia leva de um clone limpo ate o painel no ar, em outra rede, com outro IP e outro
dominio.

## O que voce precisa na maquina de destino

- Docker + plugin Compose, com seu usuario no grupo `docker`
- `git`, `openssl`
- Porta 8080 livre (o Tomcat escuta nela; o TLS fica no seu proxy reverso)
- Um nome DNS apontando para a maquina

Confira tudo de uma vez:

```bash
./scripts/provision.sh --check
```

## Subida em um comando

```bash
git clone https://github.com/josegoncalves2/repo-mdm.git
cd repo-mdm

./scripts/provision.sh \
  --dominio mdm.suaempresa.com.br \
  --ip 192.168.0.10 \
  --proxy 10.20.30.1 \
  --email voce@suaempresa.com.br
```

Sem argumentos ele pergunta cada valor. O script e' idempotente: rodar de novo nao
duplica nada e nao sobrescreve o `.env` existente (use `--force-env` para regerar).

O que ele faz, nesta ordem: confere pre-requisitos, gera `source/.env`, coloca o WAR e o
APK no lugar, sobe os containers, restaura o dump **se** o banco estiver vazio, reaponta
as URLs do dump para o seu dominio, espera o Tomcat e confere o resultado.

## Os tres valores que decidem se funciona

| Valor | O que quebra se estiver errado |
|---|---|
| `BASE_DOMAIN` | Vai gravado no QR code. Errado = nenhum tablet matricula. |
| `LOCAL_IP` | IP desta maquina na rede dos tablets. O container o usa no `/etc/hosts` para resolver o proprio dominio. |
| `PROXY_ADDRESSES` | IPs dos saltos confiaveis (proxy reverso, tunel). Vazio ou errado = o painel mostra o IP do proxy no lugar do IP do dispositivo. |

Para descobrir o `LOCAL_IP`: `ip -4 addr show | grep inet`

Para descobrir o `PROXY_ADDRESSES`: e' o IP de onde as requisicoes dos tablets **chegam**
ao Tomcat. Suba sem ele, faca um tablet sincronizar e veja o que o painel mostra na coluna
de IP — esse valor e' o que deve entrar aqui.

## SHARED_SECRET: cuidado ao restaurar o dump

O dump em `source/sql/hmdm-dump.sql.gz` traz os dispositivos ja matriculados. Se voce
quiser que eles continuem sendo reconhecidos, o `SHARED_SECRET` do destino tem de ser o
**mesmo** do ambiente de origem. O script oferece gerar um novo — so aceite isso se for
comecar do zero.

## O que esta no repositorio

| Caminho | O que e' |
|---|---|
| `dist/hmdm.war` | Aplicacao construida, **com as alteracoes Java deste projeto**. Nao troque pelo WAR do h-mdm.com. |
| `dist/hmdm-v1.0-kiosk.apk` | Agente Android assinado (v2+v3), com as APIs de kiosk |
| `source/sql/hmdm-dump.sql.gz` | Dump do banco (perfis, dispositivos, usuarios) |
| `server-source/` | Codigo do servidor e do painel web |
| `android-source/` | Codigo do agente Android + keystore de release |
| `scripts/provision.sh` | Este provisionamento |
| `scripts/aplicar-seletores-mdm.py` | Reaplica os seletores de perfil apos qualquer restauracao |
| `tests/playwright/` | Suite de testes contra o dominio real |

## Reconstruir os artefatos

**APK** (precisa de JDK 21 — com o 25 o Gradle 8.13 falha com "Unsupported class file
major version 69"):

```bash
cd android-source
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ANDROID_HOME=$PWD/../.android-sdk \
  ./gradlew :app:assembleRelease --no-daemon
```

Conferir sempre antes de publicar — tem de dar `v2: true` e `v3: true`:

```bash
apksigner verify -v app/build/outputs/apk/opensource/release/app-opensource-release.apk
```

Um APK assinado so com v1 e' recusado pelo Android 14 e sinalizado pelo Play Protect.
Foi exatamente essa a causa do erro de Play Protect em 26/07/2026.

**WAR**: `cd server-source && mvn clean package` — o `clean` nao e' opcional, ver
"armadilhas" abaixo.

## Rodar os testes

```bash
npm install
HWMDM_BASE_URL=https://mdm.suaempresa.com.br \
  npx playwright test tests/playwright/hwmdm-perfil-opcoes.spec.js \
  --config=tests/playwright/playwright.config.js --workers=1
```

## Armadilhas conhecidas

**O keystore nao pode mudar.** `android-source/keystore/hwmdm-release.jks` e' a identidade
permanente do APK. O Android recusa atualizar um app quando o certificado muda: trocar ou
perder o keystore obriga factory reset em todo tablet matriculado. Faca backup fora do git.

**Tablets ja matriculados com o APK oficial da Headwind nao aceitam o nosso.** As
assinaturas diferem. Apontar `configurations.mainappid` para uma versao nossa faz o agente
tentar instalar e falhar em todo sync — o que reaparece como erro de Play Protect. Ate
haver factory reset + nova matricula, deixe `mainappid` na versao oficial que eles ja tem.

**Deploy do WAR com o Tomcat vivo corrompe a explosao.** A sequencia confiavel e': parar o
container, remover `source/volumes/webapps/ROOT`, trocar o WAR, subir de novo.

**`mvn package` sem `clean` ressuscita arquivos deletados**, porque o maven-war-plugin
mantem um diretorio explodido em `server/target/`. Use sempre `mvn clean package`.

**`BaseIPFilter` tem IPs desta rede compilados dentro.** A constante `INFRASTRUCTURE_IPS`
em `server-source/common/src/main/java/com/hmdm/rest/filter/BaseIPFilter.java` lista
`10.0.17.106, 10.0.9.1, 10.1.1.1`. Noutra rede ela simplesmente nao casa com nada — nao
quebra, mas tambem nao filtra. Quem comanda a leitura do `X-Forwarded-For` e' o
`PROXY_ADDRESSES` do `.env`, e esse e' parametrizado. Se quiser o filtro extra no destino,
edite a constante e reconstrua o WAR.

**`mainappid` e `configurationapplications` sao dois lados da mesma relacao.** Mudar o
`configurations.mainappid` por SQL sem mudar junto o `applicationversionid` da linha
correspondente em `configurationapplications` deixa o perfil inconsistente: o editor nao
consegue resolver o app principal, e no proximo save grava `mainappid = NULL`. O efeito
visivel e' o **botao de QR code sumir da lista de perfis**, porque `qrCodeAvailable()` exige
`mainAppId > 0`. Foi o que aconteceu em 26/07/2026 com o perfil "Kiosk Total". Ao trocar a
versao do app principal, atualize sempre os dois:

```sql
UPDATE configurationapplications SET applicationversionid=<versao>
 WHERE configurationid=<perfil> AND applicationid=<app>;
UPDATE configurations SET mainappid=<versao> WHERE id=<perfil>;
```

`tests/playwright/hwmdm-qrcode.spec.js` cobre essa regressao.

**O painel esteve fora do controle de versao.** `server-source/` ficou no `.gitignore` ate
26/07/2026, e nesse periodo o layout novo foi sobrescrito por uma copia antiga e so foi
recuperado do diretorio de build do maven. Agora esta versionado — mantenha assim.

## Pendencias conhecidas

- Kiosk real nos tablets ja matriculados exige factory reset + nova matricula (bloqueio de
  assinatura do Android, nao do codigo).
- Registrar uma versao nova de APK ainda exige SQL; nao ha tela para isso.
- As telas `kiosk` e `remote` foram restauradas e sao servidas, mas nao foram verificadas
  ponta a ponta.
