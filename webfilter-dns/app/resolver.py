#!/usr/bin/env python3
"""Supervisor e atualizador de listas do resolvedor webfilter-dns (design D8).

O plugin Web Filter do MDM grava em /app/dns (somente leitura aqui):
  blocky.yml               configuracao completa do Blocky
  profiles/*.txt           allow/deny de cada perfil, ja normalizadas
  sources.json             categoria -> URLs das fontes

Este processo:
  * baixa as fontes de cada categoria usada para /app/lists/<categoria>.txt, normalizando
    toda entrada para "*.<dominio>" (bloqueia o dominio e seus subdominios), no maximo a
    cada 24 h e nunca mais de uma vez por hora por categoria; falha ou lista vazia mantem
    a lista anterior;
  * so inicia o Blocky quando a configuracao e todas as listas que ela referencia existem
    (fail-closed: sem filtro pronto, a porta 853 fica fechada e nada e resolvido sem filtro);
  * reinicia o Blocky quando blocky.yml muda e volta para a ultima configuracao boa se a
    nova nao ficar saudavel;
  * pede POST /api/lists/refresh (sem reinicio) quando so listas mudam.
"""
import hashlib
import json
import logging
import os
import re
import shutil
import signal
import socket
import struct
import subprocess
import sys
import threading
import time
import urllib.request

DNS_DIR = os.environ.get("WEBFILTER_DNS_DIR", "/app/dns")
LISTS_DIR = os.environ.get("WEBFILTER_LISTS_DIR", "/app/lists")
STATE_DIR = os.environ.get("WEBFILTER_STATE_DIR", "/app/state")
BLOCKY = os.environ.get("BLOCKY_BINARY", "/usr/local/bin/blocky")
API = "http://127.0.0.1:4000"
REFRESH_MAX = 24 * 3600
REFRESH_MIN = 3600
LOOP_SECONDS = 5
HEALTH_TIMEOUT = 180
RETRY_FIRST_SECONDS = 120
RETRY_MAX_SECONDS = 3600
DRY_RUN_DNS_PORT = 5353
RESTART_GOOD_SECONDS = 10

log = logging.getLogger("webfilter-dns")
LABEL = re.compile(r"^[a-z0-9_](?:[a-z0-9_-]{0,61}[a-z0-9_])?$")


# ------------------------------------------------------------------------------------ listas
def normalize(line):
    """Uma linha de lista -> '*.dominio' ou None (comentario, IP, lixo)."""
    line = line.split("#", 1)[0].strip().lower()
    if not line:
        return None
    parts = line.split()
    token = parts[-1] if len(parts) > 1 else parts[0]  # formato hosts: "0.0.0.0 dominio"
    while token.startswith("*.") or token.startswith("."):
        token = token[2:] if token.startswith("*.") else token[1:]
    token = token.rstrip(".")
    labels = token.split(".")
    if len(labels) < 2 or len(token) > 253:
        return None
    if labels[-1].isdigit():  # IP literal
        return None
    if not all(LABEL.match(label) for label in labels):
        return None
    return "*." + token


