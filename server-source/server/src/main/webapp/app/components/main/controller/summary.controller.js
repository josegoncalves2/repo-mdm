// Localization completed
angular.module('headwind-kiosk')
    .controller('SummaryTabController', function ($scope, $state, $timeout, localization, summaryService, externalLibLoader, HWMDMMap) {
        var mapInstance = null;
        var tileServerUrl = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';

        $scope.stat = {};
        $scope.errorMessage = undefined;
        $scope.dashboardCards = [];
        $scope.recentDevices = [];
        $scope.locatedDevices = [];
        $scope.alerts = [];
        $scope.mapReady = false;
        $scope.mapError = false;
        $scope.dateFormat = localization.localize('devices.date.format');

        $scope.enrollmentLabels = [
            localization.localize('summary.devices.enrolled.earlier'),
            localization.localize('summary.devices.enrolled.monthly')
        ];
        $scope.enrollmentColors = ['#A3A3A3', '#2F80ED'];

        $scope.statusLabels = [
            localization.localize('summary.devices.offline'),
            localization.localize('summary.devices.idle'),
            localization.localize('summary.devices.active')
        ];
        $scope.statusColors = ['#D64545', '#D98634', '#1F8A70'];

        $scope.installLabels = [
            localization.localize('summary.devices.installation.failed'),
            localization.localize('summary.devices.version.mismatch'),
            localization.localize('summary.devices.installation.completed')
        ];
        $scope.installColors = ['#D64545', '#D98634', '#1F8A70'];

        $scope.monthlyEnrollColors = [];
        for (var i = 0; i < 12; i++) {
            $scope.monthlyEnrollColors.push('#2F80ED');
        }

        $scope.statusByConfigSeries = [
            localization.localize('summary.devices.offline'),
            localization.localize('summary.devices.idle'),
            localization.localize('summary.devices.active')
        ];
        $scope.statusByConfigColors = $scope.statusColors;

        $scope.installByConfigSeries = [
            localization.localize('summary.devices.installation.failed'),
            localization.localize('summary.devices.version.mismatch'),
            localization.localize('summary.devices.installation.completed')
        ];
        $scope.installByConfigColors = $scope.installColors;

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
            var info = device.info || {};
            var ip = info.deviceIp || info.ip || info.localIp || null;
            return ip && !infrastructureIps[String(ip).trim()] ? ip : null;
        };

        $scope.getDeviceIpDisplay = function (device) {
            return reportedDeviceIp(device) || localization.localize('devices.ip.not.reported');
        };

        var popupTemplate = function (device) {
            var title = escapeHtml(device.number || localization.localize('devices.unknown'));
            var model = escapeHtml(device.model || localization.localize('devices.model.unknown'));
            var ip = escapeHtml($scope.getDeviceIpDisplay(device));
            return '<div class="summary-map-popup">' +
                '<strong>' + title + '</strong><br>' +
                '<span>' + model + '</span><br>' +
                '<span>IP: ' + ip + '</span>' +
                '</div>';
        };

        var addSummaryMarkers = function () {
            if (!$scope.locatedDevices.length || !mapInstance) {
                return;
            }
            $scope.locatedDevices.forEach(function (device) {
                mapInstance.addMarker(
                    'device-' + device.id,
                    device.lat,
                    device.lon,
                    markerIcon(device.statusCode),
                    device.number,
                    popupTemplate(device)
                );
            });

            if ($scope.locatedDevices.length === 1) {
                mapInstance.centerMap($scope.locatedDevices[0].lat, $scope.locatedDevices[0].lon);
            } else {
                mapInstance.fitBoundsToMarkers();
            }
            mapInstance.invalidateSize();
            $scope.mapReady = true;
        };

        var initMap = function () {
            if (!$scope.locatedDevices.length || mapInstance) {
                return;
            }
            externalLibLoader.getLoader('leaflet').load().then(function () {
                $timeout(function () {
                    try {
                        mapInstance = HWMDMMap.get();
                        mapInstance.initMap($scope, 'summary-map', tileServerUrl);
                        addSummaryMarkers();
                    } catch (e) {
                        $scope.mapError = true;
                    }
                }, 0);
            });
        };

        var getCountByAttr = function (items, attr) {
            var value = 0;
            (items || []).forEach(function (item) {
                if (item.stringAttr === attr) {
                    value = item.number;
                }
            });
            return value;
        };

        var prepareCards = function (data) {
            var active = getCountByAttr(data.statusSummary, 'green');
            var idle = getCountByAttr(data.statusSummary, 'yellow');
            var offline = getCountByAttr(data.statusSummary, 'red');
            var failures = getCountByAttr(data.installSummary, 'FAILURE');
            var alertTotal = (data.criticalAlerts || 0) + (data.warningAlerts || 0);
            $scope.dashboardCards = [
                {
                    label: localization.localize('summary.devices.enrolled.total'),
                    value: data.devicesEnrolled || 0,
                    detail: localization.localize('summary.devices.enrolled.monthly') + ': ' + (data.devicesEnrolledLastMonth || 0),
                    tone: 'blue',
                    targetState: 'main'
                },
                {
                    label: localization.localize('summary.devices.active'),
                    value: active,
                    detail: localization.localize('summary.devices.idle') + ': ' + idle,
                    tone: 'green',
                    targetState: 'main'
                },
                {
                    label: localization.localize('summary.devices.offline'),
                    value: offline,
                    detail: localization.localize('summary.devices.installation.failed') + ': ' + failures,
                    tone: 'red',
                    targetState: 'main'
                },
                {
                    label: 'GPS visible',
                    value: data.devicesWithLocation || 0,
                    detail: 'Last locations: ' + (data.locatedDevices || []).length,
                    tone: 'amber',
                    targetState: 'gpsMap'
                },
                {
                    label: 'Operational alerts',
                    value: alertTotal,
                    detail: localization.localize('summary.alerts.critical') + ': ' + (data.criticalAlerts || 0)
                        + ' / ' + localization.localize('summary.alerts.warning') + ': ' + (data.warningAlerts || 0),
                    tone: alertTotal > 0 ? 'red' : 'green',
                    targetState: 'main'
                }
            ];
        };

        $scope.openDashboardCard = function (card) {
            if (card && card.targetState) {
                $state.go(card.targetState);
            }
        };

        $scope.getDeviceStatusClass = function (device) {
            return 'summary-status-' + (device.statusCode || 'grey');
        };

        $scope.getDeviceMode = function (value) {
            if (value === true) {
                return localization.localize('form.selection.status.yes');
            }
            if (value === false) {
                return localization.localize('form.selection.status.no');
            }
            return localization.localize('devices.unknown');
        };

        $scope.getAlertClass = function (alert) {
            return 'summary-alert-' + (alert.severity || 'warning');
        };

        $scope.getAlertLabel = function (alert) {
            return localization.localize('summary.alert.' + alert.type);
        };

        summaryService.getDeviceStat(function (response) {
            var data = response.data || {};
            $scope.stat = data;

            var devicesEnrolledEarlier = (data.devicesEnrolled || 0) - (data.devicesEnrolledLastMonth || 0);
            if (devicesEnrolledEarlier < 0) {
                devicesEnrolledEarlier = 0;
            }
            $scope.enrollmentData = [devicesEnrolledEarlier, data.devicesEnrolledLastMonth || 0];
            $scope.statusData = [
                getCountByAttr(data.statusSummary, 'red'),
                getCountByAttr(data.statusSummary, 'yellow'),
                getCountByAttr(data.statusSummary, 'green')
            ];
            $scope.installData = [
                getCountByAttr(data.installSummary, 'FAILURE'),
                getCountByAttr(data.installSummary, 'VERSION_MISMATCH'),
                getCountByAttr(data.installSummary, 'SUCCESS')
            ];

            $scope.statusByConfigLabels = data.topConfigs || [];
            $scope.statusByConfigData = [
                data.statusOfflineByConfig || [],
                data.statusIdleByConfig || [],
                data.statusOnlineByConfig || []
            ];

            $scope.installByConfigLabels = $scope.statusByConfigLabels;
            $scope.installByConfigData = [
                data.appFailureByConfig || [],
                data.appMismatchByConfig || [],
                data.appSuccessByConfig || []
            ];

            $scope.monthlyEnrollLabels = [];
            $scope.monthlyEnrollData = [];
            (data.devicesEnrolledMonthly || []).forEach(function (item) {
                $scope.monthlyEnrollLabels.push(item.stringAttr);
                $scope.monthlyEnrollData.push(item.number);
            });

            $scope.recentDevices = data.recentDevices || [];
            $scope.locatedDevices = data.locatedDevices || [];
            $scope.alerts = data.alerts || [];
            prepareCards(data);
            $timeout(initMap, 0);
        }, function () {
            $scope.errorMessage = localization.localize('error.internal.server');
        });
    });
