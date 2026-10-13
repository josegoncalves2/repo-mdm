// Web Filter plugin: blocking of sites and applications per device configuration (profile).
angular.module('plugin-webfilter', ['ngResource', 'ngSanitize', 'ui.router', 'ncy-angular-breadcrumb'])
    .config(function ($stateProvider) {
        try {
            $stateProvider.state('plugin-webfilter', {
                url: '/plugin-webfilter',
                templateUrl: 'app/components/main/view/content.html?v=h3bb6f5eba8',
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
            getDashboard: {url: base + '/dashboard', method: 'GET'},
            getCatalog: {url: base + '/catalog', method: 'GET'},
            getPolicies: {url: base + '/policies', method: 'GET'},
            getPolicy: {url: base + '/policies/:configurationId', method: 'GET'},
            savePolicy: {url: base + '/policies/:configurationId', method: 'PUT'},
            getApps: {url: base + '/apps', method: 'GET'},
            addApp: {url: base + '/apps', method: 'PUT'},
            removeApp: {url: base + '/apps/:id', method: 'DELETE'},
            getSettings: {url: base + '/settings', method: 'GET'},
            saveSettings: {url: base + '/settings', method: 'PUT'},
            saveSources: {url: base + '/sources/:category', method: 'PUT'},
            saveAttribution: {url: base + '/attribution', method: 'PUT', isArray: false}
        });
    })
    .controller('PluginWebFilterController', function ($scope, $interval, pluginWebFilterService, localization) {
        var LISTS = ['domainAllow', 'domainBlock', 'appAllow', 'appBlock'];

        $scope.activeWfTab = 'dashboard';
        $scope.tabs = [
            {id: 'dashboard', key: 'plugin.webfilter.tab.dashboard'},
            {id: 'policies', key: 'plugin.webfilter.tab.policies'},
            {id: 'apps', key: 'plugin.webfilter.tab.apps'},
            {id: 'blockpage', key: 'plugin.webfilter.tab.blockpage'},
            {id: 'settings', key: 'plugin.webfilter.tab.settings'}
        ];
        $scope.dashboard = {policies: [], devices: [], events: [], events24h: 0, events7d: 0};
        $scope.catalog = {categories: [], protectedPackages: [], attribution: []};
        $scope.policies = [];
        $scope.appCategories = [];
        $scope.settings = {
            dnsDomain: '',
            blockPageTitle: '',
            blockPageMessage: '',
            blockPageLogoUrl: '',
            blockPageSupportText: '',
            blockPageCustomHtml: '',
            blockPageCustomCss: ''
        };
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
            if (id === 'dashboard') {
                loadDashboard();
            }
        };

        var loadDashboard = function () {
            $scope.dashboardLoading = true;
            pluginWebFilterService.getDashboard(function (response) {
                $scope.dashboardLoading = false;
                if (response.status === 'OK') {
                    $scope.dashboard = response.data || $scope.dashboard;
                } else {
                    showErrors(response);
                }
            }, function (response) {
                $scope.dashboardLoading = false;
                onFailure(response);
            });
        };

        $scope.reloadDashboard = function () { loadDashboard(); };
        var refreshTimer = $interval(function () {
            if ($scope.activeWfTab === 'dashboard' && !$scope.dashboardLoading) { loadDashboard(); }
        }, 10000);
        $scope.$on('$destroy', function () { $interval.cancel(refreshTimer); });
        // ------------------------------------------------------------------------------------------------ DNS lists
        // Each category carries its rows ({url, active}) plus, only here, whether it is expanded and has unsaved edits.
        var HTTPS_URL = /^https:\/\/[^\s\/?#]+\S*$/i;
        var HTTP_URL = /^https?:\/\/[^\s\/?#]+\S*$/i;
        $scope.activeSources = function (category) {
            return (category.sources || []).filter(function (s) { return s.active; }).length;
        };
        $scope.toggleSources = function (category) { category.open = !category.open; };
        $scope.markSourcesDirty = function (category) {
            category.dirty = true;
            category.sourceMessage = undefined;
            category.sourceError = undefined;
        };
        $scope.addSource = function (category) {
            category.sources.push({url: '', active: true});
            category.open = true;
            $scope.markSourcesDirty(category);
        };
        $scope.removeSource = function (category, index) {
            category.sources.splice(index, 1);
            $scope.markSourcesDirty(category);
        };
        $scope.invalidSource = function (source) {
            var url = (source.url || '').trim();
            return url.length > 0 && !HTTPS_URL.test(url);
        };
        $scope.invalidAttribution = function (source) {
            var url = (source.url || '').trim();
            return url.length > 0 && !HTTP_URL.test(url);
        };
        var sourceErrorText = function (body) {
            return body && body.message ? localization.localize(body.message) : localization.localize('error.request.failure');
        };
        $scope.saveSources = function (category) {
            // Blank rows are simply dropped
            var rows = category.sources.filter(function (s) { return (s.url || '').trim().length > 0; });
            category.sourceMessage = undefined;
            category.sourceError = undefined;
            if (rows.some($scope.invalidSource)) {
                category.sourceError = 'Use endereços que comecem com https://';
                return;
            }
            category.saving = true;
            pluginWebFilterService.saveSources({category: category.id}, rows.map(function (s) {
                return {url: s.url.trim(), active: !!s.active};
            }), function (response) {
                category.saving = false;
                if (response.status === 'OK') {
                    category.sources = response.data.sources;
                    category.sourceCount = response.data.sourceCount;
                    category.dirty = false;
                    category.sourceMessage = 'Listas salvas. O filtro DNS vai recarregá-las.';
                } else {
                    category.sourceError = sourceErrorText(response);
                }
            }, function (httpResponse) {
                category.saving = false;
                category.sourceError = sourceErrorText(httpResponse && httpResponse.data);
            });
        };
        $scope.addAttribution = function () {
            $scope.catalog.attribution = $scope.catalog.attribution || [];
            $scope.catalog.attribution.push({name: '', license: '', url: ''});
            $scope.attributionDirty = true;
        };
        $scope.removeAttribution = function (index) {
            $scope.catalog.attribution.splice(index, 1);
            $scope.attributionDirty = true;
        };
        $scope.markAttributionDirty = function () {
            $scope.attributionDirty = true;
            $scope.attributionMessage = undefined;
            $scope.attributionError = undefined;
        };
        $scope.saveAttribution = function () {
            $scope.attributionMessage = undefined;
            $scope.attributionError = undefined;
            var rows = ($scope.catalog.attribution || []).filter(function (s) {
                return (s.name || s.license || s.url);
            });
            if (rows.some($scope.invalidAttribution)) {
                $scope.attributionError = 'Use URLs que comecem com http:// ou https://';
                return;
            }
            $scope.attributionSaving = true;
            pluginWebFilterService.saveAttribution(rows, function (response) {
                $scope.attributionSaving = false;
                if (response.status === 'OK') {
                    $scope.catalog.attribution = response.data || [];
                    $scope.attributionDirty = false;
                    $scope.attributionMessage = 'Fontes salvas.';
                } else {
                    $scope.attributionError = sourceErrorText(response);
                }
            }, function (httpResponse) {
                $scope.attributionSaving = false;
                $scope.attributionError = sourceErrorText(httpResponse && httpResponse.data);
            });
        };
        var loadCatalog = function () {
            pluginWebFilterService.getCatalog(function (response) {
                if (response.status === 'OK') {
                    // A reload (e.g. after categorizing an app) keeps what is expanded and any list edit not saved yet
                    var previous = {};
                    ($scope.catalog.categories || []).forEach(function (c) { previous[c.id] = c; });
                    (response.data.categories || []).forEach(function (c) {
                        var old = previous[c.id];
                        if (old) {
                            c.open = old.open;
                            if (old.dirty) {
                                c.sources = old.sources;
                                c.dirty = true;
                            }
                        }
                    });
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
                    loadDashboard();
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
            pluginWebFilterService.saveSettings($scope.settings, function (response) {
                if (response.status === 'OK') {
                    $scope.settings = response.data;
                    $scope.savedDnsDomain = response.data.dnsDomain;
                    $scope.successMessage = localization.localize('plugin.webfilter.saved');
                    loadDashboard();
                } else {
                    showErrors(response);
                }
            }, onFailure);
        };

        loadDashboard();
        loadCatalog();
        loadPolicies();
        loadApps();
        loadSettings();
    })
    .run(function (localization) {
        localization.loadPluginResourceBundles('webfilter');
    });
