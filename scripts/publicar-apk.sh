#!/usr/bin/env bash
# Publica um APK recem-compilado do agente: arquivo -> servidor -> banco -> QR.
#
# Por que este script existe: publicar um APK a mao tem quatro passos que precisam
# terminar com o MESMO hash, e ate hoje cada um era feito separado. Quando um deles
# ficava para tras o sintoma no tablet era sempre o mesmo -- a matricula morria com
# erro de verificacao, que o setup wizard mostra como erro de Play Protect. Os quatro:
#
#   1. o arquivo servido em source/volumes/work/files/
#   2. applicationversions.apkhash no banco  (vai dentro do QR)
#   3. configurations.mainappid  E  configurationapplications.applicationversionid
#      -- os dois lados da mesma relacao; mexer num so' faz o proximo save do painel
#      gravar mainappid = NULL e o botao de QR sumir (ver PROVISIONAMENTO.md)
#   4. o PNG/JSON do QR em dist/
#
# O script faz os quatro numa transacao so' e no fim RECONFERE baixando o APK pela
# mesma URL que o tablet usa. Se o hash de ponta a ponta nao bater, ele falha.
#
#   ./scripts/publicar-apk.sh                          # publica o ultimo build
#   ./scripts/publicar-apk.sh --apk caminho/x.apk
#   ./scripts/publicar-apk.sh --perfil 44              # tambem aponta o perfil 44
#   ./scripts/publicar-apk.sh --perfil 44 --nome hmdm-v1.0-kiosk.apk
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

APK=""; PERFIL=""; NOME=""; PULAR_QR=0
BUILD_PADRAO=android-source/app/build/outputs/apk/opensource/release/app-opensource-release.apk

while [ $# -gt 0 ]; do
    case "$1" in
        --apk)    APK="$2"; shift 2 ;;
        --perfil) PERFIL="$2"; shift 2 ;;
        --nome)   NOME="$2"; shift 2 ;;
        --sem-qr) PULAR_QR=1; shift ;;
        -h|--help) sed -n '2,25p' "$0"; exit 0 ;;
        *) echo "opcao desconhecida: $1" >&2; exit 2 ;;
    esac
done

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m  ok\033[0m %s\n' "$*"; }
erro() { printf '\033[1;31mERRO\033[0m %s\n' "$*" >&2; }

# shellcheck disable=SC1091
set -a; . ./source/.env; set +a
BASE_URL="${PUBLIC_PROTOCOL}://${BASE_DOMAIN}"
# O nome do container e' PERGUNTADO ao Compose, nunca escrito a mao. Ele foi
# "source-postgresql-1" ate o projeto Compose ganhar um `name:` proprio, e todo script
# que tinha o nome antigo cravado passou a falhar com "No such container" -- num ponto
# em que metade da publicacao ja tinha acontecido. Perguntar mantem os dois em sincronia
# sozinhos, inclusive quando COMPOSE_PROJECT_NAME muda no .env.
PG_CONTAINER="$(docker compose -f "$RAIZ/source/docker-compose.yaml" ps -q postgresql 2>/dev/null | head -1)"
[ -n "$PG_CONTAINER" ] || { erro "container do postgres nao esta rodando -- suba a stack primeiro (docker compose -f source/docker-compose.yaml up -d)"; exit 1; }
PSQL=(docker exec -i "$PG_CONTAINER" psql -U "$SQL_USER" -d "$SQL_BASE" -v ON_ERROR_STOP=1)

# ---------------------------------------------------------------- ferramentas
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
AAPT="$(ls -1 "$SDK"/build-tools/*/aapt2 2>/dev/null | sort -V | tail -1 || true)"
APKSIGNER="$(ls -1 "$SDK"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1 || true)"
[ -x "$AAPT" ]      || { erro "aapt2 nao encontrado em $SDK/build-tools/"; exit 1; }
[ -x "$APKSIGNER" ] || { erro "apksigner nao encontrado em $SDK/build-tools/"; exit 1; }

[ -n "$APK" ] || APK="$BUILD_PADRAO"
[ -f "$APK" ] || { erro "APK nao encontrado: $APK  (compile antes: cd android-source && ./gradlew :app:assembleRelease)"; exit 1; }

# ---------------------------------------------------------------- 1/6 identificar
info "1/6 Lendo o APK"
BADGING="$("$AAPT" dump badging "$APK")"
PKG="$(sed -n "s/^package: name='\([^']*\)'.*/\1/p" <<<"$BADGING")"
VERSAO="$(sed -n "s/.*versionName='\([^']*\)'.*/\1/p" <<<"$BADGING" | head -1)"
VCODE="$(sed -n "s/^package:.*versionCode='\([^']*\)'.*/\1/p" <<<"$BADGING" | head -1)"
[ -n "$PKG" ] && [ -n "$VERSAO" ] || { erro "nao consegui ler package/versionName do APK"; exit 1; }
ok "$PKG  versao $VERSAO  (versionCode $VCODE)"

