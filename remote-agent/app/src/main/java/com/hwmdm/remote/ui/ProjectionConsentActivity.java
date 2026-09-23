package com.hwmdm.remote.ui;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

import com.hwmdm.remote.mdm.RemoteLog;
import com.hwmdm.remote.service.ProtectionGuard;
import com.hwmdm.remote.service.ScreenStreamService;

/**
 * <p>Obtem o consentimento de MediaProjection que o Android exige antes de qualquer leitura
 * de tela, e entrega os parametros da sessao ao {@link ScreenStreamService}.</p>
 *
 * <p>Nao ha como evitar este pedido: privilegio de device owner nao dispensa a confirmacao.
 * Ate o Android 13 o token e' guardado e reaproveitado, entao so' a primeira sessao apos
 * um reinicio pergunta. Do Android 14 em diante a plataforma o torna de uso unico e cada
 * sessao pergunta de novo.</p>
 *
 * <p>A activity e' invisivel e termina assim que o usuario responde. Ela carrega o endereco
 * do relay e o token adiante porque os dois chegam junto com o push e se perderiam
 * enquanto o dialogo esta na tela.</p>
 */
public class ProjectionConsentActivity extends Activity {

    private static final String TAG = "hwmdm-remote";
    private static final int REQUEST_CODE = 0xA3;

