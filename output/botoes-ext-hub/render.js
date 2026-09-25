// Renderiza as telas Modulos e Integracoes com o HTML e o CSS reais do webapp, sem servidor e
// sem login: os templates sao lidos do disco e compilados por um AngularJS local com
// controllers de mentira (dados fixos). Mede se "Configurar"/"Copiar" ficam numa linha so' e se
// o botao sobrepoe o interruptor. Uso: node render.js <variante> <css-modules> <dir-capturas>
const fs = require('fs');
const path = require('path');
const {chromium} = require('/opt/projetos/hwmdm/arquivados/limpeza-nao-essenciais-20260924-143028/nivel-repo-mdm/ferramentas/node_modules/playwright');

const [variante, cssModules, outDir] = process.argv.slice(2);
const W = '/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/webapp';
const HERE = __dirname;
// Tudo servido por page.route a partir do disco (file:// bloqueia mask-image dos icones por CORS).
const f = p => 'http://teste.local/fs' + p;

// Mesma ordem de folhas de estilo do index.html (linhas 10-13 e 121-127).
const CSS = [
  W + '/lib/ng-tags-input/ng-tags-input.css',
  W + '/lib/angular-bootstrap-colorpicker/colorpicker.css',
  W + '/lib/angular-input-dropdown/inputDropdownStyles.css',
  W + '/lib/intro.js/introjs.css',
  W + '/lib/bootstrap-css-only/css/bootstrap.css',
  W + '/css/main.css',
  W + '/css/mdm-picker.css',
  cssModules,
  W + '/css/hwmdm-version.css',
  W + '/css/hwmdm-ui.css',
];

// Catalogo real dos modulos, extraido do moduleRegistry.service.js.
const reg = fs.readFileSync(W + '/app/shared/service/moduleRegistry.service.js', 'utf8');
let cat = reg.slice(reg.indexOf('var CATALOG = ['));
cat = cat.slice(0, cat.indexOf('\n        ];'));
const re = /id: '([^']+)', section: '([^']+)', icon: '([^']+)'[\s\S]*?labelKey: '([^']+)', descKey: '([^']+)'[\s\S]*?type: '([^']+)', manageVia: ([^,]+), essential: (\w+)([\s\S]*?)visible/g;
const modules = [];
let m;
while ((m = re.exec(cat))) {
  const bp = /backingPlugin: '([^']+)'/.exec(m[9]);
  modules.push({id: m[1], section: m[2], icon: m[3], labelKey: m[4], descKey: m[5], type: m[6],
    manageVia: m[7] === 'null' ? null : m[7].replace(/'/g, ''), essential: m[8] === 'true',
    backingPlugin: bp ? bp[1] : null});
}
const secRe = /\{id: '(\w+)', labelKey: '([^']+)'\}/g;
const sections = [];
while ((m = secRe.exec(reg))) sections.push({id: m[1], labelKey: m[2]});

// Pontos de integracao reais, extraidos do plugins.controller.js.
const ctl = fs.readFileSync(W + '/app/components/main/controller/plugins.controller.js', 'utf8');
let pts = ctl.slice(ctl.indexOf('$scope.integrationPoints = [') + '$scope.integrationPoints = '.length);
pts = pts.slice(0, pts.indexOf('];') + 1);

const tpl = {
  modulos: fs.readFileSync(W + '/app/components/main/view/settings/extensionsHub.html', 'utf8'),
  integracoes: fs.readFileSync(W + '/app/components/main/view/settings/integrations.html', 'utf8'),
};

