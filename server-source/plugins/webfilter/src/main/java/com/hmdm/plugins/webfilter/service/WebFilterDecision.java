package com.hmdm.plugins.webfilter.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * <p>The decision order of the web filter for application packages (design D6): allowlist, then blocklist, then
 * blocked category, else allowed. Protected packages are never blocked. Pure logic, no I/O.</p>
 */
public final class WebFilterDecision {

    private WebFilterDecision() {
    }

    /**
     * @param blockedCategories categories blocked by the policy.
     * @param appsByCategory    all known packages per category (initial catalog merged with the customer's own).
     * @param allowApps         allowlist of packages of the policy.
     * @param blockApps         blocklist of packages of the policy.
     * @param protectedPackages packages which must never be blocked (MDM agents, kiosk main app).
     * @return the packages the web filter blocks, sorted.
     */
    public static Set<String> blockedPackages(Collection<String> blockedCategories,
                                              Map<String, Set<String>> appsByCategory,
                                              Set<String> allowApps,
                                              Set<String> blockApps,
                                              Set<String> protectedPackages) {
        Set<String> candidates = new TreeSet<>(blockApps);
        for (String category : blockedCategories) {
            candidates.addAll(appsByCategory.getOrDefault(category, java.util.Collections.<String>emptySet()));
        }
        candidates.removeIf(p -> allowApps.contains(p) || protectedPackages.contains(p));
        return candidates;
    }
}
