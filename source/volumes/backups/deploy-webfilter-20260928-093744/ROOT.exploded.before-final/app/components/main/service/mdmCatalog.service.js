// Catalogo das opcoes aceitas nos campos de texto livre do perfil de configuracao.
//
// Origem dos dados (nada aqui e inventado):
//  - restricoes: constantes DISALLOW_* de android.os.UserManager, extraidas de
//    .android-sdk/platforms/android-34/android.jar por reflexao. O servidor envia o VALOR
//    da constante (ex.: "no_factory_reset") e o agente o repassa a addUserRestriction().
//  - as tres pseudo-restricoes de lock task estao em
//    android-source/app/src/main/java/com/hmdm/launcher/policy/KioskPolicy.java — elas NAO
//    sao chaves de UserManager e sao aplicadas via setLockTaskFeatures/setStatusBarDisabled.
//  - admin extras: constantes QR_*_ATTR de
//    android-source/app/src/main/java/com/hmdm/launcher/Const.java (linhas 99-113).
angular.module('headwind-kiosk')
    .factory('mdmCatalog', function () {

        // grupo: rotulo do agrupamento na UI
        // key:   string exata gravada em configurations.restrictions
        // label: nome legivel
        // help:  o que o usuario perde/ganha ao marcar
        // risk:  'danger' trava a propria gestao do MDM; a UI avisa antes de marcar
        var RESTRICTIONS = [
            // ---- Reset e integridade do dispositivo
            {grupo: 'Integridade do dispositivo', key: 'no_factory_reset', label: 'Bloquear restauracao de fabrica', help: 'O usuario nao consegue apagar o tablet pelas Configuracoes. Nao impede o reset feito pelo proprio admin do MDM.'},
            {grupo: 'Integridade do dispositivo', key: 'no_safe_boot', label: 'Bloquear modo de seguranca', help: 'Impede reiniciar em safe mode, que desativaria o MDM.'},
            {grupo: 'Integridade do dispositivo', key: 'no_debugging_features', label: 'Bloquear depuracao (ADB)', help: 'Desativa as opcoes de desenvolvedor e a depuracao USB.'},
            {grupo: 'Integridade do dispositivo', key: 'no_network_reset', label: 'Bloquear reset de rede', help: 'Impede zerar as configuracoes de rede.'},
            {grupo: 'Integridade do dispositivo', key: 'no_config_date_time', label: 'Bloquear alterar data e hora', help: 'Trava relogio e fuso horario.'},
            {grupo: 'Integridade do dispositivo', key: 'no_config_locale', label: 'Bloquear alterar idioma', help: 'Trava o idioma do sistema.'},
            {grupo: 'Integridade do dispositivo', key: 'no_fun', label: 'Desativar easter eggs', help: 'Bloqueia as animacoes escondidas do Android.'},
            {grupo: 'Integridade do dispositivo', key: 'no_system_error_dialogs', label: 'Ocultar dialogos de erro do sistema', help: 'Suprime "o app parou de responder". Util em kiosk sem operador.'},
            {grupo: 'Integridade do dispositivo', key: 'no_set_wallpaper', label: 'Bloquear trocar papel de parede', help: ''},
            {grupo: 'Integridade do dispositivo', key: 'no_set_user_icon', label: 'Bloquear trocar foto do usuario', help: ''},
            {grupo: 'Integridade do dispositivo', key: 'no_ambient_display', label: 'Desativar always-on display', help: ''},
            {grupo: 'Integridade do dispositivo', key: 'disallow_config_default_apps', label: 'Bloquear apps padrao', help: 'Impede trocar o navegador/discador padrao. Requer Android 11+.'},

            // ---- Aplicativos
            {grupo: 'Aplicativos', key: 'no_install_apps', label: 'Bloquear instalacao de apps', help: 'ATENCAO: bloqueia TODA instalacao, inclusive as feitas pelo proprio MDM. O agente deixa de conseguir instalar e ate se atualizar.', risk: 'danger'},
            {grupo: 'Aplicativos', key: 'no_uninstall_apps', label: 'Bloquear desinstalacao de apps', help: 'Nada pode ser desinstalado, nem pelo MDM.', risk: 'danger'},
            {grupo: 'Aplicativos', key: 'no_control_apps', label: 'Bloquear gerenciar apps', help: 'Esconde forcar parada, limpar dados e desativar app nas Configuracoes.'},
            {grupo: 'Aplicativos', key: 'no_install_unknown_sources', label: 'Bloquear fontes desconhecidas', help: 'Impede instalar APK fora da loja neste usuario.'},
            {grupo: 'Aplicativos', key: 'no_install_unknown_sources_globally', label: 'Bloquear fontes desconhecidas (todo o aparelho)', help: 'Versao global da anterior. Requer Android 11+.'},

            // ---- Contas e usuarios
            {grupo: 'Contas e usuarios', key: 'no_add_user', label: 'Bloquear adicionar usuario', help: 'Impede criar um segundo perfil no tablet.'},
            {grupo: 'Contas e usuarios', key: 'no_remove_user', label: 'Bloquear remover usuario', help: ''},
            {grupo: 'Contas e usuarios', key: 'no_user_switch', label: 'Bloquear troca de usuario', help: ''},
            {grupo: 'Contas e usuarios', key: 'no_grant_admin', label: 'Bloquear conceder admin', help: 'Requer Android 14+.'},
            {grupo: 'Contas e usuarios', key: 'no_modify_accounts', label: 'Bloquear adicionar/remover contas', help: 'Trava contas Google e afins.'},
            {grupo: 'Contas e usuarios', key: 'no_add_managed_profile', label: 'Bloquear perfil de trabalho', help: ''},
            {grupo: 'Contas e usuarios', key: 'no_remove_managed_profile', label: 'Bloquear remover perfil de trabalho', help: ''},
            {grupo: 'Contas e usuarios', key: 'no_cross_profile_copy_paste', label: 'Bloquear copiar/colar entre perfis', help: ''},
            {grupo: 'Contas e usuarios', key: 'no_sharing_into_profile', label: 'Bloquear compartilhar para o perfil de trabalho', help: ''},
            {grupo: 'Contas e usuarios', key: 'no_unified_password', label: 'Exigir senhas separadas', help: 'Impede usar a mesma senha do aparelho no perfil de trabalho.'},
            {grupo: 'Contas e usuarios', key: 'no_config_credentials', label: 'Bloquear credenciais e certificados', help: ''},

            // ---- Rede
            {grupo: 'Rede', key: 'no_config_wifi', label: 'Bloquear configurar Wi-Fi', help: 'O usuario nao troca de rede. Combine com SSID fixo na aba Rede.'},
            {grupo: 'Rede', key: 'no_change_wifi_state', label: 'Bloquear ligar/desligar Wi-Fi', help: ''},
            {grupo: 'Rede', key: 'no_add_wifi_config', label: 'Bloquear adicionar rede Wi-Fi', help: 'Requer Android 13+.'},
            {grupo: 'Rede', key: 'no_wifi_direct', label: 'Bloquear Wi-Fi Direct', help: 'Requer Android 13+.'},
            {grupo: 'Rede', key: 'no_wifi_tethering', label: 'Bloquear roteador Wi-Fi', help: ''},
            {grupo: 'Rede', key: 'no_config_tethering', label: 'Bloquear compartilhamento de conexao', help: ''},
            {grupo: 'Rede', key: 'no_sharing_admin_configured_wifi', label: 'Bloquear compartilhar Wi-Fi do admin', help: 'Requer Android 13+.'},
            {grupo: 'Rede', key: 'no_config_mobile_networks', label: 'Bloquear configurar rede movel', help: ''},
            {grupo: 'Rede', key: 'no_data_roaming', label: 'Bloquear roaming de dados', help: 'Evita conta de dados no exterior.'},
            {grupo: 'Rede', key: 'no_cellular_2g', label: 'Bloquear rede 2G', help: 'Fecha ataques de downgrade. Requer Android 14+.'},
            {grupo: 'Rede', key: 'no_airplane_mode', label: 'Bloquear modo aviao', help: 'Impede o usuario derrubar a conexao. Requer Android 9+.'},
            {grupo: 'Rede', key: 'no_config_vpn', label: 'Bloquear configurar VPN', help: ''},
            {grupo: 'Rede', key: 'disallow_config_private_dns', label: 'Bloquear DNS privado', help: 'Requer Android 12+.'},
            {grupo: 'Rede', key: 'no_config_cell_broadcasts', label: 'Bloquear alertas de emergencia', help: ''},

            // ---- Conectividade fisica
            {grupo: 'Conectividade fisica', key: 'no_bluetooth', label: 'Desligar Bluetooth', help: 'Desativa o radio por completo.'},
            {grupo: 'Conectividade fisica', key: 'no_config_bluetooth', label: 'Bloquear configurar Bluetooth', help: 'Mantem o radio ligado, mas impede parear.'},
            {grupo: 'Conectividade fisica', key: 'no_bluetooth_sharing', label: 'Bloquear compartilhar por Bluetooth', help: ''},
            {grupo: 'Conectividade fisica', key: 'no_usb_file_transfer', label: 'Bloquear transferencia por USB', help: 'Impede copiar arquivos ligando o cabo no PC.'},
            {grupo: 'Conectividade fisica', key: 'no_physical_media', label: 'Bloquear midia removivel', help: 'Impede montar cartao SD ou pendrive.'},
            {grupo: 'Conectividade fisica', key: 'no_outgoing_beam', label: 'Bloquear NFC (Android Beam)', help: ''},
            {grupo: 'Conectividade fisica', key: 'no_ultra_wideband_radio', label: 'Desligar radio UWB', help: 'Requer Android 14+.'},
            {grupo: 'Conectividade fisica', key: 'no_printing', label: 'Bloquear impressao', help: ''},

            // ---- Privacidade e sensores
            {grupo: 'Privacidade e sensores', key: 'disallow_camera_toggle', label: 'Bloquear o botao de camera', help: 'Impede o usuario cortar a camera pelo atalho de privacidade. Requer Android 12+.'},
            {grupo: 'Privacidade e sensores', key: 'disallow_microphone_toggle', label: 'Bloquear o botao de microfone', help: 'Requer Android 12+.'},
            {grupo: 'Privacidade e sensores', key: 'no_unmute_microphone', label: 'Manter microfone mudo', help: ''},
            {grupo: 'Privacidade e sensores', key: 'no_share_location', label: 'Bloquear compartilhar localizacao', help: ''},
            {grupo: 'Privacidade e sensores', key: 'no_config_location', label: 'Bloquear configurar localizacao', help: 'CUIDADO: com o GPS obrigatorio ligado, isto apenas impede o usuario DESLIGAR o GPS — que e o efeito desejado. Nao marque junto com GPS desligado.', risk: 'warn'},
            {grupo: 'Privacidade e sensores', key: 'no_content_capture', label: 'Bloquear captura de conteudo', help: 'Requer Android 11+.'},
            {grupo: 'Privacidade e sensores', key: 'no_content_suggestions', label: 'Bloquear sugestoes de conteudo', help: 'Requer Android 11+.'},
            {grupo: 'Privacidade e sensores', key: 'no_autofill', label: 'Bloquear preenchimento automatico', help: ''},

            // ---- Telefonia
            {grupo: 'Telefonia', key: 'no_sms', label: 'Bloquear SMS', help: 'Impede enviar e receber mensagens.'},
            {grupo: 'Telefonia', key: 'no_outgoing_calls', label: 'Bloquear chamadas de saida', help: ''},

            // ---- Interface (aplicadas via lock task, nao via UserManager)
            {grupo: 'Interface do kiosk', key: 'no_status_bar', label: 'Ocultar barra de status', help: 'Aplicada por setStatusBarDisabled(), nao por UserManager. So funciona com o modo kiosk ligado.', lockTask: true},
            {grupo: 'Interface do kiosk', key: 'no_recent_apps', label: 'Bloquear apps recentes', help: 'Aplicada por setLockTaskFeatures(). So funciona com o modo kiosk ligado.', lockTask: true},
            {grupo: 'Interface do kiosk', key: 'no_notifications', label: 'Bloquear notificacoes', help: 'Aplicada por setLockTaskFeatures(). So funciona com o modo kiosk ligado.', lockTask: true},

            // ---- Ajustes rapidos
            {grupo: 'Ajustes do usuario', key: 'no_adjust_volume', label: 'Bloquear ajuste de volume', help: 'Equivale a marcar "Travar volume" na aba Hardware.'},
            {grupo: 'Ajustes do usuario', key: 'no_config_brightness', label: 'Bloquear ajuste de brilho', help: 'Equivale a definir brilho fixo na aba Hardware.'},
            {grupo: 'Ajustes do usuario', key: 'no_config_screen_timeout', label: 'Bloquear tempo de tela', help: 'Equivale a definir timeout fixo na aba Hardware.'},
            {grupo: 'Ajustes do usuario', key: 'no_create_windows', label: 'Bloquear janelas sobrepostas', help: 'Impede apps desenharem por cima de outros (bolhas, overlays).'}
        ];

        // Chaves aceitas no bundle de provisionamento, de Const.java.
        // auto=true: o servidor ja gera esta chave sozinha no QR code; listada so para
        // referencia, a UI nao deixa adicionar de novo.
        var ADMIN_EXTRAS = [
            {key: 'com.hmdm.BASE_URL', label: 'URL do servidor', tipo: 'texto', auto: true, help: 'Preenchida automaticamente com o endereco deste servidor.'},
            {key: 'com.hmdm.SERVER_PROJECT', label: 'Caminho do projeto', tipo: 'texto', auto: true, help: 'Preenchida automaticamente com o context path.'},
            {key: 'com.hmdm.DEVICE_ID', label: 'ID do dispositivo', tipo: 'texto', auto: true, help: 'Preenchida ao gerar um QR code para um dispositivo especifico.'},
            {key: 'com.hmdm.CUSTOMER', label: 'Cliente', tipo: 'texto', auto: true, help: 'Preenchida automaticamente em instalacoes multi-cliente.'},
            {key: 'com.hmdm.CONFIG', label: 'Perfil de configuracao', tipo: 'texto', auto: true, help: 'Preenchida automaticamente com o nome deste perfil.'},
            {key: 'com.hmdm.GROUP', label: 'Grupo', tipo: 'texto', auto: true, help: 'Preenchida quando se escolhe grupos ao gerar o QR.'},

            {key: 'com.hmdm.SECONDARY_BASE_URL', label: 'URL secundaria do servidor', tipo: 'texto', help: 'Endereco alternativo usado quando o principal nao responde. Ex.: https://mdm2.exemplo.com.br'},
            {key: 'com.hmdm.DEVICE_ID_USE', label: 'Origem do ID do dispositivo', tipo: 'opcao', opcoes: ['serial', 'imei', 'macaddr', 'manual'], help: 'De onde o agente tira o identificador na matricula. "serial" e o padrao recomendado.'},
            {key: 'com.hmdm.SKIP_INTRO', label: 'Pular tela de introducao', tipo: 'booleano', help: 'true faz a matricula ir direto ao ponto, sem as telas de boas-vindas.'},
            {key: 'com.hmdm.CONN_RETRY_COUNT', label: 'Tentativas de conexao', tipo: 'numero', padrao: 2, help: 'Quantas vezes o agente reteta falar com o servidor na matricula. Padrao 2.'},
            {key: 'com.hmdm.CONN_RETRY_DELAY', label: 'Intervalo entre tentativas (s)', tipo: 'numero', padrao: 15, help: 'Segundos entre as tentativas. Padrao 15.'},
            {key: 'com.hmdm.OPEN_WIFI', label: 'Abrir configuracao de Wi-Fi', tipo: 'booleano', help: 'true faz o agente abrir a tela de Wi-Fi durante a matricula, para o operador escolher a rede.'},
            {key: 'com.hmdm.WORK_PROFILE', label: 'Matricular como perfil de trabalho', tipo: 'booleano', help: 'true usa perfil de trabalho em vez de Device Owner. ATENCAO: sem Device Owner o modo kiosk nao funciona.', risk: 'warn'},
            {key: 'com.hmdm.CERTS', label: 'Certificados confiaveis', tipo: 'texto', help: 'Hashes SHA-256 de certificados a confiar, separados por virgula. Necessario quando o servidor usa TLS proprio.'}
        ];

        // Atividades que fazem sentido liberar fora do app principal em kiosk.
        // A lista completa e especifica de cada app instalado; a UI complementa esta base
        // com as atividades derivadas dos apps escolhidos no proprio perfil.
        var ALLOWED_ACTIVITIES_BASE = [
            {key: 'com.hmdm.launcher.MainActivity', label: 'Tela inicial do Headwind MDM', help: 'Necessaria para o proprio launcher aparecer. Deixe marcada salvo se outro app for o principal.'},
            {key: 'com.android.settings.Settings', label: 'Configuracoes do Android', help: 'Abre as configuracoes completas. So libere se o operador precisar mesmo.', risk: 'warn'},
            {key: 'com.android.settings.wifi.WifiSettings', label: 'Configuracoes de Wi-Fi', help: 'Apenas a tela de Wi-Fi. Alternativa segura a liberar as configuracoes inteiras.'},
            {key: 'com.android.settings.bluetooth.BluetoothSettings', label: 'Configuracoes de Bluetooth', help: ''},
            {key: 'com.android.settings.Settings$DateTimeSettingsActivity', label: 'Data e hora', help: ''},
            {key: 'com.android.settings.Settings$LanguageAndInputSettingsActivity', label: 'Idioma e teclado', help: 'Libere se o operador precisar trocar de teclado.'},
            {key: 'com.android.systemui.recents.RecentsActivity', label: 'Apps recentes', help: 'Contraria o kiosk restritivo. Nao libere sem motivo.', risk: 'warn'},
            {key: 'com.android.dialer.DialtactsActivity', label: 'Telefone', help: ''},
            {key: 'com.android.camera2.CameraActivity', label: 'Camera', help: ''}
        ];

        function porGrupo(lista) {
            var ordem = [];
            var mapa = {};
            lista.forEach(function (item) {
                if (!mapa[item.grupo]) {
                    mapa[item.grupo] = {nome: item.grupo, itens: []};
                    ordem.push(mapa[item.grupo]);
                }
                mapa[item.grupo].itens.push(item);
            });
            return ordem;
        }

        // "a, b ,, c" -> ['a','b','c']  (mesma semantica de KioskPolicy.parseRestrictions)
        function csvParaLista(csv) {
            if (!csv) {
                return [];
            }
            return csv.split(',')
                .map(function (s) { return s.trim(); })
                .filter(function (s) { return s.length > 0; })
                .filter(function (s, i, arr) { return arr.indexOf(s) === i; });
        }

        function listaParaCsv(lista) {
            return (lista || []).join(',');
        }

        // Chaves gravadas no perfil que nao constam do catalogo. Nunca sao descartadas
        // silenciosamente: a UI as mostra num bloco "personalizadas" para o admin decidir.
        function desconhecidas(csv, catalogo) {
            var conhecidas = {};
            catalogo.forEach(function (c) { conhecidas[c.key] = true; });
            return csvParaLista(csv).filter(function (k) { return !conhecidas[k]; });
        }

        return {
            restrictions: RESTRICTIONS,
            restrictionsPorGrupo: porGrupo(RESTRICTIONS),
            adminExtras: ADMIN_EXTRAS,
            adminExtrasEditaveis: ADMIN_EXTRAS.filter(function (e) { return !e.auto; }),
            allowedActivitiesBase: ALLOWED_ACTIVITIES_BASE,
            csvParaLista: csvParaLista,
            listaParaCsv: listaParaCsv,
            desconhecidas: desconhecidas
        };
    });
