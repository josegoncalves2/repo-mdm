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
        var THEME_MODE_KEY = 'hwmdm.theme.mode';
        var root = $document[0].documentElement;
        var current = {};
        var currentMode = 'light'; // 'light' ou 'dark'
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

            // If dark mode is active, override colors with dark theme values
            if (currentMode === 'dark') {
                root.style.setProperty('--hwmdm-bg', '#1a1d23');
                root.style.setProperty('--hwmdm-surface', '#252a33');
                root.style.setProperty('--hwmdm-surface-hover', '#2f3441');
                root.style.setProperty('--hwmdm-surface-subtle', '#32394a');
                root.style.setProperty('--hwmdm-border', '#444c5c');
                root.style.setProperty('--hwmdm-border-subtle', '#3a4250');
                root.style.setProperty('--hwmdm-border-input', '#4a525f');
                root.style.setProperty('--hwmdm-text', '#e0e3e8');
                root.style.setProperty('--hwmdm-text-strong', '#f5f6f8');
                root.style.setProperty('--hwmdm-muted', '#9099a6');
                root.style.setProperty('--hwmdm-muted-soft', '#707a88');
                root.style.setProperty('--hwmdm-shadow', 'rgba(0, 0, 0, 0.3)');
                root.style.setProperty('--hwmdm-overlay', 'rgba(0, 0, 0, 0.4)');
                root.style.setProperty('--hwmdm-danger-bg', '#3c1f1a');
                root.style.setProperty('--hwmdm-success-bg', '#1a3a31');
                root.style.setProperty('--hwmdm-info-bg', '#1a2d4a');
                root.style.setProperty('--hwmdm-warn-bg', '#3a3015');
            } else {
                Object.keys(current).forEach(function (key) {
                    if (key.indexOf(COLOR_PREFIX) === 0) {
                        setVar(key.substring(COLOR_PREFIX.length), current[key]);
                    }
                });
            }

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

        // Lido da mesma origem do painel: funciona pelo endereco interno e pelo externo.
        var load = function () {
            $window.fetch('theme.json?t=' + Date.now(), {cache: 'no-store'})
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

        var loadThemeMode = function () {
            try {
                var saved = $window.localStorage.getItem(THEME_MODE_KEY);
                currentMode = (saved === 'dark') ? 'dark' : 'light';
            } catch (e) {
                currentMode = 'light';
            }
            applyThemeMode();
        };

        var applyThemeMode = function () {
            if (currentMode === 'dark') {
                root.setAttribute('data-theme', 'dark');
            } else {
                root.removeAttribute('data-theme');
            }
            $rootScope.$broadcast('HWMDM_THEME_MODE_CHANGED', currentMode);
        };

        var toggleThemeMode = function () {
            currentMode = (currentMode === 'dark') ? 'light' : 'dark';
            try {
                $window.localStorage.setItem(THEME_MODE_KEY, currentMode);
            } catch (e) {}
            applyThemeMode();
            apply(current);
            return currentMode;
        };

        var getThemeMode = function () {
            return currentMode;
        };

        load();
        loadThemeMode();

        return {
            applyBrandColors: apply,
            color: color,
            current: function () { return current; },
            colorPrefix: COLOR_PREFIX,
            ready: function () { return loaded.promise; },
            reload: load,
            save: save,
            toggleMode: toggleThemeMode,
            getMode: getThemeMode,
            setMode: function (mode) {
                currentMode = (mode === 'dark') ? 'dark' : 'light';
                try {
                    $window.localStorage.setItem(THEME_MODE_KEY, currentMode);
                } catch (e) {}
                applyThemeMode();
            }
        };
    });
