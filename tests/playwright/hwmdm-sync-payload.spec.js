const { test, expect } = require('@playwright/test');

// Le o payload REAL que o servidor manda ao tablet no sync, pela cadeia publica.
// E' o mesmo endpoint que o dispositivo chama (rest/public/sync/configuration/<serial>).
const base = process.env.HWMDM_BASE_URL;
const device = process.env.HWMDM_DEVICE || 'R9XT108EM8T';

if (!base) {
  throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');
}

test('payload de sync mostra kiosk e app a instalar', async ({ request }) => {
  const url = `${base}/rest/public/sync/configuration/${device}`;
  const resp = await request.get(url, { timeout: 30000 });

  console.log('=== URL:', url);
  console.log('=== STATUS:', resp.status());

  const json = await resp.json();
  const d = json.data || json;

  console.log('=== kioskMode:', JSON.stringify(d.kioskMode));
  console.log('=== mainApp:', JSON.stringify(d.mainApp));
  console.log('=== kioskHome:', JSON.stringify(d.kioskHome),
              'kioskRecents:', JSON.stringify(d.kioskRecents),
              'kioskExit:', JSON.stringify(d.kioskExit));
  console.log('=== restrictions:', JSON.stringify(d.restrictions));
  console.log('=== pushOptions:', JSON.stringify(d.pushOptions));

  const apps = d.applications || [];
  console.log('=== APPLICATIONS (' + apps.length + '):');
  apps.forEach(a => {
    console.log(`   pkg=${a.pkg} version=${a.version} code=${a.versionCode} remove=${a.remove} url=${a.url}`);
  });

  expect(resp.status()).toBe(200);
});
