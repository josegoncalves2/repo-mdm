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

import android.app.Activity;
import com.hmdm.launcher.kiosk.KioskPolicy;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.view.View;

import com.hmdm.launcher.R;
import com.hmdm.launcher.json.ServerConfig;

import java.util.Calendar;

/**
 * These functions are available in Pro-version only
 */
public class ProUtils {

    public static boolean isPro() {
        return false;
    }

    public static boolean kioskModeRequired(Context context) {
        return KioskPolicy.required(context);
    }

    public static void initCrashlytics(Context context) {
    }

    public static void sendExceptionToCrashlytics(Throwable e) {
    }

    // Start the service checking if the foreground app is allowed to the user (by usage statistics)
    public static boolean checkAccessibilityService(Context context) {
        return true;
    }

    // Pro-version
    public static boolean checkUsageStatistics(Context context) {
        return true;
    }

    // Add a transparent view on top of the status bar which prevents user interaction with the status bar
    public static View preventStatusBarExpansion(Activity activity) {
        return null;
    }

    // Add a transparent view on top of a swipeable area at the right (opens app list on Samsung tablets)
    public static View preventApplicationsList(Activity activity) {
        return null;
    }

    public static View createKioskUnlockButton(Activity activity) {
        return null;
    }

    public static boolean isKioskAppInstalled(Context context) {
        return KioskPolicy.config(context) != null && KioskPolicy.intent(KioskPolicy.config(context).getMainApp(), context) != null;
    }

    public static boolean isKioskModeRunning(Context context) {
        return KioskPolicy.running(context);
    }

    public static Intent getKioskAppIntent(String kioskApp, Activity activity) {
        return KioskPolicy.intent(kioskApp, activity);
    }

    // Start COSU kiosk mode
    public static boolean startCosuKioskMode(String kioskApp, Activity activity, boolean enableSettings) {
        return KioskPolicy.start(kioskApp, activity, enableSettings);
    }

    // Set/update kiosk mode options (lock tack features)
    public static void updateKioskOptions(Activity activity) {
        KioskPolicy.options(activity);
    }

    // Update app list in the kiosk mode
    public static void updateKioskAllowedApps(String kioskApp, Activity activity, boolean enableSettings) {
        KioskPolicy.packages(kioskApp, activity, enableSettings);
    }

    public static void unlockKiosk(Activity activity) {
        KioskPolicy.stop(activity);
    }

    public static void processConfig(Context context, ServerConfig config) {
    }

    public static void processLocation(Context context, Location location, String provider) {
    }

    public static String getAppName(Context context) {
        return context.getString(R.string.app_name);
    }

    public static String getCopyright(Context context) {
        return "(c) " + Calendar.getInstance().get(Calendar.YEAR) + " " + context.getString(R.string.vendor);
    }
}
