const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// Valida a reforma de UI pelo dominio publico real. Sem curl, sem localhost.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

const admin = `hwmdm_ui_admin_${Date.now()}`;
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
  // O bootstrap do AngularJS as vezes demora a montar o formulario de login na
  // primeira carga; recarrega em vez de estourar o timeout do teste.
  for (let attempt = 1; attempt <= 3; attempt++) {
    await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
    try {
      await page.waitForSelector('#username', { timeout: 8000 });
      break;
    } catch (e) {
      console.log(`    (login ainda nao montou, tentativa ${attempt})`);
      if (attempt === 3) throw e;
    }
  }
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForSelector('.hwmdm-sidebar', { timeout: 20000 });
  await page.waitForTimeout(1500);
}

const SIZES = [
  { name: '1440x900 desktop', w: 1440, h: 900, drawer: false },
  { name: '1024x768 tablet', w: 1024, h: 768, drawer: true },
  { name: '768x1024 tablet portrait', w: 768, h: 1024, drawer: true },
  { name: '390x844 phone', w: 390, h: 844, drawer: true }
];

test('layout responde nos 4 tamanhos sem estouro horizontal', async ({ page }) => {
  await signIn(page);

  for (const size of SIZES) {
    await page.setViewportSize({ width: size.w, height: size.h });
    await page.waitForTimeout(600);

    const m = await page.evaluate(() => {
      const q = (s) => document.querySelector(s);
      const vis = (el) => {
        if (!el) return false;
        const r = el.getBoundingClientRect();
        return r.width > 0 && r.height > 0 && getComputedStyle(el).display !== 'none';
      };
      const sidebar = q('.hwmdm-sidebar');
      const sr = sidebar ? sidebar.getBoundingClientRect() : null;
      return {
        docWidth: document.documentElement.scrollWidth,
        viewport: window.innerWidth,
        navbarVisible: vis(q('.hwmdm-navbar')),
        toggleVisible: vis(q('.hwmdm-nav-toggle')),
        sidebarLeft: sr ? Math.round(sr.left) : null,
        sidebarWidth: sr ? Math.round(sr.width) : null,
        sidebarPos: sidebar ? getComputedStyle(sidebar).position : null,
        currentLabel: (q('.hwmdm-navbar-current') || {}).textContent || null
      };
    });

    console.log(`\n=== ${size.name}`);
    console.log(`    scrollWidth=${m.docWidth} innerWidth=${m.viewport} overflow=${m.docWidth - m.viewport}px`);
    console.log(`    navbar visivel=${m.navbarVisible} hamburger visivel=${m.toggleVisible}`);
    console.log(`    sidebar position=${m.sidebarPos} left=${m.sidebarLeft} width=${m.sidebarWidth}`);
    if (m.currentLabel) console.log(`    titulo na barra="${m.currentLabel.trim()}"`);

    // Nada de scroll horizontal na pagina (1px de folga para arredondamento).
    expect(m.docWidth, `overflow horizontal em ${size.name}`).toBeLessThanOrEqual(m.viewport + 1);

    if (size.drawer) {
      expect(m.navbarVisible, `barra de navegacao ausente em ${size.name}`).toBe(true);
      expect(m.toggleVisible, `botao de menu ausente em ${size.name}`).toBe(true);
      expect(m.sidebarPos, `sidebar deveria ser drawer em ${size.name}`).toBe('fixed');
      // Fechado => fora da tela.
      expect(m.sidebarLeft, `drawer deveria estar fechado em ${size.name}`).toBeLessThan(0);
    } else {
      expect(m.navbarVisible, `barra de drawer nao deveria aparecer em ${size.name}`).toBe(false);
      expect(m.sidebarPos).toBe('sticky');
      expect(m.sidebarLeft).toBeGreaterThanOrEqual(0);
    }

    await page.screenshot({ path: `test-results/ui-${size.w}x${size.h}.png`, fullPage: false });
  }
});

