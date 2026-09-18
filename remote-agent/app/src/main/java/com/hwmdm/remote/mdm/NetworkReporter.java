package com.hwmdm.remote.mdm;

import android.content.Context;
import android.net.Uri;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;

/**
 * <p>Informa ao servidor em que endereco este aparelho esta' alcancavel.</p>
 *
 * <p>O servidor sozinho nao consegue saber. Ele so' enxerga de onde a conexao chegou, e
 * nesta rede isso passa por um proxy HTTP e por mais um NAT depois dele: o endereco
 * observado e' o do ultimo salto, identico para todos os tablets. Nenhum cabecalho corrige
 * isso, porque o segundo NAT nao escreve nenhum. Quem sabe o endereco do aparelho e' o
 * aparelho.</p>
 */
public final class NetworkReporter {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private NetworkReporter() {
    }

    /**
     * <p>O endereco da interface por onde este aparelho fala com {@code serverHost}.</p>
     *
     * <p>Descoberto "conectando" um socket UDP ao servidor e lendo o endereco local que o
     * sistema escolheu. Nao ha trafego: conectar um socket UDP nao envia nada, so' fixa a
     * rota. E' o unico jeito honesto de responder a pergunta -- percorrer a lista de
     * interfaces devolveria a primeira que tiver um endereco, que num tablet com Wi-Fi,
     * dados moveis e interfaces virtuais frequentemente nao e' a que esta' em uso.</p>
     *
     * @return o endereco IPv4, ou null se nao for possivel determinar.
     */
    public static String localAddressFor(String serverHost) {
        if (serverHost == null || serverHost.trim().isEmpty()) {
            return null;
        }
        DatagramSocket socket = null;
        try {
            Uri uri = Uri.parse(serverHost.trim());
            String host = uri.getHost();
            if (host == null) {
                return null;
            }
            int port = uri.getPort();
            if (port <= 0) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }

            socket = new DatagramSocket();
            socket.connect(new InetSocketAddress(InetAddress.getByName(host), port));
            InetAddress local = socket.getLocalAddress();
            if (local == null || local.isAnyLocalAddress() || local.isLoopbackAddress()) {
                return null;
            }
            String address = local.getHostAddress();
            if (address == null) {
                return null;
            }
            // Descarta IPv6 e o escopo que o Android anexa (fe80::1%wlan0): o cadastro do
            // painel guarda um IPv4.
            int scope = address.indexOf('%');
            if (scope >= 0) {
                address = address.substring(0, scope);
            }
            return address.indexOf(':') >= 0 ? null : address;
        } catch (Throwable t) {
            return null;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    /**
     * <p>Descobre o endereco e o envia. Bloqueante: chame de uma thread de trabalho.</p>
     *
     * <p>Silencioso quando nao ha vinculo com o MDM ou quando o endereco nao pode ser
     * determinado -- sao estados transitorios de rede, nao falhas que alguem precise ler
     * no log do painel a cada minuto.</p>
     */
    public static void report(Context context, MdmLink.Config config) {
        if (config == null || config.deviceId == null || config.deviceId.trim().isEmpty()) {
            return;
        }
        final String ip = localAddressFor(config.serverHost);
        if (ip == null) {
            return;
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(config.httpUrl("/rest/plugins/deviceip/public/"
                    + Uri.encode(config.deviceId)));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("ip", ip);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.toString().getBytes("UTF-8"));
            }

            int status = connection.getResponseCode();
            if (status != 200) {
                RemoteLog.w(context, "Servidor recusou o endereco " + ip + " (HTTP " + status + ")");
            }
        } catch (Throwable t) {
            RemoteLog.w(context, "Nao foi possivel informar o endereco do aparelho: " + t);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
