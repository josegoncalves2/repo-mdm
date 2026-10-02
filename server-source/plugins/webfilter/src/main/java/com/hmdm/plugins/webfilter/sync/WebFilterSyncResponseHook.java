package com.hmdm.plugins.webfilter.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.Device;
import com.hmdm.plugins.webfilter.catalog.WebFilterCatalog;
import com.hmdm.plugins.webfilter.persistence.WebFilterDAO;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterDelivery;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterEntry;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterPolicy;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterSettings;
import com.hmdm.plugins.webfilter.service.WebFilterDecision;
import com.hmdm.plugins.webfilter.service.WebFilterService;
import com.hmdm.rest.json.SyncApplicationInt;
import com.hmdm.rest.json.SyncApplicationSettingInt;
import com.hmdm.rest.json.SyncResponseHook;
import com.hmdm.rest.json.SyncResponseInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * <p>Delivers the web filter policy of the device's profile in the configuration sync (design D9):</p>
 * <ul>
 *     <li>application category restrictions remain in the launcher's <code>locked_packages</code> and
 *     <code>unlocked_packages</code> settings;</li>
 *     <li>when the Web Filter companion is selected, merged browser-native policies are added under
 *     <code>webfilterBrowserPolicies</code> in the signed response and removed from launcher application settings;</li>
 *     <li>profiles without the companion retain the existing launcher-delivery behavior.</li>
 * </ul>
 * <p>Any failure keeps the original response, so the filter cannot break device sync.</p>
 */
@Singleton
public class WebFilterSyncResponseHook implements SyncResponseHook {

    private static final Logger log = LoggerFactory.getLogger(WebFilterSyncResponseHook.class);

    static final String LAUNCHER_PACKAGE = "com.hmdm.launcher";
    static final String LOCKED = "locked_packages";
    static final String UNLOCKED = "unlocked_packages";
    static final String DNS_HOST_FIELD = "webfilterDnsHost";
    static final String BROWSER_POLICIES_FIELD = "webfilterBrowserPolicies";
    static final String BROWSER_AGENT_PACKAGE = "com.hwmdm.webfilter";

    private final WebFilterDAO dao;
    private final WebFilterService service;
    private final WebFilterCatalog catalog;
    private final UnsecureDAO unsecureDAO;
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public WebFilterSyncResponseHook(WebFilterDAO dao, WebFilterService service, WebFilterCatalog catalog,
                                     UnsecureDAO unsecureDAO) {
        this.dao = dao;
        this.service = service;
        this.catalog = catalog;
        this.unsecureDAO = unsecureDAO;
    }

    @Override
    public SyncResponseInt handle(int deviceId, SyncResponseInt original) {
        try {
            return apply(deviceId, original);
        } catch (RuntimeException e) {
            log.error("Web filter could not extend the sync response of device {}; sending it unchanged", deviceId, e);
            return original;
        }
    }

