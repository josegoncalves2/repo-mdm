package com.hmdm.plugins.webfilter.persistence.domain;

import java.io.Serializable;

/**
 * <p>Web filter settings of a customer account.</p>
 */
public class WebFilterSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    private int customerId;
    private String dnsDomain;

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getDnsDomain() { return dnsDomain; }
    public void setDnsDomain(String dnsDomain) { this.dnsDomain = dnsDomain; }
}
