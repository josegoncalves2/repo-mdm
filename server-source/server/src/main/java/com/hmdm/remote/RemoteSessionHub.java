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
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    // Viewer navigation does not revoke an administrator's active support session.
    // Explicit stop, agent disconnect and pending-request expiry own that lifecycle.

    /**
     * Carencia para encerrar uma sessao cujo ultimo espectador fechou o navegador
     * (F5, troca de aba, fechou a janela). O agente continua transmitindo por este
     * tempo para dar chance ao operador de reatar (ver tryReattach no controller).
     * Passado este prazo sem ninguem, a sessao e' encerrada para poupar bateria e
     * processamento no tablet.
     */
    private static final long VIEWER_GRACE_MS = 30_000L;

    /** Quadros guardados para o transporte HTTP: ~2s de video a 15 fps. */
    private static final int MAX_BUFFERED_FRAMES = 30;

    public static final byte FRAME_CONFIG = 1;
    public static final byte FRAME_KEY = 2;
    public static final byte FRAME_DELTA = 3;

    private static final RemoteSessionHub INSTANCE = new RemoteSessionHub();

    /** Reaper periodico. Precisa ser desligado no undeploy (ver {@link #shutdown()}). */
    private static final ScheduledExecutorService REAPER;

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
        REAPER = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "remote-support-reaper");
            thread.setDaemon(true);
            return thread;
        });
        REAPER.scheduleWithFixedDelay(() -> {
            try {
                INSTANCE.reapStale();
            } catch (Exception e) {
                logger.warn("Falha na varredura periodica de sessoes de suporte remoto", e);
            }
        }, 30, 30, TimeUnit.SECONDS);

        /*
         * O reaper segura a referencia para INSTANCE (e portanto para o classloader
         * da aplicacao). Sem o shutdown explicito, um undeploy no Tomcat nao consegue
         * liberar o classloader e a thread continua viva apos a aplicacao sair --
         * vazamento classico. O undeploy precisa chamar {@link #shutdown()} (ver
         * ServletContextListener). NAO registre shutdown hook aqui: o static initializer
         * roda durante o startup do Tomcat, e se a JVM estiver parando (redeploy em
         * andamento), addShutdownHook lanca IllegalStateException e derruba o contexto.
         */
    }

    private final Map<String, RemoteSession> byDeviceNumber = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private RemoteSessionHub() {
    }

    public static RemoteSessionHub getInstance() {
        return INSTANCE;
    }

    /**
     * <p>Encerra o thread da varredura periodica. Deve ser chamado no
     * {@code contextDestroyed} de um {@code ServletContextListener} para permitir que o
     * container faca o undeploy da aplicacao sem vazar o classloader.</p>
     */
    public static void shutdown() {
        REAPER.shutdownNow();
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
        private volatile long agentDisconnectedAt;

        /*
         * Buffer circular dos quadros recentes, para o transporte HTTP.
         *
         * O WebSocket empurra; o long polling precisa PUXAR, e para isso o quadro tem de
         * continuar existindo por alguns instantes depois de gerado. Guardamos poucos e por
         * pouco tempo de proposito: o objetivo e' cobrir o intervalo entre duas requisicoes
         * do navegador, nao manter historico. Video ao vivo antigo nao tem valor.
         */
        private final ArrayDeque<byte[]> recent = new ArrayDeque<>();
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

        /** Numero de espectadores com socket aberto. Ignora sockets ja' fechados. */
        public int getViewerCount() {
            int n = 0;
            for (Session v : viewers) {
                if (v.isOpen()) n++;
            }
            return n;
        }

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
        // Troca a referencia ANTES de fechar o socket antigo: se o onClose do socket
        // antigo disparar e chamar detachAgent, ele ve session.agent == socket (novo)
        // e nao zera nada. Fechar antes abriria uma janela em que o detach do antigo
        // zeraria o campo depois que a nova referencia ja' tivesse sido atribuida.
        Session antigo = session.agent;
        session.agent = socket;
        closeQuietly(antigo, "substituido por uma conexao mais nova do agente");

        // O agente conectou, entao o aparelho provou que esta' alcancavel agora -- o prazo
        // longo de "pedido feito com aparelho offline" deixa de fazer sentido a partir daqui.
        session.pendingOffline = false;

        // Reinicia a carencia de "ninguem olhando" a partir do momento em que o agente
        // conecta. Se a sessao nasceu sem espectador, viewerlessSince estava contando
        // desde createdAt (esperando o agente); agora a espera passa a ser por espectador,
        // e o relogio deve comecar de novo.
        if (session.getViewerCount() == 0) {
            session.viewerlessSince = System.currentTimeMillis();
        }

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
        session.agentDisconnectedAt = System.currentTimeMillis();
        logger.info("Agente desconectado de '{}'", deviceNumber);
        notifyViewers(session);
    }

    public boolean attachViewer(String deviceNumber, Session socket) {
        if (socket == null) {
            return false;
        }
        RemoteSession session = byDeviceNumber.get(deviceNumber);
        if (session == null) {
            return false;
        }
        session.viewers.add(socket);
        // Ha' espectador de novo: o relogio da carencia de "ninguem esta' olhando" para.
        session.viewerlessSince = 0;
        logger.info("Espectador anexado a '{}' (total={}, agente_conectado={})",
                deviceNumber, session.getViewerCount(), session.isStreaming());
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
        if (socket == null) {
            return;
        }
        RemoteSession session = byDeviceNumber.get(deviceNumber);
        if (session == null) {
            return;
        }
        session.viewers.remove(socket);
        if (session.getViewerCount() == 0) {
            // Marca o inicio da carencia (VIEWER_GRACE_MS). Isto e' o que sobrevive a um
            // F5 ou a uma troca de aba: o navegador fecha este socket de proposito ao
            // desmontar a tela, mas a sessao no aparelho continua ligada ate' o prazo
            // vencer ou o painel reatar (ver reapStale()).
            session.viewerlessSince = System.currentTimeMillis();
        } else {
            // Ainda ha' espectadores: atualiza a contagem no painel deles.
            notifyViewers(session);
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
                deviceNumber, width, height, inputAvailable, session.getViewerCount());
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
                    session.frames, deviceNumber, frame[0], frame.length, session.getViewerCount());
        }

        final byte type = frame[0];
        if (type == FRAME_CONFIG) {
            session.codecConfig = frame;
        } else if (type == FRAME_KEY) {
            session.lastKeyFrame = frame;
        }

        boolean removeu = false;
        Iterator<Session> iterator = session.viewers.iterator();
        while (iterator.hasNext()) {
            Session viewer = iterator.next();
            if (!viewer.isOpen()) {
                iterator.remove();
                removeu = true;
                continue;
            }
            sendBinary(viewer, frame);
        }
        // Se a poda tirou o ultimo espectador sem passar por detachViewer, comeca a
        // carencia agora -- caso contrario viewerlessSince ficaria em 0 e reapStale()
        // nunca encerraria a sessao.
        if (removeu && session.getViewerCount() == 0 && session.viewerlessSince == 0) {
            session.viewerlessSince = System.currentTimeMillis();
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
            return new PolledFrames(since, Collections.emptyList(), false, 0, 0, false);
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

            List<byte[]> out = new ArrayList<>();
            if (session.sequence > since) {
                // Quando o cliente ficou para tras alem do buffer, recomecamos do que existe:
                // reenviar a configuracao e o ultimo quadro-chave e' o que permite decodificar.
                long behind = session.sequence - since;
                if (since == 0 || behind > session.recent.size()) {
                    if (session.codecConfig != null) {
                        out.add(session.codecConfig);
                    }
                    if (session.lastKeyFrame != null) {
                        out.add(session.lastKeyFrame);
                    }
                } else {
                    Iterator<byte[]> it = session.recent.iterator();
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
        public final List<byte[]> frames;
        public final boolean streaming;
        public final int width;
        public final int height;
        public final boolean input;

        PolledFrames(long cursor, List<byte[]> frames, boolean streaming,
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
     * <p>Comandos NAO sao descartados quando a fila enche: perder um "down" sem o "up"
     * correspondente deixa o aparelho com o toque preso, e perder um "up" deixa o
     * gesto incompleto. Se o agente nao estiver drenando, o socket e' fechado -- o
     * operador ve a sessao cair e reata, em vez de achar que o aparelho travou.</p>
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
        return enfileirarComandoAgente(agent, json);
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
                + ",\"viewers\":" + session.getViewerCount()
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
     * 4 -- ver politica de encerramento explicito). Chamada apenas pelo thread periodico
     * registrado no bloco {@code static} da classe.</p>
     */
    private void reapStale() {
        long now = System.currentTimeMillis();
        byDeviceNumber.entrySet().removeIf(entry -> {
            RemoteSession session = entry.getValue();

            if (session.isStreaming()) {
                // Remove espectadores cujo socket ja fechou mas nao passaram por
                // detachViewer(). Sem esta limpeza, getViewerCount() nunca chega a
                // zero e a sessao vaza recursos no tablet.
                session.viewers.removeIf(v -> !v.isOpen());

                // Se a poda removeu o ultimo espectador mas viewerlessSince ainda esta
                // em 0 (porque nunca passou por detachViewer), inicia a carencia agora.
                if (session.getViewerCount() == 0 && session.viewerlessSince == 0) {
                    session.viewerlessSince = now;
                }

                // Sessao com agente conectado: se nao ha espectadores ha mais tempo
                // que VIEWER_GRACE_MS, encerra para poupar bateria e processamento.
                if (session.getViewerCount() == 0
                        && session.viewerlessSince > 0
                        && now - session.viewerlessSince > VIEWER_GRACE_MS) {
                    logger.info("Encerrando sessao de '{}' sem espectadores apos {} ms (carencia {} ms)",
                            entry.getKey(), now - session.viewerlessSince, VIEWER_GRACE_MS);
                    closeQuietly(session.agent, "sessao sem espectadores");
                    return true;
                }
                return false;
            }

            // Sem agente conectado: o pedido ainda esta' esperando o aparelho atender. Qual
            // prazo vale depende de o aparelho ja' estar sabido offline no instante do pedido
            // (PENDING_OFFLINE_TIMEOUT_MS, bem mais longo) ou de ele estar supostamente
            // alcancavel e simplesmente nao ter respondido ainda (PENDING_TIMEOUT_MS).
            long timeout = session.pendingOffline ? PENDING_OFFLINE_TIMEOUT_MS : PENDING_TIMEOUT_MS;
            boolean stale = now - (session.agentDisconnectedAt > 0
                    ? session.agentDisconnectedAt : session.createdAt) > timeout;
            if (stale) {
                logger.info("Descartando pedido pendente de '{}' apos {} ms (prazo {} ms, offline_no_pedido={})",
                        entry.getKey(), now - session.createdAt, timeout, session.pendingOffline);
                for (Session viewer : session.viewers) {
                    closeQuietly(viewer, "sessao expirada");
                }
            }
            return stale;
        });
    }

    /**
     * <p>O {@code AsyncRemote} do Tomcat aceita UM envio em voo por socket, seja texto ou
     * binario. Chamado a cada quadro sem esperar o anterior, ele lanca
     * {@code IllegalStateException [BINARY_FULL_WRITING ou TEXT_FULL_WRITING]} e a mensagem
     * se perdia em silencio assim que o navegador drenava mais devagar que o aparelho
     * transmitia. Aqui o excedente espera numa fila curta e o quadro velho e' descartado no
     * lugar do novo, que e' o que o espectador precisa ver.</p>
     *
     * <p>Texto e binario compartilham a MESMA fila porque o AsyncRemote aceita uma unica
     * operacao por vez, independente do tipo. Sem isto, um {@code notifyViewers()} enviando
     * um JSON de status enquanto um frame esta' em voo lancava
     * {@code TEXT_FULL_WRITING} e derrubava o espectador.</p>
     *
     * <p>O agente usa uma fila SEPARADA e sem descarte -- ver {@link #enfileirarComandoAgente}.
     * Misturar as duas politicas na mesma fila faria um comando de toque ser descartado no
     * lugar de um quadro de video, deixando o aparelho com o gesto preso.</p>
     */
    private static final int MAX_FILA_ESPECTADOR = 8;

    /** Fila do agente: comporta rajadas curtas de comandos sem descartar nenhum. */
    private static final int MAX_FILA_AGENTE = 256;

    private static final class EscritaSocket {
        private final ArrayDeque<Object> fila = new ArrayDeque<>();
        private boolean escrevendo;
        private long descartados;
    }

    private static EscritaSocket escritaDe(Session socket) {
        synchronized (socket) {
            Object atual = socket.getUserProperties().get("hwmdm.escrita");
            if (atual == null) {
                atual = new EscritaSocket();
                socket.getUserProperties().put("hwmdm.escrita", atual);
            }
            return (EscritaSocket) atual;
        }
    }

    private static boolean enfileirar(Session socket, Object payload, int limite, boolean descartaAntigo) {
        EscritaSocket e = escritaDe(socket);
        synchronized (e) {
            if (e.escrevendo) {
                if (e.fila.size() >= limite) {
                    if (descartaAntigo) {
                        e.fila.pollFirst();
                        if (++e.descartados % 100 == 1) {
                            logger.info("Espectador lento: {} mensagens descartadas", e.descartados);
                        }
                    } else {
                        // Nao mexe em e.escrevendo nem limpa a fila: ha um envio em voo.
                        // Fecha o socket para que o operador veja a sessao cair, em vez de
                        // seguir mandando comando para uma fila que nunca drena.
                        logger.warn("Fila do agente cheia ({}), encerrando socket", limite);
                        closeQuietly(socket, "fila de comandos cheia");
                        return false;
                    }
                }
                e.fila.addLast(payload);
                return true;
            }
            e.escrevendo = true;
        }
        enviar(socket, e, payload);
        return true;
    }

    private static void sendBinary(Session socket, byte[] payload) {
        if (socket == null || payload == null) {
            return;
        }
        enfileirar(socket, payload, MAX_FILA_ESPECTADOR, true);
    }

    private static void sendText(Session socket, String text) {
        if (socket == null || text == null) {
            return;
        }
        enfileirar(socket, text, MAX_FILA_ESPECTADOR, true);
    }

    private static boolean enfileirarComandoAgente(Session socket, String json) {
        if (socket == null || json == null) {
            return false;
        }
        return enfileirar(socket, json, MAX_FILA_AGENTE, false);
    }

    /**
     * <p>Drena a fila de um socket, um item em voo por vez. O callback reentra neste metodo
     * quando o envio anterior completa -- em Tomcat, o callback do {@code AsyncRemote} pode
     * ser chamado de forma sincrona (quando o envio falha na hora, por exemplo), entao a
     * profundidade de recursao e' limitada pelo tamanho maximo da fila (MAX_FILA_AGENTE = 256
     * para o agente, MAX_FILA_ESPECTADOR = 8 para espectadores).</p>
     */
    private static void enviar(Session socket, EscritaSocket e, Object payload) {
        try {
            SendHandler callback = (SendResult resultado) -> {
                if (!resultado.isOK()) {
                    logger.debug("Falha no envio assincrono", resultado.getException());
                    synchronized (e) {
                        e.escrevendo = false;
                        e.fila.clear();
                    }
                    return;
                }
                Object proximo;
                synchronized (e) {
                    proximo = e.fila.pollFirst();
                    if (proximo == null) {
                        e.escrevendo = false;
                        return;
                    }
                }
                enviar(socket, e, proximo);
            };

            if (payload instanceof byte[]) {
                byte[] bin = (byte[]) payload;
                socket.getAsyncRemote().sendBinary(ByteBuffer.wrap(bin), callback);
            } else if (payload instanceof String) {
                String txt = (String) payload;
                socket.getAsyncRemote().sendText(txt, callback);
            } else {
                // Tipo desconhecido: nao deixa a fila presa.
                synchronized (e) {
                    e.escrevendo = false;
                    e.fila.clear();
                }
                logger.warn("Payload de tipo inesperado na fila de escrita: {}",
                        payload == null ? "null" : payload.getClass().getName());
            }
        } catch (Exception ex) {
            synchronized (e) {
                e.escrevendo = false;
                e.fila.clear();
            }
            logger.debug("Mensagem descartada para um socket que nao aceita mais dados", ex);
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
