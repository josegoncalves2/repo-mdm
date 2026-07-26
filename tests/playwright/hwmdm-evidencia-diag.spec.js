const { test } = require('@playwright/test');
const base = process.env.HWMDM_BASE_URL;
const OUT = process.env.SHOTS || '/tmp';

test('o que a pagina de login realmente entrega', async ({ page }) => {
  test.setTimeout(120000);
  const reqs = [];
  page.on('response', r => reqs.push(`${r.status()} ${r.url()}`));
  page.on('console', m => console.log(`CONSOLE[${m.type()}] ${m.text().slice(0, 200)}`));
  page.on('pageerror', e => console.log(`PAGEERROR ${e.message.slice(0, 300)}`));

  const resp = await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded', timeout: 60000 });
  console.log('goto status:', resp && resp.status());
  await page.waitForTimeout(8000);

  console.log('URL final:', page.url());
  console.log('TITLE:', await page.title());
  const info = await page.evaluate(() => ({
    bodyLen: document.body.innerHTML.length,
    texto: document.body.innerText.replace(/\s+/g, ' ').slice(0, 600),
    inputs: [...document.querySelectorAll('input')].map(i => `${i.id || '(sem id)'}:${i.type}:${i.name || ''}`),
    botoes: [...document.querySelectorAll('button')].map(b => `${b.type}:${b.innerText.trim().slice(0, 40)}`),
    ngApp: !!window.angular
  }));
  console.log(JSON.stringify(info, null, 2));

  console.log('\n=== REQUESTS ===');
  reqs.forEach(r => console.log(r));

  await page.screenshot({ path: `${OUT}/shots/diag-login.png`, fullPage: true });
});
