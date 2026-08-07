const { test } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio');
const OUT = process.env.DIAG_OUT;

const login = `hwmdm_probe_${Date.now()}`;
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
        select '${login}', '${login}@local.test', 'PROBE', '${hash(password)}', customerid, 2, true, true, false, null
        from users where login = 'admin';`);
});
test.afterAll(() => { psql(`delete from users where login = '${login}';`); });

async function doLogin(page) {
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(login);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(4000);
}

test('probe governance + overlay + config editor', async ({ page }) => {
  const bad = [];
  page.on('response', r => { if (r.status() >= 400) bad.push(`${r.status()} ${r.url()}`); });

  await page.setViewportSize({ width: 1440, height: 900 });
  await doLogin(page);

  // What is covering the page (the grey wash)?
  const covers = await page.evaluate(() => {
    const out = [];
    document.querySelectorAll('body *').forEach(el => {
      const cs = getComputedStyle(el);
      if ((cs.position === 'fixed' || cs.position === 'absolute')) {
        const r = el.getBoundingClientRect();
        if (r.width >= window.innerWidth * 0.9 && r.height >= window.innerHeight * 0.7 && cs.display !== 'none') {
          out.push(`${el.tagName}.${el.className} pos=${cs.position} z=${cs.zIndex} bg=${cs.backgroundColor} opacity=${cs.opacity} vis=${cs.visibility}`);
        }
      }
    });
    return out;
  });
  console.log('=== ELEMENTOS COBRINDO A TELA:');
  covers.forEach(c => console.log('   ' + c));

  // Governance tab
  await page.getByRole('button', { name: /Backups/i }).click();
  await page.waitForTimeout(2500);
  const govTxt = await page.locator('.governance-page').innerText().catch(e => 'NAO ENCONTRADO: ' + e.message);
  console.log('=== GOVERNANCE TAB TEXTO:\n' + govTxt);
  const govBtns = await page.evaluate(() => {
    const p = document.querySelector('.governance-page');
    if (!p) return ['sem .governance-page'];
    return [...p.querySelectorAll('button, label.btn')].map(b => {
      const r = b.getBoundingClientRect();
      return `"${b.innerText.trim()}" visible=${r.width > 0 && r.height > 0} rect=${Math.round(r.x)},${Math.round(r.y)},${Math.round(r.width)}x${Math.round(r.height)}`;
    });
  });
  console.log('=== BOTOES NA GOVERNANCE:');
  govBtns.forEach(b => console.log('   ' + b));
  if (OUT) await page.screenshot({ path: `${OUT}/governance.png`, fullPage: true });

  // Config editor (old layout)
  await page.goto(`${base}/#/configurations`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(2500);
  const firstEdit = page.locator('table tbody tr').first().locator('button, a').first();
  await firstEdit.click();
  await page.waitForTimeout(3000);
  console.log('=== URL EDITOR:', page.url());
  const shell = await page.evaluate(() => ({
    hasSidebar: !!document.querySelector('.hwmdm-sidebar'),
    hasBreadcrumb: !!document.querySelector('[ncy-breadcrumb]'),
    wrapper: document.querySelector('.content-wrapper') ? document.querySelector('.content-wrapper').className : null,
    tabs: [...document.querySelectorAll('.tabset .nav-tabs li a, ul.nav-tabs li a')].map(a => a.innerText.trim())
  }));
  console.log('=== SHELL DO EDITOR:', JSON.stringify(shell, null, 2));
  if (OUT) await page.screenshot({ path: `${OUT}/config-editor.png`, fullPage: true });

  console.log('=== >=400 (' + bad.length + '):');
  [...new Set(bad)].forEach(b => console.log('   ' + b));
});
