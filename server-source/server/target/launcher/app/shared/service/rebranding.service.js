// Localization completed
angular.module('headwind-kiosk')
    .factory('rebranding', function ($cookies, localization, serverRebrandingService) {

        var defaultValue = {
            appName: 'Headwind MDM',
            vendorName: 'Headwind MDM',
            vendorLink: '',
            signupLink: '',
            termsLink: ''
        };

        var fixEmptyValue = function(value) {
            if (value.appName === "") {
                // Empty strings are replaced by default values
                value.appName = localization.localize('app.name');
            }
            if (value.vendorName === "") {
                value.vendorName = localization.localize('app.vendor.name');
            }
            if (value.vendorLink === "") {
                value.vendorLink = localization.localize('app.vendor.link');
            }
            return value;
        };

        return {
            query: function(callback) {
                $cookies.remove('rebranding');
                callback(defaultValue);
            }
        }
    })
    .factory('serverRebrandingService', function ($resource) {
        return $resource('', {}, {
            query: {url: 'rest/public/name', method: 'GET'}
        });
    });
