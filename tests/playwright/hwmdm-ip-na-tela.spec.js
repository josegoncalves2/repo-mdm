const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// Olha o que um humano VE na tela sobre IP, em vez de afirmar que um seletor existe.
// Varre a lista de devices e a tela de acesso remoto procurando se IP de
// gerenciamento/tunel foi apresentado como informacao operacional do aparelho.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio.');

const OUT = process.env.SHOTS;
const admin = `hwmdm_ip_${Date.now()}`;
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

test('IP de proxy/tunel nao aparece como IP operacional do device', async ({ page }) => {
  test.setTimeout(180000);
  await page.setViewportSize({ width: 1600, height: 1000 });
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(4000);

  const infraIps = ['10.1.1.1', '10.0.17.106', '10.0.9.1'];
  const ultimoSalto = psql('select distinct publicip from devices where publicip is not null limit 1;');
  console.log('=== IP DO ULTIMO SALTO NO BANCO:', ultimoSalto);

  for (const [nome, rota] of [['lista de devices', '#/devices'], ['acesso remoto', '#/remote']]) {
    await page.goto(`${base}/${rota}`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(4000);
    if (OUT) await page.screenshot({ path: `${OUT}/ip-${rota.replace(/[#/]/g, '_')}.png`, fullPage: true });

    // Cabecalhos de tabela: algum diz so' "IP"?
    const cabecalhos = await page.evaluate(() =>
      [...document.querySelectorAll('th')].map(t => t.innerText.trim()).filter(Boolean));
    console.log(`\n=== ${nome.toUpperCase()} -- cabecalhos de coluna:`);
    console.log('   ' + (cabecalhos.join(' | ') || '<nenhuma tabela>'));

    // Onde os enderecos de infraestrutura aparecem, e com que rotulo por perto?
    const ocorrencias = await page.evaluate(ips => {
      const achados = {};
      ips.forEach(ip => { achados[ip] = []; });
      const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
      let n;
      while ((n = walker.nextNode())) {
        for (const ip of ips) {
          if (n.nodeValue && n.nodeValue.includes(ip)) {
            const el = n.parentElement;
            const celula = el.closest('td');
            let rotulo = '<sem rotulo>';
            if (celula) {
              const tabela = celula.closest('table');
              const idx = [...celula.parentElement.children].indexOf(celula);
              const th = tabela && tabela.querySelectorAll('th')[idx];
              rotulo = th ? 'coluna "' + th.innerText.trim() + '"' : '<coluna sem cabecalho>';
            } else {
              rotulo = 'texto vizinho: "' + (el.parentElement || el).innerText.trim().slice(0, 90) + '"';
            }
            achados[ip].push(rotulo);
          }
        }
      }
      return achados;
    }, infraIps);

    Object.entries(ocorrencias).forEach(([ip, achados]) => {
      console.log(`=== onde ${ip} aparece (${achados.length}):`);
      [...new Set(achados)].forEach(o => console.log('   ' + o));
    });

    const visiveis = Object.entries(ocorrencias).filter(([, achados]) => achados.length > 0);
    if (visiveis.length) {
      console.log('   >>> ENGANOSO: IP de infraestrutura apareceu na UI operacional');
    }
    expect(visiveis, `em "${nome}" apareceu IP de proxy/tunel/gerenciamento`).toEqual([]);
  }
});
