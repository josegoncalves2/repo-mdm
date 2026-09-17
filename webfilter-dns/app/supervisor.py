import hashlib
import os
import subprocess
import time
from pathlib import Path
from typing import Dict, Optional

import yaml


class Supervisor:
    def __init__(self, config_path: str, blocky_binary: str = "blocky", log_callback=None):
        self.config_path = Path(config_path)
        self.blocky_binary = blocky_binary
        self.log_callback = log_callback or (lambda msg: None)
        self.process: Optional[subprocess.Popen] = None
        self.last_valid_config: Optional[str] = None
        self.last_valid_hash: Optional[str] = None

    def _hash_file(self, path: Path) -> str:
        h = hashlib.sha256()
        if path.exists():
            h.update(path.read_bytes())
        return h.hexdigest()

    def _is_healthy(self) -> bool:
        if self.process is None:
            return False
        if self.process.poll() is not None:
            return False
        try:
            result = subprocess.run(
                [self.blocky_binary, "version"],
                capture_output=True,
                timeout=10,
            )
            return result.returncode == 0
        except Exception:
            return False

    def start(self, config_path: Optional[str] = None):
        cfg = config_path or str(self.config_path)
        self.log_callback(f"Starting blocky with config: {cfg}")
        self.process = subprocess.Popen(
            [self.blocky_binary, "-c", cfg],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        time.sleep(2)
        if not self._is_healthy():
            self.log_callback("Blocky failed to start, using last valid config")
            self._restart_with_last_valid()

    def _restart_with_last_valid(self):
        if self.last_valid_config and Path(self.last_valid_config).exists():
            self.log_callback(f"Restarting with last valid config: {self.last_valid_config}")
            self.stop()
            self.process = subprocess.Popen(
                [self.blocky_binary, "-c", self.last_valid_config],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            )
            time.sleep(2)

    def stop(self):
        if self.process and self.process.poll() is None:
            self.process.terminate()
            try:
                self.process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                self.process.kill()

    def on_config_change(self, config_path: str):
        current_hash = self._hash_file(Path(config_path))
        if current_hash != self.last_valid_hash:
            self.log_callback(f"Config changed: {config_path}, restarting Blocky")
            # Restart Blocky with the new config
            self.stop()
            self.last_valid_config = config_path
            self.last_valid_hash = current_hash
            self.process = subprocess.Popen(
                [self.blocky_binary, "-c", config_path],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            )
            time.sleep(2)
            if not self._is_healthy():
                self.log_callback("New config invalid, rolling back to last valid config")
                self._restart_with_last_valid()

    def on_profile_list_change(self, config_path: str):
        self.log_callback(f"Profile list changed, refreshing lists via Blocky API")
        try:
            subprocess.run(
                ["curl", "-s", "-X", "POST", "http://localhost:4000/api/lists/refresh"],
                timeout=30,
            )
        except Exception as e:
            self.log_callback(f"Refresh failed: {e}")

    def run(self, config_path: str, watch_interval: int = 60):
        self.start(config_path)
        last_check = time.time()
        while True:
            time.sleep(5)
            if not self._is_healthy():
                self.log_callback("Blocky unhealthy, restarting")
                self._restart_with_last_valid()

            if time.time() - last_check >= watch_interval:
                last_check = time.time()
                current_hash = self._hash_file(self.config_path)
                if current_hash != self.last_valid_hash:
                    self.on_config_change(str(self.config_path))
