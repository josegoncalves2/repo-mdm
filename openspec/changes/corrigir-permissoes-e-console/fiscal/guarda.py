#!/usr/bin/env python3
"""Guarda fiscal do change corrigir-permissoes-e-console.

Controle mecanico -- nao julga qualidade, so recusa o que as regras proibem:

  snapshot <id> <n> --allow GLOB...   fotografa o repositorio antes do EXECUTOR
  diff <id> <n>                       escopo, arquivos de teste, arquivos apagados,
                                      referencia a producao, placeholder; lista
                                      linhas removidas para justificativa
  fechar <id> <n>                     marca a tentativa como encerrada
  manifesto <pasta>                   grava MANIFESTO.sha256 da evidencia
  conferir-manifesto <pasta>          confere a evidencia contra o manifesto
  registrar "<texto>"                 acrescenta entrada ao ledger encadeado
  verificar-ledger                    recalcula a cadeia do ledger
  aceite [--tarefa <id>]              confere tudo o que o [x] exige

Saida != 0 significa violacao. Evidencias ficam fora do repositorio, em
$HWMDM_EVIDENCIAS (padrao /opt/projetos/hwmdm/evidencias/corrigir-permissoes-e-console).
"""
import argparse
import difflib
import hashlib
import json
import os
import re
import shutil
import sys
from datetime import datetime
from pathlib import Path

FISCAL = Path(__file__).resolve().parent
CHANGE = FISCAL.parent
REPO = CHANGE.parents[2]
NOME_CHANGE = CHANGE.name
EVID = Path(os.environ.get("HWMDM_EVIDENCIAS",
                           f"/opt/projetos/hwmdm/evidencias/{NOME_CHANGE}"))
LEDGER = FISCAL / "ledger.md"
TASKS = CHANGE / "tasks.md"

# Pastas que nao sao codigo do produto (toolchain vendorizado, runtime, build).
IGNORAR_DIRS = {".git", ".jdk21", ".maven", ".android-sdk", "node_modules",
                "__pycache__", "build", "target", ".gradle", ".idea"}
IGNORAR_PREFIXOS = ("source/volumes/",)
# O proprio fiscal escreve aqui durante o ciclo; nao e alteracao de executor.
FISCAL_REL = str(FISCAL.relative_to(REPO)) + "/"

TESTE = re.compile(r"(^|/)(src/test/|androidTest/|__tests__/|tests?/)"
                   r"|Tests?\.(java|kt)$|(^|/)test_[^/]*\.py$|_test\.py$"
                   r"|\.(spec|test)\.[jt]sx?$")
PRODUCAO = re.compile(r"192\.168\.1\.75|mdm\.olimpia\.sp\.gov\.br")
PLACEHOLDER = re.compile(r"\b(TODO|FIXME|XXX|placeholder|not implemented)\b", re.I)
SUSPEITOS_DE_SUMICO = re.compile(r"ng-(if|show|hide)=|display:\s*none|visibility:\s*hidden")


def glob_re(glob):
    """Glob com ** (qualquer profundidade), * e ? (sem cruzar /)."""
    out, i = "", 0
    while i < len(glob):
        c = glob[i]
        if glob.startswith("**/", i):
            out += "(?:.*/)?"; i += 3; continue
        if glob.startswith("**", i):
            out += ".*"; i += 2; continue
        out += {"*": "[^/]*", "?": "[^/]"}.get(c, re.escape(c)); i += 1
    return re.compile("^" + out + "$")


def casa(path, globs):
    return any(glob_re(g).match(path) for g in globs)


def sha256(p):
    h = hashlib.sha256()
    with open(p, "rb") as f:
        for bloco in iter(lambda: f.read(1 << 20), b""):
            h.update(bloco)
    return h.hexdigest()


def arquivos_do_repo():
    out = {}
    for raiz, dirs, nomes in os.walk(REPO):
        dirs[:] = [d for d in dirs if d not in IGNORAR_DIRS]
        for n in nomes:
            p = Path(raiz) / n
            rel = str(p.relative_to(REPO))
            if rel.startswith(IGNORAR_PREFIXOS) or rel.startswith(FISCAL_REL):
                continue
            if p.is_symlink() or not p.is_file():
                continue
            out[rel] = sha256(p)
    return out


def pasta(tarefa, n):
    return EVID / tarefa / f"t{n}"


def agora():
    return datetime.now().astimezone().isoformat(timespec="seconds")


