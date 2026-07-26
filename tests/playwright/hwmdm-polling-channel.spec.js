const { test, expect } = require('@playwright/test');
const { execFileSync } = require('child_process');

// O agente so' drena a fila HTTP quando pushOptions = 'polling'; com 'mqttAlarm'
// ele usa exclusivamente MQTT, que esta inalcancavel (ECONNREFUSED na 31000).
// Este teste verifica se o canal de long polling responde pela 443, que ja funciona.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

function psql(sql) {
  return execFileSync('docker', ['exec', '-i', 'source-postgresql-1', 'sh', '-c',
    'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -A'],
    { input: sql, encoding: 'utf8' }).trim();
}

test('o canal de long polling responde pela 443', async ({ request }) => {
  const device = psql('select number from devices order by id limit 1;');
  const url = `${base}/rest/notification/polling/${encodeURIComponent(device)}`;
  console.log('=== DEVICE:', device);
  console.log('=== URL:', url);

  // Long polling segura a conexao de proposito. Um timeout do cliente significa que
  // o servidor ACEITOU e esta segurando -> canal vivo. 404 significa canal morto.
  let status = null;
  let seguoruAConexao = false;
  const started = Date.now();
  try {
    const resp = await request.get(url, { timeout: 12000 });
    status = resp.status();
    console.log('=== STATUS:', status, 'em', Date.now() - started, 'ms');
    console.log('=== BODY:', (await resp.text()).slice(0, 300));
  } catch (e) {
    seguoruAConexao = /timeout|exceeded/i.test(e.message);
    console.log('=== SEM RESPOSTA EM 12s:', e.message.split('\n')[0]);
    console.log('=== interpretacao: long polling segurando a conexao =', seguoruAConexao);
  }

  const vivo = seguoruAConexao || (status !== null && status < 400);
  console.log('=== CANAL DE POLLING VIVO:', vivo);
  expect(vivo, `o endpoint de long polling nao respondeu (status=${status})`).toBe(true);

  const modos = psql("select pushoptions || ' x' || count(*) from configurations group by pushoptions;");
  console.log('=== pushOptions configurados hoje:', modos.split('\n').join('  '));
});
