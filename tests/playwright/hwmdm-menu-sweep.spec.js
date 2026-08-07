const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// Varre TODOS os itens do menu lateral, um por um, e julga cada um por tres coisas:
// carregou conteudo de verdade, nao quebrou nenhuma chamada de API, nao gerou erro de JS.
// Nenhum teste anterior fazia isso -- eles cobriam so' as telas que eu tinha mexido.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio.');

const OUT = process.env.SHOTS;
const admin = `hwmdm_sweep_${Date.now()}`;
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

test('todo item do menu abre uma tela que faz alguma coisa', async ({ page }) => {
  test.setTimeout(600000);
  await page.setViewportSize({ width: 1600, height: 1000 });

  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(4000);

  const itens = await page.evaluate(() =>
    [...document.querySelectorAll('.hwmdm-nav-link')]
      .map(a => a.innerText.trim()).filter(Boolean));
  console.log(`=== ITENS DE MENU ENCONTRADOS (${itens.length}): ${itens.join(' | ')}\n`);
  expect(itens.length, 'nenhum item de menu encontrado').toBeGreaterThan(5);

  const quebrados = [];

  for (const item of itens) {
    const falhas = [];
    const errosJs = [];
    const onResp = r => { if (r.status() >= 400) falhas.push(`${r.status()} ${r.url().replace(base, '')}`); };
    const onErr = m => { if (m.type() === 'error') errosJs.push(m.text().slice(0, 120)); };
    const onPageErr = e => errosJs.push('PAGEERROR ' + e.message.slice(0, 120));

    page.on('response', onResp);
    page.on('console', onErr);
    page.on('pageerror', onPageErr);

    const rotaAntes = page.url();
    try {
      await page.locator(`.hwmdm-nav-link:has-text("${item}")`).first().click({ timeout: 8000 });
    } catch (e) {
      quebrados.push({ item, motivo: 'nao foi possivel clicar no item' });
      page.off('response', onResp); page.off('console', onErr); page.off('pageerror', onPageErr);
      continue;
    }
    await page.waitForTimeout(3500);

    const estado = await page.evaluate(() => {
      const main = document.querySelector('.hwmdm-main-surface, .hwmdm-main, [ui-view]') || document.body;
      const texto = (main.innerText || '').trim();
      return {
        rota: location.hash,
        chars: texto.length,
        amostra: texto.replace(/\s+/g, ' ').slice(0, 110),
        interativos: main.querySelectorAll('button, input, select, textarea, a[ng-click], [ng-click]').length,
        linhasTabela: main.querySelectorAll('tbody tr').length,
        avisoVazio: /não implementad|not implemented|nao disponivel|not available|coming soon|em breve/i.test(texto)
      };
    });

    page.off('response', onResp); page.off('console', onErr); page.off('pageerror', onPageErr);

    const problemas = [];
    if (page.url() === rotaAntes && estado.rota === '') problemas.push('rota nao mudou');
    if (estado.chars < 40) problemas.push(`tela praticamente vazia (${estado.chars} chars)`);
    if (estado.interativos === 0) problemas.push('nenhum controle interativo');
    if (falhas.length) problemas.push(`${falhas.length} chamada(s) com erro: ${[...new Set(falhas)].slice(0, 3).join(', ')}`);
    if (errosJs.length) problemas.push(`${errosJs.length} erro(s) de JS: ${[...new Set(errosJs)].slice(0, 2).join(' / ')}`);
    if (estado.avisoVazio) problemas.push('a propria tela avisa que o recurso nao existe');

    const veredito = problemas.length ? 'QUEBRADO' : 'ok';
    console.log(`--- ${item.padEnd(24)} [${veredito}] rota=${estado.rota} chars=${estado.chars} ` +
                `controles=${estado.interativos} linhas=${estado.linhasTabela}`);
    console.log(`       "${estado.amostra}"`);
    problemas.forEach(p => console.log(`       >>> ${p}`));

    if (problemas.length) quebrados.push({ item, motivo: problemas.join(' ; ') });
    if (OUT) await page.screenshot({ path: `${OUT}/menu-${item.replace(/[^a-zA-Z0-9]/g, '_')}.png`, fullPage: true });
  }

  console.log(`\n=== RESUMO: ${itens.length - quebrados.length}/${itens.length} itens de menu funcionais`);
  quebrados.forEach(q => console.log(`   QUEBRADO: ${q.item} -- ${q.motivo}`));

  expect(quebrados.map(q => q.item),
    `${quebrados.length} de ${itens.length} itens do menu nao funcionam`).toEqual([]);
});
