package com.hwmdm.webfilter.sync;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Bundle;

import com.hwmdm.webfilter.BuildConfig;
import com.hwmdm.webfilter.mdm.MdmLink;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Iterator;

/** Downloads the signed MDM sync payload and applies browser-native managed policies. */
public final class BrowserPolicySync {
    private static final String PREFS = "webfilter-agent";
    private BrowserPolicySync() { }

    public static String refresh(Context context) throws Exception {
        MdmLink.Config mdm = MdmLink.query(context);
        if (mdm == null) throw new IllegalStateException("Aguardando matrícula no MDM 6.36");

        HttpURLConnection connection = (HttpURLConnection) new URL(mdm.syncUrl()).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("X-Request-Signature", sha1(BuildConfig.REQUEST_SIGNATURE + mdm.deviceNumber));
        try {
            int code = connection.getResponseCode();
            if (code != 200) throw new IllegalStateException("Servidor MDM respondeu HTTP " + code);
            String body = readAll(connection.getInputStream());
            verifyResponse(connection.getHeaderField("X-Response-Signature"), body);
            JSONObject envelope = new JSONObject(body);
            if (!"OK".equals(envelope.optString("status"))) {
                throw new IllegalStateException(envelope.optString("message", "Falha no sync MDM"));
            }
            JSONObject data = envelope.getJSONObject("data");
            JSONObject policies = data.optJSONObject("webfilterBrowserPolicies");
            if (policies == null) throw new IllegalStateException("Servidor ainda não envia políticas ao agente Web Filter");

            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm == null || !dpm.getDelegatedScopes(null, context.getPackageName())
                    .contains(DevicePolicyManager.DELEGATION_APP_RESTRICTIONS)) {
                throw new SecurityException("O MDM ainda não concedeu a delegação de políticas");
            }
            int applied = 0;
            Iterator<String> browsers = policies.keys();
            while (browsers.hasNext()) {
                String browser = browsers.next();
                JSONObject policy = policies.optJSONObject(browser);
                if (policy == null) continue;
                try {
                    context.getPackageManager().getPackageInfo(browser, 0);
                } catch (android.content.pm.PackageManager.NameNotFoundException notInstalled) {
                    continue;
                }
                dpm.setApplicationRestrictions(null, browser, toBundle(policy));
                applied++;
            }
            String status = "Políticas aplicadas a " + applied + " configurações de navegador; aparelho "
                    + mdm.deviceNumber;
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString("status", status).putLong("updated", System.currentTimeMillis()).apply();
            return status;
        } finally {
            connection.disconnect();
        }
    }

    private static Bundle toBundle(JSONObject json) throws JSONException {
        Bundle bundle = new Bundle();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);
            if (value instanceof String) bundle.putString(key, (String) value);
            else if (value instanceof Boolean) bundle.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer) bundle.putInt(key, (Integer) value);
            else if (value instanceof Long) bundle.putLong(key, (Long) value);
            else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                String[] strings = new String[array.length()];
                for (int i = 0; i < array.length(); i++) strings[i] = array.getString(i);
                bundle.putStringArray(key, strings);
            } else if (value instanceof JSONObject) {
                bundle.putBundle(key, toBundle((JSONObject) value));
            }
        }
        return bundle;
    }

    private static void verifyResponse(String signature, String body) throws Exception {
        if (signature == null) throw new SecurityException("Resposta sem assinatura do MDM");
        int marker = body.indexOf("\"data\":");
        if (marker < 0 || !body.endsWith("}")) throw new SecurityException("Resposta de sync inválida");
        String data = body.substring(marker + 7, body.length() - 1).replaceAll("\\s", "");
        String expected = sha1(BuildConfig.REQUEST_SIGNATURE + data);
        if (!MessageDigest.isEqual(signature.toLowerCase().getBytes(StandardCharsets.US_ASCII),
                expected.toLowerCase().getBytes(StandardCharsets.US_ASCII))) {
            throw new SecurityException("Assinatura da política do MDM não confere");
        }
    }

    private static String sha1(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (byte b : digest) out.append(String.format(java.util.Locale.ROOT, "%02X", b & 0xff));
        return out.toString();
    }

    private static String readAll(InputStream input) throws Exception {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
