// Localization completed
angular.module('headwind-kiosk')
    .factory('deviceFocusService', function () {
        var pending = null;

        return {
            focus: function (device) {
                pending = device || null;
            },
            consume: function () {
                var device = pending;
                pending = null;
                return device;
            }
        };
    });
