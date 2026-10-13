// Localization completed
angular.module('headwind-kiosk')
    .factory('authService', function ($cookies, serverAuthService) {
        var user;
        // O usuario completo fica no localStorage (sem o limite de 4 KB do cookie). Antes, apos F5,
        // ele vinha do cookie, que nao cabia e era gravado sem permissoes nem perfis: as telas que
        // carregavam antes do /users/current quebravam (ex.: user.configurations indefinido).
        var STORE_KEY = 'hwmdm.user';
        var readStored = function () {
            try {
                var v = window.localStorage.getItem(STORE_KEY);
                return v ? JSON.parse(v) : undefined;
            } catch (e) {
                return undefined;
            }
        };
        var writeStored = function (u) {
            try {
                if (u) {
                    window.localStorage.setItem(STORE_KEY, JSON.stringify(u));
                } else {
                    window.localStorage.removeItem(STORE_KEY);
                }
            } catch (e) { /* sem localStorage: fica so o cookie */ }
        };
        if ($cookies.get('user')) {
            try {
                user = JSON.parse($cookies.get('user'));
            } catch (e) {
                user = undefined;
            }
            // Mesmo usuario do cookie: usa a copia completa guardada
            var stored = readStored();
            if (user && stored && stored.id === user.id) {
                user = stored;
            }
        }
        // Campos que as telas leem direto do usuario: nunca indefinidos
        var normalize = function (u) {
            if (u) {
                if (u.configurations === undefined || u.configurations === null) u.configurations = [];
                if (u.groups === undefined || u.groups === null) u.groups = [];
                if (u.allConfigAvailable === undefined) u.allConfigAvailable = !u.configurations.length;
                if (u.allDevicesAvailable === undefined) u.allDevicesAvailable = !u.groups.length;
                if (!u.userRole) u.userRole = {permissions: []};
                if (!u.userRole.permissions) u.userRole.permissions = [];
            }
            return u;
        };
        user = normalize(user);

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
            // O limite de 4096 do navegador vale para o cookie JA CODIFICADO, e $cookies.put faz
            // percent-encoding: cada " vira %22, inflando cerca de 25%. Medir o JSON cru deixava
            // passar um valor que o navegador descartava em silencio (visto: 5115 > 4096), e ai o
            // console gravava achando que tinha guardado. O ramo de escape tambem afirmava
            // superAdmin:true para qualquer usuario; agora diz a verdade.
            var cabe = function (o) { return ('user=' + encodeURIComponent(JSON.stringify(o))).length <= 4096; };
            // singleCustomer e customerId vao tambem na versao minima: sem eles o menu escondia
            // Permissoes (canManageRoles) depois de qualquer recarga da pagina.
            return cabe(packed) ? packed : {id: u.id, login: u.login, name: u.name,
                customerId: u.customerId, singleCustomer: u.singleCustomer,
                userRole: {superAdmin: u.userRole ? u.userRole.superAdmin : false}};
        };

        return {
            login: function (login, password, successCallback) {
                serverAuthService.login({login: login, password: password}, function (response) {
                    if (response.status === "OK") {
                        user = normalize(response.data);
                        writeStored(user);
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
                writeStored(undefined);
                $cookies.remove('user');
                $cookies.remove('deviceSearch');
            },

            update: function (newUser) {
                // /users/current nao traz singleCustomer (so' o login traz). Sem preservar, a
                // atualizacao do usuario ao abrir o painel zerava o campo e o menu escondia o
                // que depende dele, como Permissoes (canManageRoles).
                // /users/current devolve singleCustomer=false sempre (campo nao preenchido nessa
                // rota); o valor que vale e' o do login.
                if (newUser && user && user.singleCustomer !== undefined) {
                    newUser.singleCustomer = user.singleCustomer;
                }
                user = normalize(newUser);
                writeStored(user);
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