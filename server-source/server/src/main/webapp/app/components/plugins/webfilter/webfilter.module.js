// Web Filter plugin: blocking of sites and applications per device configuration (profile).
angular.module('plugin-webfilter', ['ngResource', 'ui.router', 'ncy-angular-breadcrumb'])
    .config(function ($stateProvider) {
        try {
            $stateProvider.state('plugin-webfilter', {
                url: '/plugin-webfilter',
                templateUrl: 'app/components/main/view/content.html?v=h1d21823701',
                controller: 'TabController',
                ncyBreadcrumb: {
                    label: '{{"plugin.webfilter.localization.key.name" | localize}}'
                },
                resolve: {
                    openTab: function () {
                        return 'plugin-webfilter';
                    }
                }
            });
        } catch (e) {
            console.log('An error when adding state plugin-webfilter', e);
        }
    })
    .factory('pluginWebFilterService', function ($resource) {
        var base = 'rest/plugins/webfilter/private';
        return $resource('', {}, {
            getCatalog: {url: base + '/catalog', method: 'GET'},
            getPolicies: {url: base + '/policies', method: 'GET'},
            getPolicy: {url: base + '/policies/:configurationId', method: 'GET'},
            savePolicy: {url: base + '/policies/:configurationId', method: 'PUT'},
            getApps: {url: base + '/apps', method: 'GET'},
            addApp: {url: base + '/apps', method: 'PUT'},
            removeApp: {url: base + '/apps/:id', method: 'DELETE'},
            getSettings: {url: base + '/settings', method: 'GET'},
            saveSettings: {url: base + '/settings', method: 'PUT'}
        });
    })
    .controller('PluginWebFilterController', function ($scope, pluginWebFilterService, localization) {
        var LISTS = ['domainAllow', 'domainBlock', 'appAllow', 'appBlock'];

        $scope.activeWfTab = 'policies';
        $scope.tabs = [
            {id: 'policies', key: 'plugin.webfilter.tab.policies'},
            {id: 'apps', key: 'plugin.webfilter.tab.apps'},
            {id: 'settings', key: 'plugin.webfilter.tab.settings'}
        ];
        $scope.catalog = {categories: [], protectedPackages: [], attribution: []};
        $scope.policies = [];
        $scope.appCategories = [];
        $scope.settings = {dnsDomain: ''};
        $scope.editing = null;
        $scope.newApp = {packageName: '', category: ''};

        var clearMessages = function () {
            $scope.errorMessage = undefined;
            $scope.successMessage = undefined;
            $scope.fieldErrors = {};
        };
        clearMessages();

        // The API answers 400/403/404 with the console's JSON envelope in the body
        var onFailure = function (httpResponse) {
            $scope.saving = false;
            if (httpResponse && httpResponse.data && httpResponse.data.status) {
                showErrors(httpResponse.data);
            } else {
                $scope.errorMessage = localization.localize('error.request.failure');
            }
        };

        // Groups the validation errors returned by the server by field, with a readable reason for each value
        var showErrors = function (response) {
            if (response.message === 'plugin.webfilter.error.validation' && angular.isArray(response.data)) {
                $scope.errorMessage = localization.localize('plugin.webfilter.error.validation');
                response.data.forEach(function (e) {
                    ($scope.fieldErrors[e.field] = $scope.fieldErrors[e.field] || []).push(
                        (e.value ? e.value + ': ' : '') + localization.localize(e.code));
                });
            } else {
                $scope.errorMessage = localization.localizeServerResponse(response);
            }
        };

        $scope.categoryName = function (id) {
            return localization.localize('plugin.webfilter.category.' + id);
        };

        $scope.categoryDescription = function (id) {
            return localization.localize('plugin.webfilter.category.' + id + '.desc');
        };

        $scope.selectWfTab = function (id) {
            clearMessages();
            $scope.editing = null;
            $scope.activeWfTab = id;
        };

        var loadCatalog = function () {
            pluginWebFilterService.getCatalog(function (response) {
                if (response.status === 'OK') {
                    $scope.catalog = response.data;
                } else {
                    showErrors(response);
                }
            }, onFailure);
        };

        var loadPolicies = function () {
            $scope.loading = true;
            pluginWebFilterService.getPolicies(function (response) {
                $scope.loading = false;
                if (response.status === 'OK') {
                    $scope.policies = response.data;
                } else {
                    showErrors(response);
                }
            }, onFailure);
        };

        var loadApps = function () {
            pluginWebFilterService.getApps(function (response) {
                if (response.status === 'OK') {
                    $scope.appCategories = response.data;
                }
            }, onFailure);
        };

        var loadSettings = function () {
            pluginWebFilterService.getSettings(function (response) {
                if (response.status === 'OK') {
                    $scope.settings = response.data;
                    $scope.savedDnsDomain = response.data.dnsDomain;
                    $scope.settingsLoaded = true;
                }
            }, onFailure);
        };

        $scope.activePolicies = function () {
            return $scope.policies.filter(function (p) {
                return p.enabled;
            }).length;
        };

        // ------------------------------------------------------------------------------------------------ policies
        $scope.edit = function (policy) {
            clearMessages();
            pluginWebFilterService.getPolicy({configurationId: policy.configurationId}, function (response) {
                if (response.status !== 'OK') {
                    showErrors(response);
                    return;
                }
                var p = response.data;
                var editing = {
                    configurationId: p.configurationId,
                    configurationName: p.configurationName,
                    enabled: p.enabled,
                    categories: {},
                    dnsHost: p.dnsHost,
                    blockedApps: p.blockedApps
                };
                p.categories.forEach(function (c) {
                    editing.categories[c] = true;
                });
                LISTS.forEach(function (l) {
                    editing[l] = (p[l] || []).join('\n');
                });
                $scope.editing = editing;
            }, onFailure);
        };

        $scope.back = function () {
            clearMessages();
            $scope.editing = null;
            loadPolicies();
        };

        var lines = function (text) {
            return (text || '').split(/[\n,;]+/).map(function (s) {
                return s.trim();
            }).filter(function (s) {
                return s.length > 0;
            });
        };

        $scope.save = function () {
            clearMessages();
            var e = $scope.editing;
            var request = {
                configurationId: e.configurationId,
                enabled: e.enabled,
                categories: Object.keys(e.categories).filter(function (c) {
                    return e.categories[c];
                })
            };
            LISTS.forEach(function (l) {
                request[l] = lines(e[l]);
            });
            $scope.saving = true;
            pluginWebFilterService.savePolicy({configurationId: e.configurationId}, request, function (response) {
                $scope.saving = false;
                if (response.status === 'OK') {
                    var p = response.data;
                    LISTS.forEach(function (l) {
                        e[l] = (p[l] || []).join('\n');
                    });
                    e.dnsHost = p.dnsHost;
                    e.blockedApps = p.blockedApps;
                    $scope.successMessage = localization.localize('plugin.webfilter.saved');
                } else {
                    showErrors(response);
                }
            }, onFailure);
        };

        // ------------------------------------------------------------------------------------------------ apps
        $scope.addApp = function () {
            clearMessages();
            pluginWebFilterService.addApp($scope.newApp, function (response) {
                if (response.status === 'OK') {
                    $scope.appCategories = response.data;
                    $scope.newApp = {packageName: '', category: $scope.newApp.category};
                    $scope.successMessage = localization.localize('plugin.webfilter.app.added');
                    loadCatalog();
                } else {
                    showErrors(response);
                }
            }, onFailure);
        };

        $scope.removeApp = function (item) {
            clearMessages();
            pluginWebFilterService.removeApp({id: item.id}, function (response) {
                if (response.status === 'OK') {
                    $scope.appCategories = response.data;
                    $scope.successMessage = localization.localize('plugin.webfilter.app.removed');
                    loadCatalog();
                } else {
                    showErrors(response);
                }
            }, onFailure);
        };

        // ------------------------------------------------------------------------------------------------ settings
        $scope.saveSettings = function () {
            clearMessages();
            pluginWebFilterService.saveSettings({dnsDomain: $scope.settings.dnsDomain}, function (response) {
                if (response.status === 'OK') {
                    $scope.settings = response.data;
                    $scope.savedDnsDomain = response.data.dnsDomain;
                    $scope.successMessage = localization.localize('plugin.webfilter.saved');
                } else {
                    showErrors(response);
                }
            }, onFailure);
        };

        loadCatalog();
        loadPolicies();
        loadApps();
        loadSettings();
    })
    .run(function (localization) {
        localization.loadPluginResourceBundles('webfilter');
    });
