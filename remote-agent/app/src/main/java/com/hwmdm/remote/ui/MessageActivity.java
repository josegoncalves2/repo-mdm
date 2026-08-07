package com.hwmdm.remote.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hwmdm.remote.R;

/**
 * <p>Exibe no aparelho uma mensagem enviada pelo operador do painel.</p>
 *
 * <p>Este e' o handler do push {@code textMessage}. O launcher 6.36 oficial nao implementa
 * esse tipo -- a constante nem existe no binario -- entao ele cai no repasse para plugins
 * ({@code com.hmdm.push.textMessage}) e chega aqui. Nao ha nenhuma alteracao no launcher.</p>
 *
 * <p>E' uma Activity, e nao um Toast, de proposito. Um Toast e' curto, some sozinho, e a
 * partir do Android 11 e' silenciosamente descartado quando vem de segundo plano -- ou
 * seja, o operador acharia que avisou e o aparelho nao teria mostrado nada. Uma janela de
 * dialogo aparece por cima do que estiver na tela, aceita texto longo e so' sai quando
 * alguem confirma ou quando o tempo pedido acaba.</p>
 */
public class MessageActivity extends Activity {

    private static final String EXTRA_TEXT = "com.hwmdm.remote.MESSAGE_TEXT";
    private static final String EXTRA_DURATION = "com.hwmdm.remote.MESSAGE_DURATION";

    /** Os mesmos limites que com.hmdm.service.RemoteCommand aplica no servidor. */
    private static final int MAX_TEXT = 500;
    private static final int MIN_SECONDS = 1;
    private static final int MAX_SECONDS = 3600;
    private static final int DEFAULT_SECONDS = 10;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoClose;

    public static void show(Context context, String text, int durationSeconds) {
        Intent intent = new Intent(context, MessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_TEXT, text);
        intent.putExtra(EXTRA_DURATION, durationSeconds);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(false);

        // O aparelho de um usuario em atendimento costuma estar com a tela apagada. Uma
        // mensagem que so' aparece quando alguem por acaso liga a tela nao e' um aviso.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setTitle(R.string.message_title);
        setContentView(buildView(readText(getIntent())));

        scheduleAutoClose(readDuration(getIntent()));
    }

    /**
     * Uma segunda mensagem enquanto a primeira esta' na tela substitui a primeira, em vez de
     * empilhar duas janelas: com launchMode singleTask o sistema entrega aqui.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        setContentView(buildView(readText(intent)));
        scheduleAutoClose(readDuration(intent));
    }

    @Override
    protected void onDestroy() {
        cancelAutoClose();
        super.onDestroy();
    }

    // =================================================================================================================

    private View buildView(String text) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 32);

        TextView body = new TextView(this);
        body.setText(text);
        body.setTextSize(18);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        Button ok = new Button(this);
        ok.setText(R.string.message_dismiss);
        ok.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams okParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        okParams.gravity = Gravity.END;
        okParams.topMargin = 24;
        root.addView(ok, okParams);

        return root;
    }

    private void scheduleAutoClose(int seconds) {
        cancelAutoClose();
        autoClose = this::finish;
        handler.postDelayed(autoClose, seconds * 1000L);
    }

    private void cancelAutoClose() {
        if (autoClose != null) {
            handler.removeCallbacks(autoClose);
            autoClose = null;
        }
    }

    private String readText(Intent intent) {
        String text = intent == null ? null : intent.getStringExtra(EXTRA_TEXT);
        if (text == null || text.trim().isEmpty()) {
            return getString(R.string.message_empty);
        }
        return text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
    }

    private int readDuration(Intent intent) {
        int seconds = intent == null ? DEFAULT_SECONDS
                : intent.getIntExtra(EXTRA_DURATION, DEFAULT_SECONDS);
        if (seconds < MIN_SECONDS) {
            return DEFAULT_SECONDS;
        }
        return Math.min(seconds, MAX_SECONDS);
    }
}
