// Sonda minima de alcance: o dominio real responde a partir desta maquina?
const { test } = require('@playwright/test');

const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

test.setTimeout(180000);

test('alcance do dominio real', async ({ page }) => {
  const respostas = [];
  page.on('response', (r) => respostas.push(`${r.status()} ${r.url()}`));
  page.on('requestfailed', (r) =>
    respostas.push(`FALHOU ${r.url()} -> ${r.failure() && r.failure().errorText}`)
  );

  let erroNavegacao = null;
  try {
    const resp = await page.goto(base, { waitUntil: 'domcontentloaded', timeout: 120000 });
    console.log('=== NAVEGACAO ===');
    console.log('status:', resp && resp.status());
    console.log('url final:', page.url());
    const h = resp ? resp.headers() : {};
    console.log('server:', h['server'] || '(ausente)');
    console.log('cache-control:', h['cache-control'] || '(ausente)');
    console.log('etag:', h['etag'] || '(ausente)');
    console.log('last-modified:', h['last-modified'] || '(ausente)');
    console.log('titulo:', await page.title());
  } catch (e) {
    erroNavegacao = e.message;
    console.log('=== NAVEGACAO FALHOU ===');
    console.log(erroNavegacao);
  }

  console.log('\n=== TRAFEGO OBSERVADO (primeiras 25) ===');
  console.log(respostas.slice(0, 25).join('\n') || '(nenhum)');
});
