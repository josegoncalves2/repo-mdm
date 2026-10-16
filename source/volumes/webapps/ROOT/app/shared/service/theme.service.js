// Localization completed
// Tema do painel. Nenhuma cor existe no codigo: todas vem do banco.
//  - webPrimaryColor / webSidebarColor / webTextColor / webLogoUrl: tabela settings do MDM
//  - theme.color-<nome>: hwmdm_system_settings (Aparencia), vira --hwmdm-<nome>
// O servico hwmdm-admin entrega tudo junto em /theme.json, sem login, para que a tela
// de login ja saia com a marca.
angular.module('headwind-kiosk')
    .factory('themeService', function ($window, $document, $rootScope, $q) {
        var HEX_COLOR = /^#([0-9a-f]{3}|[0-9a-f]{6}|[0-9a-f]{8})$/i;
        var COLOR_PREFIX = 'theme.color-';
        var root = $document[0].documentElement;
        var current = {};
        var loaded = $q.defer();

        var adminBase = function () {
            var runtime = $window.HWMDM_RUNTIME || {};
            if (!runtime.adminPort) {
                return null;
            }
            return $window.location.protocol + '//' + $window.location.hostname + ':' + runtime.adminPort;
        };

        var setVar = function (name, value) {
            if (value && HEX_COLOR.test(value)) {
                root.style.setProperty('--hwmdm-' + name, value);
            }
        };

        var apply = function (values) {
            if (!values) {
                return;
            }
            angular.extend(current, values);

            Object.keys(current).forEach(function (key) {
                if (key.indexOf(COLOR_PREFIX) === 0) {
                    setVar(key.substring(COLOR_PREFIX.length), current[key]);
                }
            });

            var accent = current.webPrimaryColor;
            if (accent && HEX_COLOR.test(accent)) {
                setVar('accent', accent);
                setVar('accent-hover', accent);
                root.style.setProperty('--hwmdm-nav-active-bg', 'color-mix(in srgb, ' + accent + ' 16%, transparent)');
                root.style.setProperty('--hwmdm-accent-soft-bg', 'color-mix(in srgb, ' + accent + ' 8%, transparent)');
            }
            setVar('sidebar-bg', current.webSidebarColor);
            setVar('sidebar-text', current.webTextColor);
            setVar('text', current.webTextColor);

            // So mostra o painel quando o tema completo (theme.*) ja chegou do banco.
            if (current['theme.app-name'] !== undefined) {
                $rootScope.appName = current['theme.app-name'];
                root.classList.add('hwmdm-branded');
                loaded.resolve(current);
            }
            $rootScope.$broadcast('HWMDM_THEME_APPLIED', current);
        };

        var load = function () {
            var base = adminBase();
            if (!base) {
                return;
            }
            $window.fetch(base + '/theme.json', {credentials: 'include'})
                .then(function (response) { return response.json(); })
                .then(function (data) { $rootScope.$applyAsync(function () { apply(data); }); });
        };

        // Cor resolvida de uma variavel do tema, para quem desenha fora do CSS
        // (canvas de graficos, mapas, PDF).
        var color = function (name) {
            return $window.getComputedStyle(root).getPropertyValue('--hwmdm-' + name).trim();
        };

        var save = function (values) {
            var base = adminBase();
            var body = Object.keys(values).map(function (key) {
                var value = values[key] === undefined || values[key] === null ? '' : values[key];
                return encodeURIComponent(key) + '=' + encodeURIComponent(value);
            }).join('&');
            return $window.fetch(base + '/theme', {
                method: 'POST',
                credentials: 'include',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: body
            }).then(function () {
                $rootScope.$applyAsync(function () { apply(values); });
            });
        };

        load();

        return {
            applyBrandColors: apply,
            color: color,
            current: function () { return current; },
            colorPrefix: COLOR_PREFIX,
            ready: function () { return loaded.promise; },
            reload: load,
            save: save
        };
    });