const mock = `
angular.module('teste', [])
  .factory('localization', function () {
    var d = document.localization['pt_PT'];
    // Nomes que no app vem dos pacotes de idioma dos plugins, carregados do servidor.
    var extra = {
      'plugin.devicelog.localization.key.name': 'Logs de dispositivos',
      'plugin.audit.localization.key.name': 'Auditoria',
      'plugin.push.localization.key.name': 'Mensagens push',
      'plugin.deviceinfo.localization.key.name': 'Informações detalhadas'
    };
    return {localize: function (k) { k = (k || '').trim(); return d[k] || extra[k] || k; }};
  })
  .directive('localized', function (localization) {   // igual ao de locale.service.js
    return {restrict: 'A', link: function ($scope, element) { element.html(localization.localize(element.html())); }};
  })
  .filter('localize', function (localization) { return function (k) { return localization.localize(k); }; })
  .controller('ModulesTabController', function ($scope) {
    var MODULES = ${JSON.stringify(modules)};
    var SECTIONS = ${JSON.stringify(sections)};
    $scope.loading = false;
    $scope.nativeStateAvailable = true;
    $scope.sections = SECTIONS.map(function (s) {
      return {labelKey: s.labelKey, modules: MODULES.filter(function (x) { return x.section === s.id; })};
    }).filter(function (s) { return s.modules.length; });
    var byId = {}; MODULES.forEach(function (x) { byId[x.id] = x; });
    $scope.typeLabel = function (x) { return x.type === 'extension' ? 'modules.type.extension' : 'modules.type.native'; };
    $scope.isInMaintenance = function (id) { return id === 'plugin-push'; };
    var MANUT = {disabledAt: new Date(2026, 8, 25, 15, 30), disabledBy: 'admin', reason: 'Teste de layout'};
    $scope.maintenanceInfo = function () { return MANUT; };
    $scope.isEssential = function (id) { return byId[id].essential; };
    $scope.canToggle = function (id) { return !!byId[id].manageVia && !byId[id].essential; };
    $scope.settingsTarget = function (id) { return byId[id].backingPlugin ? 'plugin-settings-' + byId[id].backingPlugin : null; };
    $scope.toggle = function () {};
    $scope.openTab = function () {};
  })
  .controller('IntegrationsTabController', function ($scope) {
    $scope.loading = false;
    $scope.installedPlugins = ['audit', 'devicelog', 'deviceinfo', 'messaging', 'push', 'webfilter', 'moduleregistry']
      .map(function (id) { return {identifier: id, nameLocalizationKey: 'plugin.' + id + '.localization.key.name'}; });
    $scope.isPluginActive = function (id) { return id !== 'push'; };
    $scope.integrationPoints = ${pts};
    $scope.copiedId = null;
    $scope.copy = function (p) { $scope.copiedId = p.id; };
  });
`;

function pagina(tela) {
  return `<!DOCTYPE html><html><head><meta charset="UTF-8"><title>teste ${tela}</title>
${CSS.map(c => `<link rel="stylesheet" type="text/css" href="${f(c)}">`).join('\n')}
<script src="${f(W + '/lib/angular/angular.js')}"></script>
<script src="${f(W + '/localization/pt_PT.js')}"></script>
<script src="${f(W + '/localization/hwmdm_modules.js')}"></script>
<script>${mock}</script>
</head><body ng-app="teste">
<div class="content-wrapper fullscreen"><div class="hwmdm-workspace">
<aside class="hwmdm-sidebar" id="hwmdm-sidebar"></aside>
<main class="hwmdm-main"><div class="hwmdm-main-surface">
${tpl[tela]}
</div></main></div></div></body></html>`;
}

// Mede cada botao da tabela: linhas de texto, altura, se cabe na celula e se encosta no interruptor.
function medir() {
  function linhas(el) {
    var tops = {};
    var w = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
    var n;
    while ((n = w.nextNode())) {
      if (!n.textContent.trim()) continue;
      var r = document.createRange();
      r.selectNodeContents(n);
      Array.prototype.forEach.call(r.getClientRects(), function (q) { if (q.width > 0) tops[Math.round(q.top)] = 1; });
    }
    return Object.keys(tops).length;
  }
  function cruza(a, b) { return a.left < b.right - 0.5 && b.left < a.right - 0.5 && a.top < b.bottom - 0.5 && b.top < a.bottom - 0.5; }
  var out = [];
  document.querySelectorAll('.ext-hub .table td > .btn').forEach(function (b) {
    var td = b.parentElement, rb = b.getBoundingClientRect(), rt = td.getBoundingClientRect();
    var tg = td.querySelector('.ext-hub-row-toggle');
    var rg = tg ? tg.getBoundingClientRect() : null;
    var row = td.parentElement.querySelector('.ext-hub-row-name');
    var tr = null;
    var w2 = document.createTreeWalker(b, NodeFilter.SHOW_TEXT), n2;
    while ((n2 = w2.nextNode())) {
      if (!n2.textContent.trim()) continue;
      var r2 = document.createRange(); r2.selectNodeContents(n2);
      Array.prototype.forEach.call(r2.getClientRects(), function (q) {
        if (q.width <= 0) return;
        tr = tr ? {left: Math.min(tr.left, q.left), top: Math.min(tr.top, q.top), right: Math.max(tr.right, q.right), bottom: Math.max(tr.bottom, q.bottom)} : {left: q.left, top: q.top, right: q.right, bottom: q.bottom};
      });
    }
    out.push({
      textoDentroDoBotao: !!tr && tr.left >= rb.left - 0.5 && tr.right <= rb.right + 0.5 && tr.top >= rb.top - 0.5 && tr.bottom <= rb.bottom + 0.5,
      textoSobreInterruptor: !!(tr && rg && cruza(tr, rg)),
      linha: row ? row.textContent.trim() : '?',
      texto: b.textContent.replace(/\s+/g, ' ').trim(),
      linhasTexto: linhas(b),
      alturaBotao: Math.round(rb.height),
      larguraBotao: Math.round(rb.width),
      larguraCelula: Math.round(rt.width),
      cabeNaCelula: rb.left >= rt.left - 0.5 && rb.right <= rt.right + 0.5,
      interruptor: rg ? {sobrepoe: cruza(rb, rg), mesmaLinha: Math.abs((rb.top + rb.bottom) / 2 - (rg.top + rg.bottom) / 2) < 8,
                         aDireita: rg.left >= rb.right - 0.5} : null,
      whiteSpace: getComputedStyle(b).whiteSpace,
    });
  });
  var tr = [].map.call(document.querySelectorAll('.ext-hub .table-responsive'), function (d) { return d.scrollWidth - d.clientWidth; });
  return {botoes: out, rolagemHorizontalTabelas: tr,
          rolagemHorizontalPagina: document.documentElement.scrollWidth - document.documentElement.clientWidth};
}

