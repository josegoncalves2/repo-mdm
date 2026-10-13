// Editor dos "Admin extras" do perfil.
//
// O campo configurations.adminExtras e um FRAGMENTO de JSON, nao um JSON completo: o
// servidor o concatena dentro do bundle de provisionamento, logo depois de
// "com.hmdm.SERVER_PROJECT", ja precedido de virgula
// (ver QRCodeResource.generateExtrasBundle). Portanto o valor gravado tem de ser
// pares "chave":"valor" separados por virgula, SEM chaves envolventes e SEM virgula final:
//
//     "com.hmdm.SKIP_INTRO":"true",
//     "com.hmdm.CONN_RETRY_COUNT":"5"
//
// Antes disso o admin tinha de digitar esse fragmento a mao, sem lista de chaves validas.
angular.module('headwind-kiosk')
    .directive('mdmExtrasEditor', ['mdmCatalog', function (mdmCatalog) {
        return {
            restrict: 'E',
            require: 'ngModel',
            // Escopo FILHO, nao isolado: com isolado o ng-model do elemento e' avaliado
            // contra o escopo isolado e nunca alcanca configuration.adminExtras.
            scope: true,
            templateUrl: 'app/components/main/view/directive/mdmExtrasEditor.html?v=h2aa92a4a65',
            link: function (scope, element, attrs, ngModel) {

                scope.desabilitado = false;
                if (attrs.ngDisabled) {
                    scope.$watch(attrs.ngDisabled, function (v) { scope.desabilitado = !!v; });
                }

                scope.disponiveis = mdmCatalog.adminExtrasEditaveis;
                scope.automaticas = mdmCatalog.adminExtras.filter(function (e) { return e.auto; });
                scope.pares = [];
                scope.erro = null;
                scope.mostrarAvancado = false;

                function definicao(chave) {
                    var achou = null;
                    mdmCatalog.adminExtras.forEach(function (e) {
                        if (e.key === chave) { achou = e; }
                    });
                    return achou;
                }
                scope.definicao = definicao;

                // fragmento -> lista de pares. Envolve em chaves para poder usar JSON.parse,
                // que e mais confiavel que regex para lidar com aspas escapadas.
                ngModel.$render = function () {
                    var bruto = (ngModel.$viewValue || '').trim();
                    scope.erro = null;
                    scope.pares = [];
                    if (!bruto) {
                        return;
                    }
                    if (bruto.charAt(bruto.length - 1) === ',') {
                        bruto = bruto.substring(0, bruto.length - 1);
                    }
                    try {
                        var obj = JSON.parse('{' + bruto + '}');
                        Object.keys(obj).forEach(function (k) {
                            scope.pares.push({chave: k, valor: String(obj[k])});
                        });
                    } catch (e) {
                        // nao descarta o que o admin ja tinha: mostra em modo texto para ele corrigir
                        scope.erro = 'O conteudo atual nao e um fragmento JSON valido. '
                            + 'Corrija no modo avancado abaixo para poder usar o editor.';
                        scope.mostrarAvancado = true;
                    }
                };

                function gravar() {
                    var partes = scope.pares
                        .filter(function (p) { return (p.chave || '').trim(); })
                        .map(function (p) {
                            return JSON.stringify(p.chave.trim()) + ':' + JSON.stringify(String(p.valor === undefined || p.valor === null ? '' : p.valor));
                        });
                    ngModel.$setViewValue(partes.join(',\n'));
                }
                scope.gravar = gravar;

                scope.adicionar = function (def) {
                    var existe = scope.pares.some(function (p) { return p.chave === def.key; });
                    if (existe) {
                        return;
                    }
                    scope.pares.push({
                        chave: def.key,
                        valor: def.padrao !== undefined ? String(def.padrao)
                             : def.tipo === 'booleano' ? 'true'
                             : def.tipo === 'opcao' ? def.opcoes[0]
                             : ''
                    });
                    gravar();
                };

                scope.adicionarLivre = function () {
                    scope.pares.push({chave: '', valor: ''});
                };

                scope.remover = function (indice) {
                    scope.pares.splice(indice, 1);
                    gravar();
                };

                scope.jaAdicionada = function (def) {
                    return scope.pares.some(function (p) { return p.chave === def.key; });
                };

                // Modo avancado: edicao crua do fragmento, para casos que o editor nao cobre.
                // Usado com ng-model-options="{getterSetter: true}" para ler e gravar no
                // proprio ngModel da diretiva, sem depender da cadeia de escopos pai.
                scope.bruto = function (novo) {
                    if (angular.isDefined(novo)) {
                        ngModel.$setViewValue(novo);
                        ngModel.$render();
                    }
                    return ngModel.$viewValue;
                };
            }
        };
    }]);
