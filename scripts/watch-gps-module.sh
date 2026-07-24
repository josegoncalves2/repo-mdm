#!/usr/bin/env bash
set -u

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="${PROJECT_DIR}/artifacts/validation"
WATCH_LOG="${REPORT_DIR}/gps-module-watch.log"
MAX_ATTEMPTS="${HWMDM_GPS_WATCH_ATTEMPTS:-60}"
SLEEP_SECONDS="${HWMDM_GPS_WATCH_SLEEP_SECONDS:-30}"

mkdir -p "${REPORT_DIR}"

{
  echo "# HWMDM GPS module watcher"
  echo "started=$(date -Is)"
  echo "max_attempts=${MAX_ATTEMPTS}"
  echo "sleep_seconds=${SLEEP_SECONDS}"

  for attempt in $(seq 1 "${MAX_ATTEMPTS}"); do
    device_count="$(adb devices | awk 'NR > 1 && $2 == "device" {count++} END {print count+0}' 2>/dev/null || echo 0)"
    db_devices="$(docker exec hwmdm-postgresql-1 psql -U hmdm -d hmdm -At -c "select count(*) from devices;" 2>/dev/null || echo 0)"
    db_locations="$(docker exec hwmdm-postgresql-1 psql -U hmdm -d hmdm -At -c "select count(*) from devices where info is not null and info::jsonb ? 'location';" 2>/dev/null || echo 0)"

    echo "attempt=${attempt} time=$(date -Is) adb_devices=${device_count} db_devices=${db_devices} db_locations=${db_locations}"

    if [ "${device_count}" -gt 0 ] || [ "${db_locations}" -gt 0 ]; then
      echo "trigger=condition-met"
      "${PROJECT_DIR}/scripts/verify-gps-module.sh"
      echo "finished=$(date -Is)"
      exit 0
    fi

    sleep "${SLEEP_SECONDS}"
  done

  echo "trigger=timeout-no-device-or-location"
  "${PROJECT_DIR}/scripts/verify-gps-module.sh"
  echo "finished=$(date -Is)"
} >> "${WATCH_LOG}" 2>&1
