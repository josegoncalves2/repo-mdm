// Localization completed
angular.module('headwind-kiosk')
    .controller('RolesTabController', function ($scope, $rootScope, $state, $uibModal, alertService, confirmModal,
                                                 roleService, $window, localization) {
        $scope.viewMode = $window.localStorage.getItem('hwmdm_roles_viewMode') || 'cards';
        $scope.$watch('viewMode', function (v) { if (v) $window.localStorage.setItem('hwmdm_roles_viewMode', v); });

        $scope.init = function () {
            $rootScope.settingsTabActive = true;
            $rootScope.pluginsTabActive = false;
            $scope.search();
        };

        $scope.search = function () {
            roleService.getPermissions(
                function (response) {
                    $scope.permissions = response.data;
                });
            roleService.getRoles(
                function (response) {
                    $scope.roles = response.data;
                });
        };

        $scope.editRole = function (role) {
            var modalInstance = $uibModal.open({
                templateUrl: 'app/components/main/view/modal/role.html?v=h712d406a5b',
                controller: 'RoleModalController',
                size: 'lg',
                resolve: {
                    role: function () {
                        return role;
                    },
                    permissions: function() {
                        return $scope.permissions;
                    }
                }
            });

            modalInstance.result.then(function () {
                $scope.search();
            });
        };

        $scope.removeRole = function (role) {
            let localizedText = localization.localize('question.delete.role').replace('${roleName}', role.name);
            confirmModal.getUserConfirmation(localizedText, function () {
                roleService.removeRole({id: role.id}, function (response) {
                    if (response.status === 'OK') {
                        $scope.search();
                    } else {
                        alertService.showAlertMessage(localization.localize('error.request.failure'));
                    }
                });
            });
        };

        $scope.init();
    })
    .controller('RoleModalController', function ($scope, $uibModalInstance, roleService, role, permissions, localization) {
        $scope.role = {};
        for (var prop in role) {
            if (role.hasOwnProperty(prop)) {
                $scope.role[prop] = role[prop];
            }
        }
        // Permissoes segregadas por area. A ordem dos grupos e dos itens e' a da tela; o que nao
        // estiver em grupo nenhum (permissao nova de plugin) cai em "Outras" sozinho.
        var GROUPS = [["devices", ["edit_devices", "enroll_devices", "edit_device_desc", "device.profile.view", "device.profile.edit", "device.contacts.manage", "device.photos.upload", "device.export_import", "device.ldap.sync", "edit_device_app_settings"]], ["actions", ["device.lifecycle.lock", "device.lifecycle.unlock", "device.lifecycle.reboot", "device.lifecycle.reset", "device.lifecycle.wipe"]], ["remote", ["device.remote_access.view", "device.remote_access.control", "device.gps.view"]], ["profiles", ["configurations", "add_config", "copy_config", "device.kiosk.edit"]], ["content", ["applications", "edit_applications", "edit_application_versions", "files", "edit_files"]], ["messages", ["plugin_messaging_send", "plugin_messaging_delete", "plugin_push_send", "plugin_push_delete", "push_api"]], ["diagnostics", ["device.logs.view", "plugin_devicelog_access", "plugin_deviceinfo_access", "plugin_audit_access"]], ["filter", ["plugin_webfilter_access", "device.network.filter"]], ["admin", ["settings", "device.branding.edit", "modules_manage", "plugins_customer_access_management"]], ["other", []]];
        // Herdadas da Headwind sem uso nesta instalacao: nao aparecem, mas quem ja tem continua tendo.
        var HIDDEN = {superadmin: true, plugin_xtra_access: true, get_updates: true,
            // Sem funcao neste sistema (nenhuma tela ou rota as usa) ou duplicadas:
            'device.photos.upload': true, 'device.network.filter': true, 'device.ldap.sync': true,
            'device.contacts.manage': true, 'device.profile.view': true, 'device.export_import': true,
            'device.logs.view': true};

        var label = function (p) {
            var key = 'rolegrp.perm.' + p.name, v = localization.localize(key);
            if (v !== key) return v;
            var legacy = localization.localize('permission.' + p.name);
            return legacy !== 'permission.' + p.name ? legacy : (p.description || p.name);
        };
        var desc = function (p) {
            var key = 'rolegrp.desc.' + p.name, v = localization.localize(key);
            return v !== key ? v : '';
        };

        var selected = {};
        (role.permissions || []).forEach(function (p) { selected[p.id] = true; });
        $scope.selected = selected;

        var byName = {};
        permissions.forEach(function (p) { byName[p.name] = p; });
        var placed = {};
        $scope.groups = GROUPS.map(function (g) {
            var items = g[1].map(function (n) { return byName[n]; }).filter(Boolean);
            items.forEach(function (p) { placed[p.name] = true; });
            return {id: g[0], title: localization.localize('rolegrp.group.' + g[0]), items: items};
        });
        var other = $scope.groups.filter(function (g) { return g.id === 'other'; })[0];
        permissions.forEach(function (p) {
            if (!placed[p.name] && !HIDDEN[p.name]) other.items.push(p);
        });
        $scope.groups.forEach(function (g) {
            g.items = g.items.map(function (p) { return {id: p.id, name: p.name, label: label(p), desc: desc(p)}; });
        });
        $scope.groups = $scope.groups.filter(function (g) { return g.items.length > 0; });
        var hiddenIds = permissions.filter(function (p) { return HIDDEN[p.name]; }).map(function (p) { return p.id; });

        $scope.filter = {text: ''};
        $scope.matches = function (item) {
            var t = ($scope.filter.text || '').toLowerCase();
            return !t || item.label.toLowerCase().indexOf(t) >= 0 || item.desc.toLowerCase().indexOf(t) >= 0;
        };
        $scope.countIn = function (g) {
            return g.items.filter(function (i) { return selected[i.id]; }).length;
        };
        $scope.toggleGroup = function (g) {
            var on = $scope.countIn(g) < g.items.length;
            g.items.forEach(function (i) { selected[i.id] = on; });
        };
        $scope.setAll = function (on) {
            $scope.groups.forEach(function (g) { g.items.forEach(function (i) { selected[i.id] = on; }); });
        };
        $scope.totalSelected = function () {
            var n = 0;
            $scope.groups.forEach(function (g) { n += $scope.countIn(g); });
            return n;
        };
        $scope.totalVisible = function () {
            var n = 0;
            $scope.groups.forEach(function (g) { n += g.items.length; });
            return n;
        };

        $scope.save = function () {
            $scope.errorMessage = '';

            if (!$scope.role.name) {
                $scope.errorMessage = localization.localize('error.empty.role.name');
            } else {
                var request = {};
                for (var prop in $scope.role) {
                    if ($scope.role.hasOwnProperty(prop)) {
                        request[prop] = $scope.role[prop];
                    }
                }
                var ids = Object.keys(selected).filter(function (k) { return selected[k]; })
                    .map(function (k) { return parseInt(k, 10); });
                // Herdadas ocultas: preservadas exatamente como estavam no papel
                hiddenIds.forEach(function (id) {
                    if (ids.indexOf(id) < 0 && (role.permissions || []).some(function (p) { return p.id === id; })) {
                        ids.push(id);
                    }
                });
                request.permissions = ids.map(function (id) { return {id: id}; });

                roleService.updateRole(request, function (response) {
                    if (response.status === 'OK') {
                        $uibModalInstance.close();
                    } else {
                        $scope.errorMessage = localization.localize('error.duplicate.role.name');
                    }
                });
            }
        };

        $scope.closeModal = function () {
            $uibModalInstance.dismiss();
        }
    });