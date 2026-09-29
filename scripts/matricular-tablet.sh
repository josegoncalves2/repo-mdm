#!/usr/bin/env bash
# Matricula um tablet como Device Owner por ADB, sem passar pelo QR do setup wizard.
#
# Por que existe: desde 2026 o Google bloqueia no Play Protect a matricula por QR de
# qualquer build proprio do launcher Headwind -- o bloqueio e' na identidade do
# certificado, nao no formato do APK, entao nenhum ajuste de assinatura resolve.
# Ver https://h-mdm.com/open-source/ ("such packages will still be blocked by Play
# Protect at the enrollment step").
#
# O caminho por ADB nao usa o ManagedProvisioning, entao nao encosta nesse portao:
# instala o APK direto e promove o pacote a Device Owner pelo shell.
#
# PRE-REQUISITOS NO TABLET (sem eles o set-device-owner falha):
#   - recem-saido de factory reset, SEM nenhuma conta Google adicionada
#   - "Depuracao USB" ligada em Opcoes do desenvolvedor
#   - autorizar a chave RSA do computador quando o tablet perguntar
#
#   ./scripts/matricular-tablet.sh                       # usa o ultimo APK de dist/
#   ./scripts/matricular-tablet.sh --apk dist/x.apk
#   ./scripts/matricular-tablet.sh --serial R58N12345    # com varios tablets ligados
#   ./scripts/matricular-tablet.sh --perfil 44           # diz qual QR usar no fim
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

APK=""; SERIAL=""; PERFIL=""; MANTER_DADOS=0
COMPONENTE="com.hmdm.launcher/com.hmdm.launcher.AdminReceiver"
PACOTE="com.hmdm.launcher"

while [ $# -gt 0 ]; do
    case "$1" in
        --apk)    APK="$2"; shift 2 ;;
        --serial) SERIAL="$2"; shift 2 ;;
        --perfil) PERFIL="$2"; shift 2 ;;
        --manter-dados) MANTER_DADOS=1; shift ;;
        -h|--help) sed -n '2,26p' "$0"; exit 0 ;;
        *) echo "opcao desconhecida: $1" >&2; exit 2 ;;
    esac
done

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m  ok\033[0m %s\n' "$*"; }
aviso(){ printf '\033[1;33m  !!\033[0m %s\n' "$*"; }
erro() { printf '\033[1;31mERRO\033[0m %s\n' "$*" >&2; }

# A URL do servidor sai do .env, para a mensagem final nao mentir noutro ambiente.
# shellcheck disable=SC1091
[ -f source/.env ] && { set -a; . ./source/.env; set +a; }

# ---------------------------------------------------------------- adb
command -v adb >/dev/null 2>&1 || {
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
    [ -x "$SDK/platform-tools/adb" ] && PATH="$SDK/platform-tools:$PATH" || {
        erro "adb nao encontrado (instale platform-tools ou exporte ANDROID_HOME)"; exit 1; }
}
adb start-server >/dev/null 2>&1 || true

# ---------------------------------------------------------------- 1/7 dispositivo
info "1/7 Procurando o tablet"
if [ -z "$SERIAL" ]; then
    mapfile -t ONLINE < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')
    case "${#ONLINE[@]}" in
        0) erro "nenhum tablet autorizado. Confira: cabo USB, 'Depuracao USB' ligada,"
           erro "e o dialogo 'Permitir depuracao USB?' aceito na tela do tablet."
           adb devices -l; exit 1 ;;
        1) SERIAL="${ONLINE[0]}" ;;
        *) erro "mais de um tablet conectado -- escolha com --serial:"; adb devices -l; exit 1 ;;
    esac
