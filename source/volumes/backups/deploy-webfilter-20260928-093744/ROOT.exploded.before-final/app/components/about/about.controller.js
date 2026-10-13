// Localization completed
angular.module('headwind-kiosk')
    .controller('AboutController', function ($scope, $rootScope, $uibModalInstance, APP_VERSION, localization,
                                             pluginService, rebranding, $http) {

        rebranding.query(function(value) {
            $scope.line1Text = localization.localize('about.line.1').replace('${appName}', value.appName);
        });
        $scope.line3Text = localization.localize('about.line.3').replace('${versionNumber}', APP_VERSION);

        // Bloco de versao da PLATAFORMA HWMDM (pacote de trabalho "versao"),
        // adicional as linhas acima -- que continuam mostrando a versao do
        // nucleo Headwind MDM de base (APP_VERSION, 5.39.2) como sempre
        // mostraram. Reaproveita o mesmo build-info.json que o rodape do
        // menu lateral le, sem cache (o build muda a cada publicacao no DEV).
        $scope.hwmdmInfo = null;
        $http.get('build-info.json?_=' + new Date().getTime(), { cache: false })
            .then(function (resposta) {
                $scope.hwmdmInfo = resposta.data && resposta.data.versao ? resposta.data : null;
            }, function () {
                $scope.hwmdmInfo = null;
            });

        pluginService.getAvailablePlugins(function (response) {
            if (response.status === 'OK') {
                $scope.plugins = response.data.map(function (plugin) {
                    return localization.localize(plugin.nameLocalizationKey)
                }).sort();
                $scope.pluginList = $scope.plugins.join(', ');
            }
        });

        var listener = $scope.$on('aero_USER_LOGOUT', $uibModalInstance.dismiss);
        $scope.$on("$destroy", listener);
        
        $scope.closeModal = function () {
            $uibModalInstance.dismiss();
        }
    });
