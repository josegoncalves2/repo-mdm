const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// Prova de ponta a ponta REAL: manda um comando pelo dominio publico e espera
// o TABLET consumir a mensagem (status 0 -> 1 com sendtime preenchido).
// Nao ha mock: quem vira o status e' o device buscando a fila.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

// O device precisa de ate 2 keepalives (300s cada) para buscar a fila.
const WAIT_MS = parseInt(process.env.HWMDM_DELIVERY_WAIT_MS || '780000', 10);
const POLL_MS = 15000;

const admin = `hwmdm_deliv_${Date.now()}`;
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

test('o tablet realmente consome o comando enviado pelo painel', async ({ page }) => {
  test.setTimeout(WAIT_MS + 120000);

  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(3500);

  // Escolhe o device que fez check-in mais recentemente: e' o que responde mais rapido.
  const devices = await page.evaluate(async () => {
    const r = await fetch('rest/private/devices/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ value: '', pageNum: 1, pageSize: 50, sortBy: null, sortDir: 'ASC' })
    });
    const j = await r.json();
    return (j.data && j.data.devices && j.data.devices.items) || [];
  });
  const online = devices.filter(d => d.online).sort((a, b) => b.lastUpdate - a.lastUpdate);
  console.log('=== DEVICES ONLINE:', online.map(d => `${d.number}(${d.lastUpdateAgeSec}s)`).join(', ') || '<nenhum>');
  expect(online.length, 'nenhum device online para provar entrega').toBeGreaterThan(0);

  const device = online[0];
  console.log(`=== ALVO: #${device.id} ${device.number}, ultimo check-in ha ${device.lastUpdateAgeSec}s`);

  const sent = await page.evaluate(async id => {
    const r = await fetch(`rest/private/devices/${id}/command`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'set_config', params: {} })
    });
    return await r.text();
  }, device.id);
  console.log('=== COMANDO ENVIADO:', sent);
  expect(JSON.parse(sent).status).toBe('OK');

  const messageId = parseInt(psql(
    `select id from pushmessages where deviceid = ${device.id} order by id desc limit 1;`).trim(), 10);
  console.log('=== MENSAGEM ENFILEIRADA id =', messageId);

  const started = Date.now();
  let delivered = false;
  let lastRow = '';

  while (Date.now() - started < WAIT_MS) {
    lastRow = psql(`select status || '|' || coalesce(sendtime::text, '')
                    from pendingpushes where messageid = ${messageId};`).trim();
    const [status, sendtime] = lastRow.split('|');
    const elapsed = Math.round((Date.now() - started) / 1000);

    if (status === '1') {
      delivered = true;
      console.log(`=== ENTREGUE apos ${elapsed}s (sendtime=${sendtime})`);
      break;
    }
    console.log(`   [${elapsed}s] ainda pendente (status=${status})`);
    await new Promise(r => setTimeout(r, POLL_MS));
  }

  const checkin = psql(`select (extract(epoch from now())*1000 - lastupdate)::bigint
                        from devices where id = ${device.id};`).trim();
  console.log('=== ultimo check-in do device agora:', Math.round(parseInt(checkin, 10) / 1000) + 's atras');

  expect(delivered,
    `o device nao consumiu a mensagem ${messageId} em ${WAIT_MS / 1000}s (ultimo estado: ${lastRow})`).toBe(true);
});
