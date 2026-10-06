angular.module('headwind-kiosk')
    .controller('ShellController', function ($scope, $sce, $rootScope, $state, $timeout, userService, authService,
                                             pluginService, moduleRegistry, localization, hintService, $q, $ocLazyLoad) {

        $scope.localization = localization;
        $scope.moduleRegistry = moduleRegistry;

        var routes = {
            SUMMARY: 'summary',
            DEVICES: 'main',
            KIOSK: 'kiosk',
            REMOTE: 'remote',
            GPSMAP: 'gpsMap',
            CHAT: 'chat',
            REPORTS: 'reports',
            GOVERNANCE: 'governance',
            APPS: 'applications',
            CONFS: 'configurations',
            FILES: 'files',
            DESIGN: 'designSettings',
            COMMON: 'commonSettings',
            USERS: 'users',
            ROLES: 'roles',
            GROUPS: 'groups',
            ICONS: 'icons',
            GENERAL: 'generalSettings',
            EXTENSIONS: 'extensions',
            INTEGRATIONS: 'integrations',
            SERVER: 'server',
            PROFILE: 'profile'
        };

        var PLUGIN_STATES = {
            'plugin-webfilter': 'webfilterModule',
            'plugin-devicelog': 'devicelogModule',
            'plugin-audit': 'auditModule',
            'plugin-push': 'pushModule',
            'plugin-deviceinfo': 'deviceinfoModule',
            'plugin-settings-messaging': 'messagingSettingsModule'
        };

        var tabTitleKeys = {
            SUMMARY: 'nav.dashboard',
            DEVICES: 'nav.devices',
            KIOSK: 'nav.kiosk',
            GPSMAP: 'nav.gpsmap',
            REMOTE: 'nav.remote',
            CHAT: 'nav.chat',
            REPORTS: 'nav.reports',
            CONFS: 'nav.configurations',
            APPS: 'nav.applications',
            FILES: 'nav.files',
            ICONS: 'nav.icons',
            USERS: 'nav.users',
            ROLES: 'nav.roles',
            GROUPS: 'nav.groups',
            COMMON: 'nav.common',
            DESIGN: 'nav.design',
            GENERAL: 'nav.general',
            EXTENSIONS: 'nav.extensions',
            GOVERNANCE: 'nav.governance',
            INTEGRATIONS: 'nav.integrations',
            SERVER: 'nav.server',
            PROFILE: 'menu.profile'
        };

        var STATE_TO_TAB = {};
        angular.forEach(routes, function (state, tab) { STATE_TO_TAB[state] = tab; });
        angular.forEach(PLUGIN_STATES, function (state, tab) { STATE_TO_TAB[state] = tab; });

        function syncActiveTab() {
            var tab = STATE_TO_TAB[$state.current.name];
            if (tab) {
                $scope.activeTab = tab;
                // Persist activeTab no localStorage para sobreviver a F5
                if (typeof localStorage !== 'undefined') {
                    localStorage.setItem('hwmdm.activeTab', tab);
                }
            }
        }

        // Restaura activeTab do localStorage se a rota atual nao definir uma
        var savedTab = typeof localStorage !== 'undefined' ? localStorage.getItem('hwmdm.activeTab') : null;
        if (savedTab && STATE_TO_TAB[$state.current.name] === undefined) {
            $scope.activeTab = savedTab;
        }

        syncActiveTab();

        var NAV_SCROLL_KEY = 'hwmdm.navScrollTop';

        function saveNavScroll() {
            var nav = document.querySelector('.hwmdm-nav');
            if (nav) {
                localStorage.setItem(NAV_SCROLL_KEY, nav.scrollTop);
            }
        }

        function restoreNavScroll() {
            var saved = localStorage.getItem(NAV_SCROLL_KEY);
            if (saved == null) return;
            $timeout(function () {
                var nav = document.querySelector('.hwmdm-nav');
                if (nav) nav.scrollTop = parseInt(saved, 10);
            });
        }

        $rootScope.$on('$stateChangeStart', function () {
            saveNavScroll();
        });

        $rootScope.$on('$stateChangeSuccess', function () {
            syncActiveTab();
        });

        var cacheBustDeviceInfoResource = function (url) {
            if (typeof url !== 'string' || !url || url.indexOf('v=hux202610061730') >= 0) {
                return url;
            }
            return url + (url.indexOf('?') === -1 ? '?' : '&') + 'v=hux202610061730';
        };

        var waitForPluginModules = function (callback) {
            return function (response) {
                var plugins = response.status === 'OK' && response.data ? response.data.filter(function (plugin) {
                    return plugin.javascriptModuleFile;
                }) : [];
                $q.all(plugins.map(function (plugin) {
                    var moduleFile = plugin.identifier === 'deviceinfo'
                        ? cacheBustDeviceInfoResource(plugin.javascriptModuleFile)
                        : plugin.javascriptModuleFile;
                    return $ocLazyLoad.load(moduleFile).catch(angular.noop);
                })).then(function () {
                    callback(response);
                });
            };
        };

        var loadData = function () {
            pluginService.getAvailablePlugins(waitForPluginModules(function (response) {
                if (response.status === 'OK') {
                    if (response.data) {
                        $scope.functionsPlugins = response.data.filter(function (plugin) {
                            return plugin.functionsViewTemplate !== undefined && plugin.functionsViewTemplate !== null;
                        });
                        $scope.functionsPlugins.forEach(function (plugin) {
                            if (plugin.identifier === 'deviceinfo') {
                                plugin.functionsViewTemplate = cacheBustDeviceInfoResource(plugin.functionsViewTemplate);
                            }
                            var ID = 'plugin-' + plugin.identifier;
                            routes[ID] = PLUGIN_STATES[ID] || ('shell.' + ID);
                            STATE_TO_TAB[routes[ID]] = ID;
                        });

                        $scope.settingsPlugins = response.data.filter(function (plugin) {
                            return plugin.settingsViewTemplate !== undefined && plugin.settingsViewTemplate !== null;
                        });
                        $scope.settingsPlugins.forEach(function (plugin) {
                            var ID = 'plugin-settings-' + plugin.identifier;
                            routes[ID] = PLUGIN_STATES[ID] || ('shell.' + ID);
                            STATE_TO_TAB[routes[ID]] = ID;
                        });
                    }
                } else {
                    $scope.functionsPlugins = [];
                    $scope.settingsPlugins = [];
                }
            }));
        };

        $scope.currentUser = {};
        $scope.hasPermission = authService.hasPermission;
        $scope.canUseThemeLayout = function () {
            return authService.hasPermission('settings') || authService.hasPermission('device.branding.edit');
        };
        $scope.canManageRoles = function () {
            return authService.isSingleCustomer() || authService.isSuperAdmin();
        };

        var runtime = window.HWMDM_RUNTIME || {};
        $scope.serverAdminUrl = runtime.adminPort
            ? $sce.trustAsResourceUrl(window.location.protocol + '//' + window.location.hostname + ':' + runtime.adminPort + '/')
            : null;

        $scope.tabTitle = function () {
            if (tabTitleKeys[$scope.activeTab]) {
                return localization.localize(tabTitleKeys[$scope.activeTab]);
            }
            var plugin = ($scope.functionsPlugins || []).concat($scope.settingsPlugins || []).filter(function (p) {
                return $scope.activeTab === 'plugin-' + p.identifier
                    || $scope.activeTab === 'plugin-settings-' + p.identifier;
            })[0];
            return plugin ? localization.localize(plugin.nameLocalizationKey)
                : localization.localize('nav.extensions');
        };

        $scope.functionsPlugins = [];
        $scope.settingsPlugins = [];

        $scope.openTab = function (tabName) {
            if (tabName === $scope.activeTab) { return; }
            if (!routes[tabName]) { return; }
            hintService.stop();
            $scope.navOpen = false;

            var target = routes[tabName];
            if ($state.get(target)) {
                $state.go(target);
                return;
            }
            $scope.activeTab = tabName;
        };

        $scope.navOpen = false;
        $scope.toggleNav = function () { $scope.navOpen = !$scope.navOpen; };
        $scope.$on('HWMDM_TOGGLE_NAV', function () { $scope.toggleNav(); });
        $scope.closeNav = function () { $scope.navOpen = false; };

        var listener = $scope.$on('aero_PLUGINS_UPDATED', loadData);
        $scope.$on('$destroy', listener);

        $scope.navReady = false;
        $scope.navSections = [];

        var loadNav = function () {
            moduleRegistry.load().then(function () {
                $scope.navSections = moduleRegistry.visibleSections();
                $scope.navReady = true;
                restoreNavScroll();
            });
        };

        $scope.isModuleBlocked = function (id) {
            return $scope.navReady && moduleRegistry.isInMaintenance(id) && !moduleRegistry.canManage();
        };
        $scope.isModuleInMaintenance = function (id) {
            return $scope.navReady && moduleRegistry.isInMaintenance(id);
        };
        $scope.moduleMaintenanceInfo = function (id) {
            return moduleRegistry.maintenanceInfo(id);
        };

        var navUpdateListener = $rootScope.$on('aero_MODULES_UPDATED', function () {
            moduleRegistry.load(true).then(function () {
                $scope.navSections = moduleRegistry.visibleSections();
            });
        });
        var pluginsForNavListener = $rootScope.$on('aero_PLUGINS_UPDATED', function () {
            moduleRegistry.load(true).then(function () {
                $scope.navSections = moduleRegistry.visibleSections();
            });
        });
        $scope.$on('$destroy', navUpdateListener);
        $scope.$on('$destroy', pluginsForNavListener);

        userService.getCurrent(function (response) {
            if (response.data) {
                $scope.currentUser = response.data;
                authService.update(response.data);
                if ($scope.navReady) {
                    $scope.navSections = moduleRegistry.visibleSections();
                }
            }
        });
        var modulesLoadedListener = $rootScope.$on('aero_MODULES_LOADED', function () {
            if ($scope.navReady) {
                $scope.navSections = moduleRegistry.visibleSections();
            }
        });
        $scope.$on('$destroy', modulesLoadedListener);

        loadData();
        loadNav();
    });
