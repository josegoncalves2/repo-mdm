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
import com.hmdm.plugins.webfilter.service.WebFilterDecision;
import com.hmdm.plugins.webfilter.service.WebFilterService;
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
 *     <li><code>webfilterDnsHost</code>: the private DNS hostname of the profile, when the policy is enabled and the
 *     customer has a DNS domain configured;</li>
 *     <li><code>locked_packages</code> / <code>unlocked_packages</code> settings of the launcher, merged with any values
 *     the administrator set by hand (design D2). The launcher in production already applies them.</li>
 *     <li><code>managedConfig</code> of the Chrome packages ({@link BrowserPolicy}): the launcher, as device owner,
 *     turns it into Chrome enterprise policies, so blocked sites are blocked on the device itself. When the filter
 *     is off the value falls back to what the administrator set by hand, or <code>{}</code>, which clears it.</li>
 * </ul>
 * <p>A device whose profile has no web filter policy is returned untouched. Any failure keeps the original
 * response, so the filter can never break the device sync.</p>
 */
@Singleton
public class WebFilterSyncResponseHook implements SyncResponseHook {

    private static final Logger log = LoggerFactory.getLogger(WebFilterSyncResponseHook.class);

    static final String LAUNCHER_PACKAGE = "com.hmdm.launcher";
    static final String LOCKED = "locked_packages";
    static final String UNLOCKED = "unlocked_packages";
    static final String DNS_HOST_FIELD = "webfilterDnsHost";

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
        if (policy == null) {
            return original;
        }
        List<String> history = dao.getLockedHistory(policy.getId());

        // What the administrator configured by hand in the profile is preserved (design D2)
        List<SyncApplicationSettingInt> settings = original.getApplicationSettings() == null
                ? new ArrayList<>() : new ArrayList<>(original.getApplicationSettings());
        Set<String> adminLocked = packagesOf(settings, LOCKED);
        Set<String> adminUnlocked = packagesOf(settings, UNLOCKED);

        Set<String> blocked = new TreeSet<>();
        ObjectNode browserPolicy = null;
        if (policy.isEnabled()) {
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
            browserPolicy = BrowserPolicy.enabled(categories, catalog, allowDomains, blockDomains);
        }

        Set<String> locked = new TreeSet<>(adminLocked);
        locked.addAll(blocked);
        Set<String> unlocked = new TreeSet<>(history);
        unlocked.addAll(adminUnlocked);
        unlocked.removeAll(locked);

        long now = System.currentTimeMillis();
        settings.removeIf(s -> LAUNCHER_PACKAGE.equals(s.getPackageId())
                && (LOCKED.equals(s.getName()) || UNLOCKED.equals(s.getName())));
        if (!locked.isEmpty()) {
            settings.add(new WebFilterAppSetting(LAUNCHER_PACKAGE, LOCKED, String.join(",", locked), now));
        }
        if (!unlocked.isEmpty()) {
            settings.add(new WebFilterAppSetting(LAUNCHER_PACKAGE, UNLOCKED, String.join(",", unlocked), now));
        }
        for (String browser : BrowserPolicy.PACKAGES) {
            String adminValue = settings.stream()
                    .filter(s -> browser.equals(s.getPackageId()) && BrowserPolicy.SETTING.equals(s.getName()))
                    .map(SyncApplicationSettingInt::getValue)
                    .findFirst().orElse(null);
            settings.removeIf(s -> browser.equals(s.getPackageId()) && BrowserPolicy.SETTING.equals(s.getName()));
            settings.add(new WebFilterAppSetting(browser, BrowserPolicy.SETTING,
                    BrowserPolicy.merge(adminValue, browserPolicy), now));
        }

        ObjectNode tree = mapper.valueToTree(original);
        ArrayNode settingsJson = mapper.createArrayNode();
        for (SyncApplicationSettingInt s : settings) {
            settingsJson.add(settingJson(s));
        }
        tree.set("applicationSettings", settingsJson);

        recordDelivery(device, policy, browserPolicy, blocked.size(), now);

        String dnsHost = policy.isEnabled() ? service.dnsHost(customerId, policy.getConfigurationId()) : null;
        if (dnsHost != null) {
            tree.put(DNS_HOST_FIELD, dnsHost);
        } else {
            // A policy exists for this profile but is disabled or lacks DNS settings. Send an explicit
            // empty value so the launcher clears a Private DNS setting previously enforced by Web Filter.
            tree.put(DNS_HOST_FIELD, "");
        }
        return new WebFilterSyncResponse(original, tree, settings);
    }

    /**
     * <p>Remembers what this device received, for the dashboard. Never breaks the sync.</p>
     */
    private void recordDelivery(Device device, WebFilterPolicy policy, ObjectNode browserPolicy, int hiddenApps,
                                long now) {
        try {
            WebFilterDelivery d = new WebFilterDelivery();
            d.setDeviceId(device.getId());
            d.setCustomerId(device.getCustomerId());
            d.setConfigurationId(policy.getConfigurationId());
            d.setEnabled(policy.isEnabled());
            d.setPolicyUpdatedAt(policy.getUpdatedAt());
            d.setDeliveredAt(now);
            d.setBrowserSites(browserPolicy == null ? 0 : browserPolicy.path(BrowserPolicy.BLOCKLIST).size());
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
