package com.hmdm.plugins.moduleregistry.persistence.mapper;

import com.hmdm.plugins.moduleregistry.persistence.domain.ModuleState;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>ORM mapper da tabela plugin_moduleregistry_state.</p>
 */
public interface ModuleRegistryMapper {

    @Select("SELECT * FROM plugin_moduleregistry_state WHERE customerId = #{customerId} ORDER BY moduleId")
    List<ModuleState> findByCustomer(@Param("customerId") int customerId);

    // Upsert: a maioria dos modulos nunca teve a chave mexida (fica "ligado" por ausencia de
    // linha); so grava quando alguem de fato aciona o interruptor.
    @Insert("INSERT INTO plugin_moduleregistry_state (customerId, moduleId, disabled, disabledBy, disabledAt, reason) " +
            "VALUES (#{customerId}, #{moduleId}, #{disabled}, #{disabledBy}, #{disabledAt}, #{reason}) " +
            "ON CONFLICT (customerId, moduleId) DO UPDATE SET " +
            "disabled = EXCLUDED.disabled, disabledBy = EXCLUDED.disabledBy, " +
            "disabledAt = EXCLUDED.disabledAt, reason = EXCLUDED.reason")
    void upsert(ModuleState state);
}
