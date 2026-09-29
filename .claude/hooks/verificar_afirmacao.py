#!/usr/bin/env python3
"""Hook Stop: se a ultima mensagem do assistente afirma conclusao, confere o ambiente real.

Checagens: 3 containers com nome exato e running; volumes nomeados; painel HTTP em
localhost:8080; ao menos 1 evento em plugin_webfilter_events nas ultimas 24h.
Anti-loop: com stop_hook_active, bloqueia no maximo MAX_BLOQUEIOS vezes por sessao.
"""
import json, os, re, subprocess, sys, time, urllib.request

CONTAINERS = ["hwmdm-webfilter", "hwmdm-mdm", "hwmdm-postgres"]
VOLUMES = [os.environ.get("HWMDM_VOL_PG", "hwmdm_postgresql-data"),
           "hwmdm-webfilter_webfilter-lists", "hwmdm-webfilter_webfilter-state"]
PAINEL = os.environ.get("HWMDM_PAINEL_URL", "http://localhost:8080/")
SQL_USER = os.environ.get("SQL_USER", "hmdm")
SQL_BASE = os.environ.get("SQL_BASE", "hmdm")
MAX_BLOQUEIOS = 2
PALAVRAS = re.compile(r"\b(pronto|funcionando|conclu[ií]do|feito|validado|done|working)\b", re.I)


def ultima_msg_assistente(path):
    texto = ""
    try:
        with open(path, encoding="utf-8") as f:
            for linha in f:
                try:
                    ev = json.loads(linha)
                except ValueError:
                    continue
                msg = ev.get("message") or {}
                if ev.get("type") != "assistant" and msg.get("role") != "assistant":
                    continue
                c = msg.get("content")
                partes = [c] if isinstance(c, str) else [
                    b.get("text", "") for b in (c or []) if isinstance(b, dict) and b.get("type") == "text"]
                t = "\n".join(p for p in partes if p)
                if t.strip():
                    texto = t
    except OSError:
        pass
    return texto


def sh(cmd, timeout=15):
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        return r.returncode, r.stdout.strip(), r.stderr.strip()
    except Exception as e:  # noqa: BLE001
        return 1, "", str(e)


def checar():
    falhas = []
    for c in CONTAINERS:
        rc, out, err = sh(["docker", "inspect", "-f", "{{.Name}} {{.State.Status}}", c])
        if rc != 0:
            falhas.append(f"container {c} inexistente ({err[:120]})")
        elif out != f"/{c} running":
            falhas.append(f"container {c} nao esta running: '{out}'")
    for v in VOLUMES:
        rc, _, err = sh(["docker", "volume", "inspect", v])
        if rc != 0:
            falhas.append(f"volume {v} inexistente")
    try:
        with urllib.request.urlopen(PAINEL, timeout=10) as r:
            if r.status >= 500:
                falhas.append(f"painel {PAINEL} HTTP {r.status}")
    except urllib.error.HTTPError as e:
        if e.code >= 500:
            falhas.append(f"painel {PAINEL} HTTP {e.code}")
    except Exception as e:  # noqa: BLE001
        falhas.append(f"painel {PAINEL} nao responde: {e}")
    desde = int((time.time() - 86400) * 1000)
    rc, out, err = sh(["docker", "exec", "hwmdm-postgres", "psql", "-U", SQL_USER, "-d", SQL_BASE, "-Atc",
                       f"select count(*) from plugin_webfilter_events where createdat >= {desde}"])
    if rc != 0:
        falhas.append(f"consulta de eventos de bloqueio falhou: {err[:160]}")
    elif not out.isdigit() or int(out) < 1:
        falhas.append(f"nenhum evento de bloqueio em plugin_webfilter_events nas ultimas 24h (count={out})")
    return falhas


def main():
    try:
        dados = json.load(sys.stdin)
    except ValueError:
        return 0
    texto = ultima_msg_assistente(dados.get("transcript_path", ""))
    if not PALAVRAS.search(texto):
        return 0
    sessao = re.sub(r"[^A-Za-z0-9_-]", "", str(dados.get("session_id", "sem-sessao")))
    contador = f"/tmp/verificar_afirmacao_{sessao}.count"
    n = 0
    if dados.get("stop_hook_active"):
        try:
            n = int(open(contador).read().strip() or 0)
        except (OSError, ValueError):
            n = 0
    falhas = checar()
    if not falhas:
        try:
            os.remove(contador)
        except OSError:
            pass
        return 0
    if n >= MAX_BLOQUEIOS:
        sys.stderr.write("verificar_afirmacao: limite de bloqueios atingido; falhas: " + "; ".join(falhas) + "\n")
        return 0
    with open(contador, "w") as f:
        f.write(str(n + 1))
    print(json.dumps({"decision": "block", "reason":
                      "Afirmacao de conclusao nao confere com o ambiente. Falhou: " + "; ".join(falhas)},
                     ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
