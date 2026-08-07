import importlib.util
import json
from pathlib import Path


def load_generator():
    script = Path(__file__).resolve().parents[1] / "gerar-qr.py"
    spec = importlib.util.spec_from_file_location("gerar_qr", script)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def sample_config():
    return {
        "qrcodekey": "abc123",
        "mainappid": "10087",
        "eventreceivingcomponent": "com.hmdm.launcher.AdminReceiver",
        "launcherurl": "",
        "wifissid": "",
        "wifipassword": "",
        "wifisecuritytype": "",
        "mobileenrollment": "f",
        "encryptdevice": "f",
        "qrparameters": "",
        "adminextras": "",
        "pkg": "com.hmdm.launcher",
        "app_url": "https://mdm.example.com/files/hmdm.apk",
        "apkhash": "nYW9weTH4t1xwLjFoYzeenCTmdZA0-tNPXrIrMBNLf8=",
    }


def test_payload_uses_headwind_package_and_public_base_url():
    gerar_qr = load_generator()
    payload = gerar_qr.provisioning_payload(
        config=sample_config(),
        base_url="https://mdm.example.com",
        device_id="tablet-01",
        create=True,
        groups=["field"],
        use_id=None,
        timeout=1,
        apk_url_override=None,
    )
    assert payload["android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"] == (
        "com.hmdm.launcher/com.hmdm.launcher.AdminReceiver"
    )
    assert payload["android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"]["com.hmdm.BASE_URL"] == (
        "https://mdm.example.com"
    )
    assert payload["android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"]["com.hmdm.DEVICE_ID"] == "tablet-01"
    assert payload["android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"]["com.hmdm.CONFIG"] == "abc123"
    assert payload["android.app.extra.PROVISIONING_SKIP_ENCRYPTION"] is True
    assert "olmdm" not in json.dumps(payload).lower()


def test_admin_extras_and_qr_parameters_must_be_valid_json_fragments():
    gerar_qr = load_generator()
    config = sample_config()
    config["adminextras"] = '"com.hmdm.LOCK_POWER_MENU": true'
    config["qrparameters"] = '"android.app.extra.PROVISIONING_LOCALE": "pt_BR"'
    payload = gerar_qr.provisioning_payload(
        config=config,
        base_url="https://mdm.example.com",
        device_id=None,
        create=False,
        groups=[],
        use_id="serial",
        timeout=1,
        apk_url_override="https://mdm.example.com/files/current.apk",
    )
    assert payload["android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"]["com.hmdm.LOCK_POWER_MENU"] is True
    assert payload["android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"]["com.hmdm.DEVICE_ID_USE"] == "serial"
    assert payload["android.app.extra.PROVISIONING_LOCALE"] == "pt_BR"
    assert payload["android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"].endswith("current.apk")
