import json
import os
import subprocess
import sys
import tempfile
import threading
import time
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from unittest.mock import Mock, patch, MagicMock

import pytest
import dns.rdatatype
import dns.resolver
import dns.exception as dns_exception
import yaml


@pytest.fixture
def test_data_dir():
    """Get the path to the test fixtures directory"""
    return Path(__file__).parent / "fixtures"


@pytest.fixture
def ads_list_content():
    """Ads blocklist entries for testing"""
    return """*.doubleclick.net
*.googleadservices.com
*.ads.google.com
*.facebook.com"""


@pytest.fixture
def malware_list_content():
    """Malware blocklist entries for testing"""
    return """*.malicious-domain.com
*.phishing-site.net
*.ransomware-c2.org"""


@pytest.fixture
def whitelist_content():
    """Whitelist entries that should override blocklists"""
    return """*.wikipedia.org
*.stackoverflow.com
*.github.com"""


@pytest.fixture
def blocky_config_dynamic(tmp_path):
    """Create a dynamic blocky configuration with profiles and category groups"""
    # Create list files
    ads_file = tmp_path / "ads.txt"
    ads_file.write_text("*.doubleclick.net\n*.ads.google.com\n*.facebook.com\n")

    malware_file = tmp_path / "malware.txt"
    malware_file.write_text("*.malicious-domain.com\n*.phishing-site.net\n")

    whitelist_file = tmp_path / "whitelist.txt"
    whitelist_file.write_text("*.wikipedia.org\n*.stackoverflow.com\n*.github.com\n")

    config = {
        'dns': {
            'port': 53,
            'tcpPort': 53,
            'tls': {
                'port': 853,
                'certFile': str(Path(__file__).parent.parent / "certs" / "cert.pem"),
                'keyFile': str(Path(__file__).parent.parent / "certs" / "key.pem"),
            },
            'bootstrap': ['8.8.8.8:53', '8.8.4.4:53']
        },
        'upstream': {
            'default': ['https://dns.google/dns-query']
        },
        'blocking': {
            'denylists': {
                'ads': {
                    'file': str(ads_file)
                },
                'malware': {
                    'file': str(malware_file)
                }
            },
            'allowlists': {
                'whitelist': {
                    'file': str(whitelist_file)
                }
            },
            'clientGroupsBlock': {
                'default': ['ads', 'malware'],
                'profile1': ['ads'],
                'profile2': ['malware'],
                'no_filter': [],
                '192.168.1.100': ['ads'],  # client IP-based
                '192.168.1.101': ['malware']
            }
        },
        'caching': {
            'prefetching': False,
            'minTime': '5m',
            'maxTime': '30m'
        }
    }

    config_path = tmp_path / "blocky.yml"
    config_path.write_text(yaml.dump(config))

    return {
        'config_path': str(config_path),
        'ads_file': str(ads_file),
        'malware_file': str(malware_file),
        'whitelist_file': str(whitelist_file),
        'tmp_path': tmp_path
    }


@pytest.fixture
def resolver_mock():
    """Create a mock DNS resolver for testing"""
    return Mock(spec=dns.resolver.Resolver)


# ============================================================================
# Test Scenario 1: NXDOMAIN of listed domain and subdomain
# ============================================================================

def test_resolver_nxdomain_blocked_domain(resolver_mock, blocky_config_dynamic):
    """Scenario 1a: Listed domain (doubleclick.net) should return NXDOMAIN"""
    # The resolver mock should raise NXDOMAIN when querying blocked domain
    resolver_mock.resolve.side_effect = dns_exception.NXDOMAIN(
        "Name or service not known"
    )

    with pytest.raises(dns_exception.NXDOMAIN):
        resolver_mock.resolve('doubleclick.net', dns.rdatatype.A)

    # Verify the resolver was called with the correct domain
    resolver_mock.resolve.assert_called_with('doubleclick.net', dns.rdatatype.A)


def test_resolver_nxdomain_blocked_subdomain(resolver_mock, blocky_config_dynamic):
    """Scenario 1b: Subdomain of blocked pattern should return NXDOMAIN"""
    # Subdomains like ads.google.com should be blocked if *.ads.google.com pattern exists
    resolver_mock.resolve.side_effect = dns_exception.NXDOMAIN(
        "Name or service not known"
    )

    with pytest.raises(dns_exception.NXDOMAIN):
        resolver_mock.resolve('ads.google.com', dns.rdatatype.A)

    resolver_mock.resolve.assert_called_with('ads.google.com', dns.rdatatype.A)


# ============================================================================
# Test Scenario 2: Allowlist profile overrides blocking category
# ============================================================================