def download(url, timeout=120):
    req = urllib.request.Request(url, headers={"User-Agent": "hwmdm-webfilter-dns/1.0"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        if resp.status != 200:
            raise IOError(f"HTTP {resp.status}")
        return resp.read().decode("utf-8", errors="replace")


def update_category(category, urls):
    """Baixa e troca a lista da categoria. Retorna True se o arquivo mudou."""
    entries = set()
    for url in urls:
        text = download(url)  # qualquer falha aborta a categoria inteira: lista parcial nunca substitui a boa
        for line in text.splitlines():
            n = normalize(line)
            if n:
                entries.add(n)
    if not entries:
        raise ValueError("nenhum dominio nas fontes")
    content = "\n".join(sorted(entries)) + "\n"
    target = os.path.join(LISTS_DIR, category + ".txt")
    if os.path.exists(target) and sha256_file(target) == hashlib.sha256(content.encode()).hexdigest():
        return False
    tmp = target + ".tmp"
    with open(tmp, "w") as f:
        f.write(content)
    os.replace(tmp, target)
    log.info("lista %s atualizada: %d dominios", category, len(entries))
    return True


class Updater:
    def __init__(self):
        self.state_file = os.path.join(STATE_DIR, "updater.json")
        try:
            with open(self.state_file) as f:
                self.state = json.load(f)
        except (OSError, ValueError):
            self.state = {}

    def _save(self):
        tmp = self.state_file + ".tmp"
        with open(tmp, "w") as f:
            json.dump(self.state, f)
        os.replace(tmp, self.state_file)

    def run_once(self):
        """Atualiza as categorias vencidas. Retorna True se alguma lista mudou."""
        try:
            with open(os.path.join(DNS_DIR, "sources.json")) as f:
                sources = json.load(f)
        except (OSError, ValueError):
            return False
        changed = False
        now = time.time()
        for category, urls in sources.items():
            st = self.state.get(category, {})
            exists = os.path.exists(os.path.join(LISTS_DIR, category + ".txt"))
            age_ok = now - st.get("ok", 0) < REFRESH_MAX
            recent_try = now - st.get("try", 0) < REFRESH_MIN
            if (exists and age_ok) or recent_try:
                continue
            st["try"] = now
            try:
                changed |= update_category(category, urls)
                st["ok"] = now
            except Exception as e:  # noqa: BLE001 - qualquer falha de rede/conteudo mantem a lista anterior
                log.warning("lista %s nao atualizada (%s); mantida a anterior", category, e)
            self.state[category] = st
            self._save()
        return changed


# ------------------------------------------------------------------------------------ blocky
def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def referenced_files(config_text):
    return re.findall(r"^\s*-\s+(/app/\S+\.txt)\s*$", config_text, re.M)


def dns_ok(port=53):
    """Consulta A de 'localhost.' no listener local; qualquer resposta DNS = Blocky servindo."""
    q = struct.pack(">HHHHHH", 0x1234, 0x0100, 1, 0, 0, 0) + b"\x09localhost\x00" + struct.pack(">HH", 1, 1)
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.settimeout(2)
            s.sendto(q, ("127.0.0.1", port))
            data, _ = s.recvfrom(512)
            return data[:2] == q[:2]
    except OSError:
        return False


def api_refresh():
    try:
        req = urllib.request.Request(API + "/api/lists/refresh", method="POST")
        urllib.request.urlopen(req, timeout=120).read()
        log.info("listas recarregadas sem reinicio")
        return True
    except Exception as e:  # noqa: BLE001
        log.warning("refresh falhou: %s", e)
        return False


class Supervisor:
    def __init__(self):
        self.proc = None
        self.running_hash = None
        self.good_config = os.path.join(STATE_DIR, "blocky.good.yml")
        self.profiles_hash = None
        self.failed_hash = None
        self.retry_at = 0.0
        self.retry_delay = RETRY_FIRST_SECONDS
        self.last_good_attempt = 0.0
        self.started_once = False

    def _profiles_digest(self):
        h = hashlib.sha256()
        pdir = os.path.join(DNS_DIR, "profiles")
        if os.path.isdir(pdir):
            for name in sorted(os.listdir(pdir)):
                if name.endswith(".txt"):
                    h.update(name.encode())
                    h.update(sha256_file(os.path.join(pdir, name)).encode())
        return h.hexdigest()

    def stop(self):
        if self.proc and self.proc.poll() is None:
            self.proc.terminate()
            try:
                self.proc.wait(timeout=20)
            except subprocess.TimeoutExpired:
                self.proc.kill()
                self.proc.wait()
        self.proc = None

    def _start(self, config):
        self.stop()  # fail-closed: nunca duas instancias, nunca uma sem filtro
        log.info("iniciando Blocky com %s", config)
        self.proc = subprocess.Popen([BLOCKY, "--config", config])
        deadline = time.time() + HEALTH_TIMEOUT
        while time.time() < deadline:
            if self.proc.poll() is not None:
                log.error("Blocky terminou com codigo %s", self.proc.returncode)
                return False
            if dns_ok():
                self.started_once = True
                return True
            time.sleep(1)
        log.error("Blocky nao ficou saudavel em %ss", HEALTH_TIMEOUT)
        return False

    def _schedule_retry(self, config_hash):
        if config_hash != self.failed_hash:
            self.retry_delay = RETRY_FIRST_SECONDS
        self.failed_hash = config_hash
        self.retry_at = time.time() + self.retry_delay
        log.warning("configuracao %s: nova tentativa em %ss", config_hash[:12], self.retry_delay)
        self.retry_delay = min(self.retry_delay * 2, RETRY_MAX_SECONDS)

    def _dry_run(self, config):
        """Sobe a configuracao em portas alternativas (sem DoT) e diz se ela fica saudavel."""
        with open(config) as f:
            text = f.read()
        text = re.sub(r"^  dns: .*$", f"  dns: 127.0.0.1:{DRY_RUN_DNS_PORT}", text, flags=re.M)
        text = re.sub(r"^  tls: .*\n", "", text, flags=re.M)
        text = re.sub(r"^  http: .*$", "  http: 127.0.0.1:4001", text, flags=re.M)
        trial = os.path.join(STATE_DIR, "blocky.trial.yml")
        with open(trial, "w") as f:
            f.write(text)
        log.info("ensaiando a configuracao em portas alternativas antes de trocar a instancia em uso")
        proc = subprocess.Popen([BLOCKY, "--config", trial])
        try:
            deadline = time.time() + HEALTH_TIMEOUT
            while time.time() < deadline:
                if proc.poll() is not None:
                    return False
                if dns_ok(DRY_RUN_DNS_PORT):
                    return True
                time.sleep(1)
            return False
        finally:
            if proc.poll() is None:
                proc.terminate()
                try:
                    proc.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    proc.kill()
                    proc.wait()

    def ready(self, config_path):
        with open(config_path) as f:
            missing = [p for p in referenced_files(f.read()) if not os.path.exists(p)]
        if missing:
            log.info("aguardando listas antes de iniciar: %s", ", ".join(missing))
        return not missing

    def reconcile(self, lists_changed):
        config = os.path.join(DNS_DIR, "blocky.yml")
        if not os.path.exists(config):
            return

        # 1. A instancia em uso e mantida viva sempre, inclusive enquanto uma configuracao nova espera
        #    nova tentativa: sem instancia viva, a ultima configuracao boa e reiniciada (no maximo a cada
        #    RESTART_GOOD_SECONDS), em vez de esperar a proxima tentativa da configuracao nova.
        if self.proc is not None and self.proc.poll() is not None:
            log.error("Blocky caiu (codigo %s)", self.proc.returncode)
            self.proc = None
            self.running_hash = None
        current = sha256_file(config)
        recovering = self.started_once or current == self.failed_hash  # no boot limpo, o passo 2 sobe a atual
        if (self.proc is None and recovering and os.path.exists(self.good_config)
                and time.time() - self.last_good_attempt >= RESTART_GOOD_SECONDS):
            self.last_good_attempt = time.time()
            profiles_at_start = self._profiles_digest()
            if self._start(self.good_config):
                log.info("Blocky de volta com a ultima configuracao boa")
                self.running_hash = sha256_file(self.good_config)
                self.profiles_hash = profiles_at_start
            else:
                self.stop()

        # 2. Configuracao nova (perfil ativado/desativado, categorias): troca de instancia.
        if current != self.running_hash and self._switch_due(current) and self.ready(config):
            self._switch(config, current)
            return

        # 3. Listas mudaram (allow/deny de perfil ou lista de categoria): recarga sem reinicio na instancia
        #    em uso -- tambem durante a espera de nova tentativa de uma configuracao que falhou.
        if self.proc is not None and self.proc.poll() is None:
            profiles = self._profiles_digest()
            if lists_changed or profiles != self.profiles_hash:
                if api_refresh():
                    self.profiles_hash = profiles

    def _switch_due(self, config_hash):
        """Configuracao que ja falhou so e tentada de novo quando chega a hora agendada."""
        return config_hash != self.failed_hash or time.time() >= self.retry_at

    def _switch(self, config, current):
        retrying = current == self.failed_hash
        # Enquanto ha uma instancia boa servindo, a nova tentativa e ensaiada em portas alternativas,
        # sem derrubar o DNS dos tablets; so a configuracao aprovada no ensaio substitui a boa.
        if retrying and self.proc is not None and self.proc.poll() is None and not self._dry_run(config):
            self._schedule_retry(current)
            return
        staged = os.path.join(STATE_DIR, "blocky.candidate.yml")
        shutil.copyfile(config, staged)  # o plugin pode reescrever blocky.yml durante a troca
        # Registrado ANTES de iniciar: uma lista de perfil alterada enquanto o Blocky carrega
        # difere deste valor e ganha um refresh logo em seguida, em vez de ser perdida.
        profiles_at_start = self._profiles_digest()
        if self._start(staged):
            shutil.copyfile(staged, self.good_config)
            self.running_hash = current
            self.profiles_hash = profiles_at_start
            self.failed_hash = None
            self.retry_delay = RETRY_FIRST_SECONDS
            log.info("Blocky pronto com a configuracao %s", current[:12])
            return
        self._schedule_retry(current)
        self.stop()
        self.running_hash = None
        if os.path.exists(self.good_config):
            log.error("configuracao %s nao subiu; voltando para a ultima configuracao boa", current[:12])
        else:
            log.error("configuracao %s nao subiu e ainda nao ha configuracao boa; DNS fechado ate a nova tentativa",
                      current[:12])
        # O passo 1 do proximo ciclo sobe a configuracao boa (e insiste nela se tambem falhar).

def main():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s", stream=sys.stdout)
    for d in (LISTS_DIR, STATE_DIR):
        os.makedirs(d, exist_ok=True)
    updater, supervisor = Updater(), Supervisor()
    stop = threading.Event()

    def terminate(*_):
        stop.set()

    signal.signal(signal.SIGTERM, terminate)
    signal.signal(signal.SIGINT, terminate)
    while not stop.is_set():
        changed = updater.run_once()
        supervisor.reconcile(changed)
        stop.wait(LOOP_SECONDS)
    supervisor.stop()


if __name__ == "__main__":
    main()
