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
        $scope.selectedDevice = null;
        $scope.selectedDeviceGroups = '';
        $scope.selectedDeviceApps = [];
        $scope.deviceSearch = '';
        $scope.osBreakdown = [];
        $scope.modelBreakdown = [];
        $scope.statusBreakdown = [];
        $scope.batteryBreakdown = [];
        $scope.storageBreakdown = [];
        $scope.appBreakdown = [];

        $scope.deviceSearchFn = function (device) {
            if (!$scope.deviceSearch) return true;
            var q = $scope.deviceSearch.toLowerCase();
            return (device.number && device.number.toLowerCase().indexOf(q) >= 0) ||
                   (device.info && device.info.model && device.info.model.toLowerCase().indexOf(q) >= 0);
        };

        $scope.selectDevice = function (device) {
            $scope.selectedDevice = device;
            if (device) {
                $scope.selectedDeviceGroups = (device.groups || []).map(function (g) { return g.name; }).join(', ');
                $scope.selectedDeviceApps = (device.info && device.info.applications) || [];
            }
        };

        var statusLabel = function (device) {
            switch (device.statusCode) {
                case 'green': return localization.localize('summary.devices.active');
                case 'yellow': return localization.localize('summary.devices.idle');
                case 'red': return localization.localize('summary.devices.offline');
                default: return localization.localize('devices.unknown');
            }
        };

        var bucketize = function (items, keyFn) {
            var counts = {}, total = 0;
            items.forEach(function (item) {
                var key = keyFn(item);
                if (!key) return;
                counts[key] = (counts[key] || 0) + 1;
                total++;
            });
            return Object.keys(counts).map(function (key) {
                return { label: key, value: counts[key], percent: total > 0 ? Math.round((counts[key] / total) * 100) : 0 };
            }).sort(function (a, b) { return b.value - a.value; });
        };

        var buildReport = function (devices) {
            $scope.statusBreakdown = bucketize(devices, statusLabel);
            $scope.osBreakdown = bucketize(devices, function (d) {
                var v = d.info && d.info.androidVersion; return v ? ('Android ' + v) : null;
            });
            $scope.modelBreakdown = bucketize(devices, function (d) { return (d.info && d.info.model) || null; });
            $scope.batteryBreakdown = bucketize(devices, function (d) {
                var l = d.info && d.info.batteryLevel;
                if (l == null) return null;
                if (l <= 20) return 'Baixa (≤20%)';
                if (l <= 50) return 'Media (21-50%)';
                return 'Boa (51-100%)';
            });
            $scope.storageBreakdown = bucketize(devices, function (d) {
                var p = d.info && d.info.storageAvailablePercent;
                if (p == null) return null;
                if (p <= 10) return 'Critico (≤10%)';
                if (p <= 25) return 'Baixo (11-25%)';
                return 'Saudavel (>25%)';
            });
            var appCounts = {};
            devices.forEach(function (d) {
                var apps = d.info && d.info.applications;
                if (!apps) return;
                apps.forEach(function (app) {
                    var name = app.name || app.pkg;
                    if (!name) return;
                    appCounts[name] = (appCounts[name] || 0) + 1;
                });
            });
            $scope.appBreakdown = Object.keys(appCounts).map(function (name) {
                return { label: name, value: appCounts[name], percent: devices.length > 0 ? Math.round((appCounts[name] / devices.length) * 100) : 0 };
            }).sort(function (a, b) { return b.value - a.value; }).slice(0, MAX_APP_ROWS);
        };

        var loadDevices = function () {
            $scope.loading = true;
            $scope.errorMessage = null;
            deviceService.getAllDevices({
                value: '', pageNum: 1, pageSize: MAX_DEVICES, sortBy: null, sortDir: 'ASC'
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
            var str = value == null ? '' : String(value);
            if (/[",\n]/.test(str)) str = '"' + str.replace(/"/g, '""') + '"';
            return str;
        };

        $scope.exportCsv = function () {
            var header = ['Device number', 'Model', 'Android version', 'Status', 'Battery %',
                'Storage available %', 'Configuration', 'Group', 'Last update'];
            var rows = [header.join(',')];
            var src = $scope.selectedDevice ? [$scope.selectedDevice] : $scope.devices;
            src.forEach(function (device) {
                var info = device.info || {};
                var groups = (device.groups || []).map(function (g) { return g.name; }).join('; ');
                rows.push([
                    csvEscape(device.number), csvEscape(info.model), csvEscape(info.androidVersion),
                    csvEscape(statusLabel(device)), csvEscape(info.batteryLevel),
                    csvEscape(info.storageAvailablePercent),
                    csvEscape(device.configuration && device.configuration.name), csvEscape(groups),
                    csvEscape(device.lastUpdateDate ? new Date(device.lastUpdateDate).toISOString() : '')
                ].join(','));
            });
            var blob = new Blob([rows.join('\r\n')], {type: 'text/csv;charset=utf-8;'});
            var url = $window.URL.createObjectURL(blob);
            var a = $window.document.createElement('a');
            a.href = url;
            a.download = 'hwmdm-report-' + new Date().toISOString().slice(0, 10) + '.csv';
            $window.document.body.appendChild(a); a.click();
            $window.document.body.removeChild(a);
            $window.URL.revokeObjectURL(url);
        };

        $scope.refresh = loadDevices;
        loadDevices();
    });
