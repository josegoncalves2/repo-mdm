import hashlib
import json
import os
import time
from pathlib import Path
from typing import Dict, List, Optional, Set

import requests
import yaml


class ListUpdater:
    DEFAULT_MIN_INTERVAL = 3600
    DEFAULT_MAX_INTERVAL = 86400

    def __init__(self, sources_path: str, output_path: str, http_session: Optional[requests.Session] = None, request_timeout: int = 30):
        self.sources_path = Path(sources_path)
        self.output_path = Path(output_path)
        self.session = http_session or requests.Session()
        self.request_timeout = request_timeout
        self.last_successful_hash: Optional[str] = None
        self.last_successful_path: Optional[Path] = None

    def load_sources(self) -> Dict[str, List[str]]:
        with open(self.sources_path, "r", encoding="utf-8") as f:
            return json.load(f)

    @staticmethod
    def normalize_entry(entry: str) -> Optional[str]:
        entry = entry.strip()
        if not entry or entry.startswith("#"):
            return None
        if entry.startswith("*."):
            return entry.lower()
        if entry.startswith("."):
            return "*" + entry.lower()
        if entry.startswith("http://") or entry.startswith("https://"):
            from urllib.parse import urlparse
            parsed = urlparse(entry)
            host = parsed.hostname or ""
            if host:
                return "*." + host.lower()
        if "/" in entry:
            entry = entry.split("/", 1)[0]
        if "." in entry and not entry.startswith("-"):
            return "*." + entry.lower()
        return None

    @staticmethod
    def _sha256(content: bytes) -> str:
        return hashlib.sha256(content).hexdigest()

    def fetch_and_merge(self, sources: Optional[Dict[str, List[str]]] = None) -> Path:
        if sources is None:
            sources = self.load_sources()
        all_entries: Set[str] = set()
        for source_type, urls in sources.items():
            for url in urls:
                content = self._fetch_with_retry(url)
                for line in content.decode("utf-8", errors="replace").splitlines():
                    normalized = self.normalize_entry(line)
                    if normalized:
                        all_entries.add(normalized)

        output = "\n".join(sorted(all_entries)) + "\n"
        new_hash = self._sha256(output.encode("utf-8"))

        if self.last_successful_hash and new_hash == self.last_successful_hash:
            return self.last_successful_path or self.output_path

        tmp_path = self.output_path.with_suffix(self.output_path.suffix + ".tmp")
        tmp_path.write_bytes(output.encode("utf-8"))
        os.replace(tmp_path, self.output_path)

        self.last_successful_hash = new_hash
        self.last_successful_path = self.output_path
        return self.output_path

    def _fetch_with_retry(self, url: str, max_retries: int = 3) -> bytes:
        import time as _time
        for attempt in range(max_retries):
            try:
                resp = self.session.get(url, timeout=self.request_timeout)
                resp.raise_for_status()
                if not resp.content.strip():
                    raise ValueError(f"Empty response from {url}")
                return resp.content
            except Exception as e:
                if attempt == max_retries - 1:
                    raise
                _time.sleep(2 ** attempt)

    def should_refresh(self, last_refresh: float, sources: Optional[Dict[str, List[str]]] = None) -> bool:
        if sources is None:
            sources = self.load_sources()
        total_urls = sum(len(urls) for urls in sources.values())
        min_interval = self.DEFAULT_MIN_INTERVAL
        max_interval = self.DEFAULT_MAX_INTERVAL
        interval = min(max_interval, max(min_interval, 3600 * total_urls // max(total_urls, 1)))
        return (time.time() - last_refresh) >= interval

    def refresh(self):
        sources = self.load_sources()
        path = self.fetch_and_merge(sources)
        refresh_url = "http://localhost:4000/api/lists/refresh"
        try:
            self.session.post(refresh_url, timeout=10)
        except Exception:
            pass
        return path
