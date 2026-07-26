const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');
const fs = require('fs');

const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio');

const OUT = process.env.DIAG_OUT;
const login = `hwmdm_diag_${Date.now()}`;
const password = crypto.randomBytes(18).toString('base64url');
const passwordSalt = '5YdSYHyg2U';

function hash(raw) {
  const md5 = crypto.createHash('md5').update(raw).digest('hex').toUpperCase();
  return crypto.createHash('sha1').update(md5 + passwordSalt).digest('hex');
}

function psql(sql) {
  return execFileSync('docker', ['exec', '-i', 'source-postgresql-1', 'sh', '-c',
    'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'],
    { input: sql, encoding: 'utf8' });
}

test.beforeAll(() => {
  psql(`insert into users(login, email, name, password, customerid, userroleid, alldevicesavailable, allconfigavailable, passwordreset, authtoken)
        select '${login}', '${login}@local.test', 'DIAG', '${hash(password)}', customerid, 2, true, true, false, null
        from users where login = 'admin';`);
});
test.afterAll(() => { psql(`delete from users where login = '${login}';`); });

test('diagnostico completo do painel', async ({ page }) => {
  const errs = [];
  const bad = [];
  page.on('console', m => { if (m.type() === 'error') errs.push(m.text()); });
  page.on('pageerror', e => errs.push('PAGEERROR ' + e.message));
  page.on('response', r => { if (r.status() >= 400) bad.push(`${r.status()} ${r.url()}`); });

  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(login);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(4000);

  console.log('=== URL POS-LOGIN:', page.url());

  // Estrutura do menu lateral
  const nav = await page.evaluate(() => {
    const out = [];
    document.querySelectorAll('a, .hwmdm-nav-link, li').forEach(el => {
      const cls = el.className && el.className.toString ? el.className.toString() : '';
      if (/nav|menu|sidebar/i.test(cls)) {
        const t = (el.innerText || '').trim().split('\n')[0];
        if (t) out.push(cls.slice(0, 40) + ' | ' + t);
      }
    });
    return [...new Set(out)];
  });
  console.log('=== NAV (' + nav.length + '):');
  nav.forEach(n => console.log('   ' + n));

  // Chamada real da API de devices, com o cookie da sessao
  const api = await page.evaluate(async () => {
    const r = await fetch('rest/private/devices/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ value: '', pageNum: 1, pageSize: 50, sortBy: null, sortDir: 'ASC' })
    });
    return { status: r.status, body: await r.text() };
  });
  console.log('=== API devices/search status:', api.status);
  console.log('=== API body (2500):', api.body.slice(0, 2500));

  // Overflow horizontal em varias larguras
  for (const w of [1440, 1024, 768, 390]) {
    await page.setViewportSize({ width: w, height: 900 });
    await page.waitForTimeout(700);
    const o = await page.evaluate(() => ({
      sw: document.documentElement.scrollWidth,
      cw: document.documentElement.clientWidth
    }));
    console.log(`=== LARGURA ${w}: scrollWidth=${o.sw} clientWidth=${o.cw} overflow=${o.sw - o.cw}`);
    if (OUT) await page.screenshot({ path: `${OUT}/main-${w}.png`, fullPage: true });
  }

  await page.setViewportSize({ width: 1440, height: 900 });

  // Percorre as rotas principais
  const routes = ['#/', '#/devices', '#/configurations', '#/remote', '#/settings/common', '#/settings/general', '#/gpsmap', '#/governance', '#/updates'];
  for (const r of routes) {
    await page.goto(`${base}/${r}`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2500);
    const txt = (await page.locator('body').innerText().catch(() => '')).replace(/\n+/g, ' | ').slice(0, 300);
    console.log(`--- ROTA ${r} :: ${txt}`);
    if (OUT) await page.screenshot({ path: `${OUT}/route-${r.replace(/[#/]/g, '_')}.png`, fullPage: true });
  }

  console.log('=== RESPOSTAS >=400 (' + bad.length + '):');
  [...new Set(bad)].slice(0, 40).forEach(b => console.log('   ' + b));
  console.log('=== ERROS CONSOLE (' + errs.length + '):');
  [...new Set(errs)].slice(0, 40).forEach(e => console.log('   ' + e));
});