# ------------------------------------------------------------------ ledger
CAB = ("# Ledger fiscal -- corrigir-permissoes-e-console\n\n"
       "Encadeado: hash = sha256(hash_anterior|horario|texto). Nao edite a mao; "
       "use `guarda.py registrar`. `guarda.py verificar-ledger` recusa adulteracao.\n\n"
       "| # | Horario | Texto | Hash anterior | Hash |\n|---|---|---|---|---|\n")
LINHA = re.compile(r"^\| (\d+) \| ([^|]+) \| (.*) \| ([0-9a-f]{64}|GENESE) \| ([0-9a-f]{64}) \|$")


def ler_ledger():
    if not LEDGER.exists():
        return []
    return [m.groups() for m in map(LINHA.match, LEDGER.read_text().splitlines()) if m]


def registrar(texto):
    texto = texto.replace("|", "/").replace("\n", " ").strip()
    ent = ler_ledger()
    ant = ent[-1][4] if ent else "GENESE"
    ts = agora()
    h = hashlib.sha256(f"{ant}|{ts}|{texto}".encode()).hexdigest()
    if not LEDGER.exists():
        LEDGER.write_text(CAB)
    with open(LEDGER, "a") as f:
        f.write(f"| {len(ent) + 1} | {ts} | {texto} | {ant} | {h} |\n")
    return h


def verificar_ledger():
    erros, ant = [], "GENESE"
    for i, (num, ts, texto, a, h) in enumerate(ler_ledger(), 1):
        if int(num) != i:
            erros.append(f"entrada {num}: numeracao quebrada (esperado {i})")
        if a != ant:
            erros.append(f"entrada {num}: hash anterior nao confere")
        if hashlib.sha256(f"{a}|{ts.strip()}|{texto}".encode()).hexdigest() != h:
            erros.append(f"entrada {num}: conteudo adulterado")
        ant = h
    return erros


# ------------------------------------------------------------------ snapshot / diff
def cmd_snapshot(a):
    d = pasta(a.tarefa, a.n)
    if (d / "snapshot.json").exists():
        sys.exit(f"ERRO snapshot de {a.tarefa} t{a.n} ja existe -- tentativa nova exige n novo")
    if not a.allow:
        sys.exit("ERRO --allow obrigatorio (escopo da OS)")
    arqs = arquivos_do_repo()
    copia = d / "snapshot" / "arquivos"
    for rel in arqs:
        if casa(rel, a.allow):
            (copia / rel).parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(REPO / rel, copia / rel)
    d.mkdir(parents=True, exist_ok=True)
    (d / "snapshot.json").write_text(json.dumps(
        {"tarefa": a.tarefa, "tentativa": a.n, "criado": agora(),
         "allow": a.allow, "arquivos": arqs}, indent=0))
    registrar(f"snapshot {a.tarefa} t{a.n} allow={' '.join(a.allow)} arquivos={len(arqs)}")
    print(f"ok snapshot {d / 'snapshot.json'} ({len(arqs)} arquivos)")


def outras_tentativas_abertas(eu, criado):
    """Globs de outras tarefas que estavam abertas junto com esta."""
    globs = []
    for s in EVID.glob("*/t*/snapshot.json"):
        if s.parent == eu:
            continue
        fechado = s.parent / "fechado"
        if fechado.exists() and fechado.read_text().strip() < criado:
            continue
        globs += json.loads(s.read_text())["allow"]
    return globs


def ler_texto(p):
    try:
        return p.read_text(encoding="utf-8").splitlines()
    except (UnicodeDecodeError, FileNotFoundError):
        return None


