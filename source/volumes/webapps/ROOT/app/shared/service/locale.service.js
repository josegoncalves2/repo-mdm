// Localization completed
angular.module('headwind-kiosk')
    .factory('localization', function ($http, $timeout, settingsService, authService, getBrowserLanguage,
                                       ENGLISH, localizeText, LOCALIZATION_BUNDLES) {

        // Plugin resource bundles are fetched lazily, per plugin and per locale actually in use.
        // registeredPlugins keeps the plugin ids so that a later locale switch can pull the
        // matching bundles without the caller having to re-register anything.
        var registeredPlugins = [];
        // 'pluginId/bundleId' -> true once the bundle has been fetched (or is in flight).
        var requestedBundles = {};

        // Only the fallback bundle and the bundle currently rendered are worth fetching:
        // the UI never displays two locales at once.
        var bundlesInUse = function () {
            return (locale && locale !== ENGLISH) ? [ENGLISH, locale] : [ENGLISH];
        };

        var loadLocalizationBundle = function (pluginId, bundleId) {
            var cacheKey = pluginId + '/' + bundleId;
            if (requestedBundles[cacheKey]) {
                return;
            }
            requestedBundles[cacheKey] = true;

            var bundleUrl = 'app/components/plugins/' + pluginId + '/i18n/' + bundleId + '.json?v=hux202610071600';
            $http.get(bundleUrl).then(function (response) {
                if (response.data && typeof response.data === 'object' && document.localization[bundleId]) {
                    angular.extend(document.localization[bundleId], response.data);
                }
            }, function () {
                // A plugin is not required to ship every locale. When a bundle is absent the
                // built-in strings stay in place, so this is an expected outcome and must not
                // surface as an unhandled rejection in the console.
                requestedBundles[cacheKey] = false;
            });
        };

        var syncPluginBundles = function () {
            var wanted = bundlesInUse();
            registeredPlugins.forEach(function (pluginId) {
                wanted.forEach(function (bundleId) {
                    loadLocalizationBundle(pluginId, bundleId);
                });
            });
        };

        var loadUserLangSettings = function (scope) {
            settingsService.getSettings(function (response) {
                if (response.status === 'OK') {
                    if (response.data) {
                        var settings = response.data;
                        if (settings.useDefaultLanguage) {
                            locale = getBrowserLanguage();
                        } else if (settings.language) {
                            locale = settings.language;
                        } else {
                            locale = ENGLISH;
                        }
                        syncPluginBundles();
                        if (scope) {
                            scope.$emit('aero_LOCALE_CHANGED');
                        }
                    }
                }
            });
        };

        // Determine
        var locale = getBrowserLanguage();
        if (authService.isLoggedIn()) {
            loadUserLangSettings();
        }

        // Find the translations missing in EN bundle
        // localizationObject = document.localization;
        // for ( var prop in localizationObject[ 'ru_RU' ] ) {
        //     if ( !localizationObject[ 'en_US' ] ) {
        //         console.log( prop, ' is missing in en_US' );
        //     }
        // }

        return {
            localize: function (key) {
                return localizeText(locale, key);
            },
            localizeServerResponse: function (response) {
                var key = response.message;
                var value = document.localization[locale][key];
                if (value) {
                    if (response.data) {
                        for (var p in response.data) {
                            if (response.data.hasOwnProperty(p)) {
                                value = value.replace('${' + p + '}', response.data[p]);
                            }
                        }
                    }

                    return value;
                } else {
                    console.error('Message key ', key, ' is missing from I18N resource bundle for locale ', locale);
                    return document.localization[locale]['error.internal.server'];
                }
            },
            getLocale: function () {
                return locale;
            },
            onLangSettingsChange: function (newSettings, scope) {
                if (newSettings.useDefaultLanguage) {
                    locale = getBrowserLanguage();
                } else {
                    locale = newSettings.language;
                }
                syncPluginBundles();
                scope.$emit('aero_LOCALE_CHANGED');
            },
            onLogin: function (scope) {
                loadUserLangSettings(scope);
            },
            loadPluginResourceBundles: function (pluginId) {

                if (registeredPlugins.indexOf(pluginId) < 0) {
                    registeredPlugins.push(pluginId);
                }

                var attempts = 0;
                const waitAndLoadLocalizationBundles = function () {
                    var ready = !!document.localization;
                    for (var i = 0; ready && (i < LOCALIZATION_BUNDLES.length); i++) {
                        ready = ready && (!!document.localization[LOCALIZATION_BUNDLES[i]]);
                    }

                    if ( !ready) {
                        attempts++;

                        var delay = 100 + (attempts % 10) * 100;

                        console.log("Initial resource bundles are NOT loaded yet. Waiting for " + delay + "ms");
                        $timeout(waitAndLoadLocalizationBundles, delay);
                    } else {
                        syncPluginBundles();
                    }
                };

                waitAndLoadLocalizationBundles();

            }
        }
    })
    .directive('localized', function (localization) {
        return {
            restrict: 'A',
            link: function ($scope, element, attrs) {
                var key = element.html();
                var destroyScopeHandler = $scope.$root.$on('aero_LOCALE_CHANGED', function () {
                    element.html(localization.localize(key));
                });
                $scope.$on('$destroy', function () {
                    destroyScopeHandler();
                });
                element.html(localization.localize(key));
            }
        }
    })
    .directive('localizedChangeTracking', function (localization) {
        return {
            restrict: 'A',
            link: function ($scope, element, attrs) {
                var html = element.html();
                var destroyScopeHandler = $scope.$root.$on('aero_LOCALE_CHANGED', function () {
                    element.html(localization.localize(html));
                });
                $scope.$on('$destroy', function () {
                    destroyScopeHandler();
                });
                element.html(localization.localize(html));
            }
        }
    })
    .directive('localizedPlaceholder', function (localization) {
        return {
            restrict: 'A',
            link: function ($scope, element, attrs) {
                element.attr('placeholder', localization.localize(element.attr('localized-placeholder')));
            }
        }
    })
    .directive('localizedTitle', function (localization) {
        return {
            restrict: 'A',
            link: function ($scope, element, attrs) {
                element.attr('title', localization.localize(element.attr('localized-title')));
            }
        }
    })
    .directive('localizedAlt', function (localization) {
        return {
            restrict: 'A',
            link: function ($scope, element, attrs) {
                element.attr('alt', localization.localize(element.attr('localized-alt')));
            }
        }
    })
    .filter('localize', function (localization) {
        return function (key) {
            return localization.localize(key);
        };
    })
;
