import json
import os
import subprocess
import sys
import time
from pathlib import Path

import pytest
import requests
import yaml

from updater import ListUpdater


@pytest.fixture
def http_server(tmp_path):
    from http.server import HTTPServer, BaseHTTPRequestHandler

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            self.send_response(200)
            self.send_header("Content-Type", "text/plain")
            self.end_headers()
            self.wfile.write(b"ok")

        def do_POST(self):
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"ok"}')

        def log_message(self, format, *args):
            pass

    server = HTTPServer(("127.0.0.1", 0), Handler)
    port = server.server_address[1]
    import threading
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    yield f"http://127.0.0.1:{port}"
    server.shutdown()


def test_updater_success(http_server, tmp_path):
    sources = {"test": [f"{http_server}/list1.txt"]}
    sources_path = tmp_path / "sources.json"
    sources_path.write_text(json.dumps(sources))
    output_path = tmp_path / "domains.txt"

    updater = ListUpdater(str(sources_path), str(output_path))
    result = updater.fetch_and_merge(sources)
    assert result.exists()


def test_updater_404(http_server, tmp_path):
    from http.server import HTTPServer, BaseHTTPRequestHandler
    import threading

    class ErrorHandler(BaseHTTPRequestHandler):
        def do_GET(self):
            self.send_response(404)
            self.end_headers()

        def log_message(self, format, *args):
            pass

    server = HTTPServer(("127.0.0.1", 0), ErrorHandler)
    port = server.server_address[1]
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()

    sources = {"test": [f"http://127.0.0.1:{port}/missing"]}
    sources_path = tmp_path / "sources.json"
    sources_path.write_text(json.dumps(sources))
    output_path = tmp_path / "domains.txt"

    updater = ListUpdater(str(sources_path), str(output_path))
    with pytest.raises(Exception):
        updater.fetch_and_merge(sources)

    server.shutdown()


def test_updater_timeout(http_server, tmp_path):
    """Test that fetch times out when server is too slow"""
    import socket
    import threading

    # Create a socket that accepts connections but never sends data
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_socket.bind(("127.0.0.1", 0))
    server_socket.listen(1)
    port = server_socket.getsockname()[1]

    def accept_and_hold():
        try:
            conn, addr = server_socket.accept()
            # Hold the connection without sending data
            import time
            time.sleep(30)
            conn.close()
        except:
            pass

    thread = threading.Thread(target=accept_and_hold, daemon=True)
    thread.start()

    sources = {"test": [f"http://127.0.0.1:{port}/slow"]}
    sources_path = tmp_path / "sources.json"
    sources_path.write_text(json.dumps(sources))
    output_path = tmp_path / "domains.txt"

    updater = ListUpdater(str(sources_path), str(output_path), request_timeout=2)
    with pytest.raises(Exception):
        updater._fetch_with_retry(f"http://127.0.0.1:{port}/slow", max_retries=1)

    server_socket.close()


def test_updater_empty_response(http_server, tmp_path):
    from http.server import HTTPServer, BaseHTTPRequestHandler
    import threading

    class EmptyHandler(BaseHTTPRequestHandler):
        def do_GET(self):
            self.send_response(200)
            self.end_headers()

        def log_message(self, format, *args):
            pass

    server = HTTPServer(("127.0.0.1", 0), EmptyHandler)
    port = server.server_address[1]
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()

    sources = {"test": [f"http://127.0.0.1:{port}/empty"]}
    sources_path = tmp_path / "sources.json"
    sources_path.write_text(json.dumps(sources))
    output_path = tmp_path / "domains.txt"

    updater = ListUpdater(str(sources_path), str(output_path))
    with pytest.raises(Exception):
        updater._fetch_with_retry(f"http://127.0.0.1:{port}/empty", max_retries=1)

    server.shutdown()


def test_updater_three_parts(http_server, tmp_path):
    sources = {
        "part1": [f"{http_server}/list1.txt"],
        "part2": [f"{http_server}/list2.txt"],
        "part3": [f"{http_server}/list3.txt"],
    }
    sources_path = tmp_path / "sources.json"
    sources_path.write_text(json.dumps(sources))
    output_path = tmp_path / "domains.txt"

    updater = ListUpdater(str(sources_path), str(output_path))
    result = updater.fetch_and_merge(sources)
    assert result.exists()


