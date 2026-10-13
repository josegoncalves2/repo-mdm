package com.hmdm.plugins.webfilter.rest.json;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>The web filter policy of a configuration as shown and edited in the console.</p>
 */
public class PolicyView {

    private int configurationId;
    private String configurationName;
    private boolean enabled;
    private List<String> categories = new ArrayList<>();
    private List<String> domainAllow = new ArrayList<>();
    private List<String> domainBlock = new ArrayList<>();
    private List<String> appAllow = new ArrayList<>();
    private List<String> appBlock = new ArrayList<>();
    private Long updatedAt;
    private String updatedBy;
    /** Read only: the private DNS hostname the devices of this profile receive, if the DNS domain is configured. */
    private String dnsHost;
    /** Read only: the packages this policy blocks with the current catalogs. */
    private List<String> blockedApps = new ArrayList<>();
    /** Read only: the sites the managed browser of the devices blocks (Chrome URLBlocklist). */
    private List<String> browserSites = new ArrayList<>();

    public int getConfigurationId() { return configurationId; }
    public void setConfigurationId(int configurationId) { this.configurationId = configurationId; }
    public String getConfigurationName() { return configurationName; }
    public void setConfigurationName(String configurationName) { this.configurationName = configurationName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    public List<String> getDomainAllow() { return domainAllow; }
    public void setDomainAllow(List<String> domainAllow) { this.domainAllow = domainAllow; }
    public List<String> getDomainBlock() { return domainBlock; }
    public void setDomainBlock(List<String> domainBlock) { this.domainBlock = domainBlock; }
    public List<String> getAppAllow() { return appAllow; }
    public void setAppAllow(List<String> appAllow) { this.appAllow = appAllow; }
    public List<String> getAppBlock() { return appBlock; }
    public void setAppBlock(List<String> appBlock) { this.appBlock = appBlock; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public String getDnsHost() { return dnsHost; }
    public void setDnsHost(String dnsHost) { this.dnsHost = dnsHost; }
    public List<String> getBlockedApps() { return blockedApps; }
    public void setBlockedApps(List<String> blockedApps) { this.blockedApps = blockedApps; }
    public List<String> getBrowserSites() { return browserSites; }
    public void setBrowserSites(List<String> browserSites) { this.browserSites = browserSites; }
}
