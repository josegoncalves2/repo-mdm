// Localization completed
/*
 * Visualizador de suporte remoto do painel.
 *
 * O aparelho transmite a tela como video codificado por um WebSocket que o servidor
 * repassa; esta e' a ponta do navegador. Pelo mesmo socket sobem os comandos de toque e
 * digitacao, o que mantem o clique alinhado com a imagem que o operador esta vendo -- um
 * canal separado poderia entregar um toque referente a uma tela que ja mudou.
 *
 * Os quadros chegam como unidades H.264 em Annex-B e sao decodificados por WebCodecs. Nao
 * ha imagem parada em lugar nenhum: e' video continuo, como qualquer software de
 * compartilhamento de tela.
 */
angular.module('headwind-kiosk')
    .factory('remoteSupportService', function ($resource) {
        return $resource('', {}, {
            start: {url: 'rest/private/remote-support/:id/start', method: 'POST'},
            frames: {url: 'rest/private/remote-support/:id/frames', method: 'GET'},
            input: {url: 'rest/private/remote-support/:id/input', method: 'POST'},
            stop: {url: 'rest/private/remote-support/:id/stop', method: 'POST'},
            getStatus: {url: 'rest/private/remote-support/:id/status', method: 'GET'}
        });
    })

    .factory('remoteSupportPlayer', function ($window, $http, $timeout) {

        var FRAME_CONFIG = 1;
        var FRAME_KEY = 2;
        var FRAME_DELTA = 3;
        var HEADER_BYTES = 9;

        var isSupported = function () {
            return typeof $window.VideoDecoder === 'function'
                && typeof $window.EncodedVideoChunk === 'function';
        };

        /*
         * A string de codec que o WebCodecs exige (por exemplo "avc1.42E01E") vem dentro do
         * conjunto de parametros de sequencia, entao e' lida do fluxo em vez de assumida.
         * Chutar baseline funciona ate um aparelho codificar em high profile, e ai o
         * decodificador recusa um fluxo que saberia tocar.
         */
        var codecFromParameterSet = function (bytes) {
            for (var i = 0; i + 4 < bytes.length; i++) {
                var isStart = bytes[i] === 0 && bytes[i + 1] === 0
                    && (bytes[i + 2] === 1 || (bytes[i + 2] === 0 && bytes[i + 3] === 1));
                if (!isStart) {
                    continue;
                }
                var nal = bytes[i + 2] === 1 ? i + 3 : i + 4;
                if (nal + 3 >= bytes.length) {
                    break;
                }
                if ((bytes[nal] & 0x1f) === 7) {
                    return 'avc1.' + hex(bytes[nal + 1]) + hex(bytes[nal + 2]) + hex(bytes[nal + 3]);
                }
            }
            return null;
        };

        var hex = function (value) {
            var text = value.toString(16).toUpperCase();
            return text.length < 2 ? '0' + text : text;
        };

        var concat = function (a, b) {
            var merged = new Uint8Array(a.length + b.length);
            merged.set(a, 0);
            merged.set(b, a.length);
            return merged;
        };

        function Player(canvas, handlers, deviceId) {
            this.deviceId = deviceId;
            this.cursor = 0;
            this.http = false;
            this.canvas = canvas;
            this.context = canvas.getContext('2d');
            this.handlers = handlers || {};
            this.socket = null;
            this.decoder = null;
            this.pendingConfig = null;
            this.codecString = null;
            this.awaitingKeyFrame = true;
            this.closed = false;
            this.stats = {frames: 0, bytes: 0, since: Date.now()};
        }

        Player.prototype.open = function (socketPath) {
            var self = this;
            this.closed = false;

            var scheme = $window.location.protocol === 'https:' ? 'wss://' : 'ws://';
            var base = $window.location.pathname.replace(/[^\/]*$/, '');
            var socket = new $window.WebSocket(scheme + $window.location.host + base + socketPath);
            socket.binaryType = 'arraybuffer';
            this.socket = socket;

            socket.onmessage = function (event) {
                if (typeof event.data === 'string') {
                    self.onControl(event.data);
                } else {
                    self.onFrame(new Uint8Array(event.data));
                }
            };
            socket.onerror = function () {
                // Nao reporta erro aqui: o onclose logo abaixo decide entre cair para HTTP
                // ou avisar o operador, e um erro prematuro so' piscaria na tela.
            };
            socket.onclose = function (event) {
                if (self.closed) {
                    self.teardownDecoder();
                    self.report('onClose');
                    return;
                }
                if (!self.opened) {
                    /*
                     * O socket nunca chegou a abrir. O caso tipico e' um proxy reverso que
                     * nao repassa o upgrade de WebSocket -- foi exatamente o que acontecia
                     * aqui, com o nginx respondendo 404 no handshake enquanto o servidor
                     * gerava video para "espectadores=0". Em vez de exigir mudanca no proxy,
                     * caimos para long polling HTTP, que e' o mesmo transporte que o canal de
                     * push do MDM ja usa e que comprovadamente atravessa esse caminho.
                     */
                    self.socket = null;
                    self.startHttp();
                    return;
                }
                if (event.code !== 1000) {
                    self.report('onError', 'remote.error.stream.dropped');
                }
                self.teardownDecoder();
                self.report('onClose');
            };
            socket.onopen = function () {
                self.opened = true;
            };
        };

        /**
         * Transporte HTTP: puxa quadros por long polling e alimenta o mesmo decodificador.
         * O cursor evita reenvio; quando o navegador fica para tras alem do buffer, o
         * servidor devolve configuracao + ultimo quadro-chave para a imagem voltar sozinha.
         */
        Player.prototype.startHttp = function () {
            var self = this;
            this.http = true;
            this.report('onTransport', 'http');

            var pump = function () {
                if (self.closed) {
                    return;
                }
                $http.get('rest/private/remote-support/' + self.deviceId + '/frames',
                          {params: {since: self.cursor}, timeout: 40000})
                    .then(function (res) {
                        if (self.closed) {
                            return;
                        }
                        var data = res.data && res.data.data;
                        if (data) {
                            self.cursor = data.cursor;
                            self.report('onStatus', {
                                type: 'status', streaming: data.streaming,
                                width: data.width, height: data.height, input: data.input
                            });
                            (data.frames || []).forEach(function (b64) {
                                self.onFrame(base64ToBytes(b64));
                            });
                        }
                        pump();
                    })
                    .catch(function () {
                        if (self.closed) {
                            return;
                        }
                        // Erro de rede momentaneo: espera um pouco e insiste, em vez de
                        // derrubar a sessao inteira.
                        $timeout(pump, 2000);
                    });
            };
            pump();
        };

        var base64ToBytes = function (b64) {
            var binary = $window.atob(b64);
            var bytes = new Uint8Array(binary.length);
            for (var i = 0; i < binary.length; i++) {
                bytes[i] = binary.charCodeAt(i);
            }
            return bytes;
        };

        Player.prototype.onControl = function (text) {
            var message;
            try {
                message = JSON.parse(text);
            } catch (e) {
                return;
            }
            if (message.type === 'status') {
                if (message.width > 0 && message.height > 0) {
                    this.canvas.width = message.width;
                    this.canvas.height = message.height;
                }
                this.report('onStatus', message);
            } else if (message.type === 'input-result') {
                this.report('onInputResult', message);
            }
        };

        Player.prototype.onFrame = function (bytes) {
            if (this.closed || bytes.length <= HEADER_BYTES) {
                return;
            }
            var type = bytes[0];
            var view = new DataView(bytes.buffer, bytes.byteOffset + 1, 8);
            var timestamp = Number(view.getBigUint64(0));
            var payload = bytes.subarray(HEADER_BYTES);

            this.count(bytes.length);

            if (!isSupported()) {
                this.report('onError', 'remote.error.stream.unsupported');
                return;
            }

            if (type === FRAME_CONFIG) {
                // Os parametros ficam guardados em vez de submetidos sozinhos: em Annex-B
                // eles pertencem a frente do quadro-chave que depende deles.
                this.pendingConfig = payload;
                this.codecString = codecFromParameterSet(payload) || this.codecString;
                this.configure();
                return;
            }

            if (!this.decoder || this.decoder.state !== 'configured') {
                return;
            }
            if (type === FRAME_DELTA && this.awaitingKeyFrame) {
                // Decodificar diferenca contra uma imagem que nunca chegou produz lixo;
                // esperar custa no maximo um intervalo de quadro-chave.
                return;
            }

            var data = (type === FRAME_KEY && this.pendingConfig)
                ? concat(this.pendingConfig, payload)
                : payload;

            try {
                this.decoder.decode(new $window.EncodedVideoChunk({
                    type: type === FRAME_KEY ? 'key' : 'delta',
                    timestamp: timestamp,
                    data: data
                }));
                if (type === FRAME_KEY) {
                    this.awaitingKeyFrame = false;
                }
            } catch (e) {
                this.awaitingKeyFrame = true;
                this.report('onError', 'remote.error.stream.decode');
            }
        };

        Player.prototype.configure = function () {
            var self = this;
            if (!this.codecString) {
                return;
            }
            this.teardownDecoder();
            try {
                this.decoder = new $window.VideoDecoder({
                    output: function (frame) {
                        self.paint(frame);
                    },
                    error: function () {
                        self.awaitingKeyFrame = true;
                        self.report('onError', 'remote.error.stream.decode');
                    }
                });
                // Sem "description": o fluxo e' Annex-B, que e' o que o encoder do aparelho
                // emite e o que isto sinaliza ao WebCodecs.
                this.decoder.configure({codec: this.codecString, optimizeForLatency: true});
                this.awaitingKeyFrame = true;
            } catch (e) {
                this.decoder = null;
                this.report('onError', 'remote.error.stream.unsupported');
            }
        };

        Player.prototype.paint = function (frame) {
            try {
                if (this.canvas.width !== frame.displayWidth || this.canvas.height !== frame.displayHeight) {
                    this.canvas.width = frame.displayWidth;
                    this.canvas.height = frame.displayHeight;
                }
                this.context.drawImage(frame, 0, 0);
            } catch (e) {
                // Um quadro que nao pinta nao justifica derrubar a sessao.
            } finally {
                frame.close();
            }
        };

        // =============================================================================================================

        /*
         * Envio de entrada.
         *
         * As coordenadas viajam normalizadas entre 0 e 1, nunca em pixels. O canvas e'
         * exibido redimensionado e a resolucao transmitida nao e' a real; mandar pixels
         * obrigaria as duas pontas a concordarem sobre a escala, e qualquer divergencia --
         * uma rotacao no meio da sessao, por exemplo -- faria o toque cair no lugar errado.
         */
        Player.prototype.send = function (command) {
            if (this.http) {
                $http.post('rest/private/remote-support/' + this.deviceId + '/input', command);
                return true;
            }
            if (!this.socket || this.socket.readyState !== 1) {
                return false;
            }
            try {
                this.socket.send(JSON.stringify(command));
                return true;
            } catch (e) {
                return false;
            }
        };

        Player.prototype.tap = function (x, y) {
            return this.send({type: 'tap', x: x, y: y});
        };

        Player.prototype.longPress = function (x, y) {
            return this.send({type: 'longpress', x: x, y: y});
        };

        Player.prototype.swipe = function (x1, y1, x2, y2, duration) {
            return this.send({type: 'swipe', x1: x1, y1: y1, x2: x2, y2: y2, duration: duration});
        };

        Player.prototype.type = function (text) {
            return this.send({type: 'text', text: text});
        };

        Player.prototype.key = function (name) {
            return this.send({type: 'key', name: name});
        };

        /** Converte a posicao do ponteiro no canvas exibido para coordenada normalizada. */
        Player.prototype.toUnit = function (event) {
            var rect = this.canvas.getBoundingClientRect();
            if (rect.width === 0 || rect.height === 0) {
                return null;
            }
            return {
                x: Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width)),
                y: Math.max(0, Math.min(1, (event.clientY - rect.top) / rect.height))
            };
        };

        // =============================================================================================================

        Player.prototype.count = function (byteCount) {
            this.stats.frames++;
            this.stats.bytes += byteCount;
            var elapsed = Date.now() - this.stats.since;
            if (elapsed >= 1000) {
                this.report('onStats', {
                    fps: Math.round((this.stats.frames * 1000) / elapsed),
                    kbps: Math.round((this.stats.bytes * 8) / elapsed)
                });
                this.stats = {frames: 0, bytes: 0, since: Date.now()};
            }
        };

        Player.prototype.report = function (name, argument) {
            var handler = this.handlers[name];
            if (typeof handler === 'function') {
                handler(argument);
            }
        };

        Player.prototype.teardownDecoder = function () {
            if (this.decoder) {
                try {
                    if (this.decoder.state !== 'closed') {
                        this.decoder.close();
                    }
                } catch (e) {
                    // Ja fechado.
                }
                this.decoder = null;
            }
        };

        Player.prototype.close = function () {
            this.closed = true;
            this.teardownDecoder();
            if (this.socket) {
                try {
                    this.socket.close(1000, 'visualizador fechado');
                } catch (e) {
                    // Ja fechando.
                }
                this.socket = null;
            }
            try {
                this.context.clearRect(0, 0, this.canvas.width, this.canvas.height);
            } catch (e) {
                // Nada pintado ainda.
            }
        };

        return {
            isSupported: isSupported,
            create: function (canvas, handlers, deviceId) {
                return new Player(canvas, handlers, deviceId);
            }
        };
    });
