package com.hwmdm.webfilter.browser;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hwmdm.webfilter.mdm.RemoteLog;

public final class BlockOverlay {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static WebView view;
    private static String shownPage;

    private BlockOverlay() { }

    public static void show(final AccessibilityService service, final String page, final String html, final int top) {
        MAIN.post(() -> {
            try {
                WindowManager wm = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
                if (view != null) {
                    if (page.equals(shownPage)) return;
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
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        PixelFormat.TRANSLUCENT);
                lp.gravity = Gravity.TOP | Gravity.START;
                lp.y = Math.max(0, top);
                lp.height = Math.max(1, service.getResources().getDisplayMetrics().heightPixels - lp.y);
                wm.addView(web, lp);
                load(web, page, html);
                view = web;
                shownPage = page;
                RemoteLog.i(service, "Web Filter: página de bloqueio customizada exibida");
            } catch (Throwable t) {
                RemoteLog.w(service, "Web Filter: falha ao exibir overlay: " + t);
            }
        });
    }

    public static void hide(final AccessibilityService service) {
        if (view == null) return;
        MAIN.post(() -> {
            if (view == null) return;
            try {
                ((WindowManager) service.getSystemService(Context.WINDOW_SERVICE)).removeView(view);
                view.destroy();
            } catch (Throwable ignored) { }
            view = null;
            shownPage = null;
        });
    }

    public static boolean isShown() { return view != null; }

    static int contentTop(AccessibilityNodeInfo bar) {
        if (bar == null) return 0;
        Rect r = new Rect();
        bar.getBoundsInScreen(r);
        return r.bottom + (r.height() / 3);
    }

    private static void load(WebView web, String page, String html) {
        if (html != null) web.loadDataWithBaseURL(page, html, "text/html", "UTF-8", null);
        else web.loadUrl(page);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static WebView newWebView(Context context) {
        WebView web = new WebView(context);
        web.getSettings().setJavaScriptEnabled(true);
        web.setBackgroundColor(Color.WHITE);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView v, android.webkit.WebResourceRequest req,
                                        android.webkit.WebResourceError err) {
                if (req != null && req.isForMainFrame()) {
                    RemoteLog.w(context, "Web Filter: overlay não carregou: "
                            + (err == null ? "?" : err.getErrorCode() + " " + err.getDescription()));
                }
            }
        });
        return web;
    }
}
