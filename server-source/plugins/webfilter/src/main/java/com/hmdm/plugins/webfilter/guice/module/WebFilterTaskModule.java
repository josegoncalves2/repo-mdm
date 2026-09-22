package com.hmdm.plugins.webfilter.guice.module;

import com.google.inject.Inject;
import com.hmdm.event.EventService;
import com.hmdm.plugin.PluginTaskModule;
import com.hmdm.util.BackgroundTaskRunnerService;

public class WebFilterTaskModule implements PluginTaskModule {

    private final EventService eventService;
    private final BackgroundTaskRunnerService taskRunner;

    @Inject
    public WebFilterTaskModule(EventService eventService,
                               BackgroundTaskRunnerService taskRunner) {
        this.eventService = eventService;
        this.taskRunner = taskRunner;
    }

    @Override
    public void init() {
    }
}