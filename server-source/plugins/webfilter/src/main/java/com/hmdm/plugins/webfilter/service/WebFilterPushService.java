package com.hmdm.plugins.webfilter.service;

import com.hmdm.plugins.webfilter.decision.WebFilterDecisionService;

public class WebFilterPushService {

    private final WebFilterDecisionService decisionService;

    public WebFilterPushService(WebFilterDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    public void sendConfigUpdated(String policyId) {
        // Send configUpdated to affected devices
    }
}