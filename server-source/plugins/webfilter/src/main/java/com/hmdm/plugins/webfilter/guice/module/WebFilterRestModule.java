package com.hmdm.plugins.webfilter.guice.module;

import com.hmdm.plugins.webfilter.rest.*;
import com.google.inject.AbstractModule;

public class WebFilterRestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(WebFilterCatalogResource.class);
        bind(WebFilterPolicyResource.class);
        bind(WebFilterAppCategoryResource.class);
        bind(WebFilterDnsResource.class);
    }
}