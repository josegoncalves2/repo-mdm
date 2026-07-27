/*
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Copyright (C) 2019 Headwind Solutions LLC (http://h-sms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hmdm.launcher.pro;

import android.app.ActivityManager;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.location.Location;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.hmdm.launcher.AdminReceiver;
import com.hmdm.launcher.Const;
import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.json.ServerConfig;
import com.hmdm.launcher.policy.KioskPolicy;
import com.hmdm.launcher.ui.MainActivity;
import com.hmdm.launcher.util.RemoteLogger;
import com.hmdm.launcher.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

/**
 * These functions are available in Pro-version only
 * In a free version, the class contains stubs
 */
public class ProUtils {

    public static boolean isPro() {
        return false;
    }

    public static boolean kioskModeRequired(Context context) {
        try {
            ServerConfig config = SettingsHelper.getInstance(context.getApplicationContext()).getConfig();
            return config != null && config.isKioskMode();
        } catch (Exception e) {
            Log.w(Const.LOG_TAG, "Failed to check kiosk mode requirement", e);
            return false;
        }
    }

    public static void initCrashlytics(Context context) {
        // Stub
    }

    public static void sendExceptionToCrashlytics(Throwable e) {
        // Stub
    }

    // Start the service checking if the foreground app is allowed to the user (by usage statistics)
    public static boolean checkAccessibilityService(Context context) {
        // Stub
        return true;
    }

    // Pro-version
    public static boolean checkUsageStatistics(Context context) {
        // Stub
        return true;
    }

    // Add a transparent view on top of the status bar which prevents user interaction with the status bar
    public static View preventStatusBarExpansion(Activity activity) {
        return addBlockingOverlay(activity, getStatusBarHeight(activity), Gravity.TOP, "status bar");
    }

    // Add a transparent view on top of a swipeable area at the right (opens app list on Samsung tablets)
    public static View preventApplicationsList(Activity activity) {
        // Samsung tablets open the app list by swiping from the right edge, so the strip is placed there.
        // The width matches the status bar height, which scales with the screen density.
        return addBlockingOverlay(activity, getStatusBarHeight(activity), Gravity.END, "app list gesture");
    }

