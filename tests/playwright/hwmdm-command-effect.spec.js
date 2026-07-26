const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// Mede o EFEITO do comando remoto no tablet, nao a saida dele do servidor.
// set_config faz o agente reler a configuracao, o que atualiza devices.lastupdate.
// Se o lastupdate avanca logo apos o comando, o dispositivo agiu. Se nao avanca,
// o comando saiu do servidor e morreu -- que e' exatamente o que os outros testes
// nao conseguiam distinguir.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

const JANELA_MS = parseInt(process.env.HWMDM_EFFECT_WINDOW_MS || '120000', 10);
const POLL_MS = 5000;

const admin = `hwmdm_efeito_${Date.now()}`;
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

test('o comando remoto produz efeito OBSERVAVEL no tablet', async ({ page }) => {
  test.setTimeout(JANELA_MS + 120000);

  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(3500);

  // Alvo: o tablet que deu sinal mais recentemente.
  const alvo = psql(`select id || '|' || number || '|' || lastupdate from devices
                     order by lastupdate desc limit 1;`).split('|');
  const [id, number, lastUpdateAntes] = [parseInt(alvo[0], 10), alvo[1], parseInt(alvo[2], 10)];
  const idadeAntes = Math.round((Date.now() - lastUpdateAntes) / 1000);
  console.log(`=== ALVO: #${id} ${number}, ultimo sinal ha ${idadeAntes}s`);

  const resp = await page.evaluate(async d => {
    const r = await fetch(`rest/private/devices/${d}/command`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'set_config', params: {} })
    });
    return await r.text();
  }, id);
  console.log('=== COMANDO ENVIADO:', resp);
  expect(JSON.parse(resp).status, 'o servidor recusou o comando').toBe('OK');

  const t0 = Date.now();
  let reagiu = false;
  let lastUpdateDepois = lastUpdateAntes;

  while (Date.now() - t0 < JANELA_MS) {
    lastUpdateDepois = parseInt(psql(`select lastupdate from devices where id=${id};`), 10);
    const decorrido = Math.round((Date.now() - t0) / 1000);
    if (lastUpdateDepois > lastUpdateAntes) {
      reagiu = true;
      console.log(`=== O TABLET REAGIU apos ${decorrido}s (lastupdate avancou ` +
                  `${lastUpdateDepois - lastUpdateAntes}ms)`);
      break;
    }
    console.log(`   [${decorrido}s] sem reacao do tablet ainda`);
    await new Promise(r => setTimeout(r, POLL_MS));
  }

  expect(reagiu,
    `${number} nao deu nenhum sinal em ${JANELA_MS / 1000}s depois do comando. ` +
    `O servidor aceitou e despachou, mas o dispositivo nao agiu: ` +
    `entrega confirmada no servidor NAO significa comando executado no tablet.`).toBe(true);
});
