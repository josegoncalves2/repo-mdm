import os
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "source"
RUNTIME_ENV = ROOT / ".runtime" / "hwmdm.compose.env"


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def iter_project_files():
    skipped_parts = {
        ".git",
        ".runtime",
        "node_modules",
        "target",
        "webtarget",
        "volumes",
        "db",
        "backups",
        "test-results",
        "__pycache__",
    }
    for path in ROOT.rglob("*"):
        if not path.is_file():
            continue
        if any(part in skipped_parts for part in path.parts):
            continue
        if path.name == "prompt.md":
            continue
        yield path


def test_versioned_files_do_not_contain_runtime_secrets():
    forbidden = [
        "ghp" + "_",
        "Kwe" + "88",
        "Senha" + "Forte",
        "ChangeMe" + "_Secure",
        "TOKEN" + " PARA",
        "USUARIO" + " GITHUB",
    ]
    offenders = []
    for path in iter_project_files():
        text = read_text(path)
        for marker in forbidden:
            if marker in text:
                offenders.append(f"{path.relative_to(ROOT)} contains {marker}")
    assert offenders == []


def test_product_files_do_not_use_abandoned_brand_name():
    old_brand_markers = ["ol" + "mdm", "OL" + " MDM", "OL" + "MDM"]
    offenders = []
    for path in iter_project_files():
        text = read_text(path)
        lowered = text.lower()
        if old_brand_markers[0] in lowered:
            offenders.append(str(path.relative_to(ROOT)))
        for marker in old_brand_markers[1:]:
            if marker in text:
                offenders.append(str(path.relative_to(ROOT)))
    assert sorted(set(offenders)) == []


def test_compose_uses_external_tls_proxy_contract():
    compose = read_text(SOURCE / "docker-compose.yaml")
    server_xml = read_text(SOURCE / "tomcat_conf" / "server.xml")
    assert "8080:8080" in compose
    assert "31000:31000" in compose
    assert "443:8443" not in compose
    assert "certbot:" not in compose
    assert "certificateKeystoreFile" not in server_xml


def test_compose_is_reproducible_with_runtime_env():
    assert RUNTIME_ENV.exists()
    assert oct(RUNTIME_ENV.stat().st_mode & 0o777) == "0o600"
    result = subprocess.run(
        [
            "docker",
            "compose",
            "-p",
            "hwmdm",
            "--env-file",
            str(RUNTIME_ENV),
            "config",
        ],
        cwd=SOURCE,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    config = result.stdout
    assert "PUBLIC_PROTOCOL: https" in config
    assert "PROTOCOL: http" in config
    assert 'published: "8080"' in config
    assert 'published: "31000"' in config
    assert 'published: "443"' not in config


def run_psql(sql: str) -> str:
    container_result = subprocess.run(
        [
            "docker",
            "ps",
            "--filter",
            "label=com.docker.compose.project=hwmdm",
            "--filter",
            "label=com.docker.compose.service=postgresql",
            "--format",
            "{{.Names}}",
        ],
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    container = container_result.stdout.strip().splitlines()[0]
    result = subprocess.run(
        [
            "docker",
            "exec",
            "-i",
            container,
            "sh",
            "-c",
            (
                'PGPASSWORD="$POSTGRES_PASSWORD" '
                'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" '
                "-t -A -F '|'"
            ),
        ],
        check=True,
        text=True,
        input=sql,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return result.stdout.strip()


def test_live_rbac_roles_and_granular_permissions_are_seeded():
    expected_permissions = {
        "device.remote_access.view",
        "device.remote_access.control",
        "device.gps.view",
        "device.kiosk.edit",
        "device.lifecycle.lock",
        "device.lifecycle.unlock",
        "device.lifecycle.reboot",
        "device.lifecycle.wipe",
        "device.lifecycle.reset",
        "device.logs.view",
        "device.photos.upload",
        "device.network.filter",
        "device.ldap.sync",
        "device.export_import",
        "device.contacts.manage",
        "device.branding.edit",
    }
    permissions = set(
        run_psql("select name from permissions where name like 'device.%' order by name;").splitlines()
    )
    assert expected_permissions <= permissions

    role_counts = dict(
        line.split("|", 1)
        for line in run_psql(
            """
            select r.name, count(urp.permissionid)
            from userroles r
            left join userrolepermissions urp on urp.roleid = r.id
            where r.name in ('Admin', 'Helpdesk 1', 'Helpdesk 2', 'Helpdesk 3', 'Guest')
            group by r.name
            order by r.name;
            """
        ).splitlines()
    )
    assert set(role_counts) == {"Admin", "Helpdesk 1", "Helpdesk 2", "Helpdesk 3", "Guest"}
    assert int(role_counts["Admin"]) >= len(expected_permissions)
    assert int(role_counts["Helpdesk 1"]) == 6
    assert int(role_counts["Helpdesk 2"]) == 7
    assert int(role_counts["Helpdesk 3"]) == 9
    assert int(role_counts["Guest"]) == 0