    /** Inicia a sessao, pedindo consentimento apenas quando nao ha token aproveitavel. */
    public static void startSession(Context context, String url, String token, String format,
                                    int fps, int bitrate, int maxWidth) {
        // Acorda a tela ANTES de qualquer outra coisa. Ate aqui nada no fluxo de chamada
        // pedia para a tela ligar -- so' o ScreenStreamService, depois de a sessao ja estar
        // em andamento, tratava disso. Se o aparelho estiver dormindo quando o chamado
        // chega, a activity de consentimento e' criada (a isencao de sobreposicao cuida
        // disso) mas a tela fisica nunca acende: o dialogo fica esperando uma resposta que
        // o usuario nao pode dar porque nao ve nada, e a sessao trava em "conectando" com a
        // tela do tablet preta ate alguem tocar fisicamente no aparelho. E' o mesmo sintoma
        // relatado como "a tela fica preta se nao mexer no tablet", so' que na largada da
        // sessao em vez de no meio dela.
        wakeScreenForConsent(context);

        if (ScreenStreamService.hasConsent()) {
            launchService(context, buildIntent(context, url, token, format, fps, bitrate, maxWidth));
            return;
        }
        Intent consent = new Intent(context, ProjectionConsentActivity.class);
        consent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        putExtras(consent, url, token, format, fps, bitrate, maxWidth);

        /*
         * Sem a permissao de sobreposicao, o Android 10+ descarta este startActivity quando
         * o aplicativo nao esteve em primeiro plano recentemente -- e descarta em silencio,
         * sem lancar excecao. O sintoma no tablet e' nada acontecer.
         *
         * Avisar antes de tentar e' o que transforma "nao funcionou" em "falta conceder a
         * permissao neste aparelho", que e' uma frase acionavel.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            RemoteLog.w(context, "Consentimento pode nao aparecer: falta a permissao de "
                    + "sobreposicao de tela neste aparelho (Configuracoes > Exibir sobre outros apps)");
        }
        // A janela de autorizacao da captura pertence as Configuracoes neste aparelho, e a
        // protecao fecha as Configuracoes. Sem suspende-la, um recurso derruba o outro.
        ProtectionGuard.suspend(90_000L);
        try {
            context.startActivity(consent);
            RemoteLog.i(context, "Pedindo consentimento de captura ao usuario");
        } catch (Throwable t) {
            RemoteLog.e(context, "Nao foi possivel exibir o pedido de consentimento: " + t);
        }
    }

    /**
     * <p>Wake lock de vida curta so' para acender a tela fisica a tempo do dialogo de
     * consentimento aparecer.</p>
     *
     * <p>{@code SCREEN_BRIGHT_WAKE_LOCK} com {@code ACQUIRE_CAUSES_WAKEUP} e' o mesmo par
     * ja usado por {@link ScreenStreamService} para manter a tela acesa durante a
     * transmissao -- aqui ele cobre a lacuna anterior a isso, entre o push chegar e a
     * sessao comecar de fato. O teto de 30s nao precisa ser generoso: assim que a
     * activity aparece, a propria janela dela carrega {@code FLAG_KEEP_SCREEN_ON} (ver
     * {@link #onCreate}), e assim que o usuario responde o {@code ScreenStreamService}
     * assume com o seu proprio lock.</p>
     */
    private static void wakeScreenForConsent(Context context) {
        try {
            PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (power == null) {
                return;
            }
            PowerManager.WakeLock lock = power.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "hwmdm:consentimento");
            lock.setReferenceCounted(false);
            lock.acquire(30_000L);
        } catch (Throwable ignored) {
            // Acender a tela e' o que torna o dialogo visivel, nao um requisito para a
            // sessao existir -- um aparelho ja acordado (o caso comum, alguem com o
            // tablet na mao pedindo suporte) nao depende disto em nada.
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyScreenWakeFlags();
        dismissKeyguardIfPossible();
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            Log.w(TAG, "Aparelho sem gerenciador de projecao");
            finish();
            return;
        }
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    /**
     * <p>Faz esta janela aparecer por cima da tela de bloqueio e manter a tela acesa
     * enquanto estiver visivel.</p>
     *
     * <p>{@code FLAG_TURN_SCREEN_ON} e {@code FLAG_DISMISS_KEYGUARD} estao obsoletos desde
     * o Android 8.1 em favor de {@code setTurnScreenOn}/{@code setShowWhenLocked}, e mesmo
     * assim ficam os dois aqui: aparelhos anteriores ao 8.1 so entendem as flags de janela,
     * e manter as duas formas cobre a faixa inteira sem precisar de outro caminho. Um
     * aparelho com bloqueio protegido por senha continua exigindo a senha -- isto nao
     * contorna isso, so evita que a tela fique preta num aparelho sem bloqueio nenhum, que
     * e' o caso normal de um tablet em quiosque.</p>
     */
    @SuppressWarnings("deprecation")
    private void applyScreenWakeFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    /**
     * <p>Pede ao Android para dispensar o bloqueio de tela, alem de so' aparecer por cima
     * dele.</p>
     *
     * <p>{@code setShowWhenLocked}/{@code setTurnScreenOn} (em {@link #applyScreenWakeFlags})
     * fazem esta janela aparecer sobre o keyguard, mas nao o dispensam -- num aparelho com
     * bloqueio protegido por PIN/padrao/senha o Android continua exigindo a credencial por
     * cima da janela. {@code requestDismissKeyguard} e' o unico caminho de app comum (sem
     * ser device owner) para pedir a dispensa: quando o bloqueio NAO tem credencial (o caso
     * usual de um tablet em quiosque, so' com "deslizar para desbloquear" ou nada), o
     * Android dispensa sozinho e o callback chama {@code onDismissSucceeded()}. Quando HA
     * credencial, o proprio Android mostra a tela de bloqueio para o usuario digitar --
     * ninguem, nem device owner, contorna isso sem a senha: e' a mesma garantia que protege
     * um aparelho perdido ou roubado, e nao existe excecao para MDM.</p>
     *
     * <p><b>Verificado no codigo deste projeto, nao presumido:</b> perguntei se o launcher
     * (device owner) oferece um caminho proprio de dispensa de keyguard pelo plugin que este
     * aplicativo ja usa ({@link com.hwmdm.remote.mdm.MdmLink}, interface
     * {@code com.hmdm.IMdmApi}). Ele nao oferece -- o AIDL
     * ({@code app/src/main/aidl/com/hmdm/IMdmApi.aidl}) expoe {@code queryConfig},
     * {@code log}, {@code queryAppPreference}, {@code setAppPreference},
     * {@code commitAppPreferences}, {@code getVersion}, {@code queryPrivilegedConfig},
     * {@code setCustom}, {@code forceConfigUpdate} e {@code sendPush}; nenhum deles chama
     * {@code DevicePolicyManager.setKeyguardDisabled} ou equivalente. Esse metodo do
     * Android so' existe para quem E' device owner/profile owner -- que e' o
     * {@code com.hmdm.launcher}, nao este aplicativo -- e {@code LauncherControl.java} (o
     * outro ponto de contato com o launcher) so' traz {@code bringToFront}, sem nada de
     * bloqueio de tela. Sem alterar o launcher (fora do escopo autorizado aqui), nao ha
     * caminho de device owner disponivel para este recurso.</p>
     */
    private void dismissKeyguardIfPossible() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // requestDismissKeyguard existe desde a API 26. Abaixo disso, as flags de
            // janela aplicadas em applyScreenWakeFlags (FLAG_DISMISS_KEYGUARD, obsoleta mas
            // funcional em aparelhos antigos) sao o unico mecanismo disponivel.
            return;
        }
        try {
            KeyguardManager keyguard = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguard == null || !keyguard.isKeyguardLocked()) {
                return;
            }
            keyguard.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                @Override
                public void onDismissSucceeded() {
                    RemoteLog.i(ProjectionConsentActivity.this,
                            "Bloqueio de tela dispensado para a sessao de suporte");
                }

