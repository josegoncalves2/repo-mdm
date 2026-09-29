/*
 * HWMDM 1.0 - tela "Integracoes e extensoes" (pacote de trabalho "modulos", item 16 do
 * pedido). Substitui "More plugins..." (o plugin xtra, que nunca fez nada e nao aparece em
 * lugar nenhum agora) por uma secao com proposito: o que este servidor tem instalado, os
 * pontos de integracao que EXISTEM de verdade neste servidor (cada um conferido abrindo o
 * endpoint ou lendo o codigo-fonte antes de entrar nesta lista) e o procedimento real para
 * acrescentar um modulo novo.
 *
 * Este arquivo reaproveita o nome de arquivo "plugins.controller.js" (estava dedicado ao
 * antigo PluginsTabController, hoje redistribuido entre TabController/ModulesTabController) -
 * nenhuma outra tela referenciava PluginsTabController pelo nome, entao a troca e segura.
 */
angular.module('headwind-kiosk')
    .controller('IntegrationsTabController', function ($scope, $window, $q, moduleRegistry, localization) {

        $scope.localization = localization;
        $scope.loading = true;
        $scope.installedPlugins = [];

        moduleRegistry.load().then(function () {
            $scope.loading = false;
            $scope.installedPlugins = moduleRegistry.installedPlugins();
            $scope.isPluginActive = moduleRegistry.isPluginActiveForCustomer;
        });

        // Cada endereco abaixo foi conferido lendo o codigo-fonte deste servidor (rest/swagger.json
        // gerado a partir das proprias classes JAX-RS, plugins/push, plugins/moduleregistry,
        // com.hmdm.rest.SyncResource e com.hmdm.rest.QRResource) antes de entrar nesta lista -
        // nenhum item aqui e' suposicao.
        $scope.integrationPoints = [
            {
                id: 'rest-api',
                icon: 'file-text',
                titleKey: 'integrations.point.restapi.title',
                descKey: 'integrations.point.restapi.desc',
                address: 'rest/swagger.json'
            },
            {
                id: 'push-api',
                icon: 'send',
                titleKey: 'integrations.point.push.title',
                descKey: 'integrations.point.push.desc',
                address: 'POST rest/private/push'
            },
            {
                id: 'webfilter-dns',
                icon: 'filter',
                titleKey: 'integrations.point.dns.title',
                descKey: 'integrations.point.dns.desc',
                // The resolver runs on the MDM host itself: show the host this console was
                // opened on, not the DEV address that used to be written here (wrong on PROD).
                address: $window.location.hostname + ':53'
            },
            {
                id: 'qr-enroll',
                icon: 'qr-code',
                titleKey: 'integrations.point.qr.title',
                descKey: 'integrations.point.qr.desc',
                address: 'rest/public/qr/{id}'
            },
            {
                id: 'device-sync',
                icon: 'refresh-cw',
                titleKey: 'integrations.point.sync.title',
                descKey: 'integrations.point.sync.desc',
                address: 'rest/public/sync/info'
            }
        ];

        $scope.copiedId = null;
        $scope.copy = function (point) {
            var text = point.address;
            var ok = false;
            try {
                if ($window.navigator.clipboard && $window.navigator.clipboard.writeText) {
                    $q.when($window.navigator.clipboard.writeText(text)).then(function () {
                        $scope.copiedId = point.id;
                    }, function () {
                        $scope.copiedId = null;
                        $scope.copyError = localization.localize('error.request.failure');
                    });
                    return;
                } else {
                    var el = $window.document.createElement('textarea');
                    el.value = text;
                    el.style.position = 'fixed';
                    el.style.opacity = '0';
                    $window.document.body.appendChild(el);
                    el.select();
                    ok = $window.document.execCommand('copy');
                    $window.document.body.removeChild(el);
                }
            } catch (e) {
                ok = false;
            }
            $scope.copiedId = ok ? point.id : null;
        };
    });
