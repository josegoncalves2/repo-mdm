// Prova de assets quebrados: scripts referenciados pelo index.html que nao existem no servidor.
const { test, expect } = require('@playwright/test');

const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

test('assets 404 no carregamento do painel', async ({ page }) => {
  const falhas = [];
  const erros = [];
  page.on('response', (r) => {
    if (r.status() >= 400) falhas.push(`${r.status()} ${r.url()}`);
  });
  page.on('pageerror', (e) => erros.push(e.message));

  await page.goto(base, { waitUntil: 'networkidle', timeout: 60000 });

  console.log('\n=== REQUISICOES COM ERRO ===');
  console.log(falhas.length ? falhas.join('\n') : '(nenhuma)');

  console.log('\n=== ERROS DE JAVASCRIPT ===');
  console.log(erros.length ? erros.join('\n') : '(nenhum)');

  // JSEncrypt e usado para cifrar a senha no login; sem ele o objeto global some.
  const presentes = await page.evaluate(() => ({
    JSEncrypt: typeof window.JSEncrypt,
    introJs: typeof window.introJs,
    angular: typeof window.angular,
  }));
  console.log('\n=== GLOBAIS ESPERADOS ===');
  console.log(JSON.stringify(presentes, null, 2));

  expect(presentes.angular, 'angular nao carregou').not.toBe('undefined');
});
