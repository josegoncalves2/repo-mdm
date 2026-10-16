// Localization completed
// Nome, fornecedor e logo do painel vem do banco (Aparencia), pelo themeService.
angular.module('headwind-kiosk')
    .factory('rebranding', function ($cookies, themeService) {
        var fromTheme = function (theme) {
            return {
                appName: theme['theme.app-name'] || '',
                vendorName: theme['theme.vendor-name'] || '',
                vendorLink: theme['theme.vendor-link'] || '',
                logoUrl: theme.webLogoUrl || '',
                signupLink: '',
                termsLink: ''
            };
        };

        return {
            query: function(callback) {
                $cookies.remove('rebranding');
                themeService.ready().then(function (theme) {
                    callback(fromTheme(theme));
                });
            }
        }
    })
    .factory('serverRebrandingService', function ($resource) {
        return $resource('', {}, {
            query: {url: 'rest/public/name', method: 'GET'}
        });
    });
