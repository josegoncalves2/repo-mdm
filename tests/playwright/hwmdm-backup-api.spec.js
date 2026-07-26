const { test, expect } = require('@playwright/test');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

// Exercita a API de backup pela cadeia publica real: criar, listar, baixar,
// negar quem nao tem permissao e recusar path traversal. Sem curl, sem localhost.
const base = process.env.HWMDM_BASE_URL;
if (!base) throw new Error('HWMDM_BASE_URL obrigatorio. Sem fallback para localhost.');

const admin = `hwmdm_bkp_admin_${Date.now()}`;
const limited = `hwmdm_bkp_guest_${Date.now()}`;
const password = crypto.randomBytes(18).toString('base64url');
const passwordSalt = '5YdSYHyg2U';

function hash(raw) {
  const md5 = crypto.createHash('md5').update(raw).digest('hex').toUpperCase();
  return crypto.createHash('sha1').update(md5 + passwordSalt).digest('hex');
}

function psql(sql) {
  return execFileSync('docker', ['exec', '-i', 'source-postgresql-1', 'sh', '-c',
    'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -t -A'],
    { input: sql, encoding: 'utf8' }).trim();
}

function makeUser(login, roleId) {
  psql(`insert into users(login, email, name, password, customerid, userroleid, alldevicesavailable, allconfigavailable, passwordreset, authtoken)
        select '${login}', '${login}@local.test', '${login}', '${hash(password)}', customerid, ${roleId}, true, true, false, null
        from users where login = 'admin';`);
}

test.beforeAll(() => { makeUser(admin, 2); makeUser(limited, 104); });
test.afterAll(() => { psql(`delete from users where login in ('${admin}','${limited}');`); });

async function signIn(page, login) {
  await page.goto(`${base}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('#username').fill(login);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForTimeout(3500);
}

const api = (page, path, method) => page.evaluate(async ([p, m]) => {
  const r = await fetch(p, { method: m || 'GET' });
  return { status: r.status, body: (await r.text()).slice(0, 800) };
}, [path, method]);

test('cria, lista e baixa um backup real do banco', async ({ page }) => {
  test.setTimeout(300000);
  await signIn(page, admin);

  const antes = await api(page, 'rest/private/backup/list');
  console.log('=== GET /backup/list ->', antes.status, antes.body.slice(0, 200));
  expect(antes.status, 'endpoint de backup nao existe').toBe(200);
  const listaAntes = JSON.parse(antes.body).data || [];
  console.log('=== backups existentes:', listaAntes.length);

  const criado = await api(page, 'rest/private/backup/create', 'POST');
  console.log('=== POST /backup/create ->', criado.status, criado.body.slice(0, 300));
  const criadoJson = JSON.parse(criado.body);
  expect(criadoJson.status, `criacao falhou: ${criado.body}`).toBe('OK');

  const nome = criadoJson.data.name;
  const tamanho = criadoJson.data.size;
  console.log(`=== BACKUP CRIADO: ${nome} (${tamanho} bytes)`);
  expect(nome).toMatch(/^hmdm-backup-\d{8}-\d{6}\.sql\.gz$/);
  expect(tamanho, 'backup veio vazio').toBeGreaterThan(1000);

  // Existe mesmo no disco do servidor?
  const noDisco = execFileSync('docker',
    ['exec', 'source-hmdm-1', 'sh', '-c', `ls -la /usr/local/tomcat/work/backups/${nome}`],
    { encoding: 'utf8' }).trim();
  console.log('=== NO DISCO:', noDisco);

  // O conteudo e' um dump utilizavel? Descompacta e procura SQL de verdade.
  const conteudo = execFileSync('docker',
    ['exec', 'source-hmdm-1', 'sh', '-c',
     `gunzip -c /usr/local/tomcat/work/backups/${nome} | grep -cE 'CREATE TABLE public.(devices|configurations)'`],
    { encoding: 'utf8' }).trim();
  console.log('=== TABELAS-CHAVE ENCONTRADAS NO DUMP:', conteudo);
  expect(parseInt(conteudo, 10), 'o dump nao contem as tabelas principais').toBeGreaterThanOrEqual(2);

  const linhasDeDados = execFileSync('docker',
    ['exec', 'source-hmdm-1', 'sh', '-c',
     `gunzip -c /usr/local/tomcat/work/backups/${nome} | grep -c 'COPY public.devices'`],
    { encoding: 'utf8' }).trim();
  console.log('=== BLOCO DE DADOS DE DEVICES NO DUMP:', linhasDeDados);
  expect(parseInt(linhasDeDados, 10)).toBeGreaterThan(0);

  const depois = await api(page, 'rest/private/backup/list');
  const listaDepois = JSON.parse(depois.body).data || [];
  console.log('=== backups apos criar:', listaDepois.length);
  expect(listaDepois.length).toBeGreaterThan(listaAntes.length);
  expect(listaDepois[0].name, 'o mais novo deveria vir primeiro').toBe(nome);

  // Download pela cadeia publica
  const dl = await page.evaluate(async n => {
    const r = await fetch(`rest/private/backup/${encodeURIComponent(n)}/download`);
    const buf = await r.arrayBuffer();
    return { status: r.status, bytes: buf.byteLength, disp: r.headers.get('content-disposition') };
  }, nome);
  console.log(`=== DOWNLOAD -> ${dl.status}, ${dl.bytes} bytes, ${dl.disp}`);
  expect(dl.status).toBe(200);
  expect(dl.bytes, 'download nao bate com o tamanho do arquivo').toBe(tamanho);
  expect(dl.disp).toContain(nome);

  // Limpeza: apaga o backup criado por este teste
  const del = await api(page, `rest/private/backup/${encodeURIComponent(nome)}`, 'DELETE');
  console.log('=== DELETE ->', del.status, del.body.slice(0, 120));
  expect(JSON.parse(del.body).status).toBe('OK');
});

test('recusa path traversal e nome invalido', async ({ page }) => {
  await signIn(page, admin);

  for (const alvo of ['../../conf/Catalina/localhost/ROOT.xml', '..%2F..%2FROOT.xml', 'qualquer-coisa.sql.gz']) {
    const r = await api(page, `rest/private/backup/${encodeURIComponent(alvo)}/download`);
    console.log(`=== DOWNLOAD "${alvo}" -> ${r.status}`);
    expect(r.status, `traversal aceito para ${alvo}`).not.toBe(200);
  }
});

test('quem nao tem permissao de settings nao mexe em backup', async ({ page }) => {
  await signIn(page, limited);

  const lista = await api(page, 'rest/private/backup/list');
  console.log('=== GET /backup/list como Guest ->', lista.status, lista.body.slice(0, 150));
  expect(JSON.parse(lista.body).message).toBe('error.permission.denied');

  const cria = await api(page, 'rest/private/backup/create', 'POST');
  console.log('=== POST /backup/create como Guest ->', cria.status, cria.body.slice(0, 150));
  expect(JSON.parse(cria.body).message).toBe('error.permission.denied');
});
