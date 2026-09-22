#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

BLOCKED_TEXT_PATTERN='(?i:mocko|\bmock(ed|ing)?\b|mockito|org\.mockito|\bstub(s|bed|bing)?\b|lorem ipsum|sample data|example data|dummy data|com\.example|com\.exemplo|org\.example|br\.com\.empresa|Example(Unit|Instrumented)Test|placeholder (implementation|data|stub|temporario|provisorio|temporary))|TODO|FIXME'
PLACEHOLDER_EXAMPLE_PATTERN='(?i)placeholder=(["'\''])[^"'\''>]*(exemplo|example|dummy|lorem|com\.example|com\.exemplo|org\.example|br\.com\.empresa)[^"'\''>]*\1'

PATHS=(
  "android-source/app/src"
  "android-source/lib/src"
  "remote-agent/app/src"
  "server-source/pom.xml"
  "server-source/server/src/main/java"
  "server-source/server/src/main/webapp/app"
  "server-source/server/src/main/webapp/localization"
  "server-source/plugins"
  "scripts"
  ".githooks"
)

EXISTING_PATHS=()
for path in "${PATHS[@]}"; do
  if [[ -e "$path" ]]; then
    EXISTING_PATHS+=("$path")
  fi
done

if [[ ${#EXISTING_PATHS[@]} -eq 0 ]]; then
  exit 0
fi

matches="$(
  {
  rg -n --pcre2 "$BLOCKED_TEXT_PATTERN" "${EXISTING_PATHS[@]}" \
    -g '!**/build/**' \
    -g '!**/target/**' \
    -g '!**/node_modules/**' \
    -g '!**/__pycache__/**' \
    -g '!**/*.class' \
    -g '!**/*.jar' \
    -g '!**/*.war' \
    -g '!**/*.apk' \
    -g '!android-source/app/src/main/java/org/eclipse/paho/**' \
    -g '!server-source/server/src/main/webapp/app/lib/**' \
    -g '!server-source/server/src/main/webapp/lib/**' \
    -g '!server-source/server/webtarget/**' \
    -g '!source/**' \
    || true
  rg -n --pcre2 "$PLACEHOLDER_EXAMPLE_PATTERN" "${EXISTING_PATHS[@]}" \
    -g '!**/build/**' \
    -g '!**/target/**' \
    -g '!**/node_modules/**' \
    -g '!**/__pycache__/**' \
    -g '!**/*.class' \
    -g '!**/*.jar' \
    -g '!**/*.war' \
    -g '!**/*.apk' \
    -g '!android-source/app/src/main/java/org/eclipse/paho/**' \
    -g '!server-source/server/src/main/webapp/app/lib/**' \
    -g '!server-source/server/src/main/webapp/lib/**' \
    -g '!server-source/server/webtarget/**' \
    -g '!source/**' \
    || true
  }
)"

filtered="$(
  printf '%s\n' "$matches" \
    | grep -vE 'IMdmApi\.Stub|scripts/guard-no-placeholders\.sh:|generated|\.placeholder["'\'':]' \
    || true
)"

if [[ -n "$filtered" ]]; then
  printf '%s\n' "ERRO: marcadores de lixo encontrados (mock/stub/TODO/example/lorem/etc.):" >&2
  printf '%s\n' "$filtered" >&2
  printf '%s\n' "Remova o placeholder, mock, stub ou marcador antes de continuar." >&2
  exit 1
fi
