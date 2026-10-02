package com.hwmdm.webfilter.mdm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.hmdm.IMdmApi;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Reads the server and enrolled device number from the existing Headwind launcher. */
public final class MdmLink {
    private static final String ACTION_CONNECT = "com.hmdm.action.Connect";
    private static final String PACKAGE = "com.hmdm.launcher";

    public static final class Config {
        public final String baseUrl;
        public final String serverPath;
        public final String deviceNumber;
        Config(String baseUrl, String serverPath, String deviceNumber) {
            this.baseUrl = baseUrl;
            this.serverPath = serverPath;
            this.deviceNumber = deviceNumber;
        }
        public String httpUrl(String path) {
            String base = baseUrl == null ? "" : baseUrl.replaceFirst("/+\\z", "");
            if (serverPath != null && !serverPath.trim().isEmpty()) {
                base += "/" + serverPath.trim().replaceAll("^/+|/+$", "");
            }
            return base + path;
        }

        public String syncUrl() {
            try {
                return httpUrl("/rest/public/sync/configuration/"
                        + java.net.URLEncoder.encode(deviceNumber, "UTF-8"));
            } catch (java.io.UnsupportedEncodingException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }

    private MdmLink() { }

    public static Config query(Context context) throws Exception {
        AtomicReference<Config> value = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        ServiceConnection connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                try {
                    Bundle b = IMdmApi.Stub.asInterface(binder).queryConfig();
                    if (b != null) {
                        String number = b.getString("DEVICE_ID");
                        String host = b.getString("SERVER_HOST");
                        if (number != null && !number.trim().isEmpty()
                                && host != null && !host.trim().isEmpty()) {
                            value.set(new Config(host, b.getString("SERVER_PATH"), number));
                        }
                    }
                } catch (Exception ignored) { }
                latch.countDown();
            }
            @Override public void onServiceDisconnected(ComponentName name) { latch.countDown(); }
            @Override public void onNullBinding(ComponentName name) { latch.countDown(); }
        };
        Intent intent = new Intent(ACTION_CONNECT).setPackage(PACKAGE);
        boolean bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        if (!bound) return null;
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) return null;
            return value.get();
        } finally {
            try { context.unbindService(connection); } catch (Exception ignored) { }
        }
    }
}
