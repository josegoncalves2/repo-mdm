#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Gera WEBAPP/build-info.json da plataforma HWMDM a partir de VERSION, do git
(somente leitura) e das versoes de cada componente lidas dos arquivos de
origem. So usa biblioteca padrao do Python 3 -- nada de dependencia externa.

Ver docs/VERSIONAMENTO.md para o significado de cada campo (versao, build,
compilacao, semver_completo) e a tabela de componentes.

Uso:
    python3 docs/versao/gerar-build-info.py
        Le VERSION + git, incrementa o contador de build e grava
        WEBAPP/build-info.json.

    python3 docs/versao/gerar-build-info.py --sem-incremento
        Regera o build-info.json sem consumir um numero novo do contador
        (util para so reler os componentes depois de uma mudanca manual).

    python3 docs/versao/gerar-build-info.py --carimbar-index
        Alem de gerar o build-info.json, reescreve todo '?v=...' do
        WEBAPP/index.html real para '?v=<versao>-b<N>' (invalida cache de
        navegador). Uso normalmente reservado ao orquestrador, na
        integracao final -- ver aviso no pacote de trabalho "versao".

    python3 docs/versao/gerar-build-info.py --carimbar-index --index-alvo /caminho/copia.html
        Mesma reescrita de '?v=...', mas aplicada a uma copia de teste, sem
        tocar no index.html real. Forma usada para testar esta ferramenta.
