package com.hwmdm.remote.mdm;

import android.accessibilityservice.AccessibilityService;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONObject;

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

/**
 * <p>Avisa o servidor quando o Chrome mostra a pagina de site bloqueado pelo Web Filter.</p>
 *
 * <p>O filtro chega ao Chrome como politica gerenciada (URLBlocklist): quem bloqueia e' o
 * proprio navegador, no aparelho, e ele nao avisa ninguem. Sem este aviso o painel nunca
 * via uma tentativa feita no tablet -- so' as consultas DNS que chegavam ao resolvedor do
 * filtro, que o tablet nem usa.</p>
 *
 * <p>O que sai do aparelho e' so' a URL da pagina bloqueada, e so' quando a pagina de
 * bloqueio esta na tela. Nenhuma outra navegacao e' lida para fora, logada ou enviada.</p>
 */
public final class BlockedPageReporter {

    /** Os mesmos navegadores que recebem a politica (BrowserPolicy.PACKAGES no servidor). */
    private static final Set<String> BROWSERS = new HashSet<>(Arrays.asList(
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary"));

    /** Codigo que o Chrome mostra na pagina de erro de um site barrado por politica. */
    private static final String ERROR_CODE = "err_blocked_by_administrator";

    /**
     * Frases da mesma pagina, para quando o codigo nao aparecer. Sozinhas elas podem estar
     * num texto qualquer, por isso so' valem numa pagina pequena como a de erro.
     */
    private static final String[] PHRASES = {
            "não permite que você acesse", "nao permite que voce acesse",
            "doesn't allow you to view", "doesn’t allow you to view",
            "bloqueado pelo administrador", "blocked by your administrator"};
    private static final int SMALL_PAGE_NODES = 80;

    private static final int MAX_NODES = 500;

    /** Espera o fim da rajada de eventos de uma navegacao antes de olhar a tela. */
    private static final long SCAN_DELAY_MS = 800L;

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private static Handler handler;
    private static boolean scanPending;
    /** Host da pagina de bloqueio na tela; null quando o Chrome saiu dela. */
    private static String shownHost;
    private static volatile MdmLink.Config config;

    private BlockedPageReporter() {
    }

    public static boolean isBrowser(CharSequence pkg) {
        return pkg != null && BROWSERS.contains(pkg.toString());
    }

    /** Chamado pelo servico de acessibilidade a cada evento de um navegador. */
    public static synchronized void onBrowserEvent(final AccessibilityService service) {
        if (scanPending) {
            return;
        }
        if (handler == null) {
            HandlerThread thread = new HandlerThread("hwmdm-remote-webfilter");
            thread.start();
            handler = new Handler(thread.getLooper());
        }
        scanPending = true;
        handler.postDelayed(() -> scan(service), SCAN_DELAY_MS);
    }

    private static void scan(AccessibilityService service) {
        synchronized (BlockedPageReporter.class) {
            scanPending = false;
        }
        AccessibilityNodeInfo root;
        try {
            root = service.getRootInActiveWindow();
        } catch (Throwable t) {
            return;
        }
        if (root == null) {
            return;
        }
        try {
            if (!isBrowser(root.getPackageName())) {
                return;
            }
            String address = urlBarText(root);
            if (address == null) {
                // Omnibox em edicao: o texto ali nao e' a pagina carregada, e a proxima
                // pagina de bloqueio, mesmo do mesmo site, e' uma nova tentativa.
                shownHost = null;
                return;
            }
            if (!isBlockedPage(root)) {
                shownHost = null;
                return;
            }
            String host = hostOf(address);
            if (host == null || host.equals(shownHost)) {
                return;
            }
            shownHost = host;
            final String url = address;
            new Thread(() -> send(service, url, host), "hwmdm-remote-webfilter-send").start();
        } catch (Throwable t) {
            // A arvore muda enquanto e' lida; a proxima rajada de eventos tenta de novo.
        } finally {
            root.recycle();
        }
    }

    /** O endereco na barra do Chrome, ou null quando ela esta sendo editada. */
    private static String urlBarText(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> bars =
                root.findAccessibilityNodeInfosByViewId(root.getPackageName() + ":id/url_bar");
        String text = null;
        for (AccessibilityNodeInfo bar : bars) {
            if (text == null && !bar.isFocused() && bar.getText() != null) {
                text = bar.getText().toString().trim();
            }
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
                if (text.contains(ERROR_CODE)) {
                    node.recycle();
                    return true;
                }
                if (!phrase) {
                    for (String p : PHRASES) {
                        if (text.contains(p)) {
                            phrase = true;
                            break;
                        }
                    }
                }
                for (int i = 0; i < node.getChildCount(); i++) {
                    AccessibilityNodeInfo child = node.getChild(i);
                    if (child != null) {
                        queue.add(child);
                    }
                }
                node.recycle();
            }
            return phrase && queue.isEmpty() && visited <= SMALL_PAGE_NODES;
        } finally {
            for (AccessibilityNodeInfo n : queue) {
                n.recycle();
            }
        }
    }

    private static String textOf(AccessibilityNodeInfo node) {
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        String value = (text == null ? "" : text) + " " + (description == null ? "" : description);
        return value.toLowerCase(Locale.ROOT);
    }

    /** Host do endereco da barra ("facebook.com/x" vira "facebook.com"), ou null. */
    static String hostOf(String address) {
        String value = address.contains("://") ? address : "http://" + address;
        try {
            String host = Uri.parse(value).getHost();
            if (host == null || host.indexOf('.') < 0 || host.contains(" ")) {
                return null;
            }
            return host.toLowerCase(Locale.ROOT);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Envia a tentativa. Bloqueante: roda na thread propria criada em {@link #scan}. */
    private static void send(AccessibilityService service, String url, String host) {
        MdmLink.Config c = config;
        if (c == null) {
            c = MdmLink.query(service);
            config = c;
        }
        if (c == null || c.deviceId == null || c.deviceId.trim().isEmpty()) {
            RemoteLog.w(service, "Web Filter: tentativa bloqueada em " + host
                    + " nao enviada -- sem vinculo com o MDM");
            return;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(c.httpUrl("/rest/plugins/webfilter/public/blocked/"
                    + Uri.encode(c.deviceId))).openConnection();
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
                RemoteLog.i(service, "Web Filter: tentativa bloqueada no navegador enviada ao painel: " + host);
            } else {
                RemoteLog.w(service, "Web Filter: servidor recusou a tentativa bloqueada em " + host
                        + " (HTTP " + status + ")");
            }
        } catch (Throwable t) {
            RemoteLog.w(service, "Web Filter: nao foi possivel enviar a tentativa bloqueada em " + host + ": " + t);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
