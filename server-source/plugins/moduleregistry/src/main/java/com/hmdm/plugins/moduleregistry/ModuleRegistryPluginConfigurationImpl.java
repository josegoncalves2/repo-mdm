package com.hmdm.plugins.moduleregistry;

import com.google.inject.Module;
import com.hmdm.plugin.PluginConfiguration;
import com.hmdm.plugin.PluginTaskModule;
import com.hmdm.plugins.moduleregistry.guice.module.ModuleRegistryLiquibaseModule;
import com.hmdm.plugins.moduleregistry.guice.module.ModuleRegistryPersistenceModule;
import com.hmdm.plugins.moduleregistry.guice.module.ModuleRegistryRestModule;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <p>Configuracao do plugin "moduleregistry": guarda, por cliente (customer), quais modulos
 * NATIVOS do console (Dispositivos, Kiosk, Relatorios etc.) estao desligados para manutencao,
 * quem desligou e quando. Nao mexe na tabela "plugins" existente nem no PluginDAO/PluginList
 * ja usados pelo Web Filter e pelos demais plugins reais — ver o comentario do pom.xml.</p>
 */
public class ModuleRegistryPluginConfigurationImpl implements PluginConfiguration {

    public static final String PLUGIN_ID = "moduleregistry";

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getRootPackage() {
        return "com.hmdm.plugins.moduleregistry";
    }

    @Override
    public List<Module> getPluginModules(ServletContext context) {
        List<Module> modules = new ArrayList<>();
        modules.add(new ModuleRegistryLiquibaseModule(context));
        modules.add(new ModuleRegistryPersistenceModule(context));
        modules.add(new ModuleRegistryRestModule());
        return modules;
    }

    @Override
    public Optional<List<Class<? extends PluginTaskModule>>> getTaskModules(ServletContext context) {
        return Optional.empty();
    }
}