(async () => {
  fs.mkdirSync(outDir, {recursive: true});
  const browser = await chromium.launch();
  const ctx = await browser.newContext();
  await ctx.route('http://teste.local/**', route => {
    const u = new URL(route.request().url());
    route.fulfill({path: decodeURIComponent(u.pathname.replace(/^\/fs/, ''))});
  });
  const resultado = {variante, cssModules, chromium: browser.version(), telas: {}};
  for (const tela of ['modulos', 'integracoes']) {
    const arq = path.join(HERE, `pagina-${tela}-${variante}.html`);
    fs.writeFileSync(arq, pagina(tela));
    for (const largura of [1366, 1920]) {
      const page = await ctx.newPage(); await page.setViewportSize({width: largura, height: 900});
      const erros = [];
      page.on('pageerror', e => erros.push(String(e)));
      page.on('requestfailed', r => erros.push('falhou: ' + r.url()));
      await page.goto(f(arq));
      await page.waitForSelector('.ext-hub .table td > .btn');
      await page.waitForTimeout(300);
      const chave = `${tela}-${largura}`;
      resultado.telas[chave] = await page.evaluate(medir);
      resultado.telas[chave].erros = erros;
      await page.screenshot({path: path.join(outDir, `${tela}-${largura}-${variante}.png`), fullPage: true, timeout: 90000});
      // Recorte so' da primeira tabela que tem botao, para ver o detalhe.
      await page.locator('.ext-hub .table-responsive:has(td > .btn)').first()
        .screenshot({path: path.join(outDir, `${tela}-${largura}-${variante}-tabela.png`), timeout: 90000});
      if (tela === 'integracoes') {
        // Estado "Copiado" (depois do clique) tambem precisa caber numa linha.
        await page.click('.ext-hub .table td > .btn');
        await page.waitForTimeout(100);
        resultado.telas[chave + '-copiado'] = await page.evaluate(medir);
      }
      await page.close();
    }
  }
  await browser.close();
  fs.writeFileSync(path.join(outDir, `medidas-${variante}.json`), JSON.stringify(resultado, null, 2));
  for (const [k, v] of Object.entries(resultado.telas)) {
    const b = v.botoes;
    const multi = b.filter(x => x.linhasTexto !== 1);
    const sob = b.filter(x => x.interruptor && x.interruptor.sobrepoe);
    const fora = b.filter(x => !x.cabeNaCelula);
    const vaza = b.filter(x => !x.textoDentroDoBotao);
    const tsob = b.filter(x => x.textoSobreInterruptor);
    console.log(`${variante} ${k}: botoes=${b.length} textos=[${[...new Set(b.map(x => x.texto))].join(', ')}] ` +
      `linhas=[${[...new Set(b.map(x => x.linhasTexto))].join(',')}] alturas=[${[...new Set(b.map(x => x.alturaBotao))].join(',')}] ` +
      `larg.botao=[${[...new Set(b.map(x => x.larguraBotao))].join(',')}] larg.celula=[${[...new Set(b.map(x => x.larguraCelula))].join(',')}] ` +
      `>1linha=${multi.length} textoVazaBotao=${vaza.length} botaoSobreInterr=${sob.length} textoSobreInterr=${tsob.length} foraDaCelula=${fora.length} ` +
      `ladoALado=${b.filter(x => x.interruptor).every(x => x.interruptor.mesmaLinha && x.interruptor.aDireita)} ` +
      `ws=${[...new Set(b.map(x => x.whiteSpace))]} rolagemTabelas=${JSON.stringify(v.rolagemHorizontalTabelas)} ` +
      `rolagemPagina=${v.rolagemHorizontalPagina} erros=${(v.erros || []).length}`);
  }
})().catch(e => { console.error(e); process.exit(1); });
