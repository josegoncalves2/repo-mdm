package com.hmdm.plugins.webfilter;

import com.google.inject.Module;
import com.hmdm.plugin.PluginConfiguration;
import com.hmdm.plugin.PluginTaskModule;
import com.hmdm.plugins.webfilter.guice.module.WebFilterLiquibaseModule;
import com.hmdm.plugins.webfilter.guice.module.WebFilterPersistenceModule;
import com.hmdm.plugins.webfilter.guice.module.WebFilterRestModule;
import com.hmdm.plugins.webfilter.guice.module.WebFilterTaskModule;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WebFilterPluginConfigurationImpl implements PluginConfiguration {

    public static final String PLUGIN_ID = "webfilter";

    public WebFilterPluginConfigurationImpl() {
    }

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getRootPackage() {
        return "com.hmdm.plugins.webfilter";
    }

    @Override
    public List<Module> getPluginModules(ServletContext context) {
        List<Module> modules = new ArrayList<>();
        modules.add(new WebFilterLiquibaseModule(context));
        modules.add(new WebFilterPersistenceModule(context));
        modules.add(new WebFilterRestModule());
        return modules;
    }

    @Override
    public Optional<List<Class<? extends PluginTaskModule>>> getTaskModules(ServletContext context) {
        List<Class<? extends PluginTaskModule>> modules = new ArrayList<>();
        modules.add(WebFilterTaskModule.class);
        return Optional.of(modules);
    }
}
