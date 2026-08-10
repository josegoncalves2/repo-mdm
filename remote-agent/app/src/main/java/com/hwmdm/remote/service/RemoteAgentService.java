package com.hwmdm.remote.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.hwmdm.remote.R;
import com.hwmdm.remote.mdm.LauncherControl;
import com.hwmdm.remote.mdm.MdmLink;
import com.hwmdm.remote.mdm.NetworkReporter;
import com.hwmdm.remote.mdm.RemoteLog;
import com.hwmdm.remote.service.ScreenStreamService;
import com.hwmdm.remote.ui.MessageActivity;
import com.hwmdm.remote.ui.ProjectionConsentActivity;

import org.json.JSONObject;

/**
 * <p>Servico residente. E' ele que faz este aplicativo existir enquanto ninguem o abre.</p>
 *
 * <p>Duas responsabilidades:</p>
 *
 * <ol>
 *   <li>Manter registrado, <b>em runtime</b>, o receiver do gatilho remoto. O registro tem
 *       de ser em runtime: desde o Android 8 um receiver declarado no manifest nao recebe
 *       broadcast implicito, e o launcher emite o dele sem destinatario explicito
 *       ({@code sendBroadcast(new Intent("com.hmdm.push." + tipo))}). Declarar no manifest
 *       compilaria, instalaria, e nunca dispararia -- o pior tipo de falha.</li>
 *   <li>Descobrir servidor e numero do aparelho pelo agente MDM, para nao haver
 *       configuracao duplicada.</li>
 * </ol>
 *
 * <p>Como o gatilho chega: o painel manda o push {@code remoteScreenStart}; o launcher
 * 6.36 nao conhece esse tipo, e por isso o repassa a quem estiver ouvindo, com o payload
 * em {@code com.hmdm.PUSH_DATA}. Nao ha nenhuma alteracao no launcher -- esse repasse e'
 * comportamento oficial dele para tipos desconhecidos.</p>
 */
public class RemoteAgentService extends Service {

    private static final String TAG = "hwmdm-remote";

    private static final String CHANNEL_ID = "hwmdm_remote_agent";
    private static final int NOTIFICATION_ID = 0xA1;

    /** Prefixo e extra definidos por com.hmdm.launcher.Const. */
    private static final String PUSH_PREFIX = "com.hmdm.push.";
    private static final String PUSH_EXTRA = "com.hmdm.PUSH_DATA";

    public static final String PUSH_START = PUSH_PREFIX + "remoteScreenStart";
    public static final String PUSH_STOP = PUSH_PREFIX + "remoteScreenStop";

    /*
     * Dois tipos de push que o painel sempre ofereceu e que o launcher 6.36 oficial nao
     * implementa: as constantes textMessage e lockKiosk nao existem no binario. Ele os
     * recebe, nao reconhece, e repassa para os plugins -- onde ate agora nao havia ninguem.
     * Atender aqui e' o que faz esses botoes funcionarem sem tocar no launcher.
     */
    public static final String PUSH_MESSAGE = PUSH_PREFIX + "textMessage";
    public static final String PUSH_LOCK_KIOSK = PUSH_PREFIX + "lockKiosk";

    /*
     * Liga e desliga a protecao que impede abrir as Configuracoes no aparelho. Tipos novos,
     * atendidos so' por este aplicativo -- o launcher os repassa por nao conhecer.
     *
     * O par existe porque a protecao se sustenta sozinha: uma vez ativa, desliga-la pelo
     * aparelho exigiria entrar nas Configuracoes, que e' exatamente o que ela bloqueia. Sem
     * um jeito de soltar a distancia, um engano custaria o tablet.
     */
    public static final String PUSH_PROTECT_ON = PUSH_PREFIX + "protectionOn";
    public static final String PUSH_PROTECT_OFF = PUSH_PREFIX + "protectionOff";

    private volatile MdmLink.Config config;
    private BroadcastReceiver trigger;
    private Thread addressReporter;

    /** De quanto em quanto tempo o aparelho reconfirma o proprio endereco. */
    private static final long ADDRESS_REPORT_INTERVAL_MS = 5 * 60 * 1000L;

    /*
     * Momento do ultimo chamado aceito.
     *
     * O operador clica "Conectar" varias vezes quando a imagem demora, e cada clique vira
     * um chamado. No Android 14 cada um abre um pedido de consentimento novo e uma sessao
     * nova, e as sessoes passam a se atropelar. Aceitar um chamado por vez e' o que
     * transforma cliques ansiosos em uma unica sessao, em vez de varias meio-abertas.
     */
    private volatile long lastRequestAt;
    private static final long REQUEST_DEBOUNCE_MS = 15_000L;

