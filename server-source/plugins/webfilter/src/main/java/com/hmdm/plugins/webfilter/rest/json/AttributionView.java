package com.hmdm.plugins.webfilter.rest.json;

/**
 * <p>A source attribution shown below the DNS lists.</p>
 */
public class AttributionView {

    private String name;
    private String license;
    private String url;

    public AttributionView() {
    }

    public AttributionView(String name, String license, String url) {
        this.name = name;
        this.license = license;
        this.url = url;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
