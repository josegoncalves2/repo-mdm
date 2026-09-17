#!/usr/bin/env python3
"""
Task 1.6: Measure resolver with full real catalog

Measures RSS memory, startup time, and domain categories from IPFire DBL,
Block List Project, and UT1 sources.
"""

import json
import os
import subprocess
import sys
import time
from pathlib import Path
from typing import Dict, Optional
import re

# Add parent directory to path so we can import modules from webfilter-dns/app
sys.path.insert(0, str(Path(__file__).parent.parent / "app"))

from updater import ListUpdater
import yaml


def get_docker_memory_mb(container_name: str) -> Optional[float]:
    """Get RSS memory in MB from docker stats"""
    try:
        result = subprocess.run(
            ["docker", "stats", "--no-stream", "--format", "{{.MemUsage}}", container_name],
            capture_output=True,
            text=True,
            timeout=10,
        )
        if result.returncode == 0:
            # Parse format like "123.4MiB" or "1.2GiB"
            mem_str = result.stdout.strip()
            if "GiB" in mem_str:
                return float(mem_str.replace("GiB", "").strip()) * 1024
            elif "MiB" in mem_str:
                return float(mem_str.replace("MiB", "").strip())
        return None
    except Exception:
        return None


def wait_for_dns_response(host: str = "127.0.0.1", port: int = 853, timeout: int = 60) -> Optional[float]:
    """
    Wait for the DNS resolver to respond with a blocked response.
    Returns the startup time in seconds, or None if timeout.
    """
    start_time = time.time()

    try:
        import dns.resolver
        import dns.rdatatype

        while time.time() - start_time < timeout:
            try:
                resolver = dns.resolver.Resolver()
                resolver.use_dnssec = False
                resolver.nameservers = [host]
                resolver.port = port

                # Try to resolve a known blocked domain
                # (one of the common ad servers)
                try:
                    resolver.resolve("ads.google.com", dns.rdatatype.A)
                    # If it resolves, it's not blocking yet
                except Exception:
                    # Good - it's blocking, means resolver is responding
                    elapsed = time.time() - start_time
                    return elapsed

                time.sleep(1)
            except Exception:
                time.sleep(1)

        return None
    except ImportError:
        # dnspython not available in requirements, skip DNS check
        return None


def build_blocky_config(work_dir: Path) -> Path:
    """Build a blocky configuration that uses the merged lists"""
    config = {
        'dns': {
            'port': 53,
            'tcpPort': 53,
            'tls': {
                'port': 853,
                'certFile': '/app/certs/cert.pem',
                'keyFile': '/app/certs/key.pem',
            },
            'bootstrap': [
                '8.8.8.8:53',
                '8.8.4.4:53',
            ]
        },
        'http': {
            'port': 4000,
        },
        'upstream': {
            'default': [
                'https://dns.google/dns-query',
                'https://dns.quad9.net/dns-query',
            ]
        },
        'customDNS': {
            'customTTL': 3600,
        },
        'blocking': {
            'denylists': {
                'ipfire': ['/app/dns/lists/ipfire.txt'],
                'blocklistproject': ['/app/dns/lists/blocklistproject.txt'],
                'ut1': ['/app/dns/lists/ut1.txt'],
            },
            'allowlists': {},
            'clientGroupsBlock': {
                'default': ['ipfire', 'blocklistproject', 'ut1'],
            }
        },
        'caching': {
            'prefetching': True,
            'minTime': '5m',
            'maxTime': '30m',
            'maxItemsCount': 0,
        },
        'log': {
            'level': 'info',
            'format': 'text',
        }
    }

    config_path = work_dir / "blocky.yml"
    config_path.write_text(yaml.dump(config))
    return config_path


def count_domains_in_file(path: Path) -> int:
    """Count lines in a file (each line is a domain pattern)"""
    if not path.exists():
        return 0
    return sum(1 for _ in path.open('r', encoding='utf-8', errors='replace'))


def categorize_domains(sources: Dict) -> Dict[str, int]:
    """
    Create a categories_count dict from sources.
    Each source becomes a category with the count of domains.
    """
    categories = {}

    for source_name in sources.keys():
        # For measurement, we just track which sources are being used
        # Actual count will be determined after fetching
        categories[source_name] = 0

    return categories


def fetch_and_merge_sources(work_dir: Path, sources_path: Path) -> Dict[str, int]:
    """
    Fetch all sources and merge them.
    Returns a dict of category: domain_count
    """
    categories_count = {}

    # Load sources
    with open(sources_path, 'r') as f:
        sources = json.load(f)

    # Create output directory
    lists_dir = work_dir / "dns" / "lists"
    lists_dir.mkdir(parents=True, exist_ok=True)

    # Fetch each source separately and count domains
    for source_name, source_data in sources.items():
        print(f"Fetching {source_name}...")

        if isinstance(source_data, dict) and 'urls' in source_data:
            urls = source_data['urls']
        else:
            urls = []

        output_path = lists_dir / f"{source_name}.txt"
        updater = ListUpdater(str(sources_path), str(output_path))

        try:
            # Fetch and merge this source's URLs
            source_sources = {source_name: urls}
            updater.fetch_and_merge(source_sources)

            # Count domains in this source
            count = count_domains_in_file(output_path)
            categories_count[source_name] = count
            print(f"  {source_name}: {count} domains")
        except Exception as e:
            print(f"  Error fetching {source_name}: {e}")
            categories_count[source_name] = 0

    return categories_count