test('drawer abre, navega e fecha no telefone', async ({ page }) => {
  await signIn(page);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.waitForTimeout(600);

  await page.locator('.hwmdm-nav-toggle').click();
  await page.waitForTimeout(500);

  const open = await page.evaluate(() => {
    const s = document.querySelector('.hwmdm-sidebar').getBoundingClientRect();
    const scrim = document.querySelector('.hwmdm-nav-scrim');
    return {
      left: Math.round(s.left),
      width: Math.round(s.width),
      scrimVisible: scrim ? getComputedStyle(scrim).display !== 'none' : false,
      links: Array.from(document.querySelectorAll('.hwmdm-nav-link'))
        .map(b => b.textContent.trim()).filter(Boolean)
    };
  });
  console.log(`\n=== DRAWER ABERTO: left=${open.left} width=${open.width} scrim=${open.scrimVisible}`);
  console.log('=== ITENS DO MENU:\n   ' + open.links.join('\n   '));
  expect(open.left, 'drawer nao abriu').toBe(0);
  expect(open.scrimVisible, 'scrim nao apareceu').toBe(true);

  // Menu nao pode ter dois itens ambiguos "Devices" / "Devices table".
  const dupes = open.links.filter(l => /^devices$/i.test(l) || /devices table/i.test(l));
  console.log('=== ITENS AMBIGUOS "Devices*":', JSON.stringify(dupes));
  expect(dupes.filter(l => /devices table/i.test(l)).length, '"Devices table" ainda existe').toBe(0);

  // Backup precisa estar visivel e clicavel no menu.
  const backup = page.locator('.hwmdm-nav-link', { hasText: /backup/i });
  await expect(backup).toHaveCount(1);
  await backup.click();
  await page.waitForTimeout(1200);

  const afterNav = await page.evaluate(() => ({
    left: Math.round(document.querySelector('.hwmdm-sidebar').getBoundingClientRect().left),
    title: (document.querySelector('.hwmdm-navbar-current') || {}).textContent || '',
    h3: (document.querySelector('.governance-page h3') || {}).textContent || ''
  }));
  console.log(`=== APOS CLICAR EM BACKUP: drawer left=${afterNav.left} barra="${afterNav.title.trim()}" pagina h3="${afterNav.h3.trim()}"`);
  expect(afterNav.left, 'drawer nao fechou apos navegar').toBeLessThan(0);
  expect(afterNav.h3.toLowerCase()).toContain('backup');
});

test('botoes de backup/import/export estao visiveis e clicaveis', async ({ page }) => {
  await signIn(page);
  await page.setViewportSize({ width: 1440, height: 900 });

  await page.locator('.hwmdm-nav-link', { hasText: /backup/i }).click();
  await page.waitForTimeout(1500);

  const report = await page.evaluate(() => {
    const out = [];
    document.querySelectorAll('.governance-page button, .governance-page label.gov-file-btn').forEach(el => {
      const r = el.getBoundingClientRect();
      const cs = getComputedStyle(el);
      // Elemento no topo do ponto central: prova que nada (overlay do tour) esta por cima.
      const top = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2);
      out.push({
        text: el.textContent.trim().replace(/\s+/g, ' '),
        w: Math.round(r.width), h: Math.round(r.height),
        display: cs.display, visibility: cs.visibility, opacity: cs.opacity,
        clickable: !!(top && (el === top || el.contains(top)))
      });
    });
    const overlays = Array.from(document.querySelectorAll('.introjs-overlay, .introjs-helperLayer')).length;
    return { buttons: out, overlays };
  });

  console.log('\n=== BOTOES NA PAGINA DE BACKUP/IMPORT/EXPORT:');
  report.buttons.forEach(b => console.log(
    `   "${b.text}" ${b.w}x${b.h} display=${b.display} vis=${b.visibility} opacity=${b.opacity} clicavel=${b.clickable}`));
  console.log(`=== OVERLAYS intro.js na pagina: ${report.overlays}`);

  const wanted = ['export configuration', 'choose file to import', 'create backup now'];
  for (const w of wanted) {
    const found = report.buttons.find(b => b.text.toLowerCase().includes(w));
    expect(found, `botao "${w}" nao encontrado`).toBeTruthy();
    expect(found.w, `botao "${w}" tem largura zero`).toBeGreaterThan(0);
    expect(found.clickable, `botao "${w}" esta coberto por outro elemento`).toBe(true);
  }
  expect(report.overlays, 'overlay do tour ainda cobre a pagina').toBe(0);

  await page.screenshot({ path: 'test-results/ui-governance-1440.png', fullPage: true });
});

