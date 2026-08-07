const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// Valida o acesso remoto de ponta a ponta pela cadeia publica real
// (dominio -> proxy -> tomcat -> PushService -> fila de entrega ao device).
// Nao usa curl e nao usa localhost.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

const admin = `hwmdm_cmd_admin_${Date.now()}`;
const limited = `hwmdm_cmd_guest_${Date.now()}`;
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

function makeUser(login, roleId) {
  psql(`insert into users(login, email, name, password, customerid, userroleid, alldevicesavailable, allconfigavailable, passwordreset, authtoken)
        select '${login}', '${login}@local.test', '${login}', '${hash(password)}', customerid, ${roleId}, true, true, false, null
        from users where login = 'admin';`);
}

// 2 = Admin (tem edit_devices). 104 = Guest (read-only, sem permissao de comando).
test.beforeAll(() => {
  makeUser(admin, 2);
  makeUser(limited, 104);
});
test.afterAll(() => {
  psql(`delete from users where login in ('${admin}', '${limited}');`);
});

async function signIn(page, login) {
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(login);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(3500);
}

async function postCommand(page, deviceId, body) {
  return page.evaluate(async ([id, payload]) => {
    const r = await fetch(`rest/private/devices/${id}/command`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    return { httpStatus: r.status, body: await r.text() };
  }, [deviceId, body]);
}

test('endpoint de comando remoto existe, valida, autoriza e enfileira', async ({ page }) => {
  await signIn(page, admin);

  const devices = await page.evaluate(async () => {
    const r = await fetch('rest/private/devices/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ value: '', pageNum: 1, pageSize: 50, sortBy: null, sortDir: 'ASC' })
    });
    const j = await r.json();
    return (j.data && j.data.devices && j.data.devices.items) || [];
  });
  expect(devices.length, 'nenhum device disponivel para testar').toBeGreaterThan(0);

  const device = devices[0];
  console.log(`=== DEVICE ALVO: #${device.id} ${device.number} online=${device.online} ` +
              `idade=${device.lastUpdateAgeSec}s limiar=${device.onlineThresholdSec}s keepalive=${device.keepaliveSec}s`);

  // ---- 1) catalogo de comandos suportados -----------------------------------------------------
  const catalog = await page.evaluate(async () => {
    const r = await fetch('rest/private/devices/commands');
    return { httpStatus: r.status, body: await r.text() };
  });
  console.log('=== GET /commands ->', catalog.httpStatus, catalog.body.slice(0, 500));
  expect(catalog.httpStatus, 'endpoint de catalogo nao existe').toBe(200);
  const catalogJson = JSON.parse(catalog.body);
  expect(catalogJson.status).toBe('OK');
  expect(catalogJson.data).toContain('reboot');
  expect(catalogJson.data).toContain('unlock_screen');

  // ---- 2) comando seguro, de verdade ----------------------------------------------------------
  // set_config -> push 'configUpdated': manda o device reler a config. Nao destrutivo.
  const before = parseInt(psql(`select count(*) from pushmessages where deviceid = ${device.id};`).trim(), 10);

  const ok = await postCommand(page, device.id, { action: 'set_config', params: {} });
  console.log('=== POST set_config ->', ok.httpStatus, ok.body);
  expect(ok.httpStatus, 'endpoint /command ainda retorna erro HTTP').toBe(200);
  const okJson = JSON.parse(ok.body);
  expect(okJson.status, `servidor recusou o comando: ${ok.body}`).toBe('OK');
  expect(okJson.data.pushType).toBe('configUpdated');
  expect(okJson.data.deviceId).toBe(device.id);

  // PushSenderPolling tem dois caminhos legitimos:
  //   - device COM long polling aberto -> desce direto no fio, NAO grava no banco
  //   - device sem conexao aberta      -> enfileira em pushmessages/pendingPushes
  // Exigir a linha no banco daria falso negativo justamente quando o canal esta melhor.
  await new Promise(r => setTimeout(r, 1500));
  const after = parseInt(psql(`select count(*) from pushmessages where deviceid = ${device.id};`).trim(), 10);
  const viaFila = after > before;
  console.log(`=== pushmessages para o device: antes=${before} depois=${after} -> ` +
              (viaFila ? 'ENFILEIRADO (device sem conexao aberta)' : 'INSTANTANEO (desceu pelo fio)'));
  if (viaFila) {
    const queued = psql(`select id || ' | ' || messagetype || ' | ' || coalesce(payload,'<sem payload>')
                         from pushmessages where deviceid = ${device.id} order by id desc limit 3;`).trim();
    console.log('=== ULTIMAS MENSAGENS ENFILEIRADAS:\n' + queued.split('\n').map(l => '   ' + l).join('\n'));
  }

  // O que precisa ser verdade nos dois casos: o servidor aceitou e despachou.
  const despachou = execFileSync('docker', ['logs', '--tail', '60', 'source-hmdm-1'], { encoding: 'utf8' })
    .split('\n').filter(l => l.includes('Remote command') && l.includes(device.number));
  console.log('=== SERVIDOR DESPACHOU:', despachou.length, 'linha(s) para', device.number);
  despachou.slice(-2).forEach(l => console.log('   ' + l.trim().slice(-110)));
  expect(despachou.length, 'o servidor nao registrou o despacho do comando').toBeGreaterThan(0);

  // ---- 3) comando com payload: validacao de parametro ------------------------------------------
  const withPayload = await postCommand(page, device.id, {
    action: 'message', params: { text: 'Teste de acesso remoto HWMDM', duration: 10 }
  });
  console.log('=== POST message ->', withPayload.httpStatus, withPayload.body);
  const msgJson = JSON.parse(withPayload.body);
  expect(msgJson.status).toBe('OK');
  expect(msgJson.data.pushType).toBe('textMessage');

  // Se caiu na fila, o payload tem de estar integro no banco. Se desceu pelo fio,
  // a prova do payload e' a resposta do servidor, ja verificada acima.
  const msgRow = psql(`select payload from pushmessages where deviceid = ${device.id}
                       and messagetype = 'textMessage' order by id desc limit 1;`).trim();
  if (msgRow) {
    console.log('=== PAYLOAD GRAVADO:', msgRow);
    expect(msgRow, 'payload da mensagem foi persistido corrompido').toContain('Teste de acesso remoto HWMDM');
  } else {
    console.log('=== PAYLOAD nao foi ao banco: mensagem entregue direto na conexao aberta do device');
  }

  // ---- 4) rejeicoes: comando desconhecido e parametro invalido ---------------------------------
  const unknown = await postCommand(page, device.id, { action: 'nao_existe', params: {} });
  console.log('=== POST comando inexistente ->', unknown.httpStatus, unknown.body);
  expect(JSON.parse(unknown.body).status).toBe('ERROR');

  const badPkg = await postCommand(page, device.id, { action: 'run_app', params: { pkg: 'nao eh pacote!!' } });
  console.log('=== POST run_app com pkg invalido ->', badPkg.httpStatus, badPkg.body);
  expect(JSON.parse(badPkg.body).status).toBe('ERROR');

  const missing = await postCommand(page, device.id, { action: 'run_app', params: {} });
  console.log('=== POST run_app sem pkg ->', missing.httpStatus, missing.body);
  expect(JSON.parse(missing.body).status).toBe('ERROR');

  const traversal = await postCommand(page, device.id, { action: 'delete_file', params: { path: '/sdcard/../../etc/passwd' } });
  console.log('=== POST delete_file com path traversal ->', traversal.httpStatus, traversal.body);
  expect(JSON.parse(traversal.body).status).toBe('ERROR');

  // ---- 5) device inexistente --------------------------------------------------------------------
  const noDevice = await postCommand(page, 999999, { action: 'reboot', params: {} });
  console.log('=== POST em device inexistente ->', noDevice.httpStatus, noDevice.body);
  expect(JSON.parse(noDevice.body).status).toBe('ERROR');
});

