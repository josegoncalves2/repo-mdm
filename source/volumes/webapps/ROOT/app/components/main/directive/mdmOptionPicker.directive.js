// Seletor de opcoes para os campos que antes eram textarea de texto livre
// (restrictions, allowedClasses). Le o catalogo de mdmCatalog e grava de volta a mesma
// string CSV que o servidor ja esperava — nenhuma mudanca de contrato com o backend.
//
// Uso:
//   <mdm-option-picker ng-model="configuration.restrictions"
//                      grupos="catalogo.restrictionsPorGrupo"
//                      catalogo="catalogo.restrictions"
//                      ng-disabled="configuration.permissive"></mdm-option-picker>
angular.module('headwind-kiosk')
    .directive('mdmOptionPicker', ['mdmCatalog', function (mdmCatalog) {
        return {
            restrict: 'E',
            require: 'ngModel',
            // Escopo FILHO, nao isolado. Com escopo isolado o ng-model do proprio elemento
            // e' avaliado contra o escopo isolado -- 'configuration.restrictions' nao existe
            // la, entao a leitura vinha vazia e a gravacao nao chegava ao perfil.
            // Com scope:true o ng-model resolve 'configuration' herdado do pai por
            // referencia, e escrever .restrictions altera o mesmo objeto que o controller ve.
            scope: true,
            templateUrl: 'app/components/main/view/directive/mdmOptionPicker.html?v=hc619afb59f',
            link: function (scope, element, attrs, ngModel) {

                // Sem bindings '=', as entradas sao lidas por watch sobre o escopo pai.
                // Nomes internos distintos dos atributos para nao sombrear nada herdado.
                scope.gruposLista = [];
                scope.catalogoLista = [];
                scope.desabilitado = false;
                scope.$watch(attrs.grupos, function (v) { scope.gruposLista = v || []; });
                scope.$watch(attrs.catalogo, function (v) {
                    scope.catalogoLista = v || [];
                    ngModel.$render();
                });
                if (attrs.ngDisabled) {
                    scope.$watch(attrs.ngDisabled, function (v) { scope.desabilitado = !!v; });
                }

                scope.busca = '';
                scope.selecionadas = {};
                scope.extras = [];      // chaves gravadas no perfil que nao estao no catalogo
                scope.novaChave = '';
                scope.recolhido = {};

                // string CSV -> mapa de marcados
                ngModel.$render = function () {
                    var lista = mdmCatalog.csvParaLista(ngModel.$viewValue);
                    scope.selecionadas = {};
                    lista.forEach(function (k) { scope.selecionadas[k] = true; });
                    scope.extras = mdmCatalog.desconhecidas(ngModel.$viewValue, scope.catalogoLista || []);
                };

                // mapa de marcados -> string CSV, preservando a ordem do catalogo
                // e mantendo no fim as chaves personalizadas que o admin ja tinha.
                function gravar() {
                    var saida = [];
                    (scope.catalogoLista || []).forEach(function (item) {
                        if (scope.selecionadas[item.key]) {
                            saida.push(item.key);
                        }
                    });
                    scope.extras.forEach(function (k) {
                        if (scope.selecionadas[k]) {
                            saida.push(k);
                        }
                    });
                    ngModel.$setViewValue(mdmCatalog.listaParaCsv(saida));
                }

                scope.alternar = function (item) {
                    if (scope.desabilitado) {
                        return;
                    }
                    scope.selecionadas[item.key] = !scope.selecionadas[item.key];
                    gravar();
                };

                scope.adicionarPersonalizada = function () {
                    var chave = (scope.novaChave || '').trim();
                    if (!chave) {
                        return;
                    }
                    if (scope.extras.indexOf(chave) === -1) {
                        scope.extras.push(chave);
                    }
                    scope.selecionadas[chave] = true;
                    scope.novaChave = '';
                    gravar();
                };

                scope.removerPersonalizada = function (chave) {
                    scope.extras = scope.extras.filter(function (k) { return k !== chave; });
                    delete scope.selecionadas[chave];
                    gravar();
                };

                scope.marcarGrupo = function (grupo, valor) {
                    if (scope.desabilitado) {
                        return;
                    }
                    grupo.itens.forEach(function (item) {
                        if (scope.visivel(item)) {
                            scope.selecionadas[item.key] = valor;
                        }
                    });
                    gravar();
                };

                scope.limparTudo = function () {
                    if (scope.desabilitado) {
                        return;
                    }
                    scope.selecionadas = {};
                    gravar();
                };

                // filtro da caixa de busca: casa rotulo, chave ou texto de ajuda
                scope.visivel = function (item) {
                    var termo = (scope.busca || '').trim().toLowerCase();
                    if (!termo) {
                        return true;
                    }
                    return (item.label || '').toLowerCase().indexOf(termo) !== -1
                        || (item.key || '').toLowerCase().indexOf(termo) !== -1
                        || (item.help || '').toLowerCase().indexOf(termo) !== -1;
                };

                scope.grupoTemVisivel = function (grupo) {
                    return grupo.itens.some(scope.visivel);
                };

                scope.contarGrupo = function (grupo) {
                    return grupo.itens.filter(function (i) { return scope.selecionadas[i.key]; }).length;
                };

                scope.totalSelecionado = function () {
                    return Object.keys(scope.selecionadas).filter(function (k) {
                        return scope.selecionadas[k];
                    }).length;
                };

                scope.alternarGrupoRecolhido = function (nome) {
                    scope.recolhido[nome] = !scope.recolhido[nome];
                };
            }
        };
    }]);
