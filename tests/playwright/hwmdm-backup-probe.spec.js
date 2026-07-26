const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// Sonda curta: descobre EM QUAL passo a API de backup trava, com timeout apertado
// em cada chamada, em vez de pendurar a suite inteira.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio.');

const admin = `hwmdm_probe_${Date.now()}`;
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

test('onde a API de backup trava', async ({ page }) => {
  test.setTimeout(180000);

  console.log('--- passo 1: login');
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(3500);
  console.log('    login ok, url =', page.url());

  const call = async (path, method, timeoutMs) => {
    const t0 = Date.now();
    const r = await page.evaluate(async ([p, m, t]) => {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), t);
      try {
        const resp = await fetch(p, { method: m, signal: ctrl.signal });
        return { status: resp.status, body: (await resp.text()).slice(0, 400) };
      } catch (e) {
        return { status: -1, body: 'ABORTADO/ERRO: ' + e.name };
      } finally {
        clearTimeout(timer);
      }
    }, [path, method, timeoutMs]);
    console.log(`    ${method} ${path} -> ${r.status} em ${Date.now() - t0}ms :: ${r.body.slice(0, 250)}`);
    return r;
  };

  console.log('--- passo 2: list (10s de teto)');
  const list = await call('rest/private/backup/list', 'GET', 10000);

  console.log('--- passo 3: create (120s de teto)');
  const create = await call('rest/private/backup/create', 'POST', 120000);

  console.log('--- passo 4: o que ficou no disco');
  const disco = execFileSync('docker', ['exec', 'source-hmdm-1', 'sh', '-c',
    'ls -la /usr/local/tomcat/work/backups/ 2>&1 || echo "diretorio ausente"'], { encoding: 'utf8' });
  console.log(disco.split('\n').map(l => '    ' + l).join('\n'));

  console.log('--- passo 5: o servidor logou algo?');
  const log = execFileSync('docker', ['logs', '--tail', '80', 'source-hmdm-1'], { encoding: 'utf8' });
  log.split('\n').filter(l => /backup|pg_dump|Backup/i.test(l)).slice(-8)
     .forEach(l => console.log('    ' + l.trim().slice(0, 220)));

  expect(list.status, 'endpoint /backup/list nao respondeu').toBe(200);
  expect(create.status, 'endpoint /backup/create nao respondeu').toBe(200);
});