test('acesso remoto: acoes vem do catalogo do servidor e IP de proxy/tunel nao aparece como device', async ({ page }) => {
  await signIn(page);
  await page.setViewportSize({ width: 1440, height: 900 });

  const catalog = await page.evaluate(async () => {
    const r = await fetch('rest/private/devices/commands');
    return (await r.json()).data;
  });
  console.log('\n=== CATALOGO DO SERVIDOR (' + catalog.length + '):', catalog.join(', '));

  await page.locator('.hwmdm-nav-link', { hasText: /remote access/i }).click();
  await page.waitForTimeout(3000);

  const ui = await page.evaluate(() => ({
    groups: Array.from(document.querySelectorAll('.remote-command-group')).map(g => ({
      title: g.querySelector('.remote-command-group-title').textContent.trim(),
      buttons: Array.from(g.querySelectorAll('.remote-action-grid .btn')).map(b => b.textContent.trim())
    })),
    ipCells: Array.from(document.querySelectorAll('.remote-ip-cell')).map(c => ({
      label: c.querySelector('.remote-ip-label').textContent.trim(),
      value: c.querySelector('strong').textContent.trim(),
      note: c.textContent.trim().replace(/\s+/g, ' ')
    })),
    body: document.body.innerText,
    online: (document.querySelector('.remote-device-header .label') || {}).textContent || ''
  }));

  console.log('=== PALETA DE ACOES RENDERIZADA:');
  ui.groups.forEach(g => console.log(`   [${g.title}] ${g.buttons.join(' | ')}`));
  const rendered = ui.groups.reduce((n, g) => n + g.buttons.length, 0);
  console.log(`=== total de botoes renderizados: ${rendered} (catalogo do servidor: ${catalog.length})`);

  console.log('=== ROTULOS DE IP:');
  ui.ipCells.forEach(c => console.log(`   "${c.label}" -> ${c.value}`));

  expect(ui.groups.length, 'nenhum grupo de acoes renderizado').toBeGreaterThan(0);
  expect(rendered, 'a paleta nao cobre o catalogo do servidor').toBe(catalog.length);

  expect(ui.ipCells.length, 'IP do dispositivo nao foi renderizado').toBe(1);
  const labels = ui.ipCells.map(c => c.label.toLowerCase());
  expect(labels.some(l => l.includes('device ip')), 'falta o rotulo do IP do dispositivo').toBe(true);
  expect(ui.body, 'IP de tunel apareceu na tela').not.toContain('10.1.1.1');
  expect(ui.body, 'IP de gerenciamento do proxy apareceu na tela').not.toContain('10.0.17.106');
  expect(ui.body, 'IP de gerenciamento do proxy apareceu na tela').not.toContain('10.0.9.1');

  await page.screenshot({ path: 'test-results/ui-remote-1440.png', fullPage: true });
});

