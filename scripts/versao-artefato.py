#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Fonte unica de verdade para a versao de qualquer artefato do hwmdm.

Por que este script existe: ate hoje nao havia como olhar um WAR ou um APK em dist/
e dizer de qual commit ele veio. Sete scripts de build soltos na raiz (build.py,
build_maven.py, ...) escreviam artefatos sem carimbo nenhum, e mais de uma sessao
de agente ja trocou dist/hmdm.war por um backup antigo sem ninguem perceber (ver o
incidente de 2026-09-21 no relatorio da tarefa C). Este script e' chamado tanto ANTES
de compilar (subcomando "fonte", para saber que versao carimbar) quanto DEPOIS
(subcomando "artefato", para auditar um arquivo que ja existe em dist/ ou em
qualquer outro lugar). scripts/build.sh usa os dois; scripts/versao-publicada.sh usa
"artefato" contra o que esta rodando no DEV.

Roda sem rede e com a arvore suja de proposito: um build feito em cima de mudancas
nao commitadas nao pode fingir que corresponde a um commit limpo, mas tambem nao
pode travar so' porque alguem esqueceu de commitar. Nesses casos o commit vem
marcado com "+sujo".

Uso:
    scripts/versao-artefato.py fonte console
    scripts/versao-artefato.py fonte launcher
    scripts/versao-artefato.py fonte agente-remoto
    scripts/versao-artefato.py artefato dist/hmdm.war
    scripts/versao-artefato.py artefato dist/hwmdm-remote-1.17.apk
    scripts/versao-artefato.py ferramentas
    scripts/versao-artefato.py manifesto --alvo console --artefato dist/hmdm.war
