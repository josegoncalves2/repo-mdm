#!/usr/bin/env python3
"""hwmdm-admin: gestao do servidor HWMDM pela web.

Roda como um container proprio da mesma stack (mesmo compose, rede, banco e login do MDM),
justamente para sobreviver quando reinicia, derruba, sobe ou reconstroi os outros: quem aplica
a mudanca nao e' o painel que vai cair.

- Login: os mesmos usuarios do MDM (tabela users), perfis Super-Admin e Admin.
- Configuracao: le e grava o source/.env da stack e espelha o modo DEV/PRD e os enderecos dos
  servidores na tabela hwmdm_system_settings, que o MDM le (pagina de bloqueio do Web Filter).
- Containers: estado, logs, reiniciar, aplicar configuracao (compose up -d), reconstruir.
- Backup: e' o modulo que ja existe no MDM (BackupResource); aqui so' ha o atalho para ele.

Somente biblioteca padrao + psycopg2 + o cliente docker/compose da imagem.
"""
import hashlib
import html
import http.cookies
import http.server
import json
import os
import re
import secrets
import socketserver
import subprocess
import threading
import time
import urllib.parse
import urllib.request

import psycopg2

ROOT = os.environ["HWMDM_ROOT"]
STACK_DIR = os.path.join(ROOT, "source")
ENV_FILE = os.path.join(STACK_DIR, ".env")
PROJECT = os.environ.get("COMPOSE_PROJECT_NAME", "hwmdm")
SELF_SERVICE = "admin"
PORT = int(os.environ.get("ADMIN_PORT", "8090"))
PASS_SALT = os.environ.get("HMDM_PASS_SALT", "5YdSYHyg2U")
ADMIN_ROLES = tuple(int(x) for x in os.environ.get("ADMIN_ROLE_IDS", "1,2").split(","))
SESSION_TTL = int(os.environ.get("ADMIN_SESSION_TTL", "28800"))
# Endereco interno do MDM na rede da stack: valida a sessao do painel (login unico).
MDM_INTERNAL_URL = os.environ.get("MDM_INTERNAL_URL", "http://hmdm:8080").rstrip("/")

SESSIONS = {}
JOBS_FILE = os.environ.get("ADMIN_JOBS_FILE", "/data/jobs.json")
try:
    with open(JOBS_FILE, encoding="utf-8") as _f:
        JOBS = json.load(_f)
except (OSError, ValueError):
    JOBS = []
JOBS_LOCK = threading.Lock()

