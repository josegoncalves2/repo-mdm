package com.hmdm.plugins.webfilter.persistence.domain;

import java.io.Serializable;

/**
 * <p>A web filter policy of a single device configuration (profile).</p>
 */
public class WebFilterPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private int customerId;
    private int configurationId;
    private boolean enabled;
    private Long updatedAt;
    private String updatedBy;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public int getConfigurationId() { return configurationId; }
    public void setConfigurationId(int configurationId) { this.configurationId = configurationId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
