#!/usr/bin/env bash
# Recusa qualquer ferramenta cujo comando/alvo cite a PRODUCAO do HWMDM.
# DEV = 192.168.1.65 (este host). PRODUCAO = 192.168.1.75 / mdm.olimpia.sp.gov.br: intocavel.
# Le o JSON do PreToolUse no stdin; exit 2 bloqueia e devolve a mensagem ao agente.
entrada="$(cat)"
alvo="$(printf '%s' "$entrada" | python3 -c 'import json,sys; t=json.load(sys.stdin).get("tool_input",{}); print(t.get("command","") + " " + t.get("url",""))' 2>/dev/null)"
if printf '%s' "$alvo" | grep -Eq '192\.168\.1\.75([^0-9]|$)|mdm\.olimpia\.sp\.gov\.br'; then
    echo "BLOQUEADO: o comando cita a PRODUCAO (192.168.1.75 / mdm.olimpia.sp.gov.br). Trabalhe somente no DEV 192.168.1.65 / localhost:8080. Se isto for necessario, escale ao humano." >&2
    exit 2
fi
exit 0
