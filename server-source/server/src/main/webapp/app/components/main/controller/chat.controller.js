// Localization completed
angular.module('headwind-kiosk')
    .controller('ChatTabController', function ($scope, $window, $timeout, $interval, localization, chatService,
                                                authService) {
        $scope.canSendChat = authService.hasPermission('plugin_messaging_send') ||
            authService.hasPermission('device.remote_access.control') ||
            authService.hasPermission('edit_devices');
        $scope.canViewChat = $scope.canSendChat || authService.hasPermission('plugin_messaging_delete');
        $scope.loading = false;
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

        var lookupDeviceInfo = function (device) {
            if (!device || !device.info) {
                return undefined;
            }
            try {
                return JSON.parse(device.info);
            } catch (e) {
                return undefined;
            }
        };

        var resolveDeviceField = function (serverData, deviceInfoData) {
            serverData = serverData || '';
            deviceInfoData = deviceInfoData || '';
            if (serverData === deviceInfoData) {
                return serverData;
            }
            if (!serverData && deviceInfoData) {
                return deviceInfoData;
            }
            return serverData || deviceInfoData;
        };

        $scope.deviceLookupFormatter = function (value) {
            if (value) {
                var pos = value.indexOf('/');
                if (pos > -1) {
                    return value.substr(0, pos).trim();
                }
            }
            return value;
        };

        $scope.getDevices = function (value) {
            return chatService.lookupDevices(value).$promise.then(function (response) {
                if (response.status !== 'OK') {
                    return [];
                }
                return (response.data || []).map(function (device) {
                    var deviceInfo = lookupDeviceInfo(device);
                    var serverIMEI = device.imei || '';
                    var deviceInfoIMEI = deviceInfo ? (deviceInfo.imei || '') : '';
                    var resolvedIMEI = resolveDeviceField(serverIMEI, deviceInfoIMEI);
                    return device.name + (resolvedIMEI.length > 0 ? ' / ' + resolvedIMEI : '');
                });
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

        $scope.loadMessages();

        var refreshInterval = $interval($scope.loadMessages, 15000);
        $scope.$on('$destroy', function () {
            if (refreshInterval) {
                $interval.cancel(refreshInterval);
            }
        });
    });
