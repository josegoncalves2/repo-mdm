// Localization completed
angular.module('headwind-kiosk')
    .controller('TabController', function ($scope, $rootScope, $timeout, $state, userService, authService, openTab,
                                           pluginService, moduleRegistry, localization, hintService, $q, $ocLazyLoad) {

        $scope.localization = localization;
        $scope.moduleRegistry = moduleRegistry;

        // Only tabs that content.html can actually render. LANG/HINTS/PLUGINS used to be
        // listed here without a matching template, so openTab() accepted them and left the
        // content area empty.
        // Every entry is now a real ui-router state, which is what gives each screen its
        // own URL and therefore its own browser-history entry.
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
            INTEGRATIONS: 'integrations'
        };

        // Modulos/extensoes abertos direto do menu (fora da tela Modulos) tambem ganham
        // estado proprio, senao F5 nessas telas perde o lugar (item 9 do pedido "modulos").
        // Qualquer plugin fora desta lista continua no comportamento antigo (troca so no
        // cliente, sem URL propria) - nenhum dos catalogados em moduleRegistry fica de fora.
        var PLUGIN_STATES = {
            'plugin-webfilter': 'webfilterModule',
            'plugin-devicelog': 'devicelogModule',
            'plugin-audit': 'auditModule',
            'plugin-push': 'pushModule',
            'plugin-deviceinfo': 'deviceinfoModule',
            'plugin-settings-messaging': 'messagingSettingsModule'
        };

        // Same wording as the sidebar. Shown in the narrow-viewport bar next to the menu
        // button so the drawer being closed never leaves you without a "where am I".
        // Resource keys, not literals, so this bar follows the configured language along
        // with the sidebar instead of staying in English.
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
            INTEGRATIONS: 'nav.integrations'
        };

        // Entrada direta pela URL (F5) numa tela de plugin: o app.js carrega o modulo JS do plugin em
        // paralelo, e o template da tela nao pode ser montado antes dele (o ng-controller ainda nao
        // existiria). Espera esses modulos antes de entregar a lista; os ja carregados resolvem na hora.
        var waitForPluginModules = function (callback) {
            return function (response) {
                var plugins = response.status === 'OK' && response.data ? response.data.filter(function (plugin) {
                    return plugin.javascriptModuleFile;
                }) : [];
                $q.all(plugins.map(function (plugin) {
                    return $ocLazyLoad.load(plugin.javascriptModuleFile).catch(angular.noop);
                })).then(function () {
                    callback(response);
                });
            };
        };

        var loadData = function () {
            pluginService.getAvailablePlugins(waitForPluginModules(function (response) {
                if (response.status === 'OK') {
                    if (response.data) {
                        // Plugins available for Functions tab
                        $scope.functionsPlugins = response.data.filter(function (plugin) {
                            return plugin.functionsViewTemplate !== undefined && plugin.functionsViewTemplate !== null;
                        });
                        $scope.functionsPlugins.forEach(function (plugin) {
                            let ID = 'plugin-' + plugin.identifier;
                            routes[ID] = PLUGIN_STATES[ID] || ID;
                        });

                        // Plugins available for Settings tab
                        $scope.settingsPlugins = response.data.filter(function (plugin) {
                            return plugin.settingsViewTemplate !== undefined && plugin.settingsViewTemplate !== null;
                        });
                        $scope.settingsPlugins.forEach(function (plugin) {
                            let ID = 'plugin-settings-' + plugin.identifier;
                            routes[ID] = PLUGIN_STATES[ID] || ID;
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
        $scope.canUseThemeLayout = function() {
            return authService.hasPermission('settings') || authService.hasPermission('device.branding.edit');
        };
        $scope.canManageRoles = function() {
            return authService.isSingleCustomer() || authService.isSuperAdmin();
        };

        $scope.activeTab = openTab;

        $scope.tabTitle = function () {
            if (tabTitleKeys[$scope.activeTab]) {
                return localization.localize(tabTitleKeys[$scope.activeTab]);
            }
            var plugin = $scope.functionsPlugins.concat($scope.settingsPlugins).filter(function (p) {
                return $scope.activeTab === 'plugin-' + p.identifier
                    || $scope.activeTab === 'plugin-settings-' + p.identifier;
            })[0];
            return plugin ? localization.localize(plugin.nameLocalizationKey)
                          : localization.localize('nav.extensions');
        };

        $scope.act = {};
        $scope.act[openTab] = true;

        $scope.functionsPlugins = [];
        $scope.settingsPlugins = [];

        var setActiveTab = function (tabName) {
            $scope.activeTab = tabName;
            $scope.act = {};
            $scope.act[tabName] = true;
        };

        $scope.openTab = function (tabName) {
            if (tabName === $scope.activeTab) {
                return;
            }
            if (!routes[tabName]) {
                return;
            }
            // Without this, the guided tour started on one tab keeps its full-page
            // overlay on top of the next one.
            hintService.stop();
            $scope.navOpen = false;

            var target = routes[tabName];

            // Every sidebar entry maps to a state of its own, so navigating through
            // ui-router is what puts the screen in the address bar and in the browser's
            // history. Previously every tab switch was client-side only: the URL never
            // moved off the landing route, so Back always walked out to Devices no
            // matter which screen you were on.
            if ($state.get(target) && $state.current.name !== target) {
                $state.go(target);
                return;
            }

            // Reached from a plugin screen, which renders inside its parent's state
            // (no state of its own). The target state is the one we are already in, so
            // a transition would be a no-op and the screen would silently not update -
            // switch the view directly instead.
            setActiveTab(tabName);
        };

        // Off-canvas sidebar state, only meaningful below the drawer breakpoint.
        $scope.navOpen = false;
        $scope.toggleNav = function () {
            $scope.navOpen = !$scope.navOpen;
        };
        $scope.closeNav = function () {
            $scope.navOpen = false;
        };

        var listener = $scope.$on('aero_PLUGINS_UPDATED', loadData);
        $scope.$on('$destroy', listener);

        // Menu montado de uma vez so, a partir do registro (item 12 do pedido "modulos":
        // nada de item pulando pra dentro do menu depois que a pagina ja carregou). Ate a
        // primeira resposta chegar, content.html mostra um esqueleto no lugar do <nav>.
        $scope.navReady = false;
        $scope.navSections = [];

        var loadNav = function () {
            moduleRegistry.load().then(function () {
                $scope.navSections = moduleRegistry.visibleSections();
                $scope.navReady = true;
            });
        };

        // Um modulo nativo em manutencao (moduleregistry) nao carrega o template real: o
        // controller dela nem chega a rodar (item 13 do pedido "modulos").
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
            }
        });

        // hintService start is fired by the controllers themselves after they are loaded all required content
//        $timeout(function () {
//            hintService.onStateChangeSuccess();
//        }, 100);

        loadData();
        loadNav();
    });
