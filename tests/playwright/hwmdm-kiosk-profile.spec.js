const { test, expect } = require('@playwright/test');
const { execFileSync } = require('child_process');

// Valida o perfil de kiosk novo pelo caminho publico real: o payload que o tablet
// receberia, a allowlist por perfil (app A permitido / app B bloqueado) e o APK baixavel.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

const PROFILE = process.env.HWMDM_PROFILE || 'Kiosk Total (6.37.3)';
const APK_VERSION = '6.37.3';
const APK_FILE = 'hmdm-6.37.3-kiosk.apk';
const APK_VERSION_CODE = 15374;
const APK_BYTES = 7815271;

function psql(sql) {
  return execFileSync('docker', ['exec', '-i', 'source-postgresql-1', 'sh', '-c',
    'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -t -A'],
    { input: sql, encoding: 'utf8' }).trim();
}

test('o perfil de kiosk entrega allowlist por perfil no payload de sync', async ({ request }) => {
  const configId = psql(`select id from configurations where name = '${PROFILE.replace(/'/g, "''")}';`);
  expect(configId, `perfil "${PROFILE}" nao existe`).not.toBe('');
  console.log('=== PERFIL:', PROFILE, '(id', configId + ')');

  // Um device descartavel apontando para o perfil, so' para ler o payload.
  const probe = `kioskprobe${Date.now()}`;
  psql(`insert into devices(number, description, lastupdate, configurationid, customerid)
        select '${probe}', 'sonda temporaria de teste', 0, ${configId}, customerid
        from configurations where id = ${configId};`);

  try {
    const resp = await request.get(`${base}/rest/public/sync/configuration/${probe}`, { timeout: 30000 });
    expect(resp.status()).toBe(200);
    const d = (await resp.json()).data || {};

    console.log('=== kioskMode:', d.kioskMode, '| mainApp:', d.mainApp);
    console.log('=== kioskHome:', d.kioskHome, 'kioskRecents:', d.kioskRecents,
                'kioskNotifications:', d.kioskNotifications, 'kioskExit:', d.kioskExit);
    console.log('=== pushOptions:', d.pushOptions, '| keepalive:', d.keepaliveTime);
    console.log('=== restrictions:', d.restrictions);

    const apps = d.applications || [];
    console.log('=== APPLICATIONS (' + apps.length + '):');
    apps.forEach(a => console.log(`   pkg=${a.pkg} version=${a.version} code=${a.code} useKiosk=${a.useKiosk} showIcon=${a.showIcon}`));

    // Kiosk no maximo de restricao
    expect(d.kioskMode, 'kiosk nao esta ligado').toBe(true);
    expect(d.kioskHome, 'botao home deveria estar desligado').toBeFalsy();
    expect(d.kioskRecents, 'recentes deveria estar desligado').toBeFalsy();
    expect(d.kioskNotifications, 'notificacoes deveriam estar desligadas').toBeFalsy();
    expect(d.kioskExit, 'saida do kiosk deveria estar desligada').toBeFalsy();
    expect(d.pushOptions, 'precisa ser polling: MQTT esta inalcancavel').toBe('polling');

    // A allowlist POR PERFIL: app A entra, app B nao.
    const permitidos = apps.filter(a => a.useKiosk).map(a => a.pkg);
    console.log('=== PERMITIDOS NO LOCK TASK:', permitidos.join(', ') || '<nenhum>');
    expect(permitidos, 'Chrome (app A) deveria estar permitido').toContain('com.android.chrome');
    expect(permitidos, 'o agente MDM precisa estar permitido').toContain('com.hmdm.launcher');
    expect(permitidos, 'Play Store (app B) NAO deveria estar permitido').not.toContain('com.android.vending');
    expect(permitidos, 'Settings NAO deveria estar permitido').not.toContain('com.android.settings');

    // O agente entregue tem de ser o APK de kiosk fechado no prompt: 6.37.3.
    // Se alguem trocar o perfil para "ultima versao" ou outro APK, este teste deixa de passar.
    const agente = apps.find(a => a.pkg === 'com.hmdm.launcher');
    const [versaoDoPerfil, urlDoPerfil] = psql(`select av.version || '|' || coalesce(av.url, '') from configurations c
                                                join applicationversions av on av.id = c.mainappid
                                                where c.id = ${configId};`).split('|');
    console.log('=== AGENTE ENTREGUE:', agente && agente.version, '->', agente && agente.url);
    console.log('=== APK CONFIGURADO NO PERFIL:', versaoDoPerfil, '->', urlDoPerfil);
    expect(agente, 'o agente MDM nao esta no payload').toBeTruthy();
    expect(versaoDoPerfil, `perfil "${PROFILE}" nao aponta para o APK ${APK_VERSION}`).toBe(APK_VERSION);
    expect(urlDoPerfil, `perfil "${PROFILE}" nao aponta para ${APK_FILE}`).toContain(APK_FILE);
    expect(agente.version, `payload nao entrega o APK ${APK_VERSION}`).toBe(APK_VERSION);
    expect(agente.code, `payload nao entrega versionCode ${APK_VERSION_CODE}`).toBe(APK_VERSION_CODE);
    expect(agente.url || '', `payload nao entrega ${APK_FILE}`).toContain(APK_FILE);
  } finally {
    psql(`delete from devices where number = '${probe}';`);
  }
});

test('o APK 6.37.3 e baixavel pelo dominio publico', async ({ request }) => {
  const url = `${base}/files/${APK_FILE}`;
  const resp = await request.get(url, { timeout: 90000 });
  console.log('=== URL:', url, '| STATUS:', resp.status());

  expect(resp.status(), `APK retornou ${resp.status()}`).toBe(200);
  const body = await resp.body();
  console.log('=== BYTES:', body.length, '| ASSINATURA:', body.slice(0, 2).toString('ascii'));

  expect(body.slice(0, 2).toString('ascii'), 'nao e um APK/ZIP valido').toBe('PK');
  expect(body.length, 'APK veio truncado').toBe(APK_BYTES);

  const registrado = psql(`select version || '|' || versioncode from applicationversions
                           where url like '%${APK_FILE}%';`);
  console.log('=== REGISTRADO NO SERVIDOR:', registrado);
  expect(registrado).toBe(`${APK_VERSION}|${APK_VERSION_CODE}`);
});