# Todas as variaveis que a stack le do .env, agrupadas como aparecem na tela.
FIELDS = [
    ("Servidor", [
        ("SERVER_MODE", "Modo do servidor", "dev = SERVIDOR-DEV (desenvolvimento/homologação); prd = SERVIDOR-PRD (produção)"),
        ("SERVER_DEV_URL", "Endereço do SERVIDOR-DEV", "Ex.: http://mdm.pmeto.local"),
        ("SERVER_PRD_URL", "Endereço do SERVIDOR-PRD", "Ex.: https://mdm.olimpia.sp.gov.br"),
        ("BASE_DOMAIN", "Domínio/host do MDM", "Gravado no QR de matrícula; errado aqui = nenhum tablet matricula"),
        ("LOCAL_IP", "IP do servidor", "IP pelo qual os tablets alcançam esta máquina"),
        ("PROTOCOL", "Protocolo interno", "http ou https"),
        ("PUBLIC_PROTOCOL", "Protocolo público", "https quando há proxy TLS na frente"),
        ("PROXY_ADDRESSES", "Proxies confiáveis", "IPs separados por vírgula; vazio se não há proxy"),
        ("TZ", "Fuso horário", "Ex.: America/Sao_Paulo"),
    ]),
    ("DNS / Web Filter", [
        ("WEBFILTER_BIND_ADDR", "Endereço do DNS do filtro", "Vazio = IP do servidor"),
        ("WEBFILTER_CERTS_DIR", "Pasta dos certificados DoT", "cert.pem e key.pem"),
    ]),
    ("Portas", [
        ("MDM_HTTP_PORT", "Porta HTTP do painel", ""),
        ("MDM_PUBLIC_PORT", "Porta pública (sem :porta na URL)", ""),
        ("MDM_PUSH_PORT", "Porta do push", ""),
        ("POSTGRES_PORT", "Porta do PostgreSQL no host", ""),
        ("ADMIN_PORT", "Porta desta gestão", ""),
    ]),
    ("Banco de dados", [
        ("SQL_USER", "Usuário", ""),
        ("SQL_BASE", "Base", ""),
        ("SQL_PASS", "Senha", ""),
    ]),
    ("MDM", [
        ("ADMIN_EMAIL", "E-mail do administrador", ""),
        ("SHARED_SECRET", "Segredo compartilhado", "Mantenha o mesmo ao restaurar um banco"),
        ("CLIENT_VERSION", "Versão do launcher", ""),
        ("HMDM_VARIANT", "Variante", ""),
        ("HMDM_URL", "URL do WAR", "Vazio = WAR do repositório"),
        ("DOWNLOAD_CREDENTIALS", "Credenciais de download", ""),
        ("FORCE_RECONFIGURE", "Reconfigurar no próximo boot", "true/false"),
        ("APK_SUFIXO", "Sufixo dos APKs publicados", ""),
    ]),
]
SECRET_KEYS = {"SQL_PASS", "SHARED_SECRET", "DOWNLOAD_CREDENTIALS"}
SETTINGS_KEYS = {"SERVER_MODE": "server.mode", "SERVER_DEV_URL": "server.dev.url",
                 "SERVER_PRD_URL": "server.prd.url"}


# ============================================================================== .env

def read_env():
    values, lines = {}, []
    if os.path.exists(ENV_FILE):
        with open(ENV_FILE, encoding="utf-8") as f:
            lines = f.read().splitlines()
    for line in lines:
        m = re.match(r"^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$", line)
        if m:
            values[m.group(1)] = m.group(2)
    return values, lines


def write_env(changes):
    values, lines = read_env()
    out, seen = [], set()
    for line in lines:
        m = re.match(r"^\s*([A-Za-z_][A-Za-z0-9_]*)=", line)
        if m and m.group(1) in changes:
            out.append("%s=%s" % (m.group(1), changes[m.group(1)]))
            seen.add(m.group(1))
        else:
            out.append(line)
    for k, v in changes.items():
        if k not in seen:
            out.append("%s=%s" % (k, v))
    tmp = ENV_FILE + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        f.write("\n".join(out) + "\n")
    if os.path.exists(ENV_FILE):
        os.replace(ENV_FILE, ENV_FILE + ".anterior")
    os.replace(tmp, ENV_FILE)


# ============================================================================== banco

def db():
    env, _ = read_env()
    return psycopg2.connect(host=os.environ.get("SQL_HOST", "postgresql"),
                            port=int(os.environ.get("SQL_PORT", "5432")),
                            dbname=env.get("SQL_BASE") or os.environ["SQL_BASE"],
                            user=env.get("SQL_USER") or os.environ["SQL_USER"],
                            password=env.get("SQL_PASS") or os.environ["SQL_PASS"],
                            connect_timeout=5)


def ensure_schema():
    with db() as c, c.cursor() as cur:
        cur.execute("CREATE TABLE IF NOT EXISTS hwmdm_system_settings ("
                    "key VARCHAR(100) PRIMARY KEY, value TEXT, updated TIMESTAMP DEFAULT now())")


def sync_settings(env):
    with db() as c, c.cursor() as cur:
        for env_key, key in SETTINGS_KEYS.items():
            if env_key in env:
                cur.execute("INSERT INTO hwmdm_system_settings(key, value, updated) VALUES (%s, %s, now()) "
                            "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, updated = now()",
                            (key, env[env_key]))


