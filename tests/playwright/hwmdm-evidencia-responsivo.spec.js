const { test } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// EVIDENCIA 3: overflow horizontal medido (scrollWidth vs innerWidth), elementos
// que vazam da viewport, e se a navegacao continua alcancavel em cada largura.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio.');
const OUT = process.env.SHOTS || '/tmp';

const admin = `hwmdm_evr_${Date.now()}`;
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

const TELAS = [
  { rotulo: 'Devices', arquivo: 'devices' },
  { rotulo: 'Device profiles', arquivo: 'configurations' },
  { rotulo: 'Dashboard', arquivo: 'summary' },
  { rotulo: 'Server defaults', arquivo: 'settings' },
  { rotulo: 'Remote access', arquivo: 'remote' },
  { rotulo: 'Backup', arquivo: 'backup' }
];
const LARGURAS = [360, 768, 1280];

test('overflow horizontal e alcancabilidade em 360/768/1280', async ({ page }) => {
  test.setTimeout(900000);

  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').waitFor({ timeout: 30000 });
  await page.locator('#username').fill(admin);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.locator('.hwmdm-nav-link').first().waitFor({ timeout: 30000 });
  await page.waitForTimeout(2500);

  const resultados = [];

  for (const w of LARGURAS) {
    await page.setViewportSize({ width: w, height: 900 });
    await page.waitForTimeout(1200);

    for (const tela of TELAS) {
      // Em larguras estreitas o menu vira gaveta: abre se preciso.
      const precisouAbrirMenu = await page.evaluate(() => {
        const t = document.querySelector('.hwmdm-nav-toggle');
        return !!(t && (t.offsetWidth || t.offsetHeight));
      });
      if (precisouAbrirMenu) {
        await page.locator('.hwmdm-nav-toggle').click();
        await page.waitForTimeout(700);
      }

      let clicou = true;
      try {
        const alvo = page.locator(`.hwmdm-nav-link`).first();
        await alvo.scrollIntoViewIfNeeded({ timeout: 4000 });
        await alvo.click({ timeout: 6000 });
      } catch (e) { clicou = false; }
      await page.waitForTimeout(3500);

      const m = await page.evaluate(() => {
        const de = document.documentElement;
        const vw = window.innerWidth;
        const vazando = [];
        document.querySelectorAll('.hwmdm-main-surface *, .hwmdm-navbar *').forEach(el => {
          const r = el.getBoundingClientRect();
          if (r.width === 0 && r.height === 0) return;
          if (r.right > vw + 2) {
            vazando.push(`${el.tagName.toLowerCase()}.${(el.className || '').toString().split(' ')[0]} right=${Math.round(r.right)} w=${Math.round(r.width)}`);
          }
        });
        // Elemento com scroll proprio (tabela dentro de container rolavel) nao conta como quebra
        const scrollers = [...document.querySelectorAll('.hwmdm-main-surface *')]
          .filter(el => el.scrollWidth > el.clientWidth + 2 && ['auto', 'scroll'].includes(getComputedStyle(el).overflowX))
          .map(el => `${el.tagName.toLowerCase()}.${(el.className || '').toString().split(' ')[0]} scrollW=${el.scrollWidth} clientW=${el.clientWidth}`);
        return {
          rota: location.hash,
          innerWidth: vw,
          docScrollWidth: de.scrollWidth,
          bodyScrollWidth: document.body.scrollWidth,
          overflowPx: de.scrollWidth - vw,
          vazandoTotal: vazando.length,
          vazandoAmostra: [...new Set(vazando)].slice(0, 6),
          containersRolaveis: [...new Set(scrollers)].slice(0, 4),
          conteudoChars: (document.querySelector('.hwmdm-main-surface') || document.body).innerText.trim().length
        };
      });

      const veredito = !clicou ? 'INALCANCAVEL'
        : m.overflowPx > 2 ? `OVERFLOW ${m.overflowPx}px`
        : 'ok';
      resultados.push({ w, tela: tela.rotulo, clicou, ...m, veredito });

      console.log(`\n[${w}px] ${tela.rotulo}  =>  ${veredito}`);
      console.log(`   rota=${m.rota} innerWidth=${m.innerWidth} document.scrollWidth=${m.docScrollWidth} (diff ${m.overflowPx}px) chars=${m.conteudoChars}`);
      if (m.vazandoTotal) console.log(`   ${m.vazandoTotal} elemento(s) passando da borda: ${m.vazandoAmostra.join(' ; ')}`);
      if (m.containersRolaveis.length) console.log(`   container(s) com scroll proprio: ${m.containersRolaveis.join(' ; ')}`);

      await page.screenshot({ path: `${OUT}/shots/resp-${tela.arquivo}-${w}.png`, fullPage: false });
    }
  }

  console.log('\n\n===== TABELA RESPONSIVIDADE =====');
  console.log('| largura | tela | rota | clicavel | innerWidth | doc.scrollWidth | overflow px | elems vazando | veredito |');
  console.log('|---|---|---|---|---|---|---|---|---|');
  resultados.forEach(r => console.log(
    `| ${r.w} | ${r.tela} | ${r.rota} | ${r.clicou ? 'S' : 'N'} | ${r.innerWidth} | ${r.docScrollWidth} | ${r.overflowPx} | ${r.vazandoTotal} | ${r.veredito} |`));
});
