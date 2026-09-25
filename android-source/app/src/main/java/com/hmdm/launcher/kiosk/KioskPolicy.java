package com.hmdm.launcher.kiosk;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.WindowManager;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.json.Application;
import com.hmdm.launcher.json.ServerConfig;
import com.hmdm.launcher.util.LegacyUtils;
import com.hmdm.launcher.util.RemoteLogger;
import com.hmdm.launcher.Const;
import java.util.LinkedHashSet;
import java.util.Set;

/** Android Enterprise kiosk policy, independent of optional commercial integrations. */
public final class KioskPolicy {
    private KioskPolicy() { }
    public static ServerConfig config(Context context) { return SettingsHelper.getInstance(context).getConfig(); }
    public static boolean required(Context context) {
        ServerConfig config = config(context);
        return config != null && config.isKioskMode();
    }
    public static boolean running(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return manager != null && manager.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_LOCKED;
    }
    private static DevicePolicyManager owner(Context context) {
        DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (manager == null || !manager.isDeviceOwnerApp(context.getPackageName())) {
            throw new IllegalStateException("O quiosque exige launcher provisionado como Device Owner");
        }
        return manager;
    }
    public static Intent intent(String pkg, Context context) {
        return pkg == null ? null : context.getPackageManager().getLaunchIntentForPackage(pkg);
    }
    public static void packages(String main, Activity activity, boolean settings) {
        DevicePolicyManager manager = owner(activity);
        Set<String> packages = new LinkedHashSet<>();
        packages.add(activity.getPackageName());
        packages.add("com.hwmdm.remote");
        if (main != null) { packages.add(main); }
        if (settings) { packages.add("com.android.settings"); }
        ServerConfig config = config(activity);
        if (config != null && config.getApplications() != null) {
            for (Application app : config.getApplications()) {
                if (app.isUseKiosk() && app.getPkg() != null) { packages.add(app.getPkg()); }
            }
        }
        manager.setLockTaskPackages(LegacyUtils.getAdminComponentName(activity), packages.toArray(new String[0]));
    }
    public static void options(Activity activity) {
        DevicePolicyManager manager = owner(activity);
        ComponentName admin = LegacyUtils.getAdminComponentName(activity);
        ServerConfig config = config(activity);
        if (config == null) { return; }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int features = DevicePolicyManager.LOCK_TASK_FEATURE_NONE;
            boolean home = Boolean.TRUE.equals(config.getKioskHome());
            boolean recents = Boolean.TRUE.equals(config.getKioskRecents());
            boolean notifications = Boolean.TRUE.equals(config.getKioskNotifications());
            if (home || recents || notifications) { features |= DevicePolicyManager.LOCK_TASK_FEATURE_HOME; }
            if (recents) { features |= DevicePolicyManager.LOCK_TASK_FEATURE_OVERVIEW; }
            if (notifications) { features |= DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS; }
            if (Boolean.TRUE.equals(config.getKioskSystemInfo())) { features |= DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO; }
            if (Boolean.TRUE.equals(config.getKioskKeyguard())) { features |= DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD; }
            if (!Boolean.TRUE.equals(config.getKioskLockButtons())) { features |= DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS; }
            manager.setLockTaskFeatures(admin, features);
        }
        if (Boolean.TRUE.equals(config.getKioskScreenOn())) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else { activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); }
    }
    public static boolean start(String main, Activity activity, boolean settings) {
        try {
            Intent intent = intent(main, activity);
            if (intent == null) { throw new IllegalStateException("Aplicativo inicial não instalado: " + main); }
            packages(main, activity, settings);
            options(activity);
            if (!running(activity)) { activity.startLockTask(); }
            if (!activity.getPackageName().equals(main)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ActivityOptions opts = ActivityOptions.makeBasic();
                    opts.setLockTaskEnabled(true);
                    activity.startActivity(intent, opts.toBundle());
                } else { activity.startActivity(intent); }
            }
            return running(activity);
        } catch (RuntimeException e) {
            RemoteLogger.log(activity, Const.LOG_ERROR, "Falha ao aplicar quiosque: " + e.getMessage());
            return false;
        }
    }
    public static void stop(Activity activity) {
        try {
            // Revoking the allowlist also exits a task started in a different allowed app.
            owner(activity).setLockTaskPackages(LegacyUtils.getAdminComponentName(activity), new String[0]);
            if (running(activity)) { activity.stopLockTask(); }
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (RuntimeException e) { RemoteLogger.log(activity, Const.LOG_ERROR, "Falha ao sair do quiosque: " + e.getMessage()); }
    }
}
