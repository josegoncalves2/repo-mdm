// Localization completed
angular.module('headwind-kiosk')
    .controller('RemoteAccessTabController', function ($scope, $document, deviceService,
                                                       remoteSupportService, remoteSupportPlayer, confirmModal,
                                                       alertService, localization, authService, deviceFocusService) {

        var MAX_SEARCH_LENGTH = 100;

        /*
         * Presentation metadata for the remote actions.
         *
         * The set of actions actually offered is NOT taken from here - it comes from
         * GET rest/private/devices/commands, which is the server's own catalog. This map
         * only supplies wording, grouping, the parameter form and the required permission
         * for actions the server reports. An action the server gains without an entry here
         * still shows up (with a label derived from its name) instead of silently vanishing;
         * an action listed here but absent from the server is never rendered, so the panel
         * can no longer offer a button that the backend would reject as unsupported.
         *
         * Parameter names and constraints mirror com.hmdm.service.RemoteCommand#buildPayload.
         */
        var PACKAGE_PATTERN = /^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$/;
        var INTENT_ACTION_PATTERN = /^[a-zA-Z0-9_.]{1,255}$/;

        var pkgField = function () {
            return {
                name: 'pkg',
                labelKey: 'remote.field.pkg',
                placeholder: 'com.example.app',
                required: true,
                pattern: PACKAGE_PATTERN,
                errorKey: 'remote.error.field.pkg'
            };
        };

        var pathField = function () {
            return {
                name: 'path',
                labelKey: 'remote.field.path',
                placeholder: '/sdcard/Download/file.bin',
                required: true,
                maxLength: 1024,
                forbidRelative: true,
                errorKey: 'remote.error.field.path'
            };
        };

        var actionField = function () {
            return {
                name: 'action',
                labelKey: 'remote.field.intent.action',
                placeholder: 'android.intent.action.VIEW',
                required: true,
                pattern: INTENT_ACTION_PATTERN,
                errorKey: 'remote.error.field.intent.action'
            };
        };

        var dataField = function () {
            return {name: 'data', labelKey: 'remote.field.intent.data', required: false, maxLength: 1024};
        };

        var COMMAND_UI = {
            lock_screen: {group: 'session', labelKey: 'button.remote.lock', permission: 'device.lifecycle.lock'},
            unlock_screen: {group: 'session', labelKey: 'button.remote.unlock', permission: 'device.lifecycle.unlock'},
            protect_settings: {group: 'session', labelKey: 'button.remote.protect', permission: 'device.lifecycle.lock'},
            release_settings: {group: 'session', labelKey: 'button.remote.release', permission: 'device.lifecycle.unlock'},
            admin_panel: {group: 'session', labelKey: 'button.remote.adminpanel', permission: 'device.remote_access.control'},
            message: {
                group: 'session', labelKey: 'button.remote.message', permission: 'device.remote_access.control',
                fields: [
                    {name: 'text', labelKey: 'remote.field.text', required: true, maxLength: 500,
                        errorKey: 'remote.error.message.too.long'},
                    {name: 'duration', labelKey: 'remote.field.duration', type: 'number', required: false,
                        defaultValue: 10, min: 1, max: 3600, errorKey: 'remote.error.field.duration'}
                ]
            },

            set_config: {group: 'lifecycle', labelKey: 'button.remote.config', permission: 'device.remote_access.control'},
            grant_permissions: {group: 'lifecycle', labelKey: 'button.remote.grant', permission: 'device.remote_access.control'},
            permissive_mode: {group: 'lifecycle', labelKey: 'button.remote.permissive', permission: 'device.remote_access.control'},
            reboot: {group: 'lifecycle', labelKey: 'button.remote.reboot', permission: 'device.lifecycle.reboot', danger: true},
            wipe: {
                group: 'lifecycle', labelKey: 'button.remote.wipe', permission: 'device.lifecycle.wipe', danger: true,
                fields: [
                    {name: 'externalStorage', labelKey: 'remote.field.externalstorage', type: 'checkbox',
                        required: false, defaultValue: false}
                ]
            },

            run_app: {group: 'apps', labelKey: 'button.remote.runapp', permission: 'device.remote_access.control', fields: [pkgField()]},
            uninstall_app: {group: 'apps', labelKey: 'button.remote.uninstallapp', permission: 'device.remote_access.control', danger: true, fields: [pkgField()]},
            clear_app_data: {group: 'apps', labelKey: 'button.remote.clearappdata', permission: 'device.remote_access.control', danger: true, fields: [pkgField()]},

            delete_file: {group: 'storage', labelKey: 'button.remote.deletefile', permission: 'device.remote_access.control', danger: true, fields: [pathField()]},
            delete_dir: {group: 'storage', labelKey: 'button.remote.deletedir', permission: 'device.remote_access.control', danger: true, fields: [pathField()]},
            purge_dir: {group: 'storage', labelKey: 'button.remote.purgedir', permission: 'device.remote_access.control', danger: true, fields: [pathField()]},
            clear_downloads: {group: 'storage', labelKey: 'button.remote.cleardownloads', permission: 'device.remote_access.control', danger: true},

            run_command: {
                group: 'advanced', labelKey: 'button.remote.runcommand', permission: 'device.remote_access.control', danger: true,
                fields: [{name: 'command', labelKey: 'remote.field.command', required: true, maxLength: 2048,
                    placeholder: 'settings put global adb_enabled 1'}]
            },
            intent: {group: 'advanced', labelKey: 'button.remote.intent', permission: 'device.remote_access.control', fields: [actionField(), dataField()]},
            broadcast: {group: 'advanced', labelKey: 'button.remote.broadcast', permission: 'device.remote_access.control', fields: [actionField(), dataField()]}
        };

        var COMMAND_GROUPS = [
            {id: 'session', titleKey: 'remote.group.session'},
            {id: 'lifecycle', titleKey: 'remote.group.lifecycle'},
            {id: 'apps', titleKey: 'remote.group.apps'},
            {id: 'storage', titleKey: 'remote.group.storage'},
            {id: 'advanced', titleKey: 'remote.group.advanced'}
        ];

        var hasPermission = authService.hasPermission;
        var infrastructureIps = {
            '10.0.17.106': true,
            '10.0.9.1': true,
            '10.1.1.1': true
        };

        var isInfrastructureIp = function (ip) {
            return !!(ip && infrastructureIps[String(ip).trim()]);
        };

        var reportedDeviceIp = function (device) {
            if (!device || !device.info) {
                return null;
            }
            var ip = device.info.deviceIp || device.info.ip || device.info.localIp || null;
            return isInfrastructureIp(ip) ? null : ip;
        };

        $scope.hasPermission = hasPermission;
        $scope.devices = [];
        $scope.loading = false;
        $scope.errorMessage = null;
        $scope.searchValue = '';
        $scope.selectedDevice = null;
        $scope.supportMessage = '';

        /*
         * Estado da sessao de suporte remoto.
         *
         * "connecting" cobre o handshake inteiro: o painel pediu a sessao e o push saiu, mas
         * nenhum video chegou ainda. Esse intervalo nao e' instantaneo -- o tablet precisa
         * receber o chamado e, no Android 14, alguem precisa aceitar o aviso de captura --
         * entao e' um estado proprio, e nao algo dobrado em "ligado" ou "desligado".
         */
        $scope.remote = {
            connecting: false,
            connected: false,
            streaming: false,
            input: false,
            width: 0,
            height: 0,
            fps: 0,
            kbps: 0,
            startedAt: null,
            error: null,
            inputNote: null,
            typing: ''
        };

        $scope.remoteSupported = remoteSupportPlayer.isSupported();

        var hasAnyPermission = function (permissions) {
            return permissions.some(function (permission) {
                return hasPermission(permission);
            });
        };

        $scope.canViewRemote = function () {
            return hasAnyPermission([
                'edit_devices',
                'device.remote_access.view',
                'device.remote_access.control'
            ]);
        };

        $scope.canControlRemote = function () {
            return hasAnyPermission(['edit_devices', 'device.remote_access.control']);
        };

        $scope.canEditKiosk = function () {
            return hasAnyPermission(['edit_devices', 'device.kiosk.edit', 'device.remote_access.control']);
        };

        $scope.canReboot = function () {
            return hasAnyPermission(['edit_devices', 'device.lifecycle.reboot']);
        };

        $scope.canWipe = function () {
            return hasAnyPermission(['edit_devices', 'device.lifecycle.wipe']);
        };

        $scope.canRunAnyCommand = function () {
            return $scope.canControlRemote() || $scope.canEditKiosk() || $scope.canReboot() || $scope.canWipe();
        };

        $scope.getRemoteDeviceIp = function (device) {
            return reportedDeviceIp(device) || localization.localize('devices.ip.not.reported');
        };

        $scope.getRemoteNetworkNote = function (device) {
            if (reportedDeviceIp(device)) {
                return localization.localize('devices.ip.reported.agent');
            }
            if (isInfrastructureIp(device && device.publicIp)) {
                return localization.localize('devices.ip.infra.hidden');
            }
            return localization.localize('devices.ip.not.reported.hint');
        };

        /*
         * The command palette, rebuilt from the server catalog.
         *
         * Until the catalog answers, commandGroups stays empty and the panel says so,
         * rather than showing buttons it cannot honour.
         */
        $scope.commandGroups = [];
        $scope.commandCatalogLoaded = false;
        $scope.commandCatalogError = null;

        var labelForAction = function (action, meta) {
            if (meta && meta.labelKey) {
                var localized = localization.localize(meta.labelKey);
                if (localized && localized !== meta.labelKey) {
                    return localized;
                }
            }
            // An action the server knows and this build has no wording for: show the raw
            // action, humanised. Better a plain label than a missing button.
            return action.replace(/_/g, ' ').replace(/^./, function (c) {
                return c.toUpperCase();
            });
        };

        var buildCommandGroups = function (supportedActions) {
            var byGroup = {};
            COMMAND_GROUPS.forEach(function (group) {
                byGroup[group.id] = {id: group.id, title: localization.localize(group.titleKey), commands: []};
            });
            var extras = {id: 'other', title: localization.localize('remote.group.other'), commands: []};

            supportedActions.forEach(function (action) {
                var meta = COMMAND_UI[action] || {};
                var target = byGroup[meta.group] || extras;
                target.commands.push({
                    action: action,
                    label: labelForAction(action, meta),
                    danger: !!meta.danger,
                    permission: meta.permission || 'device.remote_access.control',
                    fields: (meta.fields || []).map(function (field) {
                        return angular.extend({}, field, {label: localization.localize(field.labelKey)});
                    })
                });
            });

            return COMMAND_GROUPS.map(function (group) {
                return byGroup[group.id];
            }).concat([extras]).filter(function (group) {
                return group.commands.length > 0;
            });
        };

        var loadCommandCatalog = function () {
            deviceService.getSupportedCommands(function (response) {
                if (response.status === 'OK' && angular.isArray(response.data)) {
                    $scope.commandGroups = buildCommandGroups(response.data);
                    $scope.commandCatalogLoaded = true;
                    $scope.commandCatalogError = null;
                } else {
                    $scope.commandCatalogLoaded = true;
                    $scope.commandCatalogError = localization.localize('remote.error.catalog.failed');
                }
            }, function () {
                $scope.commandCatalogLoaded = true;
                $scope.commandCatalogError = localization.localize('remote.error.catalog.failed');
            });
        };

        // A command is offered when the operator holds either its specific permission or the
        // blanket edit_devices, which is exactly how the server authorises it.
        $scope.canRunCommand = function (command) {
            return hasPermission('edit_devices') || hasPermission(command.permission);
        };

        $scope.loadDevices = function (keepSelection) {
            var value = ($scope.searchValue || '').trim();
            if (value.length > MAX_SEARCH_LENGTH) {
                $scope.errorMessage = localization.localize('remote.error.search.too.long');
                return;
            }
            $scope.errorMessage = null;
            $scope.loading = true;
            deviceService.getAllDevices({
                value: value,
                pageNum: 1,
                pageSize: 100,
                sortBy: null,
                sortDir: 'ASC'
            }, function (response) {
                $scope.loading = false;
                if (response.status === 'OK' && response.data && response.data.devices && response.data.devices.items) {
                    // device.online, lastUpdateAgeSec and onlineThresholdSec are computed by the
                    // server from the device's own keepalive interval. The browser used to
                    // re-derive it from a hardcoded 15-minute window, which disagreed with the
                    // server whenever a profile changed its keepalive.
                    $scope.devices = response.data.devices.items;
                    if (keepSelection && $scope.selectedDevice) {
                        var selected = $scope.devices.find(function (device) {
                            return device.id === $scope.selectedDevice.id;
                        });
                        if (selected) {
                            $scope.selectedDevice = selected;
                            return;
                        }
                    }
                    var focusedDevice = deviceFocusService.consume();
                    if (focusedDevice) {
                        var focusMatch = $scope.devices.find(function (device) {
                            return device.id === focusedDevice.id;
                        });
                        if (focusMatch) {
                            $scope.selectDevice(focusMatch);
                            return;
                        }
                    }
                    if (!$scope.selectedDevice && $scope.devices.length > 0) {
                        var firstOnlineDevice = $scope.devices.find(function (device) {
                            return device.online;
                        });
                        $scope.selectDevice(firstOnlineDevice || $scope.devices[0]);
                    }
                } else {
                    $scope.devices = [];
                    $scope.errorMessage = localization.localize('remote.error.load.failed');
                }
            }, function () {
                $scope.loading = false;
                $scope.devices = [];
                $scope.errorMessage = localization.localize('remote.error.load.failed');
            });
        };

        $scope.selectDevice = function (device) {
            if (!device) {
                return;
            }
            // Trocar de aparelho encerra a sessao anterior, em vez de deixar o
            // tablet transmitindo para um visualizador que ninguem esta olhando.
            $scope.stopRemote();
            $scope.selectedDevice = device;
            $scope.cancelCommand();
        };

        /*
         * Parameter form state for the action that is currently armed.
         * Only one action is armed at a time; picking another discards the pending values.
         */
        $scope.pendingCommand = null;
        $scope.pendingParams = {};
        $scope.pendingError = null;

        $scope.startCommand = function (command) {
            if (!$scope.selectedDevice || !$scope.canRunCommand(command)) {
                return;
            }
            $scope.pendingError = null;
            if (!command.fields || command.fields.length === 0) {
                confirmAndSend(command, {});
                return;
            }
            $scope.pendingCommand = command;
            $scope.pendingParams = {};
            command.fields.forEach(function (field) {
                if (field.defaultValue !== undefined) {
                    $scope.pendingParams[field.name] = field.defaultValue;
                }
            });
        };

        $scope.cancelCommand = function () {
            $scope.pendingCommand = null;
            $scope.pendingParams = {};
            $scope.pendingError = null;
        };

        /*
         * Client-side mirror of the server's validation. It exists to give the operator an
         * immediate, specific message instead of a round trip that returns a generic failure;
         * the server still validates everything independently and remains the authority.
         */
        var collectParams = function (command) {
            var params = {};
            for (var i = 0; i < command.fields.length; i++) {
                var field = command.fields[i];
                var raw = $scope.pendingParams[field.name];

                if (field.type === 'checkbox') {
                    params[field.name] = !!raw;
                    continue;
                }

                var value = (raw === undefined || raw === null) ? '' : String(raw).trim();

                if (value.length === 0) {
                    if (field.required) {
                        $scope.pendingError = localization.localize('remote.error.field.required')
                            .replace('${field}', field.label);
                        return null;
                    }
                    continue;
                }
                if (field.maxLength && value.length > field.maxLength) {
                    $scope.pendingError = localization.localize(field.errorKey || 'remote.error.field.invalid')
                        .replace('${field}', field.label);
                    return null;
                }
                if (field.pattern && !field.pattern.test(value)) {
                    $scope.pendingError = localization.localize(field.errorKey || 'remote.error.field.invalid')
                        .replace('${field}', field.label);
                    return null;
                }
                if (field.forbidRelative && value.indexOf('..') >= 0) {
                    $scope.pendingError = localization.localize(field.errorKey || 'remote.error.field.invalid')
                        .replace('${field}', field.label);
                    return null;
                }
                if (field.type === 'number') {
                    var num = parseInt(value, 10);
                    if (isNaN(num) || (field.min !== undefined && num < field.min)
                            || (field.max !== undefined && num > field.max)) {
                        $scope.pendingError = localization.localize(field.errorKey || 'remote.error.field.invalid')
                            .replace('${field}', field.label);
                        return null;
                    }
                    params[field.name] = num;
                    continue;
                }
                params[field.name] = value;
            }
            return params;
        };

        $scope.submitCommand = function () {
            var command = $scope.pendingCommand;
            if (!command) {
                return;
            }
            $scope.pendingError = null;
            var params = collectParams(command);
            if (params === null) {
                return;
            }
            confirmAndSend(command, params);
        };

        var describeFailure = function (response) {
            // error.remote.command.unsupported is returned when the server build does not
            // implement the action. Surfacing the server's own key keeps the message truthful
            // instead of collapsing every failure into "the command failed".
            if (response && response.message) {
                var localized = localization.localize(response.message);
                if (localized && localized !== response.message) {
                    return localized;
                }
                return response.message;
            }
            return localization.localize('remote.error.command.failed');
        };

        var dispatch = function (device, action, params) {
            deviceService.sendCommand({id: device.id}, {
                action: action,
                params: params || {}
            }, function (response) {
                if (response.status === 'OK') {
                    $scope.cancelCommand();
                    alertService.showAlertMessage(localization.localize('success.remote.command'));
                } else {
                    alertService.showAlertMessage(describeFailure(response));
                }
            }, alertService.onRequestFailure);
        };

        var confirmAndSend = function (command, params) {
            var device = $scope.selectedDevice;
            if (!device || !device.id) {
                return;
            }
            var localizedText = localization.localize('question.remote.command')
                .replace('${command}', command.label)
                .replace('${deviceNumber}', device.number);
            confirmModal.getUserConfirmation(localizedText, function () {
                dispatch(device, command.action, params);
            });
        };

        // Kiosk lock has a dedicated endpoint (it re-applies the kiosk policy server-side, not
        // just a push), so it is issued through that endpoint rather than the generic command.
        $scope.forceKiosk = function (device) {
            if (!device || !device.id) {
                return;
            }
            var localizedText = localization.localize('question.remote.command')
                .replace('${command}', localization.localize('button.remote.lock'))
                .replace('${deviceNumber}', device.number);
            confirmModal.getUserConfirmation(localizedText, function () {
                deviceService.forceKiosk({id: device.id}, {}, function (response) {
                    if (response.status === 'OK') {
                        alertService.showAlertMessage(localization.localize('success.remote.command'));
                    } else {
                        alertService.showAlertMessage(describeFailure(response));
                    }
                }, alertService.onRequestFailure);
            });
        };

        $scope.sendRemoteMessage = function (device, messageText) {
            var text = (messageText === undefined || messageText === null) ? '' : String(messageText).trim();
            if (text.length === 0) {
                return;
            }
            if (text.length > 500) {
                alertService.showAlertMessage(localization.localize('remote.error.message.too.long'));
                return;
            }
            var localizedText = localization.localize('question.remote.command')
                .replace('${command}', localization.localize('button.remote.message'))
                .replace('${deviceNumber}', device.number);
            confirmModal.getUserConfirmation(localizedText, function () {
                dispatch(device, 'message', {text: text, duration: 10});
                $scope.supportMessage = '';
            });
        };

        /*
         * ---------------------------------------------------------------------------------
         * Sessao de suporte remoto
         * ---------------------------------------------------------------------------------
         *
         * Pedir a sessao ao servidor devolve duas coisas: uma vaga de relay para o uplink de
         * video do aparelho, e o caminho do socket que este navegador deve assistir. O
         * aparelho e' chamado por push, entao a imagem aparece quando ele atende -- nao
         * quando esta chamada retorna.
         *
         * Quem transmite e' o agente de suporte (com.hwmdm.remote), um aplicativo separado.
         * O launcher continua sendo o hmdm-6.36-os.apk oficial, sem uma linha alterada: ele
         * apenas repassa por broadcast o push que nao conhece, que e' comportamento dele.
         */
        var player = null;

        var canvas = function () {
            return $document[0].getElementById('remote-screen-canvas');
        };

        var resetRemote = function () {
            $scope.remote.connecting = false;
            $scope.remote.connected = false;
            $scope.remote.streaming = false;
            $scope.remote.input = false;
            $scope.remote.width = 0;
            $scope.remote.height = 0;
            $scope.remote.fps = 0;
            $scope.remote.kbps = 0;
            $scope.remote.startedAt = null;
            $scope.remote.inputNote = null;
        };

        $scope.startRemote = function () {
            var device = $scope.selectedDevice;
            if (!device || !device.id || !device.online
                    || $scope.remote.connecting || $scope.remote.connected) {
                return;
            }
            if (!canvas()) {
                return;
            }
            resetRemote();
            $scope.remote.error = null;
            $scope.remote.connecting = true;

            remoteSupportService.start({id: device.id}, {}, function (response) {
                if (response.status !== 'OK' || !response.data || !response.data.socket) {
                    $scope.remote.connecting = false;
                    $scope.remote.error = localization.localize('remote.error.session.start');
                    return;
                }
                if (!$scope.selectedDevice || $scope.selectedDevice.id !== device.id) {
                    // O operador mudou de aparelho enquanto a chamada estava em voo; nao
                    // deixe o tablet transmitindo para um visualizador que nunca vai abrir.
                    remoteSupportService.stop({id: device.id}, {}, angular.noop, angular.noop);
                    return;
                }
                $scope.remote.connected = true;
                $scope.remote.startedAt = new Date();
                openPlayer(response.data.socket, device.id);
            }, function () {
                $scope.remote.connecting = false;
                $scope.remote.error = localization.localize('remote.error.session.start');
            });
        };

        var openPlayer = function (socketPath, deviceId) {
            closePlayer();
            player = remoteSupportPlayer.create(canvas(), {
                onStatus: function (status) {
                    $scope.$applyAsync(function () {
                        $scope.remote.connecting = !status.streaming;
                        $scope.remote.streaming = !!status.streaming;
                        $scope.remote.input = !!status.input;
                        $scope.remote.width = status.width || 0;
                        $scope.remote.height = status.height || 0;
                        if (status.streaming) {
                            $scope.remote.error = null;
                            // Dizer isto antes do primeiro clique frustrado: sem
                            // acessibilidade habilitada no aparelho a tela e' so' vista.
                            $scope.remote.inputNote = status.input
                                ? null
                                : localization.localize('remote.input.unavailable');
                        }
                    });
                },
                onStats: function (stats) {
                    $scope.$applyAsync(function () {
                        $scope.remote.fps = stats.fps;
                        $scope.remote.kbps = stats.kbps;
                    });
                },
                onInputResult: function (result) {
                    if (!result.applied) {
                        $scope.$applyAsync(function () {
                            $scope.remote.inputNote = localization.localize('remote.input.rejected');
                        });
                    }
                },
                onError: function (key) {
                    $scope.$applyAsync(function () {
                        $scope.remote.error = localization.localize(key);
                    });
                },
                onTransport: function (kind) {
                    // O operador precisa saber por qual caminho a imagem esta vindo: HTTP
                    // funciona atras de qualquer proxy, mas tem latencia maior que o socket.
                    $scope.$applyAsync(function () {
                        $scope.remote.transport = kind;
                    });
                },
                onClose: function () {
                    $scope.$applyAsync(function () {
                        $scope.remote.connecting = false;
                        $scope.remote.connected = false;
                        $scope.remote.streaming = false;
                    });
                }
            }, deviceId);
            player.open(socketPath);
        };

        var closePlayer = function () {
            if (player) {
                player.close();
                player = null;
            }
        };

        /**
         * Encerra nas duas pontas. Fechar so' o lado do navegador deixaria o aparelho
         * capturando a propria tela indefinidamente.
         */
        $scope.stopRemote = function () {
            var device = $scope.selectedDevice;
            var wasOpen = $scope.remote.connected || $scope.remote.connecting;
            closePlayer();
            resetRemote();
            if (wasOpen && device && device.id) {
                remoteSupportService.stop({id: device.id}, {}, angular.noop, angular.noop);
            }
        };

        // -----------------------------------------------------------------------------
        // Entrada: o que o operador faz no canvas vira toque no aparelho.
        // -----------------------------------------------------------------------------

        var pressedAt = null;
        var pressedTime = 0;

        // Limiares para separar as tres intencoes num unico ponteiro. Abaixo de 12 px de
        // deslocamento e' clique parado, nao arrasto curto; acima de 500 ms sem sair do
        // lugar e' toque longo, que abre menu de contexto no Android.
        var DRAG_THRESHOLD = 0.02;
        var LONG_PRESS_MS = 500;

        $scope.onScreenDown = function (event) {
            if (!player || !$scope.remote.streaming) {
                return;
            }
            pressedAt = player.toUnit(event);
            pressedTime = Date.now();
        };

        $scope.onScreenUp = function (event) {
            if (!player || !$scope.remote.streaming || !pressedAt) {
                return;
            }
            var released = player.toUnit(event);
            var held = Date.now() - pressedTime;
            var start = pressedAt;
            pressedAt = null;
            if (!released) {
                return;
            }

            var moved = Math.abs(released.x - start.x) + Math.abs(released.y - start.y);
            if (moved > DRAG_THRESHOLD) {
                player.swipe(start.x, start.y, released.x, released.y, Math.max(80, Math.min(held, 2000)));
            } else if (held >= LONG_PRESS_MS) {
                player.longPress(start.x, start.y);
            } else {
                player.tap(start.x, start.y);
            }
        };

        $scope.sendRemoteKey = function (name) {
            if (player && $scope.remote.streaming) {
                player.key(name);
            }
        };

        /*
         * A digitacao vai por um campo proprio, e nao capturando teclas sobre o canvas.
         * O Android aplica o texto no campo em foco de uma vez (ACTION_SET_TEXT); mandar
         * tecla a tecla disputaria com o aplicativo em foco no aparelho. Alem disso, um
         * campo visivel deixa o operador conferir o que vai enviar antes de enviar.
         */
        $scope.sendRemoteText = function () {
            var text = ($scope.remote.typing || '').trim();
            if (!player || !$scope.remote.streaming || text.length === 0) {
                return;
            }
            player.type(text);
            $scope.remote.typing = '';
        };

        $scope.$on('$destroy', function () {
            $scope.stopRemote();
        });

        loadCommandCatalog();
        $scope.loadDevices();
    });
