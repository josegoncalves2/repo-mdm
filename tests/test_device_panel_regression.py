from pathlib import Path


def test_device_profile_permissions_exist():
    seed = Path("c:/REPOSITORIOS/mdm/repo-mdm/source/sql/hwmdm_rbac_seed.sql").read_text(encoding="utf-8")
    assert "('device.profile.view'" in seed
    assert "('device.profile.edit'" in seed


def test_device_table_no_longer_uses_fixed_width_and_single_line_actions():
    css = Path("c:/REPOSITORIOS/mdm/repo-mdm/server-source/server/src/main/webapp/css/main.css").read_text(encoding="utf-8")
    assert "max-width: 1000px" not in css
    assert "white-space: nowrap" not in css
