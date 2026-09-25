#!/usr/bin/env python3
"""Gera uma pagina estatica que renderiza o template REAL de devices.html (sem Angular)
com o CSS REAL do console, para medir larguras em Chromium headless.
Nao e' teste da interface: e' uma calculadora de layout com os mesmos arquivos.

uso: gen.py <raiz_webapp> <tema light|dark> <cenario A|B> <saida.html>
"""
import json
import sys
from pathlib import Path

raiz = Path(sys.argv[1]).resolve()
tema = sys.argv[2]
cenario = sys.argv[3]
saida = Path(sys.argv[4])

tpl = (raiz / "app/components/main/view/devices.html").read_text(encoding="utf-8")
real = Path("/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/webapp")

css = [
    "lib/ng-tags-input/ng-tags-input.css",
    "lib/angular-bootstrap-colorpicker/colorpicker.css",
    "lib/angular-input-dropdown/inputDropdownStyles.css",
    "lib/intro.js/introjs.css",
    "lib/bootstrap-css-only/css/bootstrap.css",
    "css/main.css",
    "css/mdm-picker.css",
    "css/hwmdm-modules.css",
    "css/hwmdm-version.css",
    "css/hwmdm-ui.css",
]
links = []
for c in css:
    p = raiz / c
    if not p.exists():
        p = real / c
    links.append(f"<link rel='stylesheet' href='file://{p}'>")

# Cenario A = colunas citadas na queixa do usuario. B = A + Modelo, IMEI, Descricao, Grupo.
A = [
    "columnDisplayedDeviceStatus", "columnDisplayedDeviceDate", "columnDisplayedDeviceNumber",
    "columnDisplayedDevicePermissionsStatus", "columnDisplayedDeviceAppInstallStatus",
    "columnDisplayedDeviceFilesStatus", "columnDisplayedDeviceConfiguration",
    "columnDisplayedLauncherVersion", "columnDisplayedBatteryLevel", "columnDisplayedMdmMode",
    "columnDisplayedKioskMode", "columnDisplayedAndroidVersion", "columnDisplayedEnrollmentDate",
    "columnDisplayedSerial", "columnDisplayedPublicIp",
]
B = A + ["columnDisplayedDeviceModel", "columnDisplayedDeviceImei",
         "columnDisplayedDeviceDesc", "columnDisplayedDeviceGroup"]
flags = {k: True for k in (A if cenario == "A" else B)}

