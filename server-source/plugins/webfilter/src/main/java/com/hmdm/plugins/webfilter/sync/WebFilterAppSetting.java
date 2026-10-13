package com.hmdm.plugins.webfilter.sync;

import com.hmdm.rest.json.SyncApplicationSettingInt;

/**
 * <p>A launcher application setting produced by the web filter (<code>locked_packages</code> /
 * <code>unlocked_packages</code> of <code>com.hmdm.launcher</code>).</p>
 */
public class WebFilterAppSetting implements SyncApplicationSettingInt {

    private final String packageId;
    private final String name;
    private final String value;
    private final long lastUpdate;

    public WebFilterAppSetting(String packageId, String name, String value, long lastUpdate) {
        this.packageId = packageId;
        this.name = name;
        this.value = value;
        this.lastUpdate = lastUpdate;
    }

    @Override public String getPackageId() { return packageId; }
    @Override public String getName() { return name; }
    @Override public int getType() { return 1; }
    @Override public String getValue() { return value; }
    @Override public Boolean isReadonly() { return Boolean.FALSE; }
    @Override public long getLastUpdate() { return lastUpdate; }
    @Override public Boolean isVariable() { return Boolean.FALSE; }
}
