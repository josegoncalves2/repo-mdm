const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// Testa os campos do perfil que antes eram textarea de texto livre.
//
// O oraculo NAO e "o componente apareceu na tela". E' o valor gravado em
// configurations.restrictions / allowedclasses / adminextras depois de clicar em Salvar,
// lido de volta do banco. Um teste que so verificasse visibilidade passaria mesmo com o
// componente desconectado do model -- que e' o tipo de teste-mentira que ja aconteceu aqui.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

const admin = `hwmdm_opcoes_${Date.now()}`;
const password = crypto.randomBytes(18).toString('base64url');
const salt = '5YdSYHyg2U';
const PERFIL = `zz-teste-opcoes-${Date.now()}`;

function hash(raw) {
  const md5 = crypto.createHash('md5').update(raw).digest('hex').toUpperCase();
  return crypto.createHash('sha1').update(md5 + salt).digest('hex');
}
function psql(sql) {
  return execFileSync('docker', ['exec', '-i', 'source-postgresql-1', 'sh', '-c',
    'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -A'],
    { input: sql, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] }).trim();
}

// psql devolve a linha de dados E a tag do comando ("INSERT 0 1"), entao um .trim()
// simples produz "17\nINSERT 0 1" -- que virou id de rota invalido e fez a tela nunca
// carregar o perfil. Aqui fica so a primeira linha, e vazio e' erro, nao silencio.
function psqlObrigatorio(sql, oQue) {
  const r = psql(sql).split('\n')[0].trim();
  if (!r) throw new Error(`psql nao retornou valor para ${oQue}. SQL:\n${sql}`);
  return r;
}

let perfilId;

test.beforeAll(() => {
  psql(`insert into users(login, email, name, password, customerid, userroleid, alldevicesavailable, allconfigavailable, passwordreset, authtoken)
        select '${admin}', '${admin}@l.test', '${admin}', '${hash(password)}', customerid, 2, true, true, false, null
        from users where login='admin';`);
  // Perfil descartavel: nenhum tablet real aponta para ele, entao o teste nao
  // pode danificar producao.
  perfilId = psqlObrigatorio(`insert into configurations(name, type, customerid, qrcodekey, restrictions, allowedclasses, adminextras, password, pushoptions, mainappid, eventreceivingcomponent)
                   select '${PERFIL}', 0, c.id, md5(random()::text), '', '', '', 'teste1234', 'polling', 10086, 'com.hmdm.launcher.AdminReceiver'
                   from customers c limit 1 returning id;`, 'criacao do perfil de teste');
  // O editor exige que o app principal esteja de fato vinculado ao perfil; sem esta linha
  // ele recusa qualquer save com "wrong main application" -- e o primeiro save da suite
  // (que envia applications: []) desfazia o vinculo para os testes seguintes.
  psql(`insert into configurationapplications(configurationid, applicationid, applicationversionid, action, showicon)
        values (${perfilId}, 46, 10086, 1, false);`);
});

// Cada teste comeca do mesmo estado: os testes gravam de verdade no perfil, entao sem
// isto um teste herda o resultado do anterior.
test.beforeEach(() => {
  psql(`update configurations set restrictions='', allowedclasses='', adminextras='',
        mainappid=10086, password='teste1234', pushoptions='polling',
        eventreceivingcomponent='com.hmdm.launcher.AdminReceiver' where id=${perfilId};`);
  psql(`insert into configurationapplications(configurationid, applicationid, applicationversionid, action, showicon)
        select ${perfilId}, 46, 10086, 1, false
        where not exists (select 1 from configurationapplications
                          where configurationid=${perfilId} and applicationid=46);`);
});

test.afterAll(() => {
  psql(`delete from configurationapplications where configurationid=${perfilId};`);
  psql(`delete from configurations where name='${PERFIL}';`);
  psql(`delete from users where login='${admin}';`);
});

async function entrar(page) {
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(3500);
}

