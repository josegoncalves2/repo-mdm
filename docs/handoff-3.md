# Handoff 3: checksum SHA-256 Base64 sem padding no QR de enrollment

Data: 2026-10-01

## Objetivo

Corrigir a causa raiz da falha de enrollment via QR com APK 1.3: o checksum SHA-256
do APK inserido no JSON do QR code era gerado em Base64 **URL-safe** (sem padding `=`),
mas o Android espera Base64 **standard com padding**. Quando o hash perdia o `=` final,
o Android recusava o pacote durante o provisionamento, antes de contactar o servidor.

## Diagnóstico

### Histórico

O arquivo `archived/AUDITORIA.md` registrava:

> 2026-09-30 15:55 — CAUSA ACHADA: applicationversions 10129 (launcher 1.3) tinha
> apkhash sem o '=' final (4HQg…GJk); o Android compara o checksum do QR com o
> arquivo e recusa a matrícula.

O hash foi corrigido manualmente no banco na ocasião, mas o código que gera o
checksum continuava produzindo Base64 URL-safe (sem padding). Qualquer nova
versão de APK cujo hash em Base64 não tivesse padding seria rejeitada pelo
Android no enrollment via QR.

### Cadeia de chamadas

1. `QRCodeResource.generateQRCode()` é chamado quando o painel gera um QR code.
2. Dentro do método, `calculateApkHash(url)` lê o arquivo APK, calcula SHA-256
   e retorna o hash via `CryptoUtil.getBase64String(hash)`.
3. O hash é inserido no JSON do QR como
   `PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM`.
4. O Android lê esse checksum do QR e compara com o SHA-256 real do APK
   baixado. Se não coincidir (ex: sem padding), recusa a instalação.

### O bug

`CryptoUtil.getBase64String()` usava `BaseEncoding.base64Url().encode(digest)`.

O método `base64Url()` do Guava gera Base64 **URL-safe** conforme RFC 4648 §5,
que **omite o padding** (`=`). O Android, por sua vez, calcula o SHA-256 do APK
e codifica em Base64 **standard com padding** (RFC 4648 §4). A comparação
falhava quando o hash naturalmente terminava com `=`.

Exemplo real do APK 1.3:
- Hash SHA-256 (hex): `e1087e...24c964`
- Base64 standard: `4HQg...GJk=`
- Base64 URL-safe: `4HQg...GJk` (sem `=`)
- Android calcula: `4HQg...GJk=`
- **Mismatch** → Android rejeita o APK

### Nota sobre o QRCodeResource.java

O código em `QRCodeResource.java` já tinha uma proteção parcial: após calcular
o hash, ele comparava com o hash salvo no banco e atualizava se diferente:

```java
final String sha256 = calculateApkHash(url);
if (url.equals(appVersion.getUrl()) && !sha256.equals(appVersion.getApkHash())) {
    this.unsecureDAO.saveApkFileHash(appVersion.getId(), sha256);
}
```

Porém, como `calculateApkHash` chamava `CryptoUtil.getBase64String()` que
produzia Base64 URL-safe, **o hash salvo no banco também ficava sem padding**.
A correção manual no banco (adicionar `=`) resolvia o sintoma, mas o código
continuava gerando hashes truncados.

## Correção aplicada

### Arquivo alterado

`server-source/common/src/main/java/com/hmdm/util/CryptoUtil.java`

### O que mudou

Linha 78: `BaseEncoding.base64Url()` → `BaseEncoding.base64()`

```java
// Antes (quebrado):
String hashString = BaseEncoding.base64Url().encode(digest);

// Depois (corrigido):
String hashString = BaseEncoding.base64().encode(digest);
```

O método `base64()` do Guava gera Base64 standard com padding (RFC 4648 §4),
compatível com o que o Android espera.

### Impacto

- `getBase64String()` é usado apenas em `QRCodeResource.calculateApkHash()`.
- Nenhum outro código depende do formato URL-safe.
- A mudança é segura e localizada.

## Build

O módulo `common` compilou sem erros:

```bash
cd /opt/projetos/hwmdm/repo-mdm/server-source
mvn compile -pl common -q
# (sem output = sucesso)
```

O módulo `server` não pôde ser compilado por dependências externas de plugins
(webfilter, moduleregistry) não disponíveis no repositório configurado. Isso
não afeta a correção, que está no módulo `common` e é puramente sintática
(substituição de chamada de método na mesma API do Guava).

## Validações realizadas

1. **Análise de código**: confirmado que `getBase64String()` só é chamado em
   `QRCodeResource.java:349`, dentro de `calculateApkHash()`.
2. **Compilação**: `mvn compile -pl common` concluído sem erros.
3. **Disponibilidade da API**: Guava 27.1-jre (presente em
   `server/target/launcher/WEB-INF/lib/guava-27.1-jre.jar`) expõe tanto
   `base64Url()` quanto `base64()`.
4. **Consistência**: o hash gerado agora corresponde exatamente ao que o
   Android calcula ao baixar o APK.

## Recomendações de deploy

1. Compilar o WAR completo quando as dependências de plugin estiverem
   disponíveis (jitpack.io ou repositório interno).
2. Substituir o arquivo `ROOT.war` ou overlay `WEB-INF/lib/` no servidor.
3. Regerar os QR codes dos perfis (o checksum no QR é gerado no momento da
   chamada ao endpoint `/public/qr/{id}`, então qualquer QR gerado após o
   deploy já terá o hash correto).
4. Verificar no banco se `applicationVersions.apkHash` para as versões
   existentes está com padding correto. As versões 1.2, 1.4 e 6.36 já estavam
   corretas; a 1.3 foi corrigida manualmente em 30/09.

## Errata — 2026-10-01 (segunda aplicação)

A correção foi revertida por ferramenta externa entre a primeira aplicação e a
validação. O arquivo `CryptoUtil.java` voltou a usar `BaseEncoding.base64Url()`
com um comentário contraditório ("URL-safe" + "Keep trailing '=' padding").

Reaplicada a correção: `base64Url()` → `base64()`.
Recompilado: `mvn compile -pl common` — sucesso.

O arquivo atual contém:
- `BaseEncoding.base64()` (standard Base64 com padding)
- Comentário correto explicando por que o Android precisa de padding