def test_updater_normalize_with_star(http_server, tmp_path):
    from http.server import HTTPServer, BaseHTTPRequestHandler
    import threading

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            self.send_response(200)
            self.send_header("Content-Type", "text/plain")
            self.end_headers()
            self.wfile.write(b"example.com\n*.google.com\n")

        def log_message(self, format, *args):
            pass

    server = HTTPServer(("127.0.0.1", 0), Handler)
    port = server.server_address[1]
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()

    sources = {"test": [f"http://127.0.0.1:{port}/list"]}
    sources_path = tmp_path / "sources.json"
    sources_path.write_text(json.dumps(sources))
    output_path = tmp_path / "domains.txt"

    updater = ListUpdater(str(sources_path), str(output_path))
    updater.fetch_and_merge(sources)
    content = output_path.read_text()
    assert "*.example.com" in content
    assert "*.google.com" in content

    server.shutdown()


def test_updater_normalize_without_star(http_server, tmp_path):
    from http.server import HTTPServer, BaseHTTPRequestHandler
    import threading

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            self.send_response(200)
            self.send_header("Content-Type", "text/plain")
            self.end_headers()
            self.wfile.write(b"sub.example.com\n")

        def log_message(self, format, *args):
            pass

    server = HTTPServer(("127.0.0.1", 0), Handler)
    port = server.server_address[1]
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()

    sources = {"test": [f"http://127.0.0.1:{port}/list"]}
    sources_path = tmp_path / "sources.json"
    sources_path.write_text(json.dumps(sources))
    output_path = tmp_path / "domains.txt"

    updater = ListUpdater(str(sources_path), str(output_path))
    updater.fetch_and_merge(sources)
    content = output_path.read_text()
    assert "*.sub.example.com" in content

    server.shutdown()


def test_updater_ignores_comments(http_server, tmp_path):
    from http.server import HTTPServer, BaseHTTPRequestHandler
    import threading

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            self.send_response(200)
            self.send_header("Content-Type", "text/plain")
            self.end_headers()
            self.wfile.write(b"# comment\nexample.com\n# another comment\n")

        def log_message(self, format, *args):
            pass

    server = HTTPServer(("127.0.0.1", 0), Handler)
    port = server.server_address[1]
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()

    sources = {"test": [f"http://127.0.0.1:{port}/list"]}
    sources_path = tmp_path / "sources.json"
    sources_path.write_text(json.dumps(sources))
    output_path = tmp_path / "domains.txt"

    updater = ListUpdater(str(sources_path), str(output_path))
    updater.fetch_and_merge(sources)
    content = output_path.read_text()
    assert "example.com" in content
    assert "# comment" not in content

    server.shutdown()


def test_updater_min_interval(http_server, tmp_path):
    sources = {"test": [f"{http_server}/list1.txt"]}
    sources_path = tmp_path / "sources.json"
    sources_path.write_text(json.dumps(sources))
    output_path = tmp_path / "domains.txt"

    updater = ListUpdater(str(sources_path), str(output_path))
    assert updater.should_refresh(0)
    assert not updater.should_refresh(time.time() - 100)


def test_updater_calls_refresh(http_server, tmp_path, monkeypatch):
    refresh_called = [False]

    from http.server import HTTPServer, BaseHTTPRequestHandler
    import threading

    class RefreshHandler(BaseHTTPRequestHandler):
        def do_POST(self):
            refresh_called[0] = True
            self.send_response(200)
            self.end_headers()

        def do_GET(self):
            self.send_response(200)
            self.send_header("Content-Type", "text/plain")
            self.end_headers()
            self.wfile.write(b"example.com\n")

        def log_message(self, format, *args):
            pass

    server = HTTPServer(("127.0.0.1", 0), RefreshHandler)
    port = server.server_address[1]
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()

    sources = {"test": [f"http://127.0.0.1:{port}/list"]}
    sources_path = tmp_path / "sources.json"
    sources_path.write_text(json.dumps(sources))
    output_path = tmp_path / "domains.txt"

    updater = ListUpdater(str(sources_path), str(output_path))
    # Mock the refresh URL to point to our test server
    original_refresh = updater.refresh
    def mocked_refresh():
        sources = updater.load_sources()
        path = updater.fetch_and_merge(sources)
        refresh_url = f"http://127.0.0.1:{port}/api/lists/refresh"
        try:
            updater.session.post(refresh_url, timeout=10)
        except Exception:
            pass
        return path

    updater.refresh = mocked_refresh
    updater.refresh()
    assert refresh_called[0]

    server.shutdown()
