// Localization completed
angular.module('headwind-kiosk')
    .factory('themeService', function ($window, $document) {
        var STORAGE_KEY = 'hwmdm.theme.mode';
        var HEX_COLOR = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i;
        var root = $document[0].documentElement;

        var getStoredMode = function () {
            try {
                return $window.localStorage.getItem(STORAGE_KEY);
            } catch (e) {
                return null;
            }
        };

        var setStoredMode = function (mode) {
            try {
                $window.localStorage.setItem(STORAGE_KEY, mode);
            } catch (e) {
                // localStorage unavailable (private browsing, disabled storage): theme just won't persist
            }
        };

        var currentMode = getStoredMode() === 'dark' ? 'dark' : 'light';
        var lastBrandSettings = null;

        // Chart.js paints straight onto a canvas, so no stylesheet can reach its
        // axis labels and grid lines. They are read from Chart.defaults at draw
        // time, which is why they have to be re-pointed whenever the mode flips.
        var applyChartDefaults = function (mode) {
            if (!$window.Chart || !$window.Chart.defaults || !$window.Chart.defaults.global) {
                return;
            }
            var dark = mode === 'dark';
            $window.Chart.defaults.global.defaultFontColor = dark ? '#97a3b1' : '#667085';
            var scale = $window.Chart.defaults.scale;
            if (scale) {
                var line = dark ? 'rgba(223, 230, 236, 0.12)' : 'rgba(23, 32, 42, 0.1)';
                scale.gridLines.color = line;
                scale.gridLines.zeroLineColor = line;
                scale.ticks.fontColor = dark ? '#97a3b1' : '#667085';
            }
        };

        var applyMode = function (mode) {
            root.setAttribute('data-theme', mode === 'dark' ? 'dark' : 'light');
            applyChartDefaults(mode);
        };

        applyMode(currentMode);

        var expandHex = function (hex) {
            if (hex.length === 4) {
                return '#' + hex[1] + hex[1] + hex[2] + hex[2] + hex[3] + hex[3];
            }
            return hex;
        };

        var hexToRgba = function (hex, alpha) {
            var full = expandHex(hex);
            var r = parseInt(full.substr(1, 2), 16);
            var g = parseInt(full.substr(3, 2), 16);
            var b = parseInt(full.substr(5, 2), 16);
            return 'rgba(' + r + ', ' + g + ', ' + b + ', ' + alpha + ')';
        };

        // Brand colours are written as inline custom properties on <html>, which
        // outranks every [data-theme="dark"] rule in the stylesheet. The accent is
        // a brand mark and applies to both modes, but the panel background and body
        // text colours are picked by the admin against the LIGHT panel: forcing them
        // in dark mode is what produced a light sidebar with light-on-light labels
        // and near-black headings on the dark canvas. In dark mode those two are
        // left to the stylesheet, and re-applied as soon as the user switches back.
        var applyBrandColors = function (settings) {
            if (settings) {
                lastBrandSettings = settings;
            }
            settings = lastBrandSettings;
            if (!settings) {
                return;
            }
            if (settings.webPrimaryColor && HEX_COLOR.test(settings.webPrimaryColor)) {
                root.style.setProperty('--hwmdm-accent', settings.webPrimaryColor);
                root.style.setProperty('--hwmdm-accent-hover', settings.webPrimaryColor);
                root.style.setProperty('--hwmdm-link', settings.webPrimaryColor);
                // The "selected/hovered nav item" highlight is derived from the accent
                // color itself (a soft tint), so picking one accent color consistently
                // themes buttons, links AND the active/hover navigation state together.
                root.style.setProperty('--hwmdm-nav-active-bg', hexToRgba(settings.webPrimaryColor, 0.16));
                root.style.setProperty('--hwmdm-accent-soft-bg', hexToRgba(settings.webPrimaryColor, 0.08));
            }

            var lightOnly = ['--hwmdm-sidebar-bg', '--hwmdm-sidebar-text', '--hwmdm-sidebar-heading', '--hwmdm-text'];
            if (currentMode === 'dark') {
                lightOnly.forEach(function (name) {
                    root.style.removeProperty(name);
                });
                return;
            }

            if (settings.webSidebarColor && HEX_COLOR.test(settings.webSidebarColor)) {
                root.style.setProperty('--hwmdm-sidebar-bg', settings.webSidebarColor);
            }
            if (settings.webTextColor && HEX_COLOR.test(settings.webTextColor)) {
                root.style.setProperty('--hwmdm-sidebar-text', settings.webTextColor);
                root.style.setProperty('--hwmdm-sidebar-heading', hexToRgba(settings.webTextColor, 0.65));
                root.style.setProperty('--hwmdm-text', settings.webTextColor);
            }
        };

        return {
            getMode: function () {
                return currentMode;
            },
            isDark: function () {
                return currentMode === 'dark';
            },
            toggle: function () {
                currentMode = currentMode === 'dark' ? 'light' : 'dark';
                applyMode(currentMode);
                setStoredMode(currentMode);
                applyBrandColors();
                return currentMode;
            },
            applyBrandColors: applyBrandColors
        };
    });
