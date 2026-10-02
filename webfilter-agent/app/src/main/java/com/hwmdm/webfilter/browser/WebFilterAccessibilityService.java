package com.hwmdm.webfilter.browser;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

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
        CharSequence statePkg = event.getPackageName();
        if (statePkg != null && !getPackageName().contentEquals(statePkg)
                && !"com.android.systemui".contentEquals(statePkg)) {
            BlockedPageReporter.newNavigation();
        }
        CharSequence ownerPkg = event.getPackageName() == null ? "" : event.getPackageName();
        if (!BlockedPageReporter.isBrowser(ownerPkg) && BlockOverlay.isShown()
                && !getPackageName().contentEquals(ownerPkg)
                && !"com.android.systemui".contentEquals(ownerPkg)) {
            BlockOverlay.hide(this);
        }
    }

    @Override
    public void onInterrupt() { }
}
