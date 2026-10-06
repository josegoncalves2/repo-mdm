// Localization completed
angular.module('headwind-kiosk')
    .controller('GpsMapTabController', function ($scope, $timeout, $window, $http, $filter, localization, summaryService, groupService,
                                                 externalLibLoader, HWMDMMap, alertService, deviceFocusService) {
        var mapInstance = null;
        var leafletMap = null;
        var tileLayers = {};
        var REFRESH_INTERVAL_STORAGE_KEY = 'hwmdm.gps.refreshIntervalMs';
        var DEFAULT_REFRESH_INTERVAL_MS = 30000;
        var ONLINE_THRESHOLD_MS = 300000;
        var STOP_THRESHOLD_MS = 300000;
        var STOP_RADIUS_M = 50;
        var refreshTimer = null;
        var focusedDevice = deviceFocusService.consume();
        var firstMapFitDone = false;
        var playbackTimer = null;
        var playbackMarker = null;
        var accuracyCircle = null;
        var stopMarkers = [];

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
        $scope.showFilters = false;
        $scope.filters = { onlineOnly: false, lowBattery: false, moving: false };
        $scope.currentLayer = 'streets';

        $scope.refreshOptions = [
            {value: 30000, label: localization.localize('gpsmap.refresh.30s')},
            {value: 60000, label: localization.localize('gpsmap.refresh.60s')},
            {value: 0, label: localization.localize('gpsmap.refresh.off')}
        ];

        var storedInterval = parseInt($window.localStorage.getItem(REFRESH_INTERVAL_STORAGE_KEY), 10);
        $scope.refreshIntervalMs = [0, 30000, 60000].indexOf(storedInterval) >= 0
            ? storedInterval : DEFAULT_REFRESH_INTERVAL_MS;

        // --- Playback state ---
        $scope.historyMode = false;
        $scope.historyPeriod = '24';
        $scope.historyPoints = [];
        $scope.historyLoading = false;
        $scope.historyStats = null;
        $scope.historyStops = [];
        $scope.playback = {
            playing: false,
            index: 0,
            speed: 1,
            currentTime: '',
            endTime: '',
            currentPoint: null
        };

        // --- Helpers ---
        var markerIcon = function (statusCode) {
            var color = statusCode || 'grey';
            if (['green', 'yellow', 'red', 'brown', 'grey'].indexOf(color) < 0) color = 'grey';
            return { iconUrl: 'images/circle-' + color + '.png', iconSize: [18, 18], iconAnchor: [9, 9], popupAnchor: [0, -9] };
        };

        var escapeHtml = function (v) {
            return String(v || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        };

        var infrastructureIps = { '10.0.17.106': true, '10.0.9.1': true, '10.1.1.1': true };

        var reportedDeviceIp = function (device) {
            if (!device) return null;
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
            $scope.devices.forEach(function (d) { if (d.isOnline) on++; else off++; });
            $scope.onlineCount = on;
            $scope.offlineCount = off;
        };

        var haversine = function (lat1, lon1, lat2, lon2) {
            var R = 6371000, dLat = (lat2 - lat1) * Math.PI / 180, dLon = (lon2 - lon1) * Math.PI / 180;
            var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2);
            return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        };

        var formatDuration = function (ms) {
            var s = Math.round(ms / 1000);
            if (s < 60) return s + 's';
            var m = Math.floor(s / 60);
            if (m < 60) return m + 'min';
            var h = Math.floor(m / 60);
            m = m % 60;
            return h + 'h' + (m ? m + 'min' : '');
        };

        var formatTime = function (ts) {
            return $filter('date')(ts, 'dd/MM HH:mm:ss');
        };

        // --- Filters ---
        $scope.deviceSearchFilter = function (device) {
            if ($scope.filters.onlineOnly && !device.isOnline) return false;
            if ($scope.filters.lowBattery && (device.batteryLevel == null || device.batteryLevel >= 20)) return false;
            if ($scope.filters.moving && !(device.speed > 5)) return false;
            if (!$scope.deviceSearch) return true;
            var q = $scope.deviceSearch.toLowerCase();
            return (device.number && device.number.toLowerCase().indexOf(q) >= 0) ||
                   (device.model && device.model.toLowerCase().indexOf(q) >= 0) ||
                   (device.serial && device.serial.toLowerCase().indexOf(q) >= 0);
        };

        $scope.applyFilters = function () {};

        // --- Map popup ---
        var popupTemplate = function (device) {
            var title = escapeHtml(device.number || localization.localize('devices.unknown'));
            var model = escapeHtml(device.model || '');
            var ts = device.locationTs ? new Date(device.locationTs).toLocaleString() : '-';
            var statusLabel = device.isOnline ? localization.localize('gpsmap.status.online') : localization.localize('gpsmap.status.offline');
            var statusColor = device.isOnline ? '#1f8a70' : '#d64545';
            var battery = device.batteryLevel != null ? '<br><span>&#9889; ' + device.batteryLevel + '%</span>' : '';
            return '<div class="summary-map-popup"><strong>' + title + '</strong> <span style="color:' + statusColor + ';font-size:11px">' + escapeHtml(statusLabel) + '</span><br>' +
                '<span>' + model + '</span><br>' +
                '<span>' + escapeHtml(localization.localize('gpsmap.popup.updated')) + ': ' + escapeHtml(ts) + '</span>' +
                battery + '</div>';
        };

        // --- Map layer switching ---
        var setupTileLayers = function () {
            if (!leafletMap) return;
            tileLayers.streets = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; OpenStreetMap contributors', maxZoom: 19
            });
            tileLayers.satellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                attribution: '&copy; Esri', maxZoom: 18
            });
            tileLayers.topo = L.tileLayer('https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; OpenTopoMap', maxZoom: 17
            });
        };

        $scope.setMapLayer = function (name) {
            if (!leafletMap || !tileLayers[name]) return;
            if (tileLayers[$scope.currentLayer]) leafletMap.removeLayer(tileLayers[$scope.currentLayer]);
            tileLayers[name].addTo(leafletMap);
            $scope.currentLayer = name;
        };

        // --- Markers ---
        var clearMarkers = function () { if (mapInstance) mapInstance.removeAllMarkers(); };

        var addMarkers = function (fitMode) {
            if (!mapInstance) return;
            clearMarkers();
            $scope.devices.forEach(function (device) {
                mapInstance.addMarker('device-' + device.id, device.lat, device.lon,
                    markerIcon(device.isOnline ? 'green' : 'grey'), device.number, popupTemplate(device));
            });

            if (focusedDevice) {
                var focusMatch = $scope.devices.find(function (d) { return d.id === focusedDevice.id; });
                focusedDevice = null;
                if (focusMatch) {
                    $scope.selectedDeviceId = focusMatch.id;
                    $scope.selectedDevice = focusMatch;
                    mapInstance.centerMap(focusMatch.lat, focusMatch.lon);
                    mapInstance.openMarkerPopup('device-' + focusMatch.id);
                    mapInstance.invalidateSize();
                    return;
                }
            }

            if ($scope.selectedDeviceId) {
                var sel = $scope.devices.find(function (d) { return d.id === $scope.selectedDeviceId; });
                if (sel) {
                    $scope.selectedDevice = sel;
                    mapInstance.centerMap(sel.lat, sel.lon);
                    mapInstance.invalidateSize();
                    return;
                }
                $scope.selectedDeviceId = null;
                $scope.selectedDevice = null;
            }

            if (fitMode === 'none') { mapInstance.invalidateSize(); return; }

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
            if (mapInstance) { addMarkers(fitMode); return; }
            externalLibLoader.getLoader('leaflet').load().then(function () {
                $timeout(function () {
                    try {
                        mapInstance = HWMDMMap.get();
                        leafletMap = mapInstance.initMap($scope, 'gps-map', 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png');
                        setupTileLayers();
                        $scope.mapReady = true;
                        addMarkers(fitMode);
                    } catch (e) {
                        $scope.mapError = true;
                    }
                }, 0);
            });
        };

        // --- Refresh ---
        var cancelRefresh = function () { if (refreshTimer) { $timeout.cancel(refreshTimer); refreshTimer = null; } };
        var scheduleRefresh = function () {
            cancelRefresh();
            if (!$scope.refreshIntervalMs) return;
            refreshTimer = $timeout(function () { $scope.loadDevices(false); }, $scope.refreshIntervalMs);
        };

        $scope.loadDevices = function (userRequested) {
            if ($scope.loading) return;
            $scope.loading = true;
            $scope.errorMessage = null;
            var params = {};
            if ($scope.selectedGroupId) params.groupId = $scope.selectedGroupId;
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
            if (mapInstance && $scope.devices.length) { mapInstance.fitBoundsToMarkers(); firstMapFitDone = true; }
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
            if ($scope.historyMode) { $scope.historyMode = false; $scope.clearHistory(); }
        };

        $scope.centerOnDevice = function (device) {
            if (mapInstance && device) { mapInstance.centerMap(device.lat, device.lon); mapInstance.invalidateSize(); }
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

        // --- History ---
        $scope.toggleHistory = function () {
            $scope.historyMode = !$scope.historyMode;
            if ($scope.historyMode) $scope.loadHistory();
            else { $scope.stopPlayback(); $scope.clearHistory(); }
        };

        var detectStops = function (points) {
            var stops = [];
            if (!points || points.length < 2) return stops;
            var i = 0;
            while (i < points.length - 1) {
                var j = i + 1;
                while (j < points.length && haversine(points[i].lat, points[i].lon, points[j].lat, points[j].lon) < STOP_RADIUS_M) {
                    j++;
                }
                var duration = points[j - 1].ts - points[i].ts;
                if (duration >= STOP_THRESHOLD_MS) {
                    stops.push({
                        lat: points[i].lat,
                        lon: points[i].lon,
                        arrivalTime: formatTime(points[i].ts),
                        departureTime: formatTime(points[j - 1].ts),
                        durationMs: duration,
                        durationText: formatDuration(duration),
                        index: i
                    });
                }
                i = j;
            }
            return stops;
        };

        var computeHistoryStats = function (points) {
            if (!points || points.length < 2) { $scope.historyStats = null; return; }
            var totalDist = 0;
            for (var i = 1; i < points.length; i++) {
                totalDist += haversine(points[i - 1].lat, points[i - 1].lon, points[i].lat, points[i].lon);
            }
            var totalTime = points[points.length - 1].ts - points[0].ts;
            $scope.historyStats = {
                distance: (totalDist / 1000).toFixed(1),
                stops: $scope.historyStops.length,
                duration: formatDuration(totalTime)
            };
        };

        $scope.loadHistory = function () {
            if (!$scope.selectedDeviceId) return;
            $scope.historyLoading = true;
            $scope.historyPoints = [];
            $scope.historyStats = null;
            $scope.historyStops = [];
            $scope.stopPlayback();
            var now = Date.now();
            var hours = parseInt($scope.historyPeriod, 10) || 24;
            var from = now - hours * 3600000;
            $http.get('/api-location-history.jsp', {
                params: { deviceId: $scope.selectedDeviceId, from: from, to: now, limit: 5000 }
            }).then(function (resp) {
                $scope.historyLoading = false;
                $scope.historyPoints = resp.data || [];
                $scope.historyStops = detectStops($scope.historyPoints);
                computeHistoryStats($scope.historyPoints);
                $scope.drawHistory();
                if ($scope.historyPoints.length) {
                    $scope.playback.endTime = formatTime($scope.historyPoints[$scope.historyPoints.length - 1].ts);
                    $scope.playback.currentTime = formatTime($scope.historyPoints[0].ts);
                    $scope.playback.index = 0;
                    updatePlaybackPoint();
                }
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
            removeStopMarkers();
            removePlaybackMarker();

            var coords = $scope.historyPoints.map(function (p) { return [p.lat, p.lon]; });
            mapInstance.addPolyline('history-track', coords, { color: '#0ea5b7', weight: 3, opacity: 0.8 });

            var first = $scope.historyPoints[0];
            var last = $scope.historyPoints[$scope.historyPoints.length - 1];
            mapInstance.addMarker('history-start', first.lat, first.lon,
                { iconUrl: 'images/circle-green.png', iconSize: [14, 14], iconAnchor: [7, 7] }, 'Inicio');
            mapInstance.addMarker('history-end', last.lat, last.lon,
                { iconUrl: 'images/circle-red.png', iconSize: [14, 14], iconAnchor: [7, 7] }, 'Fim');

            // Stop markers
            $scope.historyStops.forEach(function (stop, idx) {
                if (leafletMap) {
                    var m = L.circleMarker([stop.lat, stop.lon], {
                        radius: 8, color: '#e74c3c', fillColor: '#e74c3c', fillOpacity: 0.6, weight: 2
                    }).addTo(leafletMap);
                    m.bindPopup('<strong>Parada #' + (idx + 1) + '</strong><br>' +
                        stop.arrivalTime + ' - ' + stop.departureTime + '<br>' + stop.durationText);
                    stopMarkers.push(m);
                }
            });

            // Playback marker
            if (leafletMap) {
                playbackMarker = L.marker([first.lat, first.lon], {
                    icon: L.divIcon({
                        className: 'gps-playback-marker',
                        iconSize: [16, 16],
                        iconAnchor: [8, 8]
                    }),
                    zIndexOffset: 1000
                }).addTo(leafletMap);
            }
        };

        var removeStopMarkers = function () {
            stopMarkers.forEach(function (m) { if (leafletMap) leafletMap.removeLayer(m); });
            stopMarkers = [];
        };

        var removePlaybackMarker = function () {
            if (playbackMarker && leafletMap) { leafletMap.removeLayer(playbackMarker); playbackMarker = null; }
            if (accuracyCircle && leafletMap) { leafletMap.removeLayer(accuracyCircle); accuracyCircle = null; }
        };

        $scope.clearHistory = function () {
            $scope.historyPoints = [];
            $scope.historyStats = null;
            $scope.historyStops = [];
            removeStopMarkers();
            removePlaybackMarker();
            if (mapInstance) {
                mapInstance.removePolyline('history-track');
                mapInstance.removeMarker('history-start');
                mapInstance.removeMarker('history-end');
            }
        };

        // --- Playback ---
        var updatePlaybackPoint = function () {
            var idx = parseInt($scope.playback.index, 10) || 0;
            if (idx >= $scope.historyPoints.length) idx = $scope.historyPoints.length - 1;
            if (idx < 0) idx = 0;
            var pt = $scope.historyPoints[idx];
            if (!pt) return;
            $scope.playback.currentPoint = pt;
            $scope.playback.currentTime = formatTime(pt.ts);
            if (playbackMarker) playbackMarker.setLatLng([pt.lat, pt.lon]);
            if (pt.accuracy && leafletMap) {
                if (accuracyCircle) leafletMap.removeLayer(accuracyCircle);
                accuracyCircle = L.circle([pt.lat, pt.lon], {
                    radius: pt.accuracy, color: '#0ea5b7', fillOpacity: 0.1, weight: 1
                }).addTo(leafletMap);
            } else if (accuracyCircle && leafletMap) {
                leafletMap.removeLayer(accuracyCircle);
                accuracyCircle = null;
            }
        };

        $scope.seekPlayback = function () { updatePlaybackPoint(); };

        $scope.togglePlayback = function () {
            if ($scope.playback.playing) {
                $scope.playback.playing = false;
                if (playbackTimer) { $timeout.cancel(playbackTimer); playbackTimer = null; }
            } else {
                if (parseInt($scope.playback.index, 10) >= $scope.historyPoints.length - 1) $scope.playback.index = 0;
                $scope.playback.playing = true;
                advancePlayback();
            }
        };

        $scope.stopPlayback = function () {
            $scope.playback.playing = false;
            $scope.playback.index = 0;
            if (playbackTimer) { $timeout.cancel(playbackTimer); playbackTimer = null; }
            updatePlaybackPoint();
        };

        var advancePlayback = function () {
            if (!$scope.playback.playing) return;
            var idx = parseInt($scope.playback.index, 10) + 1;
            if (idx >= $scope.historyPoints.length) {
                $scope.playback.playing = false;
                return;
            }
            $scope.playback.index = idx;
            updatePlaybackPoint();
            var speed = parseInt($scope.playback.speed, 10) || 1;
            playbackTimer = $timeout(advancePlayback, Math.max(50, 500 / speed));
        };

        $scope.goToStop = function (stop) {
            if (leafletMap) leafletMap.panTo([stop.lat, stop.lon]);
            $scope.playback.index = stop.index;
            updatePlaybackPoint();
        };

        // --- Export ---
        $scope.exportHistory = function (format) {
            if (!$scope.historyPoints.length) return;
            var blob, filename;
            var deviceId = $scope.selectedDeviceId;
            if (format === 'csv') {
                var lines = ['lat,lon,alt,speed,timestamp,datetime'];
                $scope.historyPoints.forEach(function (p) {
                    lines.push([p.lat, p.lon, p.alt || 0, p.speed || 0, p.ts, new Date(p.ts).toISOString()].join(','));
                });
                blob = new Blob([lines.join('\n')], { type: 'text/csv' });
                filename = 'gps-history-' + deviceId + '.csv';
            } else if (format === 'gpx') {
                var gpx = '<?xml version="1.0" encoding="UTF-8"?>\n<gpx version="1.1" creator="HWMDM">\n<trk><name>Device ' + deviceId + '</name><trkseg>\n';
                $scope.historyPoints.forEach(function (p) {
                    gpx += '<trkpt lat="' + p.lat + '" lon="' + p.lon + '">';
                    if (p.alt) gpx += '<ele>' + p.alt + '</ele>';
                    gpx += '<time>' + new Date(p.ts).toISOString() + '</time></trkpt>\n';
                });
                gpx += '</trkseg></trk></gpx>';
                blob = new Blob([gpx], { type: 'application/gpx+xml' });
                filename = 'gps-history-' + deviceId + '.gpx';
            } else {
                var kml = '<?xml version="1.0" encoding="UTF-8"?>\n<kml xmlns="http://www.opengis.net/kml/2.2"><Document><name>GPS History</name><Placemark><LineString><coordinates>\n';
                $scope.historyPoints.forEach(function (p) { kml += p.lon + ',' + p.lat + ',' + (p.alt || 0) + '\n'; });
                kml += '</coordinates></LineString></Placemark></Document></kml>';
                blob = new Blob([kml], { type: 'application/vnd.google-earth.kml+xml' });
                filename = 'gps-history-' + deviceId + '.kml';
            }
            var url = URL.createObjectURL(blob);
            var a = document.createElement('a'); a.href = url; a.download = filename; a.click();
            URL.revokeObjectURL(url);
        };

        // --- Groups ---
        var loadGroups = function () {
            groupService.getAllGroups(function (response) {
                $scope.groups = response.data || [];
                $scope.groups.unshift({id: null, name: localization.localize('gpsmap.all.groups')});
            });
        };

        $scope.$on('$destroy', function () {
            cancelRefresh();
            if (playbackTimer) $timeout.cancel(playbackTimer);
            removeStopMarkers();
            removePlaybackMarker();
        });

        loadGroups();
        $scope.loadDevices(true);
    });
