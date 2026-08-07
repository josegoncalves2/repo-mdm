from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(relative_path: str) -> str:
    return (ROOT / relative_path).read_text(encoding="utf-8")


def test_gps_map_has_configurable_real_time_refresh():
    controller = read("server-source/server/src/main/webapp/app/components/main/controller/gpsmap.controller.js")

    assert "DEFAULT_REFRESH_INTERVAL_MS = 30000" in controller
    assert "{value: 30000" in controller
    assert "{value: 60000" in controller
    assert "REFRESH_INTERVAL_STORAGE_KEY" in controller
    assert "$window.localStorage.setItem" in controller
    assert "if ($scope.loading)" in controller
    assert "summaryService.getDeviceLocations" in controller
    assert "scheduleRefresh();" in controller


def test_gps_map_view_is_localized_and_selects_devices():
    view = read("server-source/server/src/main/webapp/app/components/main/view/gpsmap.html")

    assert "gps-refresh-select" in view
    assert "gpsmap.title" in view
    assert "gpsmap.auto.refresh" in view
    assert "selectDevice(device)" in view
    assert "No device has reported" not in view
    assert "Maps / GPS</span>" not in view


def test_map_service_safely_handles_empty_bounds_and_marker_popup():
    service = read("server-source/server/src/main/webapp/app/shared/service/map.service.js")

    assert "if (!points || !points.length)" in service
    assert "openMarkerPopup" in service
    assert "markers[identifier].openPopup()" in service


def test_gps_localization_has_refresh_labels():
    localization = read("server-source/server/src/main/webapp/localization/hwmdm_modules.js")

    assert "'gpsmap.refresh.30s': 'Refresh every 30s'" in localization
    assert "'gpsmap.refresh.60s': 'Refresh every 1 min'" in localization
    assert "'gpsmap.refresh.30s': 'Atualizar a cada 30s'" in localization
    assert "'gpsmap.refresh.off': 'Atualização automática desligada'" in localization
