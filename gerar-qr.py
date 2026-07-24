#!/usr/bin/env python3
"""
Generate a Headwind MDM Android Enterprise enrollment QR payload.

This script intentionally reads the running HWMDM PostgreSQL container instead
of storing database credentials in source files. It can also emit JSON only when
the optional Python QR dependency is unavailable.
"""

import argparse
import base64
import hashlib
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Dict, List, Optional


PSQL_CONTAINER = os.environ.get("HWMDM_POSTGRES_CONTAINER", "hwmdm-postgresql-1")
PSQL_USER = os.environ.get("HWMDM_POSTGRES_USER", "hmdm")
PSQL_DB = os.environ.get("HWMDM_POSTGRES_DB", "hmdm")


class EnrollmentError(RuntimeError):
    pass


def run_psql(sql: str) -> str:
    cmd = [
        "docker",
        "exec",
        PSQL_CONTAINER,
        "psql",
        "-U",
        PSQL_USER,
        "-d",
        PSQL_DB,
        "-AtF",
        "\t",
        "-c",
        sql,
    ]
    try:
        completed = subprocess.run(cmd, check=True, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    except subprocess.CalledProcessError as exc:
        raise EnrollmentError(f"Falha ao consultar PostgreSQL do HWMDM: {exc.stderr.strip()}") from exc
    return completed.stdout.strip()


def first_row(sql: str) -> List[str]:
    output = run_psql(sql)
    if not output:
        raise EnrollmentError("Nenhum registro encontrado para a configuração informada.")
    return output.splitlines()[0].split("\t")


def sql_escape(value: str) -> str:
    return value.replace("'", "''")


def get_configuration(config_key: str) -> Dict[str, str]:
    row = first_row(
        "select c.qrcodekey, c.mainappid, c.eventreceivingcomponent, coalesce(c.launcherurl,''), "
        "coalesce(c.wifissid,''), coalesce(c.wifipassword,''), coalesce(c.wifisecuritytype,''), "
        "c.mobileenrollment, c.encryptdevice, coalesce(c.qrparameters,''), coalesce(c.adminextras,''), "
        "a.pkg, av.url, coalesce(av.apkhash,'') "
        "from configurations c "
        "join applicationversions av on av.id = c.mainappid "
        "join applications a on a.id = av.applicationid "
        f"where c.qrcodekey = '{sql_escape(config_key)}'"
    )
    return {
        "qrcodekey": row[0],
        "mainappid": row[1],
        "eventreceivingcomponent": row[2],
        "launcherurl": row[3],
        "wifissid": row[4],
        "wifipassword": row[5],
        "wifisecuritytype": row[6],
        "mobileenrollment": row[7],
        "encryptdevice": row[8],
        "qrparameters": row[9],
        "adminextras": row[10],
        "pkg": row[11],
        "app_url": row[12],
        "apkhash": row[13],
    }


def sha256_urlsafe_base64(url: str, timeout: int) -> str:
    digest = hashlib.sha256()
    with urllib.request.urlopen(url, timeout=timeout) as response:
        while True:
            chunk = response.read(1024 * 1024)
            if not chunk:
                break
            digest.update(chunk)
    return base64.urlsafe_b64encode(digest.digest()).decode("ascii").rstrip("=")


def validate_url(url: str, timeout: int) -> None:
    request = urllib.request.Request(url, method="GET", headers={"User-Agent": "hwmdm-enrollment-check/1.0"})
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            status = getattr(response, "status", 200)
            if status < 200 or status >= 400:
                raise EnrollmentError(f"URL retornou HTTP {status}: {url}")
    except urllib.error.URLError as exc:
        raise EnrollmentError(f"Falha ao acessar URL de enrollment ({url}): {exc.reason}") from exc


def extras_bundle(config: Dict[str, str], base_url: str, device_id: Optional[str], create: bool,
                  groups: List[str], use_id: Optional[str]) -> Dict[str, str]:
    extras: Dict[str, str] = {
        "com.hmdm.BASE_URL": base_url.rstrip("/"),
        "com.hmdm.SERVER_PROJECT": "",
    }
    if device_id:
        extras["com.hmdm.DEVICE_ID"] = device_id.strip()
    if create:
        extras["com.hmdm.CONFIG"] = config["qrcodekey"]
    if groups:
        extras["com.hmdm.GROUP"] = ",".join(groups)
    if use_id:
        extras["com.hmdm.DEVICE_ID_USE"] = use_id
    if config["adminextras"].strip():
        try:
            extras.update(json.loads("{" + config["adminextras"].strip().strip(",") + "}"))
        except json.JSONDecodeError as exc:
            raise EnrollmentError(f"adminExtras da configuração não é JSON válido: {exc}") from exc
    return extras


def provisioning_payload(config: Dict[str, str], base_url: str, device_id: Optional[str], create: bool,
                         groups: List[str], use_id: Optional[str], timeout: int,
                         apk_url_override: Optional[str]) -> Dict[str, object]:
    apk_url = (apk_url_override or config["launcherurl"] or config["app_url"]).replace(" ", "%20")
    apk_hash = config["apkhash"] or sha256_urlsafe_base64(apk_url, timeout)
    payload: Dict[str, object] = {
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME":
            f"{config['pkg']}/{config['eventreceivingcomponent']}",
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": apk_url,
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": apk_hash,
        "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": True,
        "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE":
            extras_bundle(config, base_url, device_id, create, groups, use_id),
    }
    if config["wifissid"].strip():
        payload["android.app.extra.PROVISIONING_WIFI_SSID"] = config["wifissid"].strip()
        payload["android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE"] = config["wifisecuritytype"].strip() or "WPA"
    if config["wifipassword"].strip():
        payload["android.app.extra.PROVISIONING_WIFI_PASSWORD"] = config["wifipassword"].strip()
    if config["mobileenrollment"] == "t":
        payload["android.app.extra.PROVISIONING_USE_MOBILE_DATA"] = True
    if config["encryptdevice"] == "f":
        payload["android.app.extra.PROVISIONING_SKIP_ENCRYPTION"] = True
    if config["qrparameters"].strip():
        try:
            payload.update(json.loads("{" + config["qrparameters"].strip().strip(",") + "}"))
        except json.JSONDecodeError as exc:
            raise EnrollmentError(f"qrParameters da configuração não é JSON válido: {exc}") from exc
    return payload


def write_qr_png(payload: Dict[str, object], output: Path) -> None:
    try:
        import qrcode
    except ImportError as exc:
        raise EnrollmentError("Pacote Python 'qrcode' não está instalado; JSON foi gerado, PNG não.") from exc
    image = qrcode.make(json.dumps(payload, ensure_ascii=False, separators=(",", ":")))
    image.save(output)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Gera QR de enrollment Android Enterprise para HWMDM.")
    parser.add_argument("--config-key", required=True, help="Chave QR da configuração HWMDM.")
    parser.add_argument("--base-url", default=os.environ.get("HWMDM_BASE_URL", "https://mdm.example.com"),
                        help="URL pública do painel usada em com.hmdm.BASE_URL.")
    parser.add_argument("--apk-url", help="Override da URL pública do APK no QR.")
    parser.add_argument("--device-id", help="ID de device a embutir no QR.")
    parser.add_argument("--create", action="store_true", help="Criar device sob demanda usando a configuração.")
    parser.add_argument("--group", action="append", default=[], help="Grupo a atribuir; pode repetir.")
    parser.add_argument("--use-id", choices=["imei", "serial", "mac", "user", "suggest"], help="Fonte de ID automática.")
    parser.add_argument("--json-out", default="enrollment-qr.json", help="Arquivo JSON de saída.")
    parser.add_argument("--png-out", default="enrollment-qr.png", help="Arquivo PNG de saída.")
    parser.add_argument("--no-png", action="store_true", help="Não gerar PNG, somente JSON.")
    parser.add_argument("--validate", action="store_true", help="Valida acesso real à URL base e APK.")
    parser.add_argument("--timeout", type=int, default=20, help="Timeout de rede em segundos.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        config = get_configuration(args.config_key)
        payload = provisioning_payload(
            config=config,
            base_url=args.base_url,
            device_id=args.device_id,
            create=args.create,
            groups=args.group,
            use_id=args.use_id,
            timeout=args.timeout,
            apk_url_override=args.apk_url,
        )
        json_path = Path(args.json_out)
        json_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        if args.validate:
            validate_url(args.base_url.rstrip("/") + "/#/login", args.timeout)
            validate_url(str(payload["android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"]), args.timeout)
        if not args.no_png:
            write_qr_png(payload, Path(args.png_out))
        print(f"JSON gerado: {json_path}")
        if not args.no_png:
            print(f"PNG gerado: {Path(args.png_out)}")
        print("DPC:", payload["android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"])
        print("APK:", payload["android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"])
        return 0
    except EnrollmentError as exc:
        print(f"ERRO: {exc}", file=sys.stderr)
        return 2
    except Exception as exc:
        print(f"ERRO inesperado: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
