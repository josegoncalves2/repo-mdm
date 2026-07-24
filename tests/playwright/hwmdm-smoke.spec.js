const { test, expect } = require('@playwright/test');

const base = process.env.HWMDM_BASE_URL || 'http://localhost:8080';
const badMarkers = [
  'OL' + ' MDM',
  'tab.dashboard',
  'tab.remote',
  'tab.theme.layout',
  'updates.title',
  'form.personalize',
  'remote.description',
  'button.refresh',
  'remote.available.devices'
];

async function expectCleanText(response, label) {
  expect(response.ok(), `${label} should load`).toBeTruthy();
  const text = await response.text();
  for (const marker of badMarkers) {
    expect(text.includes(marker), `${label} must not contain ${marker}`).toBeFalsy();
  }
  return text;
}

test('HWMDM deploy serves rebuilt UI and clean templates', async ({ page, request }) => {
  const indexResponse = await request.get(`${base}/`, { headers: { 'Cache-Control': 'no-cache' } });
  const index = await expectCleanText(indexResponse, 'index.html');
  expect(index).toContain('Headwind MDM');
  expect(index).toContain('app/app.js?v=hwmdm-20260723-1918');

  const appResponse = await request.get(`${base}/app/app.js?v=hwmdm-20260723-1918`);
  const app = await expectCleanText(appResponse, 'app.js');
  expect(app).toContain('content.html?v=hwmdm-20260723-1918');

  for (const path of [
    '/app/components/main/view/content.html?v=hwmdm-20260723-1918',
    '/app/components/main/view/summary.html?v=hwmdm-20260723-1918',
    '/app/components/main/view/remote.html?v=hwmdm-20260723-1918',
    '/app/components/main/view/gpsmap.html?v=hwmdm-20260723-1918',
    '/app/components/main/view/settings/personalize.html?v=hwmdm-20260723-1918',
    '/app/components/main/view/updates.html?v=hwmdm-20260723-1918'
  ]) {
    await expectCleanText(await request.get(`${base}${path}`), path);
  }

  await page.goto(`${base}/#/`, { waitUntil: 'domcontentloaded' });
  await expect(page).toHaveTitle(/Headwind MDM/);
});
