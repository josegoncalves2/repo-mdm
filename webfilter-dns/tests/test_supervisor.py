import json
import os
import subprocess
import sys
from pathlib import Path
from unittest.mock import Mock, patch

import pytest
import requests
import yaml

from supervisor import Supervisor


@pytest.fixture
def blocky_config(tmp_path):
    config = {
        "dns": {
            "listen": ["127.0.0.1:853"],
            "bootstrap": ["1.1.1.1:853"],
        },
        "upstream": ["https://dns.google/dns-query"],
        "clientGroupsBlock": {},
    }
    config_path = tmp_path / "blocky.yml"
    config_path.write_text(yaml.dump(config))
    return str(config_path)


def test_supervisor_start(blocky_config, tmp_path):
    log = []
    sup = Supervisor(blocky_config, log_callback=lambda msg: log.append(msg))
    assert sup.process is None

    # Mock subprocess.Popen
    with patch('supervisor.subprocess.Popen') as mock_popen:
        mock_process = Mock()
        mock_process.poll.return_value = None
        mock_popen.return_value = mock_process

        with patch('supervisor.subprocess.run') as mock_run:
            mock_run.return_value = Mock(returncode=0)

            sup.start()
            assert sup.process is not None
            sup.stop()


def test_supervisor_config_change(blocky_config, tmp_path):
    log = []
    sup = Supervisor(blocky_config, log_callback=lambda msg: log.append(msg))

    with patch('supervisor.subprocess.Popen') as mock_popen:
        mock_process = Mock()
        mock_process.poll.return_value = None
        mock_popen.return_value = mock_process

        with patch('supervisor.subprocess.run') as mock_run:
            mock_run.return_value = Mock(returncode=0)

            sup.start()

            new_config = blocky_config
            sup.on_config_change(new_config)
            assert any("changed" in msg for msg in log)
            sup.stop()


def test_supervisor_invalid_config(tmp_path):
    bad_config = tmp_path / "blocky.yml"
    bad_config.write_text("invalid: yaml: content: [")

    log = []
    sup = Supervisor(str(bad_config), log_callback=lambda msg: log.append(msg))

    with patch('supervisor.subprocess.Popen') as mock_popen:
        mock_process = Mock()
        mock_process.poll.return_value = 1  # Simulate failure
        mock_popen.return_value = mock_process

        with patch('supervisor.subprocess.run') as mock_run:
            mock_run.return_value = Mock(returncode=1)  # Simulate health check failure

            sup.start()
            assert any("unhealthy" in msg.lower() or "fail" in msg.lower() for msg in log)
            sup.stop()


def test_supervisor_profile_list_change(blocky_config, tmp_path):
    log = []
    sup = Supervisor(blocky_config, log_callback=lambda msg: log.append(msg))

    with patch('supervisor.subprocess.Popen') as mock_popen:
        mock_process = Mock()
        mock_process.poll.return_value = None
        mock_popen.return_value = mock_process

        with patch('supervisor.subprocess.run') as mock_run:
            mock_run.return_value = Mock(returncode=0)

            sup.start()
            sup.on_profile_list_change(str(blocky_config))
            assert any("refresh" in msg.lower() for msg in log)
            sup.stop()
