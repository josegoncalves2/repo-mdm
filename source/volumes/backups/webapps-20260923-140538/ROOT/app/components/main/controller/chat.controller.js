// Localization completed
angular.module('headwind-kiosk')
    .controller('ChatTabController', function ($scope, $window, $timeout, $interval, localization, chatService,
                                                deviceService, authService) {
        // The server rejects the send unless the caller holds plugin_messaging_send, so
        // offering the button to anyone with edit_devices only produced a denied request.
        $scope.canSendChat = authService.hasPermission('plugin_messaging_send');
        $scope.canViewChat = $scope.canSendChat ||
            authService.hasPermission('plugin_messaging_delete') ||
            authService.hasPermission('device.remote_access.control') ||
            authService.hasPermission('edit_devices');
        $scope.loading = false;
        $scope.loadingDevices = false;
        $scope.devices = [];
        $scope.sending = false;
        $scope.errorMessage = undefined;
        $scope.successMessage = undefined;
        $scope.messages = [];
        $scope.dateFormat = localization.localize('format.date.plugin.messaging.createTime') || 'dd/MM/yyyy HH:mm:ss';
        $scope.chat = {
            deviceNumber: '',
            text: ''
        };
        $scope.paging = {
            pageNum: 1,
            pageSize: 50,
            totalItems: 0,
            deviceFilter: '',
            messageFilter: '',
            status: -1,
            sortValue: 'createTime'
        };

        // The history filter is a free-text field and an operator may still paste a
        // "number / imei" label into it, so the number is taken from before the slash.
        $scope.deviceLookupFormatter = function (value) {
            if (value) {
                var pos = value.indexOf('/');
                if (pos > -1) {
                    return value.substr(0, pos).trim();
                }
            }
            return value;
        };

        $scope.deviceLabel = function (device) {
            if (!device) {
                return '';
            }
            var extra = device.imei || (device.info && device.info.imei) || device.serial || '';
            return device.number + (extra ? ' / ' + extra : '');
        };

        var loadDevices = function () {
            $scope.loadingDevices = true;
            deviceService.getAllDevices({
                value: '',
                pageNum: 1,
                pageSize: 1000,
                sortBy: null,
                sortDir: 'ASC'
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

        var setTransientSuccess = function (key) {
            $scope.successMessage = localization.localize(key);
            $timeout(function () {
                $scope.successMessage = undefined;
            }, 5000);
        };

        $scope.statusLabel = function (status) {
            switch (status) {
                case 0:
                    return localization.localize('plugin.messaging.status.sent');
                case 1:
                    return localization.localize('plugin.messaging.status.delivered');
                case 2:
                    return localization.localize('plugin.messaging.status.read');
                default:
                    return localization.localize('devices.unknown');
            }
        };

        $scope.search = function () {
            $scope.paging.pageNum = 1;
            $scope.loadMessages();
        };

        $scope.loadMessages = function () {
            if (!$scope.canViewChat || $scope.loading) {
                return;
            }

            $scope.loading = true;
            $scope.errorMessage = undefined;
            var request = angular.copy($scope.paging);
            request.deviceFilter = $scope.deviceLookupFormatter(request.deviceFilter || $scope.chat.deviceNumber);

            chatService.getMessages(request, function (response) {
                $scope.loading = false;
                if (response.status === 'OK') {
                    $scope.messages = response.data.items || [];
                    $scope.paging.totalItems = response.data.totalItemsCount || 0;
                    if (!$scope.chat.deviceNumber && request.deviceFilter) {
                        $scope.chat.deviceNumber = request.deviceFilter;
                    }
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
            if (!$scope.canSendChat) {
                $scope.errorMessage = localization.localize('chat.no.permission');
                return;
            }

            var deviceNumber = $scope.deviceLookupFormatter($scope.chat.deviceNumber || '').trim();
            var text = ($scope.chat.text || '').trim();
            if (!deviceNumber) {
                $scope.errorMessage = localization.localize('plugin.messaging.error.empty.device');
                return;
            }
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
                scope: 'device',
                deviceNumber: deviceNumber,
                message: text
            }, function (response) {
                $scope.sending = false;
                if (response.status === 'OK') {
                    $scope.chat.deviceNumber = deviceNumber;
                    $scope.chat.text = '';
                    $scope.paging.deviceFilter = deviceNumber;
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

        $scope.$watch('paging.pageNum', function () {
            $window.scrollTo(0, 0);
            $scope.loadMessages();
        });

        loadDevices();
        $scope.loadMessages();

        var refreshInterval = $interval($scope.loadMessages, 15000);
        $scope.$on('$destroy', function () {
            if (refreshInterval) {
                $interval.cancel(refreshInterval);
            }
        });
    });
