// Localization completed
angular.module('headwind-kiosk')
    .controller('ReportsTabController', function ($scope, $window, deviceService, localization) {
        var MAX_DEVICES = 5000;
        var MAX_APP_ROWS = 20;

        $scope.loading = true;
        $scope.errorMessage = null;
        $scope.devices = [];
        $scope.deviceCount = 0;
        $scope.generatedAt = null;
        $scope.osBreakdown = [];
        $scope.modelBreakdown = [];
        $scope.statusBreakdown = [];
        $scope.batteryBreakdown = [];
        $scope.storageBreakdown = [];
        $scope.appBreakdown = [];

        var statusLabel = function (device) {
            switch (device.statusCode) {
                case 'green':
                    return localization.localize('summary.devices.active');
                case 'yellow':
                    return localization.localize('summary.devices.idle');
                case 'red':
                    return localization.localize('summary.devices.offline');
                default:
                    return localization.localize('devices.unknown');
            }
        };

        var bucketize = function (items, keyFn) {
            var counts = {};
            var total = 0;
            items.forEach(function (item) {
                var key = keyFn(item);
                if (!key) {
                    return;
                }
                counts[key] = (counts[key] || 0) + 1;
                total++;
            });
            return Object.keys(counts)
                .map(function (key) {
                    return {
                        label: key,
                        value: counts[key],
                        percent: total > 0 ? Math.round((counts[key] / total) * 100) : 0
                    };
                })
                .sort(function (a, b) {
                    return b.value - a.value;
                });
        };

        var buildReport = function (devices) {
            $scope.statusBreakdown = bucketize(devices, statusLabel);

            $scope.osBreakdown = bucketize(devices, function (device) {
                var version = device.info && device.info.androidVersion;
                return version ? ('Android ' + version) : null;
            });

            $scope.modelBreakdown = bucketize(devices, function (device) {
                return (device.info && device.info.model) || null;
            });

            $scope.batteryBreakdown = bucketize(devices, function (device) {
                var level = device.info && device.info.batteryLevel;
                if (level === undefined || level === null) {
                    return null;
                }
                if (level <= 20) {
                    return 'Low (≤20%)';
                }
                if (level <= 50) {
                    return 'Medium (21-50%)';
                }
                return 'Good (51-100%)';
            });

            $scope.storageBreakdown = bucketize(devices, function (device) {
                var percent = device.info && device.info.storageAvailablePercent;
                if (percent === undefined || percent === null) {
                    return null;
                }
                if (percent <= 10) {
                    return 'Critical (≤10%)';
                }
                if (percent <= 25) {
                    return 'Low (11-25%)';
                }
                return 'Healthy (>25%)';
            });

            var appCounts = {};
            devices.forEach(function (device) {
                var apps = device.info && device.info.applications;
                if (!apps) {
                    return;
                }
                apps.forEach(function (app) {
                    var name = app.name || app.pkg;
                    if (!name) {
                        return;
                    }
                    appCounts[name] = (appCounts[name] || 0) + 1;
                });
            });
            $scope.appBreakdown = Object.keys(appCounts)
                .map(function (name) {
                    return {
                        label: name,
                        value: appCounts[name],
                        percent: devices.length > 0 ? Math.round((appCounts[name] / devices.length) * 100) : 0
                    };
                })
                .sort(function (a, b) {
                    return b.value - a.value;
                })
                .slice(0, MAX_APP_ROWS);
        };

        var loadDevices = function () {
            $scope.loading = true;
            $scope.errorMessage = null;
            deviceService.getAllDevices({
                value: '',
                pageNum: 1,
                pageSize: MAX_DEVICES,
                sortBy: null,
                sortDir: 'ASC'
            }, function (response) {
                $scope.loading = false;
                if (response.status === 'OK' && response.data && response.data.devices && response.data.devices.items) {
                    $scope.devices = response.data.devices.items;
                    $scope.deviceCount = $scope.devices.length;
                    $scope.generatedAt = new Date();
                    buildReport($scope.devices);
                } else {
                    $scope.errorMessage = localization.localize('error.internal.server');
                }
            }, function () {
                $scope.loading = false;
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        var csvEscape = function (value) {
            var str = value === undefined || value === null ? '' : String(value);
            if (/[",\n]/.test(str)) {
                str = '"' + str.replace(/"/g, '""') + '"';
            }
            return str;
        };

        $scope.exportCsv = function () {
            var header = ['Device number', 'Model', 'Android version', 'Status', 'Battery %',
                'Storage available %', 'Configuration', 'Group', 'Last update'];
            var rows = [header.join(',')];
            $scope.devices.forEach(function (device) {
                var info = device.info || {};
                var groups = (device.groups || []).map(function (group) {
                    return group.name;
                }).join('; ');
                rows.push([
                    csvEscape(device.number),
                    csvEscape(info.model),
                    csvEscape(info.androidVersion),
                    csvEscape(statusLabel(device)),
                    csvEscape(info.batteryLevel),
                    csvEscape(info.storageAvailablePercent),
                    csvEscape(device.configuration && device.configuration.name),
                    csvEscape(groups),
                    csvEscape(device.lastUpdateDate ? new Date(device.lastUpdateDate).toISOString() : '')
                ].join(','));
            });

            var csvContent = rows.join('\r\n');
            var blob = new Blob([csvContent], {type: 'text/csv;charset=utf-8;'});
            var url = $window.URL.createObjectURL(blob);
            var link = $window.document.createElement('a');
            link.href = url;
            link.download = 'hwmdm-devices-report-' + new Date().toISOString().slice(0, 10) + '.csv';
            $window.document.body.appendChild(link);
            link.click();
            $window.document.body.removeChild(link);
            $window.URL.revokeObjectURL(url);
        };

        $scope.refresh = loadDevices;

        loadDevices();
    });