fi
ADB=(adb -s "$SERIAL")
"${ADB[@]}" wait-for-device
MODELO="$("${ADB[@]}" shell getprop ro.product.model | tr -d '\r')"
ANDROID="$("${ADB[@]}" shell getprop ro.build.version.release | tr -d '\r')"
API="$("${ADB[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
ok "$SERIAL  $MODELO  Android $ANDROID (API $API)"
[ "${API:-0}" -ge 26 ] || { erro "o agente exige Android 8+ (API 26); este e' API $API"; exit 1; }

# ---------------------------------------------------------------- 2/7 pre-condicoes
# O set-device-owner e' recusado se houver qualquer conta no aparelho, e a mensagem
# do adb nao diz isso com clareza. Melhor conferir antes de mexer em nada.
info "2/7 Conferindo as pre-condicoes do Device Owner"
CONTAS="$("${ADB[@]}" shell dumpsys account 2>/dev/null | grep -c 'Account {' || true)"
if [ "${CONTAS:-0}" -gt 0 ]; then
    erro "o tablet tem ${CONTAS} conta(s) cadastrada(s); o Android recusa Device Owner assim."
    erro "Remova todas as contas (ou faca factory reset) e rode de novo."
    exit 1
fi
ok "nenhuma conta cadastrada"

DONO_ATUAL="$("${ADB[@]}" shell dumpsys device_policy 2>/dev/null | sed -n 's/.*Device Owner: *//p' | head -1 | tr -d '\r')"
if [ -n "$DONO_ATUAL" ]; then
    aviso "ja existe um Device Owner: $DONO_ATUAL"
    aviso "Se nao for o nosso pacote, so' factory reset remove."
fi

# ---------------------------------------------------------------- 3/7 APK
info "3/7 Escolhendo o APK"
if [ -z "$APK" ]; then
    APK="$(ls -1t dist/hmdm-*.apk 2>/dev/null | head -1 || true)"
    [ -n "$APK" ] || { erro "nenhum APK em dist/ -- rode ./scripts/publicar-apk.sh antes"; exit 1; }
fi
[ -f "$APK" ] || { erro "APK nao encontrado: $APK"; exit 1; }
ok "$APK ($(du -h "$APK" | cut -f1))"

# ---------------------------------------------------------------- 4/7 verificador
# O shell do adb tem WRITE_SECURE_SETTINGS, entao aqui (e so' aqui) da' para desligar
# de verdade o verificador de instalacao. Dentro do app isso nao e' possivel: essas
# chaves nao estao na allowlist do setGlobalSetting do Device Owner.
info "4/7 Desligando a verificacao de instalacao por ADB"
ANTES_ADB="$("${ADB[@]}" shell settings get global verifier_verify_adb_installs | tr -d '\r')"
ANTES_PKG="$("${ADB[@]}" shell settings get global package_verifier_enable | tr -d '\r')"
"${ADB[@]}" shell settings put global verifier_verify_adb_installs 0 >/dev/null 2>&1 || true
"${ADB[@]}" shell settings put global package_verifier_enable 0 >/dev/null 2>&1 || true
ok "verifier_verify_adb_installs: ${ANTES_ADB} -> 0"
ok "package_verifier_enable: ${ANTES_PKG} -> 0"

# ---------------------------------------------------------------- 5/7 instalar
info "5/7 Instalando o agente"
JA_INSTALADO="$("${ADB[@]}" shell pm list packages "$PACOTE" | tr -d '\r')"
if [ -n "$JA_INSTALADO" ] && [ "$MANTER_DADOS" -eq 0 ]; then
    # Uma versao antiga assinada com outro certificado faz o install falhar com
    # INSTALL_FAILED_UPDATE_INCOMPATIBLE. Desinstalar resolve e nao custa nada aqui,
    # porque o aparelho acabou de sair de factory reset.
    aviso "$PACOTE ja instalado -- removendo para evitar conflito de assinatura"
    "${ADB[@]}" shell pm uninstall "$PACOTE" >/dev/null 2>&1 || true
fi

# -g concede as permissoes de runtime na instalacao; sem isso o agente sobe pedindo
# permissao numa tela que o proprio kiosk pode esconder.
SAIDA_INSTALL="$("${ADB[@]}" install -r -g "$APK" 2>&1 || true)"
if ! grep -q "Success" <<<"$SAIDA_INSTALL"; then
    erro "instalacao falhou:"
    printf '%s\n' "$SAIDA_INSTALL" >&2
    case "$SAIDA_INSTALL" in
        *INSTALL_FAILED_UPDATE_INCOMPATIBLE*)
            erro "-> versao anterior tem outro certificado. Rode com o app removido, ou factory reset." ;;
        *INSTALL_FAILED_VERIFICATION_FAILURE*)
            erro "-> o Play Protect bloqueou mesmo com o verificador desligado."
            erro "   Desligue tambem em Play Store > Play Protect > Configuracoes." ;;
        *INSTALL_FAILED_DEPRECATED_SDK_VERSION*)
            erro "-> targetSdk incompativel com esta versao do Android." ;;
    esac
    exit 1
