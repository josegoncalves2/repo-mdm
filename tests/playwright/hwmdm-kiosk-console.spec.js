const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio.');

const PROFILE = 'Kiosk Total (6.37.3)';
const APK_VERSION = '6.37.3';
const APK_FILE = 'hmdm-6.37.3-kiosk.apk';
const INFRA_IPS = ['10.1.1.1', '10.0.17.106', '10.0.9.1'];
const RELATO_FRESCO_S = parseInt(process.env.HWMDM_FRESH_S || '1800', 10);
const admin = `kiosk_${String(Date.now()).slice(-12)}`;
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

function sqlEscape(value) {
  return String(value).replace(/'/g, "''");
}

function psqlRows(sql) {
  const out = execFileSync('docker', ['exec', '-i', 'source-postgresql-1', 'sh', '-c',
    'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -t -A -F"|"'],
    { input: sql, encoding: 'utf8' }).trim();
  return out ? out.split('\n') : [];
}

function kioskReality() {
  return psqlRows(`
    SELECT d.number,
           coalesce(d.info::jsonb->>'kioskMode', '?'),
           coalesce(d.info::jsonb->>'mdmMode', '?'),
           coalesce((SELECT a->>'version' FROM jsonb_array_elements(d.info::jsonb->'applications') a
                     WHERE a->>'pkg'='com.hmdm.launcher' LIMIT 1), '?'),
           round((extract(epoch from now())*1000 - d.lastupdate)/1000),
           coalesce(d.publicip, '')
      FROM devices d
      JOIN configurations c ON c.id = d.configurationid
     WHERE c.name = '${sqlEscape(PROFILE)}'
     ORDER BY d.number;`).map(line => {
    const [number, kioskMode, mdmMode, launcherVersion, age, publicIp] = line.split('|');
    return {
      number,
      kioskMode,
      mdmMode,
      launcherVersion,
      age: parseInt(age, 10),
      publicIp
    };
  });
}

function expectedAgent() {
  const [id, version, url] = psql(`
    SELECT av.id || '|' || av.version || '|' || coalesce(av.url, '')
      FROM configurations c
      JOIN applicationversions av ON av.id = c.mainappid
     WHERE c.name = '${sqlEscape(PROFILE)}';`).split('|');
  return {id: parseInt(id, 10), version, url};
}

test.beforeAll(() => {
  psql(`insert into users(login, email, name, password, customerid, userroleid, alldevicesavailable, allconfigavailable, passwordreset, authtoken)
        select '${admin}', '${admin}@local.test', '${admin}', '${hash(password)}', customerid, 2, true, true, false, null
        from users where login = 'admin';`);
});

test.afterAll(() => {
  psql(`delete from users where login = '${admin}';`);
});

async function signIn(page) {
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForSelector('.hwmdm-sidebar', { timeout: 20000 });
  await page.waitForTimeout(1200);
}

async function openKiosk(page) {
  await signIn(page);
  await page.setViewportSize({ width: 1440, height: 900 });

  await expect(page.locator('[data-testid="nav-kiosk"]')).toBeVisible();
  await page.locator('[data-testid="nav-kiosk"]').click();
  await page.waitForSelector('[data-testid="kiosk-status-strip"]', { timeout: 20000 });
  await expect(page.locator('.hwmdm-navbar-current')).toHaveText('Kiosk');

  const profileSelect = page.locator('[data-testid="kiosk-profile-select"]');
  await expect(profileSelect).toBeVisible();
  await profileSelect.selectOption({ label: PROFILE });
  await expect(page.locator('[data-testid="kiosk-status-strip"]')).toContainText(PROFILE);
}

test('Kiosk existe como console proprio, denuncia a realidade dos devices e nao mostra IP de infra', async ({ page }) => {
  await openKiosk(page);

  const tabs = await page.locator('.kiosk-tab').allTextContents();
  console.log('=== ABAS DO KIOSK:', tabs.map(t => t.trim()).join(' | '));
  expect(tabs.map(t => t.trim())).toEqual([
    'Devices', 'Lockdown policy', 'Allowed apps', 'MDM APK', 'Network & device', 'Compliance', 'Apply'
  ]);

  const reality = kioskReality();
  console.log('=== REALIDADE KIOSK NO BANCO:', JSON.stringify(reality));
  expect(reality.length, `nenhum device usa o perfil "${PROFILE}"`).toBeGreaterThan(0);

  const fresh = reality.filter(d => d.age <= RELATO_FRESCO_S);
  expect(fresh.length, `nenhum device do perfil "${PROFILE}" reportou nos ultimos ${RELATO_FRESCO_S}s`).toBeGreaterThan(0);

  for (const device of fresh) {
    const row = page.locator('[data-testid="kiosk-devices-tab"] tbody tr', { hasText: device.number });
    await expect(row, `device ${device.number} nao apareceu na tabela Kiosk`).toBeVisible();
    if (device.kioskMode === 'false') {
      await expect(row, `device ${device.number} reporta kiosk=false, mas a GUI nao mostra Not locked`).toContainText('Not locked');
      await expect(page.locator('body'),
        `device ${device.number} reporta kiosk=false; a GUI precisa mostrar essa evidencia literal`).toContainText(`kiosk=false`);
    }
  }

  for (const label of ['Devices', 'Lockdown policy', 'Allowed apps', 'MDM APK', 'Network & device', 'Compliance', 'Apply']) {
    await page.locator('.kiosk-tab', { hasText: label }).click();
    await page.waitForTimeout(250);
    const visibleText = await page.locator('body').innerText();
    for (const ip of INFRA_IPS) {
      expect(visibleText, `IP de infraestrutura ${ip} apareceu na aba ${label}`).not.toContain(ip);
    }
  }
});

test('Kiosk usa perfil Kiosk Total com APK MDM 6.37.3 e envia payload completo pela GUI', async ({ page }) => {
  await openKiosk(page);
  const agent = expectedAgent();
  console.log('=== AGENTE ESPERADO DO PERFIL:', JSON.stringify(agent));
  expect(agent.version, `perfil "${PROFILE}" nao aponta para o APK ${APK_VERSION}`).toBe(APK_VERSION);
  expect(agent.url, `perfil "${PROFILE}" nao aponta para ${APK_FILE}`).toContain(APK_FILE);

  await page.locator('.kiosk-tab', { hasText: 'Lockdown policy' }).click();
  await page.locator('[data-testid="kiosk-page-strict-preset"]').click();

  await page.locator('.kiosk-tab', { hasText: 'MDM APK' }).click();
  await page.waitForSelector('[data-testid="kiosk-agent-tab"] tbody tr', { timeout: 20000 });
  const expectedApkRow = page.locator('[data-testid="kiosk-agent-tab"] tbody tr', { hasText: APK_VERSION }).filter({ hasText: APK_FILE });
  await expect(expectedApkRow, `APK ${APK_VERSION} / ${APK_FILE} nao aparece na aba MDM APK`).toBeVisible();
  await expectedApkRow.locator('button').click();

  await page.locator('.kiosk-tab', { hasText: 'Network & device' }).click();
  await page.locator('[data-testid="kiosk-page-enable-gps"]').click();

  let payload = null;
  await page.route('**/rest/private/configurations', async route => {
    if (route.request().method() === 'PUT') {
      payload = JSON.parse(route.request().postData());
    }
    await route.abort();
  });

  await page.locator('[data-testid="kiosk-save-profile"]').click();
  await page.waitForTimeout(1000);

  const model = await page.evaluate(() => {
    const s = angular.element(document.querySelector('[data-testid="kiosk-status-strip"]')).scope();
    return {
      profile: s.configuration.name,
      kioskMode: s.configuration.kioskMode,
      kioskHome: s.configuration.kioskHome,
      kioskRecents: s.configuration.kioskRecents,
      kioskNotifications: s.configuration.kioskNotifications,
      kioskExit: s.configuration.kioskExit,
      kioskLockButtons: s.configuration.kioskLockButtons,
      mainAppId: s.configuration.mainAppId,
      agent: s.getAgentSummary(),
      requestUpdates: s.configuration.requestUpdates,
      gps: s.configuration.gps,
      disableLocation: s.configuration.disableLocation,
      appPermissions: s.configuration.appPermissions,
      restrictions: s.configuration.restrictions,
      allowed: s.allowedApps().map(a => a.pkg)
    };
  });

  console.log('=== KIOSK GUI MODEL:', JSON.stringify(model));
  console.log('=== KIOSK GUI PAYLOAD:', JSON.stringify({
    kioskMode: payload && payload.kioskMode,
    mainAppId: payload && payload.mainAppId,
    agent: payload && payload.applications && payload.applications.find(a => a.pkg === 'com.hmdm.launcher'),
    requestUpdates: payload && payload.requestUpdates,
    gps: payload && payload.gps,
    disableLocation: payload && payload.disableLocation,
    appPermissions: payload && payload.appPermissions,
    restrictions: payload && payload.restrictions,
    allowed: payload && payload.applications && payload.applications.filter(a => a.useKiosk).map(a => a.pkg)
  }));

  expect(model.kioskMode).toBe(true);
  expect(model.profile).toBe(PROFILE);
  expect(model.kioskHome).toBe(false);
  expect(model.kioskRecents).toBe(false);
  expect(model.kioskNotifications).toBe(false);
  expect(model.kioskExit).toBe(false);
  expect(model.kioskLockButtons).toBe(true);
  expect(model.mainAppId).toBe(agent.id);
  expect(model.agent).toContain(APK_VERSION);
  expect(model.gps).toBe(true);
  expect(model.requestUpdates).toBe('GPS');
  expect(model.disableLocation).toBe(false);
  expect(model.appPermissions).toBe('GRANTALL');
  expect(model.restrictions).toContain('no_factory_reset');
  expect(model.restrictions).toContain('no_config_wifi');
  expect(model.restrictions).not.toContain('no_share_location');
  expect(model.allowed).toContain('com.hmdm.launcher');

  expect(payload, 'clicar salvar no console Kiosk nao disparou PUT').toBeTruthy();
  expect(payload.kioskMode).toBe(true);
  expect(payload.mainAppId, 'APK MDM 6.37.3 nao foi gravado no payload').toBe(agent.id);
  const launcher = payload.applications.find(a => a.pkg === 'com.hmdm.launcher');
  expect(launcher, 'payload nao contem o agente MDM').toBeTruthy();
  expect(launcher.usedVersionId).toBe(agent.id);
  expect(launcher.version).toBe(APK_VERSION);
  expect(launcher.url || '', `payload nao entrega ${APK_FILE}`).toContain(APK_FILE);
  expect(launcher.useKiosk, 'agente MDM precisa estar permitido no kiosk').toBe(true);
  expect(payload.requestUpdates).toBe('GPS');
  expect(payload.gps).toBe(true);
  expect(payload.disableLocation).toBe(false);
  expect(payload.appPermissions).toBe('GRANTALL');
  expect(payload.restrictions || '').not.toContain('no_share_location');
});

test('Kiosk continua responsivo em celular sem overflow horizontal', async ({ page }) => {
  await signIn(page);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.locator('.hwmdm-nav-toggle').click();
  await page.locator('[data-testid="nav-kiosk"]').click();
  await page.waitForSelector('[data-testid="kiosk-status-strip"]', { timeout: 20000 });

  for (const label of ['Devices', 'Lockdown policy', 'Allowed apps', 'MDM APK', 'Network & device', 'Compliance', 'Apply']) {
    await page.locator('.kiosk-tab', { hasText: label }).click();
    await page.waitForTimeout(250);
    const m = await page.evaluate(() => ({
      docWidth: document.documentElement.scrollWidth,
      viewport: window.innerWidth,
      title: document.querySelector('.hwmdm-navbar-current').textContent.trim()
    }));
    console.log(`=== KIOSK MOBILE ${label}: overflow=${m.docWidth - m.viewport}px title=${m.title}`);
    expect(m.docWidth).toBeLessThanOrEqual(m.viewport + 1);
    expect(m.title).toBe('Kiosk');
  }
});

test('Remote access nao renderiza imagem quebrada quando screenshot nao existe', async ({ page }) => {
  await signIn(page);
  await page.locator('.hwmdm-nav-link', { hasText: /remote access/i }).click();
  await page.waitForSelector('.remote-screen-panel', { timeout: 20000 });

  const refresh = page.locator('.remote-live-bar button', { hasText: /refresh screenshot/i }).first();
  if (await refresh.isEnabled()) {
    await refresh.click();
    await page.waitForTimeout(3500);
  }

  const broken = await page.evaluate(() => Array.from(document.images)
    .filter(img => {
      const r = img.getBoundingClientRect();
      return r.width > 0 && r.height > 0 && img.complete && img.naturalWidth === 0;
    })
    .map(img => ({src: img.getAttribute('src') || img.currentSrc, alt: img.getAttribute('alt') || ''})));

  console.log('=== IMAGENS QUEBRADAS VISIVEIS:', JSON.stringify(broken));
  expect(broken).toEqual([]);
});
