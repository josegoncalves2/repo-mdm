package com.hmdm.plugins.webfilter.rest.json;

/**
 * <p>One list (URL) of a category as shown and edited in the console. An inactive list stays registered but is left
 * out of the resolver.</p>
 */
public class SourceView {

    private String url;
    private boolean active = true;

    public SourceView() {
    }

    public SourceView(String url, boolean active) {
        this.url = url;
        this.active = active;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