    private SyncResponseInt apply(int deviceId, SyncResponseInt original) {
        Device device = unsecureDAO.getDeviceById(deviceId);
        if (device == null || device.getConfigurationId() == null) {
            return original;
        }
        int customerId = device.getCustomerId();
        WebFilterPolicy policy = dao.getPolicy(customerId, device.getConfigurationId());
        List<String> history = policy == null ? new ArrayList<>() : dao.getLockedHistory(policy.getId());
        boolean browserAgentSelected = original.getApplications() != null && original.getApplications().stream()
                .anyMatch(a -> BROWSER_AGENT_PACKAGE.equals(a.getPkg()) && !Boolean.TRUE.equals(a.isRemove()));

        // What the administrator configured by hand in the profile is preserved (design D2)
        List<SyncApplicationSettingInt> settings = original.getApplicationSettings() == null
                ? new ArrayList<>() : new ArrayList<>(original.getApplicationSettings());
        Set<String> adminLocked = packagesOf(settings, LOCKED);
        Set<String> adminUnlocked = packagesOf(settings, UNLOCKED);

        ObjectNode browserPolicyNode = null;
        Set<String> blocked = new TreeSet<>();
        if (policy != null && policy.isEnabled()) {
            Set<String> allow = new HashSet<>();
            Set<String> block = new HashSet<>();
            Set<String> allowDomains = new TreeSet<>();
            Set<String> blockDomains = new TreeSet<>();
            for (WebFilterEntry e : dao.getEntries(policy.getId())) {
                boolean allowed = WebFilterEntry.LIST_ALLOW.equals(e.getList());
                if (WebFilterEntry.KIND_APP.equals(e.getKind())) {
                    (allowed ? allow : block).add(e.getValue());
                } else if (WebFilterEntry.KIND_DOMAIN.equals(e.getKind())) {
                    (allowed ? allowDomains : blockDomains).add(e.getValue());
                }
            }
            // The required category is blocked while the policy is enabled, but never stored (design D7)
            Set<String> categories = new TreeSet<>(dao.getCategories(policy.getId()));
            categories.add(WebFilterCatalog.REQUIRED_CATEGORY);
            Set<String> protectedPackages = new HashSet<>(catalog.getProtectedPackages());
            if (original.getMainApp() != null) {
                protectedPackages.add(original.getMainApp()); // kiosk main app is never hidden (PRD-08)
            }
            blocked.addAll(WebFilterDecision.blockedPackages(categories,
                    service.appsByCategory(customerId), allow, block, protectedPackages));
            dao.addLockedHistory(policy.getId(), blocked);

            browserPolicyNode = BrowserPolicy.enabled(categories, catalog, allowDomains, blockDomains);
        }

        long now = System.currentTimeMillis();
        if (policy != null) {
            Set<String> locked = new TreeSet<>(adminLocked);
            locked.addAll(blocked);
            Set<String> unlocked = new TreeSet<>(history);
            unlocked.addAll(adminUnlocked);
            unlocked.removeAll(locked);

            settings.removeIf(s -> LAUNCHER_PACKAGE.equals(s.getPackageId())
                    && (LOCKED.equals(s.getName()) || UNLOCKED.equals(s.getName())));
            if (!locked.isEmpty()) {
                settings.add(new WebFilterAppSetting(LAUNCHER_PACKAGE, LOCKED, String.join(",", locked), now));
            }
            if (!unlocked.isEmpty()) {
                settings.add(new WebFilterAppSetting(LAUNCHER_PACKAGE, UNLOCKED, String.join(",", unlocked), now));
            }
        }

        // When the separate Web Filter APK is selected in the profile, it owns browser
        // restrictions through its narrow delegated scope. Before it is selected, preserve
        // the legacy launcher-delivery path so existing profiles keep working.
        ObjectNode browserPolicies = mapper.createObjectNode();
        for (String browser : BrowserPolicy.PACKAGES) {
            String adminBrowserValue = null;
            for (SyncApplicationSettingInt s : settings) {
                if (browser.equals(s.getPackageId()) && BrowserPolicy.SETTING.equals(s.getName())) {
                    adminBrowserValue = s.getValue();
                    break;
                }
            }
            try {
                String merged = BrowserPolicy.mergeForBrowser(browser, adminBrowserValue, browserPolicyNode);
                if (browserAgentSelected) {
                    browserPolicies.set(browser, mapper.readTree(merged));
                    settings.removeIf(s -> browser.equals(s.getPackageId()) && BrowserPolicy.SETTING.equals(s.getName()));
                } else if (browserPolicyNode != null) {
                    settings.removeIf(s -> browser.equals(s.getPackageId()) && BrowserPolicy.SETTING.equals(s.getName()));
                    settings.add(new WebFilterAppSetting(browser, BrowserPolicy.SETTING, merged, now));
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Could not serialize browser policy", e);
            }
        }

        ObjectNode tree = mapper.valueToTree(original);
        ArrayNode settingsJson = mapper.createArrayNode();
        for (SyncApplicationSettingInt s : settings) {
            settingsJson.add(settingJson(s));
        }
        tree.set("applicationSettings", settingsJson);
        if (browserAgentSelected) tree.set(BROWSER_POLICIES_FIELD, browserPolicies);

        if (policy != null) {
            recordDelivery(device, policy, browserPolicyNode, blocked.size(), now);
        }
        // Site filtering is applied by the companion through browser managed settings, not
        // by the launcher's Device Owner Private DNS policy.
        if (browserAgentSelected) {
            tree.remove(DNS_HOST_FIELD);
        } else if (policy != null) {
            String dnsHost = policy.isEnabled() ? service.dnsHost(customerId, policy.getConfigurationId()) : null;
            tree.put(DNS_HOST_FIELD, dnsHost == null ? "" : dnsHost);
        }
        return new WebFilterSyncResponse(original, tree, settings);
    }

    /**
     * <p>Remembers what this device received, for the dashboard. Never breaks the sync.</p>
     */
    private void recordDelivery(Device device, WebFilterPolicy policy, ObjectNode browserPolicyNode, int hiddenApps, long now) {
        try {
            WebFilterDelivery d = new WebFilterDelivery();
            d.setDeviceId(device.getId());
            d.setCustomerId(device.getCustomerId());
            d.setConfigurationId(policy.getConfigurationId());
            d.setEnabled(policy.isEnabled());
            d.setPolicyUpdatedAt(policy.getUpdatedAt());
            d.setDeliveredAt(now);
            d.setBrowserSites(browserPolicyNode == null ? 0 : browserPolicyNode.path(BrowserPolicy.BLOCKLIST).size());
            d.setHiddenApps(hiddenApps);
            dao.saveDelivery(d);
        } catch (RuntimeException e) {
            log.warn("Web filter could not record the delivery to device {}", device.getId(), e);
        }
    }

    private static Set<String> packagesOf(List<SyncApplicationSettingInt> settings, String name) {
        return settings.stream()
                .filter(s -> LAUNCHER_PACKAGE.equals(s.getPackageId()) && name.equals(s.getName()) && s.getValue() != null)
                .flatMap(s -> Arrays.stream(s.getValue().split(",")))
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * <p>Same JSON shape as the core serializes a sync application setting: the launcher reads
     * <code>packageId</code>, <code>name</code>, <code>type</code>, <code>value</code>, <code>lastUpdate</code>.</p>
     */
    private JsonNode settingJson(SyncApplicationSettingInt s) {
        ObjectNode n = mapper.valueToTree(s);
        if (!n.has("packageId")) {
            n.put("packageId", s.getPackageId());
        }
        return n;
    }
}
