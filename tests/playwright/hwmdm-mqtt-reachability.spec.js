const { test, expect } = require('@playwright/test');
const net = require('net');
const { execFileSync } = require('child_process');

// O agente Android usa MQTT (pushOptions = mqttAlarm) para receber comandos na hora.
// Se o broker nao for alcancavel pelo caminho que o TABLET usa, o unico canal que
// sobra e' o polling lento -> comando remoto demora e device "parece" offline.
// Este teste mede isso pelo dominio publico, sem curl e sem localhost.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

const host = new URL(base).hostname;
const MQTT_PORT = parseInt(process.env.HWMDM_MQTT_PORT || '31000', 10);
const CONNECT_TIMEOUT_MS = 8000;

function tcpProbe(targetHost, port) {
  return new Promise(resolve => {
    const socket = new net.Socket();
    const started = Date.now();
    let done = false;
    const finish = result => {
      if (done) return;
      done = true;
      socket.destroy();
      resolve({ ...result, ms: Date.now() - started });
    };
    socket.setTimeout(CONNECT_TIMEOUT_MS);
    socket.once('connect', () => finish({ reachable: true, reason: 'conectou' }));
    socket.once('timeout', () => finish({ reachable: false, reason: 'timeout' }));
    socket.once('error', err => finish({ reachable: false, reason: err.code || err.message }));
    socket.connect(port, targetHost);
  });
}

test('o broker MQTT e alcancavel pelo caminho que o tablet usa', async () => {
  console.log(`=== HOST PUBLICO: ${host}`);

  const https = await tcpProbe(host, 443);
  console.log(`=== 443/tcp (painel + sync): reachable=${https.reachable} (${https.reason}, ${https.ms}ms)`);
  expect(https.reachable, 'nem o 443 responde; a cadeia publica esta fora').toBe(true);

  const mqtt = await tcpProbe(host, MQTT_PORT);
  console.log(`=== ${MQTT_PORT}/tcp (MQTT): reachable=${mqtt.reachable} (${mqtt.reason}, ${mqtt.ms}ms)`);

  // Quantos tablets estao de fato conectados no broker agora?
  const established = execFileSync('docker', ['exec', 'source-hmdm-1', 'sh', '-c',
    `cat /proc/net/tcp /proc/net/tcp6 2>/dev/null | awk '$2 ~ /7918$/ && $4 == "01"'`],
    { encoding: 'utf8' }).trim();
  const linhas = established ? established.split('\n') : [];
  console.log(`=== CONEXOES ESTABELECIDAS NO BROKER: ${linhas.length}`);
  linhas.forEach(l => console.log('   ' + l.trim().split(/\s+/).slice(1, 4).join(' ')));

  // O proprio servidor mantem 1 conexao cliente com o seu broker. Acima disso sao devices.
  const conexoesDeDevices = Math.max(0, linhas.length - 1);
  console.log(`=== CONEXOES QUE SAO DE DEVICES: ${conexoesDeDevices}`);

  // Cadencia real de check-in observada, que e' a consequencia direta.
  const idades = execFileSync('docker', ['exec', '-i', 'source-postgresql-1', 'sh', '-c',
    'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -A'],
    { input: `select number || '=' || round((extract(epoch from now())*1000 - lastupdate)/1000) || 's'
              from devices order by id;`, encoding: 'utf8' }).trim();
  console.log('=== IDADE DO ULTIMO CHECK-IN POR DEVICE:', idades.split('\n').join('  '));

  if (!mqtt.reachable) {
    console.log('=== DIAGNOSTICO: o broker NAO e alcancavel pelo dominio publico.');
    console.log('    Consequencia: o tablet nunca abre sessao MQTT, entao comando remoto');
    console.log('    so chega no ciclo lento de polling do agente (~900s), e qualquer regra');
    console.log('    de "online" mais apertada que isso mostra tudo OFFLINE.');
  }

  // Este teste documenta a realidade da infra. Ele falha de proposito enquanto o
  // canal instantaneo nao existir, porque isso e' um defeito real de entrega.
  expect(mqtt.reachable,
    `MQTT ${MQTT_PORT} inalcancavel em ${host}: o tablet nao tem canal instantaneo`).toBe(true);
});
