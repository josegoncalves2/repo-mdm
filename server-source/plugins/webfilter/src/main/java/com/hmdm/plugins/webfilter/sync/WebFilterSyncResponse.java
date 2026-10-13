package com.hmdm.plugins.webfilter.sync;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hmdm.rest.json.SyncApplicationInt;
import com.hmdm.rest.json.SyncApplicationSettingInt;
import com.hmdm.rest.json.SyncConfigurationFileInt;
import com.hmdm.rest.json.SyncResponseInt;

import java.util.List;

/**
 * <p>A sync response extended by the web filter (design D9). It is serialized from a JSON tree built from the original
 * response, so every field of the original is kept, including those not declared by {@link SyncResponseInt}; both
 * the REST layer and the response signature serialize this same tree. The getters reflect the merged values.</p>
 *
 * <p>The delegating getters were generated from {@link SyncResponseInt}; regenerate them when the interface changes.</p>
 */
public class WebFilterSyncResponse implements SyncResponseInt {

    private final SyncResponseInt original;
    private final ObjectNode tree;
    private final List<SyncApplicationSettingInt> applicationSettings;

    public WebFilterSyncResponse(SyncResponseInt original, ObjectNode tree,
                                 List<SyncApplicationSettingInt> applicationSettings) {
        this.original = original;
        this.tree = tree;
        this.applicationSettings = applicationSettings;
    }

    @JsonValue
    public ObjectNode toJson() {
        return tree;
    }

    @Override
    @JsonIgnore
    public String getBackgroundColor() {
        return original.getBackgroundColor();
    }

    @Override
    @JsonIgnore
    public String getTextColor() {
        return original.getTextColor();
    }

    @Override
    @JsonIgnore
    public String getBackgroundImageUrl() {
        return original.getBackgroundImageUrl();
    }

    @Override
    @JsonIgnore
    public List<SyncApplicationInt> getApplications() {
        return original.getApplications();
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return original.getPassword();
    }

    @Override
    @JsonIgnore
    public String getImei() {
        return original.getImei();
    }

    @Override
    @JsonIgnore
    public String getPhone() {
        return original.getPhone();
    }

    @Override
    @JsonIgnore
    public String getIconSize() {
        return original.getIconSize();
    }

    @Override
    @JsonIgnore
    public String getTitle() {
        return original.getTitle();
    }

    @Override
    @JsonIgnore
    public Boolean getGps() {
        return original.getGps();
    }

    @Override
    @JsonIgnore
    public Boolean getBluetooth() {
        return original.getBluetooth();
    }

    @Override
    @JsonIgnore
    public Boolean getWifi() {
        return original.getWifi();
    }

    @Override
    @JsonIgnore
    public Boolean getMobileData() {
        return original.getMobileData();
    }

    @Override
    @JsonIgnore
    public boolean isKioskMode() {
        return original.isKioskMode();
    }

    @Override
    @JsonIgnore
    public Boolean getKioskHome() {
        return original.getKioskHome();
    }

    @Override
    @JsonIgnore
    public Boolean getKioskRecents() {
        return original.getKioskRecents();
    }

    @Override
    @JsonIgnore
    public Boolean getKioskNotifications() {
        return original.getKioskNotifications();
    }

    @Override
    @JsonIgnore
    public Boolean getKioskSystemInfo() {
        return original.getKioskSystemInfo();
    }

    @Override
    @JsonIgnore
    public Boolean getKioskKeyguard() {
        return original.getKioskKeyguard();
    }

    @Override
    @JsonIgnore
    public Boolean getKioskLockButtons() {
        return original.getKioskLockButtons();
    }

    @Override
    @JsonIgnore
    public Boolean getKioskScreenOn() {
        return original.getKioskScreenOn();
    }

    @Override
    @JsonIgnore
    public String getMainApp() {
        return original.getMainApp();
    }

    @Override
    @JsonIgnore
    public boolean isLockStatusBar() {
        return original.isLockStatusBar();
    }

    @Override
    @JsonIgnore
    public int getSystemUpdateType() {
        return original.getSystemUpdateType();
    }

    @Override
    @JsonIgnore
    public String getSystemUpdateFrom() {
        return original.getSystemUpdateFrom();
    }

    @Override
    @JsonIgnore
    public String getSystemUpdateTo() {
        return original.getSystemUpdateTo();
    }

    @Override
    @JsonIgnore
    public Boolean getScheduleAppUpdate() {
        return original.getScheduleAppUpdate();
    }

