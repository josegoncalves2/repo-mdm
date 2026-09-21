package com.hmdm.launcher.helper;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.UserManager;

import com.hmdm.launcher.Const;
import com.hmdm.launcher.json.ServerConfig;
import com.hmdm.launcher.util.LegacyUtils;
import com.hmdm.launcher.util.RemoteLogger;
import com.hmdm.launcher.util.Utils;

/**
 * Web Filter: applies the private DNS hostname received in the sync (design D10). The DPM call checks the host over the
 * network, so it always runs on a background thread. A host is only recorded as applied after the DPM accepted it;
 * a refused host is retried on the next sync.
 */
public final class PrivateDnsManager {

    private static final String PREFS = "webfilter_private_dns";
    private static final String KEY_APPLIED = "applied_host";
    private static final String KEY_UNSUPPORTED_LOGGED = "unsupported_logged_host";

    private PrivateDnsManager() {
    }

    public static void applyAsync(final Context context, final ServerConfig config) {
        if (config == null) {
            return;
        }
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                apply(app, config);
            } catch (Exception e) {
                RemoteLogger.log(app, Const.LOG_ERROR, "Web Filter: private DNS failed: " + e.getMessage());
            }
        }, "webfilter-private-dns").start();
    }

    private static synchronized void apply(Context context, ServerConfig config) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String applied = prefs.getString(KEY_APPLIED, null);
        PrivateDnsDecision d = PrivateDnsDecision.decide(Build.VERSION.SDK_INT, Utils.isDeviceOwner(context),
                config.getWebfilterDnsHost(), applied, config.getRestrictions());
        if (d.action == PrivateDnsDecision.Action.NONE) {
            return;
        }
        if (d.action == PrivateDnsDecision.Action.UNSUPPORTED) {
            if (!d.host.equals(prefs.getString(KEY_UNSUPPORTED_LOGGED, null))) {
                RemoteLogger.log(context, Const.LOG_WARN, "Web Filter: private DNS " + d.host
                        + " not applied: requires Android 10+ and device owner");
                prefs.edit().putString(KEY_UNSUPPORTED_LOGGED, d.host).apply();
            }
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = LegacyUtils.getAdminComponentName(context);

        if (d.action == PrivateDnsDecision.Action.APPLY) {
            int result = dpm.setGlobalPrivateDnsModeSpecifiedHost(admin, d.host);
            if (result == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR) {
                dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_PRIVATE_DNS);
                if (!d.host.equals(applied)) {
                    RemoteLogger.log(context, Const.LOG_INFO, "Web Filter: private DNS set to " + d.host);
                }
                prefs.edit().putString(KEY_APPLIED, d.host).apply();
            } else {
                String reason = result == DevicePolicyManager.PRIVATE_DNS_SET_ERROR_HOST_NOT_SERVING
                        ? "host not serving DNS-over-TLS (DNS record or certificate)" : "failure setting (code " + result + ")";
                RemoteLogger.log(context, Const.LOG_ERROR, "Web Filter: private DNS " + d.host + " refused: " + reason
                        + "; will retry on next sync");
            }
            return;
        }

        // REMOVE: filter turned off for this profile
        dpm.setGlobalPrivateDnsModeOpportunistic(admin);
        if (d.releaseRestriction) {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_PRIVATE_DNS);
        }
        prefs.edit().remove(KEY_APPLIED).apply();
        RemoteLogger.log(context, Const.LOG_INFO, "Web Filter: private DNS back to automatic");
    }
}
