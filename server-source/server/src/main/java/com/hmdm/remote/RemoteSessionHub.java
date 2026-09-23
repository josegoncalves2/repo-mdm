/*
 *
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Copyright (C) 2019 Headwind Solutions LLC (http://h-sms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hmdm.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>As sessoes de suporte remoto que este servidor esta intermediando, em memoria.</p>
 *
 * <p>Uma sessao tem tres partes: o navegador do operador pede, o agente do aparelho
 * (com.hwmdm.remote) empurra o video da tela, e o servidor repassa. No sentido contrario
 * descem os comandos de toque e digitacao. Nada e' gravado em disco e nada sobra depois --
 * os quadros existem enquanto alguem esta olhando, que e' a diferenca entre isto e o
 * mecanismo de imagem parada que existia antes.</p>
 *
 * <p>Singleton estatico, e nao um binding do Guice, porque os endpoints WebSocket sao
 * instanciados pelo container e nao pelo injetor, e os dois lados precisam alcancar a
 * mesma instancia.</p>
 */
public final class RemoteSessionHub {

    private static final Logger logger = LoggerFactory.getLogger(RemoteSessionHub.class);

    /**
     * Uma sessao cujo agente nunca conecta e' abandonada quando o aparelho estava (supostamente)
     * online no momento do pedido: o push deveria ter chegado na hora, entao dois minutos sem o
     * agente aparecer e' sinal de algo errado (push perdido, operador fechou o painel antes de o
     * aparelho atender), e nao vale a pena manter a vaga e o token validos por mais tempo.
     */
    private static final long PENDING_TIMEOUT_MS = 120_000L;

    /**
     * <p>Prazo para um pedido registrado com o aparelho JA' sabido offline no momento do pedido
     * (item 3 da queixa: "as requisicoes se perdem no caminho"). O push fica gravado em
     * {@code pendingpushes} e so' e' entregue quando o aparelho sincronizar de novo -- pode ser
     * minutos ou horas depois, fora do controle deste servidor.</p>
     *
     * <p>Trinta minutos e' o meio-termo escolhido: curto o bastante para nao deixar um token
     * valido por tempo indefinido (superficie de ataque desnecessaria, e o {@code status} do
     * painel ficaria "aguardando" para sempre sem nunca virar erro), longo o bastante para cobrir
     * um aparelho que estava com a tela apagada ou em local sem sinal e sincroniza de novo dentro
     * do mesmo atendimento. Depois disso o operador ve o pedido expirado e decide se abre outro --
     * ele tambem pode cancelar antes, pelo mesmo botao de sempre.</p>
     */
    private static final long PENDING_OFFLINE_TIMEOUT_MS = 30 * 60_000L;

    /**
     * <p>Quanto uma sessao com o agente transmitindo sobrevive sem NENHUM espectador antes de
     * ser encerrada (item 4 da queixa: F5 e troca de aba nao podem mais derrubar a sessao no
     * servidor, mas um tablet nao pode ficar transmitindo para sempre so' porque ninguem nunca
     * mais voltou a olhar).</p>
     *
     * <p>Tres minutos cobre confortavelmente um F5, uma troca de aba do menu e ate' uma queda
     * breve de rede do navegador do operador -- tudo isso agora reata em vez de reabrir sessao
     * nova. Alem disso, presume-se que o atendimento acabou: manter o encoder do aparelho ligado
     * gasta bateria e dados do tablet sem ninguem observando.</p>
     */
    private static final long VIEWER_GRACE_MS = 3 * 60_000L;

    /** Quadros guardados para o transporte HTTP: ~2s de video a 15 fps. */
    private static final int MAX_BUFFERED_FRAMES = 30;

    public static final byte FRAME_CONFIG = 1;
    public static final byte FRAME_KEY = 2;
    public static final byte FRAME_DELTA = 3;

    private static final RemoteSessionHub INSTANCE = new RemoteSessionHub();