                @Override
                public void onDismissError() {
                    // Nao e' uma falha silenciosa: registrada no log do painel, porque quem
                    // esta' atendendo precisa saber que o toque nao vai funcionar ate
                    // alguem desbloquear fisicamente o aparelho.
                    RemoteLog.w(ProjectionConsentActivity.this,
                            "Nao foi possivel dispensar o bloqueio de tela automaticamente");
                }

                @Override
                public void onDismissCancelled() {
                    RemoteLog.w(ProjectionConsentActivity.this,
                            "Dispensa do bloqueio de tela cancelada");
                }
            });
        } catch (Throwable t) {
            Log.w(TAG, "requestDismissKeyguard indisponivel", t);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (resultCode == RESULT_OK && data != null) {
            ScreenStreamService.cacheConsent(resultCode, data);
            Intent source = getIntent();
            launchService(this, buildIntent(this,
                    source.getStringExtra(ScreenStreamService.EXTRA_URL),
                    source.getStringExtra(ScreenStreamService.EXTRA_TOKEN),
                    source.getStringExtra(ScreenStreamService.EXTRA_FORMAT),
                    source.getIntExtra(ScreenStreamService.EXTRA_FPS, 15),
                    source.getIntExtra(ScreenStreamService.EXTRA_BITRATE, 2000000),
                    source.getIntExtra(ScreenStreamService.EXTRA_MAX_WIDTH, 800)));
        } else {
            // No servidor, e nao so' no logcat: quem esta' no painel precisa distinguir
            // "o usuario recusou" de "nada apareceu na tela", que ate agora eram a mesma
            // coisa vista de fora.
            RemoteLog.w(this, "Sessao recusada: consentimento negado pelo usuario no aparelho");
        }
        // Respondido: a protecao volta imediatamente, sem esperar o prazo da suspensao.
        ProtectionGuard.resume();
        finish();
    }

    private static Intent buildIntent(Context context, String url, String token, String format,
                                      int fps, int bitrate, int maxWidth) {
        Intent intent = new Intent(context, ScreenStreamService.class);
        intent.setAction(ScreenStreamService.ACTION_START);
        putExtras(intent, url, token, format, fps, bitrate, maxWidth);
        return intent;
    }

    private static void putExtras(Intent intent, String url, String token, String format,
                                  int fps, int bitrate, int maxWidth) {
        intent.putExtra(ScreenStreamService.EXTRA_URL, url);
        intent.putExtra(ScreenStreamService.EXTRA_TOKEN, token);
        intent.putExtra(ScreenStreamService.EXTRA_FORMAT, format);
        intent.putExtra(ScreenStreamService.EXTRA_FPS, fps);
        intent.putExtra(ScreenStreamService.EXTRA_BITRATE, bitrate);
        intent.putExtra(ScreenStreamService.EXTRA_MAX_WIDTH, maxWidth);
    }

    private static void launchService(Context context, Intent service) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
    }
}
