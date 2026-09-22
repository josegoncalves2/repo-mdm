package com.hmdm.plugins.webfilter.guice.module;

import com.hmdm.guice.module.AbstractPersistenceModule;

import javax.servlet.ServletContext;

public class WebFilterPersistenceModule extends AbstractPersistenceModule {

    public WebFilterPersistenceModule(ServletContext context) {
        super(context);
    }

    @Override
    protected String getMapperPackageName() {
        return "com.hmdm.plugins.webfilter.persistence.mapper";
    }

    @Override
    protected String getDomainObjectsPackageName() {
        return "com.hmdm.plugins.webfilter.persistence.domain";
    }
}