html = f"""<!doctype html>
<html data-theme="{tema.split('-')[0]}"><head><meta charset="utf-8">
<base href="file://{real}/">
{''.join(links)}
<script src="file://{real}/localization/pt_PT.js"></script>
</head><body>
<div class="content-wrapper fullscreen"><div class="hwmdm-workspace">
<div class="hwmdm-navbar"></div><div class="hwmdm-nav-scrim"></div>
<aside class="hwmdm-sidebar" id="hwmdm-sidebar"><div style="height:640px"></div></aside>
<main class="hwmdm-main"><div class="hwmdm-main-surface" id="host"></div></main>
</div></div>
<pre id="out"></pre>
<script>
const TPL = {json.dumps(tpl)};
const FLAGS = {json.dumps(flags)};
const L = document.localization['pt_PT'];
if ({json.dumps(tema)}.includes('-de')) {{ L['table.heading.device.status.permissions'] = 'Berechtigungsstatus'; L['table.heading.device.status.installation'] = 'Installationsstatus'; document.documentElement.setAttribute('data-theme', 'light'); }}
const loc = k => (L && L[k.trim()]) || k.trim();
const T0 = Date.UTC(2026, 8, 25, 17, 33);
const devs = [
  ['R9XT200AMYY','192.168.1.100'], ['R9XT106VP1E','192.168.1.128'],
  ['R9XT106Y5RP','192.168.1.165'], ['R9XT108EM8T','192.168.1.123']
].map((d, i) => ({{id: 46 + i, number: d[0], ip: d[1], lastUpdateDate: T0 - i * 60000,
  enrollTime: T0 - 86400000 * 3, configuration: {{name: 'Kiosk Total (6.37.3)', qrCodeKey: 'k'}},
  groups: [{{name: 'Tablets'}}], description: 'Tablet sala ' + (i + 1), displayedIMEI: '35' + (870000000000 + i),
  info: {{mdmMode: true}}, 'class': ''}}));
const root = {{
  settings: FLAGS, hasPermission: () => true, hasDeviceActions: () => true,
  plugins: [{{nameLocalizationKey: 'plugin.x'}}], commonSettings: {{deviceLimit: 0}},
  devices: devs, paging: {{totalItems: 4, pageSize: 50, pageNum: 1, sortBy: ({json.dumps(tema)}.includes('-sort') ? 'PERMISSIONS' : undefined), sortAsc: true}}, selection: {{}},
  additionalParams: {{enabled: false}}, searchParams: {{}}, accountExpired: false, errorMessage: '',
  configAvailable: () => true, firstRecord: () => 1, lastRecord: () => 4, isNotSelected: () => true,
  getDeviceIndicatorImage: () => 'images/online.png', calculateStatusText: () => 'Online',
  getDevicePermissionIndicatorImage: () => 'images/online.png', getDevicePermissionTitle: () => '',
  getDeviceApplicationsIndicatorImage: () => 'images/online.png', getDeviceApplicationsTitle: () => '',
  getDeviceFilesIndicatorImage: () => 'images/online.png', getDeviceFilesTitle: () => '',
  getDeviceModel: () => 'SM-X200', formatMultiLine: t => t,
  getDeviceLauncherVersionColor: () => '', getDeviceLauncherVersion: () => '6.37.3',
  getDeviceBatteryLevel: () => '85%', getDeviceStorageAvailable: () => '12 GB',
  isBackgroundMode: () => 'Nao', getIsMdmMode: () => 'Sim', isKioskMode: () => 'Sim',
  getAndroidVersion: () => '14', getSerial: d => d.number,
  getDeviceIpDisplay: d => d.ip, getDeviceIpNote: () => loc('devices.ip.reported.agent'),
  dateFormat: 'dd/MM/yy HH:mm'
}};
function ev(expr, s) {{
  try {{ return (new Function('s', 'with (s) {{ return (' + expr + '); }}'))(s); }}
  catch (e) {{ return undefined; }}
}}
function fmt(ms) {{
  const d = new Date(ms), p = n => String(n).padStart(2, '0');
  return p(d.getUTCDate()) + '/' + p(d.getUTCMonth() + 1) + '/' + String(d.getUTCFullYear()).slice(2)
       + ' ' + p(d.getUTCHours()) + ':' + p(d.getUTCMinutes());
}}
function interp(str, s) {{
  return str.replace(/\\{{\\{{([^}}]*)\\}}\\}}/g, (m, e) => {{
    let f = null;
    const mm = e.match(/^(.*?)\\|\\s*(date|localize)\\b.*$/);
    if (mm && !/\\|\\|/.test(e)) {{ e = mm[1]; f = mm[2]; }}
    let v = ev(e, s);
    if (f === 'date') v = fmt(v);
    if (f === 'localize') v = loc(String(v));
    return v == null ? '' : String(v);
  }});
}}
function walk(el, s) {{
  if (el.nodeType === 3) {{ el.nodeValue = interp(el.nodeValue, s); return; }}
  if (el.nodeType !== 1) return;
  const rep = el.getAttribute('ng-repeat');
  if (rep) {{
    const m = rep.match(/^\\s*(\\w+)\\s+in\\s+(.+?)\\s*$/);
    const list = (m && ev(m[2], s)) || [];
    const parent = el.parentNode, next = el.nextSibling;
    el.remove();
    for (const item of list) {{
      const c = el.cloneNode(true);
      c.removeAttribute('ng-repeat');
      const cs = Object.create(s); cs[m[1]] = item;
      parent.insertBefore(c, next);
      walk(c, cs);
    }}
    return;
  }}
  const nif = el.getAttribute('ng-if');
  if (nif !== null && !ev(nif, s)) {{ el.remove(); return; }}
  const nsh = el.getAttribute('ng-show');
  if (nsh !== null && !ev(nsh, s)) el.classList.add('ng-hide');
  for (const a of Array.from(el.attributes)) {{
    if (a.value.includes('{{{{')) el.setAttribute(a.name, interp(a.value, s));
  }}
  if (el.hasAttribute('ng-src')) el.setAttribute('src', el.getAttribute('ng-src'));
  if (el.hasAttribute('localized-title')) el.setAttribute('title', loc(el.getAttribute('localized-title')));
  if (el.hasAttribute('ng-bind-html')) el.innerHTML = String(ev(el.getAttribute('ng-bind-html'), s) || '');
  if (el.hasAttribute('localized')) {{ el.textContent = loc(el.textContent); return; }}
  for (const c of Array.from(el.tagName === 'TEMPLATE' ? el.content.childNodes : el.childNodes)) walk(c, s);
}}
const t = document.createElement('template');
t.innerHTML = TPL;
const frag = t.content;
for (const c of Array.from(frag.childNodes)) walk(c, root);
document.getElementById('host').appendChild(frag);

function measure() {{
  const wrap = document.querySelector('.table-responsive');
  const table = wrap.querySelector('table');
  wrap.scrollLeft = 0;
  const wr = wrap.getBoundingClientRect();
  const ths = Array.from(table.querySelectorAll('thead th'));
  const act = table.querySelector('thead th:last-child');
  const actTd = table.querySelector('tbody tr td:last-child');
  const ar = act.getBoundingClientRect();
  const btns = Array.from(actTd.querySelectorAll('.btn')).map(b => {{
    const r = b.getBoundingClientRect(); return [Math.round(r.left), Math.round(r.top), Math.round(r.width), Math.round(r.height)];
  }});
  const tdr = actTd.getBoundingClientRect();
  const overflowBtn = btns.some(b => b[0] < tdr.left - 0.5 || b[0] + b[2] > tdr.right + 0.5);
  // rola ate' o fim e mede de novo a coluna Acoes (sticky deve continuar no mesmo lugar)
  wrap.scrollLeft = wrap.scrollWidth;
  const ar2 = act.getBoundingClientRect();
  wrap.scrollLeft = 0;
  const list = document.querySelector('.hwmdm-list-page');
  const ws = document.querySelector('.hwmdm-workspace');
  const cs = getComputedStyle(actTd);
  const out = {{
    viewport: innerWidth,
    workspace: [Math.round(ws.getBoundingClientRect().left), Math.round(ws.getBoundingClientRect().width)],
    main: Math.round(document.querySelector('.hwmdm-main').getBoundingClientRect().width),
    listPage: Math.round(list.getBoundingClientRect().width),
    wrapClient: wrap.clientWidth, wrapScroll: wrap.scrollWidth, tableWidth: Math.round(table.getBoundingClientRect().width),
    hScroll: wrap.scrollWidth > wrap.clientWidth,
    actionsVisibleAtScroll0: ar.right <= wr.right + 1 && ar.left >= wr.left - 1,
    actionsLeftAtScroll0: Math.round(ar.left - wr.left), actionsLeftAtScrollEnd: Math.round(ar2.left - wr.left),
    actionsTdPosition: cs.position, actionsTdBg: cs.backgroundColor, actionsTdShadow: cs.boxShadow,
    actionsBtnOverflowCell: overflowBtn, actionsBtns: btns.length, actionsCell: [Math.round(tdr.width), Math.round(tdr.height)],
    rowHeight: Math.round(table.querySelector('tbody tr').getBoundingClientRect().height),
    thFont: getComputedStyle(ths[1]).fontSize, thWhite: getComputedStyle(ths[1]).whiteSpace, thPad: getComputedStyle(ths[1]).padding,
    tdFont: getComputedStyle(table.querySelector('tbody td:nth-child(3)')).fontSize,
    tdPad: getComputedStyle(table.querySelector('tbody td:nth-child(3)')).padding,
    cols: ths.map(th => [th.textContent.replace(/\\s+/g, ' ').trim() || '[x]', Math.round(th.getBoundingClientRect().width)]),
    spans: ths.map(th => {{ const sp = th.querySelector('span'); if (!sp) return null; const r = sp.getBoundingClientRect(); return [Math.round(r.width), Math.round(r.height)]; }}),
    tdWidths: Array.from(table.querySelectorAll('tbody tr:first-child > td')).map(td => {{ const c = td.firstElementChild; return c ? Math.round(c.getBoundingClientRect().width) : null; }}),
    dotOverflow: Array.from(table.querySelectorAll('thead th.devices-th-dot')).map(th => {{ const sp = th.querySelector('span'); const a = sp.getBoundingClientRect(), b = th.getBoundingClientRect(); return [Math.round(a.height), sp.scrollHeight, Math.round(b.height), a.top >= b.top - 0.5 && a.bottom <= b.bottom + 0.5 && sp.scrollHeight <= Math.ceil(a.height) + 1]; }}),
    btnInnerOverflow: Array.from(actTd.querySelectorAll('.btn')).filter(b => b.scrollWidth > b.clientWidth + 0.5 || b.scrollHeight > b.clientHeight + 0.5).length,
    ipCell: (() => {{ const td = table.querySelector('tbody tr:first-child td:has(.device-address-secondary)') || null; if (!td) return null; const sp = td.querySelectorAll('span'); return [Math.round(td.getBoundingClientRect().width), Math.round(sp[0].getBoundingClientRect().height), Math.round(sp[1].getBoundingClientRect().height)]; }})(),
    theadH: Math.round(table.querySelector('thead').getBoundingClientRect().height),
    fontsLoaded: Array.from(document.fonts).filter(f => f.status === 'loaded').map(f => f.family + f.weight).join(',')
  }};
  document.getElementById('out').textContent = 'RESULT' + JSON.stringify(out) + 'END';
}}
document.fonts.ready.then(() => setTimeout(measure, 50));
</script></body></html>
"""
saida.write_text(html, encoding="utf-8")
