// Localization completed
angular.module('headwind-kiosk')
    .controller('TabController', function ($scope, $rootScope, $timeout, $state, userService, authService, openTab,
                                           pluginService, localization, hintService) {

        $scope.localization = localization;

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
            EXTENSIONS: 'extensions'
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
            GOVERNANCE: 'nav.governance'
        };

        var loadData = function () {
            pluginService.getAvailablePlugins(function (response) {
                if (response.status === 'OK') {
                    if (response.data) {
                        // Plugins available for Functions tab
                        $scope.functionsPlugins = response.data.filter(function (plugin) {
                            return plugin.functionsViewTemplate !== undefined && plugin.functionsViewTemplate !== null;
                        });
                        $scope.functionsPlugins.forEach(function (plugin) {
                            let ID = 'plugin-' + plugin.identifier;
                            routes[ID] = ID;
                        });

                        // Plugins available for Settings tab
                        $scope.settingsPlugins = response.data.filter(function (plugin) {
                            return plugin.settingsViewTemplate !== undefined && plugin.settingsViewTemplate !== null;
                        });
                        $scope.settingsPlugins.forEach(function (plugin) {
                            let ID = 'plugin-settings-' + plugin.identifier;
                            routes[ID] = ID;
                        });
                    }
                } else {
                    $scope.functionsPlugins = [];
                    $scope.settingsPlugins = [];
                }
            });
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

        userService.getCurrent(function (response) {
            if (response.data) {
                $scope.currentUser = response.data;
            }
        });

        // hintService start is fired by the controllers themselves after they are loaded all required content
//        $timeout(function () {
//            hintService.onStateChangeSuccess();
//        }, 100);

        loadData();
    });
