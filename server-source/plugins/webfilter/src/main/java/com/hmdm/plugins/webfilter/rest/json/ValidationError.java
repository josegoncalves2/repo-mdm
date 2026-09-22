package com.hmdm.plugins.webfilter.rest.json;

/**
 * <p>A rejected input value: which field, which value and the localization key of the reason.</p>
 */
public class ValidationError {

    private String field;
    private String value;
    private String code;

    public ValidationError() {
    }

    public ValidationError(String field, String value, String code) {
        this.field = field;
        this.value = value;
        this.code = code;
    }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