    public static void start(Context context) {
        Intent intent = new Intent(context, RemoteAgentService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundCompat();
        registerTrigger();
        new Thread(this::refreshConfig, "hwmdm-remote-mdmlink").start();
        startAddressReporting();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY: se o sistema matar o servico por pressao de memoria, ele volta.
        // Um agente de suporte que so' funciona ate o primeiro aperto de RAM nao serve.
        return START_STICKY;
    }

    private void registerTrigger() {
        trigger = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) {
                    return;
                }
                if (PUSH_START.equals(intent.getAction())) {
                    RemoteLog.i(RemoteAgentService.this, "Chamado de suporte recebido do launcher");
                    onStartRequested(intent.getStringExtra(PUSH_EXTRA));
                } else if (PUSH_STOP.equals(intent.getAction())) {
                    Log.i(TAG, "Suporte remoto: encerramento pedido pelo painel");
                    ScreenStreamService.stop(RemoteAgentService.this);
                } else if (PUSH_MESSAGE.equals(intent.getAction())) {
                    onMessageRequested(intent.getStringExtra(PUSH_EXTRA));
                } else if (PUSH_LOCK_KIOSK.equals(intent.getAction())) {
                    onLockKioskRequested();
                } else if (PUSH_PROTECT_ON.equals(intent.getAction())) {
                    onProtectionRequested(true);
                } else if (PUSH_PROTECT_OFF.equals(intent.getAction())) {
                    onProtectionRequested(false);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(PUSH_START);
        filter.addAction(PUSH_STOP);
        filter.addAction(PUSH_MESSAGE);
        filter.addAction(PUSH_LOCK_KIOSK);
        filter.addAction(PUSH_PROTECT_ON);
        filter.addAction(PUSH_PROTECT_OFF);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // O broadcast vem de outro aplicativo (o launcher), entao tem de ser exportado.
            registerReceiver(trigger, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(trigger, filter);
        }
        RemoteLog.i(this, "Agente de suporte no ar; gatilho registrado em " + PUSH_START);
    }

    /**
     * O payload traz o endereco do relay e o token de uso unico que o servidor emitiu para
     * esta sessao. Sem eles nao ha o que fazer: recusamos em vez de tentar adivinhar um
     * endereco, porque adivinhar aqui significaria transmitir a tela para o lugar errado.
     */
    private void onStartRequested(String payload) {
        final long now = System.currentTimeMillis();
        if (now - lastRequestAt < REQUEST_DEBOUNCE_MS && ScreenStreamService.isStreaming()) {
            RemoteLog.i(this, "Chamado ignorado: ja ha uma sessao em andamento");
            return;
        }
        lastRequestAt = now;

        if (payload == null || payload.trim().isEmpty()) {
            RemoteLog.w(this, "Chamado recusado: push sem parametros de sessao");
            return;
        }
        try {
            JSONObject json = new JSONObject(payload);
            String token = json.optString("token", null);
            if (token == null || token.trim().isEmpty()) {
                RemoteLog.w(this, "Chamado recusado: push sem token de sessao");
                return;
            }

            MdmLink.Config current = config;
            if (current == null) {
                current = MdmLink.query(this);
                config = current;
            }

            // A URL do relay pode vir pronta no payload; quando nao vem, montamos a partir
            // do que o agente MDM informou, que e' a fonte de verdade do endereco.
            String url = json.optString("url", null);
            if ((url == null || url.trim().isEmpty()) && current != null) {
                url = current.socketUrl("/ws/remote/agent/"
                        + android.net.Uri.encode(current.deviceId));
            }
            if (url == null || url.trim().isEmpty()) {
                RemoteLog.w(this, "Chamado recusado: sem endereco de relay e sem vinculo com o MDM");
                return;
            }

            RemoteLog.i(this, "Iniciando sessao; relay=" + url
                    + " consentimento_em_cache=" + ScreenStreamService.hasConsent());
            ProjectionConsentActivity.startSession(this, url, token,
                    json.optString("format", "h264"),
                    json.optInt("fps", 15),
                    json.optInt("bitrate", 2000000),
                    json.optInt("maxWidth", 800));
        } catch (Throwable t) {
            RemoteLog.e(this, "Chamado recusado: payload ilegivel (" + t + ")");
        }
    }

    /**
     * <p>Push {@code textMessage}: uma mensagem que o operador quer que apareca no aparelho.</p>
     *
     * <p>O payload e' o que com.hmdm.service.RemoteCommand monta: {@code text} obrigatorio e
     * {@code duration} opcional em segundos. Uma mensagem vazia nao vira uma janela em
     * branco -- isso so' confundiria quem esta' com o aparelho -- entao ela e' recusada e o
     * motivo vai para o log do servidor, onde o operador consegue ver.</p>
     */
    private void onMessageRequested(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            RemoteLog.w(this, "Mensagem recusada: push sem conteudo");
            return;
        }
        try {
            JSONObject json = new JSONObject(payload);
            String text = json.optString("text", "");
            if (text.trim().isEmpty()) {
                RemoteLog.w(this, "Mensagem recusada: campo 'text' vazio");
                return;
            }
            int duration = json.optInt("duration", 10);
            MessageActivity.show(this, text, duration);
            RemoteLog.i(this, "Mensagem exibida no aparelho (" + duration + "s)");
        } catch (Throwable t) {
            RemoteLog.e(this, "Mensagem recusada: payload ilegivel (" + t + ")");
        }
    }

