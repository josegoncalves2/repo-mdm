package com.hmdm.plugins.webfilter.persistence.domain;

import java.io.Serializable;

/**
 * <p>An allowlist/blocklist entry of a policy: a domain or an application package.</p>
 */
public class WebFilterEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String KIND_DOMAIN = "DOMAIN";
    public static final String KIND_APP = "APP";
    public static final String LIST_ALLOW = "ALLOW";
    public static final String LIST_BLOCK = "BLOCK";

    private String kind;
    private String list;
    private String value;

    public WebFilterEntry() {
    }

    public WebFilterEntry(String kind, String list, String value) {
        this.kind = kind;
        this.list = list;
        this.value = value;
    }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getList() { return list; }
    public void setList(String list) { this.list = list; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
