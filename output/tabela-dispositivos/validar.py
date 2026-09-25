#!/usr/bin/env python3
"""Validacao estatica: chaves do CSS balanceadas (ignorando comentarios e strings),
nenhuma regra vazia/quebrada no bloco novo, e HTML do devices.html balanceado
(html.parser da stdlib, tags de abertura x fechamento, ignorando void elements)."""
import re
import sys
from html.parser import HTMLParser
from pathlib import Path

W = Path("/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/webapp")
ok = True

# ---------- CSS ----------
css = (W / "css/hwmdm-ui.css").read_text(encoding="utf-8")
sem_coment = re.sub(r"/\*.*?\*/", "", css, flags=re.S)
sem_str = re.sub(r"'[^'\n]*'|\"[^\"\n]*\"", "''", sem_coment)
prof, minimo, linha = 0, 0, 1
for ch in sem_str:
    if ch == "\n":
        linha += 1
    if ch == "{":
        prof += 1
    elif ch == "}":
        prof -= 1
        minimo = min(minimo, prof)
print(f"CSS hwmdm-ui.css: abre={sem_str.count('{')} fecha={sem_str.count('}')} profundidade_final={prof} minimo={minimo}")
ok &= prof == 0 and minimo == 0

bloco = css[css.index("   21. Tabela de Dispositivos (app/"):]
bloco_sc = re.sub(r"/\*.*?\*/", "", bloco, flags=re.S)
regras = re.findall(r"([^{}]+)\{([^{}]*)\}", bloco_sc)
print(f"Bloco 21: {len(regras)} regras")
for sel, corpo in regras:
    decls = [d.strip() for d in corpo.split(";") if d.strip()]
    ruins = [d for d in decls if not re.match(r"^-?[a-z-]+\s*:\s*\S", d)]
    if not decls or ruins:
        ok = False
        print("  REGRA SUSPEITA:", " ".join(sel.split()), ruins or "(vazia)")
print("  todas as declaracoes no formato propriedade: valor" if ok else "  ha' problemas")

# ---------- HTML ----------
VOID = {"area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "source", "track", "wbr"}

class P(HTMLParser):
    def __init__(self):
        super().__init__()
        self.pilha, self.erros = [], []
    def handle_starttag(self, tag, attrs):
        if tag not in VOID:
            self.pilha.append((tag, self.getpos()[0]))
    def handle_startendtag(self, tag, attrs):
        pass
    def handle_endtag(self, tag):
        if tag in VOID:
            return
        if not self.pilha:
            self.erros.append(f"fecha </{tag}> sem abrir (linha {self.getpos()[0]})")
            return
        t, l = self.pilha.pop()
        if t != tag:
            self.erros.append(f"</{tag}> na linha {self.getpos()[0]} fecha <{t}> aberto na linha {l}")

for nome in ["app/components/main/view/devices.html"]:
    p = P()
    p.feed((W / nome).read_text(encoding="utf-8"))
    p.close()
    print(f"HTML {nome}: pilha_final={p.pilha} erros={p.erros}")
    ok &= not p.pilha and not p.erros

print("RESULTADO:", "OK" if ok else "FALHOU")
sys.exit(0 if ok else 1)