"""
import argparse
import datetime
import json
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
VERSION_FILE = REPO_ROOT / "VERSION"
CONTADOR_FILE = Path(__file__).resolve().parent / "contador-build.txt"
WEBAPP = REPO_ROOT / "server-source" / "server" / "src" / "main" / "webapp"
BUILD_INFO_FILE = WEBAPP / "build-info.json"
INDEX_FILE = WEBAPP / "index.html"


def ler_versao():
    if not VERSION_FILE.exists():
        raise SystemExit(f"ERRO: {VERSION_FILE} nao existe.")
    versao = VERSION_FILE.read_text(encoding="utf-8").strip()
    if not versao:
        raise SystemExit(f"ERRO: {VERSION_FILE} esta vazio.")
    return versao


def git(*args):
    """Roda git em modo somente-leitura (rev-parse / status). Nunca escreve."""
    try:
        resultado = subprocess.run(
            ["git", "-C", str(REPO_ROOT), *args],
            capture_output=True, text=True, check=False,
        )
    except FileNotFoundError:
        return ""
    return resultado.stdout.strip()


def commit_curto():
    saida = git("rev-parse", "--short=7", "HEAD")
    return saida if saida else "0000000"


def arvore_suja():
    saida = git("status", "--porcelain")
    return bool(saida)


def ler_contador():
    if CONTADOR_FILE.exists():
        conteudo = CONTADOR_FILE.read_text(encoding="utf-8").strip()
        if conteudo.isdigit():
            return int(conteudo)
    return 0


def gravar_contador(n):
    CONTADOR_FILE.parent.mkdir(parents=True, exist_ok=True)
    CONTADOR_FILE.write_text(str(n) + "\n", encoding="utf-8")


def _primeiro_valor(padrao, texto):
    m = re.search(padrao, texto)
    return m.group(1) if m else None


def _versao_pom(caminho_pom):
    """
    Le a PRIMEIRA tag <version> de um pom.xml de plugin -- nos poms deste
    projeto (webfilter, moduleregistry) a versao do proprio projeto vem
    antes do bloco <parent>, entao o primeiro match e' sempre a versao do
    componente, nao a do pai nem de uma dependencia.
    """
    if not caminho_pom.exists():
        return {"versao": None, "origem": f"{caminho_pom} (nao encontrado)"}
    texto = caminho_pom.read_text(encoding="utf-8", errors="replace")
    versao = _primeiro_valor(r"<version>([^<]+)</version>", texto)
    try:
        origem = str(caminho_pom.relative_to(REPO_ROOT))
    except ValueError:
        origem = str(caminho_pom)
    return {"versao": versao, "origem": origem}


def ler_componentes():
    """
    Le a versao de cada componente do arquivo de origem correspondente (ver
    a tabela em docs/VERSIONAMENTO.md, secao 8). Se um arquivo faltar ou o
    padrao nao bater, o campo "versao"/"versionName" vem None -- isso NUNCA
    derruba a geracao do build-info.json, so fica visivel no JSON.
    """
    componentes = {}

    # Nucleo Headwind MDM de base -- mesma constante que o dialogo About usa
    # hoje (about.controller.js injeta APP_VERSION).
    app_js = WEBAPP / "app" / "app.js"
    versao_nucleo = None
    if app_js.exists():
        texto = app_js.read_text(encoding="utf-8", errors="replace")
        versao_nucleo = _primeiro_valor(r'APP_VERSION"\s*,\s*"([^"]+)"', texto)
    componentes["nucleo_headwind"] = {
        "versao": versao_nucleo,
        "origem": "server-source/server/src/main/webapp/app/app.js (constante APP_VERSION)",
    }

    componentes["plugin_webfilter"] = _versao_pom(
        REPO_ROOT / "server-source" / "plugins" / "webfilter" / "pom.xml"
    )
    componentes["plugin_moduleregistry"] = _versao_pom(
        REPO_ROOT / "server-source" / "plugins" / "moduleregistry" / "pom.xml"
    )

    # Agente Android de acesso remoto
    gradle = REPO_ROOT / "remote-agent" / "app" / "build.gradle"
    versao_agente = None
    codigo_agente = None
    if gradle.exists():
        texto = gradle.read_text(encoding="utf-8", errors="replace")
        versao_agente = _primeiro_valor(r'versionName\s+"([^"]+)"', texto)
        codigo_agente = _primeiro_valor(r"versionCode\s+(\d+)", texto)
    componentes["agente_remoto_android"] = {
        "versionName": versao_agente,
        "versionCode": int(codigo_agente) if codigo_agente else None,
        "origem": "remote-agent/app/build.gradle",
    }

    # Resolvedor DNS do webfilter -- versao vem da tag da imagem Docker,
    # declarada no docker-compose deste componente (nao ha numero "seu"
    # dentro do Dockerfile: BLOCKY_VERSION la e' de uma dependencia externa).
    compose = REPO_ROOT / "webfilter-dns" / "docker-compose.yaml"
    versao_dns = None
    if compose.exists():
        texto = compose.read_text(encoding="utf-8", errors="replace")
        versao_dns = _primeiro_valor(r"webfilter-dns:([0-9][^\s'\"]*)", texto)
    componentes["resolvedor_dns_webfilter"] = {
        "versao": versao_dns,
        "origem": "webfilter-dns/docker-compose.yaml (tag da imagem hwmdm/webfilter-dns)",
    }

    return componentes


def montar_build_info(incrementar):
    versao = ler_versao()
    commit = commit_curto()
    sujo = arvore_suja()

    n = ler_contador()
    if incrementar:
        n += 1
        gravar_contador(n)

    agora = datetime.datetime.now(datetime.timezone.utc)
    data_compacta = agora.strftime("%Y%m%d")

    semver_completo = f"{versao}+build.{n}.{data_compacta}.{commit}"
    if sujo:
        semver_completo += ".sujo"

    return {
        "versao": versao,
        "build": n,
        "compilacao": f"{data_compacta}.{n}",
        "commit": commit,
        "sujo": sujo,
        "data_build_utc": agora.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "semver_completo": semver_completo,
        "componentes": ler_componentes(),
    }


def gravar_build_info(info):
    WEBAPP.mkdir(parents=True, exist_ok=True)
    BUILD_INFO_FILE.write_text(
        json.dumps(info, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )


PADRAO_QUERY_V = re.compile(r"(\?v=)[^\"'\s>]*")


def carimbar_index(caminho_index, versao, n):
    """
    Reescreve todos os '?v=...' de um arquivo index.html para
    '?v=<versao>-b<N>'. Recebe o caminho explicitamente -- quem chama
    decide se e' o index.html real ou uma copia de teste. Retorna quantas
    substituicoes foram feitas.
    """
    texto = caminho_index.read_text(encoding="utf-8")
    marca = f"{versao}-b{n}"
    texto_novo, total = PADRAO_QUERY_V.subn(rf"\g<1>{marca}", texto)
    caminho_index.write_text(texto_novo, encoding="utf-8")
    return total


def main():
    parser = argparse.ArgumentParser(
        description="Gera build-info.json da plataforma HWMDM (VERSION + git + componentes)."
    )
    parser.add_argument(
        "--sem-incremento", action="store_true",
        help="Regera o build-info.json sem consumir um numero novo do contador de build.",
    )
    parser.add_argument(
        "--carimbar-index", action="store_true",
        help="Reescreve os '?v=...' do index.html para '?v=<versao>-b<N>'. Por padrao "
             "aplica no index.html real do WEBAPP -- use --index-alvo para testar "
             "numa copia sem tocar no arquivo real.",
    )
    parser.add_argument(
        "--index-alvo", default=None,
        help="Caminho alternativo de index.html para --carimbar-index (uso em teste).",
    )
    args = parser.parse_args()

    info = montar_build_info(incrementar=not args.sem_incremento)
    gravar_build_info(info)

    print(f"build-info.json gravado em {BUILD_INFO_FILE}")
    print(json.dumps(info, ensure_ascii=False, indent=2))

    if args.carimbar_index:
        alvo = Path(args.index_alvo) if args.index_alvo else INDEX_FILE
        if not alvo.exists():
            print(f"AVISO: {alvo} nao existe, nada carimbado.", file=sys.stderr)
            return 1
        total = carimbar_index(alvo, info["versao"], info["build"])
        print(f"{total} ocorrencia(s) de '?v=...' carimbadas em {alvo}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
