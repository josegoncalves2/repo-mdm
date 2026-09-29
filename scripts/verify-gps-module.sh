#!/usr/bin/env bash
set -u

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DIR="${PROJECT_DIR}/artifacts/validation"
REPORT_FILE="${REPORT_DIR}/gps-module-debug-latest.log"
BASE_URL="${HWMDM_BASE_URL:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"

mkdir -p "${REPORT_DIR}"

{
  echo "# HWMDM GPS module validation"
  date -Is
  echo

  echo "## Python tests"
  cd "${PROJECT_DIR}" && python3 -m pytest -q tests/test_gps_map_module.py tests/test_enrollment_qr.py
  echo

  echo "## JavaScript syntax"
  cd "${PROJECT_DIR}" && node --check server-source/server/src/main/webapp/app/components/main/controller/gpsmap.controller.js
  cd "${PROJECT_DIR}" && node --check server-source/server/src/main/webapp/app/shared/service/map.service.js
  cd "${PROJECT_DIR}" && node --check server-source/server/src/main/webapp/localization/hwmdm_modules.js
  echo

  echo "## Served asset checks"
  cd "${PROJECT_DIR}" && python3 - <<PY
import urllib.request, urllib.error
base = "${BASE_URL}/"
checks = {
    "index.html": [
        "gpsmap.controller.js?v=hwmdm-20260723-2135",
        "map.service.js?v=hwmdm-20260723-2135",
        "css/main.css?v=hwmdm-20260723-2135",
    ],
    "app/components/main/controller/gpsmap.controller.js?v=hwmdm-20260723-2135": [
        "REFRESH_INTERVAL_STORAGE_KEY",
        "DEFAULT_REFRESH_INTERVAL_MS = 30000",
        "summaryService.getDeviceLocations",
        "openMarkerPopup",
    ],
    "app/components/main/view/gpsmap.html": [
        "gps-refresh-select",
        "gpsmap.auto.refresh",
        "selectDevice(device)",
    ],
    "app/shared/service/map.service.js?v=hwmdm-20260723-2135": [
        "openMarkerPopup",
        "if (!points || !points.length)",
    ],
    "localization/hwmdm_modules.js?v=hwmdm-20260723-2135": [
        "gpsmap.refresh.30s",
        "Atualizar a cada 30s",
    ],
    "css/main.css?v=hwmdm-20260723-2135": [
        "gps-refresh-select",
        "glyphicon-spin",
    ],
}
for path, needles in checks.items():
    with urllib.request.urlopen(base + path, timeout=15) as response:
        text = response.read().decode("utf-8", "replace")
    missing = [needle for needle in needles if needle not in text]
    print(path, "OK" if not missing else "MISSING " + repr(missing))
try:
    urllib.request.urlopen(base + "rest/private/summary/locations", timeout=15)
except urllib.error.HTTPError as error:
    print("rest/private/summary/locations", "HTTP", error.code)
PY
  echo

  echo "## Playwright smoke"
  cd "${PROJECT_DIR}" && python3 - <<PY
from playwright.sync_api import sync_playwright
url = "${BASE_URL}/#/gpsMap"
with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    console = []
    failed = []
    page.on("console", lambda msg: console.append((msg.type, msg.text)))
    page.on("requestfailed", lambda req: failed.append((req.url, req.failure.error_text if req.failure else "failed")))
    page.goto(url, wait_until="networkidle", timeout=30000)
    html = page.content()
    print("title=" + page.title())
    print("has_login=" + str("Login" in html or "username" in html.lower()))
    print("failed_requests=" + repr(failed))
    print("console_errors=" + repr([c for c in console if c[0] in ("error", "warning")]))
    browser.close()
PY
  echo

  echo "## Database GPS status"
  docker exec hwmdm-postgres psql -U hmdm -d hmdm -c "select count(*) as devices_total from devices;"
  docker exec hwmdm-postgres psql -U hmdm -d hmdm -c "select count(*) as devices_with_location from devices where info is not null and info::jsonb ? 'location';"
  echo

  echo "## ADB devices"
  adb devices -l || true
  echo

  echo "## Recent HWMDM server errors"
  docker logs --since 10m hwmdm-mdm 2>&1 | rg 'ERROR|SEVERE|Exception|gpsmap|summary/locations' || true
} 2>&1 | tee "${REPORT_FILE}"

echo
echo "Debug report: ${REPORT_FILE}"
