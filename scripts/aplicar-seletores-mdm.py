#!/usr/bin/env python3
"""Aplica os seletores guiados de perfil (restrictions / allowedClasses / adminExtras).

Por que isto e um script e nao apenas edicoes soltas: em 26/07/2026 a arvore
server-source/server/src/main/webapp foi sobrescrita por uma copia antiga e todo o
trabalho feito direto nos arquivos se perdeu (o diretorio esta no .gitignore, entao nao
havia historico para restaurar). Este script torna a aplicacao repetivel: rodar de novo
depois de qualquer restauracao recoloca tudo no lugar.

E idempotente -- rodar duas vezes nao duplica nada.

Uso:
    python3 scripts/aplicar-seletores-mdm.py            # aplica
    python3 scripts/aplicar-seletores-mdm.py --check    # so verifica, nao escreve
"""
import argparse
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent / "server-source/server/src/main/webapp"

SCRIPTS = [
    "app/components/main/service/mdmCatalog.service.js",
    "app/components/main/directive/mdmOptionPicker.directive.js",
    "app/components/main/directive/mdmExtrasEditor.directive.js",
]

# Arquivos que o script NAO cria, apenas exige que existam (sao versionados a parte).
OBRIGATORIOS = SCRIPTS + [
    "app/components/main/view/directive/mdmOptionPicker.html",
    "app/components/main/view/directive/mdmExtrasEditor.html",
    "css/mdm-picker.css",
]

PICKER_RESTRICTIONS = """                    <div class='form-group'>
                        <label class='col-sm-3 control-label'>
                            <span localized>form.configuration.settings.mdm.restrictions</span>
                            <br>
                            <small class="text-muted">O que o usuario do tablet fica proibido de fazer.
                                Marque o que quiser bloquear.</small>
                        </label>
                        <div class='col-sm-9'>
                            <div class="alert alert-warning" ng-if="configuration.permissive"
                                 style="padding:8px 12px;font-size:12px;margin-bottom:8px;">
                                As restricoes estao desativadas porque o perfil esta em modo permissivo.
                                Desmarque "Modo permissivo" acima para poder editar.
                            </div>
                            <mdm-option-picker ng-model='configuration.restrictions'
                                               name="restrictions-c"
                                               grupos="catalogo.restrictionsPorGrupo"
                                               catalogo="catalogo.restrictions"
                                               ng-disabled="configuration.permissive"></mdm-option-picker>
                        </div>
                    </div>"""

PICKER_ACTIVITIES = """                    <div class='form-group'>
                        <label class='col-sm-3 control-label'>
                            <span localized>form.configuration.settings.mdm.allowed.classes</span>
                            <br>
                            <small class="text-muted">Telas que continuam abrindo mesmo com o kiosk
                                travado. Nada marcado = so o app principal abre.</small>
                        </label>
                        <div class='col-sm-9'>
                            <mdm-option-picker ng-model='configuration.allowedClasses'
                                               name="allowedClasses-c"
                                               grupos="atividadesGrupos"
                                               catalogo="atividadesCatalogo"
                                               ng-disabled="configuration.permissive"></mdm-option-picker>
                        </div>
                    </div>"""

EDITOR_EXTRAS = """                    <div class='form-group'>
                        <label class='col-sm-3 control-label'>
                            <span localized>form.configuration.settings.mdm.extras</span>
                            <br>
                            <small class="text-muted">Parametros entregues ao agente no momento
                                da matricula (QR code).</small>
                        </label>
                        <div class='col-sm-9'>
                            <mdm-extras-editor ng-model='configuration.adminExtras'
                                               name="adminExtras-c"></mdm-extras-editor>
                        </div>
                    </div>"""

TRECHO_CONTROLLER = """
            // Catalogo das opcoes validas de restricoes / atividades / admin extras.
            // Alimenta os seletores que substituiram os textarea de texto livre.
            $scope.catalogo = mdmCatalog;

            // Atividades sugeridas = base do catalogo + as atividades derivadas dos apps
            // escolhidos neste proprio perfil, para o admin nao ter de digitar nomes de classe.
            $scope.atividadesDisponiveis = function () {
                var lista = mdmCatalog.allowedActivitiesBase.slice();
                var vistos = {};
                lista.forEach(function (a) { vistos[a.key] = true; });
                ($scope.configuration && $scope.configuration.applications || []).forEach(function (app) {
                    if (!app.pkg || app.type && app.type !== 'app' || app.remove) {
                        return;
                    }
                    var chave = app.pkg + '.MainActivity';
                    if (!vistos[chave]) {
                        vistos[chave] = true;
                        lista.push({
                            grupo: 'Apps deste perfil',
                            key: chave,
                            label: app.name || app.pkg,
                            help: 'Atividade principal presumida de ' + app.pkg + '. Confirme o nome real da classe se o app nao abrir.'
                        });
                    }
                });
                lista.forEach(function (a) { if (!a.grupo) { a.grupo = 'Atividades comuns'; } });
                return lista;
            };

            // Arrays com identidade estavel: bindings '=' comparam por referencia, e recriar
            // o array a cada digest deixaria o Angular em loop de "infdig".
            $scope.atividadesCatalogo = [];
            $scope.atividadesGrupos = [];

            function recalcularAtividades() {
                var lista = $scope.atividadesDisponiveis();
                var ordem = [], mapa = {};
                lista.forEach(function (item) {
                    if (!mapa[item.grupo]) {
                        mapa[item.grupo] = {nome: item.grupo, itens: []};
                        ordem.push(mapa[item.grupo]);
                    }
                    mapa[item.grupo].itens.push(item);
                });
                $scope.atividadesCatalogo.length = 0;
                Array.prototype.push.apply($scope.atividadesCatalogo, lista);
                $scope.atividadesGrupos.length = 0;
                Array.prototype.push.apply($scope.atividadesGrupos, ordem);
            }

            $scope.$watch(function () {
                return ($scope.configuration && $scope.configuration.applications || [])
                    .map(function (a) { return a.pkg + ':' + (a.remove ? 'r' : ''); })
                    .join('|');
            }, recalcularAtividades);
"""

