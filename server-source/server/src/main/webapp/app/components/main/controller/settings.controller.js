// Localization completed
angular.module('headwind-kiosk')
    .controller('SettingsTabController', function ($scope, $rootScope, $timeout, $uibModal, $window, hintService, settingsService,
                                                   localization, authService, userService, confirmModal, Idle,
                                                   groupService, configurationService, twoFactorAuthService, themeService) {
        $scope.settings = {};
        $scope.userRoleSettings = {};
        $scope.loading = false;

        var userRoleSettings = {};

        $scope.formData = {
            userRoleId: authService.getUser().userRole.id
        };

        var onRequestFailure = function () {
            $scope.loading = false;
            $scope.errorMessage = localization.localize('error.request.failure');
        };

        var clearMessages = function () {
            $scope.successMessage = undefined;
            $scope.errorMessage = undefined;
        };

        var normalizeLocalFileUrl = function (url) {
            if (!url || url.indexOf('/files/') === -1) {
                return url;
            }
            var link = document.createElement('a');
            link.href = url;
            if (!link.hostname) {
                return $window.location.origin + (url.charAt(0) === '/' ? url : '/' + url);
            }
            if (link.hostname === $window.location.hostname && link.port !== $window.location.port) {
                link.protocol = $window.location.protocol;
                link.port = $window.location.port;
                return link.href;
            }
            return url;
        };

        var normalizeDesignSettings = function (settings) {
            if (settings) {
                settings.backgroundImageUrl = normalizeLocalFileUrl(settings.backgroundImageUrl);
            }
        };

        $scope.init = function () {
            $rootScope.settingsTabActive = true;
            $rootScope.pluginsTabActive = false;

            clearMessages();

            $scope.loading = true;

            groupService.getAllGroups(function (response) {
                $scope.groups = response.data;

                configurationService.getAllConfigurations(function (response) {
                    $scope.configurations = response.data;

                    settingsService.getSettings(function (response) {
                        if (response.data) {
                            $scope.settings = response.data;
                            normalizeDesignSettings($scope.settings);
                            $scope.initTwoFactor($scope.settings);
                        }
                        $scope.loading = false;
                    }, onRequestFailure);
                }, onRequestFailure);
            }, onRequestFailure);

        };

        var user = authService.getUser();
        $scope.twoFactor = {
            success: null,
            error: null,
            accepted: user.twoFactorAccepted,
            qrCodeUrl: 'rest/private/twofactor/qr/' + user.id,
            code: ''
        };

        $scope.initTwoFactor = function(settings) {
            $scope.twoFactor.use = settings.twoFactor;
        };

        $scope.twoFactorToggled = function() {
            if (!$scope.twoFactor.use) {
                $scope.twoFactor.use = true;
                confirmModal.getUserConfirmation(localization.localize('form.two.factor.auth.off.confirm'), function () {
                    twoFactorAuthService.reset(function(response) {
                        if (response.status === 'OK') {
                            var user = authService.getUser();
                            user.twoFactorSecret = null;
                            user.twoFactorAccepted = false;
                            authService.update(user);
                            $scope.settings.twoFactor = false;
                            $scope.twoFactor.accepted = false;
                            $scope.twoFactor.code = '';
                            $scope.twoFactor.error = '';
                            $scope.twoFactor.success = localization.localize('form.two.factor.auth.reset');
                            $scope.twoFactor.use = false;
                            $timeout(function () {
                                $scope.twoFactor.success = null;
                            }, 5000);
                        } else {
                            $scope.twoFactor.error = localization.localizeServerResponse(response);
                        }
                    });
                });
            } else {
                // Force QR code to reload and re-generate the secret
                $scope.twoFactor.qrCodeUrl = 'rest/private/twofactor/qr/' + authService.getUser().id +
                    '?' + new Date().getTime();
            }
        };

        $scope.verifyTwoFactor = function() {
            if ($scope.twoFactor.code.length != 6 || !/^\d+$/.test($scope.twoFactor.code)) {
                $scope.twoFactor.error = localization.localize('form.two.factor.auth.code.error');
                return;
            }

            var data = {
                user: authService.getUser().id,
                code: $scope.twoFactor.code
            };
            twoFactorAuthService.verify(data, function (response) {
                if (response.status === 'OK') {
                    var user = authService.getUser();
                    user.twoFactorAccepted = true;
                    authService.update(user);
                    twoFactorAuthService.set(function(response) {
                        if (response.status === 'OK') {
                            $scope.settings.twoFactor = true;
                            $scope.twoFactor.accepted = true;
                            $scope.twoFactor.code = '';
                            $scope.twoFactor.error = '';
                            $scope.twoFactor.success = localization.localize('form.two.factor.auth.set');
                            $timeout(function () {
                                $scope.twoFactor.success = null;
                            }, 5000);
                        } else {
                            $scope.twoFactor.error = localization.localizeServerResponse(response);
                        }
                    });
                } else if (response.status === 'ERROR') {
                    if (response.message === 'error.permission.denied') {
                        $scope.twoFactor.error = localization.localize('form.two.factor.auth.code.invalid');
                    } else {
                        $scope.twoFactor.error = localization.localizeServerResponse(response);
                    }
                }
            });
        };

        $scope.desktopHeaderTemplatePlaceholder = localization.localize('form.configuration.settings.design.desktop.header.template.placeholder') + ' deviceId, description, custom1, custom2, custom3';

        $scope.initDesignSettings = function () {
            $rootScope.settingsTabActive = false;
            $rootScope.pluginsTabActive = false;
            clearMessages();
            $scope.loading = true;

            settingsService.getSettings(function (response) {
                if (response.status === 'OK' && response.data) {
                    $scope.settings = response.data;
                    normalizeDesignSettings($scope.settings);
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
                $scope.loading = false;
            }, onRequestFailure);
        };

        $scope.columnGroups = [
            {
                label: 'Identification',
                columns: [
                    {model: 'columnDisplayedDeviceNumber', labelKey: 'form.settings.common.device.number'},
                    {model: 'columnDisplayedDeviceImei', labelKey: 'form.settings.common.imei'},
                    {model: 'columnDisplayedDevicePhone', labelKey: 'form.settings.common.phone.number'},
                    {model: 'columnDisplayedDeviceModel', labelKey: 'form.settings.common.phone.model'},
                    {model: 'columnDisplayedSerial', labelKey: 'form.settings.common.serial'},
                    {model: 'columnDisplayedDeviceDesc', labelKey: 'form.settings.common.desc'},
                    {model: 'columnDisplayedDeviceGroup', labelKey: 'form.settings.common.group'},
                    {model: 'columnDisplayedDeviceConfiguration', labelKey: 'form.settings.common.config'}
                ]
            },
            {
                label: 'Status & health',
                columns: [
                    {model: 'columnDisplayedDeviceStatus', labelKey: 'form.settings.common.status'},
                    {model: 'columnDisplayedDevicePermissionsStatus', labelKey: 'form.settings.common.status.permissions'},
                    {model: 'columnDisplayedDeviceAppInstallStatus', labelKey: 'form.settings.common.status.installation'},
                    {model: 'columnDisplayedDeviceFilesStatus', labelKey: 'form.settings.common.status.files'},
                    {model: 'columnDisplayedBatteryLevel', labelKey: 'form.settings.common.battery.level'},
                    {model: 'columnDisplayedStorageAvailable', labelKey: 'form.settings.common.storage.available'}
                ]
            },
            {
                label: 'Device info',
                columns: [
                    {model: 'columnDisplayedLauncherVersion', labelKey: 'form.settings.common.launcher.version'},
                    {model: 'columnDisplayedAndroidVersion', labelKey: 'form.settings.common.android.version'},
                    {model: 'columnDisplayedMdmMode', labelKey: 'form.settings.common.mdm.mode'},
                    {model: 'columnDisplayedKioskMode', labelKey: 'form.settings.common.kiosk.mode'},
                    {model: 'columnDisplayedDefaultLauncher', labelKey: 'form.settings.common.default.launcher'},
                    {model: 'columnDisplayedPublicIp', labelKey: 'form.settings.common.publicip'}
                ]
            },
            {
                label: 'Timing',
                columns: [
                    {model: 'columnDisplayedDeviceDate', labelKey: 'form.settings.common.date'},
                    {model: 'columnDisplayedEnrollmentDate', labelKey: 'form.settings.common.enrollment.date'}
                ]
            },
            {
                label: 'Custom fields',
                columns: [
                    {model: 'columnDisplayedCustom1', customProperty: 'customPropertyName1'},
                    {model: 'columnDisplayedCustom2', customProperty: 'customPropertyName2'},
                    {model: 'columnDisplayedCustom3', customProperty: 'customPropertyName3'}
                ]
            }
        ];

        $scope.groupHasVisibleColumns = function (group) {
            for (var i = 0; i < group.columns.length; i++) {
                if (!group.columns[i].customProperty || $scope.settings[group.columns[i].customProperty]) {
                    return true;
                }
            }
            return false;
        };

        $scope.columnLabel = function (col) {
            if (col.customProperty) {
                return $scope.settings[col.customProperty];
            }
            return localization.localize(col.labelKey);
        };

        $scope.setGroupColumns = function (group, value) {
            group.columns.forEach(function (col) {
                if (!col.customProperty || $scope.settings[col.customProperty]) {
                    $scope.userRoleSettings[col.model] = value;
                }
            });
        };

        $scope.countVisibleColumns = function () {
            var total = 0;
            var visible = 0;
            $scope.columnGroups.forEach(function (group) {
                group.columns.forEach(function (col) {
                    if (!col.customProperty || $scope.settings[col.customProperty]) {
                        total++;
                        if ($scope.userRoleSettings[col.model]) {
                            visible++;
                        }
                    }
                });
            });
            return visible + ' / ' + total;
        };

        $scope.initCommonSettings = function () {
            clearMessages();

            var roleId = authService.getUser().userRole.id;
            $scope.loading = true;
            settingsService.getUserRoleSettings({roleId: roleId}, function (response) {
                if (response.status === 'OK') {
                    $scope.userRoleSettings = response.data;
                    userRoleSettings[roleId] = response.data;

                    userService.getUserRoles(function (response) {
                        if (response.status === 'OK') {
                            $scope.userRoles = response.data;
                        } else {
                            $scope.errorMessage = localization.localizeServerResponse(response);
                        }
                        $scope.loading = false;
                    }, onRequestFailure);
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
            }, onRequestFailure);
        };

        $scope.userRoleChanged = function () {
            clearMessages();

            var roleId = $scope.formData.userRoleId;
            if (!userRoleSettings[roleId]) {
                $scope.loading = true;
                settingsService.getUserRoleSettings({roleId: roleId}, function (response) {
                    if (response.status === 'OK') {
                        $scope.userRoleSettings = response.data;
                        userRoleSettings[roleId] = response.data;
                    } else {
                        $scope.errorMessage = localization.localizeServerResponse(response);
                    }
                    $scope.loading = false;
                }, onRequestFailure);
            } else {
                $scope.userRoleSettings = userRoleSettings[roleId];
            }
        };

        $scope.uploadBackground = function () {
            var modalInstance = $uibModal.open({
                templateUrl: 'app/components/main/view/modal/file.html?v=he8add84400',
                // Defined in files.controller.js
                controller: 'FileModalController',
                resolve: {
                    file: function () {
                        return null;
                    }
                }
            });

            modalInstance.result.then(function (data) {
                if (data) {
                    $scope.settings.backgroundImageUrl = normalizeLocalFileUrl(data.url);
                }
            });
        };

        $scope.initPersonalizeSettings = function () {
            $rootScope.settingsTabActive = false;
            $rootScope.pluginsTabActive = false;
            clearMessages();
            $scope.loading = true;

            settingsService.getSettings(function (response) {
                if (response.status === 'OK' && response.data) {
                    $scope.settings = response.data;
                    normalizeDesignSettings($scope.settings);
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
                $scope.loading = false;
            }, onRequestFailure);
        };

        $scope.uploadLogo = function () {
            var modalInstance = $uibModal.open({
                templateUrl: 'app/components/main/view/modal/file.html?v=he8add84400',
                // Defined in files.controller.js
                controller: 'FileModalController',
                resolve: {
                    file: function () {
                        return null;
                    }
                }
            });

            modalInstance.result.then(function (data) {
                if (data) {
                    $scope.settings.webLogoUrl = normalizeLocalFileUrl(data.url);
                }
            });
        };

        $scope.resetWebBranding = function () {
            $scope.settings.webPrimaryColor = null;
            $scope.settings.webSidebarColor = null;
            $scope.settings.webTextColor = null;
            $scope.settings.webLogoUrl = null;
        };

        $scope.saveDefaultDesignSettings = function () {
            clearMessages();
            normalizeDesignSettings($scope.settings);
            settingsService.updateDefaultDesignSettings($scope.settings, function (response) {
                if (response.status === 'OK') {
                    themeService.applyBrandColors($scope.settings);
                    $scope.successMessage = localization.localize('success.settings.design.saved');
                    $timeout(function () {
                        $scope.successMessage = '';
                    }, 2000);
                }
            });
        };

        $scope.saveCommonSettings = function () {
            clearMessages();
            var settings = [];
            for (var p in userRoleSettings) {
                if (userRoleSettings.hasOwnProperty(p)) {
                    settings.push(userRoleSettings[p]);
                }
            }

            settingsService.updateUserRolesCommonSettings(settings, function (response) {
                if (response.status === 'OK') {
                    $scope.successMessage = localization.localize('success.settings.common.saved');
                    $timeout(function () {
                        $scope.successMessage = '';
                    }, 2000);
                    $rootScope.$broadcast('aero_COMMON_SETTINGS_UPDATED', settings);
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
            });
        };

        $scope.saveLanguageSettings = function () {
            clearMessages();

            if ($scope.settings.createNewDevices && !$scope.settings.newDeviceConfigurationId) {
                $scope.errorMessage = localization.localize('error.empty.configuration');
                return;
            }

            if ($scope.settings.idleLogout) {
                Idle.setIdle($scope.settings.idleLogout);
                Idle.setTimeout(10);
                Idle.watch();
            } else {
                $scope.settings.idleLogout = null;  // Change 0 to null
                Idle.unwatch();
            }

            settingsService.updateMiscSettings($scope.settings, function (response) {
                if (response.status === 'OK') {
                    settingsService.updateLanguageSettings($scope.settings, function (response) {
                        if (response.status === 'OK') {
                            $rootScope.$broadcast('aero_LANGUAGE_SETTINGS_UPDATED', $scope.settings);
                            $scope.successMessage = localization.localize('success.settings.saved');
                            $timeout(function () {
                                $scope.successMessage = '';
                            }, 2000);
                        }
                    });
                }
            });
        };

        $scope.enableHints = function () {
            clearMessages();
            hintService.enableHints(function (response) {
                if (response.status === 'OK') {
                    $scope.successMessage = localization.localize('success.settings.hints.enabled');
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
            }, function () {
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        $scope.disableHints = function () {
            clearMessages();
            hintService.disableHints(function (response) {
                if (response.status === 'OK') {
                    $scope.successMessage = localization.localize('success.settings.hints.disabled');
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
            }, function () {
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        $scope.init();

    });
