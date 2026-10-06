// Localization completed
angular.module('headwind-kiosk')
    .controller('ChatTabController', function ($scope, $window, $timeout, $interval, localization, chatService,
                                                deviceService, authService) {
        $scope.canSendChat = authService.hasPermission('plugin_messaging_send');
        $scope.canViewChat = $scope.canSendChat ||
            authService.hasPermission('plugin_messaging_delete') ||
            authService.hasPermission('device.remote_access.control') ||
            authService.hasPermission('edit_devices');
        $scope.loading = false;
        $scope.loadingDevices = false;
        $scope.devices = [];
        $scope.selectedDevice = null;
        $scope.deviceSearch = '';
        $scope.sending = false;
        $scope.errorMessage = undefined;
        $scope.successMessage = undefined;
        $scope.messages = [];
        $scope.unreadCount = 0;
        $scope.dateFormat = localization.localize('format.date.plugin.messaging.createTime') || 'dd/MM/yyyy HH:mm:ss';
        $scope.chat = { text: '' };
        $scope.paging = {
            pageNum: 1,
            pageSize: 50,
            totalItems: 0,
            deviceFilter: '',
            messageFilter: '',
            status: -1,
            sortValue: 'createTime'
        };

        $scope.deviceSearchFn = function (device) {
            if (!$scope.deviceSearch) return true;
            var q = $scope.deviceSearch.toLowerCase();
            return (device.number && device.number.toLowerCase().indexOf(q) >= 0) ||
                   (device.model && device.model.toLowerCase().indexOf(q) >= 0) ||
                   (device.imei && device.imei.toLowerCase().indexOf(q) >= 0);
        };

        $scope.deviceLabel = function (device) {
            if (!device) return '';
            var extra = device.imei || (device.info && device.info.imei) || device.serial || '';
            return device.number + (extra ? ' / ' + extra : '');
        };

        $scope.loadDevices = function (userRequested) {
            $scope.loadingDevices = true;
            deviceService.getAllDevices({
                value: '', pageNum: 1, pageSize: 1000, sortBy: null, sortDir: 'ASC'
            }, function (response) {
                $scope.loadingDevices = false;
                if (response.status === 'OK' && response.data && response.data.devices) {
                    $scope.devices = response.data.devices.items || [];
                } else {
                    $scope.devices = [];
                }
            }, function () {
                $scope.loadingDevices = false;
                $scope.devices = [];
            });
        };

        $scope.selectDevice = function (device) {
            $scope.selectedDevice = device;
            $scope.paging.deviceFilter = device.number;
            $scope.paging.pageNum = 1;
            $scope.chat.text = '';
            $scope.loadMessages();
        };

        $scope.clearSelection = function () {
            $scope.selectedDevice = null;
            $scope.paging.deviceFilter = '';
            $scope.messages = [];
        };

        var setTransientSuccess = function (key) {
            $scope.successMessage = localization.localize(key);
            $timeout(function () { $scope.successMessage = undefined; }, 5000);
        };

        $scope.statusLabel = function (status) {
            switch (status) {
                case 0: return localization.localize('plugin.messaging.status.sent');
                case 1: return localization.localize('plugin.messaging.status.delivered');
                case 2: return localization.localize('plugin.messaging.status.read');
                default: return localization.localize('devices.unknown');
            }
        };

        $scope.loadMessages = function () {
            if (!$scope.canViewChat || $scope.loading || !$scope.selectedDevice) return;
            $scope.loading = true;
            $scope.errorMessage = undefined;
            var request = angular.copy($scope.paging);
            chatService.getMessages(request, function (response) {
                $scope.loading = false;
                if (response.status === 'OK') {
                    $scope.messages = response.data.items || [];
                    $scope.paging.totalItems = response.data.totalItemsCount || 0;
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
            }, function () {
                $scope.loading = false;
                $scope.errorMessage = localization.localize('chat.error.load.failed');
            });
        };

        $scope.send = function () {
            $scope.errorMessage = undefined;
            if (!$scope.canSendChat || !$scope.selectedDevice) return;
            var deviceNumber = $scope.selectedDevice.number;
            var text = ($scope.chat.text || '').trim();
            if (!text) {
                $scope.errorMessage = localization.localize('plugin.messaging.error.empty.text');
                return;
            }
            if (text.length > 5000) {
                $scope.errorMessage = localization.localize('chat.error.message.too.long');
                return;
            }
            $scope.sending = true;
            chatService.sendMessage({
                scope: 'device', deviceNumber: deviceNumber, message: text
            }, function (response) {
                $scope.sending = false;
                if (response.status === 'OK') {
                    $scope.chat.text = '';
                    setTransientSuccess('plugin.messaging.send.success');
                    $scope.loadMessages();
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
            }, function () {
                $scope.sending = false;
                $scope.errorMessage = localization.localize('chat.error.send.failed');
            });
        };

        $scope.$watch('paging.pageNum', function (newVal, oldVal) {
            if (newVal !== oldVal && $scope.selectedDevice) {
                $window.scrollTo(0, 0);
                $scope.loadMessages();
            }
        });

        $scope.loadDevices();

        var refreshInterval = $interval(function () {
            if ($scope.selectedDevice) $scope.loadMessages();
        }, 15000);
        $scope.$on('$destroy', function () { if (refreshInterval) $interval.cancel(refreshInterval); });
    });
