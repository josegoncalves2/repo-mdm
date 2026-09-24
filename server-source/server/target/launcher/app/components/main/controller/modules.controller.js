/*
 * HWMDM 1.0 - tela "Modulos" (pacote de trabalho "modulos", item 5 do pedido).
 *
 * Substitui as tres grades repetidas de app/components/main/view/settings/extensionsHub.html
 * (Device actions / Module settings / Module maintenance, que mostravam os mesmos seis
 * plugins tres vezes) por UMA lista, agrupada pelas mesmas secoes do menu lateral, uma
 * linha por modulo. O catalogo (o que existe, onde entra, se pode ser desligado) vem inteiro
 * de moduleRegistry; este controller so busca o estado, reage ao clique e pede o motivo
 * quando o usuario desliga um modulo.
 */
angular.module('headwind-kiosk')
    .controller('ModulesTabController', function ($scope, $rootScope, $q, $uibModal,
                                                   moduleRegistry, localization, alertService) {

        $scope.localization = localization;
        $scope.loading = true;
        $scope.sections = [];

        var refresh = function () {
            $scope.sections = moduleRegistry.allSections();
            $scope.canManage = moduleRegistry.canManage();
            $scope.nativeStateAvailable = moduleRegistry.nativeStateAvailable();
        };

        moduleRegistry.load().then(function () {
            $scope.loading = false;
            refresh();
        });

        $scope.typeLabel = function (m) {
            return m.type === 'extension' ? 'modules.type.extension' : 'modules.type.native';
        };

        $scope.isInMaintenance = function (id) { return moduleRegistry.isInMaintenance(id); };
        $scope.maintenanceInfo = function (id) { return moduleRegistry.maintenanceInfo(id); };
        $scope.isEssential = function (id) { return moduleRegistry.isEssential(id); };
        $scope.canToggle = function (id) { return moduleRegistry.canToggle(id); };
        $scope.settingsTarget = function (id) { return moduleRegistry.settingsTarget(id); };

        // Motivo do desligamento: pedido explicitamente no item 5 ("interruptor que pede o
        // motivo num modal ao desligar"). So pede ao desligar - religar nao precisa de motivo.
        var promptForReason = function () {
            var deferred = $q.defer();
            var modalInstance = $uibModal.open({
                template:
                    '<div class="modal-header"><h4 class="modal-title">' +
                    '<span localized>modules.maintenance.modal.title</span></h4></div>' +
                    '<div class="modal-body">' +
                    '<p><span localized>modules.maintenance.modal.body</span></p>' +
                    '<textarea class="form-control" ng-model="reason" rows="3" ' +
                    'localized-placeholder="modules.maintenance.modal.placeholder" autofocus></textarea>' +
                    '</div>' +
                    '<div class="modal-footer">' +
                    '<button type="button" class="btn btn-default" ng-click="cancel()"><span localized>button.cancel</span></button> ' +
                    '<button type="button" class="btn btn-danger" ng-click="ok()"><span localized>modules.maintenance.modal.confirm</span></button>' +
                    '</div>',
                controller: function ($scope, $uibModalInstance) {
                    $scope.reason = '';
                    $scope.ok = function () { $uibModalInstance.close($scope.reason); };
                    $scope.cancel = function () { $uibModalInstance.dismiss(); };
                }
            });
            modalInstance.result.then(function (reason) {
                deferred.resolve(reason);
            }, function () {
                deferred.reject('cancelled');
            });
            return deferred.promise;
        };

        var applyToggle = function (id, disabled, reason) {
            $scope.loading = true;
            moduleRegistry.toggle(id, disabled, reason).then(function () {
                $scope.loading = false;
                refresh();
            }, function (err) {
                $scope.loading = false;
                refresh();
                var message = (err && err.message) ? localization.localizeServerResponse(err)
                    : localization.localize('error.request.failure');
                alertService.showAlertMessage(message);
            });
        };

        $scope.toggle = function (m) {
            if (!$scope.canToggle(m.id)) return;
            var turningOff = !moduleRegistry.isInMaintenance(m.id);
            if (turningOff) {
                promptForReason().then(function (reason) {
                    applyToggle(m.id, true, reason);
                }, function () { /* cancelado: nao muda nada */ });
            } else {
                applyToggle(m.id, false, null);
            }
        };

        var listener1 = $rootScope.$on('aero_MODULES_UPDATED', refresh);
        var listener2 = $rootScope.$on('aero_PLUGINS_UPDATED', refresh);
        $scope.$on('$destroy', listener1);
        $scope.$on('$destroy', listener2);
    });