fi
ok "instalado"

# ---------------------------------------------------------------- 6/7 device owner
info "6/7 Promovendo a Device Owner"
SAIDA_DPM="$("${ADB[@]}" shell dpm set-device-owner "$COMPONENTE" 2>&1 || true)"
if ! grep -qi "Success" <<<"$SAIDA_DPM"; then
    erro "set-device-owner falhou:"
    printf '%s\n' "$SAIDA_DPM" >&2
    case "$SAIDA_DPM" in
        *"already several users"*|*"users on the device"*)
            erro "-> existe mais de um usuario. Remova os perfis extras ou faca factory reset." ;;
        *"accounts on the device"*|*account*)
            erro "-> ainda ha conta cadastrada no aparelho." ;;
        *"already set"*|*"already has a device owner"*)
            erro "-> ja existe Device Owner; so' factory reset troca." ;;
        *"not allowed after device setup"*|*provisioned*)
            erro "-> o setup wizard ja foi concluido. O Android so' aceita Device Owner"
            erro "   antes disso: faca factory reset e PULE o login de conta Google." ;;
    esac
    exit 1
fi
ok "$(tr -d '\r' <<<"$SAIDA_DPM")"

# ---------------------------------------------------------------- 7/7 conferencia
info "7/7 Conferindo"
CONFERE="$("${ADB[@]}" shell dumpsys device_policy 2>/dev/null | grep -A2 'Device Owner:' | tr -d '\r' || true)"
if grep -q "$PACOTE" <<<"$CONFERE"; then
    ok "Device Owner ativo: $PACOTE"
else
    erro "o dumpsys nao confirma o Device Owner -- confira manualmente:"
    erro "  adb -s $SERIAL shell dumpsys device_policy | head -30"
    exit 1
fi
"${ADB[@]}" shell monkey -p "$PACOTE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
ok "agente iniciado no tablet"

echo
info "Falta so' apontar o agente para este servidor"
echo
echo "  O APK e' compilado com BASE_URL = https://app.h-mdm.com (padrao do upstream),"
echo "  e a matricula por ADB nao carrega os extras que o QR carregaria. Entao, na tela"
echo "  que o agente abriu agora, use o leitor de QR dele e aponte para:"
echo
if [ -n "$PERFIL" ] && [ -f "dist/qr-config${PERFIL}.png" ]; then
    echo "      dist/qr-config${PERFIL}.png"
else
    echo "      dist/qr-config<PERFIL>.png     (gere com ./scripts/publicar-apk.sh --perfil N)"
fi
echo
echo "  E' o MESMO arquivo do QR de matricula: o agente le os extras dele e grava"
echo "  a URL do servidor. Alternativa manual: digitar ${PUBLIC_PROTOCOL:-http}://${BASE_DOMAIN:?defina BASE_DOMAIN no source/.env}"
echo
echo "  Depois confira o tablet aparecendo no painel."
