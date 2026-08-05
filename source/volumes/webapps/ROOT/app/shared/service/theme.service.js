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

        var applyMode = function (mode) {
            root.setAttribute('data-theme', mode === 'dark' ? 'dark' : 'light');
        };

        var currentMode = getStoredMode() === 'dark' ? 'dark' : 'light';
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
                return currentMode;
            },
            applyBrandColors: function (settings) {
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
                if (settings.webSidebarColor && HEX_COLOR.test(settings.webSidebarColor)) {
                    root.style.setProperty('--hwmdm-sidebar-bg', settings.webSidebarColor);
                }
                if (settings.webTextColor && HEX_COLOR.test(settings.webTextColor)) {
                    root.style.setProperty('--hwmdm-sidebar-text', settings.webTextColor);
                    root.style.setProperty('--hwmdm-sidebar-heading', hexToRgba(settings.webTextColor, 0.65));
                    root.style.setProperty('--hwmdm-text', settings.webTextColor);
                }
            }
        };
    });