mudancas = []


def trocar(texto, alvo_re, novo, rotulo, ja_aplicado):
    """Substitui alvo_re por novo. Se ja_aplicado casar, considera feito e nao mexe."""
    if re.search(ja_aplicado, texto):
        return texto, False
    # re.S: os blocos alvo tem varias linhas, entao '.' precisa cruzar quebra de linha
    novo_texto, n = re.subn(alvo_re, lambda m: novo, texto, count=1, flags=re.S)
    if n == 0:
        raise SystemExit(f"ERRO: nao encontrei o ponto de aplicacao de '{rotulo}'. "
                         f"O arquivo mudou de forma inesperada -- revise a mao.")
    mudancas.append(rotulo)
    return novo_texto, True


def aplicar(check):
    faltando = [f for f in OBRIGATORIOS if not (RAIZ / f).exists()]
    if faltando:
        raise SystemExit("ERRO: arquivos ausentes:\n  " + "\n  ".join(faltando))

    # ---- index.html: registra os scripts e o css
    p = RAIZ / "index.html"
    s = p.read_text()
    for rel in SCRIPTS:
        nome = rel.rsplit("/", 1)[1]
        if nome not in s:
            ancora = "    <script src='app/components/main/service/main.service.js"
            i = s.index(ancora)
            s = s[:i] + f"    <script src='{rel}'></script>\n" + s[i:]
            mudancas.append(f"index.html: registra {nome}")
    if "css/mdm-picker.css" not in s:
        # o link de main.css carrega um token ?v=<hash> posto por stamp-assets.py,
        # entao a ancora tem de ser tolerante a ele
        s, n = re.subn(r"(<link rel='stylesheet' type='text/css' href='css/main\.css[^']*'>)",
                       r"\1\n    <link rel='stylesheet' type='text/css' href='css/mdm-picker.css'>",
                       s, count=1)
        if n == 0:
            raise SystemExit("ERRO: nao achei o link de css/main.css no index.html.")
        mudancas.append("index.html: registra mdm-picker.css")
    if not check:
        p.write_text(s)

    # ---- configuration.html: troca os 3 textarea pelos seletores
    p = RAIZ / "app/components/main/view/configuration.html"
    s = p.read_text()
    s, _ = trocar(s,
                  r"[ \t]*<div class='form-group'>\s*<label[^>]*>\s*<span localized>form\.configuration\.settings\.mdm\.restrictions</span>.*?</div>\s*</div>",
                  PICKER_RESTRICTIONS, "configuration.html: seletor de Restrictions",
                  r"mdm-option-picker[^>]*configuration\.restrictions")
    s, _ = trocar(s,
                  r"[ \t]*<div class='form-group'>\s*<label[^>]*>form\.configuration\.settings\.mdm\.allowed\.classes</label>.*?</div>\s*</div>",
                  PICKER_ACTIVITIES, "configuration.html: seletor de Allowed Activities",
                  r"mdm-option-picker[^>]*configuration\.allowedClasses")
    s, _ = trocar(s,
                  r"[ \t]*<div class='form-group'>\s*<label[^>]*>\s*<span localized>form\.configuration\.settings\.mdm\.extras</span></label>.*?</div>\s*</div>",
                  EDITOR_EXTRAS, "configuration.html: editor de Admin Extras",
                  r"mdm-extras-editor")
    if not check:
        p.write_text(s)

    # ---- configurations.controller.js: injeta mdmCatalog no editor
    p = RAIZ / "app/components/main/controller/configurations.controller.js"
    s = p.read_text()
    if "mdmCatalog" not in s:
        alvo = "appVersionComparisonService, settingsService) {\n\n            $scope.successMessage = null;"
        if alvo not in s:
            raise SystemExit("ERRO: assinatura do ConfigurationEditorController mudou; revise a mao.")
        s = s.replace(alvo,
                      "appVersionComparisonService, settingsService,\n"
                      "                  mdmCatalog) {\n\n"
                      "            $scope.successMessage = null;\n" + TRECHO_CONTROLLER,
                      1)
        mudancas.append("configurations.controller.js: injeta mdmCatalog")
        if not check:
            p.write_text(s)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true", help="so relata, nao escreve")
    args = ap.parse_args()
    aplicar(args.check)
    if not mudancas:
        print("Nada a fazer: os seletores ja estao aplicados.")
        return 0
    verbo = "Faltam aplicar" if args.check else "Aplicado"
    print(f"{verbo}:")
    for m in mudancas:
        print("  -", m)
    return 1 if args.check else 0


if __name__ == "__main__":
    sys.exit(main())
