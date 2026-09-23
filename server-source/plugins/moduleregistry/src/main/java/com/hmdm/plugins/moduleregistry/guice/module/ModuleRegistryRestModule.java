package com.hmdm.plugins.moduleregistry.guice.module;

import com.google.inject.servlet.ServletModule;
import com.hmdm.plugin.rest.PluginAccessFilter;
import com.hmdm.plugins.moduleregistry.rest.ModuleRegistryResource;
import com.hmdm.rest.filter.AuthFilter;
import com.hmdm.rest.filter.PrivateIPFilter;
import com.hmdm.security.jwt.JWTFilter;

import java.util.Collections;
import java.util.List;

/**
 * <p>REST do plugin moduleregistry. Mesma cadeia de filtros que os demais plugins (webfilter,
 * messaging): so usuario autenticado do console acessa; a permissao fina (quem pode desligar
 * um modulo) e' checada dentro do proprio recurso.</p>
 */
public class ModuleRegistryRestModule extends ServletModule {

    private static final List<String> protectedResources = Collections.singletonList(
            "/rest/plugins/moduleregistry/private/*"
    );

    @Override
    protected void configureServlets() {
        this.filter(protectedResources).through(JWTFilter.class);
        this.filter(protectedResources).through(AuthFilter.class);
        this.filter(protectedResources).through(PluginAccessFilter.class);
        this.filter(protectedResources).through(PrivateIPFilter.class);
        this.bind(ModuleRegistryResource.class);
    }
}
