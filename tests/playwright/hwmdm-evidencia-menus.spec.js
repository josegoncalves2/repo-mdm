const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const fs = require('fs');
const { execFileSync } = require('child_process');

// EVIDENCIA 2, 4 e 5: percorre todo item de menu renderizado, registra erro de console,
// request >= 400 e o que sobrou na tela. Depois cava especificamente Backup/Import/Export
// e Acesso remoto, capturando o JSON real das chamadas de cada um.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio.');
const OUT = process.env.SHOTS || '/tmp';

const admin = `hwmdm_evm_${Date.now()}`;
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
  await page.locator('#username').waitFor({ timeout: 30000 });
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.locator('.hwmdm-nav-link').first().waitFor({ timeout: 30000 });
  await page.waitForTimeout(2500);
}

test('inventario de todos os itens de menu, com erros de console e requests falhadas', async ({ page }) => {
  test.setTimeout(900000);
  await page.setViewportSize({ width: 1600, height: 1000 });
  await login(page);

  const itens = await page.evaluate(() =>
    [...document.querySelectorAll('.hwmdm-nav-link')].map(a => ({
      rotulo: a.innerText.replace(/\s+/g, ' ').trim(),
      secao: (a.closest('.hwmdm-nav-section') || {}).querySelector
        ? a.closest('.hwmdm-nav-section').querySelector('.hwmdm-nav-heading').innerText.trim() : ''
    })).filter(x => x.rotulo));

  console.log(`\n===== ITENS DE MENU RENDERIZADOS: ${itens.length} =====`);
  itens.forEach(i => console.log(`  [${i.secao}] ${i.rotulo}`));

  const linhas = [];

  for (const { rotulo, secao } of itens) {
    const falhas = [];
    const errosJs = [];
    const onResp = r => { if (r.status() >= 400) falhas.push(`${r.status()} ${r.url().replace(base, '')}`); };
    const onFail = r => falhas.push(`NETFAIL ${r.url().replace(base, '')}`);
    const onCons = m => { if (m.type() === 'error') errosJs.push(m.text().replace(/\s+/g, ' ').slice(0, 200)); };
    const onPageErr = e => errosJs.push('PAGEERROR ' + e.message.replace(/\s+/g, ' ').slice(0, 200));

    page.on('response', onResp);
    page.on('requestfailed', onFail);
    page.on('console', onCons);
    page.on('pageerror', onPageErr);

    let clicou = true;
    try {
      const alvo = page.locator(`.hwmdm-nav-link`).first();
      await alvo.scrollIntoViewIfNeeded({ timeout: 5000 });
      await alvo.click({ timeout: 8000 });
    } catch (e) { clicou = false; }
    await page.waitForTimeout(4000);

    const estado = await page.evaluate(() => {
      const main = document.querySelector('.hwmdm-main-surface') || document.body;
      const texto = (main.innerText || '').trim();
      return {
        rota: location.hash,
        chars: texto.length,
        amostra: texto.replace(/\s+/g, ' ').slice(0, 130),
        botoes: main.querySelectorAll('button, [ng-click]').length,
        inputs: main.querySelectorAll('input, select, textarea').length,
        linhasTabela: main.querySelectorAll('tbody tr').length,
        avisoVazio: /não implementad|not implemented|coming soon|em breve|no data|nothing here/i.test(texto)
      };
    });

    page.off('response', onResp); page.off('requestfailed', onFail);
    page.off('console', onCons); page.off('pageerror', onPageErr);

    const probs = [];
    if (!clicou) probs.push('nao foi possivel clicar (item inalcancavel no viewport)');
    if (estado.chars < 60) probs.push(`tela quase vazia (${estado.chars} chars)`);
    if (estado.botoes === 0 && estado.inputs === 0) probs.push('nenhum controle');
    const falhasU = [...new Set(falhas)];
    const errosU = [...new Set(errosJs)];
    if (falhasU.length) probs.push(`req com erro: ${falhasU.slice(0, 4).join(' | ')}`);
    if (errosU.length) probs.push(`erro JS: ${errosU.slice(0, 3).join(' | ')}`);

    const veredito = !clicou ? 'INALCANCAVEL'
      : (falhasU.length || errosU.length) ? 'QUEBRADO'
      : estado.chars < 60 ? 'VAZIO' : 'funciona';

    linhas.push({ secao, rotulo, rota: estado.rota, clicou, ...estado,
      falhas: falhasU, erros: errosU, veredito });

    console.log(`\n--- [${secao}] ${rotulo}  =>  ${veredito}`);
    console.log(`    rota=${estado.rota} chars=${estado.chars} botoes=${estado.botoes} inputs=${estado.inputs} linhasTabela=${estado.linhasTabela}`);
    console.log(`    texto: "${estado.amostra}"`);
    falhasU.forEach(f => console.log(`    REQ  >>> ${f}`));
    errosU.forEach(e => console.log(`    JS   >>> ${e}`));

    await page.screenshot({ path: `${OUT}/shots/menu-${rotulo.replace(/[^a-zA-Z0-9]/g, '_')}.png`, fullPage: true });
  }

  fs.writeFileSync(`${OUT}/menus.json`, JSON.stringify(linhas, null, 2));

  console.log('\n\n===== TABELA FINAL =====');
  console.log('| secao | rotulo | rota | carregou | chars | erros console | requests com erro | veredito |');
  console.log('|---|---|---|---|---|---|---|---|');
  linhas.forEach(l => console.log(
    `| ${l.secao} | ${l.rotulo} | ${l.rota} | ${l.clicou ? 'S' : 'N'} | ${l.chars} | ${l.erros.length ? l.erros.join(' ; ').slice(0, 120) : '-'} | ${l.falhas.length ? l.falhas.join(' ; ').slice(0, 120) : '-'} | ${l.veredito} |`));
});

