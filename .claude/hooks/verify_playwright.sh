#!/usr/bin/env bash
# Hook de verificação REAL - só passa se Playwright/Chromium provar.
# Se não testar com Playwright, NÃO PARA O LOOP.
set -uo pipefail

# Executa teste Playwright real (não stub)
python3 -c "
import subprocess, sys
# Tenta rodar Playwright com Chromium
try:
    from playwright.sync_api import sync_playwright
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        # Prova real: acessa o serviço MDM
        page.goto('http://localhost:8080/', timeout=10000)
        assert 'MDM' in page.content() or page.status == 200, 'Serviço MDM não respondeu corretamente'
        browser.close()
    print('TESTE_PLAYWRIGHT_OK')
    sys.exit(0)
except Exception as e:
    print('TESTE_PLAYWRIGHT_FALHA:', e)
    sys.exit(1)
"
