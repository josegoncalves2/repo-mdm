#!/usr/bin/env python3
"""
Hook de verificacao de afirmacoes.
Requer execucao de teste Playwright + Chromium como criterio de sucesso.
Se o teste Playwright passar (exit 0), o loop pode parar.
Se nao passar, o loop continua.
"""
import sys
import subprocess
import os

# Caminho para o teste Playwright
TEST_SCRIPT = os.environ.get(
    "PLAYWRIGHT_TEST_SCRIPT",
    "/opt/projetos/hwmdm/repo-mdm/.claude/loop/playwright_test.js"
)

# Verificar se o script de teste existe
if not os.path.isfile(TEST_SCRIPT):
    print(f"ERRO: Script de teste Playwright nao encontrado: {TEST_SCRIPT}", file=sys.stderr)
    sys.exit(1)

# Executar o teste Playwright
try:
    result = subprocess.run(
        ["node", TEST_SCRIPT],
        capture_output=True,
        text=True,
        timeout=120
    )
    exit_code = result.returncode
    stdout = result.stdout
    stderr = result.stderr
except subprocess.TimeoutExpired:
    print("TIMEOUT: Teste Playwright excedeu 120 segundos", file=sys.stderr)
    sys.exit(124)
except Exception as e:
    print(f"ERRO ao executar teste Playwright: {e}", file=sys.stderr)
    sys.exit(1)

if exit_code == 0:
    print("SUCESSO: Teste Playwright + Chromium passou.")
    # Salvar prova
    with open("/opt/projetos/hwmdm/repo-mdm/.claude/loop/ultima_prova.txt", "w") as f:
        f.write(f"Playwright test passed at {os.popen('date -Is').read().strip()}\n")
        f.write(f"stdout: {stdout}\n")
        f.write(f"stderr: {stderr}\n")
    sys.exit(0)
else:
    print(f"FALHA: Teste Playwright + Chromium nao passou. Exit code: {exit_code}", file=sys.stderr)
    if stdout:
        print(f"stdout: {stdout}", file=sys.stderr)
    if stderr:
        print(f"stderr: {stderr}", file=sys.stderr)
    sys.exit(1)
