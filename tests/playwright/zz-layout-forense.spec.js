const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// Forense: o que o navegador REALMENTE recebe do dominio publico.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio.');
const OUT = process.env.SHOTS || '/tmp';

const admin = `hwmdm_lf_${Date.now()}`;
const password = crypto.randomBytes(18).toString('base64url');
const salt = '5YdSYHyg2U';

function hash(raw) {
  const md5 = crypto.createHash('md5').update(raw).digest('hex').toUpperCase();
  return crypto.createHash('sha1').update(md5 + salt).digest('hex');
}
function psql(sql) {
  return execFileSync('docker', ['exec', '-i', 'source-postgresql-1', 'sh', '-c',
    'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -A'],
    { input: sql, encoding: 'utf8' }).trim();
}

test.beforeAll(() => {
  psql(`insert into users(login, email, name, password, customerid, userroleid, alldevicesavailable, allconfigavailable, passwordreset, authtoken)
        select '${admin}', '${admin}@l.test', '${admin}', '${hash(password)}', customerid, 2, true, true, false, null
        from users where login='admin';`);
});
test.afterAll(() => { psql(`delete from users where login='${admin}';`); });

test('forense do layout servido', async ({ page }) => {
  test.setTimeout(180000);
  const requests = [];
  page.on('response', r => requests.push({ url: r.url(), status: r.status() }));
  const consoleErrs = [];
  page.on('console', m => { if (m.type() === 'error') consoleErrs.push(m.text()); });

  await page.setViewportSize({ width: 1600, height: 1000 });
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').waitFor({ timeout: 30000 });
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(6000);
  await page.screenshot({ path: `${OUT}/live-pos-login.png`, fullPage: true });

  const diag = await page.evaluate(() => ({
    stylesheets: [...document.querySelectorAll('link[rel=stylesheet]')].map(l => l.getAttribute('href')),
    topbar: !!document.querySelector('.hwmdm-topbar'),
    navLinks: [...document.querySelectorAll('.hwmdm-nav-link')].map(a => a.innerText.replace(/\s+/g, ' ').trim()),
    navSections: [...document.querySelectorAll('.hwmdm-nav-heading')].map(a => a.innerText.trim()),
    oldNavbarTabs: [...document.querySelectorAll('.nav-tabs a, ul.nav.navbar-nav a')].map(a => a.innerText.replace(/\s+/g, ' ').trim()).filter(Boolean),
    bodyClasses: document.body.className,
    hasSidebar: !!document.querySelector('.hwmdm-sidebar, .hwmdm-nav-section'),
    firstH1: (document.querySelector('h1,h2') || {}).innerText || null,
  }));

  console.log('\n===== STYLESHEETS CARREGADAS =====');
  diag.stylesheets.forEach(s => console.log('  ' + s));
  console.log('\n===== ESTRUTURA =====');
  console.log('  .hwmdm-topbar presente: ' + diag.topbar);
  console.log('  sidebar nova presente : ' + diag.hasSidebar);
  console.log('  secoes do menu novo   : ' + JSON.stringify(diag.navSections));
  console.log('  links do menu novo    : ' + JSON.stringify(diag.navLinks));
  console.log('  abas layout ANTIGO    : ' + JSON.stringify(diag.oldNavbarTabs));
  console.log('  body class            : ' + diag.bodyClasses);

  const css = requests.filter(r => r.url.includes('.css'));
  console.log('\n===== REQUESTS CSS (status) =====');
  css.forEach(r => console.log(`  ${r.status}  ${r.url}`));

  const falhas = requests.filter(r => r.status >= 400);
  console.log('\n===== REQUESTS >= 400 =====');
  falhas.forEach(r => console.log(`  ${r.status}  ${r.url}`));
  console.log('\n===== CONSOLE ERRORS =====');
  consoleErrs.slice(0, 20).forEach(e => console.log('  ' + e));

  expect(true).toBeTruthy();
});