[ -n "$NOME" ] || NOME="hmdm-${VERSAO}-olimpia.apk"
case "$NOME" in *.apk) ;; *) NOME="${NOME}.apk" ;; esac

# ---------------------------------------------------------------- 2/6 assinatura
# O APK oficial da Headwind que instala sem alarde e' v2-only. Qualquer desvio disso
# (v1 sobrando, v3 sobrando) volta a ser escrutinado na instalacao, entao conferimos
# o perfil ANTES de publicar em vez de descobrir no tablet.
info "2/6 Conferindo o perfil de assinatura"
SIG="$("$APKSIGNER" verify --verbose --print-certs "$APK" 2>/dev/null)"
grep -q '^Verifies' <<<"$SIG" || { erro "APK nao verifica -- assinatura invalida"; exit 1; }
v1="$(sed -n 's/^Verified using v1 scheme (JAR signing): \(.*\)/\1/p' <<<"$SIG")"
v2="$(sed -n 's/^Verified using v2 scheme (APK Signature Scheme v2): \(.*\)/\1/p' <<<"$SIG")"
v3="$(sed -n 's/^Verified using v3 scheme (APK Signature Scheme v3): \(.*\)/\1/p' <<<"$SIG")"
if [ "$v1" != "false" ] || [ "$v2" != "true" ] || [ "$v3" != "false" ]; then
    erro "perfil de assinatura fora do padrao do APK oficial (v1=false v2=true v3=false)"
    erro "  obtido: v1=$v1 v2=$v2 v3=$v3 -- ajuste signingConfigs em android-source/app/build.gradle"
    exit 1
fi
ok "v1=false v2=true v3=false (igual ao APK oficial)"
ok "$(sed -n 's/^Signer #1 certificate DN: //p' <<<"$SIG")"

# ---------------------------------------------------------------- 3/6 publicar arquivo
info "3/6 Publicando o arquivo"
HASH="$(openssl dgst -sha256 -binary "$APK" | openssl base64 -A | tr '+/' '-_' | tr -d '=')"
mkdir -p dist source/volumes/work/files
# cp sem -n de proposito: o bug classico aqui e' republicar com o mesmo nome e o
# arquivo antigo continuar servido, deixando o hash do banco correto e os bytes errados.
# O -f nao basta: quando o --apk ja e' o proprio destino o cp aborta, entao pulamos.
copiar() { [ "$(readlink -f "$1")" = "$(readlink -f "$2")" ] || cp -f "$1" "$2"; }
copiar "$APK" "dist/$NOME"
copiar "$APK" "source/volumes/work/files/$NOME"
ok "dist/$NOME"
ok "source/volumes/work/files/$NOME"
ok "sha256 (url-safe b64): $HASH"

URL="${BASE_URL}/files/${NOME}"

# ---------------------------------------------------------------- 4/6 banco
info "4/6 Registrando no banco"

# Se o pacote nao existe em applications, o INSERT nao produz linha e os UPDATEs abaixo
# gravariam mainappid = NULL -- exatamente o estrago descrito em PROVISIONAMENTO.md.
# Melhor abortar aqui do que deixar o perfil sem app principal.
APPID="$("${PSQL[@]}" -tA -c "SELECT id FROM applications WHERE pkg='${PKG}' ORDER BY id LIMIT 1;" | tr -d '\r')"
[ -n "$APPID" ] || { erro "pacote ${PKG} nao esta cadastrado em applications -- cadastre o app no painel primeiro"; exit 1; }

# Uma unica instrucao: em Postgres a saida de uma CTE que escreve so' e' visivel dentro
# da propria instrucao, entao os dois lados da relacao tem de viajar juntos aqui.
CTE_PERFIL=""; STMT_FINAL="SELECT id FROM nova;"
if [ -n "$PERFIL" ]; then
    CTE_PERFIL=", sync_ca AS (
    UPDATE configurationapplications ca
       SET applicationversionid = (SELECT id FROM nova)
     WHERE ca.configurationid = ${PERFIL}
       AND ca.applicationid = (SELECT applicationid FROM nova)
       AND (SELECT id FROM nova) IS NOT NULL
    RETURNING ca.id
)"
    STMT_FINAL="UPDATE configurations
   SET mainappid = (SELECT id FROM nova)
 WHERE id = ${PERFIL}
   AND (SELECT id FROM nova) IS NOT NULL
   AND (SELECT count(*) FROM sync_ca) >= 0;"
