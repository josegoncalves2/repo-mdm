/*
 * HWMDM 1.0 - registro unico de modulos do console (pacote de trabalho "modulos").
 *
 * Um so lugar sabe: quais modulos existem, em que secao do menu cada um aparece, com que
 * icone/rotulo/descricao, se e nativo ou extensao (plugin), se pode ser desligado sem parar
 * o resto (e por qual dos dois mecanismos do servidor), e se esta em manutencao agora.
 *
 * Os predicados de visibilidade abaixo sao os MESMOS que estavam escritos a mao em cada
 * ng-if de content.html antes deste pacote — só foram movidos para cá, sem afrouxar nem
 * apertar nenhum.
 *
 * Dois mecanismos de manutencao distintos, os dois ja existentes no nucleo:
 *   - manageVia: 'native' -> plugin moduleregistry (rest/plugins/moduleregistry/private/*).
 *     Cobre as telas nativas e os dois itens de menu que, apesar de tecnicamente serem
 *     plugins, ja estao na lista fechada (KNOWN_MODULE_IDS) do proprio moduleregistry:
 *     Web Filter (plugin-webfilter) e Mensagens (CHAT, que reaproveita o plugin messaging).
 *     Antes de o moduleregistry ser publicado no servidor, toda consulta de estado volta
 *     404: nesse caso TODO modulo nativo fica "ligado" e os interruptores ficam desabilitados
 *     com o aviso "Servico de manutencao de modulos indisponivel", nunca com um erro solto.
 *   - manageVia: 'plugin' -> API ja existente do nucleo (rest/plugin/main/private/*), a
 *     mesma que a extinta tela "Module maintenance" usava. Cobre as extensoes que nao tem id
 *     proprio no moduleregistry: Logs (devicelog), Auditoria (audit), Push (push) e
 *     Informacoes detalhadas (deviceinfo). Essa via tem imposicao real no servidor: o
 *     PluginAccessFilter devolve 404 pro REST do plugin desligado.
 *   - manageVia: null -> tela de administracao da propria plataforma (Modulos, Integracoes).
 *     Nao tem id no moduleregistry de proposito: quem desliga modulo nao pode se trancar do
 *     lado de fora. Fica sempre visivel para quem pode gerenciar.
 *
 * O plugin "messaging" continua existindo no servidor (fornece o back-end de Mensagens e a
 * tela de configuracoes "purgar mensagens antigas"), mas NAO tem entrada propria aqui: a
 * tela de funcoes dele (plugin-messaging) duplicava a tela nativa CHAT, que e' a que fica.
 * A configuracao dele (purga) e' alcancada pelo botao "Configurar" da linha Mensagens.
 */
