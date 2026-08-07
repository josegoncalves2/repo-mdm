package com.hwmdm.remote.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

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
        if (ScreenStreamService.hasConsent()) {
            launchService(context, buildIntent(context, url, token, format, fps, bitrate, maxWidth));
            return;
        }
        Intent consent = new Intent(context, ProjectionConsentActivity.class);
        consent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        putExtras(consent, url, token, format, fps, bitrate, maxWidth);
        context.startActivity(consent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            Log.w(TAG, "Aparelho sem gerenciador de projecao");
            finish();
            return;
        }
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CODE);
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
            Log.w(TAG, "Sessao recusada: consentimento negado no aparelho");
        }
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
