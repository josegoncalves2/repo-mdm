package com.hmdm.plugins.webfilter.resolver;

import com.hmdm.plugins.webfilter.service.WebFilterPolicyService;

public class WebFilterResolverConfigWriter {

    private final WebFilterPolicyService policyService;

    public WebFilterResolverConfigWriter(WebFilterPolicyService policyService) {
        this.policyService = policyService;
    }

    public void writeConfiguration(String outputPath) {
        // Write blocky.yml, profiles, sources.json
    }
}