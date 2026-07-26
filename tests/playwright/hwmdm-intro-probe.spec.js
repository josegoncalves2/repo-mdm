const { test } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio');

const login = `hwmdm_intro_${Date.now()}`;
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
        select '${login}', '${login}@local.test', 'INTRO', '${hash(password)}', customerid, 2, true, true, false, null
        from users where login = 'admin';`);
});
test.afterAll(() => { psql(`delete from users where login = '${login}';`); });

test('intro.js overlay forensics', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(login);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();

  for (const wait of [1500, 3000, 6000, 10000]) {
    await page.waitForTimeout(wait === 1500 ? 1500 : 1500);
    const dump = await page.evaluate(() => {
      const els = [...document.querySelectorAll('[class*="introjs"], [class*="introjsFloating"]')];
      return {
        t: Date.now(),
        els: els.map(e => {
          const r = e.getBoundingClientRect();
          const cs = getComputedStyle(e);
          return `${e.tagName}.${e.className} rect=${Math.round(r.width)}x${Math.round(r.height)} op=${cs.opacity} z=${cs.zIndex} display=${cs.display} parent=${e.parentNode && e.parentNode.tagName}`;
        }),
        hintAnchors: document.querySelectorAll('[data-hint-key]').length,
        bodyChildren: document.body.children.length
      };
    });
    console.log(`--- t+${wait}ms  introjs elements=${dump.els.length} hintAnchors=${dump.hintAnchors}`);
    dump.els.forEach(e => console.log('      ' + e));
  }

  // hints history from the API
  const hist = await page.evaluate(async () => {
    const r = await fetch('rest/private/hints/history');
    return { s: r.status, b: await r.text() };
  });
  console.log('=== hints/history:', hist.s, hist.b.slice(0, 500));
});
