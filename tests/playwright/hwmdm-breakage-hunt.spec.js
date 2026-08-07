const { test, expect } = require('@playwright/test');

// Caça-falhas: NAO valida que painel "aparece". Valida que a cadeia real responde.
// Alvo obrigatoriamente o dominio publico (proxy + tunel + TLS), nunca localhost.
const base = process.env.HWMDM_BASE_URL;

if (!base) {
  throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');
}

test('cadeia real responde no dominio publico', async ({ page }) => {
  const failed = [];
  const consoleErrors = [];

  page.on('response', r => {
    if (r.status() >= 400) failed.push(`${r.status()} ${r.request().method()} ${r.url()}`);
  });
  page.on('console', m => {
    if (m.type() === 'error') consoleErrors.push(m.text());
  });
  page.on('pageerror', e => consoleErrors.push('PAGEERROR: ' + e.message));

  const resp = await page.goto(`${base}/#/`, { waitUntil: 'domcontentloaded', timeout: 25000 });

  console.log('=== STATUS RAIZ:', resp && resp.status());
  console.log('=== URL FINAL:', page.url());
  console.log('=== TITLE:', await page.title());

  const bodyText = (await page.locator('body').innerText().catch(() => '')).slice(0, 400);
  console.log('=== BODY (400 chars):\n' + bodyText);

  console.log('=== RESPOSTAS >=400 (' + failed.length + '):');
  failed.slice(0, 25).forEach(f => console.log('   ' + f));

  console.log('=== ERROS DE CONSOLE (' + consoleErrors.length + '):');
  consoleErrors.slice(0, 15).forEach(e => console.log('   ' + e));

  expect(resp.status(), `Raiz retornou ${resp && resp.status()} no dominio publico`).toBeLessThan(400);
});
