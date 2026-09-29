// HWMDM 1.0 -- versao/build no rodape (pacote de trabalho "versao").
//
// Uso:
//   <hwmdm-version-footer></hwmdm-version-footer>
//
// O pacote de trabalho "modulos" e' quem coloca este elemento no rodape do
// menu lateral (dentro de .hwmdm-sidebar-footer, ~240px de largura, em
// content.html). Se esse elemento ainda nao estiver la, esta diretiva
// simplesmente nao aparece em lugar nenhum -- mas continua funcional para
// quem a colocar manualmente em qualquer outra tela (ex.: dentro do
// dialogo About, para conferir o build sem depender do rodape).
//
// Le server-source/.../build-info.json SEM cache: o build muda a cada
// "docker cp" no DEV (varios agentes publicam ao mesmo tempo -- ver memoria
// "Agentes concorrentes"), entao o navegador nao pode segurar um build
// velho em cache do $http nem de HTTP.
//
// Ver docs/VERSIONAMENTO.md para o significado de cada campo do
// build-info.json (versao, build, compilacao, semver_completo, commit,
// sujo, componentes).
angular.module('headwind-kiosk')
    .directive('hwmdmVersionFooter', ['$http', '$uibModal', function ($http, $uibModal) {

        // Rotulos legiveis para as chaves de "componentes" do build-info.json.
        // Mantido aqui (nao no build-info.json) porque e' texto de interface,
        // nao dado de build.
        var ROTULOS_COMPONENTES = {
            nucleo_headwind: 'Núcleo Headwind MDM (base)',
            plugin_webfilter: 'Plugin Web Filter',
            plugin_moduleregistry: 'Plugin Module Registry',
            agente_remoto_android: 'Agente de acesso remoto (Android)',
            resolvedor_dns_webfilter: 'Resolvedor DNS do Web Filter'
        };

        function listaDeComponentes(componentes) {
            var lista = [];
            if (!componentes) {
                return lista;
            }
            Object.keys(componentes).forEach(function (chave) {
                var c = componentes[chave] || {};
                var versaoExibida = c.versao;
                if (versaoExibida === undefined && c.versionName !== undefined) {
                    versaoExibida = c.versionName +
                        (c.versionCode !== undefined && c.versionCode !== null
                            ? ' (versionCode ' + c.versionCode + ')' : '');
                }
                lista.push({
                    nome: ROTULOS_COMPONENTES[chave] || chave,
                    versao: versaoExibida || '—',
                    origem: c.origem || ''
                });
            });
            return lista;
        }

        // Markup do modal de detalhes, montado aqui (sem templateUrl) para
        // este pacote nao depender de mais um arquivo alem dos dois que lhe
        // foram atribuidos (esta diretiva + o CSS).
        var TEMPLATE_MODAL =
            '<div class="modal-header hwmdm-version-modal__header">' +
                '<h4 class="modal-title">HWMDM {{info.versao}}</h4>' +
            '</div>' +
            '<div class="modal-body hwmdm-version-modal__body">' +
                '<dl class="hwmdm-version-modal__lista">' +
                    '<dt>Versão</dt><dd>{{info.versao}}</dd>' +
                    '<dt>Build</dt><dd>{{info.build}}</dd>' +
                    '<dt>Compilação</dt><dd>{{info.compilacao}}</dd>' +
                    '<dt>Commit</dt><dd>{{info.commit}}<span ng-if="info.sujo"> (árvore com alterações não commitadas)</span></dd>' +
                    '<dt>Data do build (UTC)</dt><dd>{{info.data_build_utc}}</dd>' +
                    '<dt>SemVer completo</dt><dd class="hwmdm-version-modal__semver">{{info.semver_completo}}</dd>' +
                '</dl>' +
                '<h5 class="hwmdm-version-modal__subtitulo">Componentes</h5>' +
                '<table class="hwmdm-version-modal__tabela">' +
                    '<tr ng-repeat="c in componentes">' +
                        '<td>{{c.nome}}</td>' +
                        '<td>{{c.versao}}</td>' +
                    '</tr>' +
                '</table>' +
            '</div>' +
            '<div class="modal-footer">' +
                '<button type="button" class="btn btn-default" ng-click="fechar()">Fechar</button>' +
            '</div>';

        return {
            restrict: 'E',
            scope: {},
            template:
                '<div class="hwmdm-version-footer">' +
                    '<button type="button" class="hwmdm-version-footer__botao" ' +
                        'ng-if="info" ng-click="abrirDetalhes()" title="{{tooltip}}">' +
                        '<span class="hwmdm-version-footer__plataforma">HWMDM {{info.versao}}</span>' +
                        '<span class="hwmdm-version-footer__build">build {{info.build}} · {{info.commit}}</span>' +
                    '</button>' +
                    '<span class="hwmdm-version-footer__indisponivel" ng-if="!info && carregado">' +
                        'versão indisponível' +
                    '</span>' +
                '</div>',
            link: function (scope) {
                scope.info = null;
                scope.carregado = false;
                scope.tooltip = '';

                // Query anti-cache alem do cache:false: alguns proxies/CDNs
                // ignoram os headers de no-cache do $http mas respeitam URL
                // diferente a cada carga.
                var urlAntiCache = 'build-info.json?_=' + new Date().getTime();

                $http.get(urlAntiCache, { cache: false }).then(function (resposta) {
                    scope.info = resposta.data && resposta.data.versao ? resposta.data : null;
                    scope.carregado = true;
                    if (scope.info) {
                        scope.tooltip = scope.info.semver_completo + ' — ' + scope.info.data_build_utc;
                    }
                }, function () {
                    // build-info.json ausente ou inacessivel: nao quebra a
                    // tela, so mostra "versao indisponivel".
                    scope.info = null;
                    scope.carregado = true;
                });

                scope.abrirDetalhes = function () {
                    if (!scope.info) {
                        return;
                    }
                    var infoAtual = scope.info;
                    $uibModal.open({
                        template: TEMPLATE_MODAL,
                        windowClass: 'hwmdm-version-modal',
                        controller: ['$scope', '$uibModalInstance', function ($scope, $uibModalInstance) {
                            $scope.info = infoAtual;
                            $scope.componentes = listaDeComponentes(infoAtual.componentes);
                            $scope.fechar = function () {
                                $uibModalInstance.dismiss();
                            };
                        }]
                    });
                };
            }
        };
    }]);
