package com.hmdm.plugins.webfilter.persistence.domain;

import java.io.Serializable;

/**
 * <p>An access the web filter blocked on a device, as reported by the device (the managed browser showed its
 * "blocked by your administrator" page).</p>
 */
public class WebFilterEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SOURCE_BROWSER = "browser";

    private Integer id;
    private int customerId;
    private Integer deviceId;
    private String clientIp;
    private String sourceKey;
    private Integer configurationId;
    private String host;
    private String url;
    private String category;
    private String source;
    private long createdAt;
    /** Read only, joined from the devices table. */
    private String deviceNumber;

    public String getClientIp() { return clientIp; }
    public void setClientIp(String value) { clientIp = value; }
    public String getSourceKey() { return sourceKey; }
    public void setSourceKey(String value) { sourceKey = value; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public Integer getDeviceId() { return deviceId; }
    public void setDeviceId(Integer deviceId) { this.deviceId = deviceId; }
    public Integer getConfigurationId() { return configurationId; }
    public void setConfigurationId(Integer configurationId) { this.configurationId = configurationId; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getDeviceNumber() { return deviceNumber; }
    public void setDeviceNumber(String deviceNumber) { this.deviceNumber = deviceNumber; }
}
