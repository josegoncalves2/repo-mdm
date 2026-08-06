#!/usr/bin/env bash
# Confere se um APK do launcher segue o mesmo padrao do binario oficial da Headwind
# (hmdm-6.36-os.apk), que e' o unico permitido e comprovadamente funcional.
#
#   scripts/verificar-apk.sh <apk>
#
# Sai com 0 se o APK pode ser publicado, 1 se qualquer conferencia falhar.

set -u

APK="${1:-}"
if [ -z "$APK" ] || [ ! -f "$APK" ]; then
    echo "uso: $0 <apk>" >&2
    exit 1
fi

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
BT="$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)"

if [ -z "$BT" ] || [ ! -x "$BT/apksigner" ]; then
    echo "FALHA: build-tools do Android SDK nao encontradas em $SDK" >&2
    exit 1
fi

# DN do keystore de release deste servidor (android-source/keystore/README.md)
DN_ESPERADO="CN=Headwind MDM Self-Hosted, OU=TI, O=Prefeitura Municipal de Olimpia, L=Olimpia, ST=SP, C=BR"

falhas=0
ok()    { printf '  \033[32mOK\033[0m    %s\n' "$1"; }
falha() { printf '  \033[31mFALHA\033[0m %s\n' "$1"; falhas=$((falhas + 1)); }

echo "Conferindo $APK"

ASSINATURA="$("$BT/apksigner" verify --verbose --print-certs "$APK" 2>&1)"

if ! grep -q '^Verifies$' <<< "$ASSINATURA"; then
    falha "assinatura nao confere (apksigner recusou o APK)"
    echo "$ASSINATURA" | sed 's/^/        /'
    exit 1
fi
ok "assinatura confere"

esquema() { grep -m1 "^Verified using $1 scheme" <<< "$ASSINATURA" | grep -oE '(true|false)$'; }

[ "$(esquema v1)" = "false" ] && ok "v1 desligado" || falha "v1 ligado (recusado no targetSdk 34)"
[ "$(esquema v2)" = "true"  ] && ok "v2 ligado"    || falha "v2 desligado (exigido pelo targetSdk 34)"
[ "$(esquema v3)" = "false" ] && ok "v3 desligado" || falha "v3 ligado (o binario oficial nao tem bloco de rotacao de chave)"

DN="$(grep -m1 '^Signer #1 certificate DN: ' <<< "$ASSINATURA" | sed 's/^Signer #1 certificate DN: //')"
if [ "$DN" = "$DN_ESPERADO" ]; then
    ok "assinado com o keystore de release"
else
    falha "DN inesperado: $DN"
fi

# Um servico de acessibilidade declarado no manifest e' o gatilho classico do Play Protect
# em APK sideloaded. O launcher -os oficial nao declara nenhum.
MANIFEST="$("$BT/aapt2" dump xmltree --file AndroidManifest.xml "$APK" 2>/dev/null)"
if [ -z "$MANIFEST" ]; then
    falha "nao foi possivel ler o AndroidManifest.xml do APK"
elif grep -qi 'accessibilityservice' <<< "$MANIFEST"; then
    falha "o manifest declara servico de acessibilidade"
    grep -i 'accessibilityservice' <<< "$MANIFEST" | sed 's/^ */        /'
else
    ok "nenhum servico de acessibilidade declarado"
fi

if "$BT/zipalign" -c 4 "$APK" >/dev/null 2>&1; then
    ok "alinhado em 4 bytes"
else
    falha "APK nao alinhado (zipalign -c 4)"
fi

VERSAO="$("$BT/aapt2" dump badging "$APK" 2>/dev/null | grep -m1 '^package:')"
echo "  ----  $VERSAO"
echo "  ----  sha256 $(sha256sum "$APK" | cut -d' ' -f1)"

if [ "$falhas" -ne 0 ]; then
    printf '\n\033[31m%s conferencia(s) falharam — NAO publique este APK.\033[0m\n' "$falhas"
    exit 1
fi

printf '\n\033[32mAPK pode ser publicado.\033[0m\n'