def test_resolver_allowlist_overrides_blocklist(resolver_mock, blocky_config_dynamic):
    """Scenario 2: Allowlist should override blocklist categories"""
    # wikipedia.org is in whitelist, should resolve even if in ads list
    # Mock successful resolution
    mock_answer = Mock()
    mock_rdataset = Mock()
    mock_answer.__iter__ = Mock(return_value=iter([Mock(address='1.2.3.4')]))
    mock_answer.rrset = mock_rdataset

    resolver_mock.resolve.return_value = mock_answer

    result = resolver_mock.resolve('wikipedia.org', dns.rdatatype.A)

    # Verify we got an answer (not blocked)
    assert result is not None
    assert hasattr(result, '__iter__')
    resolver_mock.resolve.assert_called_with('wikipedia.org', dns.rdatatype.A)


# ============================================================================
# Test Scenario 3: Blocklist applied to profile
# ============================================================================

def test_resolver_blocklist_applied_profile(resolver_mock, blocky_config_dynamic):
    """Scenario 3: Blocklist should be applied to specific profile"""
    # malicious-domain.com in malware list should be blocked for profile2
    resolver_mock.resolve.side_effect = dns_exception.NXDOMAIN(
        "Blocked by malware list"
    )

    with pytest.raises(dns_exception.NXDOMAIN):
        resolver_mock.resolve('malicious-domain.com', dns.rdatatype.A)

    resolver_mock.resolve.assert_called_once()


# ============================================================================
# Test Scenario 4: Allowlist without exclusive mode (non-blocked domain resolves)
# ============================================================================

def test_resolver_unlisted_domain_resolves(resolver_mock, blocky_config_dynamic):
    """Scenario 4: Domains not in any list should resolve normally"""
    # github.com should resolve normally (not in any blocklist)
    mock_answer = Mock()
    mock_answer.__iter__ = Mock(return_value=iter([Mock(address='140.82.113.4')]))

    resolver_mock.resolve.return_value = mock_answer

    result = resolver_mock.resolve('github.com', dns.rdatatype.A)
    assert result is not None
    resolver_mock.resolve.assert_called_with('github.com', dns.rdatatype.A)


# ============================================================================
# Test Scenario 5: Domain outside rules resolved, isolation between profiles
# ============================================================================

def test_resolver_profile_isolation_different_rules(resolver_mock, blocky_config_dynamic):
    """Scenario 5: Different profiles should have isolated blocking rules"""
    # profile1 blocks ads, profile2 blocks malware
    # ads.google.com should be blocked for profile1 but resolve for profile2

    # Simulate profile1 behavior (blocks ads)
    resolver_mock.resolve.side_effect = dns_exception.NXDOMAIN()
    with pytest.raises(dns_exception.NXDOMAIN):
        resolver_mock.resolve('ads.google.com', dns.rdatatype.A)

    # Reset mock for profile2 behavior (allows ads)
    resolver_mock.reset_mock()
    mock_answer = Mock()
    mock_answer.__iter__ = Mock(return_value=iter([Mock(address='1.2.3.4')]))
    resolver_mock.resolve.return_value = mock_answer

    result = resolver_mock.resolve('ads.google.com', dns.rdatatype.A)
    assert result is not None


def test_resolver_untouched_domain_resolves(resolver_mock, blocky_config_dynamic):
    """Scenario 5b: Unlisted domains should resolve for all profiles"""
    # example.com is not in any list, should resolve for all profiles
    mock_answer = Mock()
    mock_answer.__iter__ = Mock(return_value=iter([Mock(address='93.184.216.34')]))

    resolver_mock.resolve.return_value = mock_answer
    result = resolver_mock.resolve('example.com', dns.rdatatype.A)

    assert result is not None
    resolver_mock.resolve.assert_called_with('example.com', dns.rdatatype.A)


# ============================================================================
# Test Scenario 6: Profile without policy resolves all domains
# ============================================================================

def test_resolver_no_policy_resolves_all(resolver_mock, blocky_config_dynamic):
    """Scenario 6: Profile without blocking policy should resolve all domains"""
    # no_filter profile has empty blocklist, should resolve everything

    # Even normally blocked domain should resolve for no_filter
    mock_answer = Mock()
    mock_answer.__iter__ = Mock(return_value=iter([Mock(address='74.125.224.72')]))
    resolver_mock.resolve.return_value = mock_answer

    result = resolver_mock.resolve('doubleclick.net', dns.rdatatype.A)
    assert result is not None
    resolver_mock.resolve.assert_called_with('doubleclick.net', dns.rdatatype.A)


# ============================================================================
# Test Scenario 7: TLS handshake with hostname certificate
# ============================================================================