    static {
        /*
         * Varredura periodica das sessoes.
         *
         * Antes desta trava, reapStale() so' era chamado dentro de open() -- ou seja, uma
         * sessao so' era reavaliada quando ALGUEM ABRIA OUTRA sessao de suporte remoto (para
         * qualquer aparelho). Um tablet podia ficar transmitindo para zero espectadores por
         * horas se nenhum outro chamado acontecesse nesse intervalo, e um pedido pendente para
         * um aparelho offline podia ser descartado cedo demais (ou tarde demais) dependendo so'
         * de quando outro operador usasse a tela em outro aparelho -- coincidencia, nao prazo.
         *
         * Um unico thread daemon, de baixa frequencia, resolve isso sem exigir nenhuma outra
         * peca de infraestrutura (nao ha Quartz nem outro agendador disponivel neste modulo).
         */
        java.util.concurrent.ScheduledExecutorService reaper =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "remote-support-reaper");
                    thread.setDaemon(true);
                    return thread;
                });
        reaper.scheduleWithFixedDelay(() -> {
            try {
                INSTANCE.reapStale();
            } catch (Exception e) {
                logger.warn("Falha na varredura periodica de sessoes de suporte remoto", e);
            }
        }, 30, 30, java.util.concurrent.TimeUnit.SECONDS);
    }

    private final Map<String, RemoteSession> byDeviceNumber = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private RemoteSessionHub() {
    }

    public static RemoteSessionHub getInstance() {
        return INSTANCE;
    }

    /**
     * <p>Uma sessao de suporte para um aparelho.</p>
     *
     * <p>A ultima configuracao de codec e o ultimo quadro-chave ficam guardados para que um
     * navegador que entre no meio -- ou que recarregue a pagina -- comece a decodificar na
     * hora, em vez de olhar um retangulo cinza ate o encoder resolver emitir o proximo
     * quadro-chave.</p>
     */
    public static final class RemoteSession {
        private final String deviceNumber;
        private final int deviceId;
        private final String token;
        private final long createdAt = System.currentTimeMillis();

        private volatile Session agent;
        private final Set<Session> viewers = Collections.newSetFromMap(new ConcurrentHashMap<>());

        private volatile int width;
        private volatile int height;
        private volatile boolean inputAvailable;
        private volatile byte[] codecConfig;
        private volatile byte[] lastKeyFrame;
        private volatile long frames;
        private volatile long bytes;

        /**
         * O aparelho era sabido offline no momento em que o pedido foi feito. Controla qual
         * prazo de abandono se aplica antes de o agente conectar (ver {@link #reapStale()}).
         */
        private volatile boolean pendingOffline;

        /**
         * Instante (epoch ms) desde quando a sessao esta' sem nenhum espectador, ou {@code 0}
         * quando ha' pelo menos um agora. Comeca em {@code createdAt}: uma sessao recem-aberta
         * ainda nao tem espectador nenhum, entao o relogio da carencia ja' esta' correndo caso
         * o agente conecte e nenhum navegador jamais apareca para assistir.
         */
        private volatile long viewerlessSince;

        /*
         * Buffer circular dos quadros recentes, para o transporte HTTP.
         *
         * O WebSocket empurra; o long polling precisa PUXAR, e para isso o quadro tem de
         * continuar existindo por alguns instantes depois de gerado. Guardamos poucos e por
         * pouco tempo de proposito: o objetivo e' cobrir o intervalo entre duas requisicoes
         * do navegador, nao manter historico. Video ao vivo antigo nao tem valor.
         */
        private final java.util.ArrayDeque<byte[]> recent = new java.util.ArrayDeque<>();
        private volatile long sequence;
        private final Object pump = new Object();

        private RemoteSession(int deviceId, String deviceNumber, String token) {
            this.deviceId = deviceId;
            this.deviceNumber = deviceNumber;
            this.token = token;
            this.viewerlessSince = this.createdAt;
        }

        public String getDeviceNumber() { return deviceNumber; }
        public int getDeviceId() { return deviceId; }
        public String getToken() { return token; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public boolean isInputAvailable() { return inputAvailable; }
        public long getFrames() { return frames; }
        public long getBytes() { return bytes; }
        public int getViewerCount() { return viewers.size(); }
        public boolean isStreaming() { return agent != null && agent.isOpen(); }
        /** Quando o pedido foi feito (epoch ms) -- o painel mostra isto no estado "pendente". */
        public long getCreatedAt() { return createdAt; }
        /** O aparelho estava offline quando este pedido foi registrado. */
        public boolean isPendingOffline() { return pendingOffline; }
    }

    // =================================================================================================================

    /**
     * <p>Abre uma vaga de sessao e devolve o token de uso unico que o agente tera de
     * apresentar.</p>
     *
     * <p>Pedir uma sessao para um aparelho que ja tem uma substitui a anterior: o operador
     * pedindo de novo esta pedindo um fluxo novo, e manter o socket antigo deixaria dois
     * encoders empurrando para o mesmo relay.</p>
     *
     * @param deviceOnline se o aparelho estava online no instante do pedido. Determina qual
     *                     prazo de abandono vale ate' o agente conectar -- ver
     *                     {@link #PENDING_TIMEOUT_MS} e {@link #PENDING_OFFLINE_TIMEOUT_MS}.
     */
    public RemoteSession open(int deviceId, String deviceNumber, boolean deviceOnline) {
        reapStale();
        byte[] raw = new byte[24];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        RemoteSession session = new RemoteSession(deviceId, deviceNumber, token);
        session.pendingOffline = !deviceOnline;
        RemoteSession previous = byDeviceNumber.put(deviceNumber, session);
        if (previous != null) {
            closeQuietly(previous.agent, "substituida por uma nova sessao");
            for (Session viewer : previous.viewers) {
                closeQuietly(viewer, "substituida por uma nova sessao");
            }
        }
        logger.info("Sessao de suporte remoto aberta para o aparelho '{}' (aparelho {} no pedido)",
                deviceNumber, deviceOnline ? "online" : "offline");
        return session;
    }

    public RemoteSession find(String deviceNumber) {
        return deviceNumber == null ? null : byDeviceNumber.get(deviceNumber);
    }

    public void close(String deviceNumber) {
        RemoteSession session = byDeviceNumber.remove(deviceNumber);
        if (session == null) {
            return;
        }
        closeQuietly(session.agent, "sessao encerrada");
        for (Session viewer : session.viewers) {
            closeQuietly(viewer, "sessao encerrada");
        }
        logger.info("Sessao encerrada para '{}' apos {} quadros / {} bytes",
                deviceNumber, session.frames, session.bytes);
    }

    // =================================================================================================================

    /**
     * <p>Prende o socket do agente, recusando token que nao bata com a sessao pendente.</p>
     *
     * <p>O token e' emitido pelo servidor e entregue apenas dentro do push enderecado
     * aquele aparelho, entao um fluxo so' pode ser empurrado para uma sessao que um
     * operador autorizado realmente pediu.</p>
     */
    public RemoteSession attachAgent(String deviceNumber, String token, Session socket) {
        RemoteSession session = byDeviceNumber.get(deviceNumber);
        if (session == null) {
            logger.warn("Fluxo recusado para '{}': nenhuma sessao foi pedida", deviceNumber);
            return null;
        }
        if (token == null || !constantTimeEquals(session.token, token)) {
            logger.warn("Fluxo recusado para '{}': token nao confere", deviceNumber);
            return null;
        }
        closeQuietly(session.agent, "substituido por uma conexao mais nova do agente");
        session.agent = socket;
        // O agente conectou, entao o aparelho provou que esta' alcancavel agora -- o prazo
        // longo de "pedido feito com aparelho offline" deixa de fazer sentido a partir daqui.
        session.pendingOffline = false;
        logger.info("Agente conectado para '{}'", deviceNumber);
        notifyViewers(session);
        return session;
    }

    public void detachAgent(String deviceNumber, Session socket) {
        RemoteSession session = byDeviceNumber.get(deviceNumber);
        if (session == null || session.agent != socket) {
            return;
        }
        session.agent = null;
        logger.info("Agente desconectado de '{}'", deviceNumber);
        notifyViewers(session);
    }

    public boolean attachViewer(String deviceNumber, Session socket) {
        RemoteSession session = byDeviceNumber.get(deviceNumber);
        if (session == null) {
            return false;
        }
        session.viewers.add(socket);
        // Ha' espectador de novo: o relogio da carencia de "ninguem esta' olhando" para.
        session.viewerlessSince = 0;
        logger.info("Espectador anexado a '{}' (total={}, agente_conectado={})",
                deviceNumber, session.viewers.size(), session.isStreaming());
        sendText(socket, describe(session));
        byte[] config = session.codecConfig;
        byte[] key = session.lastKeyFrame;
        if (config != null) {
            sendBinary(socket, config);
        }
        if (key != null) {
            sendBinary(socket, key);
        }
        return true;
    }

    public void detachViewer(String deviceNumber, Session socket) {
        RemoteSession session = byDeviceNumber.get(deviceNumber);
        if (session != null) {
            session.viewers.remove(socket);
            if (session.viewers.isEmpty()) {
                // Marca o inicio da carencia (VIEWER_GRACE_MS). Isto e' o que sobrevive a um
                // F5 ou a uma troca de aba: o navegador fecha este socket de proposito ao
                // desmontar a tela, mas a sessao no aparelho continua ligada ate' o prazo
                // vencer ou o painel reatar (ver reapStale()).
                session.viewerlessSince = System.currentTimeMillis();
            }
        }
    }

    public void describeStream(String deviceNumber, int width, int height, boolean inputAvailable) {
        RemoteSession session = byDeviceNumber.get(deviceNumber);
        if (session == null) {
            return;
        }
        session.width = width;
        session.height = height;
        session.inputAvailable = inputAvailable;
        logger.info("Fluxo descrito por '{}': {}x{} entrada={} espectadores={}",
                deviceNumber, width, height, inputAvailable, session.viewers.size());
        // Descricao nova invalida os quadros guardados do fluxo anterior.
        session.codecConfig = null;
        session.lastKeyFrame = null;
        notifyViewers(session);
    }

    /**
     * <p>Repassa um quadro do agente para todo navegador que estiver olhando.</p>
     *
     * <p>Um espectador com o socket congestionado e' pulado, nao esperado: uma conexao lenta
     * de um operador nao pode travar o encoder nem os demais espectadores. Descartar quadro
     * e' o comportamento correto em video ao vivo -- eles ja nao valem nada quando
     * chegariam.</p>
     */
    public void relayFrame(String deviceNumber, byte[] frame) {
        RemoteSession session = byDeviceNumber.get(deviceNumber);
        if (session == null || frame == null || frame.length == 0) {
            return;
        }
        session.frames++;
        session.bytes += frame.length;
        // Primeiros quadros logados: sem isto nao ha como distinguir "agente nao envia" de
        // "relay nao repassa" de "navegador nao decodifica".
        if (session.frames <= 3 || session.frames % 200 == 0) {
            logger.info("Quadro {} de '{}': tipo={} bytes={} espectadores={}",
                    session.frames, deviceNumber, frame[0], frame.length, session.viewers.size());
        }

        final byte type = frame[0];
        if (type == FRAME_CONFIG) {
            session.codecConfig = frame;
        } else if (type == FRAME_KEY) {
            session.lastKeyFrame = frame;
        }

        Iterator<Session> iterator = session.viewers.iterator();
        while (iterator.hasNext()) {
            Session viewer = iterator.next();
            if (!viewer.isOpen()) {
                iterator.remove();
                continue;
            }
            sendBinary(viewer, frame);
        }

        // Alimenta tambem o transporte HTTP e acorda quem estiver em long polling.
        synchronized (session.pump) {
            session.recent.addLast(frame);
            while (session.recent.size() > MAX_BUFFERED_FRAMES) {
                session.recent.pollFirst();
            }
            session.sequence++;
            session.pump.notifyAll();
        }
    }

    /**
     * <p>Transporte HTTP: espera ate {@code waitMs} por quadros mais novos que {@code since} e
     * devolve o que houver.</p>
     *
     * <p>Existe porque nem todo proxy reverso repassa upgrade de WebSocket -- o desta
     * instalacao nao repassa, e o resultado era o servidor gerando video para
     * "espectadores=0". Long polling e' exatamente o que o canal de push do MDM ja usa e
     * atravessa o mesmo proxy sem ajuste nenhum, entao a imagem chega pelo caminho que
     * comprovadamente funciona.</p>
     *
     * @return quadros novos e o cursor atualizado, ou lista vazia quando nada chegou a tempo.
     */
    public PolledFrames poll(String deviceNumber, long since, long waitMs) {
        RemoteSession session = byDeviceNumber.get(deviceNumber);
        if (session == null) {
            return new PolledFrames(since, java.util.Collections.emptyList(), false, 0, 0, false);
        }
        final long deadline = System.currentTimeMillis() + Math.max(0, waitMs);
        synchronized (session.pump) {
            while (session.sequence <= since && System.currentTimeMillis() < deadline) {
                try {
                    session.pump.wait(Math.max(1, deadline - System.currentTimeMillis()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            java.util.List<byte[]> out = new java.util.ArrayList<>();
            if (session.sequence > since) {
                // Quando o cliente ficou para tras alem do buffer, recomecamos do que existe:
                // reenviar a configuracao e o ultimo quadro-chave e' o que permite decodificar.
                long behind = session.sequence - since;
                if (behind > session.recent.size()) {
                    if (session.codecConfig != null) {
                        out.add(session.codecConfig);
                    }
                    if (session.lastKeyFrame != null) {
                        out.add(session.lastKeyFrame);
                    }
                } else {
                    java.util.Iterator<byte[]> it = session.recent.iterator();
                    long skip = session.recent.size() - behind;
                    while (skip-- > 0 && it.hasNext()) {
                        it.next();
                    }
                    while (it.hasNext()) {
                        out.add(it.next());
                    }
                }
            }
            return new PolledFrames(session.sequence, out, session.isStreaming(),
                    session.width, session.height, session.inputAvailable);
        }
    }

    /** Resposta do transporte HTTP. */
    public static final class PolledFrames {
        public final long cursor;
        public final java.util.List<byte[]> frames;
        public final boolean streaming;
        public final int width;
        public final int height;
        public final boolean input;

        PolledFrames(long cursor, java.util.List<byte[]> frames, boolean streaming,
                     int width, int height, boolean input) {
            this.cursor = cursor;
            this.frames = frames;
            this.streaming = streaming;
            this.width = width;
            this.height = height;
            this.input = input;
        }
    }

    /**
     * <p>Encaminha um comando de toque ou digitacao do painel ao aparelho.</p>
     *
     * @return false quando nao ha agente conectado, para que o painel diga ao operador que
     *         o clique nao saiu, em vez de deixa-lo achar que a tela travou.
     */
    public boolean relayCommand(String deviceNumber, String json) {
        RemoteSession session = byDeviceNumber.get(deviceNumber);
        if (session == null) {
            return false;
        }
        Session agent = session.agent;
        if (agent == null || !agent.isOpen()) {
            return false;
        }
        sendText(agent, json);
        return true;
    }

    /** Repassa aos espectadores o retorno do agente sobre um comando de entrada. */
    public void relayToViewers(String deviceNumber, String json) {
        RemoteSession session = byDeviceNumber.get(deviceNumber);
        if (session == null) {
            return;
        }
        for (Session viewer : session.viewers) {
            sendText(viewer, json);
        }
    }

    // =================================================================================================================

    public String describe(RemoteSession session) {
        return "{\"type\":\"status\""
                + ",\"streaming\":" + session.isStreaming()
                + ",\"pending\":" + !session.isStreaming()
                + ",\"requestedAt\":" + session.createdAt
                + ",\"width\":" + session.width
                + ",\"height\":" + session.height
                + ",\"input\":" + session.inputAvailable
                + ",\"viewers\":" + session.viewers.size()
                + ",\"frames\":" + session.frames
                + ",\"bytes\":" + session.bytes
                + "}";
    }

    private void notifyViewers(RemoteSession session) {
        String status = describe(session);
        for (Session viewer : session.viewers) {
            sendText(viewer, status);
        }
    }

    /**
     * <p>Varredura unica que cobre os dois prazos da sessao: o agente que nunca aparece (o
     * pedido em si expira) e o espectador que some de uma sessao ja' viva (a carencia do item
     * 4 -- ver {@link #VIEWER_GRACE_MS}). Chamada tanto por {@link #open(int, String, boolean)}
     * quanto pelo thread periodico registrado no bloco {@code static} da classe.</p>
     */
    private void reapStale() {
        long now = System.currentTimeMillis();
        byDeviceNumber.entrySet().removeIf(entry -> {
            RemoteSession session = entry.getValue();

            if (session.isStreaming()) {
                // Sessao viva: so' cai se ficou tempo demais sem NENHUM espectador. Isto e' o
                // que evita o tablet transmitir para sempre depois que todo mundo fechou a
                // aba -- mas da' tempo de um F5 ou uma troca de aba reatar primeiro.
                boolean abandoned = session.viewers.isEmpty()
                        && session.viewerlessSince > 0
                        && now - session.viewerlessSince > VIEWER_GRACE_MS;
                if (abandoned) {
                    logger.info("Encerrando sessao de '{}': sem espectador ha' mais de {} ms",
                            entry.getKey(), VIEWER_GRACE_MS);
                    closeQuietly(session.agent, "sem espectador ha' tempo demais");
                    for (Session viewer : session.viewers) {
                        closeQuietly(viewer, "sem espectador ha' tempo demais");
                    }
                }
                return abandoned;
            }

            // Sem agente conectado: o pedido ainda esta' esperando o aparelho atender. Qual
            // prazo vale depende de o aparelho ja' estar sabido offline no instante do pedido
            // (PENDING_OFFLINE_TIMEOUT_MS, bem mais longo) ou de ele estar supostamente
            // alcancavel e simplesmente nao ter respondido ainda (PENDING_TIMEOUT_MS).
            long timeout = session.pendingOffline ? PENDING_OFFLINE_TIMEOUT_MS : PENDING_TIMEOUT_MS;
            boolean stale = now - session.createdAt > timeout;
            if (stale) {
                logger.info("Descartando pedido pendente de '{}' apos {} ms (prazo {} ms, offline_no_pedido={})",
                        entry.getKey(), now - session.createdAt, timeout, session.pendingOffline);
            }
            return stale;
        });
    }

    /** Um envio em voo por espectador; o excedente espera numa fila curta. */
    private static final int MAX_FILA_ESPECTADOR = 8;

    private static final class EscritaEspectador {
        private final java.util.ArrayDeque<byte[]> fila = new java.util.ArrayDeque<>();
        private boolean escrevendo;
        private long descartados;
    }

    private static EscritaEspectador escritaDe(Session socket) {
        // getUserProperties() e' um mapa por sessao mantido pelo proprio container.
        synchronized (socket) {
            Object atual = socket.getUserProperties().get("hwmdm.escrita");
            if (atual == null) {
                atual = new EscritaEspectador();
                socket.getUserProperties().put("hwmdm.escrita", atual);
            }
            return (EscritaEspectador) atual;
        }
    }

    /**
     * <p>O {@code AsyncRemote} do Tomcat aceita UM envio binario em voo por socket. Chamado a cada
     * quadro sem esperar o anterior, ele lanca {@code IllegalStateException [BINARY_FULL_WRITING]}
     * e o quadro se perdia em silencio assim que o navegador drenava mais devagar que o aparelho
     * transmitia. Aqui o excedente espera numa fila curta e o quadro velho e' descartado no lugar
     * do novo, que e' o que o espectador precisa ver.</p>
     */
    private static void sendBinary(Session socket, byte[] payload) {
        EscritaEspectador e = escritaDe(socket);
        synchronized (e) {
            if (e.escrevendo) {
                if (e.fila.size() >= MAX_FILA_ESPECTADOR) {
                    e.fila.pollFirst();
                    if (++e.descartados % 100 == 1) {
                        logger.info("Espectador lento: {} quadros descartados", e.descartados);
                    }
                }
                e.fila.addLast(payload);
                return;
            }
            e.escrevendo = true;
        }
        escrever(socket, e, payload);
    }

    private static void escrever(Session socket, EscritaEspectador e, byte[] payload) {
        try {
            // Assincrono de proposito: um envio bloqueante para um espectador seguraria a
            // thread que le do agente e, com ela, todos os outros espectadores.
            // A profundidade de recursao e' limitada pela fila (MAX_FILA_ESPECTADOR).
            socket.getAsyncRemote().sendBinary(ByteBuffer.wrap(payload), resultado -> {
                if (!resultado.isOK()) {
                    logger.debug("Falha ao enviar quadro a um espectador", resultado.getException());
                }
                byte[] proximo;
                synchronized (e) {
                    proximo = e.fila.pollFirst();
                    if (proximo == null) {
                        e.escrevendo = false;
                        return;
                    }
                }
                escrever(socket, e, proximo);
            });
        } catch (Exception ex) {
            synchronized (e) {
                e.escrevendo = false;
                e.fila.clear();
            }
            logger.debug("Quadro descartado para um espectador que nao aceita mais dados", ex);
        }
    }

    private static void sendText(Session socket, String text) {
        try {
            socket.getAsyncRemote().sendText(text);
        } catch (Exception e) {
            logger.debug("Nao foi possivel enviar texto pelo socket", e);
        }
    }

    private static void closeQuietly(Session socket, String reason) {
        if (socket == null || !socket.isOpen()) {
            return;
        }
        try {
            socket.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, reason));
        } catch (Exception e) {
            logger.debug("Falha ao fechar um socket de sessao", e);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null || expected.length() != actual.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < expected.length(); i++) {
            diff |= expected.charAt(i) ^ actual.charAt(i);
        }
        return diff == 0;
    }
}
