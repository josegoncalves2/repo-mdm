package com.hwmdm.webfilter.browser;

import android.accessibilityservice.AccessibilityService;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.accessibility.AccessibilityNodeInfo;

import com.hwmdm.webfilter.mdm.MdmLink;
import com.hwmdm.webfilter.mdm.RemoteLog;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BlockedPageReporter {

    private static final Set<String> BROWSERS = new HashSet<>(Arrays.asList(
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary"));

    private static final String ERROR_CODE = "err_blocked_by_administrator";

    private static final String[] PHRASES = {
            "não permite que você acesse", "nao permite que voce acesse",
            "doesn't allow you to view", "doesn't allow you to view",
            "bloqueado pelo administrador", "blocked by your administrator"};
    private static final int SMALL_PAGE_NODES = 80;
    private static final int MAX_NODES = 500;
    private static final long SCAN_DELAY_MS = 800L;
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private static Handler handler;
    private static boolean scanPending;
    private static String shownHost;
    private static volatile MdmLink.Config config;

    private BlockedPageReporter() { }

    public static synchronized void newNavigation() { shownHost = null; }

    public static boolean isBrowser(CharSequence pkg) {
        return pkg != null && BROWSERS.contains(pkg.toString());
    }

    public static synchronized void onBrowserEvent(final AccessibilityService service) {
        if (scanPending) return;
        if (handler == null) {
            HandlerThread thread = new HandlerThread("hwmdm-webfilter-scan");
            thread.start();
            handler = new Handler(thread.getLooper());
        }
        scanPending = true;
        handler.postDelayed(() -> scan(service), SCAN_DELAY_MS);
    }

    private static void scan(AccessibilityService service) {
        synchronized (BlockedPageReporter.class) { scanPending = false; }
        AccessibilityNodeInfo root;
        try { root = service.getRootInActiveWindow(); } catch (Throwable t) { return; }
        if (root == null) return;
        try {
            if (!isBrowser(root.getPackageName())) return;
            // Barra focada ou pagina carregando nao provam que o usuario saiu: esconder
            // aqui e mostrar de novo no evento seguinte era a piscada do bloqueio.
            String address = urlBarText(root);
            if (address == null) return;
            String host = hostOf(address);
            if (!isBlockedPage(root)) {
                if (host == null || !host.equals(shownHost)) {
                    shownHost = null;
                    BlockOverlay.hide(service);
                }
                return;
            }
            if (host == null) return;
            if (host.equals(shownHost)) {
                BlockOverlay.reveal(service);
                return;
            }
            shownHost = host;
            final String url = address;
            AccessibilityNodeInfo bar = firstNode(root, root.getPackageName() + ":id/url_bar");
            final int top = BlockOverlay.contentTop(bar);
            if (bar != null) bar.recycle();
            new Thread(() -> {
                String page = send(service, url, host);
                if (page != null) {
                    String html = fetch(service, page);
                    BlockOverlay.show(service, page, html, top);
                }
            }, "hwmdm-webfilter-send").start();
        } catch (Throwable t) {
            // tree changes mid-read; next event batch retries
        } finally {
            root.recycle();
        }
    }

    private static String urlBarText(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> bars =
                root.findAccessibilityNodeInfosByViewId(root.getPackageName() + ":id/url_bar");
        String text = null;
        for (AccessibilityNodeInfo bar : bars) {
            if (text == null && !bar.isFocused() && bar.getText() != null)
                text = bar.getText().toString().trim();
            bar.recycle();
        }
        return text == null || text.isEmpty() ? null : text;
    }

    private static boolean isBlockedPage(AccessibilityNodeInfo root) {
        Deque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(AccessibilityNodeInfo.obtain(root));
        int visited = 0;
        boolean phrase = false;
        try {
            while (!queue.isEmpty() && visited < MAX_NODES) {
                AccessibilityNodeInfo node = queue.poll();
                visited++;
                String text = textOf(node);
                if (text.contains(ERROR_CODE)) { node.recycle(); return true; }
                if (!phrase) {
                    for (String p : PHRASES) {
                        if (text.contains(p)) { phrase = true; break; }
                    }
                }
                for (int i = 0; i < node.getChildCount(); i++) {
                    AccessibilityNodeInfo child = node.getChild(i);
                    if (child != null) queue.add(child);
                }
                node.recycle();
            }
            return phrase && queue.isEmpty() && visited <= SMALL_PAGE_NODES;
        } finally {
            for (AccessibilityNodeInfo n : queue) n.recycle();
        }
    }

    private static String textOf(AccessibilityNodeInfo node) {
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        return ((text == null ? "" : text) + " " + (desc == null ? "" : desc)).toString().toLowerCase(Locale.ROOT);
    }

    static String hostOf(String address) {
        String value = address.contains("://") ? address : "http://" + address;
        try {
            String host = Uri.parse(value).getHost();
            if (host == null || host.indexOf('.') < 0 || host.contains(" ")) return null;
            return host.toLowerCase(Locale.ROOT);
        } catch (Throwable t) { return null; }
    }

    private static AccessibilityNodeInfo firstNode(AccessibilityNodeInfo root, String viewId) {
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(viewId);
        AccessibilityNodeInfo found = null;
        for (AccessibilityNodeInfo n : nodes) {
            if (found == null) found = n; else n.recycle();
        }
        return found;
    }

    private static String fetch(AccessibilityService service, String page) {
        HttpURLConnection connection = null;
        try {
            String address = page;
            MdmLink.Config c = config;
            Uri u = Uri.parse(page);
            if (c != null && u.getEncodedPath() != null) {
                address = c.httpUrl(u.getEncodedPath()
                        + (u.getEncodedQuery() == null ? "" : "?" + u.getEncodedQuery()));
            }
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            if (connection.getResponseCode() != 200) return null;
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int n;
                while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
                return buf.toString("UTF-8");
            }
        } catch (Throwable t) {
            RemoteLog.w(service, "Web Filter: falha ao baixar página de bloqueio " + page + ": " + t);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String send(AccessibilityService service, String url, String host) {
        MdmLink.Config c = config;
        if (c == null) {
            try { c = MdmLink.query(service); } catch (Exception ignored) { }
            config = c;
        }
        if (c == null || c.deviceNumber == null || c.deviceNumber.trim().isEmpty()) {
            RemoteLog.w(service, "Web Filter: bloqueio em " + host + " não enviado — sem vínculo MDM");
            return null;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(c.httpUrl("/rest/plugins/webfilter/public/blocked/"
                    + Uri.encode(c.deviceNumber))).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            JSONObject body = new JSONObject();
            body.put("url", url);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.toString().getBytes("UTF-8"));
            }
            int status = connection.getResponseCode();
            if (status == 200) {
                RemoteLog.i(service, "Web Filter: bloqueio reportado ao painel: " + host);
                try (InputStream in = connection.getInputStream()) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    byte[] chunk = new byte[4096];
                    int n;
                    while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
                    String page = new JSONObject(buf.toString("UTF-8")).optString("data", "");
                    return page.startsWith("http") ? page : null;
                }
            }
        } catch (Throwable t) {
            RemoteLog.w(service, "Web Filter: falha ao reportar bloqueio em " + host + ": " + t);
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }
}