fi

"${PSQL[@]}" >/dev/null <<SQL
BEGIN;
WITH nova AS (
    INSERT INTO applicationversions (applicationid, version, url, apkhash, versioncode)
    VALUES (${APPID}, '${VERSAO}', '${URL}', '${HASH}', ${VCODE})
    ON CONFLICT (applicationid, version)
    DO UPDATE SET url = EXCLUDED.url,
                  apkhash = EXCLUDED.apkhash,
                  versioncode = EXCLUDED.versioncode
    RETURNING id, applicationid
)${CTE_PERFIL}
${STMT_FINAL}
COMMIT;
SQL
ok "applicationversions: ${PKG} ${VERSAO} -> ${URL}"
[ -n "$PERFIL" ] && ok "perfil ${PERFIL}: mainappid e configurationapplications apontam para ${VERSAO}"

# ---------------------------------------------------------------- 5/6 QR
if [ -n "$PERFIL" ] && [ "$PULAR_QR" -eq 0 ]; then
    info "5/6 Regerando o QR do perfil ${PERFIL}"
    CHAVE="$("${PSQL[@]}" -tA -c "SELECT qrcodekey FROM configurations WHERE id=${PERFIL};" | tr -d '\r')"
    if [ -z "$CHAVE" ]; then
        erro "perfil ${PERFIL} nao existe"; exit 1
    fi
    HWMDM_POSTGRES_CONTAINER="$PG_CONTAINER" \
        python3 gerar-qr.py --config-key "$CHAVE" --base-url "$BASE_URL" \
            --json-out "dist/qr-config${PERFIL}.json" \
            --png-out  "dist/qr-config${PERFIL}.png" \
        || { erro "gerar-qr.py falhou"; exit 1; }
    ok "dist/qr-config${PERFIL}.png"
else
    info "5/6 QR nao regerado (sem --perfil)"
fi

# ---------------------------------------------------------------- 6/6 conferencia
# Conferencia de ponta a ponta: baixa pela mesma URL que o tablet usa e compara com
# o que ficou gravado no banco. E' esta checagem que pega arquivo velho servido,
# hash desatualizado e QR obsoleto -- as tres causas de matricula morta.
info "6/6 Conferindo ponta a ponta"
HASH_BANCO="$("${PSQL[@]}" -tA -c \
    "SELECT coalesce(apkhash,'') FROM applicationversions av
      JOIN applications a ON a.id=av.applicationid
     WHERE a.pkg='${PKG}' AND av.version='${VERSAO}';" | tr -d '\r')"
HASH_SERVIDO="$(curl -fsS -m 60 "$URL" | openssl dgst -sha256 -binary | openssl base64 -A | tr '+/' '-_' | tr -d '=' || true)"

FALHOU=0
[ "$HASH_BANCO"   = "$HASH" ] && ok "banco confere"        || { erro "banco: $HASH_BANCO != $HASH"; FALHOU=1; }
[ "$HASH_SERVIDO" = "$HASH" ] && ok "arquivo servido confere" || { erro "servido: $HASH_SERVIDO != $HASH"; FALHOU=1; }

if [ -n "$PERFIL" ] && [ -f "dist/qr-config${PERFIL}.json" ]; then
    HASH_QR="$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM'])" \
               "dist/qr-config${PERFIL}.json" 2>/dev/null || echo '')"
    [ "$HASH_QR" = "$HASH" ] && ok "QR confere" || { erro "QR: $HASH_QR != $HASH"; FALHOU=1; }
fi

[ "$FALHOU" -eq 0 ] || { erro "publicacao inconsistente -- NAO use este QR"; exit 1; }

echo
ok "Publicado e conferido: ${PKG} ${VERSAO}"
echo
echo "  Atencao: desde 2026 o Google bloqueia no Play Protect a matricula por QR de"
echo "  builds proprios do launcher Headwind (h-mdm.com/open-source). Se o tablet"
echo "  parar no Play Protect, matricule por ADB:"
echo
echo "      ./scripts/matricular-tablet.sh --apk dist/${NOME}"
