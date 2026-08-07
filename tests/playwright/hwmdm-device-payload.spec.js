const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// Le o item de device EXATAMENTE como o painel recebe, pela cadeia publica.
// Usado para diagnosticar status online e IP reportado.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

const login = `hwmdm_payload_${Date.now()}`;
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
        select '${login}', '${login}@local.test', 'PAYLOAD', '${hash(password)}', customerid, 2, true, true, false, null
        from users where login = 'admin';`);
});
test.afterAll(() => { psql(`delete from users where login = '${login}';`); });

test('item de device tem lastUpdate e IP utilizaveis', async ({ page }) => {
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(login);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(3500);

  const res = await page.evaluate(async () => {
    const r = await fetch('rest/private/devices/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ value: '', pageNum: 1, pageSize: 50, sortBy: null, sortDir: 'ASC' })
    });
    const j = await r.json();
    return (j.data && j.data.devices && j.data.devices.items) || [];
  });

  console.log('=== DEVICES RETORNADOS:', res.length);
  const agora = Date.now();
  res.forEach(d => {
    const idade = d.lastUpdate ? Math.round((agora - d.lastUpdate) / 1000) : null;
    console.log(`   #${d.id} ${d.number} lastUpdate=${d.lastUpdate} (ha ${idade}s) ` +
                `publicIp=${d.publicIp} statusCode=${d.statusCode} kiosk=${d.kioskMode} ` +
                `mdm=${d.mdmMode} launcher=${d.launcherVersion}`);
    if (d.info) {
      console.log(`      info.deviceIp=${d.info.deviceIp} info.ip=${d.info.ip} ` +
                  `info.model=${d.info.model} info.serial=${d.info.serial}`);
    }
  });

  expect(res.length, 'nenhum device retornado').toBeGreaterThan(0);
  const comLastUpdate = res.filter(d => typeof d.lastUpdate === 'number' && d.lastUpdate > 0);
  console.log('=== com lastUpdate numerico:', comLastUpdate.length, 'de', res.length);
  expect(comLastUpdate.length, 'lastUpdate ausente no payload do painel').toBe(res.length);
});
