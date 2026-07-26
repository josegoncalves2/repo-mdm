const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// PushSenderPolling tem dois caminhos:
//   - device SEM conexao de long polling aberta -> persiste na fila (pendingPushes)
//   - device COM conexao aberta                 -> entrega direto no fio, sem tocar no banco
// Logo, para um device conectado, "nenhuma linha nova na fila" E' a prova de entrega
// instantanea. Este teste distingue os dois casos em vez de confundi-los.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

const admin = `hwmdm_inst_${Date.now()}`;
const password = crypto.randomBytes(18).toString('base64url');
const passwordSalt = '5YdSYHyg2U';

function hash(raw) {
  const md5 = crypto.createHash('md5').update(raw).digest('hex').toUpperCase();
  return crypto.createHash('sha1').update(md5 + passwordSalt).digest('hex');
}

function psql(sql) {
  return execFileSync('docker', ['exec', '-i', 'source-postgresql-1', 'sh', '-c',
    'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -t -A'],
    { input: sql, encoding: 'utf8' }).trim();
}

function serverLogTail() {
  return execFileSync('docker', ['logs', '--tail', '40', 'source-hmdm-1'],
    { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
}

test.beforeAll(() => {
  psql(`insert into users(login, email, name, password, customerid, userroleid, alldevicesavailable, allconfigavailable, passwordreset, authtoken)
        select '${admin}', '${admin}@local.test', '${admin}', '${hash(password)}', customerid, 2, true, true, false, null
        from users where login = 'admin';`);
});
test.afterAll(() => { psql(`delete from users where login = '${admin}';`); });

test('comando para device conectado desce pelo fio, sem passar pela fila', async ({ page }) => {
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(3500);

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
  console.log('=== ONLINE:', online.map(d => `${d.number}(${d.lastUpdateAgeSec}s)`).join(', ') || '<nenhum>');
  expect(online.length, 'nenhum device online').toBeGreaterThan(0);

  const resultados = [];

  for (const device of online) {
    const antes = parseInt(psql('select coalesce(max(id),0) from pushmessages;'), 10);

    const t0 = Date.now();
    const resp = await page.evaluate(async id => {
      const r = await fetch(`rest/private/devices/${id}/command`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'set_config', params: {} })
      });
      return await r.text();
    }, device.id);
    const ms = Date.now() - t0;

    const json = JSON.parse(resp);
    expect(json.status, `servidor recusou o comando para ${device.number}`).toBe('OK');

    // Da um instante para o INSERT acontecer, se for o caminho de fila.
    await new Promise(r => setTimeout(r, 1500));
    const depois = parseInt(psql('select coalesce(max(id),0) from pushmessages;'), 10);

    const foiParaFila = depois > antes;
    const modo = foiParaFila ? 'FILA (device sem conexao aberta)' : 'INSTANTANEO (desceu pelo fio)';
    console.log(`=== ${device.number}: ${modo}  [HTTP ${ms}ms, maxId ${antes}->${depois}]`);
    resultados.push({ number: device.number, instantaneo: !foiParaFila, ms });
  }

  const log = serverLogTail();
  const linhas = log.split('\n').filter(l => l.includes('Remote command'));
  console.log('=== SERVIDOR PROCESSOU (ultimas linhas):');
  linhas.slice(-4).forEach(l => console.log('   ' + l.trim().slice(-120)));
  expect(linhas.length, 'o servidor nao registrou nenhum comando').toBeGreaterThan(0);

  const instantaneos = resultados.filter(r => r.instantaneo);
  console.log(`=== RESUMO: ${instantaneos.length}/${resultados.length} devices com entrega instantanea`);
  resultados.forEach(r => console.log(`   ${r.number}: ${r.instantaneo ? 'instantaneo' : 'enfileirado'} (${r.ms}ms)`));

  expect(instantaneos.length,
    'nenhum device tem conexao de long polling aberta; o canal instantaneo nao esta ativo').toBeGreaterThan(0);
});
