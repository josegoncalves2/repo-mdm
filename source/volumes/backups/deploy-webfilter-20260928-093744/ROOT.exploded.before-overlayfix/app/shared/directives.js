// Localization completed
angular.module('headwind-kiosk')
    .directive('ngEnter', function () {
        return function (scope, element, attrs) {
            element.bind("keydown keypress", function (event) {
                if (event.which === 13) {
                    scope.$apply(function () {
                        scope.$eval(attrs.ngEnter);
                    });

                    event.preventDefault();
                }
            });
        };
    })
    .directive('notificationMessage', function ($rootScope) {
        return {
            restrict: 'E',
            replace: false,
            transclude: true,
            template: "    <div class='notification-message' ng-show='message'>" +
                "        <div ng-show='message' class='success'><span>{{message}}</span></div>" +
                "    </div>",
            link: function (scope, elem, attrs) {

                var attrName = attrs.attrName;
                var message = $rootScope[attrName];

                if (message) {
                    scope.message = message;
                    $rootScope[attrName] = undefined;
                }

                var timer = setTimeout(function () {
                    scope.message = undefined;
                }, 5000);

                scope.$on('$destroy', function () {
                    clearTimeout(timer);
                });
            }
        }
    })
    .directive('fileInputDisabler', function () {
        return {
            restrict: 'A',
            link: function (scope, elem, attrs) {
                if ('inputDisabled' in attrs) {
                    attrs.$observe('inputDisabled', function uploadButtonDisabledObserve(value) {
                        var fileInput = elem.find('input');
                        if (fileInput.length > 0) {
                            fileInput[0].disabled = scope.$eval(value);
                        }
                    });
                }
            }
        }
    })
    .directive( 'datepickerPopup', function (){
        return {
            restrict: 'EAC',
            require: 'ngModel',
            link: function( scope, element, attr, controller ) {
                controller.$formatters.shift();
            }
        }
    })
    .directive('focusMe', function ($timeout) {
        return {
            link: function (scope, element, attrs) {
                $timeout(function () {
                    element[0].focus();
                }, 200);
            }
        };
    });
// Shared, accessible help for native buttons and links used as actions.
angular.module('headwind-kiosk').factory('actionHelpDirective', function ($timeout, localization) {
    var descriptions = {
        saveProfile: 'Salva as alterações do perfil e envia a política aos dispositivos vinculados.',
        applyStrictKiosk: 'Preenche uma política restritiva. Use Salvar para aplicá-la aos dispositivos.',
        forceKiosk: 'Envia ao dispositivo o comando para entrar no modo quiosque.',
        pushConfig: 'Solicita ao dispositivo a sincronização do perfil salvo.',
        toggleAppInstall: 'Inclui ou retira o aplicativo da instalação gerenciada deste perfil.',
        toggleAppKiosk: 'Permite ou bloqueia o uso deste aplicativo no quiosque.',
        selectContentApp: 'Define o aplicativo que abrirá inicialmente neste perfil.',
        startRemote: 'Inicia ou retoma o atendimento remoto do dispositivo selecionado.',
        stopRemote: 'Encerra o atendimento remoto deste dispositivo.',
        generateJson: 'Exibe os parâmetros usados para matricular o dispositivo pelo QR code.',
        copy: 'Copia o endereço da integração para a área de transferência.',
        search: 'Pesquisa os registros usando os termos informados.',
        createBackup: 'Cria uma cópia no servidor com o escopo selecionado.',
        downloadBackup: 'Baixa o arquivo de backup selecionado.',
        beginRestore: 'Abre a revisão dos dados que serão restaurados.',
        deleteBackup: 'Solicita confirmação para excluir somente este arquivo de backup.'
    };
    return function () {
        return {restrict: 'E', link: function (scope, element, attrs) {
            if (element[0].tagName === 'A' && !attrs.ngClick && !attrs.role && !element.hasClass('btn')) { return; }
            if (attrs.title || attrs.localizedTitle || attrs.uibTooltip) { return; }
            var update = function () {
                var text = (element[0].innerText || element.text() || '').replace(/\s+/g, ' ').trim();
                if (!text || text.indexOf('{{') !== -1) { return; }
                var match = (attrs.ngClick || '').match(/^\s*(\w+)\s*\(/);
                var help = match && descriptions[match[1]];
                element.attr('title', help || text);
                if (!attrs.ariaLabel) { element.attr('aria-label', text); }
            };
            var timer = $timeout(update, 0, false);
            element.on('mouseenter focus', update);
            scope.$on('$destroy', function () {
                $timeout.cancel(timer);
                element.off('mouseenter focus', update);
            });
        }};
    };
}).directive('button', function (actionHelpDirective) { return actionHelpDirective(); })
  .directive('a', function (actionHelpDirective) { return actionHelpDirective(); });
