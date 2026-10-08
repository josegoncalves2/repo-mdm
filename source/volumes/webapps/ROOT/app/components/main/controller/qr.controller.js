// Localization completed
angular.module('headwind-kiosk')
    .controller('QRController', function ($state, $scope, $window, $stateParams, $http,
                                          localization, hintService, groupService, $timeout, rebranding) {
        $scope.size = Math.min((Math.min($window.innerWidth, $window.innerHeight) * 0.80), 250).toFixed(0);
        $scope.deviceId = $stateParams.deviceId;

        $scope.formData = {
            deviceIdNew: $stateParams.deviceId,
            groups: null,
            // Configuration QR codes are primarily used for fresh device enrollment.
            // If auto-create is off, the QR omits com.hmdm.CONFIG and the launcher
            // performs a blind sync against a device ID that doesn't exist yet,
            // resulting in the generic "failed to connect" flow on reset devices.
            // Keep automatic creation enabled by default for configuration QR
            // screens; admins can still disable it manually if they are enrolling
            // against a pre-created device record.
            create: !$stateParams.deviceId,
            // Default to resolving the device ID from the serial number automatically.
            // Leaving this on the "request" option (the old default) means every device
            // that scans this QR stops on a manual "enter device ID" dialog after setup -
            // not a real zero-touch WiFi+QR enrollment. Admins can still switch it to
            // IMEI or back to manual entry from the dropdown if they need to.
            useId: 'serial'
        };

        $scope.qrCodeKey = $stateParams.qrCode;
        $scope.devices = [];
        $scope.device = {};
        $scope.showQR = true;
        $scope.showHelp = false;
        $scope.helpSize = (Math.min($window.innerWidth, $window.innerHeight) * 0.80).toFixed(0);

        rebranding.query(function(value) {
            $scope.qrCodeHelpLine5 = localization.localize('qrcode.help.line5').replace('${appName}', value.appName);
        });

        $scope.renew = function () {
            $scope.showQR = false;
            $scope.size = Math.min((Math.min($window.innerWidth, $window.innerHeight) * 0.80), 250).toFixed(0);
            $scope.deviceId = $scope.formData.deviceIdNew;
            generateQrUrl();
            if ($scope.jsonData) {
                $scope.generateJson();
            }
            $scope.showQR = true;
        };

        $scope.groupsList = [];

        $scope.groupsSelection = ($scope.formData.groups || []).map(function (group) {
            return {id: group.id};
        });

        groupService.getAllGroups(function (response) {
            $scope.groups = response.data;
            $scope.groupsList = response.data.map(function (group) {
                return {id: group.id, label: group.name};
            });
        });

        $scope.groupsSelectionEvents = {
            onItemSelect: function(item) { $scope.renew(); },
            onItemDeselect: function(item) { $scope.renew(); },
            onSelectAll: function() { $scope.renew(); },
            onDeselectAll: function() { $scope.renew(); }
        };

        var urlPart = function() {
            var res = "";
            if ($scope.formData.deviceIdNew) {
                res += "&deviceId=" + $scope.formData.deviceIdNew;
            }
            if ($scope.formData.useId) {
                res += "&useId=" + $scope.formData.useId;
            }
            if ($scope.formData.create) {
                res += "&create=1";
                for (var i = 0; i < $scope.groupsSelection.length; i++) {
                    var group = $scope.groupsSelection[i];
                    res += "&group=" + encodeURI(group.id);
                }
            }
            return res;
        };

        var generateQrUrl = function() {
            $scope.qrCodeUrl = "rest/public/qr/" + $scope.qrCodeKey + "?size=" + $scope.size;
            if ($scope.deviceId) {
                $scope.qrCodeUrl += "&deviceId=" + $scope.deviceId;
            }
            $scope.qrCodeUrl += urlPart();
        };
        $scope.renew();

        $scope.generateJson = function() {
            var url = "rest/public/qr/json/" + $scope.qrCodeKey + "?" + urlPart().substring(1);
            $http.get(url)
                .then(function (response) {
                    if (response.status === 200) {
                        $scope.jsonData = response.data;
                    }
                });
        };

        $scope.tableFilteringTexts = {
            'buttonDefaultText': localization.localize('table.filtering.no.selected.group'),
            'checkAll': localization.localize('table.filtering.check.all'),
            'uncheckAll': localization.localize('table.filtering.uncheck.all'),
            'dynamicButtonTextSuffix': localization.localize('table.filtering.suffix.group')
        };

        // angular.element($window).bind('resize', function(){
        //     $scope.helpSize = (Math.min($window.innerWidth, $window.innerHeight) * 0.80).toFixed(0);
        // });

        $timeout(function () {
            hintService.onStateChangeSuccess();
        }, 300);

        $scope.helpClicked = function () {
            $scope.showHelp = !$scope.showHelp;
            if ($scope.showHelp) {
                $timeout(function() {
                    $window.scrollTo({
                        'top': 500,
                        'left': 0,
                        'behavior': 'smooth'
                    });
                }, 100);
            }
        };
    });

