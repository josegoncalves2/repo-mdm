// Localization completed
angular.module('headwind-kiosk')
    .controller('TabController', function ($scope, $rootScope, $timeout, userService, authService, openTab,
                                           pluginService, localization, hintService) {

        $scope.localization = localization;

        // Only tabs that content.html can actually render. LANG/HINTS/PLUGINS used to be
        // listed here without a matching template, so openTab() accepted them and left the
        // content area empty.
        var routes = {
            SUMMARY: 'summary',
            DEVICES: 'main',
            KIOSK: 'kiosk',
            REMOTE: 'remote',
            GPSMAP: 'gpsMap',
            CHAT: 'chat',
            REPORTS: 'REPORTS',
            GOVERNANCE: 'GOVERNANCE',
            APPS: 'applications',
            CONFS: 'configurations',
            FILES: 'files',
            DESIGN: 'designSettings',
            COMMON: 'commonSettings',
            USERS: 'users',
            ROLES: 'roles',
            GROUPS: 'groups',
            ICONS: 'icons',
            GENERAL: 'GENERAL',
            EXTENSIONS: 'EXTENSIONS'
        };

        // Same wording as the sidebar. Shown in the narrow-viewport bar next to the menu
        // button so the drawer being closed never leaves you without a "where am I".
        var tabTitles = {
            SUMMARY: 'Dashboard',
            DEVICES: 'Devices',
            KIOSK: 'Kiosk',
            GPSMAP: 'Location map',
            REMOTE: 'Remote access',
            CHAT: 'Messages',
            REPORTS: 'Reports',
            CONFS: 'Device profiles',
            APPS: 'Applications',
            FILES: 'Files',
            ICONS: 'Launcher icons',
            USERS: 'Panel users',
            ROLES: 'Permissions',
            GROUPS: 'Device groups',
            COMMON: 'Device list columns',
            DESIGN: 'Appearance & branding',
            GENERAL: 'Server defaults',
            EXTENSIONS: 'Plugins',
            GOVERNANCE: 'Backup & restore'
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
            if (tabTitles[$scope.activeTab]) {
                return tabTitles[$scope.activeTab];
            }
            var plugin = $scope.functionsPlugins.concat($scope.settingsPlugins).filter(function (p) {
                return $scope.activeTab === 'plugin-' + p.identifier
                    || $scope.activeTab === 'plugin-settings-' + p.identifier;
            })[0];
            return plugin ? localization.localize(plugin.nameLocalizationKey) : 'Plugins';
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
            // Tab switches are always client-side (setActiveTab), never a ui-router
            // transition. Reason: several tabs (Reports, General,
            // Extensions...) don't have a dedicated state, so they never change
            // $state.current. If a later click tried to $state.transitionTo() a
            // *different* tab that happens to map to the state we technically never
            // left (e.g. 'main'), ui-router treats it as a no-op transition and the
            // screen silently never updates. Keeping ALL tab navigation client-side
            // avoids that trap and also stops the sidebar+content template from being
            // torn down and rebuilt on every click (it lives inside content.html,
            // which is the ui-router templateUrl for every real state).
            if (routes[tabName]) {
                // Tab switches never trigger a ui-router transition, so nothing else would
                // tear down a running guided tour. Without this, the tour started on the
                // Devices tab keeps its full-page overlay on top of every other tab.
                hintService.stop();
                setActiveTab(tabName);
                $scope.navOpen = false;
            }
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
