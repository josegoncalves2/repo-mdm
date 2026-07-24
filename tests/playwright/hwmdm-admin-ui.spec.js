const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const base = process.env.HWMDM_BASE_URL || 'http://localhost:8080';
const login = `hwmdm_ui_${Date.now()}`;
const password = crypto.randomBytes(18).toString('base64url');
const passwordSalt = '5YdSYHyg2U';

function hashPasswordForHwmdm(rawPassword) {
  const md5 = crypto.createHash('md5').update(rawPassword).digest('hex').toUpperCase();
  return crypto.createHash('sha1').update(md5 + passwordSalt).digest('hex');
}

function runPsql(sql) {
  execFileSync(
    'docker',
    [
      'exec',
      '-i',
      'hwmdm-postgresql-1',
      'sh',
      '-c',
      'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'
    ],
    { input: sql, stdio: ['pipe', 'pipe', 'pipe'] }
  );
}

test.beforeAll(() => {
  const passwordHash = hashPasswordForHwmdm(password);
  runPsql(`
    insert into users(login, email, name, password, customerid, userroleid, alldevicesavailable, allconfigavailable, passwordreset, authtoken)
    select '${login}', '${login}@local.test', 'HWMDM UI Test', '${passwordHash}', customerid, 2, true, true, false, null
    from users
    where login = 'admin';
  `);
});

test.afterAll(() => {
  runPsql(`delete from users where login = '${login}';`);
});

test('admin console modules render real content without broken layout', async ({ page }) => {
  const consoleErrors = [];
  const failedResponses = [];

  page.on('console', message => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text());
    }
  });

  page.on('response', response => {
    const status = response.status();
    const url = response.url();
    if (status >= 400 && !url.includes('/rest/plugins/')) {
      failedResponses.push(`${status} ${url}`);
    }
  });

  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(login);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();

  await expect(page.locator('.hwmdm-sidebar')).toBeVisible({ timeout: 15000 });
  await expect(page.locator('.hwmdm-sidebar-brand strong')).toContainText('Headwind MDM');
  await expect(page.locator('.hwmdm-main-surface')).toBeVisible();

  await page.locator('.hwmdm-nav-link:has-text("Dashboard")').click();
  await expect(page.locator('.summary-metric')).toHaveCount(5);
  await expect(page.locator('.summary-panel:has-text("Recent devices")')).toBeVisible();
  await expect(page.locator('.summary-panel:has-text("Operational alerts")')).toBeVisible();

  await page.locator('.hwmdm-nav-link:has-text("Remote Access")').click();
  await expect(page.locator('h4:has-text("Remote Access")')).toBeVisible();
  await expect(page.locator('.remote-device-panel')).toBeVisible();
  await expect(page.locator('.remote-screen-panel')).toBeVisible();

  await page.locator('.hwmdm-nav-link:has-text("Maps / GPS")').click();
  await expect(page.locator('.gps-map-page')).toBeVisible();
  await expect(page.locator('.gps-page-toolbar')).toBeVisible();

  await page.locator('.hwmdm-nav-link:has-text("Personalization")').click();
  await expect(page.locator('h4:has-text("Admin console personalization")')).toBeVisible();
  await expect(page.locator('legend:has-text("Colors")')).toBeVisible();
  await expect(page.locator('legend:has-text("Preview")')).toBeVisible();

  const sidebarBox = await page.locator('.hwmdm-sidebar').boundingBox();
  const mainBox = await page.locator('.hwmdm-main').boundingBox();
  expect(sidebarBox.width).toBeGreaterThan(200);
  expect(mainBox.x).toBeGreaterThan(sidebarBox.x + sidebarBox.width - 1);
  expect(mainBox.width).toBeLessThanOrEqual(1440 - mainBox.x + 1);

  const bodyOverflow = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth
  }));
  expect(bodyOverflow.scrollWidth).toBeLessThanOrEqual(bodyOverflow.clientWidth + 4);

  expect(failedResponses).toEqual([]);
  expect(consoleErrors.filter(error => !error.includes('favicon'))).toEqual([]);
});
