const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

const admin = `hguide_${Date.now()}`;
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

test.afterAll(() => {
  psql(`delete from users where login = '${admin}';`);
});

async function signIn(page) {
  for (let attempt = 1; attempt <= 3; attempt++) {
    await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
    try {
      await page.waitForSelector('#username', { timeout: 8000 });
      break;
    } catch (e) {
      if (attempt === 3) throw e;
    }
  }
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForSelector('.hwmdm-sidebar', { timeout: 20000 });
  await page.waitForTimeout(1200);
}

test('editor de perfil troca campos crus por opcoes guiadas e salva payload real', async ({ page }) => {
  test.setTimeout(60000);
  await signIn(page);
  await page.setViewportSize({ width: 1440, height: 900 });

  const configId = parseInt(psql("select id from configurations where name = 'Kiosk Total (6.37.3)'").trim(), 10);
  await page.goto(`${base}/#/configuration/${configId}`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('[data-testid="profile-admin-extras-builder"]', { timeout: 20000 });
  await page.waitForSelector('[data-testid="profile-allowed-activities-builder"]', { timeout: 20000 });
  await page.waitForSelector('[data-testid="profile-restrictions-builder"]', { timeout: 20000 });

  const catalog = await page.evaluate(() => {
    function controllerScope(selector) {
      let scope = angular.element(document.querySelector(selector)).scope();
      while (scope && !scope.restrictionCatalog) scope = scope.$parent;
      if (!scope) throw new Error('ConfigurationEditorController scope not found');
      return scope;
    }
    const scope = controllerScope('[data-testid="profile-restrictions-builder"]');
    return {
      adminExtras: scope.adminExtraCatalog.map(x => x.key),
      activities: scope.allowedActivityCatalog.map(x => x.key),
      restrictions: scope.restrictionCatalog.flatMap(g => g.items.map(x => x.key)),
      requestUpdates: scope.configuration.requestUpdates,
      gps: scope.configuration.gps,
      disableLocation: scope.configuration.disableLocation
    };
  });

  console.log('\n=== PROFILE GUIDED CATALOG:', JSON.stringify(catalog));
  expect(catalog.adminExtras).toContain('com.hmdm.OPEN_WIFI');
  expect(catalog.adminExtras).toContain('com.hmdm.LOCK_POWER_MENU');
  expect(catalog.activities).toContain('com.hmdm.launcher.MainActivity');
  expect(catalog.restrictions).toContain('no_factory_reset');
  expect(catalog.restrictions).toContain('no_install_apps');
  expect(catalog.restrictions).toContain('no_uninstall_apps');
  expect(catalog.requestUpdates).toBe('GPS');
  expect(catalog.gps).toBe(true);
  expect(catalog.disableLocation).toBe(false);

  await page.locator('[data-testid="profile-admin-extra-com-hmdm-OPEN_WIFI"]').check();
  await page.locator('[data-testid="profile-activity-com-hmdm-launcher-MainActivity"]').check();
  await page.locator('[data-testid="profile-restriction-no_install_apps"]').check();
  await page.locator('[data-testid="profile-maximum-restrictions"]').click();
  await page.locator('[data-testid="kiosk-lockdown-preset"]').click();
  await expect.poll(async () => page.evaluate(() => {
    let scope = angular.element(document.querySelector('[data-testid="profile-restrictions-builder"]')).scope();
    while (scope && !scope.restrictionCatalog) scope = scope.$parent;
    return scope && scope.mainApp && scope.mainApp.url;
  }), { timeout: 10000 }).toContain('-kiosk.apk');

  const model = await page.evaluate(() => {
    function controllerScope(selector) {
      let scope = angular.element(document.querySelector(selector)).scope();
      while (scope && !scope.restrictionCatalog) scope = scope.$parent;
      if (!scope) throw new Error('ConfigurationEditorController scope not found');
      return scope;
    }
    const scope = controllerScope('[data-testid="profile-restrictions-builder"]');
    return {
      adminExtras: scope.configuration.adminExtras || '',
      allowedClasses: scope.configuration.allowedClasses || '',
      restrictions: scope.configuration.restrictions || '',
      gps: scope.configuration.gps,
      requestUpdates: scope.configuration.requestUpdates,
      disableLocation: scope.configuration.disableLocation,
      appPermissions: scope.configuration.appPermissions,
      mainAppUrl: scope.mainApp && scope.mainApp.url
    };
  });

  expect(model.adminExtras).toContain('"com.hmdm.OPEN_WIFI": true');
  expect(model.allowedClasses).toContain('com.hmdm.launcher.MainActivity');
  expect(model.restrictions).toContain('no_factory_reset');
  expect(model.restrictions).toContain('no_install_apps');
  expect(model.restrictions).toContain('no_uninstall_apps');
  expect(model.restrictions).not.toContain('no_share_location');
  expect(model.gps).toBe(true);
  expect(model.requestUpdates).toBe('GPS');
  expect(model.disableLocation).toBe(false);
  expect(model.appPermissions).toBe('GRANTALL');
  expect(model.mainAppUrl).toContain('-kiosk.apk');

  let payload = null;
  await page.route('**/rest/private/configurations', async route => {
    if (route.request().method() === 'PUT') {
      payload = JSON.parse(route.request().postData());
    }
    await route.abort();
  });

  await page.locator('.cfg-savebar .btn-primary').click();
  await page.waitForTimeout(800);

  console.log('=== PROFILE GUIDED PAYLOAD:', JSON.stringify({
    adminExtras: payload && payload.adminExtras,
    allowedClasses: payload && payload.allowedClasses,
    restrictions: payload && payload.restrictions,
    gps: payload && payload.gps,
    requestUpdates: payload && payload.requestUpdates,
    disableLocation: payload && payload.disableLocation,
    appPermissions: payload && payload.appPermissions
  }));

  expect(payload, 'o editor nao tentou salvar pelo PUT real da GUI').toBeTruthy();
  expect(payload.adminExtras || '').toContain('"com.hmdm.OPEN_WIFI": true');
  expect(payload.allowedClasses || '').toContain('com.hmdm.launcher.MainActivity');
  expect(payload.restrictions || '').toContain('no_factory_reset');
  expect(payload.restrictions || '').toContain('no_install_apps');
  expect(payload.restrictions || '').toContain('no_uninstall_apps');
  expect(payload.restrictions || '').not.toContain('no_share_location');
  expect(payload.gps).toBe(true);
  expect(payload.requestUpdates).toBe('GPS');
  expect(payload.disableLocation).toBe(false);
  expect(payload.appPermissions).toBe('GRANTALL');
});
