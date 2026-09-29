package com.hwmdm.remote.mdm;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * <p>A pagina de bloqueio customizada DENTRO do Chrome: uma janela de acessibilidade desenhada
 * sobre a area de conteudo do navegador, logo abaixo da barra de endereco, no lugar da tela
 * nativa "bloqueado pelo administrador".</p>
 *
 * <p>Nada e' digitado na barra e nenhuma aba e' aberta: o Chrome continua na pagina de erro
 * dele, e esta janela a cobre. A barra fica livre, entao o usuario digita outro endereco
 * normalmente; quando o Chrome sai da pagina de bloqueio ou perde a tela, a janela some.</p>
 *
 * <p>TYPE_ACCESSIBILITY_OVERLAY nao exige permissao alem do proprio servico de acessibilidade.</p>
 */
public final class BlockOverlay {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static WebView view;
    private static String shownPage;

    private BlockOverlay() {
    }

    /**
     * @param top altura, em pixels de tela, a partir da qual o conteudo do Chrome comeca
     *            (base da barra de ferramentas).
     */
    public static void show(final AccessibilityService service, final String page, final String html, final int top) {
        MAIN.post(() -> {
            try {
                WindowManager wm = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
                if (view != null) {
                    if (page.equals(shownPage)) {
                        return;
                    }
                    load(view, page, html);
                    shownPage = page;
                    return;
                }
                WebView web = newWebView(service);
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                // Janela criada por um servico nao nasce acelerada, e sem
                                // aceleracao a WebView nao desenha nada (tela branca).
                                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        PixelFormat.OPAQUE);
                lp.gravity = Gravity.TOP | Gravity.START;
                lp.y = Math.max(0, top);
                lp.height = Math.max(1, service.getResources().getDisplayMetrics().heightPixels - lp.y);
                wm.addView(web, lp);
                load(web, page, html);
                view = web;
                shownPage = page;
                RemoteLog.i(service, "Web Filter: pagina de bloqueio customizada exibida no Chrome");
            } catch (Throwable t) {
                RemoteLog.w(service, "Web Filter: nao foi possivel exibir a pagina de bloqueio: " + t);
            }
        });
    }

    public static void hide(final AccessibilityService service) {
        if (view == null) {
            return;
        }
        MAIN.post(() -> {
            if (view == null) {
                return;
            }
            try {
                ((WindowManager) service.getSystemService(Context.WINDOW_SERVICE)).removeView(view);
                view.destroy();
            } catch (Throwable ignored) {
                // A janela ja foi removida pelo sistema
            }
            view = null;
            shownPage = null;
        });
    }

    public static boolean isShown() {
        return view != null;
    }

    /** Base da barra de ferramentas do Chrome, em coordenadas de tela, ou 0. */
    static int contentTop(android.view.accessibility.AccessibilityNodeInfo bar) {
        if (bar == null) {
            return 0;
        }
        Rect r = new Rect();
        bar.getBoundsInScreen(r);
        // A barra de endereco fica dentro da toolbar, com uma folga embaixo
        return r.bottom + (r.height() / 3);
    }

    private static void load(WebView web, String page, String html) {
        if (html != null) {
            web.loadDataWithBaseURL(page, html, "text/html", "UTF-8", null);
        } else {
            web.loadUrl(page);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static WebView newWebView(final Context context) {
        WebView web = new WebView(context);
        web.getSettings().setJavaScriptEnabled(true);
        web.setBackgroundColor(android.graphics.Color.WHITE);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView v, android.webkit.WebResourceRequest req,
                                        android.webkit.WebResourceError err) {
                if (req != null && req.isForMainFrame()) {
                    RemoteLog.w(context, "Web Filter: a pagina de bloqueio nao carregou: "
                            + (err == null ? "?" : err.getErrorCode() + " " + err.getDescription()));
                }
            }
        });
        return web;
    }
}
