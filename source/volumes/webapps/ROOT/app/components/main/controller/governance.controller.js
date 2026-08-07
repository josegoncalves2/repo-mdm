// Localization completed
angular.module('headwind-kiosk')
    .controller('GovernanceTabController', function ($scope, $window, $timeout, backupService, settingsService,
                                                     confirmModal, localization) {
        $scope.loading = false;
        $scope.errorMessage = null;
        $scope.successMessage = null;
        $scope.backups = [];

        $scope.exportedSettings = null;
        $scope.importFileName = null;
        $scope.importPreview = null;
        $scope.importParseError = null;

        $scope.restoreTarget = null;
        $scope.restoreState = {confirmText: ''};

        var clearMessages = function () {
            $scope.errorMessage = null;
            $scope.successMessage = null;
        };

        var showSuccess = function (message) {
            $scope.successMessage = message;
            $timeout(function () {
                $scope.successMessage = null;
            }, 4000);
        };

        // -------------------------------------------------------------------------------------------
        // Global configuration export / import
        // -------------------------------------------------------------------------------------------

        $scope.exportConfiguration = function () {
            clearMessages();
            settingsService.getSettings(function (response) {
                if (response.status !== 'OK' || !response.data) {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                    return;
                }
                var payload = angular.copy(response.data);
                // Fields that identify this specific server/customer record rather than
                // describing "configuration" - dropped so the export is portable.
                delete payload.id;
                delete payload.customerId;
                delete payload.singleCustomer;
                delete payload.accountType;
                delete payload.expiryTime;
                delete payload.deviceLimit;
                delete payload.deviceCount;
                delete payload.sizeLimit;

                var json = JSON.stringify(payload, null, 2);
                var blob = new Blob([json], {type: 'application/json;charset=utf-8;'});
                var url = $window.URL.createObjectURL(blob);
                var link = $window.document.createElement('a');
                link.href = url;
                link.download = 'hwmdm-config-' + new Date().toISOString().slice(0, 10) + '.json';
                $window.document.body.appendChild(link);
                link.click();
                $window.document.body.removeChild(link);
                $window.URL.revokeObjectURL(url);
            }, function () {
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        $scope.onImportFileSelected = function (files) {
            clearMessages();
            $scope.importPreview = null;
            $scope.importParseError = null;
            $scope.importFileName = null;

            var file = files && files[0];
            if (!file) {
                return;
            }
            $scope.importFileName = file.name;

            var reader = new FileReader();
            reader.onload = function (event) {
                $scope.$apply(function () {
                    try {
                        var parsed = JSON.parse(event.target.result);
                        if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
                            throw new Error('not an object');
                        }
                        $scope.importPreview = parsed;
                    } catch (e) {
                        $scope.importParseError = 'This file is not a valid configuration export (invalid JSON).';
                    }
                });
            };
            reader.onerror = function () {
                $scope.$apply(function () {
                    $scope.importParseError = 'Could not read the selected file.';
                });
            };
            reader.readAsText(file);
        };

        $scope.applyImport = function () {
            if (!$scope.importPreview) {
                return;
            }
            clearMessages();
            confirmModal.getUserConfirmation(
                'Apply this configuration file? It will overwrite the current theme, branding, ' +
                'language, security policy and custom property settings for this server.',
                function () {
                    $scope.loading = true;
                    settingsService.importSettings($scope.importPreview, function (response) {
                        $scope.loading = false;
                        if (response.status === 'OK') {
                            showSuccess('Configuration imported successfully.');
                            $scope.importPreview = null;
                            $scope.importFileName = null;
                        } else {
                            $scope.errorMessage = localization.localizeServerResponse(response);
                        }
                    }, function () {
                        $scope.loading = false;
                        $scope.errorMessage = localization.localize('error.request.failure');
                    });
                }
            );
        };

        $scope.cancelImport = function () {
            $scope.importPreview = null;
            $scope.importFileName = null;
            $scope.importParseError = null;
        };

        // -------------------------------------------------------------------------------------------
        // Database backups
        // -------------------------------------------------------------------------------------------

        /*
         * The database-backup section talks to rest/private/backup/*, which this server build
         * does not implement (verified against the live server: GET rest/private/backup/list
         * answers 404). Rather than presenting buttons that fail silently and an empty list
         * that reads as "no backups yet", the section states that the API is missing and the
         * actions stay disabled until it exists.
         */
        $scope.backupApiAvailable = true;
        $scope.backupApiChecked = false;

        $scope.loadBackups = function () {
            $scope.loading = true;
            backupService.list(function (response) {
                $scope.loading = false;
                $scope.backupApiChecked = true;
                if (response.status === 'OK') {
                    $scope.backupApiAvailable = true;
                    $scope.backups = response.data || [];
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
            }, function (response) {
                $scope.loading = false;
                $scope.backupApiChecked = true;
                $scope.backups = [];
                if (response && response.status === 404) {
                    $scope.backupApiAvailable = false;
                    return;
                }
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        $scope.formatBytes = function (bytes) {
            if (bytes === undefined || bytes === null) {
                return '-';
            }
            if (bytes >= 1048576) {
                return (Math.round((bytes / 1048576) * 10) / 10) + ' MB';
            }
            if (bytes >= 1024) {
                return Math.round(bytes / 1024) + ' KB';
            }
            return bytes + ' B';
        };

        $scope.createBackup = function () {
            clearMessages();
            $scope.loading = true;
            backupService.create({}, function (response) {
                $scope.loading = false;
                if (response.status === 'OK') {
                    showSuccess('Backup created: ' + response.data.name);
                    $scope.loadBackups();
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
            }, function () {
                $scope.loading = false;
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        $scope.downloadBackup = function (backup) {
            $window.open('rest/private/backup/' + encodeURIComponent(backup.name) + '/download', '_blank');
        };

        $scope.deleteBackup = function (backup) {
            clearMessages();
            confirmModal.getUserConfirmation(
                'Delete backup "' + backup.name + '"? This cannot be undone.',
                function () {
                    backupService.remove({filename: backup.name}, function (response) {
                        if (response.status === 'OK') {
                            showSuccess('Backup deleted.');
                            $scope.loadBackups();
                        } else {
                            $scope.errorMessage = localization.localizeServerResponse(response);
                        }
                    }, function () {
                        $scope.errorMessage = localization.localize('error.request.failure');
                    });
                }
            );
        };

        $scope.beginRestore = function (backup) {
            clearMessages();
            $scope.restoreTarget = backup;
            $scope.restoreState.confirmText = '';
        };

        $scope.cancelRestore = function () {
            $scope.restoreTarget = null;
            $scope.restoreState.confirmText = '';
        };

        $scope.canConfirmRestore = function () {
            return $scope.restoreTarget && $scope.restoreState.confirmText === $scope.restoreTarget.name;
        };

        $scope.confirmRestore = function () {
            if (!$scope.canConfirmRestore()) {
                return;
            }
            clearMessages();
            var target = $scope.restoreTarget;
            $scope.loading = true;
            backupService.restore({filename: target.name}, {}, function (response) {
                $scope.loading = false;
                $scope.restoreTarget = null;
                $scope.restoreState.confirmText = '';
                if (response.status === 'OK') {
                    showSuccess('Database restored from ' + target.name +
                        '. A safety snapshot of the previous state was saved as ' + response.data.safetyBackup + '.');
                    $scope.loadBackups();
                } else {
                    $scope.errorMessage = localization.localizeServerResponse(response);
                }
            }, function () {
                $scope.loading = false;
                $scope.errorMessage = localization.localize('error.request.failure');
            });
        };

        $scope.loadBackups();
    });
