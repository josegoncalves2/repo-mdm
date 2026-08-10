package com.hwmdm.remote.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import com.hwmdm.remote.R;
import com.hwmdm.remote.mdm.RemoteLog;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * <p>Transmite a tela do aparelho ao painel enquanto durar a sessao de suporte, e recebe de
 * volta o toque e a digitacao do tecnico.</p>
 *
 * <p>A tela e' codificada em H.264 pelo encoder de hardware e enviada quadro a quadro por
 * WebSocket. E' a mesma forma de qualquer software de compartilhamento de tela: um fluxo de
 * video continuo. Nao ha imagem parada em disco em ponto nenhum -- os quadros existem
 * enquanto alguem esta olhando e sao esquecidos depois.</p>
 *
 * <p>O socket e' <b>bidirecional</b>: sobem quadros, descem comandos de entrada. Usar a
 * mesma conexao para os dois e' o que mantem o clique alinhado com a imagem que o tecnico
 * esta vendo; um canal separado poderia entregar um toque referente a uma tela que ja
 * mudou.</p>
 *
 * <p><b>Consentimento.</b> O Android nunca deixa ler a tela sem um token de MediaProjection,
 * e privilegio de device owner nao dispensa isso. Ate o Android 13 o token e' reaproveitado
 * ate o proximo reinicio; a partir do Android 14 a plataforma o torna de uso unico, entao
 * cada sessao pede confirmacao no aparelho. Nao ha contorno sem assinatura de sistema.</p>
 */
public class ScreenStreamService extends Service {

    private static final String TAG = "hwmdm-remote";

    public static final String ACTION_START = "com.hwmdm.remote.STREAM_START";
    public static final String ACTION_STOP = "com.hwmdm.remote.STREAM_STOP";

    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TOKEN = "token";
    public static final String EXTRA_FORMAT = "format";
    public static final String EXTRA_FPS = "fps";
    public static final String EXTRA_BITRATE = "bitrate";
    public static final String EXTRA_MAX_WIDTH = "maxWidth";

    private static final String CHANNEL_ID = "hwmdm_remote_sharing";
    private static final int NOTIFICATION_ID = 0xA2;

    /** Marcadores de quadro. O servidor repassa sem tocar; o navegador e' quem interpreta. */
    private static final byte FRAME_CONFIG = 1;
    private static final byte FRAME_KEY = 2;
    private static final byte FRAME_DELTA = 3;

    private static final int KEYFRAME_INTERVAL_SEC = 2;

    /**
     * Acima disto o socket esta atrasado e os quadros novos sao descartados em vez de
     * enfileirados. Video ao vivo atrasado nao tem valor: guardar quadros velhos so'
     * transformaria um engasgo passageiro em atraso permanente.
     */
    private static final long MAX_SOCKET_BACKLOG_BYTES = 1_500_000L;

    private static int consentResultCode = 0;
    private static Intent consentData = null;
    private static volatile boolean streaming = false;

    /** Segura a tela acesa durante a sessao; ver {@link #acquireScreenLock()}. */
    private PowerManager.WakeLock screenLock;

    /** Teto de seguranca: uma sessao interrompida nao pode manter a tela acesa para sempre. */
    private static final long SCREEN_LOCK_TIMEOUT_MS = 2 * 60 * 60 * 1000L;

    public static void cacheConsent(int resultCode, Intent data) {
        consentResultCode = resultCode;
        consentData = data;
    }

