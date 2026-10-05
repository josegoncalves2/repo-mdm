// Localization completed
angular.module('headwind-kiosk')
    .controller('GpsMapTabController', function ($scope, $timeout, $window, $http, localization, summaryService, groupService,
                                                 externalLibLoader, HWMDMMap, alertService, deviceFocusService) {
        var mapInstance = null;
        var tileServerUrl = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
        var REFRESH_INTERVAL_STORAGE_KEY = 'hwmdm.gps.refreshIntervalMs';
        var DEFAULT_REFRESH_INTERVAL_MS = 30000;
        var ONLINE_THRESHOLD_MS = 300000;
        var refreshTimer = null;
        var focusedDevice = deviceFocusService.consume();
        var firstMapFitDone = false;

        $scope.devices = [];
        $scope.groups = [];
        $scope.selectedGroupId = null;
        $scope.selectedDeviceId = null;
        $scope.selectedDevice = null;
        $scope.loading = false;
        $scope.errorMessage = null;
        $scope.mapReady = false;
        $scope.mapError = false;
        $scope.lastUpdated = null;
        $scope.deviceSearch = '';
        $scope.onlineCount = 0;
        $scope.offlineCount = 0;
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
            var info = device.info || {};
            var ip = info.deviceIp || info.ip || info.localIp || device.publicIp || null;
            return ip && !infrastructureIps[String(ip).trim()] ? ip : null;
        };

        var enrichDevice = function (device) {
            var now = Date.now();
            device.isOnline = device.locationTs && (now - device.locationTs) < ONLINE_THRESHOLD_MS;
            var info = device.info || {};
            device.batteryLevel = info.batteryLevel != null ? info.batteryLevel : null;
            device.networkType = info.wifi ? 'Wi-Fi' : (info.mobile ? info.networkType || '4G' : null);
            device.serial = info.serial || device.serial || null;
            device.deviceIp = reportedDeviceIp(device);
            device.speed = info.speed != null ? info.speed * 3.6 : null;
            device.alt = info.altitude || null;
        };

        var updateCounts = function () {
            var on = 0, off = 0;
            $scope.devices.forEach(function (d) {
                if (d.isOnline) on++; else off++;
            });
            $scope.onlineCount = on;
            $scope.offlineCount = off;
        };

        $scope.deviceSearchFilter = function (device) {
            if (!$scope.deviceSearch) return true;
            var q = $scope.deviceSearch.toLowerCase();
            return (device.number && device.number.toLowerCase().indexOf(q) >= 0) ||
                   (device.model && device.model.toLowerCase().indexOf(q) >= 0) ||
                   (device.serial && device.serial.toLowerCase().indexOf(q) >= 0);
        };

        var popupTemplate = function (device) {
            var title = escapeHtml(device.number || localization.localize('devices.unknown'));
            var model = escapeHtml(device.model || localization.localize('devices.model.unknown'));
            var ip = escapeHtml(device.deviceIp || localization.localize('devices.ip.not.reported'));
            var ts = device.locationTs ? new Date(device.locationTs).toLocaleString() : '-';
            var statusLabel = device.isOnline ? localization.localize('gpsmap.status.online') : localization.localize('gpsmap.status.offline');
            var statusColor = device.isOnline ? '#1f8a70' : '#d64545';
            var battery = device.batteryLevel != null ? '<br><span>&#9889; ' + device.batteryLevel + '%</span>' : '';
            return '<div class="summary-map-popup">' +
                '<strong>' + title + '</strong> <span style="color:' + statusColor + ';font-size:11px">' + escapeHtml(statusLabel) + '</span><br>' +
                '<span>' + model + '</span><br>' +
                '<span>IP: ' + ip + '</span><br>' +
                '<span>' + escapeHtml(localization.localize('gpsmap.popup.updated')) + ': ' + escapeHtml(ts) + '</span>' +
                battery +
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
                    markerIcon(device.isOnline ? 'green' : 'grey'),
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
                    $scope.selectedDevice = focusMatch;
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
                    $scope.selectedDevice = selectedDevice;
                    mapInstance.centerMap(selectedDevice.lat, selectedDevice.lon);
                    mapInstance.invalidateSize();
                    return;
                }
                $scope.selectedDeviceId = null;
                $scope.selectedDevice = null;
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
                    $scope.devices.forEach(enrichDevice);
                    updateCounts();
                    $scope.lastUpdated = new Date();
                    ensureMap(userRequested ? 'force' : 'none');
                } else {
                    $scope.devices = [];
                    $scope.errorMessage = localization.localizeServerResponse(response);
                    updateCounts();
                }
                scheduleRefresh();
            }, function () {
                $scope.loading = false;
                $scope.devices = [];
                $scope.errorMessage = localization.localize('gpsmap.error.load.failed');
                updateCounts();
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
            $scope.selectedDevice = device;
            if (mapInstance) {
                mapInstance.centerMap(device.lat, device.lon);
                mapInstance.openMarkerPopup('device-' + device.id);
                mapInstance.invalidateSize();
            }
        };

        $scope.clearSelection = function () {
            $scope.selectedDeviceId = null;
            $scope.selectedDevice = null;
            if ($scope.historyMode) {
                $scope.historyMode = false;
                $scope.clearHistory();
            }
        };

        $scope.centerOnDevice = function (device) {
            if (mapInstance && device) {
                mapInstance.centerMap(device.lat, device.lon);
                mapInstance.invalidateSize();
            }
        };

        $scope.onGroupChanged = function () {
            $scope.selectedDeviceId = null;
            $scope.selectedDevice = null;
            firstMapFitDone = false;
            $scope.loadDevices(true);
        };

        $scope.onRefreshIntervalChanged = function () {
            $window.localStorage.setItem(REFRESH_INTERVAL_STORAGE_KEY, String($scope.refreshIntervalMs || 0));
            scheduleRefresh();
        };

        $scope.historyMode = false;
        $scope.historyPeriod = '24';
        $scope.historyPoints = [];
        $scope.historyLoading = false;
        $scope.historyStats = null;

        $scope.toggleHistory = function () {
            $scope.historyMode = !$scope.historyMode;
            if ($scope.historyMode) {
                $scope.loadHistory();
            } else {
                $scope.clearHistory();
            }
        };

        var computeHistoryStats = function (points) {
            if (!points || points.length < 2) {
                $scope.historyStats = null;
                return;
            }
            var totalDist = 0;
            var stops = 0;
            var STOP_THRESHOLD_MS = 300000;
            var STOP_RADIUS_M = 50;

            for (var i = 1; i < points.length; i++) {
                totalDist += haversine(points[i - 1].lat, points[i - 1].lon, points[i].lat, points[i].lon);
                if (points[i].ts - points[i - 1].ts > STOP_THRESHOLD_MS) {
                    var dist = haversine(points[i - 1].lat, points[i - 1].lon, points[i].lat, points[i].lon);
                    if (dist < STOP_RADIUS_M) {
                        stops++;
                    }
                }
            }
            $scope.historyStats = {
                distance: (totalDist / 1000).toFixed(1),
                stops: stops
            };
        };

        var haversine = function (lat1, lon1, lat2, lon2) {
            var R = 6371000;
            var dLat = (lat2 - lat1) * Math.PI / 180;
            var dLon = (lon2 - lon1) * Math.PI / 180;
            var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2);
            return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        };

        $scope.loadHistory = function () {
            if (!$scope.selectedDeviceId) return;
            $scope.historyLoading = true;
            $scope.historyPoints = [];
            $scope.historyStats = null;
            var now = Date.now();
            var hours = parseInt($scope.historyPeriod, 10) || 24;
            var from = now - hours * 3600000;
            $http.get('/api-location-history.jsp', {
                params: { deviceId: $scope.selectedDeviceId, from: from, to: now, limit: 5000 }
            }).then(function (resp) {
                $scope.historyLoading = false;
                $scope.historyPoints = resp.data || [];
                computeHistoryStats($scope.historyPoints);
                $scope.drawHistory();
            }, function () {
                $scope.historyLoading = false;
                $scope.historyPoints = [];
                $scope.historyStats = null;
            });
        };

        $scope.drawHistory = function () {
            if (!mapInstance || !$scope.historyPoints.length) return;
            mapInstance.removePolyline('history-track');
            mapInstance.removeMarker('history-start');
            mapInstance.removeMarker('history-end');
            var coords = $scope.historyPoints.map(function (p) { return [p.lat, p.lon]; });
            mapInstance.addPolyline('history-track', coords, { color: '#0ea5b7', weight: 3, opacity: 0.8 });
            var first = $scope.historyPoints[0];
            var last = $scope.historyPoints[$scope.historyPoints.length - 1];
            mapInstance.addMarker('history-start', first.lat, first.lon,
                { iconUrl: 'images/circle-green.png', iconSize: [14, 14], iconAnchor: [7, 7] },
                localization.localize('gpsmap.history') + ' - ' + localization.localize('gpsmap.history.start'));
            mapInstance.addMarker('history-end', last.lat, last.lon,
                { iconUrl: 'images/circle-red.png', iconSize: [14, 14], iconAnchor: [7, 7] },
                localization.localize('gpsmap.history') + ' - ' + localization.localize('gpsmap.history.end'));
        };

        $scope.clearHistory = function () {
            $scope.historyPoints = [];
            $scope.historyStats = null;
            if (mapInstance) {
                mapInstance.removePolyline('history-track');
                mapInstance.removeMarker('history-start');
                mapInstance.removeMarker('history-end');
            }
        };

        $scope.exportHistory = function (format) {
            if (!$scope.historyPoints.length) return;
            var blob, filename;
            var deviceId = $scope.selectedDeviceId;
            if (format === 'csv') {
                var lines = ['lat,lon,alt,speed,timestamp'];
                $scope.historyPoints.forEach(function (p) {
                    lines.push([p.lat, p.lon, p.alt || 0, p.speed || 0, p.ts].join(','));
                });
                blob = new Blob([lines.join('\n')], { type: 'text/csv' });
                filename = 'gps-history-' + deviceId + '.csv';
            } else if (format === 'gpx') {
                var gpx = '<?xml version="1.0" encoding="UTF-8"?>\n<gpx version="1.1" creator="HWMDM">\n<trk><name>Device ' + deviceId + '</name><trkseg>\n';
                $scope.historyPoints.forEach(function (p) {
                    gpx += '<trkpt lat="' + p.lat + '" lon="' + p.lon + '">';
                    if (p.alt) gpx += '<ele>' + p.alt + '</ele>';
                    gpx += '<time>' + new Date(p.ts).toISOString() + '</time>';
                    gpx += '</trkpt>\n';
                });
                gpx += '</trkseg></trk></gpx>';
                blob = new Blob([gpx], { type: 'application/gpx+xml' });
                filename = 'gps-history-' + deviceId + '.gpx';
            } else {
                var kml = '<?xml version="1.0" encoding="UTF-8"?>\n<kml xmlns="http://www.opengis.net/kml/2.2"><Document><name>GPS History</name><Placemark><LineString><coordinates>\n';
                $scope.historyPoints.forEach(function (p) {
                    kml += p.lon + ',' + p.lat + ',' + (p.alt || 0) + '\n';
                });
                kml += '</coordinates></LineString></Placemark></Document></kml>';
                blob = new Blob([kml], { type: 'application/vnd.google-earth.kml+xml' });
                filename = 'gps-history-' + deviceId + '.kml';
            }
            var url = URL.createObjectURL(blob);
            var a = document.createElement('a');
            a.href = url; a.download = filename; a.click();
            URL.revokeObjectURL(url);
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
