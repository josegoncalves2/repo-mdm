package com.hmdm.launcher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hmdm.launcher.Const;
import com.hmdm.launcher.helper.Initializer;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.pro.ProUtils;
import com.hmdm.launcher.ui.MainActivity;
import com.hmdm.launcher.util.RemoteLogger;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(Const.LOG_TAG, "Got the BOOT_RECEIVER broadcast");
        RemoteLogger.log(context, Const.LOG_DEBUG, "Got the BOOT_RECEIVER broadcast");

        SettingsHelper settingsHelper = SettingsHelper.getInstance(context.getApplicationContext());
        if (!settingsHelper.isBaseUrlSet()) {
            // We're here before initializing after the factory reset! Let's ignore this call
            return;
        }

        // Re-arm the kiosk policies FIRST, synchronously, from the cached configuration.
        // This runs before Initializer.init() (which is async and network-dependent) and before any
        // Activity is started, so there is no window in which the device is booted but unrestricted.
        try {
            ProUtils.rearmKioskPoliciesAtBoot(context);
        } catch (Exception e) {
            Log.e(Const.LOG_TAG, "Failed to re-arm kiosk policies at boot", e);
            RemoteLogger.log(context, Const.LOG_ERROR, "Boot re-arm failed: " + e.getMessage());
        }

        long lastAppStartTime = settingsHelper.getAppStartTime();
        long bootTime = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime();
        Log.d(Const.LOG_TAG, "appStartTime=" + lastAppStartTime + ", bootTime=" + bootTime);
        if (lastAppStartTime < bootTime) {
            Log.i(Const.LOG_TAG, "Headwind MDM wasn't started since boot, start initializing services");
        } else {
            Log.i(Const.LOG_TAG, "Headwind MDM is already started, ignoring BootReceiver");
            return;
        }

        Initializer.init(context, () -> {
            Initializer.startServicesAndLoadConfig(context);

            SettingsHelper.getInstance(context).setMainActivityRunning(false);
            if (ProUtils.kioskModeRequired(context)) {
                Log.i(Const.LOG_TAG, "Kiosk mode required, forcing Headwind MDM to run in the foreground");
                Intent launcherIntent = new Intent(context, MainActivity.class);
                launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(launcherIntent);
            }
        });
    }
}
