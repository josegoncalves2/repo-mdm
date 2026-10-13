package com.hmdm.plugins.webfilter.persistence.domain;

import java.io.Serializable;

/**
 * <p>What the web filter handed to a device in its last configuration sync, joined with the device itself for the
 * dashboard. The delivery columns are <code>null</code> for a device that has not synced since the filter exists.</p>
 */
public class WebFilterDelivery implements Serializable {

    private static final long serialVersionUID = 1L;

    private int deviceId;
    private int customerId;
    private int configurationId;
    private String deviceNumber;
    private String configurationName;
    /** Last time the device reported to the server (devices.lastUpdate). */
    private Long lastUpdate;
    private Boolean enabled;
    private Long policyUpdatedAt;
    private Long deliveredAt;
    private Integer browserSites;
    private Integer hiddenApps;

    public int getDeviceId() { return deviceId; }
    public void setDeviceId(int deviceId) { this.deviceId = deviceId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public int getConfigurationId() { return configurationId; }
    public void setConfigurationId(int configurationId) { this.configurationId = configurationId; }
    public String getDeviceNumber() { return deviceNumber; }
    public void setDeviceNumber(String deviceNumber) { this.deviceNumber = deviceNumber; }
    public String getConfigurationName() { return configurationName; }
    public void setConfigurationName(String configurationName) { this.configurationName = configurationName; }
    public Long getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(Long lastUpdate) { this.lastUpdate = lastUpdate; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Long getPolicyUpdatedAt() { return policyUpdatedAt; }
    public void setPolicyUpdatedAt(Long policyUpdatedAt) { this.policyUpdatedAt = policyUpdatedAt; }
    public Long getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Long deliveredAt) { this.deliveredAt = deliveredAt; }
    public Integer getBrowserSites() { return browserSites; }
    public void setBrowserSites(Integer browserSites) { this.browserSites = browserSites; }
    public Integer getHiddenApps() { return hiddenApps; }
    public void setHiddenApps(Integer hiddenApps) { this.hiddenApps = hiddenApps; }
}
