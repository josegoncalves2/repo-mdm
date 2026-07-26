const { test, expect } = require('@playwright/test');

// Verifica que o APK publicado e' realmente baixavel pela cadeia real
// (dominio publico -> proxy -> tomcat), que e' o caminho que o tablet usa.
const base = process.env.HWMDM_BASE_URL;

if (!base) {
  throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');
}

test('APK do kiosk e baixavel pelo dominio publico', async ({ request }) => {
  const url = `${base}/files/hmdm-6.37.2-kiosk.apk`;
  const resp = await request.get(url, { timeout: 60000 });

  console.log('=== URL:', url);
  console.log('=== STATUS:', resp.status());
  console.log('=== CONTENT-TYPE:', resp.headers()['content-type']);
  console.log('=== CONTENT-LENGTH:', resp.headers()['content-length']);

  expect(resp.status(), `APK retornou ${resp.status()}`).toBe(200);

  const body = await resp.body();
  console.log('=== BYTES RECEBIDOS:', body.length);
  // APK e' um ZIP: assinatura PK\x03\x04
  console.log('=== ASSINATURA:', body.slice(0, 2).toString('ascii'));

  expect(body.length, 'APK veio truncado').toBe(7812895);
  expect(body.slice(0, 2).toString('ascii'), 'nao e um ZIP/APK valido').toBe('PK');
});