def cmd_diff(a):
    d = pasta(a.tarefa, a.n)
    snap = json.loads((d / "snapshot.json").read_text())
    antes, depois, allow = snap["arquivos"], arquivos_do_repo(), snap["allow"]
    alheios = outras_tentativas_abertas(d, snap["criado"])
    novos = sorted(set(depois) - set(antes))
    apagados = sorted(set(antes) - set(depois))
    mudados = sorted(p for p in set(antes) & set(depois) if antes[p] != depois[p])

    viol, avisos, removidas, rel = [], [], [], []
    for p in novos + mudados + apagados:
        if not casa(p, allow):
            if casa(p, alheios):
                avisos.append(f"{p}: alterado por outra tarefa aberta")
                continue
            viol.append(f"FORA_DO_ESCOPO {p}")
        if p.startswith("openspec/"):
            viol.append(f"PROTEGIDO {p}")
        if TESTE.search(p):
            viol.append(f"TESTE_AUTOMATIZADO {p}")
    for p in apagados:
        viol.append(f"ARQUIVO_APAGADO {p} (nada pode desaparecer -- decisao humana)")

    for p in novos + mudados:
        if not casa(p, allow):
            continue
        velho = ler_texto(d / "snapshot" / "arquivos" / p) if p in antes else []
        novo = ler_texto(REPO / p)
        if novo is None or velho is None:
            rel.append(f"### {p}\n(binario: sha256 {antes.get(p, '-')} -> {depois[p]})\n")
            continue
        linhas = list(difflib.unified_diff(velho, novo, f"a/{p}", f"b/{p}", lineterm="", n=2))
        rel.append(f"### {p}\n```diff\n" + "\n".join(linhas) + "\n```\n")
        for ln in linhas:
            if ln.startswith("+") and not ln.startswith("+++"):
                if PRODUCAO.search(ln):
                    viol.append(f"PRODUCAO {p}: {ln[1:].strip()[:120]}")
                if PLACEHOLDER.search(ln):
                    viol.append(f"PLACEHOLDER {p}: {ln[1:].strip()[:120]}")
                if SUSPEITOS_DE_SUMICO.search(ln):
                    avisos.append(f"{p}: linha nova pode esconder algo -- conferir: {ln[1:].strip()[:120]}")
            elif ln.startswith("-") and not ln.startswith("---") and ln[1:].strip():
                removidas.append(f"{p}: {ln[1:].strip()[:160]}")

    out = [f"# Guarda diff {a.tarefa} t{a.n} -- {agora()}\n",
           f"Snapshot: {snap['criado']} | escopo: `{' '.join(allow)}`\n",
           f"Novos: {len(novos)} | alterados: {len(mudados)} | apagados: {len(apagados)}\n",
           "## Violacoes\n" + ("\n".join(f"- {v}" for v in viol) or "- nenhuma") + "\n",
           "## Avisos (o VALIDADOR confere cada um)\n" + ("\n".join(f"- {v}" for v in avisos) or "- nenhum") + "\n",
           "## Linhas removidas (cada uma exige justificativa no relatorio do EXECUTOR)\n"
           + ("\n".join(f"- {r}" for r in removidas) or "- nenhuma") + "\n",
           "## Diff\n"] + rel
    # A primeira execucao fica em diff.md (entra no manifesto); reexecucoes, como a do
    # VALIDADOR, nao podem reescreve-la, senao o manifesto deixa de conferir.
    alvo = d / "diff.md"
    if alvo.exists():
        alvo = d / f"diff-reexecucao-{datetime.now():%Y%m%d-%H%M%S}.md"
    alvo.write_text("\n".join(out))
    registrar(f"diff {a.tarefa} t{a.n} violacoes={len(viol)} removidas={len(removidas)}")
    print("\n".join(out[:6]))
    sys.exit(1 if viol else 0)


def cmd_fechar(a):
    d = pasta(a.tarefa, a.n)
    (d / "fechado").write_text(agora())
    registrar(f"fechar {a.tarefa} t{a.n}")
    print("ok")


# ------------------------------------------------------------------ manifesto
def cmd_manifesto(a):
    d = Path(a.pasta)
    linhas = []
    for p in sorted(d.rglob("*")):
        if p.is_file() and p.name != "MANIFESTO.sha256" and "snapshot" not in p.relative_to(d).parts:
            linhas.append(f"{sha256(p)}  {p.relative_to(d)}")
    (d / "MANIFESTO.sha256").write_text("\n".join(linhas) + "\n")
    h = sha256(d / "MANIFESTO.sha256")
    registrar(f"manifesto {d} arquivos={len(linhas)} sha256={h}")
    print(f"ok {len(linhas)} arquivos; sha256 do manifesto: {h}")


def conferir_manifesto(d):
    m = d / "MANIFESTO.sha256"
    if not m.exists():
        return [f"{m} ausente"]
    erros = []
    for ln in m.read_text().splitlines():
        h, rel = ln.split("  ", 1)
        p = d / rel
        if not p.exists():
            erros.append(f"evidencia sumiu: {rel}")
        elif sha256(p) != h:
            erros.append(f"evidencia alterada: {rel}")
    return erros


def cmd_conferir(a):
    erros = conferir_manifesto(Path(a.pasta))
    print("\n".join(erros) or "ok manifesto confere")
    sys.exit(1 if erros else 0)


# ------------------------------------------------------------------ aceite
SEM_JORNADA = {"3.1"}


def campo(texto, nome):
    m = re.search(rf"^{nome}:\s*(.+?)\s*$", texto, re.M)
    return m.group(1) if m else ""


