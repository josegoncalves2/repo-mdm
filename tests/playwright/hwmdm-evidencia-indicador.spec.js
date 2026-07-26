const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const fs = require('fs');
const { execFileSync } = require('child_process');

// EVIDENCIA 1 e 6: o que a API devolve de verdade para statusCode/lastUpdate/publicIp
// e qual imagem cai no DOM para cada device. Nada aqui e' deduzido do fonte: tudo
// vem do JSON real interceptado e do src efetivo do <img> renderizado.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio.');
const OUT = process.env.SHOTS || '/tmp';

const admin = `hwmdm_evid_${Date.now()}`;
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

async function login(page) {
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(4000);
}

test('indicador online/offline e IP: JSON real vs DOM real', async ({ page }) => {
  test.setTimeout(240000);
  await page.setViewportSize({ width: 1600, height: 1000 });

  const capturados = [];
  page.on('response', async r => {
    const u = r.url();
    if (!/\/rest\/private\/devices\/search|\/rest\/private\/summary|\/rest\/private\/settings/.test(u)) return;
    try {
      const body = await r.text();
      capturados.push({ url: u.replace(base, ''), status: r.status(), body });
    } catch (e) { /* corpo ja consumido */ }
  });

  await login(page);

  // Aba de devices
  await page.locator('.hwmdm-nav-link:has-text("Devices")').first().click();
  await page.waitForTimeout(5000);

  const searchResp = capturados.filter(c => /devices\/search/.test(c.url)).pop();
  console.log('\n===== RESPOSTA CRUA DE /rest/private/devices/search =====');
  if (!searchResp) {
    console.log('NENHUMA request para devices/search foi capturada. URLs vistas:',
      capturados.map(c => c.url).join(', '));
  } else {
    console.log(`HTTP ${searchResp.status} ${searchResp.url}`);
    let j;
    try { j = JSON.parse(searchResp.body); } catch (e) { j = null; }
    if (j && j.data && j.data.items) {
      console.log(`totalItemsCount=${j.data.totalItemsCount} items=${j.data.items.length}`);
      j.data.items.forEach(d => {
        console.log(JSON.stringify({
          id: d.id, number: d.number, statusCode: d.statusCode, lastUpdate: d.lastUpdate,
          lastUpdateISO: d.lastUpdate ? new Date(d.lastUpdate).toISOString() : null,
          idadeMin: d.lastUpdate ? Math.round((Date.now() - d.lastUpdate) / 60000) : null,
          publicIp: d.publicIp, configFilesStatus: d.configFilesStatus,
          applicationsStatus: d.applicationsStatus
        }));
      });
      fs.writeFileSync(`${OUT}/devices-search.json`, JSON.stringify(j, null, 2));
    } else {
      console.log('CORPO (primeiros 2000 chars):', searchResp.body.slice(0, 2000));
    }
  }

  // Que <img> realmente aparece por linha, e a imagem carregou?
  const linhas = await page.evaluate(async () => {
    const out = [];
    const trs = [...document.querySelectorAll('table tbody tr')];
    for (const tr of trs) {
      const imgs = [...tr.querySelectorAll('img.device-indicator')].map(i => ({
        src: i.getAttribute('src'),
        resolved: i.src,
        naturalWidth: i.naturalWidth,
        complete: i.complete,
        title: i.getAttribute('title')
      }));
      out.push({ texto: tr.innerText.replace(/\s+/g, ' ').trim().slice(0, 160), imgs });
    }
    return out;
  });

  console.log('\n===== LINHAS DA TABELA DE DEVICES E SEUS <img class=device-indicator> =====');
  console.log(`linhas encontradas: ${linhas.length}`);
  linhas.forEach((l, i) => {
    console.log(`\n[linha ${i}] ${l.texto}`);
    l.imgs.forEach(im => console.log(
      `   img src="${im.src}" carregou=${im.complete && im.naturalWidth > 0} naturalWidth=${im.naturalWidth} title="${(im.title || '').replace(/\n/g, ' | ')}"`));
    if (!l.imgs.length) console.log('   (nenhum img.device-indicator nesta linha)');
  });

  // Cabecalhos visiveis (quais colunas o painel decidiu mostrar)
  const cabecalhos = await page.evaluate(() =>
    [...document.querySelectorAll('table thead th')].map(t => t.innerText.replace(/\s+/g, ' ').trim()));
  console.log('\n===== COLUNAS VISIVEIS NA TABELA DE DEVICES =====');
  console.log(JSON.stringify(cabecalhos));

  // Existe qualquer imagem quebrada na pagina inteira?
  const quebradas = await page.evaluate(() =>
    [...document.querySelectorAll('img')]
      .filter(i => i.complete && i.naturalWidth === 0)
      .map(i => i.src));
  console.log('\n===== IMAGENS QUEBRADAS NA TELA DE DEVICES =====');
  console.log(quebradas.length ? JSON.stringify(quebradas, null, 2) : 'nenhuma');

  await page.screenshot({ path: `${OUT}/shots/devices-1600.png`, fullPage: true });

  // ---------- SUMMARY: qual IP e qual status ele mostra ----------
  capturados.length = 0;
  await page.locator('.hwmdm-nav-link:has-text("Dashboard")').first().click();
  await page.waitForTimeout(5000);

  console.log('\n===== REQUESTS DO DASHBOARD (summary) =====');
  for (const c of capturados) {
    console.log(`HTTP ${c.status} ${c.url}  ->  ${c.body.slice(0, 1200)}`);
  }

  const recentes = await page.evaluate(() =>
    [...document.querySelectorAll('.summary-device-row')].map(r => ({
      texto: r.innerText.replace(/\s+/g, ' ').trim(),
      classeStatus: (r.querySelector('.summary-device-status') || {}).className,
      corStatus: r.querySelector('.summary-device-status')
        ? getComputedStyle(r.querySelector('.summary-device-status')).backgroundColor : null
    })));
  console.log('\n===== "Recent devices" NO DASHBOARD (texto + classe/cor do ponto de status) =====');
  console.log(JSON.stringify(recentes, null, 2));

  await page.screenshot({ path: `${OUT}/shots/summary-1600.png`, fullPage: true });

  expect(linhas.length, 'a tabela de devices nao renderizou nenhuma linha').toBeGreaterThan(0);
});
