const { test, expect } = require('@playwright/test');
const { execFileSync } = require('child_process');

// ESTE e' o unico teste da suite que julga pelo que o TABLET reporta, e nao pelo
// que o servidor manda. Todos os outros observam o lado servidor da fronteira e,
// por isso, passavam verde enquanto nenhum tablet obedecia a nada.
//
// A fonte de verdade e' devices.info -- o JSON que o proprio agente envia no sync.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

// Um device so' conta como testemunha se o relato dele e' recente.
const RELATO_FRESCO_S = parseInt(process.env.HWMDM_FRESH_S || '1800', 10);

function psql(sql) {
  return execFileSync('docker', ['exec', '-i', 'source-postgresql-1', 'sh', '-c',
    'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -A -F"|"'],
    { input: sql, encoding: 'utf8' }).trim();
}

function frota() {
  const linhas = psql(`
    SELECT d.number,
           round((extract(epoch from now())*1000 - d.lastupdate)/1000),
           coalesce(d.info::jsonb->>'mdmMode','?'),
           coalesce(d.info::jsonb->>'kioskMode','?'),
           coalesce(d.info::jsonb->>'defaultLauncher','?'),
           coalesce((SELECT a->>'version' FROM jsonb_array_elements(d.info::jsonb->'applications') a
                     WHERE a->>'pkg'='com.hmdm.launcher' LIMIT 1),'?'),
           c.name, c.kioskmode
      FROM devices d JOIN configurations c ON c.id = d.configurationid
     ORDER BY d.id;`);
  if (!linhas) return [];
  return linhas.split('\n').map(l => {
    const [number, idade, mdm, kiosk, defLauncher, launcher, perfil, perfilKiosk] = l.split('|');
    return {
      number,
      idade: parseInt(idade, 10),
      mdm: mdm === 'true',
      kiosk: kiosk === 'true',
      defaultLauncher: defLauncher === 'true',
      launcher,
      perfil,
      perfilExigeKiosk: perfilKiosk === 't'
    };
  });
}

test('o que os tablets REPORTAM bate com o que os perfis exigem', () => {
  const devices = frota();
  console.log('=== FROTA (' + devices.length + '):');
  devices.forEach(d => console.log(
    `   ${d.number.padEnd(13)} idade=${String(d.idade).padStart(6)}s  perfil="${d.perfil}"` +
    `  perfilExigeKiosk=${d.perfilExigeKiosk}  ->  REPORTA mdm=${d.mdm} kiosk=${d.kiosk}` +
    ` defaultLauncher=${d.defaultLauncher} launcher=${d.launcher}`));
  expect(devices.length, 'nenhum device cadastrado').toBeGreaterThan(0);

  const testemunhas = devices.filter(d => d.idade <= RELATO_FRESCO_S);
  console.log(`=== com relato dos ultimos ${RELATO_FRESCO_S}s: ${testemunhas.length}/${devices.length}`);
  expect(testemunhas.length, `nenhum device reportou nos ultimos ${RELATO_FRESCO_S}s`).toBeGreaterThan(0);

  // 1) Quem esta num perfil de kiosk tem de estar EM kiosk. Nao basta o servidor mandar.
  const deveriamTravar = testemunhas.filter(d => d.perfilExigeKiosk);
  const travados = deveriamTravar.filter(d => d.kiosk);
  console.log(`=== KIOSK: ${travados.length}/${deveriamTravar.length} tablets realmente travados`);
  deveriamTravar.filter(d => !d.kiosk).forEach(d =>
    console.log(`   NAO TRAVOU: ${d.number} (perfil "${d.perfil}" exige kiosk, agente ${d.launcher})`));

  // 2) O agente instalado tem de ser o que o perfil manda instalar.
  const versaoEsperada = psql(`select av.version from configurations c
                               join applicationversions av on av.id = c.mainappid
                               where c.id = (select configurationid from devices order by id limit 1);`);
  console.log('=== versao de agente que o perfil manda:', versaoEsperada);
  testemunhas.forEach(d => {
    if (d.launcher !== versaoEsperada) {
      console.log(`   DIVERGENTE: ${d.number} roda ${d.launcher}, perfil manda ${versaoEsperada}`);
    }
  });

  // 3) Enrollment de fato completo.
  testemunhas.filter(d => !d.mdm).forEach(d =>
    console.log(`   SEM MDM MODE: ${d.number} nunca completou o enrollment`));

  expect(travados.length,
    `${deveriamTravar.length - travados.length} tablet(s) em perfil de kiosk NAO estao travados. ` +
    `O servidor manda kioskMode=true e o dispositivo responde kioskMode=false: ` +
    `o APK instalado nao implementa lock task (variante open-source e' stub).`).toBe(deveriamTravar.length);
});
