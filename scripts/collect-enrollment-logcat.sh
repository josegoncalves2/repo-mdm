#!/usr/bin/env bash
set -euo pipefail

serial=""
clear_logcat=false
out_dir="logs/enrollment"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --serial)
      serial="${2:-}"
      shift 2
      ;;
    --clear)
      clear_logcat=true
      shift
      ;;
    --out-dir)
      out_dir="${2:-}"
      shift 2
      ;;
    *)
      echo "Uso: $0 [--serial SERIAL] [--clear] [--out-dir DIR]" >&2
      exit 2
      ;;
  esac
done

if ! command -v adb >/dev/null 2>&1; then
  echo "adb não encontrado no PATH." >&2
  exit 2
fi

if [[ -z "$serial" ]]; then
  serial="$(adb devices | awk 'NR > 1 && $2 == "device" {print $1; exit}')"
fi

if [[ -z "$serial" ]]; then
  echo "Nenhum device ADB conectado/autorizado. Conecte o tablet por USB e autorize ADB para coletar logcat real." >&2
  exit 3
fi

mkdir -p "$out_dir"
timestamp="$(date +%Y%m%d-%H%M%S)"
safe_serial="$(printf '%s' "$serial" | tr -c 'A-Za-z0-9_.-' '_')"
out_file="${out_dir}/${timestamp}-${safe_serial}.logcat.txt"

if [[ "$clear_logcat" == true ]]; then
  adb -s "$serial" logcat -c
fi

echo "Coletando logcat real de $serial em $out_file"
echo "Reproduza o enrollment agora. Pare com Ctrl+C quando a falha aparecer."
adb -s "$serial" logcat -v threadtime | tee "$out_file"
