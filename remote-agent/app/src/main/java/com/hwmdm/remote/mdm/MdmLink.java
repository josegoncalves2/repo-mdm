package com.hwmdm.remote.mdm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.hmdm.IMdmApi;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>Vinculo com o agente MDM instalado no aparelho.</p>
 *
 * <p>Este aplicativo nao guarda configuracao propria: nao tem tela de servidor, nao tem
 * campo de numero de serie, nao pede nada ao usuario. Ele pergunta ao launcher, pela API
 * publica de plugins que o proprio launcher expoe
 * ({@code com.hmdm.action.Connect} -> {@link IMdmApi}), qual e' o servidor e qual e' o
 * numero deste aparelho.</p>
 *
 * <p>Isso e' deliberado. Configuracao duplicada e' configuracao que diverge: um tablet
 * reaproveitado, um servidor que muda de endereco, e o app de suporte passa a apontar para
 * um lugar onde ninguem esta ouvindo -- sem nenhum sinal disso ate a hora em que o suporte
 * e' necessario. Perguntando, existe uma verdade so'.</p>
 *
 * <p>Usamos {@code queryConfig()}, a variante nao privilegiada. A variante privilegiada
 * exige uma chave de API compartilhada e devolve IMEI e numero de serie, que este
 * aplicativo nao tem motivo para conhecer.</p>
 */
public final class MdmLink {

    private static final String TAG = "hwmdm-remote";

    private static final String LAUNCHER_PACKAGE = "com.hmdm.launcher";
    private static final String CONNECT_ACTION = "com.hmdm.action.Connect";

    /** Chaves do Bundle devolvido, definidas por com.hmdm.launcher.service.PluginApiService. */
    private static final String KEY_SERVER_HOST = "SERVER_HOST";
    private static final String KEY_SERVER_PATH = "SERVER_PATH";
    private static final String KEY_DEVICE_ID = "DEVICE_ID";
    private static final String KEY_IS_MANAGED = "IS_MANAGED";

    private static final long BIND_TIMEOUT_MS = 10_000L;

    /** O que este aplicativo precisa saber para existir. */
    public static final class Config {
        public final String serverHost;
        public final String serverPath;
        public final String deviceId;
        public final boolean managed;

        Config(String serverHost, String serverPath, String deviceId, boolean managed) {
            this.serverHost = serverHost;
            this.serverPath = serverPath;
            this.deviceId = deviceId;
            this.managed = managed;
        }

        /**
         * Monta a URL do socket a partir do que o launcher informou, convertendo o esquema:
         * um painel em https tem de produzir um uplink em wss, ou a tela do aparelho
         * viajaria em texto claro por uma rede que nao controlamos.
         */
        public String socketUrl(String path) {
            String base = serverHost == null ? "" : serverHost.trim();
            while (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            if (base.startsWith("https://")) {
                base = "wss://" + base.substring("https://".length());
            } else if (base.startsWith("http://")) {
                base = "ws://" + base.substring("http://".length());
            }
            if (serverPath != null && !serverPath.trim().isEmpty()) {
                base = base + "/" + serverPath.trim();
            }
            return base + path;
        }

        /**
         * A mesma base, sem trocar o esquema: para chamar a API REST do painel, e nao o
         * relay. Mantida ao lado de {@link #socketUrl(String)} porque as duas descrevem o
         * mesmo servidor, e separa-las convida a divergirem.
         */
        public String httpUrl(String path) {
            String base = serverHost == null ? "" : serverHost.trim();
            while (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            if (serverPath != null && !serverPath.trim().isEmpty()) {
                base = base + "/" + serverPath.trim();
            }
            return base + path;
        }

        @Override
        public String toString() {
            return deviceId + " @ " + serverHost;
        }
    }

    private MdmLink() {
    }

    /**
     * <p>Le a configuracao do launcher. Bloqueante: chame de uma thread de trabalho.</p>
     *
     * @return a configuracao, ou {@code null} quando o launcher nao esta instalado, nao
     *         respondeu a tempo, ou ainda nao concluiu a propria matricula.
     */
    public static Config query(Context context) {
        final AtomicReference<Config> result = new AtomicReference<>(null);
        final CountDownLatch done = new CountDownLatch(1);

        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                try {
                    IMdmApi api = IMdmApi.Stub.asInterface(binder);
                    Bundle bundle = api.queryConfig();
                    if (bundle != null) {
                        String deviceId = bundle.getString(KEY_DEVICE_ID);
                        String host = bundle.getString(KEY_SERVER_HOST);
                        if (deviceId != null && !deviceId.trim().isEmpty()
                                && host != null && !host.trim().isEmpty()) {
                            result.set(new Config(host, bundle.getString(KEY_SERVER_PATH),
                                    deviceId, bundle.getBoolean(KEY_IS_MANAGED, false)));
                        } else {
                            // Acontece quando o launcher esta instalado mas ainda nao
                            // matriculou: ele responde, com um Bundle sem identidade.
                            Log.w(TAG, "Agente MDM respondeu sem servidor ou sem numero de aparelho");
                        }
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Falha ao consultar o agente MDM", t);
                } finally {
                    done.countDown();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                done.countDown();
            }

            @Override
            public void onNullBinding(ComponentName name) {
                done.countDown();
            }
        };

        Intent intent = new Intent(CONNECT_ACTION);
        intent.setPackage(LAUNCHER_PACKAGE);

        boolean bound = false;
        try {
            bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
            if (!bound) {
                Log.w(TAG, "Agente MDM (" + LAUNCHER_PACKAGE + ") nao esta disponivel para vinculo");
                return null;
            }
            if (!done.await(BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                Log.w(TAG, "Agente MDM nao respondeu em " + BIND_TIMEOUT_MS + " ms");
                return null;
            }
            return result.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (bound) {
                try {
                    context.unbindService(connection);
                } catch (Throwable ignored) {
                    // Ja desconectado.
                }
            }
        }
    }
}
