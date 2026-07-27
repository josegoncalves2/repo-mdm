/*
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.hmdm.launcher.policy;

import com.hmdm.launcher.json.Application;
import com.hmdm.launcher.json.ServerConfig;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KioskPolicyTest {

    private static Application app(String pkg, String type, boolean useKiosk, boolean showIcon, boolean remove) {
        Application a = new Application();
        a.setPkg(pkg);
        a.setType(type);
        a.setUseKiosk(useKiosk);
        a.setShowIcon(showIcon);
        a.setRemove(remove);
        return a;
    }

    private static ServerConfig configWith(Application... apps) {
        ServerConfig config = new ServerConfig();
        config.setApplications(new ArrayList<>(Arrays.asList(apps)));
        return config;
    }

    /**
     * THE central requirement: app A is permitted inside lock task, app B is not,
     * even though both are declared by the server and both may be installed.
     */
    @Test
    public void permitsAppAAndBlocksAppB() {
        ServerConfig config = configWith(
                app("com.vendor.appA", "app", true, false, false),    // explicitly allowed in kiosk
                app("com.vendor.appB", "app", false, false, false)    // present in the config, NOT allowed
        );

        List<String> allowed = KioskPolicy.collectAllowedPackages(config);

        assertTrue("app A must be allowlisted", allowed.contains("com.vendor.appA"));
        assertFalse("app B must NOT be allowlisted", allowed.contains("com.vendor.appB"));
        assertEquals(1, allowed.size());
    }

    @Test
    public void allowlistsDesktopIconsBecauseTheyMustBeLaunchable() {
        ServerConfig config = configWith(app("com.vendor.desktop", "app", false, true, false));
        assertEquals(java.util.Collections.singletonList("com.vendor.desktop"),
                KioskPolicy.collectAllowedPackages(config));
    }

    @Test
    public void neverAllowlistsAppsScheduledForRemoval() {
        // remove=true wins over useKiosk=true: an app being uninstalled must not be runnable
        ServerConfig config = configWith(app("com.vendor.doomed", "app", true, true, true));
        assertTrue(KioskPolicy.collectAllowedPackages(config).isEmpty());
    }

    @Test
    public void ignoresWebAndIntentShortcutsWhichAreNotPackages() {
        ServerConfig config = configWith(
                app("com.vendor.site", "web", true, true, false),
                app("com.vendor.deeplink", "intent", true, true, false)
        );
        assertTrue(KioskPolicy.collectAllowedPackages(config).isEmpty());
    }

    @Test
    public void treatsNullTypeAsLegacyInstallableApp() {
        ServerConfig config = configWith(app("com.vendor.legacy", null, true, false, false));
        assertEquals(java.util.Collections.singletonList("com.vendor.legacy"),
                KioskPolicy.collectAllowedPackages(config));
    }

    @Test
    public void ignoresBlankPackagesAndDeduplicates() {
        ServerConfig config = configWith(
                app("  ", "app", true, true, false),
                app(null, "app", true, true, false),
                app("com.vendor.twice", "app", true, false, false),
                app("com.vendor.twice", "app", false, true, false)
        );
        assertEquals(java.util.Collections.singletonList("com.vendor.twice"),
                KioskPolicy.collectAllowedPackages(config));
    }

    /**
     * The restrictions string below is the payload the production server sends to the enrolled
     * SM-T225 tablets. 12 entries are real android.os.UserManager keys; 3 are Headwind-only
     * pseudo-restrictions that addUserRestriction() cannot accept and that must instead be routed
     * to the lock task configuration.
     */
    private static final String PRODUCTION_RESTRICTIONS =
            "no_factory_reset,no_safe_boot,no_add_user,no_modify_accounts,no_control_apps," +
            "no_config_bluetooth,no_config_credentials,no_config_mobile_networks,no_config_tethering," +
            "no_config_vpn,no_config_wifi,no_debugging_features,no_status_bar,no_recent_apps,no_notifications";

    @Test
    public void splitsTheProductionRestrictionsPayloadIntoTwelvePlusThree() {
        java.util.Set<String> parsed = KioskPolicy.parseRestrictions(PRODUCTION_RESTRICTIONS);
        assertEquals(15, parsed.size());

        List<String> userManagerKeys = new ArrayList<>();
        List<String> lockTaskKeys = new ArrayList<>();
        for (String r : parsed) {
            (KioskPolicy.isLockTaskRestriction(r) ? lockTaskKeys : userManagerKeys).add(r);
        }

        assertEquals(12, userManagerKeys.size());
        assertEquals(Arrays.asList("no_status_bar", "no_recent_apps", "no_notifications"), lockTaskKeys);
    }

    @Test
    public void identifiesOnlyTheThreeNonUserManagerRestrictions() {
        assertTrue(KioskPolicy.isLockTaskRestriction("no_status_bar"));
        assertTrue(KioskPolicy.isLockTaskRestriction("no_recent_apps"));
        assertTrue(KioskPolicy.isLockTaskRestriction("no_notifications"));
        assertTrue(KioskPolicy.isLockTaskRestriction("  no_status_bar  "));
        // Real UserManager keys must still go through addUserRestriction()
        assertFalse(KioskPolicy.isLockTaskRestriction("no_factory_reset"));
        assertFalse(KioskPolicy.isLockTaskRestriction("no_safe_boot"));
        assertFalse(KioskPolicy.isLockTaskRestriction(null));
    }

    @Test
    public void parseRestrictionsTrimsBlanksAndKeepsServerOrder() {
        assertEquals(Arrays.asList("no_safe_boot", "no_add_user"),
                new ArrayList<>(KioskPolicy.parseRestrictions(" no_safe_boot , ,no_add_user, ")));
        assertTrue(KioskPolicy.parseRestrictions(null).isEmpty());
        assertTrue(KioskPolicy.parseRestrictions("").isEmpty());
    }

    @Test
    public void survivesMissingApplicationList() {
        assertTrue(KioskPolicy.collectAllowedPackages(null).isEmpty());
        ServerConfig empty = new ServerConfig();
        empty.setApplications(null);
        assertTrue(KioskPolicy.collectAllowedPackages(empty).isEmpty());
    }

    @Test
    public void defaultsKioskTargetToHeadwindWhenNoContentAppIsConfigured() {
        assertEquals("com.hmdm.launcher", KioskPolicy.resolveMainApp("", "com.hmdm.launcher"));
        assertEquals("com.hmdm.launcher", KioskPolicy.resolveMainApp(null, "com.hmdm.launcher"));
    }

    @Test
    public void hidesLauncherContentWhenHeadwindItselfIsTheKioskTarget() {
        assertFalse(KioskPolicy.shouldShowLauncherContent(true, null, "com.hmdm.launcher"));
        assertFalse(KioskPolicy.shouldShowLauncherContent(true, "com.hmdm.launcher", "com.hmdm.launcher"));
    }

    @Test
    public void keepsInventoryAndExternalKioskModesUsable() {
        assertTrue(KioskPolicy.shouldShowLauncherContent(false, null, "com.hmdm.launcher"));
        assertTrue(KioskPolicy.shouldShowLauncherContent(true, "com.vendor.camera", "com.hmdm.launcher"));
    }

    @Test
    public void onlyAllowsSettingsWhenTemporarilyReleasedOrWifiIsExplicitlyAllowed() {
        assertFalse(KioskPolicy.shouldAllowSettingsInLockTask(false, false));
        assertTrue(KioskPolicy.shouldAllowSettingsInLockTask(true, false));
        assertTrue(KioskPolicy.shouldAllowSettingsInLockTask(false, true));
    }
}
