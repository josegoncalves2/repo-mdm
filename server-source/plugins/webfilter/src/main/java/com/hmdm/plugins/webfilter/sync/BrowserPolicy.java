package com.hmdm.plugins.webfilter.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hmdm.plugins.webfilter.catalog.WebFilterCatalog;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>The managed browser policy of a web filter profile. The launcher is the device owner and hands the
 * <code>managedConfig</code> setting of a package to Android as its application restrictions, which Chrome reads
 * as enterprise policies. So the filter is enforced on the device itself, without a VPN and without depending on
 * the device DNS: a blocked site shows the Chrome "blocked by your administrator" page.</p>
 * <p>Pure logic, no I/O.</p>
 */
public final class BrowserPolicy {

    /**
     * <p>Browsers that read Chrome enterprise policies from their application restrictions.</p>
     */
    public static final List<String> CHROME_PACKAGES = Collections.unmodifiableList(Arrays.asList(
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary"));
    public static final String EDGE_PACKAGE = "com.microsoft.emmx";
    public static final List<String> PACKAGES = Collections.unmodifiableList(Arrays.asList(
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary", EDGE_PACKAGE));

    public static final String SETTING = "managedConfig";

    /**
     * <p>Chrome ignores the entries of an URL list beyond this limit.</p>
     */
    static final int MAX_URLS = 1000;

    public static final String BLOCKLIST = "URLBlocklist";
    public static final String ALLOWLIST = "URLAllowlist";

    private static final String ADULT = "adult";

    private BrowserPolicy() {
    }

    /**
     * @param categories   blocked categories of the policy, including the required one.
     * @param catalog      the category catalog.
     * @param allowDomains allowlist of domains of the policy.
     * @param blockDomains blocklist of domains of the policy.
     * @return the Chrome policies which enforce the filter.
     */
    public static ObjectNode enabled(Collection<String> categories, WebFilterCatalog catalog,
                                     Collection<String> allowDomains, Collection<String> blockDomains) {
        // Explicit entries first, so the Chrome limit never drops what the administrator typed in
        Set<String> block = new LinkedHashSet<>(blockDomains);
        for (String category : categories) {
            block.addAll(catalog.getBrowserDomains(category));
        }
        block.removeAll(allowDomains);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode policy = mapper.createObjectNode();
        policy.set(BLOCKLIST, list(mapper, block));
        if (!allowDomains.isEmpty()) {
            policy.set(ALLOWLIST, list(mapper, allowDomains));
        }
        // No private windows and no DNS-over-HTTPS of the browser itself: both would bypass the filter
        policy.put("IncognitoModeAvailability", 1);
        policy.put("DnsOverHttpsMode", "off");
        // Malware and phishing: Safe Browsing on, and its warning page cannot be skipped
        policy.put("SafeBrowsingProtectionLevel", 1);
        policy.put("DisableSafeBrowsingProceedAnyway", true);
        if (categories.contains(ADULT)) {
            policy.put("SafeSitesFilterBehavior", 1);
            policy.put("ForceGoogleSafeSearch", true);
            policy.put("ForceYouTubeRestrict", 2);
        }
        return policy;
    }

    /**
     * <p>Merges the filter policies over what the administrator configured by hand in the profile. URL lists are
     * joined; any other policy of the filter wins.</p>
     *
     * @param adminValue the <code>managedConfig</code> value set by hand, or <code>null</code>.
     * @param filter     the filter policies, or <code>null</code> when the filter is off.
     * @return the value to send; <code>{}</code> clears restrictions previously applied by the filter.
     */
    public static String merge(String adminValue, ObjectNode filter) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = parse(mapper, adminValue);
        if (filter != null) {
            filter.fields().forEachRemaining(e -> {
                JsonNode own = result.get(e.getKey());
                if (e.getValue().isArray() && own != null && own.isArray()) {
                    Set<String> joined = new LinkedHashSet<>();
                    e.getValue().forEach(n -> joined.add(n.asText()));
                    own.forEach(n -> joined.add(n.asText()));
                    result.set(e.getKey(), list(mapper, joined));
                } else {
                    result.set(e.getKey(), e.getValue());
                }
            });
        }
        return result.toString();
    }

    /** Microsoft Edge for Android uses its own managed-configuration names and pipe format. */
    public static String mergeForBrowser(String browser, String adminValue, ObjectNode filter) {
        if (!EDGE_PACKAGE.equals(browser)) return merge(adminValue, filter);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = parse(mapper, adminValue);
        if (filter == null) return result.toString();

        Set<String> urls = new LinkedHashSet<>();
        JsonNode adminBlock = result.get("com.microsoft.intune.mam.managedbrowser.BlockListURLs");
        if (adminBlock != null && adminBlock.isTextual()) {
            for (String value : adminBlock.asText().split("\\|")) {
                if (!value.trim().isEmpty()) urls.add(value.trim());
            }
        }
        JsonNode filterBlock = filter.path(BLOCKLIST);
        if (filterBlock.isArray()) {
            for (JsonNode node : filterBlock) {
                String domain = node.asText().trim();
                while (domain.startsWith("*.")) domain = domain.substring(2);
                if (domain.isEmpty()) continue;
                urls.add("http://" + domain + "/*");
                urls.add("https://" + domain + "/*");
                urls.add("http://*." + domain + "/*");
                urls.add("https://*." + domain + "/*");
            }
        }
        List<String> limited = new java.util.ArrayList<>();
        for (String url : urls) {
            if (limited.size() == MAX_URLS) break;
            limited.add(url);
        }
        result.put("com.microsoft.intune.mam.managedbrowser.BlockListURLs", String.join("|", limited));
        result.put("com.microsoft.intune.mam.managedbrowser.AllowTransitionOnBlock", false);
        result.put("com.microsoft.intune.mam.managedbrowser.openInPrivateIfBlocked", false);
        return result.toString();
    }

    private static ObjectNode parse(ObjectMapper mapper, String value) {
        if (value != null && !value.trim().isEmpty()) {
            try {
                JsonNode n = mapper.readTree(value);
                if (n != null && n.isObject()) {
                    return (ObjectNode) n;
                }
            } catch (IOException e) {
                // A malformed value set by hand is replaced by the filter policies
            }
        }
        return mapper.createObjectNode();
    }

    private static ArrayNode list(ObjectMapper mapper, Collection<String> values) {
        ArrayNode a = mapper.createArrayNode();
        for (String v : values) {
            if (a.size() >= MAX_URLS) {
                break;
            }
            a.add(v);
        }
        return a;
    }
}
