package com.hmdm.plugins.webfilter.guice.module;

import com.google.inject.servlet.ServletModule;
import com.hmdm.plugin.rest.PluginAccessFilter;
import com.hmdm.plugins.webfilter.rest.WebFilterPublicResource;
import com.hmdm.plugins.webfilter.rest.WebFilterResource;
import com.hmdm.plugins.webfilter.sync.WebFilterSyncResponseHook;
import com.hmdm.rest.filter.AuthFilter;
import com.hmdm.rest.filter.PrivateIPFilter;
import com.hmdm.security.jwt.JWTFilter;

import java.util.Collections;
import java.util.List;

/**
 * <p>REST resources of the web filter and its sync hook. The hook is discovered by <code>SyncResource</code> among
 * the injector bindings.</p>
 */
public class WebFilterRestModule extends ServletModule {

    private static final List<String> protectedResources = Collections.singletonList(
            "/rest/plugins/webfilter/private/*"
    );

    @Override
    protected void configureServlets() {
        this.filter(protectedResources).through(JWTFilter.class);
        this.filter(protectedResources).through(AuthFilter.class);
        this.filter(protectedResources).through(PluginAccessFilter.class);
        this.filter(protectedResources).through(PrivateIPFilter.class);
        this.bind(WebFilterResource.class);
        this.bind(WebFilterPublicResource.class);
        this.bind(WebFilterSyncResponseHook.class);
    }
}
