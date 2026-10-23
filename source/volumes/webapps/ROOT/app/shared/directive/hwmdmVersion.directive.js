// Versao da plataforma no rodape do menu lateral. build-info.json e' gerado pelo
// docker-entrypoint.sh a cada subida (HWMDM_VERSION, commit do .git, componentes instalados).
angular.module('headwind-kiosk')
    .directive('hwmdmVersionFooter', ['$http', '$uibModal', function ($http, $uibModal) {

        function listaDeComponentes(info) {
            var lista = [];
            [info.componentes, info.agentes].forEach(function (grupo) {
                Object.keys(grupo || {}).forEach(function (chave) {
                    var c = grupo[chave] || {};
                    lista.push({nome: c.nome || chave, versao: c.versao || '—'});
                });
            });
            return lista;
        }

        var TEMPLATE_MODAL =
            '<div class="modal-header hwmdm-version-modal__header">' +
                '<h4 class="modal-title">HWMDM {{info.versao}}</h4>' +
            '</div>' +
            '<div class="modal-body hwmdm-version-modal__body">' +
                '<dl class="hwmdm-version-modal__lista">' +
                    '<dt>Versão</dt><dd>{{info.versao}}</dd>' +
                    '<dt>Commit</dt><dd class="hwmdm-version-modal__semver">{{info.commit}}</dd>' +
                    '<dt>Branch</dt><dd>{{info.ref}}</dd>' +
                    '<dt>Data do commit (UTC)</dt><dd>{{info.data_commit_utc}}</dd>' +
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
                        'ng-if="info" ng-click="abrirDetalhes()" title="{{info.data_commit_utc}}">' +
                        '<span class="hwmdm-version-footer__plataforma">HWMDM {{info.versao}}</span>' +
                        '<span class="hwmdm-version-footer__build">{{info.ref}} · {{info.commit}}</span>' +
                    '</button>' +
                    '<span class="hwmdm-version-footer__indisponivel" ng-if="!info && carregado">' +
                        'versão indisponível' +
                    '</span>' +
                '</div>',
            link: function (scope) {
                scope.info = null;
                scope.carregado = false;

                $http.get('build-info.json?_=' + new Date().getTime(), { cache: false }).then(function (resposta) {
                    scope.info = resposta.data && resposta.data.versao ? resposta.data : null;
                    scope.carregado = true;
                }, function () {
                    scope.info = null;
                    scope.carregado = true;
                });

                scope.abrirDetalhes = function () {
                    var infoAtual = scope.info;
                    $uibModal.open({
                        template: TEMPLATE_MODAL,
                        windowClass: 'hwmdm-version-modal',
                        controller: ['$scope', '$uibModalInstance', function ($scope, $uibModalInstance) {
                            $scope.info = infoAtual;
                            $scope.componentes = listaDeComponentes(infoAtual);
                            $scope.fechar = function () {
                                $uibModalInstance.dismiss();
                            };
                        }]
                    });
                };
            }
        };
    }]);
