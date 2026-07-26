const { test, expect, request } = require('@playwright/test');

// Compara os dois caminhos de rede que chegam ao mesmo nginx:
//  (a) direto para o ultimo salto (o /etc/hosts desta maquina aponta o dominio para ele)
//  (b) passando pela borda publica, que e' por onde os tablets entram
// O servidor devolve em X-IP-Address o que ele considera "IP do cliente".
// Se (b) devolver um endereco fixo e privado, o salto da borda esta fazendo SNAT
// e o IP de origem do tablet morre ali, antes de qualquer header existir.
const base = process.env.HWMDM_BASE_URL;
const publica = process.env.HWMDM_PUBLIC_URL;
if (!base || !publica) throw new Error('HWMDM_BASE_URL e HWMDM_PUBLIC_URL obrigatorios.');
const number = process.env.HWMDM_DEVICE || 'R9XT106VP1E';

test('caminho interno x caminho da borda publica', async () => {
  test.setTimeout(120000);
  for (const [nome, url, extra] of [
    ['ultimo salto (LAN)', base, {}],
    ['borda publica', publica, { Host: 'mdm.puzzlepunker.com.br' }],
  ]) {
    const ctx = await request.newContext({ ignoreHTTPSErrors: true, extraHTTPHeaders: extra });
    try {
      const r = await ctx.get(`${url}/rest/public/sync/configuration/${number}`, { timeout: 20000 });
      console.log(`  ${nome}: ${url} -> HTTP ${r.status()} X-IP-Address=[${r.headers()['x-ip-address']}]`);
    } catch (e) {
      console.log(`  ${nome}: ${url} -> FALHOU: ${e.message.split('\n')[0]}`);
    }
    await ctx.dispose();
  }
  expect(true).toBe(true);
});
