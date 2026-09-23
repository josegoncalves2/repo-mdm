package com.hmdm.plugins.moduleregistry.guice.module;

import com.hmdm.guice.module.AbstractPersistenceModule;

import javax.servlet.ServletContext;

/**
 * <p>Configuracao MyBatis do plugin moduleregistry.</p>
 */
public class ModuleRegistryPersistenceModule extends AbstractPersistenceModule {

    public ModuleRegistryPersistenceModule(ServletContext context) {
        super(context);
    }

    @Override
    protected String getMapperPackageName() {
        return "com.hmdm.plugins.moduleregistry.persistence.mapper";
    }

    @Override
    protected String getDomainObjectsPackageName() {
        return "com.hmdm.plugins.moduleregistry.persistence.domain";
    }
}
