package com.hmdm.plugins.moduleregistry.rest.json;

/**
 * <p>Corpo do POST que liga/desliga um modulo nativo.</p>
 */
public class ToggleModuleRequest {

    private String moduleId;
    private boolean disabled;
    private String reason;

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
