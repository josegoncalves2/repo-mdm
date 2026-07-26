const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// Sonda descartavel: descobre a forma real do JSON das APIs de configuracao/aplicativos
// pelo dominio publico, para o assistente de kiosk montar o payload certo.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

const admin = `hwmdm_probe_${Date.now()}`;
const password = crypto.randomBytes(18).toString('base64url');
const passwordSalt = '5YdSYHyg2U';

function hash(raw) {
  const md5 = crypto.createHash('md5').update(raw).digest('hex').toUpperCase();
  return crypto.createHash('sha1').update(md5 + passwordSalt).digest('hex');
}
function psql(sql) {
  return execFileSync('docker', ['exec', '-i', 'source-postgresql-1', 'sh', '-c',
    'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -t -A'],
    { input: sql, encoding: 'utf8' });
}

test.beforeAll(() => {
  psql(`insert into users(login, email, name, password, customerid, userroleid, alldevicesavailable, allconfigavailable, passwordreset, authtoken)
        select '${admin}', '${admin}@local.test', '${admin}', '${hash(password)}', customerid, 2, true, true, false, null
        from users where login = 'admin';`);
});
test.afterAll(() => { psql(`delete from users where login = '${admin}';`); });

async function signIn(page) {
  for (let attempt = 1; attempt <= 5; attempt++) {
    await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
    try { await page.waitForSelector('#username', { timeout: 12000 }); break; }
    catch (e) { console.log('    (login nao montou, tentativa ' + attempt + ')'); if (attempt === 5) throw e; }
  }
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForSelector('.hwmdm-sidebar', { timeout: 20000 });
}

test('forma do JSON das APIs usadas pelo assistente', async ({ page }) => {
  await signIn(page);

  const out = await page.evaluate(async () => {
    const j = async (u) => { const r = await fetch(u); return { status: r.status, body: await r.json() }; };
    const catalog = await j('rest/private/configurations/applications');
    const cfg11 = await j('rest/private/configurations/11');
    const apps11 = await j('rest/private/configurations/applications/11');
    const list = await j('rest/private/configurations/search/');
    const noSlash = await j('rest/private/configurations/search');
    const versions = await j('rest/private/applications/46/versions');
    return {
      catalogStatus: catalog.status,
      catalogCount: (catalog.body.data || []).length,
      catalogSample: (catalog.body.data || []).filter(a => ['com.hmdm.launcher', 'com.android.chrome', 'com.android.settings'].includes(a.pkg)),
      cfg11Status: cfg11.status,
      cfg11: cfg11.body.data,
      apps11Sample: (apps11.body.data || []).filter(a => a.action != 0),
      configs: (list.body.data || []).map(c => ({ id: c.id, name: c.name, mainAppId: c.mainAppId, contentAppId: c.contentAppId, kioskMode: c.kioskMode })),
      noSlashStatus: noSlash.status,
      versionsStatus: versions.status,
      versions: versions.body.data
    };
  });

  console.log('=== catalogo /configurations/applications status=' + out.catalogStatus + ' total=' + out.catalogCount);
  console.log(JSON.stringify(out.catalogSample, null, 1));
  console.log('=== perfil 11 (status ' + out.cfg11Status + '):');
  console.log(JSON.stringify(out.cfg11, null, 1));
  console.log('=== apps do perfil 11 com action != 0:');
  console.log(JSON.stringify(out.apps11Sample, null, 1));
  console.log('=== perfis existentes:');
  console.log(JSON.stringify(out.configs, null, 1));
  console.log('=== GET /configurations/search (sem barra) status=' + out.noSlashStatus);
  console.log('=== GET /applications/46/versions status=' + out.versionsStatus);
  console.log(JSON.stringify(out.versions, null, 1));
  expect(out.catalogStatus).toBe(200);
});