angular.module('headwind-kiosk')
    .factory('moduleRegistry', function ($resource, $q, $rootScope, $timeout, authService, pluginService, localization) {

        var moduleRegistryApi = $resource('', {}, {
            getState: {url: 'rest/plugins/moduleregistry/private/state', method: 'GET'},
            toggleModule: {url: 'rest/plugins/moduleregistry/private/toggle', method: 'POST'}
        });

        // Ids essenciais no proprio moduleregistry do servidor (DEVICES, USERS, GENERAL) mais
        // as duas telas de administracao da plataforma, que nao tem id la (nunca desligaveis
        // por definicao: sem elas ninguem liga mais nada de volta).
        var ESSENTIAL_IDS = {DEVICES: true, USERS: true, GENERAL: true, EXTENSIONS: true, INTEGRATIONS: true, SERVER: true};

        var canUseThemeLayout = function () {
            return authService.hasPermission('settings') || authService.hasPermission('device.branding.edit');
        };
        var canManageRoles = function () {
            return authService.isSingleCustomer() || authService.isSuperAdmin();
        };

        // section: chave usada para agrupar (bate com nav.group.<section> em hwmdm_modules.js).
        // Ordem das secoes = ordem no menu. Cada secao responde a UMA pergunta so, para o
        // item 14 do pedido ("existe logica nos menus, e nao uma salada mista de opcoes").
        var SECTIONS = [
            {id: 'operation', labelKey: 'nav.group.operation'},
            {id: 'policies', labelKey: 'nav.group.policies'},
            {id: 'content', labelKey: 'nav.group.content'},
            {id: 'diagnostics', labelKey: 'nav.group.diagnostics'},
            {id: 'admin', labelKey: 'nav.group.admin'},
            {id: 'platform', labelKey: 'nav.group.platform'}
        ];

        var CATALOG = [
            // --- Operacao: o que a frota esta fazendo agora -----------------------------
            {
                id: 'SUMMARY', section: 'operation', icon: 'layout-dashboard', testId: 'summary',
                labelKey: 'nav.dashboard', descKey: 'modules.desc.SUMMARY',
                type: 'native', manageVia: 'native', essential: false,
                visible: function () { return true; }
            },
            {
                id: 'DEVICES', section: 'operation', icon: 'smartphone', testId: 'devices',
                labelKey: 'nav.devices', descKey: 'modules.desc.DEVICES',
                type: 'native', manageVia: 'native', essential: true,
                visible: function () { return true; }
            },
            {
                id: 'REMOTE', section: 'operation', icon: 'monitor-smartphone', testId: 'remote',
                labelKey: 'nav.remote', descKey: 'modules.desc.REMOTE',
                type: 'native', manageVia: 'native', essential: false,
                visible: function (a) {
                    return a.hasPermission('edit_devices') || a.hasPermission('device.remote_access.view') ||
                        a.hasPermission('device.remote_access.control');
                }
            },
            {
                id: 'GPSMAP', section: 'operation', icon: 'map-pin', testId: 'gpsmap',
                labelKey: 'nav.gpsmap', descKey: 'modules.desc.GPSMAP',
                type: 'native', manageVia: 'native', essential: false,
                visible: function (a) {
                    return a.hasPermission('edit_devices') || a.hasPermission('device.gps.view') ||
                        a.hasPermission('plugin_devicelocations_access');
                }
            },
            {
                id: 'CHAT', section: 'operation', icon: 'message-square', testId: 'chat',
                labelKey: 'nav.chat', descKey: 'modules.desc.CHAT',
                type: 'native', manageVia: 'native', essential: false,
                backingPlugin: 'messaging',
                visible: function (a) {
                    return a.hasPermission('edit_devices') || a.hasPermission('plugin_messaging_send') ||
                        a.hasPermission('plugin_messaging_delete') || a.hasPermission('device.remote_access.control');
                }
            },
            {
                id: 'REPORTS', section: 'operation', icon: 'bar-chart-3', testId: 'reports',
                labelKey: 'nav.reports', descKey: 'modules.desc.REPORTS',
                type: 'native', manageVia: 'native', essential: false,
                visible: function () { return true; }
            },

            // --- Politicas e seguranca: o que e imposto nos aparelhos --------------------
            {
                id: 'CONFS', section: 'policies', icon: 'layers', testId: 'confs',
                labelKey: 'nav.configurations', descKey: 'modules.desc.CONFS',
                type: 'native', manageVia: 'native', essential: false,
                visible: function (a) { return a.hasPermission('configurations'); }
            },
            {
                id: 'KIOSK', section: 'policies', icon: 'lock', testId: 'kiosk',
                labelKey: 'nav.kiosk', descKey: 'modules.desc.KIOSK',
                type: 'native', manageVia: 'native', essential: false,
                visible: function (a) {
                    return a.hasPermission('configurations') || a.hasPermission('edit_devices') ||
                        a.hasPermission('device.remote_access.control') || a.hasPermission('device.kiosk.edit');
                }
            },
            {
                id: 'plugin-webfilter', section: 'policies', icon: 'filter', testId: 'webfilter',
                labelKey: 'plugin.webfilter.localization.key.name', descKey: 'modules.desc.plugin-webfilter',
                type: 'extension', manageVia: 'native', essential: false,
                backingPlugin: 'webfilter',
                visible: function (a) { return a.hasPermission('plugin_webfilter_access'); }
            },

            // --- Conteudo: o que e entregue aos aparelhos ---------------------------------
            {
                id: 'APPS', section: 'content', icon: 'package', testId: 'apps',
                labelKey: 'nav.applications', descKey: 'modules.desc.APPS',
                type: 'native', manageVia: 'native', essential: false,
                visible: function (a) { return a.hasPermission('applications'); }
            },
            {
                id: 'FILES', section: 'content', icon: 'folder', testId: 'files',
                labelKey: 'nav.files', descKey: 'modules.desc.FILES',
                type: 'native', manageVia: 'native', essential: false,
                visible: function (a) { return a.hasPermission('files'); }
            },
            {
                id: 'ICONS', section: 'content', icon: 'image', testId: 'icons',
                labelKey: 'nav.icons', descKey: 'modules.desc.ICONS',
                type: 'native', manageVia: 'native', essential: false,
                visible: function (a) { return a.hasPermission('settings'); }
            },

            // --- Diagnostico: telas que funcionam sozinhas, sem partir de 1 aparelho -----
            // (audit/devicelog/messaging/push/deviceinfo tem busca de dispositivo dentro da
            // propria tela; nenhuma delas depende de estado vindo de outra tela.)
            {
                id: 'plugin-devicelog', section: 'diagnostics', icon: 'scroll-text', testId: 'devicelog',
                labelKey: 'plugin.devicelog.localization.key.name', descKey: 'modules.desc.plugin-devicelog',
                type: 'extension', manageVia: 'plugin', essential: false,
                backingPlugin: 'devicelog',
                visible: function (a) { return a.hasPermission('plugin_devicelog_access'); }
            },
            {
                id: 'plugin-audit', section: 'diagnostics', icon: 'shield-check', testId: 'audit',
                labelKey: 'plugin.audit.localization.key.name', descKey: 'modules.desc.plugin-audit',
                type: 'extension', manageVia: 'plugin', essential: false,
                backingPlugin: 'audit',
                visible: function (a) { return a.hasPermission('plugin_audit_access'); }
            },
            {
                id: 'plugin-push', section: 'diagnostics', icon: 'send', testId: 'push',
                labelKey: 'plugin.push.localization.key.name', descKey: 'modules.desc.plugin-push',
                type: 'extension', manageVia: 'plugin', essential: false,
                backingPlugin: 'push',
                visible: function (a) { return a.hasPermission('plugin_push_send'); }
            },
            {
                id: 'plugin-deviceinfo', section: 'diagnostics', icon: 'info', testId: 'deviceinfo',
                labelKey: 'plugin.deviceinfo.localization.key.name', descKey: 'modules.desc.plugin-deviceinfo',
                type: 'extension', manageVia: 'plugin', essential: false,
                backingPlugin: 'deviceinfo',
                visible: function (a) { return a.hasPermission('plugin_deviceinfo_access'); }
            },

            // --- Administracao: quem usa o painel e como o servidor se comporta ----------
            {
                id: 'USERS', section: 'admin', icon: 'users', testId: 'users',
                labelKey: 'nav.users', descKey: 'modules.desc.USERS',
                type: 'native', manageVia: 'native', essential: true,
                visible: function (a) { return a.hasPermission('settings'); }
            },
            {
                id: 'ROLES', section: 'admin', icon: 'shield', testId: 'roles',
                labelKey: 'nav.roles', descKey: 'modules.desc.ROLES',
                type: 'native', manageVia: 'native', essential: false,
                visible: function (a) { return a.hasPermission('settings') && canManageRoles(); }
            },
            {
                id: 'GROUPS', section: 'admin', icon: 'boxes', testId: 'groups',
                labelKey: 'nav.groups', descKey: 'modules.desc.GROUPS',
                type: 'native', manageVia: 'native', essential: false,
                visible: function (a) { return a.hasPermission('settings'); }
            },
            {
                id: 'COMMON', section: 'admin', icon: 'columns-3', testId: 'common',
                labelKey: 'nav.common', descKey: 'modules.desc.COMMON',
                type: 'native', manageVia: 'native', essential: false,
                visible: function (a) { return a.hasPermission('settings'); }
            },
            {
                id: 'DESIGN', section: 'admin', icon: 'palette', testId: 'design',
                labelKey: 'nav.design', descKey: 'modules.desc.DESIGN',
                type: 'native', manageVia: 'native', essential: false,
                visible: function (a) { return canUseThemeLayout() || a.hasPermission('settings'); }
            },
            {
                id: 'GENERAL', section: 'admin', icon: 'settings', testId: 'general',
                labelKey: 'nav.general', descKey: 'modules.desc.GENERAL',
                type: 'native', manageVia: 'native', essential: true,
                visible: function (a) { return a.hasPermission('settings'); }
            },
            {
                id: 'GOVERNANCE', section: 'admin', icon: 'database', testId: 'governance',
                labelKey: 'nav.governance', descKey: 'modules.desc.GOVERNANCE',
                type: 'native', manageVia: 'native', essential: false,
                visible: function () { return true; }
            },

            {
                id: 'SERVER', section: 'admin', icon: 'settings', testId: 'server',
                labelKey: 'nav.server', descKey: 'modules.desc.SERVER',
                type: 'native', manageVia: 'native', essential: true,
                visible: function (a) { return a.hasPermission('settings'); }
            },

            // --- Plataforma: manutencao dos proprios modulos e integracoes ---------------
            {
                id: 'EXTENSIONS', section: 'platform', icon: 'puzzle', testId: 'extensions',
                labelKey: 'nav.extensions', descKey: 'modules.desc.EXTENSIONS',
                type: 'native', manageVia: null, essential: true,
                visible: function () { return true; }
            },
            {
                id: 'INTEGRATIONS', section: 'platform', icon: 'plug', testId: 'integrations',
                labelKey: 'nav.integrations', descKey: 'modules.desc.INTEGRATIONS',
                type: 'native', manageVia: null, essential: true,
                visible: function (a) { return a.hasPermission('settings'); }
            }
        ];

        var byId = {};
        CATALOG.forEach(function (m) { byId[m.id] = m; });

        // --- estado carregado do servidor --------------------------------------------
        var nativeState = {};          // moduleId -> {disabled, disabledBy, disabledAt, reason, essential}
        var nativeStateAvailable = false;  // false ate a 1a resposta OK (ou enquanto o plugin nao existir no servidor)
        var pluginsById = {};           // identifier -> {id, identifier, settingsViewTemplate, settingsPermission, ...}
        var activePluginIdentifiers = {};  // identifier -> true quando habilitado para o customer atual
        var loaded = false;
        var loadPromise = null;

        var loadNativeState = function () {
            var deferred = $q.defer();
            moduleRegistryApi.getState({}, function (response) {
                nativeState = {};
                if (response.status === 'OK' && response.data) {
                    response.data.forEach(function (s) { nativeState[s.moduleId] = s; });
                    nativeStateAvailable = true;
                } else {
                    nativeStateAvailable = false;
                }
                deferred.resolve();
            }, function () {
                // Endpoint 404 (moduleregistry ainda nao publicado) ou qualquer outra falha:
                // trata como "todo mundo ligado", sem barulho no console.
                nativeState = {};
                nativeStateAvailable = false;
                deferred.resolve();
            });
            return deferred.promise;
        };

        var loadPlugins = function () {
            var deferred = $q.defer();
            pluginService.getActivePlugins(function (activeResponse) {
                pluginsById = {};
                if (activeResponse.status === 'OK' && activeResponse.data) {
                    activeResponse.data.forEach(function (p) { pluginsById[p.identifier] = p; });
                }
                pluginService.getAvailablePlugins(function (availableResponse) {
                    activePluginIdentifiers = {};
                    if (availableResponse.status === 'OK' && availableResponse.data) {
                        availableResponse.data.forEach(function (p) { activePluginIdentifiers[p.identifier] = true; });
                    }
                    deferred.resolve();
                }, function () {
                    activePluginIdentifiers = {};
                    deferred.resolve();
                });
            }, function () {
                pluginsById = {};
                activePluginIdentifiers = {};
                deferred.resolve();
            });
            return deferred.promise;
        };

        var load = function (force) {
            if (loadPromise && !force) {
                return loadPromise;
            }
            // O interceptor de 403 devolve uma promise que nunca resolve; sem o limite o menu
            // ficaria no esqueleto ate um Ctrl+F5.
            var timeout = $timeout(angular.noop, 8000);
            var real = $q.all([loadNativeState(), loadPlugins()]);
            real.then(function () { $rootScope.$emit('aero_MODULES_LOADED'); });
            loadPromise = $q.race([real, timeout]).then(function () {
                $timeout.cancel(timeout);
                loaded = true;
            });
            return loadPromise;
        };

        $rootScope.$on('aero_USER_AUTHENTICATED', function () { loadPromise = null; });
        $rootScope.$on('aero_USER_LOGOUT', function () { loadPromise = null; });

        var isPluginInstalled = function (identifier) {
            return !!pluginsById[identifier];
        };

        var isPluginActiveForCustomer = function (identifier) {
            return !!activePluginIdentifiers[identifier];
        };

        var canManage = function () {
            return authService.hasPermission('modules_manage') || authService.hasPermission('plugins_customer_access_management');
        };

        // Estado de manutencao "cru": {disabled, disabledBy, disabledAt, reason, source}.
        var maintenanceInfo = function (id) {
            var entry = byId[id];
            if (!entry || !entry.manageVia) {
                return {disabled: false};
            }
            if (entry.manageVia === 'plugin') {
                if (!isPluginInstalled(entry.backingPlugin)) {
                    return {disabled: false};
                }
                var pluginOff = !isPluginActiveForCustomer(entry.backingPlugin);
                return {disabled: pluginOff, source: 'plugin', reason: null, disabledBy: null, disabledAt: null};
            }
            // manageVia === 'native'
            var s = nativeState[id];
            if (s && s.disabled) {
                return {
                    disabled: true, source: 'native',
                    reason: s.reason, disabledBy: s.disabledBy, disabledAt: s.disabledAt
                };
            }
            return {disabled: false};
        };

        var isInMaintenance = function (id) {
            return maintenanceInfo(id).disabled;
        };

        var isEssential = function (id) {
            var entry = byId[id];
            if (!entry) return false;
            if (ESSENTIAL_IDS[id]) return true;
            var s = nativeState[id];
            return !!(s && s.essential);
        };

        // O item entra no menu/telas se: o predicado original permitir, o modulo existir de
        // fato neste servidor (plugin instalado) e, quando esta em manutencao, o usuario
        // puder gerenciar modulos (senao ele nunca teria acesso a tela real por tras).
        var isVisible = function (id) {
            var entry = byId[id];
            if (!entry) return false;
            if (entry.visible && !entry.visible(authService)) return false;
            if (entry.backingPlugin && entry.type === 'extension' && !isPluginInstalled(entry.backingPlugin)) return false;
            if (isInMaintenance(id) && !canManage()) return false;
            return true;
        };

        var visibleSections = function () {
            return SECTIONS.map(function (sec) {
                return {
                    id: sec.id,
                    labelKey: sec.labelKey,
                    modules: CATALOG.filter(function (m) { return m.section === sec.id && isVisible(m.id); })
                };
            }).filter(function (sec) { return sec.modules.length > 0; });
        };

        // Para a tela Modulos: tudo que o papel do usuario pode ver, mostrando tambem os
        // itens em manutencao (para quem gerencia poder religa-los). Continua respeitando o
        // predicado de permissao original (nao mostra Papeis pra quem nao pode ver Papeis) e
        // continua exigindo que o plugin exista de fato no servidor.
        var allSections = function () {
            return SECTIONS.map(function (sec) {
                return {
                    id: sec.id,
                    labelKey: sec.labelKey,
                    modules: CATALOG.filter(function (m) {
                        if (m.section !== sec.id) return false;
                        if (m.visible && !m.visible(authService)) return false;
                        if (m.backingPlugin && m.type === 'extension' && !isPluginInstalled(m.backingPlugin)) return false;
                        return true;
                    })
                };
            }).filter(function (sec) { return sec.modules.length > 0; });
        };

        // Botao "Configurar": so quando o plugin por tras da linha tem mesmo uma tela de
        // configuracoes e o usuario tem a permissao dela. Sem isso o botao simplesmente
        // nao aparece (nada de link morto).
        var settingsTarget = function (id) {
            var entry = byId[id];
            if (!entry || !entry.backingPlugin) return null;
            var plugin = pluginsById[entry.backingPlugin];
            if (!plugin || !plugin.settingsViewTemplate) return null;
            if (plugin.settingsPermission && !authService.hasPermission(plugin.settingsPermission)) return null;
            return 'plugin-settings-' + entry.backingPlugin;
        };

        var canToggle = function (id) {
            var entry = byId[id];
            if (!entry || !entry.manageVia) return false;
            if (isEssential(id)) return false;
            if (!canManage()) return false;
            if (entry.manageVia === 'native') return nativeStateAvailable;
            // manageVia === 'plugin'
            return isPluginInstalled(entry.backingPlugin);
        };

        var toggleNative = function (id, disabled, reason) {
            var deferred = $q.defer();
            moduleRegistryApi.toggleModule({moduleId: id, disabled: disabled, reason: reason || null}, function (response) {
                if (response.status === 'OK') {
                    load(true).then(function () {
                        $rootScope.$broadcast('aero_MODULES_UPDATED');
                        deferred.resolve(response);
                    });
                } else {
                    deferred.reject(response);
                }
            }, function (err) {
                deferred.reject((err && err.data) ? err.data : err);
            });
            return deferred.promise;
        };

        // O servidor so aceita a lista COMPLETA dos plugins que devem ficar desligados (ele
        // substitui, nao soma). Por isso recalculamos o conjunto inteiro a cada chamada, a
        // partir do que ja esta carregado, em vez de mandar so o identificador que mudou.
        var togglePlugin = function (identifier, disabled) {
            var newDisabledNumericIds = [];
            Object.keys(pluginsById).forEach(function (id) {
                var plugin = pluginsById[id];
                var staysDisabled = (id === identifier) ? disabled : !isPluginActiveForCustomer(id);
                if (staysDisabled) newDisabledNumericIds.push(plugin.id);
            });
            var deferred = $q.defer();
            pluginService.disablePlugins(newDisabledNumericIds, function (response) {
                if (response.status === 'OK') {
                    load(true).then(function () {
                        $rootScope.$broadcast('aero_PLUGINS_UPDATED');
                        $rootScope.$broadcast('aero_MODULES_UPDATED');
                        deferred.resolve(response);
                    });
                } else {
                    deferred.reject(response);
                }
            }, function (err) {
                deferred.reject((err && err.data) ? err.data : err);
            });
            return deferred.promise;
        };

        var toggle = function (id, disabled, reason) {
            var entry = byId[id];
            if (!entry || !canToggle(id)) {
                return $q.reject({message: 'error.permission.denied'});
            }
            if (entry.manageVia === 'plugin') {
                return togglePlugin(entry.backingPlugin, disabled);
            }
            return toggleNative(id, disabled, reason);
        };

        return {
            load: load,
            isReady: function () { return loaded; },
            getEntry: function (id) { return byId[id]; },
            catalog: function () { return CATALOG; },
            sections: function () { return SECTIONS; },
            visibleSections: visibleSections,
            allSections: allSections,
            isVisible: isVisible,
            isInMaintenance: isInMaintenance,
            maintenanceInfo: maintenanceInfo,
            isEssential: isEssential,
            canManage: canManage,
            canToggle: canToggle,
            toggle: toggle,
            settingsTarget: settingsTarget,
            nativeStateAvailable: function () { return nativeStateAvailable; },
            isPluginInstalled: isPluginInstalled,
            isPluginActiveForCustomer: isPluginActiveForCustomer,
            installedPlugins: function () {
                var list = [];
                Object.keys(pluginsById).forEach(function (id) { list.push(pluginsById[id]); });
                return list;
            }
        };
    });
