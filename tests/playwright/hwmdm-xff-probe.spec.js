const { test, expect, request } = require('@playwright/test');

// Sonda de cadeia de proxy. Nao toca em nada: usa o endpoint publico de sync que ja
// devolve no header X-IP-Address exatamente o que BaseIPFilter.getRemoteAddr() calculou.
// Objetivo: descobrir se o proxy da frente PRESERVA ou SOBRESCREVE o X-Forwarded-For
// que o cliente manda. Se ele sobrescreve, nenhum hop anterior consegue entregar o IP
// do tablet, por melhor que seja o desembrulho do lado do Tomcat.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio.');
const number = process.env.HWMDM_DEVICE || 'R9XT106VP1E';

const casos = [
  ['sem header nenhum', {}],
  ['X-Forwarded-For unico', { 'X-Forwarded-For': '203.0.113.77' }],
  ['X-Forwarded-For encadeado', { 'X-Forwarded-For': '203.0.113.77, 198.51.100.9' }],
  ['X-Real-IP', { 'X-Real-IP': '203.0.113.55' }],
  ['Forwarded RFC7239', { 'Forwarded': 'for=203.0.113.44' }],
  ['XFF + X-Real-IP juntos', { 'X-Forwarded-For': '203.0.113.77', 'X-Real-IP': '203.0.113.55' }],
  // Discriminador: o codigo atual PULA a entrada "unknown" e cai no request.getRemoteAddr().
  //   resposta 10.0.17.106 (o proprio openresty) => openresty REPASSOU o header intacto
  //   resposta 10.0.17.198 (esta maquina)        => openresty ACRESCENTOU o proprio $remote_addr
  ['XFF = unknown (discriminador)', { 'X-Forwarded-For': 'unknown' }],
];

test('o que o proxy da frente faz com o X-Forwarded-For do cliente', async () => {
  test.setTimeout(120000);
  const linhas = [];
  for (const [nome, headers] of casos) {
    const ctx = await request.newContext({ ignoreHTTPSErrors: true, extraHTTPHeaders: headers });
    const r = await ctx.get(`${base}/rest/public/sync/configuration/${number}`);
    const visto = r.headers()['x-ip-address'];
    linhas.push(`${String(r.status()).padEnd(4)} | ${nome.padEnd(28)} | enviado=${JSON.stringify(headers)} -> X-IP-Address=[${visto}]`);
    await ctx.dispose();
  }
  console.log('\n=== O QUE O SERVIDOR RESOLVEU COMO "IP DO CLIENTE" ===');
  linhas.forEach(l => console.log('  ' + l));
  expect(linhas.length).toBe(casos.length);
});