// O editor e' uma pagina unica com 6 secoes e uma barra de navegacao (cfg-stepnav);
// os campos de restricao ficam na secao "Agent & kiosk behaviour" (#cfg-agent).
// Nao ha abas para clicar -- basta rolar ate o componente.
async function abrirPerfil(page) {
  await page.goto(`${base}/#/configuration/${perfilId}`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('mdm-option-picker', { timeout: 30000 });
  await page.locator('#cfg-agent').scrollIntoViewIfNeeded();
  await page.waitForTimeout(600);
}

// save() do controller valida antes de emitir o PUT e apenas escreve em $scope.errorMessage
// quando recusa. Sem ler esse campo, uma recusa e' indistinguivel de "gravou e nao persistiu".
async function salvar(page) {
  let put = null;
  const ouvinte = async r => {
    if (r.url().includes('/rest/private/configurations') && r.request().method() === 'PUT') {
      put = { status: r.status(), corpo: (await r.text()).slice(0, 200) };
    }
  };
  page.on('response', ouvinte);
  await page.locator('.cfg-savebar button', { hasText: /Guardar|Salvar|Save/i }).first().click();
  await page.waitForTimeout(3500);
  page.off('response', ouvinte);

  const estado = await page.evaluate(() => {
    const el = document.querySelector('form[name=configurationForm]');
    const s = el && window.angular.element(el).scope();
    return { errorMessage: (s && s.errorMessage) || null, saved: s ? s.saved : null };
  });
  console.log(`=== SALVAR -> PUT=${JSON.stringify(put)} erro=${JSON.stringify(estado.errorMessage)} saved=${estado.saved}`);
  return estado;
}

test('o campo Restrictions oferece o catalogo completo, nao um textarea vazio', async ({ page }) => {
  test.setTimeout(120000);
  await entrar(page);
  await abrirPerfil(page);

  const picker = page.locator('mdm-option-picker[ng-model="configuration.restrictions"]');
  await expect(picker).toBeVisible();

  // Nao pode ter sobrado textarea de texto livre para restrictions
  const textareaVelho = page.locator('textarea[name="restrictions-c"]');
  expect(await textareaVelho.count()).toBe(0);

  // 64 restricoes de UserManager + 3 de lock task
  const itens = picker.locator('.mdm-picker-item');
  const total = await itens.count();
  console.log(`=== opcoes de restricao renderizadas: ${total}`);
  expect(total).toBeGreaterThanOrEqual(67);

  // Cada opcao mostra rotulo legivel E a chave tecnica
  const primeiro = itens.first();
  await expect(primeiro.locator('.mdm-item-label')).not.toBeEmpty();
  await expect(primeiro.locator('.mdm-item-key')).not.toBeEmpty();
});

test('a busca filtra as opcoes', async ({ page }) => {
  test.setTimeout(120000);
  await entrar(page);
  await abrirPerfil(page);

  const picker = page.locator('mdm-option-picker[ng-model="configuration.restrictions"]');
  const antes = await picker.locator('.mdm-picker-item').count();

  await picker.locator('.mdm-picker-busca').fill('wifi');
  await page.waitForTimeout(600);
  const depois = await picker.locator('.mdm-picker-item').count();

  console.log(`=== busca "wifi": ${antes} -> ${depois}`);
  expect(depois).toBeGreaterThan(0);
  expect(depois).toBeLessThan(antes);
});

test('marcar uma restricao GRAVA no banco o valor exato', async ({ page }) => {
  test.setTimeout(120000);
  await entrar(page);
  await abrirPerfil(page);

  const picker = page.locator('mdm-option-picker[ng-model="configuration.restrictions"]');
  await picker.locator('.mdm-picker-busca').fill('fabrica');
  await page.waitForTimeout(600);

  const alvo = picker.locator('.mdm-picker-item', { hasText: 'no_factory_reset' }).first();
  await alvo.locator('input[type=checkbox]').click();
  await page.waitForTimeout(400);

  await salvar(page);

  const gravado = psql(`select restrictions from configurations where id=${perfilId};`);
  console.log(`=== restrictions no banco: "${gravado}"`);
  expect(gravado).toContain('no_factory_reset');
});

test('desmarcar remove a restricao do banco', async ({ page }) => {
  test.setTimeout(120000);
  psql(`update configurations set restrictions='no_factory_reset,no_safe_boot' where id=${perfilId};`);

  await entrar(page);
  await abrirPerfil(page);

  const picker = page.locator('mdm-option-picker[ng-model="configuration.restrictions"]');
  await picker.locator('.mdm-picker-busca').fill('seguranca');
  await page.waitForTimeout(600);

  const alvo = picker.locator('.mdm-picker-item', { hasText: 'no_safe_boot' }).first();
  // Tem de vir ja marcado: o componente le o valor existente do perfil
  await expect(alvo.locator('input[type=checkbox]')).toBeChecked();

  await alvo.locator('input[type=checkbox]').click();
  await page.waitForTimeout(400);
  await salvar(page);

  const gravado = psql(`select restrictions from configurations where id=${perfilId};`);
  console.log(`=== restrictions apos desmarcar: "${gravado}"`);
  expect(gravado).not.toContain('no_safe_boot');
  expect(gravado).toContain('no_factory_reset');
});

test('as opcoes perigosas vem marcadas com aviso', async ({ page }) => {
  test.setTimeout(120000);
  await entrar(page);
  await abrirPerfil(page);

  const picker = page.locator('mdm-option-picker[ng-model="configuration.restrictions"]');
  await picker.locator('.mdm-picker-busca').fill('instala');
  await page.waitForTimeout(600);

  const perigosa = picker.locator('.mdm-item-danger', { hasText: 'no_install_apps' }).first();
  await expect(perigosa).toBeVisible();
  const ajuda = await perigosa.locator('.mdm-item-help').textContent();
  console.log(`=== aviso em no_install_apps: ${ajuda.trim().slice(0, 90)}`);
  expect(ajuda).toMatch(/ATENCAO|proprio MDM|agente/i);
});

test('Allowed Activities e Admin Extras tambem sao guiados', async ({ page }) => {
  test.setTimeout(120000);
  await entrar(page);
  await abrirPerfil(page);

  // allowedClasses virou picker
  expect(await page.locator('textarea[name="allowedClasses-c"]').count()).toBe(0);
  const pickers = page.locator('mdm-option-picker');
  expect(await pickers.count()).toBeGreaterThanOrEqual(2);

  // adminExtras virou editor com lista de chaves aceitas
  expect(await page.locator('textarea[name="adminExtras-c"]').count()).toBe(0);
  const extras = page.locator('mdm-extras-editor');
  await expect(extras).toBeVisible();
  const chips = extras.locator('.mdm-extras-chips button');
  const qtd = await chips.count();
  console.log(`=== parametros de admin extras oferecidos: ${qtd}`);
  expect(qtd).toBeGreaterThanOrEqual(8);
});

test('adicionar um admin extra grava o fragmento JSON correto', async ({ page }) => {
  test.setTimeout(120000);
  psql(`update configurations set adminextras='' where id=${perfilId};`);

  await entrar(page);
  await abrirPerfil(page);

  const extras = page.locator('mdm-extras-editor');
  await extras.locator('.mdm-extras-chips button', { hasText: /Pular tela/i }).first().click();
  await page.waitForTimeout(400);

  await salvar(page);

  const gravado = psql(`select adminextras from configurations where id=${perfilId};`);
  console.log(`=== adminextras no banco: ${gravado}`);
  expect(gravado).toContain('com.hmdm.SKIP_INTRO');
  // tem de ser fragmento valido: envolver em chaves precisa dar JSON parseavel
  expect(() => JSON.parse('{' + gravado + '}')).not.toThrow();
});

test('a tela nao quebra em celular, tablet e desktop', async ({ page }) => {
  test.setTimeout(150000);
  await entrar(page);

  const telas = [
    { nome: 'celular', width: 375, height: 812 },
    { nome: 'tablet', width: 768, height: 1024 },
    { nome: 'desktop', width: 1440, height: 900 }
  ];

  for (const t of telas) {
    await page.setViewportSize({ width: t.width, height: t.height });
    await abrirPerfil(page);

    const picker = page.locator('mdm-option-picker[ng-model="configuration.restrictions"]');
    await expect(picker).toBeVisible();

    // Nenhum scroll horizontal: o conteudo nao pode estourar a largura da janela
    const estouro = await page.evaluate(() =>
      document.documentElement.scrollWidth - document.documentElement.clientWidth);
    console.log(`=== ${t.nome} (${t.width}px): estouro horizontal = ${estouro}px`);
    expect(estouro).toBeLessThanOrEqual(1);

    // As caixas de selecao continuam clicaveis (nao ficaram com largura zero)
    const cx = picker.locator('input[type=checkbox]').first();
    const box = await cx.boundingBox();
    expect(box.width).toBeGreaterThan(0);
    expect(box.height).toBeGreaterThan(0);
  }
});
