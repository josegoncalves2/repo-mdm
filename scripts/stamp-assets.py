#!/usr/bin/env python3
"""Carimba os assets do painel com um token derivado do conteudo do arquivo.

Motivo: as referencias de asset no painel usavam tokens digitados a mao
(?v=hwmdm-20260723-2242, ?timestamp=5). Quem editava um template esquecia de mexer no
token, o navegador continuava servindo a versao velha do cache, e a tela antiga
"ressuscitava". O token agora e o hash do proprio arquivo: se o conteudo mudou o token muda
sozinho, e se nao mudou o token fica igual (nao gera churn no diff).

Uso:
    python3 scripts/stamp-assets.py --check     # so relata o que esta defasado, nao escreve
    python3 scripts/stamp-assets.py             # reescreve os tokens
"""
import argparse
import hashlib
import re
import sys
from pathlib import Path

RAIZ_PADRAO = Path(__file__).resolve().parent.parent / "server-source/server/src/main/webapp"

# Arquivos onde procuramos referencias a assets.
FONTES = ("*.html", "*.js")

# Diretorios cujos arquivos podem ser referenciados como asset local.
PREFIXOS = ("app", "css", "js", "lib", "localization")

# Uma referencia e uma string entre aspas apontando para um asset local, com token opcional.
# O backreference \1 garante que abrimos e fechamos com a mesma aspa, o que faz funcionar tanto
# src='...' do index.html quanto o ng-include src="'...'" das views.
REFERENCIA = re.compile(
    r"(['\"])"
    r"((?:" + "|".join(PREFIXOS) + r")/[A-Za-z0-9_./-]+\.(?:js|css|html))"
    r"(\?[A-Za-z0-9_.=%-]*)?"
    r"\1"
)


def token(caminho: Path) -> str:
    return "h" + hashlib.sha256(caminho.read_bytes()).hexdigest()[:10]


def processar(raiz: Path, escrever: bool):
    alterados, defasados, ignorados = [], [], []

    for padrao in FONTES:
        for fonte in sorted(raiz.rglob(padrao)):
            # lib/ e codigo de terceiros; nao reescrevemos o miolo dele.
            if "lib" in fonte.relative_to(raiz).parts[:1]:
                continue

            original = fonte.read_text(encoding="utf-8")

            def troca(m):
                aspa, alvo, atual = m.group(1), m.group(2), m.group(3)
                destino = raiz / alvo
                if not destino.is_file():
                    ignorados.append((str(fonte.relative_to(raiz)), alvo))
                    return m.group(0)
                novo = f"?v={token(destino)}"
                if atual != novo:
                    defasados.append(
                        (str(fonte.relative_to(raiz)), alvo, atual or "(sem token)", novo)
                    )
                return f"{aspa}{alvo}{novo}{aspa}"

            atualizado = REFERENCIA.sub(troca, original)
            if atualizado != original:
                alterados.append(str(fonte.relative_to(raiz)))
                if escrever:
                    fonte.write_text(atualizado, encoding="utf-8")

    return alterados, defasados, ignorados


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--raiz", type=Path, default=RAIZ_PADRAO)
    p.add_argument("--check", action="store_true", help="nao escreve, so relata")
    args = p.parse_args()

    if not args.raiz.is_dir():
        sys.exit(f"raiz inexistente: {args.raiz}")

    alterados, defasados, ignorados = processar(args.raiz, escrever=not args.check)

    if defasados:
        print(f"Tokens defasados: {len(defasados)}")
        for fonte, alvo, antes, depois in defasados:
            print(f"  {fonte}: {alvo}\n      {antes}  ->  {depois}")
    else:
        print("Todos os tokens ja batem com o conteudo dos arquivos.")

    if ignorados:
        print(f"\nReferencias sem arquivo correspondente (nao tocadas): {len(ignorados)}")
        for fonte, alvo in ignorados:
            print(f"  {fonte}: {alvo}")

    if args.check:
        print(f"\n--check: nada foi escrito. Arquivos que mudariam: {len(alterados)}")
        return 1 if defasados else 0

    print(f"\nArquivos reescritos: {len(alterados)}")
    for f in alterados:
        print(f"  {f}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