test('backup/import/export: onde esta e quantos cliques custa', async ({ page }) => {
  test.setTimeout(300000);
  await page.setViewportSize({ width: 1600, height: 1000 });
  await login(page);

  // Onde o item vive na barra lateral e ele esta visivel sem rolar?
  const geo = await page.evaluate(() => {
    const links = [...document.querySelectorAll('.hwmdm-nav-link')];
    const alvo = links.find(l => /backup/i.test(l.innerText));
    if (!alvo) return { achou: false, rotulos: links.map(l => l.innerText.trim()) };
    const side = document.querySelector('.hwmdm-sidebar');
    const r = alvo.getBoundingClientRect();
    const sr = side.getBoundingClientRect();
    return {
      achou: true,
      rotulo: alvo.innerText.replace(/\s+/g, ' ').trim(),
      indiceNaBarra: links.indexOf(alvo) + 1,
      totalItens: links.length,
      rect: { top: Math.round(r.top), bottom: Math.round(r.bottom) },
      viewportH: window.innerHeight,
      visivelSemRolar: r.top >= 0 && r.bottom <= window.innerHeight,
      sidebarScrollHeight: side.scrollHeight,
      sidebarClientHeight: side.clientHeight,
      sidebarPrecisaRolar: side.scrollHeight > side.clientHeight + 2,
      sidebarOverflowY: getComputedStyle(side).overflowY
    };
  });
  console.log('\n===== POSICAO DO ITEM "Backup & restore" NA BARRA LATERAL (1600x1000) =====');
  console.log(JSON.stringify(geo, null, 2));

  // 1 clique a partir da tela inicial?
  const capt = [];
  page.on('response', async r => {
    if (!/\/rest\//.test(r.url())) return;
    try { capt.push({ s: r.status(), u: r.url().replace(base, ''), b: (await r.text()).slice(0, 900) }); } catch (e) {}
  });

  const alvo = page.locator('.hwmdm-nav-link:has-text("Backup")').first();
  await alvo.scrollIntoViewIfNeeded();
  await alvo.click();
  await page.waitForTimeout(5000);

  const tela = await page.evaluate(() => {
    const m = document.querySelector('.hwmdm-main-surface');
    return {
      rota: location.hash,
      titulo: (m.querySelector('h3') || {}).innerText || '',
      texto: m.innerText.replace(/\s+/g, ' ').slice(0, 900),
      botoes: [...m.querySelectorAll('button, label.btn')].map(b => ({
        texto: b.innerText.replace(/\s+/g, ' ').trim(),
        visivel: !!(b.offsetWidth || b.offsetHeight),
        desabilitado: b.disabled === true
      })),
      inputsFile: m.querySelectorAll('input[type=file]').length
    };
  });
  console.log('\n===== TELA DE BACKUP/IMPORT/EXPORT (1 clique a partir do painel) =====');
  console.log(JSON.stringify(tela, null, 2));

  console.log('\n===== REQUESTS QUE A TELA DE BACKUP FEZ =====');
  capt.forEach(c => console.log(`HTTP ${c.s} ${c.u}\n   ${c.b}`));

  await page.screenshot({ path: `${OUT}/shots/backup-1600.png`, fullPage: true });
  expect(geo.achou, 'nao existe item de menu de backup').toBe(true);
});

test('acesso remoto: placeholder ou coisa real', async ({ page }) => {
  test.setTimeout(300000);
  await page.setViewportSize({ width: 1600, height: 1000 });

  const capt = [];
  page.on('response', async r => {
    if (!/\/rest\//.test(r.url())) return;
    try { capt.push({ s: r.status(), u: r.url().replace(base, ''), b: (await r.text()).slice(0, 2500) }); } catch (e) {}
  });
  const erros = [];
  page.on('pageerror', e => erros.push(e.message.slice(0, 200)));
  page.on('console', m => { if (m.type() === 'error') erros.push(m.text().slice(0, 200)); });

  await login(page);
  capt.length = 0;

  await page.locator('.hwmdm-nav-link:has-text("Remote access")').first().click();
  await page.waitForTimeout(6000);

  console.log('\n===== REQUESTS DA TELA "Remote access" =====');
  capt.forEach(c => console.log(`HTTP ${c.s} ${c.u}\n   ${c.b}\n`));

  const t1 = await page.evaluate(() => {
    const m = document.querySelector('.hwmdm-main-surface');
    return {
      rota: location.hash,
      texto: m.innerText.replace(/\s+/g, ' ').slice(0, 800),
      devices: [...m.querySelectorAll('.remote-device-item')].map(d => d.innerText.replace(/\s+/g, ' ').trim()),
      botoes: [...m.querySelectorAll('button')].map(b => b.innerText.replace(/\s+/g, ' ').trim()).filter(Boolean)
    };
  });
  console.log('===== ESTADO INICIAL DA TELA REMOTE =====');
  console.log(JSON.stringify(t1, null, 2));
  await page.screenshot({ path: `${OUT}/shots/remote-inicial.png`, fullPage: true });

  // Seleciona o primeiro device e ve o que aparece
  const primeiro = page.locator('.remote-device-item').first();
  if (await primeiro.count()) {
    capt.length = 0;
    await primeiro.click();
    await page.waitForTimeout(5000);
    const t2 = await page.evaluate(() => {
      const m = document.querySelector('.hwmdm-main-surface');
      return {
        texto: m.innerText.replace(/\s+/g, ' ').slice(0, 1500),
        botoes: [...m.querySelectorAll('button')].map(b => ({
          t: b.innerText.replace(/\s+/g, ' ').trim(), dis: b.disabled
        })).filter(b => b.t),
        temImagemTela: !!m.querySelector('.remote-screen-shell img, img.remote-screenshot')
      };
    });
    console.log('\n===== APOS SELECIONAR O 1o DEVICE =====');
    console.log(JSON.stringify(t2, null, 2));
    console.log('\n===== REQUESTS DA SELECAO =====');
    capt.forEach(c => console.log(`HTTP ${c.s} ${c.u}\n   ${c.b}\n`));
    await page.screenshot({ path: `${OUT}/shots/remote-selecionado.png`, fullPage: true });
  } else {
    console.log('\n>>> NENHUM .remote-device-item renderizado');
  }

  console.log('\n===== ERROS JS NA TELA REMOTE =====');
  console.log(erros.length ? [...new Set(erros)].join('\n') : 'nenhum');
});
