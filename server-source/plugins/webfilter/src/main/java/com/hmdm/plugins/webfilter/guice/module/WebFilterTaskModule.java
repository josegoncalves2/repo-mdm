package com.hmdm.plugins.webfilter.guice.module;

import com.google.inject.Inject;
import com.hmdm.plugin.PluginTaskModule;
import com.hmdm.plugins.webfilter.resolver.ResolverConfigWriter;
import com.hmdm.util.BackgroundTaskRunnerService;

import java.util.concurrent.TimeUnit;

/**
 * <p>Writes the resolver configuration at startup and every 10 minutes, which also drops profiles removed from the
 * MDM (design D8).</p>
 */
public class WebFilterTaskModule implements PluginTaskModule {

    private final ResolverConfigWriter resolverWriter;
    private final BackgroundTaskRunnerService taskRunner;

    @Inject
    public WebFilterTaskModule(ResolverConfigWriter resolverWriter, BackgroundTaskRunnerService taskRunner) {
        this.resolverWriter = resolverWriter;
        this.taskRunner = taskRunner;
    }

    @Override
    public void init() {
        taskRunner.submitRepeatableTask(resolverWriter::writeAll, 0, 10, TimeUnit.MINUTES);
    }
}
