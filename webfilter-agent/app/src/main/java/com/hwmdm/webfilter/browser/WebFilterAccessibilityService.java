package com.hwmdm.webfilter.browser;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.hwmdm.webfilter.mdm.RemoteLog;

public class WebFilterAccessibilityService extends AccessibilityService {

    private static volatile WebFilterAccessibilityService instance;

    public static boolean isAvailable() { return instance != null; }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        RemoteLog.i(this, "Web Filter: serviço de acessibilidade CONECTADO");
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        instance = null;
        RemoteLog.w(this, "Web Filter: serviço de acessibilidade DESCONECTADO");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (BlockedPageReporter.isBrowser(event.getPackageName())) {
            BlockedPageReporter.onBrowserEvent(this);
        }
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        // So' sair do navegador para outro app conta como navegacao nova. Janelas do
        // proprio Chrome e o teclado reiniciavam o bloqueio a cada toque (piscada e
        // o mesmo site reportado a cada 3-4 s).
        CharSequence pkg = event.getPackageName();
        if (pkg == null || BlockedPageReporter.isBrowser(pkg) || isKeyboard(pkg)
                || getPackageName().contentEquals(pkg)
                || "com.android.systemui".contentEquals(pkg)) {
            return;
        }
        BlockedPageReporter.newNavigation();
        if (BlockOverlay.isShown()) BlockOverlay.hide(this);
    }

    private boolean isKeyboard(CharSequence pkg) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm == null) return false;
        for (InputMethodInfo info : imm.getEnabledInputMethodList()) {
            if (info.getPackageName().contentEquals(pkg)) return true;
        }
        return false;
    }

    @Override
    public void onInterrupt() { }
}
