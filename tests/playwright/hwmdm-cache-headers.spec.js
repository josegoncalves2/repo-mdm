// Prova de cache dos templates AngularJS.
// Objetivo: mostrar quais cabecalhos o servidor manda para os templates .html do painel,
// e se o navegador pode reservi-los do cache sem revalidar (raiz do "layout antigo que volta").
const { test, expect } = require('@playwright/test');

const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

const ALVOS = [
  'index.html',
  'app/components/main/view/main.html',
  'app/components/main/view/devices.html',
  'app/components/main/view/summary.html',
  'app/components/main/view/remote.html',
  'app/components/main/view/governance.html',
  'app/components/main/view/content.html',
  'app/components/main/view/configuration.html',
];

test('cabecalhos de cache dos templates', async ({ request }) => {
  const linhas = [];
  for (const alvo of ALVOS) {
    const url = `${base}/${alvo}`;
    const r = await request.get(url);
    const h = r.headers();
    linhas.push({
      alvo,
      status: r.status(),
      cacheControl: h['cache-control'] || '(ausente)',
      expires: h['expires'] || '(ausente)',
      etag: h['etag'] || '(ausente)',
      lastModified: h['last-modified'] || '(ausente)',
      via: h['via'] || h['x-cache'] || h['server'] || '(n/a)',
    });
  }

  console.log('\n=== CABECALHOS DE CACHE ===');
  for (const l of linhas) {
    console.log(
      `${l.alvo}\n  status=${l.status}\n  cache-control=${l.cacheControl}\n  expires=${l.expires}\n  etag=${l.etag}\n  last-modified=${l.lastModified}\n  server/via=${l.via}\n`
    );
  }

  // Sem Cache-Control explicito o navegador aplica frescor heuristico
  // (~10% da idade do arquivo), que e exatamente como um template velho sobrevive a um F5.
  const semControle = linhas.filter(
    (l) => l.status === 200 && l.cacheControl === '(ausente)'
  );
  console.log(
    `\nTemplates servidos SEM Cache-Control: ${semControle.length}/${linhas.filter((l) => l.status === 200).length}`
  );
  console.log(semControle.map((l) => '  - ' + l.alvo).join('\n'));

  // Todos os alvos precisam ao menos existir; um 404 aqui ja explicaria tela quebrada.
  const faltando = linhas.filter((l) => l.status !== 200);
  expect(faltando, `templates inacessiveis: ${JSON.stringify(faltando)}`).toEqual([]);
});
