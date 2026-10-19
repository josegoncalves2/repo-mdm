/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.websocket.CloseReason
 *  javax.websocket.CloseReason$CloseCode
 *  javax.websocket.CloseReason$CloseCodes
 *  javax.websocket.Session
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.hmdm.remote;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RemoteSessionHub {
    private static final Logger logger = LoggerFactory.getLogger(RemoteSessionHub.class);
    private static final long PENDING_TIMEOUT_MS = 120000L;
    private static final long PENDING_OFFLINE_TIMEOUT_MS = 1800000L;
    private static final int MAX_BUFFERED_FRAMES = 30;
    public static final byte FRAME_CONFIG = 1;
    public static final byte FRAME_KEY = 2;
    public static final byte FRAME_DELTA = 3;
    private static final RemoteSessionHub INSTANCE = new RemoteSessionHub();
    private final Map<String, RemoteSession> byDeviceNumber = new ConcurrentHashMap<String, RemoteSession>();
    private final SecureRandom random = new SecureRandom();
    private static final int MAX_FILA_ESPECTADOR = 8;

    private RemoteSessionHub() {
    }

    public static RemoteSessionHub getInstance() {
        return INSTANCE;
    }

    public RemoteSession open(int n, String string, boolean bl) {
        this.reapStale();
        byte[] byArray = new byte[24];
        this.random.nextBytes(byArray);
        String string2 = Base64.getUrlEncoder().withoutPadding().encodeToString(byArray);
        RemoteSession remoteSession = new RemoteSession(n, string, string2);
        remoteSession.pendingOffline = !bl;
        RemoteSession remoteSession2 = this.byDeviceNumber.put(string, remoteSession);
        if (remoteSession2 != null) {
            RemoteSessionHub.closeQuietly(remoteSession2.agent, "substituida por uma nova sessao");
            for (Session session : remoteSession2.viewers) {
                RemoteSessionHub.closeQuietly(session, "substituida por uma nova sessao");
            }
        }
        logger.info("Sessao de suporte remoto aberta para o aparelho '{}' (aparelho {} no pedido)", (Object)string, (Object)(bl ? "online" : "offline"));
        return remoteSession;
    }

    public RemoteSession find(String string) {
        return string == null ? null : this.byDeviceNumber.get(string);
    }

    public void close(String string) {
        RemoteSession remoteSession = this.byDeviceNumber.remove(string);
        if (remoteSession == null) {
            return;
        }
        RemoteSessionHub.closeQuietly(remoteSession.agent, "sessao encerrada");
        for (Session session : remoteSession.viewers) {
            RemoteSessionHub.closeQuietly(session, "sessao encerrada");
        }
        logger.info("Sessao encerrada para '{}' apos {} quadros / {} bytes", new Object[]{string, remoteSession.frames, remoteSession.bytes});
    }

    public RemoteSession attachAgent(String string, String string2, Session session) {
        RemoteSession remoteSession = this.byDeviceNumber.get(string);
        if (remoteSession == null) {
            logger.warn("Fluxo recusado para '{}': nenhuma sessao foi pedida", (Object)string);
            return null;
        }
        if (string2 == null || !RemoteSessionHub.constantTimeEquals(remoteSession.token, string2)) {
            logger.warn("Fluxo recusado para '{}': token nao confere", (Object)string);
            return null;
        }
        RemoteSessionHub.closeQuietly(remoteSession.agent, "substituido por uma conexao mais nova do agente");
        remoteSession.agent = session;
        remoteSession.pendingOffline = false;
        logger.info("Agente conectado para '{}'", (Object)string);
        this.notifyViewers(remoteSession);
        return remoteSession;
    }

    public void detachAgent(String string, Session session) {
        RemoteSession remoteSession = this.byDeviceNumber.get(string);
        if (remoteSession == null || remoteSession.agent != session) {
            return;
        }
        remoteSession.agent = null;
        remoteSession.agentDisconnectedAt = System.currentTimeMillis();
        logger.info("Agente desconectado de '{}'", (Object)string);
        this.notifyViewers(remoteSession);
    }

    public boolean attachViewer(String string, Session session) {
        RemoteSession remoteSession = this.byDeviceNumber.get(string);
        if (remoteSession == null) {
            return false;
        }
        remoteSession.viewers.add(session);
        remoteSession.viewerlessSince = 0L;
        logger.info("Espectador anexado a '{}' (total={}, agente_conectado={})", new Object[]{string, remoteSession.viewers.size(), remoteSession.isStreaming()});
        RemoteSessionHub.sendText(session, this.describe(remoteSession));
        byte[] byArray = remoteSession.codecConfig;
        byte[] byArray2 = remoteSession.lastKeyFrame;
        if (byArray != null) {
            RemoteSessionHub.sendBinary(session, byArray);
        }
        if (byArray2 != null) {
            RemoteSessionHub.sendBinary(session, byArray2);
        }
        return true;
    }

    public void detachViewer(String string, Session session) {
        RemoteSession remoteSession = this.byDeviceNumber.get(string);
        if (remoteSession != null) {
            remoteSession.viewers.remove(session);
            if (remoteSession.viewers.isEmpty()) {
                remoteSession.viewerlessSince = System.currentTimeMillis();
            }
        }
    }

    public void describeStream(String string, int n, int n2, boolean bl) {
        RemoteSession remoteSession = this.byDeviceNumber.get(string);
        if (remoteSession == null) {
            return;
        }
        remoteSession.width = n;
        remoteSession.height = n2;
        remoteSession.inputAvailable = bl;
        logger.info("Fluxo descrito por '{}': {}x{} entrada={} espectadores={}", new Object[]{string, n, n2, bl, remoteSession.viewers.size()});
        RemoteSession.access$902(remoteSession, null);
        RemoteSession.access$1002(remoteSession, null);
        this.notifyViewers(remoteSession);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void relayFrame(String string, byte[] byArray) {
        Object object;
        byte by;
        RemoteSession remoteSession = this.byDeviceNumber.get(string);
        if (remoteSession == null || byArray == null || byArray.length == 0) {
            return;
        }
        remoteSession.frames++;
        remoteSession.bytes += byArray.length;
        if (remoteSession.frames <= 3L || remoteSession.frames % 200L == 0L) {
            logger.info("Quadro {} de '{}': tipo={} bytes={} espectadores={}", new Object[]{remoteSession.frames, string, byArray[0], byArray.length, remoteSession.viewers.size()});
        }
        if ((by = byArray[0]) == 1) {
            RemoteSession.access$902(remoteSession, byArray);
        } else if (by == 2) {
            RemoteSession.access$1002(remoteSession, byArray);
        }
        Iterator iterator = remoteSession.viewers.iterator();
        while (iterator.hasNext()) {
            object = (Session)iterator.next();
            if (!object.isOpen()) {
                iterator.remove();
                continue;
            }
            RemoteSessionHub.sendBinary(object, byArray);
        }
        object = remoteSession.pump;
        synchronized (object) {
            remoteSession.recent.addLast(byArray);
            while (remoteSession.recent.size() > 30) {
                remoteSession.recent.pollFirst();
            }
            remoteSession.sequence++;
            remoteSession.pump.notifyAll();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public PolledFrames poll(String string, long l, long l2) {
        RemoteSession remoteSession = this.byDeviceNumber.get(string);
        if (remoteSession == null) {
            return new PolledFrames(l, Collections.emptyList(), false, 0, 0, false);
        }
        long l3 = System.currentTimeMillis() + Math.max(0L, l2);
        Object object = remoteSession.pump;
        synchronized (object) {
            while (remoteSession.sequence <= l && System.currentTimeMillis() < l3) {
                try {
                    remoteSession.pump.wait(Math.max(1L, l3 - System.currentTimeMillis()));
                }
                catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            ArrayList<byte[]> arrayList = new ArrayList<byte[]>();
            if (remoteSession.sequence > l) {
                long l4 = remoteSession.sequence - l;
                if (l == 0L || l4 > (long)remoteSession.recent.size()) {
                    if (remoteSession.codecConfig != null) {
                        arrayList.add(remoteSession.codecConfig);
                    }
                    if (remoteSession.lastKeyFrame != null) {
                        arrayList.add(remoteSession.lastKeyFrame);
                    }
                } else {
                    Iterator iterator = remoteSession.recent.iterator();
                    long l5 = (long)remoteSession.recent.size() - l4;
                    while (l5-- > 0L && iterator.hasNext()) {
                        iterator.next();
                    }
                    while (iterator.hasNext()) {
                        arrayList.add((byte[])iterator.next());
                    }
                }
            }
            return new PolledFrames(remoteSession.sequence, arrayList, remoteSession.isStreaming(), remoteSession.width, remoteSession.height, remoteSession.inputAvailable);
        }
    }

    public boolean relayCommand(String string, String string2) {
        RemoteSession remoteSession = this.byDeviceNumber.get(string);
        if (remoteSession == null) {
            return false;
        }
        Session session = remoteSession.agent;
        if (session == null || !session.isOpen()) {
            return false;
        }
        RemoteSessionHub.sendText(session, string2);
        return true;
    }

    public void relayToViewers(String string, String string2) {
        RemoteSession remoteSession = this.byDeviceNumber.get(string);
        if (remoteSession == null) {
            return;
        }
        for (Session session : remoteSession.viewers) {
            RemoteSessionHub.sendText(session, string2);
        }
    }

    public String describe(RemoteSession remoteSession) {
        return "{\"type\":\"status\",\"streaming\":" + remoteSession.isStreaming() + ",\"pending\":" + !remoteSession.isStreaming() + ",\"requestedAt\":" + remoteSession.createdAt + ",\"width\":" + remoteSession.width + ",\"height\":" + remoteSession.height + ",\"input\":" + remoteSession.inputAvailable + ",\"viewers\":" + remoteSession.viewers.size() + ",\"frames\":" + remoteSession.frames + ",\"bytes\":" + remoteSession.bytes + "}";
    }

    private void notifyViewers(RemoteSession remoteSession) {
        String string = this.describe(remoteSession);
        for (Session session : remoteSession.viewers) {
            RemoteSessionHub.sendText(session, string);
        }
    }

    private void reapStale() {
        long l = System.currentTimeMillis();
        this.byDeviceNumber.entrySet().removeIf(entry -> {
            boolean bl;
            RemoteSession remoteSession = (RemoteSession)entry.getValue();
            if (remoteSession.isStreaming()) {
                return false;
            }
            long l2 = remoteSession.pendingOffline ? 1800000L : 120000L;
            boolean bl2 = bl = l - (remoteSession.agentDisconnectedAt > 0L ? remoteSession.agentDisconnectedAt : remoteSession.createdAt) > l2;
            if (bl) {
                logger.info("Descartando pedido pendente de '{}' apos {} ms (prazo {} ms, offline_no_pedido={})", new Object[]{entry.getKey(), l - remoteSession.createdAt, l2, remoteSession.pendingOffline});
            }
            return bl;
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static EscritaEspectador escritaDe(Session session) {
        Session session2 = session;
        synchronized (session2) {
            Object object = session.getUserProperties().get("hwmdm.escrita");
            if (object == null) {
                object = new EscritaEspectador();
                session.getUserProperties().put("hwmdm.escrita", object);
            }
            return (EscritaEspectador)object;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void sendBinary(Session session, byte[] byArray) {
        EscritaEspectador escritaEspectador;
        EscritaEspectador escritaEspectador2 = escritaEspectador = RemoteSessionHub.escritaDe(session);
        synchronized (escritaEspectador2) {
            if (escritaEspectador.escrevendo) {
                if (escritaEspectador.fila.size() >= 8) {
                    escritaEspectador.fila.pollFirst();
                    if (++escritaEspectador.descartados % 100L == 1L) {
                        logger.info("Espectador lento: {} quadros descartados", (Object)escritaEspectador.descartados);
                    }
                }
                escritaEspectador.fila.addLast(byArray);
                return;
            }
            escritaEspectador.escrevendo = true;
        }
        RemoteSessionHub.escrever(session, escritaEspectador, byArray);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void escrever(Session session, EscritaEspectador escritaEspectador, byte[] byArray) {
        try {
            session.getAsyncRemote().sendBinary(ByteBuffer.wrap(byArray), sendResult -> {
                byte[] byArray;
                if (!sendResult.isOK()) {
                    logger.debug("Falha ao enviar quadro a um espectador", sendResult.getException());
                }
                EscritaEspectador escritaEspectador2 = escritaEspectador;
                synchronized (escritaEspectador2) {
                    byArray = (byte[])escritaEspectador.fila.pollFirst();
                    if (byArray == null) {
                        escritaEspectador.escrevendo = false;
                        return;
                    }
                }
                RemoteSessionHub.escrever(session, escritaEspectador, byArray);
            });
        }
        catch (Exception exception) {
            EscritaEspectador escritaEspectador2 = escritaEspectador;
            synchronized (escritaEspectador2) {
                escritaEspectador.escrevendo = false;
                escritaEspectador.fila.clear();
            }
            logger.debug("Quadro descartado para um espectador que nao aceita mais dados", (Throwable)exception);
        }
    }

    private static void sendText(Session session, String string) {
        try {
            session.getAsyncRemote().sendText(string);
        }
        catch (Exception exception) {
            logger.debug("Nao foi possivel enviar texto pelo socket", (Throwable)exception);
        }
    }

    private static void closeQuietly(Session session, String string) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.close(new CloseReason((CloseReason.CloseCode)CloseReason.CloseCodes.NORMAL_CLOSURE, string));
        }
        catch (Exception exception) {
            logger.debug("Falha ao fechar um socket de sessao", (Throwable)exception);
        }
    }

    private static boolean constantTimeEquals(String string, String string2) {
        if (string == null || string2 == null || string.length() != string2.length()) {
            return false;
        }
        int n = 0;
        for (int i = 0; i < string.length(); ++i) {
            n |= string.charAt(i) ^ string2.charAt(i);
        }
        return n == 0;
    }

    static {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "remote-support-reaper");
            thread.setDaemon(true);
            return thread;
        });
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                INSTANCE.reapStale();
            }
            catch (Exception exception) {
                logger.warn("Falha na varredura periodica de sessoes de suporte remoto", (Throwable)exception);
            }
        }, 30L, 30L, TimeUnit.SECONDS);
    }

    private static final class EscritaEspectador {
        private final ArrayDeque<byte[]> fila = new ArrayDeque();
        private boolean escrevendo;
        private long descartados;

        private EscritaEspectador() {
        }
    }

    public static final class PolledFrames {
        public final long cursor;
        public final List<byte[]> frames;
        public final boolean streaming;
        public final int width;
        public final int height;
        public final boolean input;

        PolledFrames(long l, List<byte[]> list, boolean bl, int n, int n2, boolean bl2) {
            this.cursor = l;
            this.frames = list;
            this.streaming = bl;
            this.width = n;
            this.height = n2;
            this.input = bl2;
        }
    }

    public static final class RemoteSession {
        private final String deviceNumber;
        private final int deviceId;
        private final String token;
        private final long createdAt = System.currentTimeMillis();
        private volatile Session agent;
        private final Set<Session> viewers = Collections.newSetFromMap(new ConcurrentHashMap());
        private volatile int width;
        private volatile int height;
        private volatile boolean inputAvailable;
        private volatile byte[] codecConfig;
        private volatile byte[] lastKeyFrame;
        private volatile long frames;
        private volatile long bytes;
        private volatile boolean pendingOffline;
        private volatile long viewerlessSince;
        private volatile long agentDisconnectedAt;
        private final ArrayDeque<byte[]> recent = new ArrayDeque();
        private volatile long sequence;
        private final Object pump = new Object();

        private RemoteSession(int n, String string, String string2) {
            this.deviceId = n;
            this.deviceNumber = string;
            this.token = string2;
            this.viewerlessSince = this.createdAt;
        }

        public String getDeviceNumber() {
            return this.deviceNumber;
        }

        public int getDeviceId() {
            return this.deviceId;
        }

        public String getToken() {
            return this.token;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public boolean isInputAvailable() {
            return this.inputAvailable;
        }

        public long getFrames() {
            return this.frames;
        }

        public long getBytes() {
            return this.bytes;
        }

        public int getViewerCount() {
            return this.viewers.size();
        }

        public boolean isStreaming() {
            return this.agent != null && this.agent.isOpen();
        }

        public long getCreatedAt() {
            return this.createdAt;
        }

        public boolean isPendingOffline() {
            return this.pendingOffline;
        }

        static /* synthetic */ byte[] access$902(RemoteSession remoteSession, byte[] byArray) {
            remoteSession.codecConfig = byArray;
            return byArray;
        }

        static /* synthetic */ byte[] access$1002(RemoteSession remoteSession, byte[] byArray) {
            remoteSession.lastKeyFrame = byArray;
            return byArray;
        }
    }
}