test('console limpo: sem 404 de bundle de plugin e sem erro de JS', async ({ page }) => {
  const failures = [];
  const errors = [];
  page.on('response', r => { if (r.status() >= 400) failures.push(`${r.status()} ${r.url()}`); });
  page.on('pageerror', e => errors.push(String(e)));

  await signIn(page);
  for (const tab of [/dashboard/i, /remote access/i, /backup/i, /device profiles/i, /plugins/i]) {
    await page.locator('.hwmdm-nav-link', { hasText: tab }).first().click();
    await page.waitForTimeout(1800);
  }

  console.log('\n=== RESPOSTAS >= 400 (' + failures.length + '):');
  failures.forEach(f => console.log('   ' + f));
  console.log('=== ERROS DE JS (' + errors.length + '):');
  errors.forEach(e => console.log('   ' + e));

  const i18n404 = failures.filter(f => f.includes('/i18n/') && f.startsWith('404'));
  console.log('=== 404 de bundle i18n de plugin: ' + (i18n404.length ? i18n404.join(' , ') : 'NENHUM'));
  expect(i18n404, 'ainda ha 404 de bundle i18n de plugin').toEqual([]);
  expect(errors, 'erro de JS no console').toEqual([]);
});

test('editor de perfil e um fluxo linear, sem abas e com uma unica barra de salvar', async ({ page }) => {
  await signIn(page);
  await page.setViewportSize({ width: 1440, height: 900 });

  await page.locator('.hwmdm-nav-link', { hasText: /device profiles/i }).click();
  await page.waitForTimeout(2000);

  // abre o primeiro perfil da lista
  const configId = parseInt(psql('select id from configurations order by id limit 1;').trim(), 10);
  console.log('\n=== ABRINDO PERFIL id=' + configId);
  await page.goto(`${base}/#/configuration/${configId}`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('.cfg-step', { timeout: 20000 });
  await page.waitForTimeout(1500);

  const m = await page.evaluate(() => ({
    tabsets: document.querySelectorAll('tabset, .nav-tabs, tab').length,
    steps: Array.from(document.querySelectorAll('.cfg-step')).map(s => ({
      id: s.id,
      title: s.querySelector('h4').textContent.trim(),
      fields: s.querySelectorAll('input, select, textarea').length
    })),
    navLinks: Array.from(document.querySelectorAll('.cfg-step-link span:last-child')).map(e => e.textContent.trim()),
    saveButtons: document.querySelectorAll(".cfg-savebar button").length,
    saveBars: document.querySelectorAll('.cfg-savebar').length,
    allowKioskHeaders: Array.from(document.querySelectorAll('th')).filter(th => /allow in kiosk/i.test(th.textContent)).length,
    allowKioskChecks: document.querySelectorAll('input[title*="lock task"]').length,
    saveBarPinned: (() => { const b = document.querySelector('.cfg-savebar');
      return b ? { position: getComputedStyle(b).position, bottom: Math.round(b.getBoundingClientRect().bottom), viewportH: window.innerHeight } : null; })(),
    docWidth: document.documentElement.scrollWidth,
    viewport: window.innerWidth
  }));

  console.log('=== ELEMENTOS DE ABA RESTANTES (tabset/tab/.nav-tabs): ' + m.tabsets);
  console.log('=== ETAPAS DO FLUXO:');
  m.steps.forEach((s, i) => console.log(`   ${i + 1}. ${s.title}  (#${s.id}, ${s.fields} campos)`));
  console.log('=== JUMP BAR: ' + m.navLinks.join(' | '));
  console.log(`=== BARRAS DE SALVAR: ${m.saveBars} (botoes: ${m.saveButtons}) posicao=${m.saveBarPinned.position} bottom=${m.saveBarPinned.bottom} viewport=${m.saveBarPinned.viewportH}`);
  console.log(`=== ALLOWLIST DE KIOSK NO PERFIL: headers=${m.allowKioskHeaders} checkboxes=${m.allowKioskChecks}`);
  console.log(`=== overflow horizontal: ${m.docWidth - m.viewport}px`);

  expect(m.tabsets, 'ainda existem abas no editor de perfil').toBe(0);
  expect(m.steps.length, 'esperava 6 etapas').toBe(6);
  expect(m.saveBars, 'deveria haver exatamente uma barra de salvar').toBe(1);
  expect(m.saveButtons, 'a barra unica deve ter Salvar / Salvar e fechar / Cancelar').toBe(3);
  expect(m.allowKioskHeaders, 'a coluna Allow in kiosk sumiu do perfil').toBeGreaterThan(0);
  expect(m.allowKioskChecks, 'nenhum checkbox de allowlist de kiosk apareceu no perfil').toBeGreaterThan(0);
  expect(m.saveBarPinned.position, 'a barra de salvar nao esta fixa').toBe('fixed');
  expect(m.docWidth).toBeLessThanOrEqual(m.viewport + 1);

  // As tres etapas de APK/aplicativo tem que ser adjacentes.
  const idx = (re) => m.steps.findIndex(s => re.test(s.title));
  const apps = idx(/apps delivered/i), agent = idx(/agent/i), per = idx(/per-app/i);
  console.log(`=== POSICAO DAS ETAPAS DE APLICATIVO: apps=${apps + 1} agente=${agent + 1} por-app=${per + 1}`);
  expect(agent, 'as etapas de aplicativo nao ficaram adjacentes').toBe(apps + 1);
  expect(per, 'as etapas de aplicativo nao ficaram adjacentes').toBe(agent + 1);

  // jump link tem que levar a etapa ao topo util
  await page.locator('.cfg-step-link', { hasText: /per-app settings/i }).click();
  await page.waitForTimeout(1200);
  const jumped = await page.evaluate(() => Math.round(document.getElementById('cfg-appsettings').getBoundingClientRect().top));
  console.log('=== APOS CLICAR NO JUMP LINK "Per-app settings": topo da secao em y=' + jumped);
  expect(jumped, 'o jump link nao levou a secao para a area visivel').toBeLessThan(300);
  expect(jumped, 'a secao ficou escondida atras da barra fixa').toBeGreaterThan(-20);

  await page.screenshot({ path: 'test-results/ui-configuration-1440.png', fullPage: false });

  await page.setViewportSize({ width: 390, height: 844 });
  await page.waitForTimeout(800);
  const phone = await page.evaluate(() => ({ docWidth: document.documentElement.scrollWidth, viewport: window.innerWidth }));
  console.log(`=== editor de perfil a 390px: overflow=${phone.docWidth - phone.viewport}px`);
  expect(phone.docWidth).toBeLessThanOrEqual(phone.viewport + 1);
  await page.screenshot({ path: 'test-results/ui-configuration-390.png', fullPage: false });
});

test('editor de perfil nao envia PUT quando validacao obrigatoria falha', async ({ page }) => {
  await signIn(page);
  await page.setViewportSize({ width: 1440, height: 900 });

  const configId = parseInt(psql('select id from configurations order by id limit 1;').trim(), 10);
  await page.goto(`${base}/#/configuration/${configId}`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('.cfg-savebar', { timeout: 20000 });
  await page.waitForTimeout(1500);

  let putCount = 0;
  await page.route('**/rest/private/configurations', route => {
    if (route.request().method() === 'PUT') putCount++;
    route.abort();
  });

  await page.locator('#password-c').fill('');
  await page.locator('.cfg-savebar .btn-primary').click();
  await page.waitForTimeout(1000);

  const error = await page.locator('.error').first().innerText();
  console.log('\n=== VALIDACAO DE PERFIL SEM SENHA: erro="' + error.trim() + '" PUTs=' + putCount);
  expect(error.trim().length, 'nenhuma mensagem de validacao apareceu').toBeGreaterThan(0);
  expect(putCount, 'o editor chamou PUT mesmo com campo obrigatorio invalido').toBe(0);
});

test('editor de perfil expoe controle grafico de GPS e salva politica coerente', async ({ page }) => {
  await signIn(page);
  await page.setViewportSize({ width: 1440, height: 900 });

  const configId = parseInt(psql("select id from configurations where name = 'Kiosk Total (6.37.3)'").trim(), 10);
  await page.goto(`${base}/#/configuration/${configId}`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('[data-testid="location-policy-panel"]', { timeout: 20000 });
  await page.waitForTimeout(1800);

  await page.locator('[data-testid="enable-managed-gps"]').click();
  await page.locator('[data-testid="use-latest-mdm-agent"]').click();
  await expect.poll(async () => page.evaluate(() => {
    const s = angular.element(document.querySelector('[data-testid="location-policy-panel"]')).scope();
    return s.mainApp && s.mainApp.url;
  }), { timeout: 10000 }).toContain('-kiosk.apk');

  const model = await page.evaluate(() => {
    const s = angular.element(document.querySelector('[data-testid="location-policy-panel"]')).scope();
    return {
      gps: s.configuration.gps,
      requestUpdates: s.configuration.requestUpdates,
      disableLocation: s.configuration.disableLocation,
      appPermissions: s.configuration.appPermissions,
      restrictions: s.configuration.restrictions || '',
      mainAppUrl: s.mainApp && s.mainApp.url
    };
  });
  expect(model.gps).toBe(true);
  expect(model.requestUpdates).toBe('GPS');
  expect(model.disableLocation).toBe(false);
  expect(model.appPermissions).toBe('GRANTALL');
  expect(model.restrictions).not.toContain('no_share_location');
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

  console.log('\n=== GPS GUI PAYLOAD:', JSON.stringify({
    gps: payload && payload.gps,
    requestUpdates: payload && payload.requestUpdates,
    disableLocation: payload && payload.disableLocation,
    appPermissions: payload && payload.appPermissions,
    restrictions: payload && payload.restrictions
  }));

  expect(payload, 'o editor nao tentou salvar o perfil').toBeTruthy();
  expect(payload.gps).toBe(true);
  expect(payload.requestUpdates).toBe('GPS');
  expect(payload.disableLocation).toBe(false);
  expect(payload.appPermissions).toBe('GRANTALL');
  expect(payload.restrictions || '').not.toContain('no_share_location');
});

test('editor de perfil expoe kiosk completo, APK MDM e allowlist pela GUI', async ({ page }) => {
  await signIn(page);
  await page.setViewportSize({ width: 1440, height: 900 });

  const configId = parseInt(psql("select id from configurations where name = 'Kiosk Total (6.37.3)'").trim(), 10);
  await page.goto(`${base}/#/configuration/${configId}`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('[data-testid="kiosk-control-center"]', { timeout: 20000 });
  await page.waitForTimeout(1800);

  await page.locator('[data-testid="kiosk-lockdown-preset"]').click();
  const model = await page.evaluate(() => {
    const s = angular.element(document.querySelector('[data-testid="kiosk-control-center"]')).scope();
    return {
      kioskMode: s.configuration.kioskMode,
      kioskHome: s.configuration.kioskHome,
      kioskRecents: s.configuration.kioskRecents,
      kioskNotifications: s.configuration.kioskNotifications,
      kioskExit: s.configuration.kioskExit,
      kioskLockButtons: s.configuration.kioskLockButtons,
      pushOptions: s.configuration.pushOptions,
      requestUpdates: s.configuration.requestUpdates,
      mainAppId: s.configuration.mainAppId,
      mainAppPkg: s.mainApp && s.mainApp.pkg,
      restrictions: s.configuration.restrictions || '',
      allowed: (s.applications || []).filter(a => a.useKiosk).map(a => a.pkg)
    };
  });
  console.log('\n=== KIOSK GUI MODEL:', JSON.stringify(model));

  expect(model.kioskMode).toBe(true);
  expect(model.kioskHome).toBe(false);
  expect(model.kioskRecents).toBe(false);
  expect(model.kioskNotifications).toBe(false);
  expect(model.kioskExit).toBe(false);
  expect(model.kioskLockButtons).toBe(true);
  expect(model.pushOptions).toBe('polling');
  expect(model.requestUpdates).toBe('GPS');
  expect(model.mainAppId, 'APK MDM nao foi selecionado no perfil').toBeTruthy();
  expect(model.mainAppPkg).toBe('com.hmdm.launcher');
  expect(model.restrictions).toContain('no_factory_reset');
  expect(model.restrictions).toContain('no_config_wifi');
  expect(model.restrictions).not.toContain('no_share_location');
  expect(model.allowed).toContain('com.hmdm.launcher');

  let payload = null;
  await page.route('**/rest/private/configurations', async route => {
    if (route.request().method() === 'PUT') {
      payload = JSON.parse(route.request().postData());
    }
    await route.abort();
  });

  await page.locator('.cfg-savebar .btn-primary').click();
  await page.waitForTimeout(800);

  console.log('=== KIOSK GUI PAYLOAD:', JSON.stringify({
    kioskMode: payload && payload.kioskMode,
    kioskHome: payload && payload.kioskHome,
    kioskRecents: payload && payload.kioskRecents,
    kioskNotifications: payload && payload.kioskNotifications,
    kioskExit: payload && payload.kioskExit,
    kioskLockButtons: payload && payload.kioskLockButtons,
    mainAppId: payload && payload.mainAppId,
    pushOptions: payload && payload.pushOptions,
    requestUpdates: payload && payload.requestUpdates,
    restrictions: payload && payload.restrictions,
    allowed: payload && payload.applications && payload.applications.filter(a => a.useKiosk).map(a => a.pkg)
  }));

  expect(payload, 'o editor nao tentou salvar o perfil').toBeTruthy();
  expect(payload.kioskMode).toBe(true);
  expect(payload.kioskHome).toBe(false);
  expect(payload.kioskRecents).toBe(false);
  expect(payload.kioskNotifications).toBe(false);
  expect(payload.kioskExit).toBe(false);
  expect(payload.kioskLockButtons).toBe(true);
  expect(payload.pushOptions).toBe('polling');
  expect(payload.requestUpdates).toBe('GPS');
  expect(payload.mainAppId).toBeTruthy();
  expect(payload.restrictions || '').toContain('no_factory_reset');
  expect(payload.restrictions || '').not.toContain('no_share_location');
  expect(payload.applications.filter(a => a.useKiosk).map(a => a.pkg)).toContain('com.hmdm.launcher');
});

test('rotas mortas do layout antigo nao existem mais', async ({ page }) => {
  await signIn(page);

  // Arquivos orfaos removidos: tem que dar 404 no dominio real.
  const gone = [
    'app/components/main/view/updates.html',
    'app/components/main/view/settings/hints.html',
    'app/components/main/view/settings/language.html',
    'app/components/main/view/settings/plugins.html',
    'app/components/main/controller/updates.controller.js',
    'app/components/main/view/uiBreadcrumbs.tpl.html'
  ];
  const statuses = await page.evaluate(async (paths) => {
    const out = {};
    for (const p of paths) { const r = await fetch(p); out[p] = r.status; }
    return out;
  }, gone);
  console.log('\n=== ARQUIVOS DO LAYOUT ANTIGO (esperado 404):');
  Object.entries(statuses).forEach(([p, s]) => console.log(`   ${s}  ${p}`));
  Object.entries(statuses).forEach(([p, s]) => expect(s, `${p} ainda esta servido`).toBe(404));

  // Rotas mortas viram redirect para uma tela real.
  for (const [route, expected] of [['updates', 'summary'], ['hints', 'summary'],
                                   ['pluginSettings', 'summary'], ['langSettings', 'commonSettings']]) {
    await page.goto(`${base}/#/${route}`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1800);
    const landed = page.url().split('#')[1];
    const bodyHasContent = await page.evaluate(() =>
      !!document.querySelector('.hwmdm-main-surface') &&
      document.querySelector('.hwmdm-main-surface').innerText.trim().length > 0);
    console.log(`   /#/${route}  ->  ${landed}   conteudo renderizado=${bodyHasContent}`);
    expect(landed, `/#/${route} nao redirecionou`).not.toContain(route);
    expect(bodyHasContent, `/#/${route} caiu numa tela vazia`).toBe(true);
  }

  // O menu do usuario nao pode mais oferecer "Updates".
  await page.goto(`${base}/#/summary`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1500);
  await page.locator('.hwmdm-user-button').click();
  await page.waitForTimeout(400);
  const menu = await page.evaluate(() =>
    Array.from(document.querySelectorAll('.header .dropdown-menu a')).map(a => a.textContent.trim()));
  console.log('=== MENU DO USUARIO: ' + JSON.stringify(menu));
  expect(menu.join(' ').toLowerCase()).not.toContain('update');
});

test('chave de localizacao do comando nao suportado existe e e usada', async ({ page }) => {
  await signIn(page);

  const keys = await page.evaluate(() => {
    const locales = Object.keys(document.localization || {});
    const out = {};
    locales.forEach(l => { out[l] = (document.localization[l] || {})['error.remote.command.unsupported'] || null; });
    return out;
  });
  console.log('\n=== error.remote.command.unsupported POR LOCALE:');
  Object.entries(keys).forEach(([l, v]) => console.log(`   ${l}: ${v === null ? '<AUSENTE>' : '"' + v + '"'}`));
  expect(keys['en_US'], 'chave ausente em en_US').toBeTruthy();
  expect(keys['pt_PT'], 'chave ausente em pt_PT').toBeTruthy();

  // O servidor realmente devolve essa chave para uma acao que ele nao suporta.
  const deviceId = parseInt(psql('select id from devices order by id limit 1;').trim(), 10);
  const res = await page.evaluate(async (id) => {
    const r = await fetch(`rest/private/devices/${id}/command`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'remote_touch', params: {} })
    });
    return { status: r.status, body: await r.text() };
  }, deviceId);
  console.log('=== POST acao nao suportada (remote_touch) ->', res.status, res.body);
  const json = JSON.parse(res.body);
  expect(json.status).toBe('ERROR');
  expect(json.message, 'servidor nao devolveu a chave esperada').toBe('error.remote.command.unsupported');

  // E o front traduz essa chave em vez de mostrar o identificador cru.
  const translated = await page.evaluate((k) => document.localization['en_US'][k], json.message);
  console.log(`=== front traduz "${json.message}" como: "${translated}"`);
  expect(translated).not.toBe(json.message);
});

test('atalho de backup/import/export aparece no dashboard', async ({ page }) => {
  await signIn(page);
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.locator('.hwmdm-nav-link', { hasText: /dashboard/i }).click();
  await page.waitForTimeout(2000);

  const panel = await page.evaluate(() => {
    const heads = Array.from(document.querySelectorAll('.summary-panel-header h3'));
    const h = heads.find(x => /backup|import|export/i.test(x.textContent));
    if (!h) return null;
    const section = h.closest('.summary-panel');
    return {
      title: h.textContent.trim(),
      buttons: Array.from(section.querySelectorAll('button')).map(b => {
        const r = b.getBoundingClientRect();
        const top = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2);
        return { text: b.textContent.trim(), w: Math.round(r.width), clickable: !!(top && (b === top || b.contains(top))) };
      })
    };
  });
  console.log('\n=== PAINEL NO DASHBOARD: ' + (panel ? panel.title : '<AUSENTE>'));
  if (panel) panel.buttons.forEach(b => console.log(`   "${b.text}" largura=${b.w} clicavel=${b.clickable}`));
  expect(panel, 'o dashboard nao tem atalho para backup/import/export').toBeTruthy();
  expect(panel.buttons.length).toBeGreaterThan(0);
  panel.buttons.forEach(b => expect(b.clickable, `botao "${b.text}" coberto`).toBe(true));

  await page.locator('.summary-panel button', { hasText: /open backups/i }).click();
  await page.waitForTimeout(1500);
  const landed = await page.evaluate(() => (document.querySelector('.governance-page h3') || {}).textContent || '');
  console.log('=== "Open backups" levou para: "' + landed.trim() + '"');
  expect(landed.toLowerCase()).toContain('backup');
});