    /**
     * A partir do Android 14 o token de consentimento so' pode ser resgatado uma vez, entao
     * um token guardado nao vale nada e fingir que vale produziria uma sessao que morre no
     * {@code getMediaProjection}.
     */
    public static boolean hasConsent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return false;
        }
        return consentData != null;
    }

    public static boolean isStreaming() {
        return streaming;
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, ScreenStreamService.class);
        intent.setAction(ACTION_STOP);
        try {
            context.startService(intent);
        } catch (Throwable ignored) {
            // Servico ja parado.
        }
    }

    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private MediaCodec encoder;
    private Surface encoderSurface;
    private WebSocket socket;
    private HandlerThread worker;
    private Handler handler;

    private String url;
    private String token;
    private int fps = 15;
    private int bitrate = 2_000_000;
    private int maxWidth = 800;
    private int width;
    private int height;
    private int realWidth;
    private int realHeight;
    private volatile boolean stopping;

    /*
     * Numero da sessao corrente.
     *
     * A captura sobe por callback assincrono (socket abre -> startCapture). Quando um
     * chamado novo chega, ele derruba a sessao anterior; sem este contador o callback da
     * sessao velha continuava correndo e mexia nos campos da nova -- foi assim que
     * createVirtualDisplay recebeu uma MediaProjection ja anulada e estourou
     * NullPointerException, que no log aparecia como "nenhum encoder utilizavel".
     *
     * Cada etapa assincrona confere a propria geracao antes de agir.
     */
    private volatile int generation;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            shutdown("encerrada pelo painel");
            return START_NOT_STICKY;
        }

        startForegroundCompat();

        // Sessao anterior sai ANTES de a nova entrar.
        //
        // Cada clique em "Conectar" no painel manda um chamado novo, e o servico e' um so'.
        // Sem soltar o que ja estava aberto, o MediaCodec e a MediaProjection antigos
        // continuavam presos: os campos eram sobrescritos e nada os liberava. Como o numero
        // de encoders de hardware e' finito, bastavam duas ou tres tentativas para o
        // proximo configure() falhar com "nenhum encoder utilizavel" -- que foi exatamente
        // o que apareceu no log do aparelho, alternando com sessoes que funcionavam.
        if (streaming || encoder != null || projection != null || socket != null) {
            RemoteLog.i(this, "Encerrando a sessao anterior antes de abrir a nova");
            cleanup();
        }
        stopping = false;
        streaming = false;
        final int session = ++generation;

        if (intent == null) {
            // Reinicio pelo sistema sem extras nao consegue retomar sessao: o token e' de
            // uso unico e o painel nao esta mais esperando.
            stopSelf();
            return START_NOT_STICKY;
        }

        url = intent.getStringExtra(EXTRA_URL);
        token = intent.getStringExtra(EXTRA_TOKEN);
        fps = clamp(intent.getIntExtra(EXTRA_FPS, 15), 1, 30);
        bitrate = clamp(intent.getIntExtra(EXTRA_BITRATE, 2_000_000), 200_000, 8_000_000);
        maxWidth = clamp(intent.getIntExtra(EXTRA_MAX_WIDTH, 800), 240, 1920);

        if (url == null || url.trim().isEmpty() || token == null || token.trim().isEmpty()) {
            RemoteLog.w(this, "Sessao recusada: sem endereco de relay ou sem token");
            stopSelf();
            return START_NOT_STICKY;
        }
        if (consentData == null) {
            RemoteLog.w(this, "Sessao recusada: sem consentimento de captura de tela");
            stopSelf();
            return START_NOT_STICKY;
        }

        worker = new HandlerThread("hwmdm-remote-stream");
        worker.start();
        handler = new Handler(worker.getLooper());
        connect(session);
        return START_NOT_STICKY;
    }

    // =================================================================================================================

    /**
     * Abre o uplink primeiro e so' comeca a capturar quando ele e' aceito. Uma sessao
     * recusada nunca chega a ligar o encoder nem a acender o indicador de captura.
     */
    private void connect(final int session) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .build();

        String target = url + (url.contains("?") ? "&" : "?") + "token=" + android.net.Uri.encode(token);
        Request request = new Request.Builder().url(target).build();

        RemoteLog.i(this, "Discando o relay: " + target);
        socket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                if (session != generation) {
                    // Sessao ja substituida enquanto o socket abria.
                    webSocket.close(1000, "sessao substituida");
                    return;
                }
                RemoteLog.i(ScreenStreamService.this, "Relay aceitou a conexao; iniciando captura");
                handler.post(() -> startCapture(session));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handler.post(() -> onCommand(text));
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.i(TAG, "Sessao encerrada pelo servidor: " + reason);
                shutdown(null);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                RemoteLog.e(ScreenStreamService.this, "Uplink falhou: " + t);
                shutdown(null);
            }
        });
    }

    /**
     * <p>Aplica um comando de entrada vindo do painel.</p>
     *
     * <p>As coordenadas chegam normalizadas entre 0 e 1, nao em pixels. O painel exibe a
     * tela redimensionada e a resolucao transmitida nao e' a real; mandar pixels obrigaria
     * as duas pontas a concordarem sobre a escala, e qualquer divergencia -- rotacao,
     * mudanca de resolucao no meio da sessao -- faria o toque cair no lugar errado.</p>
     */
    private void onCommand(String message) {
        InputInjectionService input = InputInjectionService.get();
        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type", "");

            if ("stop".equals(type)) {
                shutdown("encerrada pelo painel");
                return;
            }

            if (input == null) {
                // A sessao continua valendo: a tela e' transmitida, so' nao ha toque.
                Log.w(TAG, "Comando '" + type + "' ignorado: acessibilidade nao habilitada no aparelho");
                reportInput(type, false, "accessibility_disabled");
                return;
            }

            boolean ok;
            switch (type) {
                case "tap":
                    ok = input.tap(px(json.optDouble("x", -1)), py(json.optDouble("y", -1)));
                    break;
                case "longpress":
                    ok = input.longPress(px(json.optDouble("x", -1)), py(json.optDouble("y", -1)));
                    break;
                case "swipe":
                    ok = input.swipe(px(json.optDouble("x1", -1)), py(json.optDouble("y1", -1)),
                            px(json.optDouble("x2", -1)), py(json.optDouble("y2", -1)),
                            json.optLong("duration", 250L));
                    break;
                case "text":
                    ok = input.type(json.optString("text", ""));
                    break;
                case "key":
                    ok = input.key(json.optString("name", ""));
                    break;
                default:
                    Log.w(TAG, "Comando desconhecido: " + type);
                    return;
            }
            reportInput(type, ok, ok ? null : "rejected");
        } catch (Throwable t) {
            Log.w(TAG, "Comando ilegivel", t);
        }
    }

    /** Normalizado (0..1) -> pixel real da tela. */
    private float px(double normalized) {
        return (float) (Math.max(0.0, Math.min(1.0, normalized)) * realWidth);
    }

    private float py(double normalized) {
        return (float) (Math.max(0.0, Math.min(1.0, normalized)) * realHeight);
    }

    /**
     * Devolve ao painel se o comando pegou. Sem isso o tecnico clicaria numa imagem que nao
     * responde sem nenhuma pista do motivo -- que e' exatamente o que ja aconteceu antes
     * neste projeto.
     */
    private void reportInput(String type, boolean applied, String reason) {
        WebSocket ws = socket;
        if (ws == null) {
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("type", "input-result");
            json.put("command", type);
            json.put("applied", applied);
            if (reason != null) {
                json.put("reason", reason);
            }
            ws.send(json.toString());
        } catch (Throwable ignored) {
        }
    }

    // =================================================================================================================

    private void startCapture(final int session) {
        if (session != generation) {
            return;
        }
        try {
            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (manager == null) {
                fail("aparelho sem gerenciador de projecao");
                return;
            }

            projection = manager.getMediaProjection(consentResultCode, consentData);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                consentData = null;
            }
            if (projection == null) {
                consentData = null;
                fail("consentimento de captura nao vale mais");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Obrigatorio no Android 14, e a unica forma de saber que o usuario
                // interrompeu a captura pela barra de status.
                projection.registerCallback(new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        Log.i(TAG, "Captura interrompida no aparelho");
                        shutdown("captura interrompida no aparelho");
                    }
                }, handler);
            }

            measure();
            if (!startEncoder(projection, session)) {
                fail("nenhum encoder H.264 utilizavel");
                return;
            }

            streaming = true;
            acquireScreenLock();
            announce();
            RemoteLog.i(this, "Transmitindo " + width + "x" + height
                    + (InputInjectionService.isAvailable() ? " com toque" : " somente visualizacao"));
        } catch (Throwable t) {
            fail("nao foi possivel iniciar: " + t.getMessage());
        }
    }

    /**
     * Resolucao transmitida: a tela real reduzida ate a largura pedida pelo painel, com os
     * dois lados arredondados para multiplo de 16.
     *
     * <p>Reduzir e' o que torna a sessao usavel em link lento -- a resolucao cheia de um
     * tablet custa varias vezes a banda por um detalhe que ninguem le num atendimento. O
     * multiplo de 16 e' exigencia pratica dos encoders de hardware: medidas quebradas saem
     * cisalhadas ou sao recusadas.</p>
     */
    private void measure() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metrics);

        realWidth = metrics.widthPixels;
        realHeight = metrics.heightPixels;

        double scale = Math.min(1.0, (double) maxWidth / (double) Math.max(realWidth, realHeight));
        width = round16((int) Math.round(realWidth * scale));
        height = round16((int) Math.round(realHeight * scale));
    }

    /**
     * Recebe a projecao por parametro, e nao pelo campo. O campo pode ser anulado por um
     * chamado concorrente entre a checagem e o uso; uma referencia local nao pode.
     */
    private boolean startEncoder(final MediaProjection activeProjection, final int session) {
        try {
            MediaFormat format = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, KEYFRAME_INTERVAL_SEC);

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            encoder.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(MediaCodec codec, int index) {
                    // A entrada chega pela Surface, nunca por buffer.
                }

                @Override
                public void onOutputBufferAvailable(MediaCodec codec, int index,
                                                    MediaCodec.BufferInfo info) {
                    try {
                        ByteBuffer buffer = codec.getOutputBuffer(index);
                        if (buffer != null && info.size > 0) {
                            buffer.position(info.offset);
                            buffer.limit(info.offset + info.size);
                            byte[] payload = new byte[info.size];
                            buffer.get(payload);
                            sendFrame(frameType(info), info.presentationTimeUs, payload);
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, "Quadro descartado: " + t.getMessage());
                    } finally {
                        try {
                            codec.releaseOutputBuffer(index, false);
                        } catch (Throwable ignored) {
                        }
                    }
                }

                @Override
                public void onError(MediaCodec codec, MediaCodec.CodecException e) {
                    Log.w(TAG, "Encoder falhou: " + e.getMessage());
                    shutdown("encoder de video falhou no aparelho");
                }

                @Override
                public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                }
            }, handler);

            if (session != generation) {
                releaseEncoder();
                return false;
            }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoderSurface = encoder.createInputSurface();
            encoder.start();

            DisplayMetrics metrics = new DisplayMetrics();
            ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay().getRealMetrics(metrics);
            virtualDisplay = activeProjection.createVirtualDisplay("hwmdm-remote",
                    width, height, metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    encoderSurface, null, handler);
            return true;
        } catch (Throwable t) {
            // A excecao real vai junto: "nenhum encoder utilizavel" sozinho nao distingue
            // encoder esgotado de resolucao recusada pelo aparelho.
            RemoteLog.e(this, "Encoder indisponivel em " + width + "x" + height + ": " + t);
            releaseEncoder();
            return false;
        }
    }

    private static byte frameType(MediaCodec.BufferInfo info) {
        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            return FRAME_CONFIG;
        }
        if ((info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
            return FRAME_KEY;
        }
        return FRAME_DELTA;
    }

    /**
     * Empacota [tipo][timestamp][dados] e entrega ao socket, descartando quando o link ja
     * esta congestionado.
     *
     * <p>Configuracao de codec e quadro-chave nunca sao descartados: sem eles o
     * visualizador nao decodifica nada do que vem depois, entao perder um custa o fluxo
     * inteiro em vez de um quadro.</p>
     */
    private void sendFrame(byte type, long presentationTimeUs, byte[] payload) {
        WebSocket ws = socket;
        if (ws == null || stopping || payload == null || payload.length == 0) {
            return;
        }
        if (type == FRAME_DELTA && ws.queueSize() > MAX_SOCKET_BACKLOG_BYTES) {
            return;
        }
        byte[] frame = new byte[9 + payload.length];
        frame[0] = type;
        for (int i = 0; i < 8; i++) {
            frame[1 + i] = (byte) (presentationTimeUs >>> (56 - 8 * i));
        }
        System.arraycopy(payload, 0, frame, 9, payload.length);
        ws.send(ByteString.of(frame));
    }

    private void announce() {
        WebSocket ws = socket;
        if (ws == null) {
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("type", "stream");
            json.put("format", "h264");
            json.put("width", width);
            json.put("height", height);
            // O painel usa isto para dizer ao operador se o clique vai funcionar, em vez de
            // deixa-lo descobrir clicando.
            json.put("input", InputInjectionService.isAvailable());
            ws.send(json.toString());
        } catch (Throwable t) {
            Log.w(TAG, "Nao foi possivel descrever o fluxo", t);
        }
    }

    private void fail(String reason) {
        RemoteLog.e(this, "Sessao falhou: " + reason);
        shutdown(reason);
    }

    private synchronized void shutdown(String reason) {
        if (stopping) {
            return;
        }
        stopping = true;
        streaming = false;

        WebSocket ws = socket;
        if (ws != null && reason != null) {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "stop");
                json.put("reason", reason);
                ws.send(json.toString());
            } catch (Throwable ignored) {
            }
        }
        cleanup();
        stopSelf();
    }

    /**
     * <p>Mantem a tela acesa enquanto durar a sessao, e acende se estiver apagada.</p>
     *
     * <p>Sem isto o atendimento morre sozinho: o aparelho atinge o tempo de suspensao, a
     * tela apaga, e o que o tecnico ve congela num quadro preto -- sem erro nenhum, porque
     * do ponto de vista do encoder nao ha falha, so' deixou de existir conteudo mudando.</p>
     *
     * <p>{@code SCREEN_BRIGHT_WAKE_LOCK} esta' obsoleto desde o Android 4.2, e mesmo assim
     * e' o certo aqui: a alternativa recomendada, {@code FLAG_KEEP_SCREEN_ON}, exige uma
     * janela, e este servico nao tem nenhuma -- criar uma janela invisivel so' para segurar
     * a tela dependeria da permissao de sobreposicao e acrescentaria uma superficie que
     * nada desenha.</p>
     *
     * <p>O tempo limite nao e' zelo excessivo: e' o que impede que uma sessao interrompida
     * de mau jeito -- rede caindo no meio, processo morto pelo sistema -- deixe a tela de
     * um tablet acesa ate a bateria acabar.</p>
     */
    private void acquireScreenLock() {
        if (screenLock != null) {
            return;
        }
        try {
            PowerManager power = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (power == null) {
                return;
            }
            screenLock = power.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "hwmdm:sessao-remota");
            screenLock.setReferenceCounted(false);
            screenLock.acquire(SCREEN_LOCK_TIMEOUT_MS);
        } catch (Throwable t) {
            // Manter a tela acesa e' conforto, nao requisito: uma sessao sem isso continua
            // valendo mais do que uma sessao que nao comeca.
            screenLock = null;
        }
    }

    private void releaseScreenLock() {
        if (screenLock == null) {
            return;
        }
        try {
            if (screenLock.isHeld()) {
                screenLock.release();
            }
        } catch (Throwable ignored) {
        }
        screenLock = null;
    }

    private void cleanup() {
        releaseScreenLock();
        if (virtualDisplay != null) {
            try { virtualDisplay.release(); } catch (Throwable ignored) { }
            virtualDisplay = null;
        }
        releaseEncoder();
        if (projection != null) {
            try { projection.stop(); } catch (Throwable ignored) { }
            projection = null;
        }
        if (socket != null) {
            try { socket.close(1000, "sessao encerrada"); } catch (Throwable ignored) { }
            socket = null;
        }
        if (worker != null) {
            worker.quitSafely();
            worker = null;
            handler = null;
        }
    }

    private void releaseEncoder() {
        if (encoder != null) {
            try { encoder.stop(); } catch (Throwable ignored) { }
            try { encoder.release(); } catch (Throwable ignored) { }
            encoder = null;
        }
        if (encoderSurface != null) {
            try { encoderSurface.release(); } catch (Throwable ignored) { }
            encoderSurface = null;
        }
    }

    /**
     * A notificacao e' visivel de proposito e nao pode ser dispensada: enquanto a tela
     * estiver sendo transmitida, quem estiver com o aparelho na mao tem de conseguir ver
     * isso sem procurar.
     */
    private void startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                        getString(R.string.notif_channel_sharing), NotificationManager.IMPORTANCE_LOW));
            }
        }
        Notification notification = new androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notif_sharing_title))
                .setContentText(getString(R.string.notif_sharing_text))
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        streaming = false;
        cleanup();
        super.onDestroy();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int round16(int value) {
        int rounded = (value / 16) * 16;
        return rounded < 16 ? 16 : rounded;
    }
}