    @Override
    @JsonIgnore
    public String getAppUpdateFrom() {
        return original.getAppUpdateFrom();
    }

    @Override
    @JsonIgnore
    public String getAppUpdateTo() {
        return original.getAppUpdateTo();
    }

    @Override
    @JsonIgnore
    public String getDownloadUpdates() {
        return original.getDownloadUpdates();
    }

    @Override
    @JsonIgnore
    public List<SyncApplicationSettingInt> getApplicationSettings() {
        return applicationSettings;
    }

    @Override
    @JsonIgnore
    public Boolean getUsbStorage() {
        return original.getUsbStorage();
    }

    @Override
    @JsonIgnore
    public String getRequestUpdates() {
        return original.getRequestUpdates();
    }

    @Override
    @JsonIgnore
    public Boolean getDisableLocation() {
        return original.getDisableLocation();
    }

    @Override
    @JsonIgnore
    public String getAppPermissions() {
        return original.getAppPermissions();
    }

    @Override
    @JsonIgnore
    public String getPushOptions() {
        return original.getPushOptions();
    }

    @Override
    @JsonIgnore
    public Integer getKeepaliveTime() {
        return original.getKeepaliveTime();
    }

    @Override
    @JsonIgnore
    public Boolean getAutoBrightness() {
        return original.getAutoBrightness();
    }

    @Override
    @JsonIgnore
    public Integer getBrightness() {
        return original.getBrightness();
    }

    @Override
    @JsonIgnore
    public Boolean getManageTimeout() {
        return original.getManageTimeout();
    }

    @Override
    @JsonIgnore
    public Integer getTimeout() {
        return original.getTimeout();
    }

    @Override
    @JsonIgnore
    public Boolean getLockVolume() {
        return original.getLockVolume();
    }

    @Override
    @JsonIgnore
    public Boolean getManageVolume() {
        return original.getManageVolume();
    }

    @Override
    @JsonIgnore
    public Integer getVolume() {
        return original.getVolume();
    }

    @Override
    @JsonIgnore
    public String getPasswordMode() {
        return original.getPasswordMode();
    }

    @Override
    @JsonIgnore
    public Integer getOrientation() {
        return original.getOrientation();
    }

    @Override
    @JsonIgnore
    public Boolean getDisplayStatus() {
        return original.getDisplayStatus();
    }

    @Override
    @JsonIgnore
    public Boolean getRunDefaultLauncher() {
        return original.getRunDefaultLauncher();
    }

    @Override
    @JsonIgnore
    public Boolean getDisableScreenshots() {
        return original.getDisableScreenshots();
    }

    @Override
    @JsonIgnore
    public Boolean getAutostartForeground() {
        return original.getAutostartForeground();
    }

    @Override
    @JsonIgnore
    public String getTimeZone() {
        return original.getTimeZone();
    }

    @Override
    @JsonIgnore
    public String getAllowedClasses() {
        return original.getAllowedClasses();
    }

    @Override
    @JsonIgnore
    public String getNewServerUrl() {
        return original.getNewServerUrl();
    }

    @Override
    @JsonIgnore
    public Boolean getLockSafeSettings() {
        return original.getLockSafeSettings();
    }

    @Override
    @JsonIgnore
    public Boolean getFactoryReset() {
        return original.getFactoryReset();
    }

    @Override
    @JsonIgnore
    public Boolean getPermissive() {
        return original.getPermissive();
    }

    @Override
    @JsonIgnore
    public Boolean getKioskExit() {
        return original.getKioskExit();
    }

    @Override
    @JsonIgnore
    public Boolean getShowWifi() {
        return original.getShowWifi();
    }

    @Override
    @JsonIgnore
    public List<SyncConfigurationFileInt> getFiles() {
        return original.getFiles();
    }

    @Override
    @JsonIgnore
    public String getNewNumber() {
        return original.getNewNumber();
    }

    @Override
    @JsonIgnore
    public String getRestrictions() {
        return original.getRestrictions();
    }

    @Override
    @JsonIgnore
    public String getCustom1() {
        return original.getCustom1();
    }

    @Override
    @JsonIgnore
    public String getCustom2() {
        return original.getCustom2();
    }

    @Override
    @JsonIgnore
    public String getCustom3() {
        return original.getCustom3();
    }

    @Override
    @JsonIgnore
    public String getAppName() {
        return original.getAppName();
    }

    @Override
    @JsonIgnore
    public String getVendor() {
        return original.getVendor();
    }

    @Override
    @JsonIgnore
    public String getDescription() {
        return original.getDescription();
    }
}