def mdm_session_user(jsessionid):
    """Login unico: a sessao do painel MDM (cookie JSESSIONID do mesmo host) vale aqui."""
    req = urllib.request.Request(MDM_INTERNAL_URL + "/rest/private/users/current",
                                 headers={"Cookie": "JSESSIONID=" + jsessionid, "Accept": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=5) as r:
            data = json.load(r).get("data") or {}
    except Exception:
        return None
    role = (data.get("userRole") or {}).get("id")
    return data.get("login") if data.get("login") and role in ADMIN_ROLES else None


def check_login(login, password):
    md5 = hashlib.md5(password.encode("utf-8")).hexdigest().upper()
    digest = hashlib.sha1((md5 + PASS_SALT).encode("utf-8")).hexdigest()
    with db() as c, c.cursor() as cur:
        cur.execute("SELECT password, userroleid FROM users WHERE login = %s", (login,))
        row = cur.fetchone()
    return bool(row) and row[0].lower() == digest and row[1] in ADMIN_ROLES


# ============================================================================== docker

def compose(*args, timeout=900):
    cmd = ["docker", "compose", "-p", PROJECT, "--project-directory", STACK_DIR,
           "-f", os.path.join(STACK_DIR, "docker-compose.yaml"), "--env-file", ENV_FILE] + list(args)
    p = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    return p.returncode, (p.stdout + p.stderr).strip()


def services():
    rc, out = compose("config", "--services")
    return [s for s in out.splitlines() if s and s != SELF_SERVICE] if rc == 0 else []


def status():
    rc, out = compose("ps", "-a", "--format", "json")
    rows = []
    for line in out.splitlines():
        line = line.strip()
        if line.startswith("{"):
            rows.append(json.loads(line))
        elif line.startswith("["):
            rows.extend(json.loads(line))
    return rows


def save_jobs():
    try:
        os.makedirs(os.path.dirname(JOBS_FILE), exist_ok=True)
        with JOBS_LOCK, open(JOBS_FILE + ".tmp", "w", encoding="utf-8") as f:
            json.dump(JOBS, f, ensure_ascii=False)
        os.replace(JOBS_FILE + ".tmp", JOBS_FILE)
    except OSError as e:
        print("nao foi possivel gravar o historico de execucoes: %s" % e, flush=True)


def run_job(title, *commands):
    """Roda em segundo plano: a pagina acompanha pelo /jobs, e este container nao cai junto."""
    job = {"id": secrets.token_hex(4), "title": title, "started": time.strftime("%Y-%m-%d %H:%M:%S"),
           "state": "rodando", "output": ""}
    with JOBS_LOCK:
        JOBS.insert(0, job)
        del JOBS[30:]
    save_jobs()

    def work():
        ok = True
        for args in commands:
            rc, out = compose(*args)
            job["output"] += "$ docker compose %s\n%s\n\n" % (" ".join(args), out)
            if rc != 0:
                ok = False
                break
        job["state"] = "ok" if ok else "falhou"
        job["finished"] = time.strftime("%Y-%m-%d %H:%M:%S")
        save_jobs()

    threading.Thread(target=work, daemon=True).start()
    return job


# ============================================================================== web

STYLE = """
:root{--bg:#f8fafb;--card:#fff;--fg:#17202a;--muted:#64748b;--line:#d9e2ec;--accent:#0d9488;--accent-hover:#0f766e;--accent-soft:#dff3ef;--ok:#1a7f37;--bad:#cf222e;--radius:8px}
@media (prefers-color-scheme:dark){:root{--bg:#0d1117;--card:#161b22;--fg:#e6edf3;--muted:#8b949e;--line:#30363d;--accent:#2dd4bf;--accent-hover:#5eead4;--accent-soft:rgba(45,212,191,.12);--ok:#3fb950;--bad:#f85149}}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--fg);font:14px/1.5 'Inter',-apple-system,'Segoe UI',Roboto,Arial,sans-serif;-webkit-font-smoothing:antialiased}
main{width:100%;padding:20px 24px}
h1{font-size:20px;font-weight:700;margin:0 0 4px;color:var(--fg)}
h2{font-size:16px;font-weight:600;margin:0 0 10px;color:var(--fg)}
.card{background:var(--card);border:1px solid var(--line);border-radius:var(--radius);padding:18px 20px;margin-bottom:16px;box-shadow:0 1px 2px rgba(0,0,0,.04)}
nav{display:flex;gap:4px;flex-wrap:wrap;margin-bottom:16px}
nav a{padding:8px 16px;border-radius:var(--radius);color:var(--fg);text-decoration:none;border:1px solid var(--line);background:var(--card);font-weight:500;font-size:13.5px;transition:background .15s,color .15s,border-color .15s}
nav a:hover{background:var(--accent-soft);color:var(--accent);border-color:var(--accent)}
nav a.on{background:var(--accent);color:#fff;border-color:var(--accent)}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:12px}
label{display:block;font-weight:600;font-size:13px;margin-bottom:4px;color:var(--fg)}
small{color:var(--muted);display:block;margin-top:3px;font-size:12px}
input,select{width:100%;padding:9px 12px;border:1px solid var(--line);border-radius:var(--radius);background:var(--bg);color:var(--fg);font-size:14px;font-family:inherit;transition:border-color .15s,box-shadow .15s}
input:focus,select:focus{outline:0;border-color:var(--accent);box-shadow:0 0 0 3px var(--accent-soft)}
button,.btn{padding:9px 16px;border:0;border-radius:var(--radius);background:var(--accent);color:#fff;cursor:pointer;text-decoration:none;display:inline-block;font-weight:600;font-size:13.5px;font-family:inherit;transition:background .15s}
button:hover,.btn:hover{background:var(--accent-hover)}
button.sec{background:transparent;color:var(--fg);border:1px solid var(--line)}
button.sec:hover{background:var(--accent-soft);color:var(--accent);border-color:var(--accent)}
table{width:100%;border-collapse:collapse}
td,th{padding:10px 12px;border-bottom:1px solid var(--line);text-align:left;vertical-align:top;font-size:13.5px}
th{font-weight:600;color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:.03em}
.ok{color:var(--ok)}.bad{color:var(--bad)}
pre{white-space:pre-wrap;background:var(--bg);padding:12px;border-radius:var(--radius);max-height:420px;overflow:auto;font-size:13px;border:1px solid var(--line)}
.row{display:flex;gap:8px;flex-wrap:wrap;align-items:center}
"""


def page(title, body, tab=""):
    tabs = [("config", "Configuração"), ("containers", "Containers"), ("jobs", "Execuções"), ("backup", "Backup")]
    nav = "".join('<a href="/%s" class="%s">%s</a>' % (t, "on" if t == tab else "", n) for t, n in tabs)
    nav = "<nav>%s</nav>" % nav
    return ("<!doctype html><html lang=pt-BR><head><meta charset=utf-8>"
            "<meta name=viewport content='width=device-width,initial-scale=1'><title>%s</title><style>%s</style>"
            "</head><body><main><h1>Gestão do servidor</h1>%s%s</main></body></html>"
            % (html.escape(title), STYLE, nav if tab else "", body))


class Handler(http.server.BaseHTTPRequestHandler):
    server_version = "hwmdm-admin"

    def log_message(self, fmt, *args):
        print("%s %s" % (self.address_string(), fmt % args), flush=True)

    # ---------------------------------------------------------------- utilidades
    def session(self):
        c = http.cookies.SimpleCookie(self.headers.get("Cookie", ""))
        sid = c.get("hwmdm_admin").value if c.get("hwmdm_admin") else None
        s = SESSIONS.get(sid)
        if s and s["exp"] > time.time():
            return s
        mdm = c.get("JSESSIONID")
        login = mdm_session_user(mdm.value) if mdm else None
        if login:
            sid = secrets.token_urlsafe(32)
            s = {"login": login, "exp": time.time() + SESSION_TTL, "csrf": secrets.token_hex(16)}
            SESSIONS[sid] = s
            self.new_cookie = "hwmdm_admin=%s; HttpOnly; SameSite=Lax; Path=/" % sid
            return s
        return None

    def form(self):
        n = int(self.headers.get("Content-Length") or 0)
        return {k: v[-1] for k, v in urllib.parse.parse_qs(self.rfile.read(n).decode("utf-8"),
                                                            keep_blank_values=True).items()}

    def send(self, code, body, ctype="text/html; charset=utf-8", headers=None):
        data = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        for k, v in (headers or {}).items():
            self.send_header(k, v)
        if getattr(self, "new_cookie", None) and "Set-Cookie" not in (headers or {}):
            self.send_header("Set-Cookie", self.new_cookie)
        self.end_headers()
        self.wfile.write(data)

    def redirect(self, to, headers=None):
        h = {"Location": to}
        h.update(headers or {})
        self.send(303, "", headers=h)

    def csrf_ok(self, s, f):
        return secrets.compare_digest(f.get("csrf", ""), s["csrf"])

    # ---------------------------------------------------------------- rotas
    def do_GET(self):
        path = urllib.parse.urlparse(self.path).path.rstrip("/") or "/"
        if path == "/health":
            return self.send(200, "ok", "text/plain")
        if path == "/login":
            return self.send(200, self.login_page())
        s = self.session()
        if not s:
            return self.redirect("/login")
        if path in ("/", "/config"):
            return self.send(200, self.config_page(s))
        if path == "/containers":
            return self.send(200, self.containers_page(s))
        if path == "/jobs":
            return self.send(200, self.jobs_page())
        if path == "/jobs.json":
            with JOBS_LOCK:
                return self.send(200, json.dumps(JOBS), "application/json")
        if path == "/logs":
            svc = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query).get("s", [""])[0]
            if svc not in services() + [SELF_SERVICE]:
                return self.send(404, "serviço desconhecido", "text/plain")
            _, out = compose("logs", "--no-color", "--tail", "400", svc, timeout=60)
            return self.send(200, page("Logs", '<div class=card><h2>Logs: %s</h2><pre>%s</pre>'
                                        '<a class=btn href=/containers>Voltar</a></div>'
                                        % (html.escape(svc), html.escape(out)), "containers"))
        if path == "/backup":
            return self.send(200, self.backup_page())
        return self.send(404, "não encontrado", "text/plain")

    def do_POST(self):
        path = urllib.parse.urlparse(self.path).path.rstrip("/")
        f = self.form()
        if path == "/login":
            try:
                ok = check_login(f.get("login", ""), f.get("password", ""))
            except Exception as e:
                return self.send(503, self.login_page("Banco indisponível: %s" % e))
            if not ok:
                return self.send(401, self.login_page("Usuário ou senha inválidos, ou sem perfil de administrador."))
            sid = secrets.token_urlsafe(32)
            SESSIONS[sid] = {"login": f["login"], "exp": time.time() + SESSION_TTL, "csrf": secrets.token_hex(16)}
            return self.redirect("/config", {"Set-Cookie": "hwmdm_admin=%s; HttpOnly; SameSite=Lax; Path=/" % sid})
        s = self.session()
        if not s or not self.csrf_ok(s, f):
            return self.redirect("/login")
        if path == "/logout":
            SESSIONS.pop(next((k for k, v in SESSIONS.items() if v is s), None), None)
            return self.redirect("/login")
        if path == "/config":
            known = {k for _, fs in FIELDS for k, _, _ in fs}
            changes = {k: f[k].replace("\n", " ").strip() for k in known if k in f}
            if changes.get("SERVER_MODE") not in ("dev", "prd"):
                changes["SERVER_MODE"] = "dev"
            write_env(changes)
            env, _ = read_env()
            sync_settings(env)
            msg = "Configuração salva."
            if f.get("apply") == "1":
                run_job("Aplicar configuracao", ["up", "-d", "--remove-orphans"] + services())
                msg += " Aplicando nos containers (veja Execuções)."
            return self.send(200, self.config_page(s, msg))
        if path == "/action":
            svc, act = f.get("service", ""), f.get("act", "")
            allowed = services()
            targets = allowed if svc == "*" else [svc]
            if not all(t in allowed for t in targets):
                return self.send(400, "serviço inválido", "text/plain")
            if act == "restart":
                run_job("Reiniciar %s" % svc, ["restart"] + targets)
            elif act == "stop":
                run_job("Parar %s" % svc, ["stop"] + targets)
            elif act == "up":
                run_job("Subir %s" % svc, ["up", "-d"] + targets)
            elif act == "recreate":
                run_job("Recriar %s" % svc, ["up", "-d", "--force-recreate"] + targets)
            elif act == "build":
                run_job("Reconstruir %s" % svc, ["build"] + targets, ["up", "-d"] + targets)
            elif act == "down":
                run_job("Derrubar e subir a stack", ["stop"] + targets, ["rm", "-f"] + targets,
                        ["up", "-d"] + targets)
            else:
                return self.send(400, "ação inválida", "text/plain")
            return self.redirect("/jobs")
        return self.send(404, "não encontrado", "text/plain")

    # ---------------------------------------------------------------- telas
    def login_page(self, error=""):
        return page("Login", '<div class=card style="max-width:420px"><h2>Entrar</h2>'
                    '<p><small>Mesmo usuário e senha do painel MDM (perfil Admin ou Super-Admin).</small></p>'
                    '%s<form method=post action=/login><p><label>Usuário</label><input name=login autofocus></p>'
                    '<p><label>Senha</label><input name=password type=password></p><button>Entrar</button></form></div>'
                    % ('<p class=bad>%s</p>' % html.escape(error) if error else ""))

    def config_page(self, s, msg=""):
        env, _ = read_env()
        parts = []
        for group, fields in FIELDS:
            cells = []
            for key, label, hint in fields:
                val = html.escape(env.get(key, ""))
                if key == "SERVER_MODE":
                    cur = env.get(key, "dev")
                    inp = ('<select name=SERVER_MODE><option value=dev %s>Desenvolvimento (SERVIDOR-DEV)</option>'
                           '<option value=prd %s>Produção (SERVIDOR-PRD)</option></select>'
                           % ("selected" if cur != "prd" else "", "selected" if cur == "prd" else ""))
                else:
                    t = "password" if key in SECRET_KEYS else "text"
                    inp = '<input type=%s name=%s value="%s">' % (t, key, val)
                cells.append('<div><label>%s <small style="display:inline">%s</small></label>%s<small>%s</small></div>'
                             % (html.escape(label), key, inp, html.escape(hint)))
            parts.append('<div class=card><h2>%s</h2><div class=grid>%s</div></div>' % (group, "".join(cells)))
        active = env.get("SERVER_PRD_URL") if env.get("SERVER_MODE") == "prd" else env.get("SERVER_DEV_URL")
        return page("Configuração", (
            ('<div class="card ok">%s</div>' % html.escape(msg) if msg else "")
            + '<div class=card>Servidor ativo: <b>%s</b> &mdash; página de bloqueio do Web Filter e links '
              'públicos usam este endereço.</div>' % html.escape(active or "(não definido)")
            + '<form method=post action=/config><input type=hidden name=csrf value="%s">%s'
              '<div class=row><button name=apply value=0>Salvar</button>'
              '<button name=apply value=1>Salvar e aplicar (recria os containers)</button></div></form>'
            % (s["csrf"], "".join(parts))), "config")

    def containers_page(self, s):
        rows = []
        for r in status():
            svc = r.get("Service", "")
            state = r.get("State", "")
            health = r.get("Health", "")
            cls = "ok" if state == "running" and health in ("", "healthy") else "bad"
            if svc == SELF_SERVICE:
                acts = "<small>esta gestão</small>"
            else:
                acts = "".join(
                    '<form method=post action=/action style="display:inline"><input type=hidden name=csrf value="%s">'
                    '<input type=hidden name=service value="%s"><button class=sec name=act value=%s>%s</button></form> '
                    % (s["csrf"], html.escape(svc), a, n)
                    for a, n in (("restart", "Reiniciar"), ("recreate", "Recriar"), ("stop", "Parar"),
                                 ("up", "Subir"), ("build", "Reconstruir")))
            rows.append('<tr><td><b>%s</b><br><small>%s</small></td><td class=%s>%s %s</td><td>%s</td>'
                        '<td><a href="/logs?s=%s">logs</a></td><td>%s</td></tr>'
                        % (html.escape(r.get("Name", "")), html.escape(r.get("Image", "")), cls, html.escape(state),
                           html.escape(health), html.escape(r.get("Status", "")), urllib.parse.quote(svc), acts))
        stack = ''.join(
            '<form method=post action=/action style="display:inline"><input type=hidden name=csrf value="%s">'
            '<input type=hidden name=service value="*"><button name=act value=%s>%s</button></form> '
            % (s["csrf"], a, n) for a, n in (("restart", "Reiniciar a stack"), ("down", "Derrubar e subir a stack"),
                                              ("build", "Reconstruir e subir a stack")))
        return page("Containers", '<div class=card><h2>Stack %s</h2><div class=row>%s</div></div>'
                    '<div class=card style="overflow-x:auto"><table><tr><th>Container</th><th>Estado</th><th>Status</th>'
                    '<th></th><th>Ações</th></tr>%s</table></div>' % (html.escape(PROJECT), stack, "".join(rows)),
                    "containers")

    def jobs_page(self):
        return page("Execuções", '<div class=card><h2>Execuções</h2><div id=j>carregando...</div></div><script>'
                    'function esc(t){return (t||"").replace(/[&<>]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;"}[c]))}'
                    'async function f(){try{const r=await fetch("/jobs.json");const js=await r.json();'
                    'document.getElementById("j").innerHTML=js.length?js.map(j=>"<h3>"+esc(j.title)+" &mdash; "'
                    '+esc(j.state)+" <small style=display:inline>"+esc(j.started)+"</small></h3><pre>"+esc(j.output)+"</pre>").join(""):'
                    '"Nenhuma execução."}catch(e){}setTimeout(f,2000)}f()</script>', "jobs")

    def backup_page(self):
        env, _ = read_env()
        active = env.get("SERVER_PRD_URL") if env.get("SERVER_MODE") == "prd" else env.get("SERVER_DEV_URL")
        return page("Backup", '<div class=card><h2>Backup</h2><p>O backup do banco e dos arquivos é o módulo '
                    'que já existe no painel MDM (criar, agendar, baixar, enviar, restaurar). Os arquivos ficam em '
                    '<code>source/volumes/backups</code>, fora dos containers.</p>'
                    '<a class=btn target=_top href="%s/#/governance">Abrir o Backup do MDM</a></div>'
                    % html.escape((active or "").rstrip("/")), "backup")


class Server(socketserver.ThreadingMixIn, http.server.HTTPServer):
    daemon_threads = True
    allow_reuse_address = True


if __name__ == "__main__":
    for attempt in range(60):
        try:
            ensure_schema()
            sync_settings(read_env()[0])
            break
        except Exception as e:
            print("banco ainda indisponivel: %s" % e, flush=True)
            time.sleep(5)
    print("hwmdm-admin ouvindo na porta %d" % PORT, flush=True)
    Server(("0.0.0.0", PORT), Handler).serve_forever()
