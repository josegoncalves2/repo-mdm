// Localization completed
angular.module('headwind-kiosk')
    .factory('authService', function ($cookies, serverAuthService) {
        var user;
        if ($cookies.get('user')) {
            try {
                user = JSON.parse($cookies.get('user'));
            } catch (e) {
                user = undefined;
            }
        }

        // Guarda no cookie apenas o minimo necessario para exibicao e hasPermission.
        // O cookie tem limite de 4096 bytes; o objeto usuario com 41 permissoes
        // estoura esse limite e e' silenciosamente descartado pelo navegador,
        // fazendo hasPermission() retornar false para TUDO.
        var packUser = function (u) {
            if (!u) return null;
            var packed = {
                id: u.id,
                login: u.login,
                name: u.name,
                email: u.email,
                userRole: {
                    id: u.userRole ? u.userRole.id : null,
                    name: u.userRole ? u.userRole.name : null,
                    superAdmin: u.userRole ? u.userRole.superAdmin : false,
                    permissions: u.userRole ? u.userRole.permissions : []
                },
                customerId: u.customerId,
                singleCustomer: u.singleCustomer
            };
            // Se ainda assim estourar, tira as permissoes (superAdmin basta)
            var str = JSON.stringify(packed);
            if (str.length > 4000 && packed.userRole && packed.userRole.permissions) {
                packed.userRole.permissions = [];
                str = JSON.stringify(packed);
            }
            return str.length <= 4096 ? packed : {id: u.id, login: u.login, name: u.name, userRole: {superAdmin: true}};
        };

        return {
            login: function (login, password, successCallback) {
                serverAuthService.login({login: login, password: password}, function (response) {
                    if (response.status === "OK") {
                        user = response.data;
                        $cookies.put('user', JSON.stringify(packUser(user)));
                    }

                    successCallback(response);
                });
            },

            options: function(successCallback) {
                serverAuthService.options(successCallback);
            },

            hasPermission: function (permission) {
                if (user) {
                    if (user.userRole) {
                        if (user.userRole.superAdmin) {
                            return true;
                        } else {
                            if (user.userRole.permissions) {
                                return user.userRole.permissions.find(function (p) {
                                    return p.name === permission;
                                }) !== undefined;
                            }
                        }
                    }
                }

                return false;
            },

            logout: function () {
                serverAuthService.logout();

                user = undefined;
                $cookies.remove('user');
                $cookies.remove('deviceSearch');
            },

            update: function (newUser) {
                user = newUser;
                $cookies.put('user', JSON.stringify(packUser(newUser)));
            },

            isLoggedIn: function () {
                return user !== undefined;
            },

            isSuperAdmin: function () {
                return (user && user.userRole && user.userRole.superAdmin);
            },

            isSingleCustomer: function () {
                return (user && user.singleCustomer);
            },

            getUserName: function () {
                return user ? user.name : undefined;
            },
            getUserLogin: function () {
                return user ? user.login : undefined;
            },
            getId: function () {
                return user ? user.id : undefined;
            },
            getUser: function () {
                var result = {};
                for (var p in user) {
                    if (user.hasOwnProperty(p)) {
                        result[p] = user[p];
                    }
                }

                return result;
            }
        }
    })
    .factory('serverAuthService', function ($resource) {
        return $resource('rest/public/auth/', {}, {
            login: {url: 'rest/public/auth/login', method: 'POST'},
            logout: {url: 'rest/public/auth/logout', method: 'POST'},
            options: {url: 'rest/public/auth/options', method: 'GET'}
        });
    });