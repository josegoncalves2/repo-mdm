// Localization completed
angular.module('headwind-kiosk')
    .controller('GpsMapTabController', function ($scope, $timeout, $window, localization, summaryService, groupService,
                                                 externalLibLoader, HWMDMMap, alertService, deviceFocusService) {
        var mapInstance = null;
        var tileServerUrl = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
        var REFRESH_INTERVAL_STORAGE_KEY = 'hwmdm.gps.refreshIntervalMs';
        var DEFAULT_REFRESH_INTERVAL_MS = 30000;
        var refreshTimer = null;
        var focusedDevice = deviceFocusService.consume();
        var firstMapFitDone = false;

        $scope.devices = [];
        $scope.groups = [];
        $scope.selectedGroupId = null;
        $scope.selectedDeviceId = null;
        $scope.loading = false;
        $scope.errorMessage = null;
        $scope.mapReady = false;
        $scope.mapError = false;
        $scope.lastUpdated = null;
        $scope.refreshOptions = [
            {value: 30000, label: localization.localize('gpsmap.refresh.30s')},
            {value: 60000, label: localization.localize('gpsmap.refresh.60s')},
            {value: 0, label: localization.localize('gpsmap.refresh.off')}
        ];

        var storedInterval = parseInt($window.localStorage.getItem(REFRESH_INTERVAL_STORAGE_KEY), 10);
        $scope.refreshIntervalMs = [0, 30000, 60000].indexOf(storedInterval) >= 0
            ? storedInterval
            : DEFAULT_REFRESH_INTERVAL_MS;

        var markerIcon = function (statusCode) {
            var color = statusCode || 'grey';
            if (['green', 'yellow', 'red', 'brown', 'grey'].indexOf(color) < 0) {
                color = 'grey';
            }
            return {
                iconUrl: 'images/circle-' + color + '.png',
                iconSize: [18, 18],
                iconAnchor: [9, 9],
                popupAnchor: [0, -9]
            };
        };

        var escapeHtml = function (value) {
            return String(value || '')
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        };

        var infrastructureIps = {
            '10.0.17.106': true,
            '10.0.9.1': true,
            '10.1.1.1': true
        };

        var reportedDeviceIp = function (device) {
            if (!device) {
                return null;
            }
            // publicIp e' a fonte real (o agente reporta o endereco do aparelho); os campos
            // de info ficam por compatibilidade -- o launcher 6.36 nao envia nenhum deles.
            var info = device.info || {};
            var ip = info.deviceIp || info.ip || info.localIp || device.publicIp || null;
            return ip && !infrastructureIps[String(ip).trim()] ? ip : null;
        };

        var popupTemplate = function (device) {
            var title = escapeHtml(device.number || localization.localize('devices.unknown'));
            var model = escapeHtml(device.model || localization.localize('devices.model.unknown'));
            var ip = escapeHtml(reportedDeviceIp(device) || localization.localize('devices.ip.not.reported'));
            var ts = device.locationTs ? new Date(device.locationTs).toLocaleString() : '-';
            return '<div class="summary-map-popup">' +
                '<strong>' + title + '</strong><br>' +
                '<span>' + model + '</span><br>' +
                '<span>IP: ' + ip + '</span><br>' +
                '<span>' + escapeHtml(localization.localize('gpsmap.popup.updated')) + ': ' + escapeHtml(ts) + '</span>' +
                '</div>';
        };

        var clearMarkers = function () {
            if (mapInstance) {
                mapInstance.removeAllMarkers();
            }
        };

        var addMarkers = function (fitMode) {
            if (!mapInstance) {
                return;
            }

            clearMarkers();
            $scope.devices.forEach(function (device) {
                mapInstance.addMarker(
                    'device-' + device.id,
                    device.lat,
                    device.lon,
                    markerIcon(device.statusCode),
                    device.number,
                    popupTemplate(device)
                );
            });

            if (focusedDevice) {
                var focusMatch = $scope.devices.find(function (device) {
                    return device.id === focusedDevice.id;
                });
                focusedDevice = null;
                if (focusMatch) {
                    $scope.selectedDeviceId = focusMatch.id;
                    mapInstance.centerMap(focusMatch.lat, focusMatch.lon);
                    mapInstance.openMarkerPopup('device-' + focusMatch.id);
                    mapInstance.invalidateSize();
                    return;
                }
                alertService.showAlertMessage(localization.localize('gpsmap.empty'));
            }

            if ($scope.selectedDeviceId) {
                var selectedDevice = $scope.devices.find(function (device) {
                    return device.id === $scope.selectedDeviceId;
                });
                if (selectedDevice) {
                    mapInstance.centerMap(selectedDevice.lat, selectedDevice.lon);
                    mapInstance.invalidateSize();
                    return;
                }
                $scope.selectedDeviceId = null;
            }

            if (fitMode === 'none') {
                mapInstance.invalidateSize();
                return;
            }

            if ($scope.devices.length === 1) {
                mapInstance.centerMap($scope.devices[0].lat, $scope.devices[0].lon);
                firstMapFitDone = true;
            } else if ($scope.devices.length > 1 && (!firstMapFitDone || fitMode === 'force')) {
                mapInstance.fitBoundsToMarkers();
                firstMapFitDone = true;
            }
            mapInstance.invalidateSize();
        };

        var ensureMap = function (fitMode) {
            if (mapInstance) {
                addMarkers(fitMode);
                return;
            }
            externalLibLoader.getLoader('leaflet').load().then(function () {
                $timeout(function () {
                    try {
                        mapInstance = HWMDMMap.get();
                        mapInstance.initMap($scope, 'gps-map', tileServerUrl);
                        $scope.mapReady = true;
                        addMarkers(fitMode);
                    } catch (e) {
                        $scope.mapError = true;
                    }
                }, 0);
            });
        };

        var cancelRefresh = function () {
            if (refreshTimer) {
                $timeout.cancel(refreshTimer);
                refreshTimer = null;
            }
        };

        var scheduleRefresh = function () {
            cancelRefresh();
            if (!$scope.refreshIntervalMs) {
                return;
            }
            refreshTimer = $timeout(function () {
                $scope.loadDevices(false);
            }, $scope.refreshIntervalMs);
        };

        $scope.loadDevices = function (userRequested) {
            if ($scope.loading) {
                return;
            }

            $scope.loading = true;
            $scope.errorMessage = null;
            var params = {};
            if ($scope.selectedGroupId) {
                params.groupId = $scope.selectedGroupId;
            }
            summaryService.getDeviceLocations(params, function (response) {
                $scope.loading = false;
                if (response.status === 'OK') {
                    $scope.devices = response.data || [];
                    $scope.lastUpdated = new Date();
                    ensureMap(userRequested ? 'force' : 'none');
                } else {
                    $scope.devices = [];
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
                scheduleRefresh();
            }, function () {
                $scope.loading = false;
                $scope.devices = [];
                $scope.errorMessage = localization.localize('gpsmap.error.load.failed');
                scheduleRefresh();
            });
        };

        $scope.fitAll = function () {
            if (mapInstance && $scope.devices.length) {
                mapInstance.fitBoundsToMarkers();
                firstMapFitDone = true;
            }
        };

        $scope.selectDevice = function (device) {
            $scope.selectedDeviceId = device.id;
            if (mapInstance) {
                mapInstance.centerMap(device.lat, device.lon);
                mapInstance.openMarkerPopup('device-' + device.id);
                mapInstance.invalidateSize();
            }
        };

        $scope.onGroupChanged = function () {
            $scope.selectedDeviceId = null;
            firstMapFitDone = false;
            $scope.loadDevices(true);
        };

        $scope.onRefreshIntervalChanged = function () {
            $window.localStorage.setItem(REFRESH_INTERVAL_STORAGE_KEY, String($scope.refreshIntervalMs || 0));
            scheduleRefresh();
        };

        var loadGroups = function () {
            groupService.getAllGroups(function (response) {
                $scope.groups = response.data || [];
                $scope.groups.unshift({id: null, name: localization.localize('gpsmap.all.groups')});
            });
        };

        $scope.$on('$destroy', function () {
            cancelRefresh();
        });

        loadGroups();
        $scope.loadDevices(true);
    });