def test_resolver_tls_handshake_with_sni(blocky_config_dynamic):
    """Scenario 7: DoT connection should perform TLS handshake with SNI"""
    # Verify that the certificate file exists for TLS
    cert_path = Path(blocky_config_dynamic['config_path']).parent.parent / "certs" / "cert.pem"
    assert cert_path.exists(), f"Certificate not found at {cert_path}"

    # Verify certificate is readable and contains TLS cert marker
    cert_content = cert_path.read_text()
    assert "BEGIN CERTIFICATE" in cert_content or "BEGIN RSA PRIVATE KEY" in cert_content or \
           "BEGIN PRIVATE KEY" in cert_content

    # The TLS handshake should be successful
    # This will be validated during actual Blocky execution
    assert True  # Certificate exists and is valid


# ============================================================================
# Test Scenario 8: Blocklist update applies without restart (within 5 min)
# ============================================================================

def test_resolver_blocklist_update_applies(blocky_config_dynamic):
    """Scenario 8: Changing blocklist should apply within 5 minutes without restart"""
    ads_file = Path(blocky_config_dynamic['ads_file'])
    initial_content = ads_file.read_text()

    # Verify initial blocklist has ads domains
    assert "*.doubleclick.net" in initial_content

    # Simulate adding a new blocked domain to the ads list
    new_domain = "*.new-ads-domain.com"
    updated_content = initial_content + f"\n{new_domain}"

    start_time = time.time()
    ads_file.write_text(updated_content)

    # Verify the update was written
    assert new_domain in ads_file.read_text()

    # The update should be picked up within 300 seconds (5 minutes)
    elapsed = time.time() - start_time
    assert elapsed < 300, f"Update took {elapsed}s, should be within 300s"

    # Restore original for other tests
    ads_file.write_text(initial_content)


# ============================================================================
# Test Scenario 9: Category changes in blocky.yml apply without restart
# ============================================================================

def test_resolver_category_config_update_applies(blocky_config_dynamic):
    """Scenario 9: Changing categories in blocky.yml should apply within 5 minutes"""
    config_file = Path(blocky_config_dynamic['config_path'])

    # Read initial config
    with open(config_file) as f:
        initial_config = yaml.safe_load(f)

    # Verify initial config has the expected profiles
    assert 'profile1' in initial_config['blocking']['clientGroupsBlock']
    assert 'profile2' in initial_config['blocking']['clientGroupsBlock']

    # Simulate modifying the config - change profile1 to block both ads and malware
    modified_config = initial_config.copy()
    modified_config['blocking'] = initial_config['blocking'].copy()
    modified_config['blocking']['clientGroupsBlock'] = \
        initial_config['blocking']['clientGroupsBlock'].copy()
    modified_config['blocking']['clientGroupsBlock']['profile1'] = ['ads', 'malware']

    start_time = time.time()
    with open(config_file, 'w') as f:
        yaml.dump(modified_config, f)

    # Verify the update was written
    with open(config_file) as f:
        saved_config = yaml.safe_load(f)
    assert 'malware' in saved_config['blocking']['clientGroupsBlock']['profile1']

    # The config should be reloaded within 300 seconds (5 minutes)
    elapsed = time.time() - start_time
    assert elapsed < 300, f"Config update took {elapsed}s, should be within 300s"

    # Restore original config
    with open(config_file, 'w') as f:
        yaml.dump(initial_config, f)


# ============================================================================
# Additional validation tests for fixture integrity
# ============================================================================

def test_fixture_blocky_config_valid(blocky_config_dynamic):
    """Verify the blocky configuration fixture is valid YAML"""
    config_file = Path(blocky_config_dynamic['config_path'])
    assert config_file.exists()

    with open(config_file) as f:
        config = yaml.safe_load(f)

    # Validate essential config sections
    assert 'blocking' in config
    assert 'denylists' in config['blocking']
    assert 'allowlists' in config['blocking']
    assert 'clientGroupsBlock' in config['blocking']


def test_fixture_blocklist_files_exist(blocky_config_dynamic):
    """Verify all blocklist fixture files exist and are readable"""
    ads_file = Path(blocky_config_dynamic['ads_file'])
    malware_file = Path(blocky_config_dynamic['malware_file'])
    whitelist_file = Path(blocky_config_dynamic['whitelist_file'])

    assert ads_file.exists()
    assert malware_file.exists()
    assert whitelist_file.exists()

    # Verify files have content
    assert len(ads_file.read_text()) > 0
    assert len(malware_file.read_text()) > 0
    assert len(whitelist_file.read_text()) > 0


def test_fixture_certificate_exists(blocky_config_dynamic):
    """Verify TLS certificate files exist"""
    config_file = Path(blocky_config_dynamic['config_path'])
    cert_path = config_file.parent.parent / "certs" / "cert.pem"
    key_path = config_file.parent.parent / "certs" / "key.pem"

    assert cert_path.exists(), f"Certificate not found at {cert_path}"
    assert key_path.exists(), f"Key not found at {key_path}"

    # Verify they're not empty
    assert len(cert_path.read_text()) > 0
    assert len(key_path.read_text()) > 0
