package com.hmdm.plugins.moduleregistry.persistence.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>Estado de manutencao de UM modulo nativo do console para UM customer. Uma linha so
 * existe depois que alguem mexeu no interruptor pelo menos uma vez; um modulo sem linha aqui
 * esta ligado (o padrao e "ligado", nunca "desligado por omissao").</p>
 */
public class ModuleState implements Serializable {

    private Integer id;
    private Integer customerId;
    private String moduleId;
    private boolean disabled;
    private String disabledBy;
    private Date disabledAt;
    private String reason;

    /**
     * Nao vem do banco: calculado a partir da lista fixa de essenciais do proprio
     * ModuleRegistryResource. Vai no JSON para o console nao precisar duplicar a lista.
     */
    private transient boolean essential;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

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

    public String getDisabledBy() {
        return disabledBy;
    }

    public void setDisabledBy(String disabledBy) {
        this.disabledBy = disabledBy;
    }

    public Date getDisabledAt() {
        return disabledAt;
    }

    public void setDisabledAt(Date disabledAt) {
        this.disabledAt = disabledAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isEssential() {
        return essential;
    }

    public void setEssential(boolean essential) {
        this.essential = essential;
    }
}