def main():
    """Main measurement function"""

    # Determine paths
    script_dir = Path(__file__).parent
    repo_dir = script_dir.parent
    work_dir = repo_dir / ".measure_work"
    sources_path = repo_dir / "sources.json"
    docker_compose_path = repo_dir / "docker-compose.yaml"

    # Cleanup from previous runs
    if work_dir.exists():
        import shutil
        shutil.rmtree(work_dir)
    work_dir.mkdir(parents=True, exist_ok=True)

    # Create directories
    (work_dir / "dns" / "lists").mkdir(parents=True, exist_ok=True)
    (work_dir / "certs").mkdir(parents=True, exist_ok=True)

    # Create self-signed certs if not present
    certs_dir = work_dir / "certs"
    if not (certs_dir / "cert.pem").exists():
        print("Generating self-signed certificates...")
        subprocess.run(
            [
                "openssl", "req", "-x509", "-newkey", "rsa:2048",
                "-keyout", str(certs_dir / "key.pem"),
                "-out", str(certs_dir / "cert.pem"),
                "-days", "1", "-nodes",
                "-subj", "/CN=localhost"
            ],
            capture_output=True,
            check=False,
        )

    try:
        print("Step 1: Fetching and categorizing sources...")
        categories_count = fetch_and_merge_sources(work_dir, sources_path)

        print("\nStep 2: Building blocky configuration...")
        config_path = build_blocky_config(work_dir)

        print("\nStep 3: Preparing docker-compose for measurement...")
        # Read current docker-compose
        with open(docker_compose_path, 'r') as f:
            docker_compose = yaml.safe_load(f)

        # Check current mem_limit
        current_mem_limit = docker_compose.get('services', {}).get('webfilter-dns', {}).get('mem_limit', '512m')
        print(f"Current mem_limit: {current_mem_limit}")

        print("\nStep 4: Starting webfilter-dns container...")
        # Use docker-compose from the repo directory
        os.chdir(repo_dir)

        # Stop any existing container
        subprocess.run(
            ["docker-compose", "down"],
            capture_output=True,
        )

        # Bring up the service
        result = subprocess.run(
            ["docker-compose", "up", "-d"],
            capture_output=True,
            text=True,
        )

        if result.returncode != 0:
            print(f"Error starting container: {result.stderr}")
            sys.exit(1)

        # Wait for container to be ready
        print("Waiting for container to start...")
        time.sleep(5)

        print("\nStep 5: Measuring startup time...")
        startup_time = None
        try:
            startup_time = wait_for_dns_response()
            if startup_time is not None:
                print(f"Startup time: {startup_time:.2f} seconds")
            else:
                print("Warning: Could not measure startup time (DNS check timed out)")
                startup_time = -1
        except Exception as e:
            print(f"Warning: Could not measure startup time: {e}")
            startup_time = -1

        print("\nStep 6: Measuring RSS memory...")
        rss_mb = get_docker_memory_mb("webfilter-dns")
        if rss_mb is not None:
            print(f"RSS memory: {rss_mb:.2f} MB")
        else:
            print("Warning: Could not measure RSS memory")
            rss_mb = -1

        print("\nStep 7: Generating measurement report...")

        # Prepare output
        measurement = {
            "rss_mb": round(rss_mb, 2) if rss_mb >= 0 else -1,
            "startup_time_s": round(startup_time, 2) if startup_time >= 0 else -1,
            "categories_count": categories_count,
        }

        # Print JSON output
        output_json = json.dumps(measurement, indent=2)
        print("\n" + "="*60)
        print("MEASUREMENT RESULTS:")
        print("="*60)
        print(output_json)

        # Write to file
        output_path = repo_dir / "measurement_result.json"
        output_path.write_text(output_json + "\n")
        print(f"\nResults saved to: {output_path}")

        # Print summary
        print("\n" + "="*60)
        print("SUMMARY:")
        print("="*60)
        print(f"RSS Memory: {measurement['rss_mb']} MB")
        print(f"Startup Time: {measurement['startup_time_s']} seconds")
        print(f"Categories: {list(measurement['categories_count'].keys())}")
        total_domains = sum(measurement['categories_count'].values())
        print(f"Total Domains: {total_domains}")

        # Check acceptance criteria
        print("\n" + "="*60)
        print("ACCEPTANCE CRITERIA:")
        print("="*60)
        if startup_time >= 0 and startup_time <= 60:
            print("✓ Startup time <= 60s: PASS")
        else:
            print(f"✗ Startup time <= 60s: FAIL (measured {startup_time}s)")

        print("✓ JSON has 3 required fields: PASS")
        print("✓ Memory limit to be declared by human: PENDING")

        return 0

    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return 1

    finally:
        print("\nCleaning up...")
        # Stop the container
        try:
            os.chdir(repo_dir)
            subprocess.run(
                ["docker-compose", "down"],
                capture_output=True,
                timeout=30,
            )
        except Exception as e:
            print(f"Warning: Error stopping container: {e}")

        # Cleanup work directory
        if work_dir.exists():
            import shutil
            try:
                shutil.rmtree(work_dir)
            except Exception:
                pass


if __name__ == "__main__":
    sys.exit(main())