"""
import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import zipfile
from datetime import datetime, timezone
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
BUILDS_DIR = RAIZ / "scripts" / "builds"
ALVOS = ("console", "launcher", "agente-remoto")


# --------------------------------------------------------------------- git / data

def git_commit_curto():
    try:
        r = subprocess.run(
            ["git", "rev-parse", "--short", "HEAD"],
            cwd=RAIZ, capture_output=True, text=True, timeout=15,
        )
        if r.returncode != 0:
            return "desconhecido"
        return r.stdout.strip() or "desconhecido"
    except Exception:
        return "desconhecido"


def arvore_suja():
    try:
        r = subprocess.run(
            ["git", "status", "--porcelain"],
            cwd=RAIZ, capture_output=True, text=True, timeout=30,
        )
        if r.returncode != 0:
            return None  # nao deu pra checar -- nao inventamos "limpo"
        return bool(r.stdout.strip())
    except Exception:
        return None


def commit_marcado():
    commit = git_commit_curto()
    sujo = arvore_suja()
    if sujo:
        return f"{commit}+sujo"
    return commit


def data_utc():
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


# --------------------------------------------------------------------- fonte (pre-build)

def _ler_versao_console():
    arq = RAIZ / "scripts" / "versao-console.txt"
    if not arq.is_file():
        return None
    for linha in arq.read_text(encoding="utf-8").splitlines():
        linha = linha.strip()
        if linha and not linha.startswith("#"):
            return linha
    return None


def _ler_versao_gradle(caminho_build_gradle):
    texto = Path(caminho_build_gradle).read_text(encoding="utf-8")
    m_nome = re.search(r'versionName\s+"([^"]+)"', texto)
    m_codigo = re.search(r"versionCode\s+(\d+)", texto)
    return (
        m_nome.group(1) if m_nome else None,
        int(m_codigo.group(1)) if m_codigo else None,
    )


def fonte(alvo):
    if alvo not in ALVOS:
        sys.exit(f"alvo desconhecido: {alvo!r} (use um de: {', '.join(ALVOS)})")

    info = {
        "alvo": alvo,
        "commit": commit_marcado(),
        "data_build_utc": data_utc(),
    }

    if alvo == "console":
        info["versao"] = _ler_versao_console()
        info["origem_versao"] = "scripts/versao-console.txt"
    elif alvo == "launcher":
        nome, codigo = _ler_versao_gradle(RAIZ / "android-source/app/build.gradle")
        info["versao"] = nome
        info["version_code"] = codigo
        info["origem_versao"] = "android-source/app/build.gradle (versionName/versionCode)"
    elif alvo == "agente-remoto":
        nome, codigo = _ler_versao_gradle(RAIZ / "remote-agent/app/build.gradle")
        info["versao"] = nome
        info["version_code"] = codigo
        info["origem_versao"] = "remote-agent/app/build.gradle (versionName/versionCode)"

    return info


# --------------------------------------------------------------------- artefato (pos-build)

def sha256_arquivo(caminho):
    h = hashlib.sha256()
    with open(caminho, "rb") as f:
        for bloco in iter(lambda: f.read(1 << 20), b""):
            h.update(bloco)
    return h.hexdigest()


def detectar_alvo(caminho):
    nome = Path(caminho).name.lower()
    if nome.endswith(".war"):
        return "console"
    if nome.endswith(".apk") and "remote" in nome:
        return "agente-remoto"
    if nome.endswith(".apk"):
        return "launcher"
    return None


def _ler_carimbo_war(caminho):
    """Le server-source/server/src/main/webapp/build-info.json de DENTRO do WAR.

    Esse arquivo e' um asset estatico comum, copiado para a raiz do WAR pelo
    empacotamento do Maven e tambem pela sobreposicao que o docker-entrypoint.sh
    aplica em cima da imagem publicada (ver APPLY_CUSTOM_WEBAPP_ON_BOOT em
    source/docker-compose.yaml). Se nao existir, o WAR e' de um build anterior a
    esta tarefa -- isso e' esperado para dist/hmdm.war hoje, e o retorno None diz
    isso com honestidade em vez de inventar um valor.
    """
    try:
        with zipfile.ZipFile(caminho) as z:
            with z.open("build-info.json") as f:
                return json.load(f)
    except KeyError:
        return None
    except Exception as e:
        return {"_erro_leitura": str(e)}


def _aapt2_path():
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or str(Path.home() / "android-sdk")
    candidatos = sorted(Path(sdk).glob("build-tools/*/aapt2"))
    return str(candidatos[-1]) if candidatos else None


def _ler_badging_apk(caminho):
    aapt2 = _aapt2_path()
    if not aapt2:
        return {"_erro": "aapt2 nao encontrado -- defina ANDROID_HOME/ANDROID_SDK_ROOT"}
    try:
        r = subprocess.run(
            [aapt2, "dump", "badging", str(caminho)],
            capture_output=True, text=True, timeout=60,
        )
    except Exception as e:
        return {"_erro": f"aapt2 falhou ao rodar: {e}"}
    if r.returncode != 0:
        return {"_erro": f"aapt2 dump badging saiu com codigo {r.returncode}: {r.stderr.strip()[:300]}"}
    texto = r.stdout
    pkg = re.search(r"package: name='([^']+)'", texto)
    vname = re.search(r"versionName='([^']+)'", texto)
    vcode = re.search(r"versionCode='([^']+)'", texto)
    return {
        "pacote": pkg.group(1) if pkg else None,
        "versao": vname.group(1) if vname else None,
        "version_code": vcode.group(1) if vcode else None,
    }


def procurar_manifesto(sha256):
    """Acha, em scripts/builds/, o manifesto cujo sha256_artefato bate com o arquivo.

    E' assim que scripts/versao-publicada.sh liga "o que esta rodando" a "quem
    construiu e quando" sem precisar reconstruir nada.
    """
    if not BUILDS_DIR.is_dir():
        return None
    for arq in sorted(BUILDS_DIR.glob("*.json"), reverse=True):
        try:
            dados = json.loads(arq.read_text(encoding="utf-8"))
        except Exception:
            continue
        if dados.get("sha256_artefato") == sha256:
            dados = dict(dados)
            dados["_manifesto"] = str(arq.relative_to(RAIZ))
            return dados
    return None


def artefato(caminho, alvo=None):
    caminho = Path(caminho)
    if not caminho.is_file():
        sys.exit(f"arquivo nao encontrado: {caminho}")

    alvo = alvo or detectar_alvo(caminho)
    sha = sha256_arquivo(caminho)

    resultado = {
        "arquivo": str(caminho),
        "alvo": alvo,
        "sha256": sha,
        "tamanho_bytes": caminho.stat().st_size,
    }

    carimbo = None
    badging = None
    if alvo == "console":
        carimbo = _ler_carimbo_war(caminho)
        resultado["carimbo_embutido"] = carimbo
    elif alvo in ("launcher", "agente-remoto"):
        badging = _ler_badging_apk(caminho)
        resultado["badging"] = badging

    manifesto = procurar_manifesto(sha)
    resultado["manifesto_correspondente"] = manifesto

    # Resumo com uma unica versao/commit/data "oficial" -- na ordem de confianca:
    # manifesto de build (mais forte, porque foi escrito no momento exato do build)
    # > carimbo embutido no proprio artefato > versionName do APK (sem commit/data)
    # > nada (artefato de antes desta tarefa, sem carimbo e sem manifesto).
    if manifesto:
        resultado["versao"] = manifesto.get("versao")
        resultado["commit"] = manifesto.get("commit")
        resultado["data_build_utc"] = manifesto.get("data_build_utc")
        resultado["origem"] = f"manifesto {manifesto['_manifesto']}"
    elif carimbo and isinstance(carimbo, dict) and "_erro_leitura" not in carimbo:
        resultado["versao"] = carimbo.get("versao")
        resultado["commit"] = carimbo.get("commit")
        resultado["data_build_utc"] = carimbo.get("data_build_utc")
        resultado["origem"] = "build-info.json dentro do WAR"
    elif badging and isinstance(badging, dict) and "_erro" not in badging:
        resultado["versao"] = badging.get("versao")
        resultado["commit"] = None
        resultado["data_build_utc"] = None
        resultado["origem"] = "versionName do APK (sem manifesto correspondente -- nao ha commit/data confiaveis)"
    else:
        resultado["versao"] = None
        resultado["commit"] = None
        resultado["data_build_utc"] = None
        resultado["origem"] = "desconhecida -- artefato sem carimbo e sem manifesto em scripts/builds/"

    return resultado


# --------------------------------------------------------------------- apkhash do banco

def apkhash_para_hex(valor):
    """Converte applicationversions.apkhash (base64, formato inconsistente entre
    scripts diferentes ao longo do tempo -- as vezes com '+/' as vezes com '-_',
    as vezes com '=' de preenchimento, as vezes sem) para sha256 hex, do jeito
    que scripts/build.sh e scripts/versao-artefato.py gravam nos manifestos.

    Devolve None se o valor estiver vazio ou nao for decodificavel -- honesto em
    vez de arriscar um hex errado.
    """
    import base64

    if not valor:
        return None
    texto = valor.strip()
    if not texto:
        return None
    padded = texto + "=" * (-len(texto) % 4)
    for variante in (padded, padded.replace("-", "+").replace("_", "/")):
        try:
            cru = base64.b64decode(variante, validate=False)
        except Exception:
            continue
        if len(cru) == 32:  # sha256 tem 32 bytes
            return cru.hex()
    return None


# --------------------------------------------------------------------- ferramentas

def _primeira_linha(cmd, timeout=20):
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        saida = (r.stdout or "") + (r.stderr or "")
        for linha in saida.splitlines():
            linha = linha.strip()
            if linha:
                return linha
        return None
    except Exception as e:
        return f"nao foi possivel executar {cmd[0]}: {e}"


def _versao_gradle_wrapper(caminho_properties):
    """Le a versao do Gradle do wrapper SEM executar o gradlew.

    De proposito: rodar gradlew so' para perguntar --version disputaria o mesmo
    daemon que um build de verdade estiver usando (ver aviso em scripts/build.sh
    sobre nao concorrer com outro executor compilando remote-agent).
    """
    try:
        texto = Path(caminho_properties).read_text(encoding="utf-8")
    except Exception:
        return None
    m = re.search(r"gradle-([0-9.]+)-bin\.zip", texto)
    return m.group(1) if m else None


def ferramentas():
    java_home = os.environ.get("JAVA_HOME", "/usr/lib/jvm/java-21-openjdk-amd64")
    java_bin = str(Path(java_home) / "bin" / "java")
    if not Path(java_bin).is_file():
        java_bin = "java"
    return {
        "java": _primeira_linha([java_bin, "-version"]),
        "maven": _primeira_linha([str(RAIZ / ".maven/bin/mvn"), "-v"]),
        "gradle_wrapper_remote_agent": _versao_gradle_wrapper(
            RAIZ / "remote-agent/gradle/wrapper/gradle-wrapper.properties"
        ),
        "gradle_wrapper_launcher": _versao_gradle_wrapper(
            RAIZ / "android-source/gradle/wrapper/gradle-wrapper.properties"
        ),
    }


# --------------------------------------------------------------------- manifesto

def gravar_manifesto(alvo, artefato_path=None, versao=None, nota=None):
    BUILDS_DIR.mkdir(parents=True, exist_ok=True)

    base = fonte(alvo) if alvo in ALVOS else {
        "alvo": alvo,
        "commit": commit_marcado(),
        "data_build_utc": data_utc(),
    }

    sha = None
    tamanho = None
    caminho_relativo = None
    if artefato_path:
        p = Path(artefato_path)
        if not p.is_file():
            sys.exit(f"artefato nao encontrado para o manifesto: {p}")
        sha = sha256_arquivo(p)
        tamanho = p.stat().st_size
        try:
            caminho_relativo = str(p.resolve().relative_to(RAIZ))
        except ValueError:
            caminho_relativo = str(p)

    doc = {
        "alvo": alvo,
        "versao": versao if versao is not None else base.get("versao"),
        "commit": base.get("commit"),
        "data_build_utc": base.get("data_build_utc"),
        "artefato": caminho_relativo,
        "sha256_artefato": sha,
        "tamanho_bytes": tamanho,
        "nota": nota,
        "ferramentas": ferramentas(),
    }

    carimbo_nome = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    destino = BUILDS_DIR / f"{carimbo_nome}-{alvo}.json"
    # Nao sobrescreve manifesto existente por acidente: dois builds no mesmo
    # segundo sao raros, mas quando acontecem cada um precisa do seu proprio
    # registro -- sobrescrever um apagaria rastro de um build de verdade.
    i = 1
    while destino.exists():
        destino = BUILDS_DIR / f"{carimbo_nome}-{alvo}-{i}.json"
        i += 1

    destino.write_text(json.dumps(doc, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return destino


# --------------------------------------------------------------------- CLI

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)

    p1 = sub.add_parser("fonte", help="versao a partir da arvore de fontes (antes de compilar)")
    p1.add_argument("alvo", choices=ALVOS)

    p2 = sub.add_parser("artefato", help="versao a partir de um artefato ja construido")
    p2.add_argument("arquivo")
    p2.add_argument("--alvo", choices=ALVOS)

    sub.add_parser("ferramentas", help="versoes de java/maven/gradle disponiveis")

    p3 = sub.add_parser("manifesto", help="grava scripts/builds/<data-hora>-<alvo>.json")
    p3.add_argument("--alvo", required=True)
    p3.add_argument("--artefato", help="caminho do arquivo produzido pelo build")
    p3.add_argument("--versao", help="sobrescreve a versao lida da fonte (raro; use com cuidado)")
    p3.add_argument("--nota", help="texto livre explicando o que este manifesto cobre")

    p4 = sub.add_parser("apkhash-para-hex", help="converte applicationversions.apkhash (base64) para sha256 hex")
    p4.add_argument("valor")

    p5 = sub.add_parser("manifesto-por-sha", help="procura em scripts/builds/ um manifesto com este sha256 hex")
    p5.add_argument("sha256_hex")

    args = ap.parse_args()

    if args.cmd == "fonte":
        print(json.dumps(fonte(args.alvo), indent=2, ensure_ascii=False))
    elif args.cmd == "artefato":
        print(json.dumps(artefato(args.arquivo, args.alvo), indent=2, ensure_ascii=False))
    elif args.cmd == "ferramentas":
        print(json.dumps(ferramentas(), indent=2, ensure_ascii=False))
    elif args.cmd == "manifesto":
        destino = gravar_manifesto(args.alvo, args.artefato, args.versao, args.nota)
        print(str(destino.relative_to(RAIZ)))
    elif args.cmd == "apkhash-para-hex":
        print(apkhash_para_hex(args.valor) or "")
    elif args.cmd == "manifesto-por-sha":
        m = procurar_manifesto(args.sha256_hex)
        print(json.dumps(m, indent=2, ensure_ascii=False) if m else "null")


if __name__ == "__main__":
    main()