test('RBAC bloqueia quem nao tem permissao', async ({ page }) => {
  await signIn(page, limited);

  const deviceId = parseInt(psql('select id from devices order by id limit 1;').trim(), 10);
  const res = await postCommand(page, deviceId, { action: 'reboot', params: {} });
  console.log('=== POST reboot como Guest ->', res.httpStatus, res.body);

  const json = JSON.parse(res.body);
  expect(json.status, 'usuario sem permissao conseguiu mandar reboot').toBe('ERROR');
  expect(json.message).toBe('error.permission.denied');
});

test('acao remota fica registrada na auditoria', async ({ page }) => {
  await signIn(page, admin);
  const deviceId = parseInt(psql('select id from devices order by id limit 1;').trim(), 10);

  await postCommand(page, deviceId, { action: 'set_config', params: {} });
  await page.waitForTimeout(1500);

  const rows = psql(`select login || ' | ' || action || ' | ' || coalesce(payload,'')
                     from plugin_audit_log
                     where action = 'plugin.audit.action.device.remote.command'
                     order by id desc limit 3;`).trim();
  console.log('=== AUDITORIA (ultimas 3):\n' + (rows ? rows.split('\n').map(l => '   ' + l).join('\n') : '   <vazio>'));
  expect(rows.length, 'nenhum registro de auditoria para o comando remoto').toBeGreaterThan(0);
  expect(rows).toContain(admin);
});
