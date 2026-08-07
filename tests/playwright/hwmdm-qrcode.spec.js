const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// O botao de QR code some da lista de perfis quando qrCodeAvailable() e' falso, e ele exige
// TRES coisas ao mesmo tempo (configurations.controller.js):
//     configuration.qrCodeKey && configuration.mainAppId > 0 && configuration.eventReceivingComponent
// Em 26/07/2026 o perfil "Kiosk Total" ficou com mainappid NULL e o botao sumiu sem aviso.
// Este teste falha se qualquer perfil perder o botao de novo.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

const admin = `hwmdm_qr_${Date.now()}`;
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

async function entrar(page) {
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(3500);
}

test('todo perfil apto mostra o botao de QR code na lista', async ({ page }) => {
  test.setTimeout(120000);
  await entrar(page);
  await page.goto(`${base}/#/configurations`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('table tbody tr', { timeout: 30000 });
  await page.waitForTimeout(1500);

  // Quem o banco diz que DEVE ter o botao
  const aptos = psql(`select name from configurations
                      where mainappid > 0 and qrcodekey is not null and qrcodekey <> ''
                        and eventreceivingcomponent is not null and eventreceivingcomponent <> ''
                      order by id;`).split('\n').map(s => s.trim()).filter(Boolean);
  expect(aptos.length).toBeGreaterThan(0);

  const semBotao = [];
  for (const nome of aptos) {
    const linha = page.locator('table tbody tr', { hasText: nome }).first();
    const botao = linha.locator('.glyphicon-qrcode');
    if (await botao.count() === 0) { semBotao.push(nome); }
  }
  console.log(`=== perfis aptos: ${aptos.length} | sem botao de QR: ${semBotao.length}`);
  if (semBotao.length) { console.log('=== FALTANDO EM:', semBotao.join(', ')); }
  expect(semBotao).toEqual([]);
});

test('o QR code do perfil Kiosk Total abre e gera imagem', async ({ page }) => {
  test.setTimeout(120000);
  await entrar(page);
  await page.goto(`${base}/#/configurations`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('table tbody tr', { timeout: 30000 });
  await page.waitForTimeout(1500);

  const linha = page.locator('table tbody tr', { hasText: 'Kiosk Total' }).first();
  const botao = linha.locator('.glyphicon-qrcode');
  await expect(botao).toBeVisible();

  await botao.click();
  await page.waitForTimeout(3500);

  // A tela de QR pede a imagem em rest/public/qr/... -- o oraculo e' a imagem carregar,
  // nao a rota mudar. Uma imagem quebrada tem naturalWidth 0.
  const img = page.locator('img[src*="qr"]').first();
  await expect(img).toBeVisible({ timeout: 20000 });
  const largura = await img.evaluate(e => e.naturalWidth);
  console.log(`=== imagem do QR: naturalWidth=${largura}px | url=${page.url()}`);
  expect(largura).toBeGreaterThan(50);
});
