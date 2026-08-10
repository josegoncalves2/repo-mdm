package com.hwmdm.remote.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * <p>Reproduz no aparelho o toque e a digitacao que o tecnico faz no painel.</p>
 *
 * <p>Um servico de acessibilidade e' o unico caminho disponivel. {@code INJECT_EVENTS} e'
 * uma permissao de assinatura de sistema, e ser device owner nao a concede -- por isso
 * nenhuma ferramenta de suporte remoto que nao venha embutida na ROM consegue clicar sem
 * passar por aqui. Foi por nao existir esse caminho que a versao anterior deste recurso
 * era so' visualizacao.</p>
 *
 * <p><b>O escopo e' de saida, nao de entrada.</b> O servico declara apenas
 * {@code canPerformGestures} e nao pede {@code canRetrieveWindowContent} para observar a
 * tela. Ele nao le o conteudo das janelas nem registra o que o usuario faz; a unica
 * leitura que existe e' localizar o campo em foco na hora de escrever um texto, o que e'
 * inevitavel para digitar. A imagem da tela vem da MediaProjection, que o usuario autoriza
 * explicitamente e que mostra um indicador enquanto dura.</p>
 */
public class InputInjectionService extends AccessibilityService {

    private static final String TAG = "hwmdm-remote";

    /** Duracao de um toque simples. Curto o bastante para nao virar toque longo. */
    private static final long TAP_DURATION_MS = 60L;
    private static final long LONG_PRESS_DURATION_MS = 700L;

    private static volatile InputInjectionService instance;

    /**
     * @return o servico, ou {@code null} quando a acessibilidade nao foi habilitada nas
     *         configuracoes do aparelho. Nesse caso a sessao continua valida -- a tela e'
     *         transmitida -- apenas sem toque e sem digitacao.
     */
    public static InputInjectionService get() {
        return instance;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "Injecao de toque disponivel");
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    /**
     * <p>A unica coisa que este servico escuta: qual aplicativo assumiu a tela.</p>
     *
     * <p>Nao ha leitura de conteudo -- {@code getPackageName()} vem no proprio evento, sem
     * {@code canRetrieveWindowContent}. Serve a {@link ProtectionGuard}, que devolve o
     * aparelho a tela inicial quando as Configuracoes sobem com a protecao ativa.</p>
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        if (ProtectionGuard.shouldBlock(this, event.getPackageName())) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            ProtectionGuard.reportBlock(this, event.getPackageName());
        }
    }

    @Override
    public void onInterrupt() {
    }

    // =================================================================================================================

    /** Toque simples nas coordenadas em pixels da tela real. */
    public boolean tap(float x, float y) {
        return stroke(x, y, x, y, TAP_DURATION_MS);
    }

    public boolean longPress(float x, float y) {
        return stroke(x, y, x, y, LONG_PRESS_DURATION_MS);
    }

    /** Arrasto/rolagem: um unico traco continuo entre dois pontos. */
    public boolean swipe(float x1, float y1, float x2, float y2, long durationMs) {
        return stroke(x1, y1, x2, y2, Math.max(20L, Math.min(durationMs, 10_000L)));
    }

    private boolean stroke(float x1, float y1, float x2, float y2, long durationMs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }
        try {
            Path path = new Path();
            path.moveTo(x1, y1);
            if (x1 != x2 || y1 != y2) {
                path.lineTo(x2, y2);
            }
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0L, durationMs))
                    .build();
            return dispatchGesture(gesture, null, null);
        } catch (Throwable t) {
            Log.w(TAG, "Gesto recusado", t);
            return false;
        }
    }

    /**
     * <p>Escreve um texto no campo que estiver em foco.</p>
     *
     * <p>Vai pela acao {@code ACTION_SET_TEXT} do no em foco, e nao por eventos de tecla:
     * um aplicativo sem privilegio de sistema nao consegue injetar KeyEvent, e emular
     * teclado por gestos no teclado virtual dependeria do layout do teclado instalado --
     * o que quebra em cada aparelho diferente.</p>
     *
     * <p>Limite conhecido: substitui o conteudo do campo em vez de acrescentar ao final.
     * E' o que a API oferece; digitar caractere a caractere produziria uma corrida com o
     * proprio aplicativo em foco.</p>
     */
    public boolean type(String text) {
        if (text == null) {
            return false;
        }
        AccessibilityNodeInfo focused = null;
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            }
            if (focused == null) {
                Log.w(TAG, "Digitacao ignorada: nenhum campo de texto em foco");
                return false;
            }
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        } catch (Throwable t) {
            Log.w(TAG, "Digitacao recusada", t);
            return false;
        } finally {
            if (focused != null) {
                focused.recycle();
            }
        }
    }

    /** Teclas de navegacao do sistema, que nao tem coordenada na tela. */
    public boolean key(String name) {
        if (name == null) {
            return false;
        }
        Integer action = null;
        switch (name) {
            case "back":     action = GLOBAL_ACTION_BACK; break;
            case "home":     action = GLOBAL_ACTION_HOME; break;
            case "recents":  action = GLOBAL_ACTION_RECENTS; break;
            case "notifications": action = GLOBAL_ACTION_NOTIFICATIONS; break;
            default: break;
        }
        if (action == null) {
            Log.w(TAG, "Tecla desconhecida: " + name);
            return false;
        }
        try {
            return performGlobalAction(action);
        } catch (Throwable t) {
            Log.w(TAG, "Tecla recusada", t);
            return false;
        }
    }
}
