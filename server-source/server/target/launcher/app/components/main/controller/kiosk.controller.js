// Localization completed
angular.module('headwind-kiosk')
    .controller('KioskTabController', function ($scope, $state, $timeout, deviceService, configurationService,
                                                applicationService, alertService, localization, appVersionComparisonService) {
        $scope.lastSeenText = function (device) {
            var sec = device ? device.lastUpdateAgeSec : null;
            if (sec === null || sec === undefined || sec === '' || isNaN(sec)) {
                return localization.localize('kiosk.never.seen');
            }
            sec = Math.max(0, Math.round(sec));
            if (sec < 60) { return sec + ' s ' + localization.localize('kiosk.ago'); }
            if (sec < 3600) { return Math.round(sec / 60) + ' min ' + localization.localize('kiosk.ago'); }
            if (sec < 86400) { return Math.round(sec / 3600) + ' h ' + localization.localize('kiosk.ago'); }
            return Math.round(sec / 86400) + ' d ' + localization.localize('kiosk.ago');
        };
        var infrastructureIps = {
            '10.0.17.106': true,
            '10.0.9.1': true,
            '10.1.1.1': true
        };
        var strictRestrictions = [
            'no_factory_reset',
            'no_safe_boot',
            'no_add_user',
            'no_modify_accounts',
            'no_control_apps',
            'no_config_bluetooth',
            'no_config_credentials',
            'no_config_mobile_networks',
            'no_config_tethering',
            'no_config_vpn',
            'no_config_wifi',
            'no_debugging_features',
            'no_usb_file_transfer',
            'no_outgoing_beam'
        ];

        $scope.loading = false;
        $scope.saving = false;
        $scope.configurations = [];
        $scope.devices = [];
        $scope.applications = [];
        $scope.agentVersions = [];
        $scope.selectedConfigurationId = null;
        $scope.configuration = null;
        $scope.activeKioskTab = 'devices';
        $scope.feedback = null;
        $scope.errorMessage = null;
        var configurationsLoaded = false;
        var devicesLoaded = false;
        var defaultSelected = false;

        $scope.tabs = [
            {id: 'devices', label: localization.localize('kiosk.tab.devices')},
            {id: 'lockdown', label: localization.localize('kiosk.tab.lockdown')},
            {id: 'apps', label: localization.localize('kiosk.tab.apps')},
            {id: 'agent', label: localization.localize('kiosk.tab.agent')},
            {id: 'location', label: localization.localize('kiosk.tab.location')},
            {id: 'apply', label: localization.localize('kiosk.tab.apply')}
        ];

        var versionIndex = function (versionText) {
            return String(versionText || '0').split('.').map(function (part) {
                var clean = part.replace(/[^0-9]+/g, '') || '0';
                while (clean.length < 10) {
                    clean = '0' + clean;
                }
                return clean;
            }).join('');
        };

        var parseRestrictions = function () {
            return ($scope.configuration && $scope.configuration.restrictions || '')
                .split(',')
                .map(function (item) { return item.trim(); })
                .filter(function (item) { return item.length > 0; });
        };

        var writeRestrictions = function (items) {
            if ($scope.configuration) {
                $scope.configuration.restrictions = items.join(',');
            }
        };

        var appVersionId = function (app) {
            return app && (app.usedVersionId || app.applicationVersionId || app.latestVersion);
        };

        var isInfrastructureIp = function (ip) {
            return !!(ip && infrastructureIps[String(ip).trim()]);
        };

        var reportedDeviceIp = function (device) {
            if (!device) {
                return null;
            }
            // publicIp e' a fonte real (o agente reporta o endereco do aparelho); os campos
            // de info ficam por compatibilidade -- o launcher 6.36 nao envia nenhum deles.
            var info = device.info || {};
            var ip = info.deviceIp || info.ip || info.localIp || device.publicIp || null;
            return isInfrastructureIp(ip) ? null : ip;
        };

        var appScore = function (app) {
            var score = 0;
            if ($scope.configuration && appVersionId(app) === $scope.configuration.mainAppId) {
                score += 100;
            }
            if ($scope.configuration && appVersionId(app) === $scope.configuration.contentAppId) {
                score += 50;
            }
            if (app.useKiosk) {
                score += 10;
            }
            if (app.actionChanged) {
                score += 5;
            }
            return score;
        };

        var installedApps = function () {
            var byApplication = {};
            ($scope.applications || []).filter(function (app) {
                return app.action != 0;
            }).forEach(function (app) {
                var key = app.id || app.pkg;
                if (!byApplication[key] || appScore(app) > appScore(byApplication[key])) {
                    byApplication[key] = app;
                }
            });
            return Object.keys(byApplication).map(function (key) {
                return byApplication[key];
            });
        };

        var launcherApp = function () {
            return ($scope.applications || []).filter(function (app) {
                return app.pkg === 'com.hmdm.launcher';
            })[0] || null;
        };

        var contentApp = function () {
            if (!$scope.configuration || !$scope.configuration.contentAppId) {
                return null;
            }
            return ($scope.applications || []).filter(function (app) {
                return appVersionId(app) === $scope.configuration.contentAppId;
            })[0] || null;
        };

        var ensureApplicationInstall = function (app, useKiosk) {
            if (!app) {
                return;
            }
            if (!app.usedVersionId) {
                app.usedVersionId = app.applicationVersionId || app.latestVersion;
            }
            app.action = 1;
            app.actionChanged = true;
            if (app.type === 'app') {
                app.useKiosk = !!useKiosk;
            }
        };

        var normalizeConfigurationCollections = function () {
            $scope.configuration.applicationSettings = $scope.configuration.applicationSettings || [];
            $scope.configuration.applicationUsageParameters = $scope.configuration.applicationUsageParameters || [];
            $scope.configuration.files = $scope.configuration.files || [];
        };

        var loadDevices = function () {
            deviceService.getAllDevices({
                value: '',
                pageNum: 1,
                pageSize: 200,
                sortBy: null,
                sortDir: 'ASC'
            }, function (response) {
                $scope.devices = response.data && response.data.devices ? response.data.devices.items : [];
                devicesLoaded = true;
                chooseDefaultConfiguration();
            });
        };

        var chooseDefaultConfiguration = function () {
            if (!configurationsLoaded || !devicesLoaded || defaultSelected || $scope.selectedConfigurationId) {
                return;
            }
            var usage = {};
            ($scope.devices || []).forEach(function (device) {
                usage[device.configurationId] = (usage[device.configurationId] || 0) + 1;
            });
            var sorted = ($scope.configurations || []).slice().sort(function (a, b) {
                var scoreA = (a.kioskMode ? 1000 : 0) + (usage[a.id] || 0);
                var scoreB = (b.kioskMode ? 1000 : 0) + (usage[b.id] || 0);
                if (scoreA === scoreB) {
                    return String(a.name || '').localeCompare(String(b.name || ''));
                }
                return scoreB - scoreA;
            });
            if (sorted.length > 0) {
                defaultSelected = true;
                $scope.selectedConfigurationId = sorted[0].id;
                selectConfiguration(sorted[0].id);
            }
        };

        var loadAgentVersions = function () {
            var agent = launcherApp();
            if (!agent || !agent.id) {
                $scope.agentVersions = [];
                return;
            }
            applicationService.getApplicationVersions({id: agent.id}, function (response) {
                if (response.status === 'OK' && response.data) {
                    $scope.agentVersions = response.data.sort(function (a, b) {
                        return versionIndex(b.version) < versionIndex(a.version) ? -1 : 1;
                    });
                }
            });
        };

        var selectConfiguration = function (id) {
            if (!id) {
                return;
            }
            $scope.loading = true;
            $scope.errorMessage = null;
            configurationService.getById({id: id}, function (response) {
                if (response.status !== 'OK' || !response.data) {
                    $scope.loading = false;
                    $scope.errorMessage = localization.localize('error.request.failure');
                    return;
                }
                $scope.configuration = response.data;
                normalizeConfigurationCollections();
                configurationService.getApplications({id: id}, function (appsResponse) {
                    $scope.loading = false;
                    if (appsResponse.status === 'OK') {
                        $scope.applications = appsResponse.data || [];
                        loadAgentVersions();
                    } else {
                        $scope.errorMessage = localization.localize(appsResponse.message || 'error.request.failure');
                    }
                });
            });
        };

        var loadConfigurations = function () {
            configurationService.getAllConfigurations({value: ''}, function (response) {
                $scope.configurations = response.data || [];
                configurationsLoaded = true;
                chooseDefaultConfiguration();
            });
        };

        $scope.selectTab = function (tab) {
            $scope.activeKioskTab = tab;
        };

        $scope.selectConfiguration = function () {
            selectConfiguration($scope.selectedConfigurationId);
        };

        $scope.hasRestriction = function (restriction) {
            return parseRestrictions().indexOf(restriction) !== -1;
        };

        $scope.setRestriction = function (restriction, enabled) {
            var items = parseRestrictions().filter(function (item) {
                return item !== restriction;
            });
            if (enabled) {
                items.push(restriction);
            }
            writeRestrictions(items);
        };

        $scope.applyStrictKiosk = function () {
            if (!$scope.configuration) {
                return;
            }
            $scope.configuration.kioskMode = true;
            $scope.configuration.kioskHome = false;
            $scope.configuration.kioskRecents = false;
            $scope.configuration.kioskNotifications = false;
            $scope.configuration.kioskSystemInfo = false;
            $scope.configuration.kioskKeyguard = false;
            $scope.configuration.kioskExit = false;
            $scope.configuration.kioskLockButtons = true;
            $scope.configuration.kioskScreenOn = true;
            $scope.configuration.permissive = false;
            $scope.configuration.lockSafeSettings = true;
            // Preserve the configured delivery transport.
            $scope.enableGps();
            strictRestrictions.forEach(function (restriction) {
                $scope.setRestriction(restriction, true);
            });
            var agent = launcherApp();
            if (agent) {
                ensureApplicationInstall(agent, true);
                $scope.configuration.mainAppId = appVersionId(agent);
            }
            var firstAllowed = ($scope.applications || []).filter(function (app) {
                return app.pkg !== 'com.hmdm.launcher' && app.useKiosk && appVersionId(app);
            })[0];
            if (firstAllowed) {
                $scope.configuration.contentAppId = appVersionId(firstAllowed);
            }
        };

        $scope.enableGps = function () {
            if (!$scope.configuration) {
                return;
            }
            $scope.configuration.gps = true;
            $scope.configuration.requestUpdates = 'GPS';
            $scope.configuration.disableLocation = false;
            $scope.configuration.appPermissions = 'GRANTALL';
            $scope.setRestriction('no_share_location', false);
        };

        $scope.toggleAppInstall = function (app) {
            if (app.action == 1) {
                app.action = 0;
                app.actionChanged = true;
                app.useKiosk = false;
            } else {
                ensureApplicationInstall(app, false);
            }
        };

        $scope.toggleAppKiosk = function (app) {
            ensureApplicationInstall(app, !app.useKiosk);
            if (app.useKiosk && app.pkg !== 'com.hmdm.launcher' && !$scope.configuration.contentAppId) {
                $scope.configuration.contentAppId = appVersionId(app);
            }
        };

        $scope.selectContentApp = function (app) {
            ensureApplicationInstall(app, true);
            $scope.configuration.contentAppId = appVersionId(app);
        };

        $scope.selectAgentVersion = function (version) {
            var agent = launcherApp();
            if (!agent || !version) {
                return;
            }
            ($scope.applications || []).forEach(function (app) {
                if (app.pkg === 'com.hmdm.launcher' && app !== agent) {
                    app.action = 0;
                    app.useKiosk = false;
                    app.actionChanged = true;
                }
            });
            ensureApplicationInstall(agent, true);
            agent.usedVersionId = version.id;
            agent.version = version.version;
            agent.url = version.url;
            agent.urlArm64 = version.urlArm64;
            agent.urlArmeabi = version.urlArmeabi;
            $scope.configuration.mainAppId = version.id;
        };

        $scope.useLatestAgent = function () {
            if ($scope.agentVersions.length > 0) {
                $scope.selectAgentVersion($scope.agentVersions[0]);
            }
        };

        $scope.getAgentSummary = function () {
            var agent = launcherApp();
            if (!agent) {
                return localization.localize('kiosk.agent.none');
            }
            return (agent.name || agent.pkg) + (agent.version ? ' ' + agent.version : '');
        };

        $scope.getContentAppSummary = function () {
            var app = contentApp();
            return app ? ((app.name || app.pkg) + (app.version ? ' ' + app.version : ''))
                       : localization.localize('kiosk.launch.app.none');
        };

        $scope.getDeviceIpDisplay = function (device) {
            return reportedDeviceIp(device) || localization.localize('devices.ip.not.reported');
        };

        $scope.getDeviceIpNote = function (device) {
            if (reportedDeviceIp(device)) {
                return localization.localize('devices.ip.reported.agent');
            }
            if (isInfrastructureIp(device && device.publicIp)) {
                return localization.localize('devices.ip.infra.hidden');
            }
            return localization.localize('devices.ip.not.reported.hint');
        };

        // O estado real de bloqueio deste parque nao e' o "kioskMode" do Headwind.
        //
        // O launcher 6.36 open source NAO tem o quiosque single-app (lock task): a funcao
        // ProUtils.isKioskModeRunning() devolve false fixo nesta edicao, e e' dela que sai
        // o device.kioskMode reportado. Ou seja, esse campo e' SEMPRE false neste launcher,
        // e ler so' ele fazia a coluna dizer "Nao bloqueado" para aparelhos que estao, sim,
        // travados -- porque estao presos no launcher do MDM como home (defaultLauncher) em
        // modo gerenciado (mdmMode), com as restricoes do perfil aplicadas.
        //
        // Entao a coluna passa a distinguir tres realidades em vez de duas:
        //   locked  -> quiosque single-app de fato rodando (so' com o launcher pago);
        //   managed -> preso no launcher do MDM como home, gerenciado (o estado destes tablets);
        //   unlocked-> nem uma coisa nem outra.
        var deviceKioskState = function (device) {
            var info = device && device.info;
            if (!info && (device.kioskMode === null || device.kioskMode === undefined)) {
                return 'unknown';
            }
            var kiosk = info ? info.kioskMode : device.kioskMode;
            if (kiosk) {
                return 'locked';
            }
            if (info && info.defaultLauncher && info.mdmMode) {
                return 'managed';
            }
            return 'unlocked';
        };

        $scope.kioskStateLabel = function (device) {
            return localization.localize('kiosk.device.state.' + deviceKioskState(device));
        };

        $scope.kioskStateClass = function (device) {
            switch (deviceKioskState(device)) {
                case 'locked':
                case 'managed':
                    return 'text-success';
                case 'unlocked':
                    return 'text-danger';
                default:
                    return 'text-muted';
            }
        };

        // Only worth a note when the device does not match what the profile asks for,
        // so the operator can tell "policy not applied yet" from "policy says no kiosk".
        $scope.kioskStateNote = function (device) {
            var state = deviceKioskState(device);
            if (state === 'unknown') {
                return localization.localize('kiosk.device.state.unknown.hint');
            }
            // Launcher gerenciado e' um estado bom e final neste launcher -- nao ha um
            // quiosque single-app "pendente" que va' aplicar depois, porque o APK livre nao
            // o tem. A nota explica o que esse estado garante, em vez de prometer mais.
            if (state === 'managed') {
                return localization.localize('kiosk.device.state.managed.hint');
            }
            // Aparelho que deveria estar gerenciado e nao esta': aviso real.
            if (state === 'unlocked') {
                return localization.localize('kiosk.device.state.unlocked.hint');
            }
            return null;
        };

        $scope.profileDevices = function () {
            if (!$scope.selectedConfigurationId) {
                return [];
            }
            return ($scope.devices || []).filter(function (device) {
                return device.configurationId === $scope.selectedConfigurationId;
            });
        };

        $scope.allowedApps = function () {
            return installedApps().filter(function (app) {
                return app.useKiosk;
            });
        };

        $scope.blockedApps = function () {
            return ($scope.applications || []).filter(function (app) {
                return app.type === 'app' && !app.useKiosk;
            });
        };

        $scope.saveProfile = function () {
            if (!$scope.configuration || $scope.saving) {
                return;
            }
            if (!$scope.configuration.password) {
                $scope.errorMessage = localization.localize('error.empty.configuration.password');
                return;
            }
            var request = angular.copy($scope.configuration);
            request.applications = installedApps();
            request.type = request.type || 0;
            $scope.saving = true;
            $scope.errorMessage = null;
            configurationService.updateConfiguration(request, function (response) {
                $scope.saving = false;
                if (response.status === 'OK') {
                    $scope.feedback = localization.localize('kiosk.save.feedback');
                    if (response.data) {
                        $scope.configuration = response.data;
                        normalizeConfigurationCollections();
                    }
                    $timeout(function () { $scope.feedback = null; }, 10000);
                    loadDevices();
                    selectConfiguration($scope.selectedConfigurationId);
                } else {
                    $scope.errorMessage = localization.localize(response.message || 'error.request.failure');
                }
            }, function () {
                $scope.saving = false;
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        $scope.forceKiosk = function (device) {
            deviceService.forceKiosk({id: device.id}, {}, function (response) {
                alertService.showAlertMessage(response.status === 'OK'
                    ? localization.localize('success.remote.command')
                    : localization.localize(response.message || 'error.request.failure'));
            }, alertService.onRequestFailure);
        };

        $scope.pushConfig = function (device) {
            deviceService.sendCommand({id: device.id}, {action: 'set_config', params: {}}, function (response) {
                alertService.showAlertMessage(response.status === 'OK'
                    ? localization.localize('success.remote.command')
                    : localization.localize(response.message || 'error.request.failure'));
            }, alertService.onRequestFailure);
        };

        $scope.openRemote = function () {
            $state.go('remote');
        };

        loadConfigurations();
        loadDevices();
    });
