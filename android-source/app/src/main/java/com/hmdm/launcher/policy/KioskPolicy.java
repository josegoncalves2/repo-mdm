/*
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.hmdm.launcher.policy;

import com.hmdm.launcher.json.Application;
import com.hmdm.launcher.json.ServerConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class KioskPolicy {

    /**
     * Restrictions the Headwind server sends in the "restrictions" payload that are NOT
     * android.os.UserManager keys. Verified against android.jar (API 34): UserManager declares
     * no DISALLOW_* constant with these values, so DevicePolicyManager.addUserRestriction()
     * cannot accept them. They are enforced through lock task features / the status bar instead.
     */
    public static final String RESTRICTION_STATUS_BAR = "no_status_bar";
    public static final String RESTRICTION_RECENT_APPS = "no_recent_apps";
    public static final String RESTRICTION_NOTIFICATIONS = "no_notifications";

    private static final Set<String> LOCK_TASK_RESTRICTIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    RESTRICTION_STATUS_BAR,
                    RESTRICTION_RECENT_APPS,
                    RESTRICTION_NOTIFICATIONS)));

    private KioskPolicy() {
    }

    /**
     * True when the restriction is enforced by the kiosk lock task configuration rather than by
     * addUserRestriction(). Passing these to addUserRestriction() only produces a spurious failure.
     */
    public static boolean isLockTaskRestriction(String restriction) {
        return restriction != null && LOCK_TASK_RESTRICTIONS.contains(restriction.trim());
    }

    /**
     * Split the server "restrictions" CSV into trimmed, non-empty, de-duplicated entries,
     * preserving the order in which the server sent them.
     */
    public static Set<String> parseRestrictions(String restrictions) {
        Set<String> result = new LinkedHashSet<>();
        if (restrictions == null) {
            return result;
        }
        for (String item : restrictions.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * Decide whether a single server-declared application is permitted to run inside lock task.
     * Only real installable packages count: "web" and "intent" entries are shortcuts, not packages,
     * and apps flagged for removal must never be allowlisted.
     */
    public static boolean isPackageAllowedInKiosk(Application app) {
        if (app == null) {
            return false;
        }
        String pkg = app.getPkg();
        if (pkg == null || pkg.trim().isEmpty()) {
            return false;
        }
        if (app.isRemove()) {
            return false;
        }
        String type = app.getType();
        // A null type means a legacy "app" entry
        if (type != null && !Application.TYPE_APP.equals(type)) {
            return false;
        }
        // The server decides: useKiosk = explicitly permitted in kiosk,
        // showIcon = reachable from the launcher desktop (so it must be launchable).
        return app.isUseKiosk() || app.isShowIcon();
    }

    /**
     * Build the list of packages the server payload permits inside lock task.
     * Anything not returned here stays blocked, even when installed on the device.
     */
    public static List<String> collectAllowedPackages(ServerConfig config) {
        List<String> result = new ArrayList<>();
        if (config == null || config.getApplications() == null) {
            return result;
        }
        for (Application app : config.getApplications()) {
            if (isPackageAllowedInKiosk(app)) {
                String pkg = app.getPkg().trim();
                if (!result.contains(pkg)) {
                    result.add(pkg);
                }
            }
        }
        return result;
    }

    public static String resolveMainApp(String configuredMainApp, String fallbackPackage) {
        if (configuredMainApp != null && configuredMainApp.trim().length() > 0) {
            return configuredMainApp.trim();
        }
        return fallbackPackage;
    }

    public static boolean shouldShowLauncherContent(boolean kioskMode, String configuredMainApp, String ownPackage) {
        if (!kioskMode) {
            return true;
        }
        return ownPackage.equals(resolveMainApp(configuredMainApp, ownPackage));
    }

    public static boolean shouldAllowSettingsInLockTask(boolean temporarySettingsAccess, boolean showWifi) {
        return temporarySettingsAccess || showWifi;
    }
}
