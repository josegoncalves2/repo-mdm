package com.hmdm.plugins.moduleregistry.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hmdm.plugins.moduleregistry.persistence.domain.ModuleState;
import com.hmdm.plugins.moduleregistry.persistence.mapper.ModuleRegistryMapper;
import org.mybatis.guice.transactional.Transactional;

import java.util.Date;
import java.util.List;

/**
 * <p>DAO do plugin moduleregistry: le e grava o estado de manutencao dos modulos nativos,
 * sempre escopado pelo customer do usuario autenticado (o mesmo isolamento multi-tenant que
 * o resto do console ja usa).</p>
 */
@Singleton
public class ModuleRegistryDAO {

    private final ModuleRegistryMapper mapper;

    @Inject
    public ModuleRegistryDAO(ModuleRegistryMapper mapper) {
        this.mapper = mapper;
    }

    public List<ModuleState> findByCustomer(int customerId) {
        return this.mapper.findByCustomer(customerId);
    }

    @Transactional
    public void setDisabled(int customerId, String moduleId, boolean disabled, String actor, String reason) {
        ModuleState state = new ModuleState();
        state.setCustomerId(customerId);
        state.setModuleId(moduleId);
        state.setDisabled(disabled);
        // Quando volta a ligar, guardamos quem/quando religou no mesmo par de campos: nao ha
        // por que manter dois pares de auditoria (desligou/religou) para uma tela que so
        // precisa responder "esta ligado ou nao, e por causa de quem".
        state.setDisabledBy(actor);
        state.setDisabledAt(new Date());
        state.setReason(disabled ? reason : null);
        this.mapper.upsert(state);
    }
}
