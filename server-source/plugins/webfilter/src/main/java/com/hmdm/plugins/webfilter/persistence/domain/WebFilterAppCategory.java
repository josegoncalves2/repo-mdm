package com.hmdm.plugins.webfilter.persistence.domain;

import java.io.Serializable;

/**
 * <p>A customer-specific assignment of an application package to a category.</p>
 */
public class WebFilterAppCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private int customerId;
    private String packageName;
    private String category;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
