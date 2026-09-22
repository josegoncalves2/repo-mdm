package com.hmdm.plugins.webfilter.rest.json;

/**
 * <p>Web filter settings of the customer as shown in the console.</p>
 */
public class SettingsView {

    private String dnsDomain;

    public String getDnsDomain() { return dnsDomain; }
    public void setDnsDomain(String dnsDomain) { this.dnsDomain = dnsDomain; }
}
