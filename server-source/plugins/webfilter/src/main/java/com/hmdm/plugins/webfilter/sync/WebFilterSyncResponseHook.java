package com.hmdm.plugins.webfilter.sync;

import com.hmdm.plugins.webfilter.decision.WebFilterDecisionService;
import com.hmdm.plugins.webfilter.service.WebFilterPolicyService;

public class WebFilterSyncResponseHook {

    private final WebFilterPolicyService policyService;
    private final WebFilterDecisionService decisionService;

    public WebFilterSyncResponseHook(
            WebFilterPolicyService policyService,
            WebFilterDecisionService decisionService) {
        this.policyService = policyService;
        this.decisionService = decisionService;
    }
}