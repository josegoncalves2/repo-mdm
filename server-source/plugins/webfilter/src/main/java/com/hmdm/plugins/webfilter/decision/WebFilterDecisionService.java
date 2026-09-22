package com.hmdm.plugins.webfilter.decision;

public class WebFilterDecisionService {

    public boolean isBlocked(String domain, String policyId) {
        // Decision logic: allowlist > blocklist > category > permitted
        return false;
    }

    public boolean isAppBlocked(String packageName, String policyId) {
        return false;
    }
}