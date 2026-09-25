// Localization completed
/*
 * Visualizador de suporte remoto do painel.
 *
 * O aparelho transmite a tela como video codificado por um WebSocket que o servidor
 * repassa; esta' e a ponta do navegador. Pelo mesmo socket sobem os comandos de toque e
 * digitacao, o que mantem o clique alinhado com a imagem que o operador esta vendo -- um
 * canal separado poderia entregar um toque referente a uma tela que ja mudou.
 *
 * Os quadros chegam como unidades H.264 em Annex-B e sao decodificados por WebCodecs. Nao
 * ha imagem parada em lugar nenhum: e' video continuo, como qualquer software de
 * compartilhamento de tela.
 *
         * Quando o navegador nao suporta WebCodecs, usa-se MSE + JMuxer para tocar o H.264.
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

        var supportsWebCodecs = function () {
            return typeof $window.VideoDecoder === 'function'
                && typeof $window.EncodedVideoChunk === 'function';
        };

        var supportsMse = function () {
            return typeof $window.MediaSource === 'function'
                && typeof $window.MediaSource.isTypeSupported === 'function'
                && $window.MediaSource.isTypeSupported('video/mp4; codecs="avc1.42E01E"')
                && typeof $window.JMuxer === 'function';
        };

        var isSupported = function () {
            return supportsWebCodecs() || supportsMse();
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

        /*
         * Converte NAL units em Annex-B para comprimento-prefixado (AVCC), que e' o que o
         * MP4 container exige. Cada NAL comeca com 4 bytes de comprimento em big-endian.
         */
        var annexBtoAvcc = function (annexB) {
            var nals = [];
            var i = 0;
            while (i + 4 <= annexB.length) {
                var startLen = 0;
                if (annexB[i] === 0 && annexB[i + 1] === 0 && annexB[i + 2] === 1) {
                    startLen = 3;
                } else if (annexB[i] === 0 && annexB[i + 1] === 0 && annexB[i + 2] === 0 && annexB[i + 3] === 1) {
                    startLen = 4;
                }
                if (startLen > 0) {
                    var nalEnd = i + startLen;
                    while (nalEnd + 4 <= annexB.length) {
                        if (annexB[nalEnd] === 0 && annexB[nalEnd + 1] === 0 && (annexB[nalEnd + 2] === 1 || (annexB[nalEnd + 2] === 0 && annexB[nalEnd + 3] === 1))) {
                            break;
                        }
                        nalEnd++;
                    }
                    if (nalEnd + 4 > annexB.length) { nalEnd = annexB.length; }
                    var nalData = annexB.subarray(i + startLen, nalEnd);
                    var prefix = new Uint8Array(4);
                    prefix[0] = (nalData.length >> 24) & 0xFF;
                    prefix[1] = (nalData.length >> 16) & 0xFF;
                    prefix[2] = (nalData.length >> 8) & 0xFF;
                    prefix[3] = nalData.length & 0xFF;
                    nals.push(prefix, nalData);
                    i = nalEnd;
                } else {
                    i++;
                }
            }
            if (nals.length === 0) {
                return annexB;
            }
            var totalLen = 0;
            for (var j = 0; j < nals.length; j++) {
                totalLen += nals[j].length;
            }
            var merged = new Uint8Array(totalLen);
            var offset = 0;
            for (var k = 0; k < nals.length; k++) {
                merged.set(nals[k], offset);
                offset += nals[k].length;
            }
            return merged;
        };

        /*
         * Extrai SPS e PPS de dados Annex-B (NAL type 7 e 8).
         * Retorna { sps: [Uint8Array], pps: [Uint8Array] }.
         */
        var extractSpsPps = function (annexB) {
            var sps = [], pps = [];
            var i = 0;
            while (i + 4 <= annexB.length) {
                var startLen = 0;
                if (annexB[i] === 0 && annexB[i + 1] === 0 && annexB[i + 2] === 1) {
                    startLen = 3;
                } else if (annexB[i] === 0 && annexB[i + 1] === 0 && annexB[i + 2] === 0 && annexB[i + 3] === 1) {
                    startLen = 4;
                }
                if (startLen > 0) {
                    var nalType = annexB[i + startLen] & 0x1f;
                    var nalEnd = i + startLen;
                    while (nalEnd + 4 <= annexB.length) {
                        if (annexB[nalEnd] === 0 && annexB[nalEnd + 1] === 0 && (annexB[nalEnd + 2] === 1 || (annexB[nalEnd + 2] === 0 && annexB[nalEnd + 3] === 1))) {
                            break;
                        }
                        nalEnd++;
                    }
                    if (nalEnd + 4 > annexB.length) { nalEnd = annexB.length; }
                    var nalData = annexB.subarray(i + startLen, nalEnd);
                    if (nalType === 7) sps.push(nalData);
                    else if (nalType === 8) pps.push(nalData);
                    i = nalEnd;
                } else {
                    i++;
                }
            }
            return { sps: sps, pps: pps };
        };

        // Fila de entrada (ver Player.prototype.send). O servidor recusa 'text' acima de 500.
        var INPUT_ACK_TIMEOUT_MS = 400;
        var INPUT_TEXT_MAX = 400;

        // Teclas que o endpoint WebSocket EM PRODUCAO recusa com "tecla desconhecida": a
        // classe RemoteViewerEndpoint no ar (build de 21-09) so' aceita back/home/recents/
        // notifications. O endpoint HTTP /input (RemoteSupportResource.input) repassa ao
        // agente sem essa lista, com a mesma permissao de controle, e o agente valida o nome
        // por conta propria. Sem isto Backspace, Enter, Tab e setas nunca chegavam.
        var KEYS_VIA_HTTP = {backspace: true, enter: true, tab: true, left: true, right: true};

        // Configuracao do codec (SPS/PPS) por aparelho, guardada no navegador. O agente so'
        // a envia no inicio da transmissao; ver o uso em onFrame. Nao e' dado sensivel: sao
        // parametros do codificador de video. Guarda a resolucao junto para nunca aplicar
        // uma configuracao de outra resolucao.
        var CODEC_STORAGE_PREFIX = 'hwmdm.remote.codec.';

        var saveCodecConfig = function (deviceId, payload, width, height) {
            try {
                var bin = '';
                for (var i = 0; i < payload.length; i++) {
                    bin += String.fromCharCode(payload[i]);
                }
                $window.localStorage.setItem(CODEC_STORAGE_PREFIX + deviceId,
                    JSON.stringify({c: $window.btoa(bin), w: width || 0, h: height || 0}));
            } catch (e) {
                // localStorage indisponivel: so' perde a retomada apos F5.
            }
        };

        var loadCodecConfig = function (deviceId, width, height) {
            try {
                var raw = JSON.parse($window.localStorage.getItem(CODEC_STORAGE_PREFIX + deviceId));
                if (!raw || !raw.c) {
                    return null;
                }
                if (width && height && raw.w && raw.h && (raw.w !== width || raw.h !== height)) {
                    return null;
                }
                var bin = $window.atob(raw.c);
                var out = new Uint8Array(bin.length);
                for (var i = 0; i < bin.length; i++) {
                    out[i] = bin.charCodeAt(i);
                }
                return out;
            } catch (e) {
                return null;
            }
        };

        function Player(canvas, handlers, deviceId) {
            this.deviceId = deviceId;
            this.haveConfig = false;
            this.outbox = [];
            this.inputInFlight = false;
            this.inputAckTimer = null;
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
            // MSE fallback via muxjs.mp4.generator
            this.useMse = !supportsWebCodecs() && supportsMse();
            this.mseVideo = null;
            this.mseSource = null;
            this.mseBuffer = null;
            this.mseInitSent = false;
            this.mseSeqNum = 1;
            this.mseTimestamp = 0;
            this.mseDuration = 3000;
            this.mseWidth = 0;
            this.mseHeight = 0;
            this.mseConfigNal = null;
            this.mseCodecString = null;
            this.mseAppendQueue = [];
            this.mseAppending = false;
            this.jmuxer = null;
            this.mseMouseDown = null;
            this.mseMouseUp = null;
            this.mseTouchStart = null;
            this.mseTouchEnd = null;
        }

        Player.prototype.open = function (socketPath) {
            var self = this;
            this.closed = false;

            // MSE fallback: cria video element
            if (this.useMse) {
                this.mseVideo = document.createElement('video');
                this.mseVideo.className = 'remote-live-canvas remote-live-video';
                this.mseVideo.setAttribute('playsinline', '');
                this.mseVideo.setAttribute('autoplay', '');
                this.mseVideo.muted = true;
                this.mseMouseDown = function (event) {
                    event.preventDefault();
                    self.report('onPointerDown', event);
                };
                this.mseMouseUp = function (event) {
                    event.preventDefault();
                    self.report('onPointerUp', event);
                };
                this.mseTouchStart = function (event) {
                    event.preventDefault();
                    self.report('onPointerDown', event);
                };
                this.mseTouchEnd = function (event) {
                    event.preventDefault();
                    self.report('onPointerUp', event);
                };
                if ($window.PointerEvent) {
                    this.mseVideo.addEventListener('pointerdown', this.mseMouseDown);
                    this.mseVideo.addEventListener('pointerup', this.mseMouseUp);
                } else {
                    this.mseVideo.addEventListener('mousedown', this.mseMouseDown);
                    this.mseVideo.addEventListener('mouseup', this.mseMouseUp);
                    this.mseVideo.addEventListener('touchstart', this.mseTouchStart, {passive: false});
                    this.mseVideo.addEventListener('touchend', this.mseTouchEnd, {passive: false});
                }
                if (this.canvas && this.canvas.parentNode) {
                    this.canvas.parentNode.insertBefore(this.mseVideo, this.canvas);
                    this.canvas.style.display = 'none';
                }
                this.initMse();
            }

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
            };
            socket.onclose = function (event) {
                if (self.closed) {
                    return;
                }
                if (!self.opened) {
                    self.socket = null;
                    self.startHttp();
                    return;
                }
                // The support session survives a viewer transport failure.
                self.socket = null;
                self.cursor = 0;
                self.startHttp();
            };
            socket.onopen = function () {
                self.opened = true;
            };
        };

        Player.prototype.startHttp = function () {
            var self = this;
            if (this.http || this.closed) { return; }
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
                    this.mseWidth = message.width;
                    this.mseHeight = message.height;
                }
                this.report('onStatus', message);
            } else if (message.type === 'input-result') {
                this.inputAcked();
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
                saveCodecConfig(this.deviceId, payload, this.mseWidth, this.mseHeight);
                this.applyConfig(payload);
                return;
            }

            // Espectador que chegou depois do inicio (F5, troca de tela, outra aba): pelo
            // WebSocket o hub no ar reenvia o ultimo quadro-chave mas NAO a configuracao do
            // codec, e sem SPS/PPS nada decodifica -- a tela ficava preta. Usa a
            // configuracao guardada neste navegador (mesma resolucao) ou busca uma vez pelo
            // endpoint HTTP, que devolve configuracao + ultimo quadro-chave.
            if (!this.haveConfig) {
                if (type === FRAME_KEY) {
                    var stored = loadCodecConfig(this.deviceId, this.mseWidth, this.mseHeight);
                    if (stored) {
                        this.applyConfig(stored);
                    } else {
                        this.fetchConfig();
                        return;
                    }
                } else {
                    return;     // delta sem configuracao nao decodifica
                }
            }

            if (this.useMse) {
                this.mseFeed(type, timestamp, payload);
                return;
            }

            if (!this.decoder || this.decoder.state !== 'configured') {
                return;
            }
            if (type === FRAME_DELTA && this.awaitingKeyFrame) {
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

        Player.prototype.fetchConfig = function () {
            if (this.fetchingConfig || this.http || this.closed) {
                return;     // no transporte HTTP a propria sondagem ja' traz a configuracao
            }
            var self = this;
            this.fetchingConfig = true;
            var done = function () {
                self.fetchingConfig = false;
            };
            $http.get('rest/private/remote-support/' + this.deviceId + '/frames',
                      {params: {since: 0}, timeout: 20000})
                .then(function (res) {
                    done();
                    var data = res.data && res.data.data;
                    if (self.closed || self.haveConfig || !data) {
                        return;
                    }
                    (data.frames || []).forEach(function (b64) {
                        var bytes = base64ToBytes(b64);
                        if (bytes[0] === FRAME_CONFIG || bytes[0] === FRAME_KEY) {
                            self.onFrame(bytes);
                        }
                    });
                }, done);
        };

        Player.prototype.applyConfig = function (payload) {
            this.haveConfig = true;
            this.pendingConfig = payload;
            this.codecString = codecFromParameterSet(payload) || this.codecString;
            this.mseConfigNal = payload;
            this.mseCodecString = this.codecString;
            if (this.useMse) {
                this.mseInitSent = false;
                this.mseSeqNum = 1;
                this.mseTimestamp = 0;
                return;
            }
            this.configure();
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
                        if (self.closed) { return; }
                        self.haveConfig = false;
                        self.awaitingKeyFrame = true;
                        self.report('onError', 'remote.error.stream.decode');
                    }
                });
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
            } finally {
                frame.close();
            }
        };

        // =============================================================================================================
        // MSE fallback via muxjs.mp4.generator
        //
        // Quando WebCodecs nao esta disponivel (Electron VS Code), construimos MP4
        // fragmentado usando as funcoes do muxjs.mp4.generator e alimentamos o
        // MediaSource. O generator produz boxes MP4 validos (ftyp, moov, moof, mdat)
        // a partir de descricoes de track e dados de amostra.

        Player.prototype.initMse = function () {
            try {
                this.jmuxer = new $window.JMuxer({
                    node: this.mseVideo,
                    mode: 'video',
                    flushingTime: 0,
                    fps: 15,
                    debug: false
                });
                this.mseReady = true;
            } catch (e) {
                this.report('onError', 'remote.error.stream.unsupported');
            }
        };

        Player.prototype.mseAppend = function (data) {
            if (!this.mseBuffer) {
                return;
            }
            if (this.mseAppending) {
                this.mseAppendQueue.push(data);
                return;
            }
            try {
                this.mseAppending = true;
                this.mseBuffer.appendBuffer(data);
            } catch (e) {
                this.mseAppending = false;
                this.report('onError', 'remote.error.stream.decode');
            }
        };

        Player.prototype.mseFlushQueue = function () {
            if (this.mseAppendQueue.length === 0) {
                return;
            }
            var data = this.mseAppendQueue.shift();
            try {
                this.mseAppending = true;
                this.mseBuffer.appendBuffer(data);
            } catch (e) {
                this.mseAppending = false;
                this.report('onError', 'remote.error.stream.decode');
            }
        };

        Player.prototype.mseFeed = function (type, timestamp, payload) {
            if (!this.mseReady || !this.jmuxer) {
                return;
            }
            var data = (type === FRAME_KEY && this.mseConfigNal)
                ? concat(this.mseConfigNal, payload)
                : payload;
            try {
                this.jmuxer.feed({video: data});
                if (this.mseVideo && this.mseVideo.play) {
                    var promise = this.mseVideo.play();
                    if (promise && promise.catch) {
                        promise.catch(function () {});
                    }
                }
            } catch (e) {
                this.report('onError', 'remote.error.stream.decode');
            }
        };

        // =============================================================================================================

        Player.prototype.send = function (command) {
            if (this.http) {
                // Em fila: POSTs em paralelo podem chegar fora de ordem ao servidor, e
                // "abc", Backspace, "d" viraria outra coisa no aparelho.
                var url = 'rest/private/remote-support/' + this.deviceId + '/input';
                var post = function () {
                    return $http.post(url, command);
                };
                var sent = this.httpInput ? this.httpInput.then(post, post) : post();
                this.httpInput = sent.then(angular.noop, angular.noop);
                return true;
            }
            if (!this.socket || this.socket.readyState !== 1) {
                return false;
            }
            /*
             * Um comando em voo por vez. O servidor repassa cada comando ao agente com um
             * envio assincrono que NAO aceita dois envios sobrepostos no mesmo socket: o
             * segundo estoura IllegalStateException (TEXT_FULL_WRITING) e e' descartado --
             * era assim que teclas sumiam ao digitar. O agente responde 'input-result' a
             * todo comando; o proximo so' sai depois dessa resposta (ou do prazo abaixo).
             * Texto digitado enquanto espera e' juntado num unico comando, entao a
             * velocidade de digitacao nao fica presa ao tempo de ida e volta.
             */
            var last = this.outbox[this.outbox.length - 1];
            // Juntar so' se o resultado nao terminar em espaco: o espaco final de um trecho se
            // perde em campos como o omnibox do Chrome (ver queueText no controller).
            if (command.type === 'text' && last && last.type === 'text'
                    && (last.text.length + command.text.length) <= INPUT_TEXT_MAX
                    && !/\s$/.test(last.text + command.text)) {
                last.text += command.text;
            } else {
                this.outbox.push(command);
            }
            this.pumpInput();
            return true;
        };

        Player.prototype.pumpInput = function () {
            if (this.inputInFlight || this.outbox.length === 0) {
                return;
            }
            if (!this.socket || this.socket.readyState !== 1) {
                this.outbox = [];
                return;
            }
            var command = this.outbox.shift();
            if (command.type === 'key' && KEYS_VIA_HTTP[command.name] === true) {
                // A resposta do agente ('input-result') volta pelo socket do espectador e
                // libera a fila do mesmo jeito.
                $http.post('rest/private/remote-support/' + this.deviceId + '/input', command);
            } else {
                try {
                    this.socket.send(JSON.stringify(command));
                } catch (e) {
                    this.outbox = [];
                    return;
                }
            }
            var self = this;
            this.inputInFlight = true;
            this.inputAckTimer = $timeout(function () {
                self.inputAcked();
            }, INPUT_ACK_TIMEOUT_MS, false);
        };

        Player.prototype.inputAcked = function () {
            if (this.inputAckTimer) {
                $timeout.cancel(this.inputAckTimer);
                this.inputAckTimer = null;
            }
            this.inputInFlight = false;
            this.pumpInput();
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

        Player.prototype.toUnit = function (event) {
            var target = (event && event.currentTarget && event.currentTarget.getBoundingClientRect)
                ? event.currentTarget
                : this.canvas;
            var rect = target.getBoundingClientRect();
            if (rect.width === 0 || rect.height === 0) {
                return null;
            }
            var point = event.changedTouches && event.changedTouches.length > 0
                ? event.changedTouches[0]
                : event;
            return {
                x: Math.max(0, Math.min(1, (point.clientX - rect.left) / rect.width)),
                y: Math.max(0, Math.min(1, (point.clientY - rect.top) / rect.height))
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
            this.teardownMse();
            if (this.decoder) {
                try {
                    if (this.decoder.state !== 'closed') {
                        this.decoder.close();
                    }
                } catch (e) {
                }
                this.decoder = null;
            }
        };

        Player.prototype.teardownMse = function () {
            if (this.jmuxer) {
                try {
                    this.jmuxer.destroy();
                } catch (e) {
                }
                this.jmuxer = null;
            }
            if (this.mseSource) {
                try {
                    if (this.mseSource.readyState !== 'ended') {
                        this.mseSource.endOfStream();
                    }
                } catch (e) {
                }
                this.mseSource = null;
            }
            this.mseBuffer = null;
            this.mseReady = false;
            if (this.mseVideo) {
                try {
                    if (this.mseMouseDown) {
                        this.mseVideo.removeEventListener('mousedown', this.mseMouseDown);
                        this.mseVideo.removeEventListener('pointerdown', this.mseMouseDown);
                    }
                    if (this.mseMouseUp) {
                        this.mseVideo.removeEventListener('mouseup', this.mseMouseUp);
                        this.mseVideo.removeEventListener('pointerup', this.mseMouseUp);
                    }
                    if (this.mseTouchStart) {
                        this.mseVideo.removeEventListener('touchstart', this.mseTouchStart);
                    }
                    if (this.mseTouchEnd) {
                        this.mseVideo.removeEventListener('touchend', this.mseTouchEnd);
                    }
                    this.mseVideo.pause();
                    this.mseVideo.src = '';
                } catch (e) {
                }
                if (this.mseVideo.parentNode) {
                    this.mseVideo.parentNode.removeChild(this.mseVideo);
                }
                this.mseVideo = null;
            }
            this.mseMouseDown = null;
            this.mseMouseUp = null;
            this.mseTouchStart = null;
            this.mseTouchEnd = null;
        };

        Player.prototype.close = function () {
            this.closed = true;
            this.handlers = {};
            this.outbox = [];
            if (this.inputAckTimer) {
                $timeout.cancel(this.inputAckTimer);
                this.inputAckTimer = null;
            }
            this.inputInFlight = false;
            this.teardownDecoder();
            this.teardownMse();
            if (this.socket) {
                try {
                    this.socket.close(1000, 'visualizador fechado');
                } catch (e) {
                }
                this.socket = null;
            }
            try {
                this.context.clearRect(0, 0, this.canvas.width, this.canvas.height);
            } catch (e) {
            }
        };

        return {
            isSupported: isSupported,
            create: function (canvas, handlers, deviceId) {
                return new Player(canvas, handlers, deviceId);
            }
        };
    });