def conferir_tarefa(tid):
    erros = []
    pareceres = sorted(FISCAL.glob(f"pareceres/{tid}-t*.md"),
                       key=lambda p: int(re.search(r"-t(\d+)\.md$", p.name).group(1)))
    if not pareceres:
        return [f"{tid}: sem parecer do VALIDADOR"]
    par = pareceres[-1]
    n = re.search(r"-t(\d+)\.md$", par.name).group(1)
    txt = par.read_text()
    if campo(txt, "Veredito") != "ACEITA":
        erros.append(f"{tid}: ultimo parecer ({par.name}) nao e ACEITA")
    papeis = {k: campo(txt, k) for k in ("Executor", "Ambiente", "Usuario", "Validador")}
    val = papeis["Validador"]
    if not val.startswith("VAL-"):
        erros.append(f"{tid}: Validador ausente ou sem rotulo VAL-")
    if val and val in [v for k, v in papeis.items() if k != "Validador"]:
        erros.append(f"{tid}: validador coincide com outro papel")
    ledger = LEDGER.read_text() if LEDGER.exists() else ""
    for k, v in papeis.items():
        if v and v != "-" and f"despacho {v} " not in ledger:
            erros.append(f"{tid}: {k} {v} sem registro de despacho no ledger (sub-agente real?)")
    d = pasta(tid, n)
    ev = campo(txt, "Evidencia")
    if ev and Path(ev).resolve() != d.resolve():
        erros.append(f"{tid}: Evidencia do parecer ({ev}) difere de {d}")
    erros += [f"{tid}: {e}" for e in conferir_manifesto(d)]
    if tid not in SEM_JORNADA:
        for sub in ("jornada/video", "jornada/capturas", "validador"):
            if not (d / sub).is_dir() or not any((d / sub).rglob("*")):
                erros.append(f"{tid}: {sub}/ vazio ou ausente (sem prova de uso real)")
        if not (d / "jornada" / "inventario-pos.md").exists():
            erros.append(f"{tid}: jornada/inventario-pos.md ausente (nada-desaparece nao conferido)")
    if (d / "snapshot.json").exists() and not (d / "diff.md").exists():
        erros.append(f"{tid}: houve snapshot mas guarda diff nao rodou")
    ass = FISCAL / "assinaturas" / f"{tid}.md"
    if not ass.exists():
        erros.append(f"{tid}: sem assinatura humana ({ass.name})")
    else:
        at = ass.read_text()
        if not campo(at, "Assinado por") or "<" in campo(at, "Assinado por"):
            erros.append(f"{tid}: assinatura sem nome")
        if campo(at, "Tentativa") != f"t{n}":
            erros.append(f"{tid}: assinatura e de outra tentativa")
        man = d / "MANIFESTO.sha256"
        if man.exists() and campo(at, "Manifesto sha256") != sha256(man):
            erros.append(f"{tid}: sha256 do manifesto na assinatura nao confere")
    if tid.startswith("3.") and tid != "3.1" and not (FISCAL / "assinaturas" / "3.1-matriz.md").exists():
        erros.append(f"{tid}: matriz 3.1 sem aprovacao humana")
    return erros


def cmd_aceite(a):
    erros = verificar_ledger()
    marcadas = re.findall(r"^- \[x\] (\d+\.\d+)", TASKS.read_text(), re.M)
    alvo = [a.tarefa] if a.tarefa else marcadas
    for tid in alvo:
        erros += conferir_tarefa(tid)
    print("\n".join(erros) or f"ok aceite conferido: {', '.join(alvo) or 'nenhuma tarefa marcada'}")
    sys.exit(1 if erros else 0)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sp = ap.add_subparsers(dest="cmd", required=True)
    s = sp.add_parser("snapshot"); s.add_argument("tarefa"); s.add_argument("n", type=int)
    s.add_argument("--allow", nargs="+", default=[])
    for nome in ("diff", "fechar"):
        s = sp.add_parser(nome); s.add_argument("tarefa"); s.add_argument("n", type=int)
    for nome in ("manifesto", "conferir-manifesto"):
        sp.add_parser(nome).add_argument("pasta")
    sp.add_parser("registrar").add_argument("texto")
    sp.add_parser("verificar-ledger")
    sp.add_parser("aceite").add_argument("--tarefa")
    a = ap.parse_args()
    if a.cmd == "registrar":
        print(registrar(a.texto))
    elif a.cmd == "verificar-ledger":
        e = verificar_ledger(); print("\n".join(e) or "ok ledger integro"); sys.exit(1 if e else 0)
    else:
        {"snapshot": cmd_snapshot, "diff": cmd_diff, "fechar": cmd_fechar,
         "manifesto": cmd_manifesto, "conferir-manifesto": cmd_conferir,
         "aceite": cmd_aceite}[a.cmd](a)


if __name__ == "__main__":
    main()
