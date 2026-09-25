#!/usr/bin/env bash
# uso: medir.sh <raiz_webapp> <tema> <cenario> <largura...>
set -u
H=$(cd "$(dirname "$0")" && pwd)
CHROME=$(ls -d ~/.cache/ms-playwright/chromium_headless_shell-*/chrome-headless-shell-linux64/chrome-headless-shell | head -1)
RAIZ=$1; TEMA=$2; CEN=$3; shift 3
OUT="$H/h-$(basename "$RAIZ")-$TEMA-$CEN.html"
python3 "$H/gen.py" "$RAIZ" "$TEMA" "$CEN" "$OUT"
for W in "$@"; do
  R=$(timeout 60 "$CHROME" --headless --no-sandbox --disable-gpu --hide-scrollbars \
      --allow-file-access-from-files --window-size="$W,900" --virtual-time-budget=8000 \
      --dump-dom "file://$OUT" 2>/dev/null | grep -o 'RESULT{.*}END' | sed 's/^RESULT//; s/END$//')
  echo "$R"
done