    private static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return (int) (25 * context.getResources().getDisplayMetrics().density);
    }

    // Adds a transparent, touch-absorbing overlay pinned to one edge of the screen.
    // Requires the overlay permission; returns null when it is not granted so the caller can fall back.
    private static View addBlockingOverlay(Activity activity, int size, int gravity, String what) {
        if (!Utils.canDrawOverlays(activity)) {
            RemoteLogger.log(activity, Const.LOG_WARN,
                    "Cannot block " + what + ": overlay permission is not granted");
            return null;
        }
        try {
            WindowManager manager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            if (manager == null) {
                return null;
            }

            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

            boolean horizontal = gravity == Gravity.TOP || gravity == Gravity.BOTTOM;
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    horizontal ? WindowManager.LayoutParams.MATCH_PARENT : size,
                    horizontal ? size : WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            params.gravity = gravity;

            View blocker = new View(activity);
            // Swallow every touch reaching the strip so the gesture never gets to the system.
            blocker.setOnTouchListener((v, event) -> true);

            manager.addView(blocker, params);
            return blocker;
        } catch (Exception e) {
            RemoteLogger.log(activity, Const.LOG_WARN, "Failed to block " + what + ": " + e.getMessage());
            return null;
        }
    }

    public static View createKioskUnlockButton(Activity activity) {
        TextView button = new TextView(activity);
        button.setText("");
        button.setContentDescription("Kiosk unlock");
        button.setWidth(96);
        button.setHeight(96);
        button.setAlpha(0.01f);
        return button;
    }

    public static boolean isKioskAppInstalled(Context context) {
        try {
            ServerConfig config = SettingsHelper.getInstance(context.getApplicationContext()).getConfig();
            if (config == null || config.getMainApp() == null || config.getMainApp().trim().isEmpty()) {
                return true;
            }
            return context.getPackageManager().getLaunchIntentForPackage(config.getMainApp()) != null;
        } catch (Exception e) {
            Log.w(Const.LOG_TAG, "Failed to check kiosk app installation", e);
            return false;
        }
    }

    public static boolean isKioskModeRunning(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            return am != null && am.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE;
        } catch (Exception e) {
            Log.w(Const.LOG_TAG, "Failed to check lock task state", e);
            return false;
        }
    }

    public static Intent getKioskAppIntent(String kioskApp, Activity activity) {
        try {
            kioskApp = KioskPolicy.resolveMainApp(kioskApp, activity.getPackageName());
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage(kioskApp.trim());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
            return intent;
        } catch (Exception e) {
            Log.w(Const.LOG_TAG, "Failed to build kiosk app intent for " + kioskApp, e);
            return null;
        }
    }

    // Start COSU kiosk mode
    public static boolean startCosuKioskMode(String kioskApp, Activity activity, boolean enableSettings) {
        try {
            if (!isDeviceOwner(activity)) {
                RemoteLogger.log(activity, Const.LOG_WARN, "Kiosk mode requires Device Owner provisioning.");
                return false;
            }
            kioskApp = KioskPolicy.resolveMainApp(kioskApp, activity.getPackageName());
            Intent intent = getKioskAppIntent(kioskApp, activity);
            if (intent == null) {
                RemoteLogger.log(activity, Const.LOG_WARN, "Kiosk app is not installed or has no launcher activity: " + kioskApp);
                return false;
            }
            updateKioskAllowedApps(kioskApp, activity, enableSettings);
            updateKioskOptions(activity);
            if (!isKioskModeRunning(activity)) {
                activity.startLockTask();
            }
            if (!activity.getPackageName().equals(kioskApp)) {
                activity.startActivity(intent);
            }
            RemoteLogger.log(activity, Const.LOG_INFO, "Kiosk mode started for package: " + kioskApp);
            return true;
        } catch (Exception e) {
            Log.e(Const.LOG_TAG, "Failed to start kiosk mode", e);
            RemoteLogger.log(activity, Const.LOG_ERROR, "Kiosk mode failed: " + e.getMessage());
            return false;
        }
    }

    // Set/update kiosk mode options (lock tack features)
    public static void updateKioskOptions(Context activity) {
        if (!isDeviceOwner(activity)) {
            return;
        }
        try {
            ServerConfig config = SettingsHelper.getInstance(activity.getApplicationContext()).getConfig();
            DevicePolicyManager dpm = getDevicePolicyManager(activity);
            ComponentName admin = getAdminComponent(activity);
            if (dpm == null || config == null) {
                return;
            }
            // The server "restrictions" payload carries three entries that are not UserManager keys.
            // They are enforced here, and they always win over the kiosk* toggles (most restrictive wins).
            Set<String> restrictions = KioskPolicy.parseRestrictions(config.getRestrictions());
            boolean noStatusBar = restrictions.contains(KioskPolicy.RESTRICTION_STATUS_BAR);
            boolean noRecents = restrictions.contains(KioskPolicy.RESTRICTION_RECENT_APPS);
            boolean noNotifications = restrictions.contains(KioskPolicy.RESTRICTION_NOTIFICATIONS);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean disableStatusBar = noStatusBar
                        || config.getKioskNotifications() == null || !config.getKioskNotifications();
                dpm.setStatusBarDisabled(admin, disableStatusBar);
                RemoteLogger.log(activity, Const.LOG_INFO, "setStatusBarDisabled(" + disableStatusBar + ")" +
                        (noStatusBar ? " (forced by no_status_bar)" : ""));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                int features = 0;
                if (Boolean.TRUE.equals(config.getKioskHome())) {
                    features |= DevicePolicyManager.LOCK_TASK_FEATURE_HOME;
                }
                if (Boolean.TRUE.equals(config.getKioskRecents()) && !noRecents) {
                    features |= DevicePolicyManager.LOCK_TASK_FEATURE_OVERVIEW;
                }
                if (Boolean.TRUE.equals(config.getKioskNotifications()) && !noNotifications) {
                    features |= DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS;
                }
                if (Boolean.TRUE.equals(config.getKioskSystemInfo()) && !noStatusBar) {
                    features |= DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO;
                }
                if (Boolean.TRUE.equals(config.getKioskKeyguard())) {
                    features |= DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD;
                }
                dpm.setLockTaskFeatures(admin, features);
                RemoteLogger.log(activity, Const.LOG_INFO, "setLockTaskFeatures(0x" +
                        Integer.toHexString(features) + ") home=" + ((features & DevicePolicyManager.LOCK_TASK_FEATURE_HOME) != 0) +
                        " overview=" + ((features & DevicePolicyManager.LOCK_TASK_FEATURE_OVERVIEW) != 0) +
                        " notifications=" + ((features & DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS) != 0) +
                        " systemInfo=" + ((features & DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO) != 0) +
                        " keyguard=" + ((features & DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD) != 0));
            }
        } catch (Exception e) {
            Log.w(Const.LOG_TAG, "Failed to update kiosk lock task options", e);
            RemoteLogger.log(activity, Const.LOG_WARN, "Failed to update kiosk lock task options: " + e.getMessage());
        }
    }

    // Update app list in the kiosk mode
    public static void updateKioskAllowedApps(String kioskApp, Context activity, boolean enableSettings) {
        if (!isDeviceOwner(activity)) {
            return;
        }
        try {
            DevicePolicyManager dpm = getDevicePolicyManager(activity);
            if (dpm == null) {
                return;
            }
            List<String> packages = new ArrayList<>();
            addInstalledPackage(activity, packages, activity.getPackageName());
            addInstalledPackage(activity, packages, KioskPolicy.resolveMainApp(kioskApp, activity.getPackageName()));
            addInstalledPackage(activity, packages, Const.KIOSK_BROWSER_PACKAGE_NAME);
            addInstalledPackage(activity, packages, Const.APUPPET_PACKAGE_NAME);
            ServerConfig config = SettingsHelper.getInstance(activity.getApplicationContext()).getConfig();
            boolean showWifi = config != null && config.isShowWifi();
            if (KioskPolicy.shouldAllowSettingsInLockTask(enableSettings, showWifi)) {
                addInstalledPackage(activity, packages, Const.SETTINGS_PACKAGE_NAME);
            }

            // Allowlist coming from the server payload: an app is permitted inside lock task only if
            // the server marked it useKiosk (explicit kiosk allowance) or showIcon (reachable from the
            // launcher desktop). Everything else stays blocked even if installed.
            List<String> fromServer = KioskPolicy.collectAllowedPackages(config);
            for (String pkg : fromServer) {
                addInstalledPackage(activity, packages, pkg);
            }

            dpm.setLockTaskPackages(getAdminComponent(activity), packages.toArray(new String[0]));
            RemoteLogger.log(activity, Const.LOG_INFO, "Kiosk allowlist (" + packages.size() + " pkg): " +
                    android.text.TextUtils.join(",", packages));

            // Report server-configured packages that could not be allowlisted because they are absent
            List<String> missing = new ArrayList<>();
            for (String pkg : fromServer) {
                if (!packages.contains(pkg)) {
                    missing.add(pkg);
                }
            }
            if (!missing.isEmpty()) {
                RemoteLogger.log(activity, Const.LOG_WARN, "Kiosk allowlist: server-configured packages not installed: " +
                        android.text.TextUtils.join(",", missing));
            }
        } catch (Exception e) {
            Log.w(Const.LOG_TAG, "Failed to update kiosk allowed packages", e);
            RemoteLogger.log(activity, Const.LOG_ERROR, "Failed to update kiosk allowlist: " + e.getMessage());
        }
    }

    /**
     * Re-arm every device-owner policy that does NOT require a foreground Activity, using the
     * configuration cached on disk. Called from BootReceiver before any user-visible UI is started,
     * so the device is never briefly unmanaged after a reboot.
     *
     * startLockTask() itself needs an Activity and is therefore still performed by MainActivity,
     * but by the time it runs the allowlist, restrictions, status bar and uninstall protection are
     * already enforced.
     */
    public static void rearmKioskPoliciesAtBoot(Context context) {
        if (!isDeviceOwner(context)) {
            RemoteLogger.log(context, Const.LOG_WARN, "Boot re-arm skipped: not the Device Owner");
            return;
        }
        ServerConfig config = SettingsHelper.getInstance(context.getApplicationContext()).getConfig();
        if (config == null) {
            RemoteLogger.log(context, Const.LOG_WARN, "Boot re-arm skipped: no cached configuration");
            return;
        }
        if (!config.isKioskMode()) {
            Log.i(Const.LOG_TAG, "Boot re-arm: kiosk mode is off in the cached config, nothing to enforce");
            return;
        }

        RemoteLogger.log(context, Const.LOG_INFO, "Boot re-arm: enforcing kiosk policies before UI");

        String kioskApp = KioskPolicy.resolveMainApp(config.getMainApp(), context.getPackageName());
        // 1. Lock task allowlist (setLockTaskPackages)
        updateKioskAllowedApps(kioskApp, context, false);
        // 2. Status bar + lock task features (setStatusBarDisabled / setLockTaskFeatures)
        updateKioskOptions(context);
        // 3. User restrictions from the server payload, one by one, with per-item logging
        if (config.getRestrictions() != null && !config.getRestrictions().trim().isEmpty()) {
            Utils.lockUserRestrictions(context, config.getRestrictions());
        }
        // 4. Self-protection: not uninstallable, not force-stoppable from the device
        Utils.protectFromUninstall(context, true);

        RemoteLogger.log(context, Const.LOG_INFO, "Boot re-arm complete for kiosk app " + kioskApp);
    }

    public static void unlockKiosk(Activity activity) {
        try {
            if (isKioskModeRunning(activity)) {
                activity.stopLockTask();
            }
            if (isDeviceOwner(activity) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                DevicePolicyManager dpm = getDevicePolicyManager(activity);
                if (dpm != null) {
                    dpm.setStatusBarDisabled(getAdminComponent(activity), false);
                }
            }
            RemoteLogger.log(activity, Const.LOG_INFO, "Kiosk mode stopped.");
        } catch (Exception e) {
            Log.w(Const.LOG_TAG, "Failed to unlock kiosk mode", e);
        }
    }

    public static void processConfig(Context context, ServerConfig config) {
        if (config == null) {
            RemoteLogger.log(context, Const.LOG_WARN, "Kiosk config processing skipped: no configuration");
            return;
        }

        if (!config.isKioskMode()) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Const.ACTION_EXIT_KIOSK));
            return;
        }

        if (!isDeviceOwner(context)) {
            RemoteLogger.log(context, Const.LOG_WARN, "Kiosk config received but cannot be enforced: not Device Owner");
            return;
        }

        String kioskApp = KioskPolicy.resolveMainApp(config.getMainApp(), context.getPackageName());
        RemoteLogger.log(context, Const.LOG_INFO, "Kiosk config received: re-arming policies for " + kioskApp);
        updateKioskAllowedApps(kioskApp, context, false);
        updateKioskOptions(context);
        if (config.getRestrictions() != null && !config.getRestrictions().trim().isEmpty()) {
            Utils.lockUserRestrictions(context, config.getRestrictions());
        }
        Utils.protectFromUninstall(context, true);

        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Const.ACTION_LOCK_KIOSK));

        try {
            Intent launcherIntent = new Intent(context, MainActivity.class);
            launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(launcherIntent);
        } catch (Exception e) {
            RemoteLogger.log(context, Const.LOG_WARN, "Kiosk config applied but launcher could not be brought to front: " + e.getMessage());
        }
    }

    public static void processLocation(Context context, Location location, String provider) {
        // Stub    
    }

    public static String getAppName(Context context) {
        return context.getString(R.string.app_name);
    }

    public static String getCopyright(Context context) {
        return "(c) " + Calendar.getInstance().get(Calendar.YEAR) + " " + context.getString(R.string.vendor);
    }

    private static boolean isDeviceOwner(Context context) {
        try {
            DevicePolicyManager dpm = getDevicePolicyManager(context);
            return dpm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    dpm.isDeviceOwnerApp(context.getPackageName());
        } catch (Exception e) {
            Log.w(Const.LOG_TAG, "Failed to verify Device Owner state", e);
            return false;
        }
    }

    private static DevicePolicyManager getDevicePolicyManager(Context context) {
        return (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    private static ComponentName getAdminComponent(Context context) {
        return new ComponentName(context.getApplicationContext(), AdminReceiver.class);
    }

    private static void addInstalledPackage(Context context, List<String> packages, String packageName) {
        if (packageName == null || packageName.trim().isEmpty() || packages.contains(packageName)) {
            return;
        }
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            packages.add(packageName);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
    }
}