    /**
     * Push {@code lockKiosk}: devolver o aparelho ao quiosque, o inverso de {@code exitKiosk}.
     */
    private void onLockKioskRequested() {
        String failure = LauncherControl.bringToFront(this);
        if (failure == null) {
            RemoteLog.i(this, "Quiosque restaurado: launcher trazido ao primeiro plano");
        } else {
            RemoteLog.w(this, "Nao foi possivel restaurar o quiosque: " + failure);
        }
    }

    /**
     * <p>Liga ou desliga a protecao das Configuracoes.</p>
     *
     * <p>Ligar sem acessibilidade nao produz protecao nenhuma: e' o servico de
     * acessibilidade que enxerga a janela subir e devolve o aparelho a tela inicial. Dizer
     * isso no log evita o pior resultado possivel aqui -- alguem confiar numa protecao que
     * nao esta em vigor.</p>
     */
    private void onProtectionRequested(boolean active) {
        ProtectionGuard.setActive(this, active);
        if (!active) {
            RemoteLog.i(this, "Protecao desligada: as Configuracoes voltam a abrir normalmente");
            return;
        }
        if (InputInjectionService.isAvailable()) {
            RemoteLog.i(this, "Protecao ligada: as Configuracoes ficam bloqueadas neste aparelho");
        } else {
            RemoteLog.w(this, "Protecao ligada, mas SEM EFEITO: depende da acessibilidade, "
                    + "que esta desligada neste aparelho");
        }
    }

    private void refreshConfig() {
        MdmLink.Config c = MdmLink.query(this);
        config = c;
        RemoteLog.i(this, c == null
                ? "Sem vinculo com o agente MDM"
                : "Vinculado ao MDM: " + c);
        NetworkReporter.report(this, c);
    }

    /**
     * <p>Reenvia o endereco do aparelho de tempos em tempos.</p>
     *
     * <p>Um endereco entregue por DHCP muda -- na renovacao da concessao, ao trocar de
     * ponto de acesso, ao voltar de um periodo sem rede. Informar so' uma vez, na partida,
     * daria um cadastro que envelhece em silencio: o painel mostraria com confianca um
     * endereco onde nao ha mais ninguem, que e' pior do que nao mostrar nada.</p>
     */
    private void startAddressReporting() {
        addressReporter = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(ADDRESS_REPORT_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                MdmLink.Config current = config;
                if (current == null) {
                    current = MdmLink.query(this);
                    config = current;
                }
                NetworkReporter.report(this, current);
            }
        }, "hwmdm-remote-ip");
        addressReporter.setDaemon(true);
        addressReporter.start();
    }

    public MdmLink.Config getConfig() {
        return config;
    }

    private void startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                        getString(R.string.notif_channel_agent), NotificationManager.IMPORTANCE_MIN);
                nm.createNotificationChannel(channel);
            }
        }
        Notification notification = new androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notif_agent_text))
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        if (addressReporter != null) {
            addressReporter.interrupt();
            addressReporter = null;
        }
        if (trigger != null) {
            try {
                unregisterReceiver(trigger);
            } catch (Throwable ignored) {
                // Ja removido.
            }
            trigger = null;
        }
        super.onDestroy();
    }
}
