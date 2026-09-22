package com.hmdm.plugins.webfilter.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hmdm.notification.PushService;
import com.hmdm.persistence.ConfigurationDAO;
import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.Application;
import com.hmdm.persistence.domain.ApplicationVersion;
import com.hmdm.persistence.domain.Configuration;
import com.hmdm.plugins.webfilter.catalog.WebFilterCatalog;
import com.hmdm.plugins.webfilter.persistence.WebFilterDAO;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterAppCategory;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterEntry;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterPolicy;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterSettings;
import com.hmdm.plugins.webfilter.resolver.ResolverConfigWriter;
import com.hmdm.plugins.webfilter.rest.json.PolicyView;
import com.hmdm.plugins.webfilter.rest.json.ValidationError;
import com.hmdm.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * <p>Business logic of the web filter used by the console: read and save policies, application categorization and
 * settings. Every change that affects devices regenerates the resolver files and sends <code>configUpdated</code> to
 * the devices of the affected profiles (PRD-12).</p>
 */
@Singleton
public class WebFilterService {

    private static final Logger log = LoggerFactory.getLogger(WebFilterService.class);

    private final WebFilterDAO dao;
    private final WebFilterCatalog catalog;
    private final ConfigurationDAO configurationDAO;
    private final ResolverConfigWriter resolverWriter;
    private final PushService pushService;
    private final UnsecureDAO unsecureDAO;

    @Inject
    public WebFilterService(WebFilterDAO dao, WebFilterCatalog catalog, ConfigurationDAO configurationDAO,
                            ResolverConfigWriter resolverWriter, PushService pushService, UnsecureDAO unsecureDAO) {
        this.unsecureDAO = unsecureDAO;
        this.dao = dao;
        this.catalog = catalog;
        this.configurationDAO = configurationDAO;
        this.resolverWriter = resolverWriter;
        this.pushService = pushService;
    }

    public static int currentCustomerId() {
        return SecurityContext.get().getCurrentCustomerId()
                .orElseThrow(() -> new SecurityException("No customer in the security context"));
    }

    // ================================================================================================= catalogs
    /**
     * <p>The application catalog of a customer: the initial catalog merged with the customer's own packages.</p>
     */
    public Map<String, Set<String>> appsByCategory(int customerId) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (String category : catalog.getCategoryIds()) {
            result.put(category, new TreeSet<>(catalog.getCatalogApps(category)));
        }
        for (WebFilterAppCategory item : dao.getAppCategories(customerId)) {
            result.computeIfAbsent(item.getCategory(), k -> new TreeSet<>()).add(item.getPackageName());
        }
        return result;
    }

    public String dnsHost(int customerId, int configurationId) {
        WebFilterSettings settings = dao.getSettings(customerId);
        String domain = settings == null ? null : WebFilterValidator.normalizeDomain(settings.getDnsDomain());
        return domain == null ? null : "id-" + ResolverConfigWriter.clientName(customerId, configurationId) + "." + domain;
    }

    // ================================================================================================= policies
    public List<PolicyView> listPolicies() {
        int customerId = currentCustomerId();
        Map<Integer, WebFilterPolicy> policies = new HashMap<>();
        for (WebFilterPolicy p : dao.getPolicies(customerId)) {
            policies.put(p.getConfigurationId(), p);
        }
        List<PolicyView> result = new ArrayList<>();
        for (Configuration c : configurationDAO.getAllConfigurations()) {
            result.add(toView(customerId, c, policies.get(c.getId())));
        }
        return result;
    }

    /**
     * @return the policy of the configuration (a disabled empty one if never saved), or <code>null</code> if the
     * current user has no access to the configuration.
     */
    public PolicyView getPolicy(int configurationId) {
        Configuration c = accessibleConfiguration(configurationId);
        if (c == null) {
            return null;
        }
        int customerId = currentCustomerId();
        return toView(customerId, c, dao.getPolicy(customerId, configurationId));
    }

    /**
     * <p>The package of the kiosk main app of the configuration, resolved the same way the sync does
     * (<code>SyncResource</code>: kiosk mode on and a content app set), or <code>null</code>.</p>
     */
    private String kioskMainApp(Configuration c) {
        if (c == null || !c.isKioskMode() || c.getContentAppId() == null) {
            return null;
        }
        ApplicationVersion version = unsecureDAO.findApplicationVersionById(c.getContentAppId());
        if (version == null) {
            return null;
        }
        Application app = unsecureDAO.findApplicationById(version.getApplicationId());
        return app == null ? null : app.getPkg();
    }

    private Set<String> protectedPackages(Configuration c) {
        Set<String> result = new HashSet<>(catalog.getProtectedPackages());
        String main = kioskMainApp(c);
        if (main != null) {
            result.add(main);
        }
        return result;
    }

    private Configuration accessibleConfiguration(int configurationId) {
        if (!configurationDAO.hasConfigurationAccess(configurationId)) {
            return null;
        }
        return configurationDAO.getConfigurationById(configurationId);
    }

    private PolicyView toView(int customerId, Configuration c, WebFilterPolicy policy) {
        PolicyView v = new PolicyView();
        v.setConfigurationId(c.getId());
        v.setConfigurationName(c.getName());
        if (policy == null) {
            return v;
        }
        v.setEnabled(policy.isEnabled());
        v.setUpdatedAt(policy.getUpdatedAt());
        v.setUpdatedBy(policy.getUpdatedBy());
        v.setCategories(dao.getCategories(policy.getId()));
        for (WebFilterEntry e : dao.getEntries(policy.getId())) {
            boolean allow = WebFilterEntry.LIST_ALLOW.equals(e.getList());
            if (WebFilterEntry.KIND_DOMAIN.equals(e.getKind())) {
                (allow ? v.getDomainAllow() : v.getDomainBlock()).add(e.getValue());
            } else {
                (allow ? v.getAppAllow() : v.getAppBlock()).add(e.getValue());
            }
        }
        if (policy.isEnabled()) {
            v.setDnsHost(dnsHost(customerId, c.getId()));
            v.setBlockedApps(new ArrayList<>(WebFilterDecision.blockedPackages(v.getCategories(),
                    appsByCategory(customerId), new HashSet<>(v.getAppAllow()), new HashSet<>(v.getAppBlock()),
                    protectedPackages(c))));
        }
        return v;
    }

    /**
     * <p>Validates and saves the policy of a configuration.</p>
     *
     * @return the validation errors (empty if the policy was saved), or <code>null</code> if the configuration does
     * not exist for the current user (another customer's profile is treated as non-existent).
     */
    public List<ValidationError> savePolicy(int configurationId, PolicyView input) {
        Configuration c = accessibleConfiguration(configurationId);
        if (c == null) {
            return null;
        }
        int customerId = currentCustomerId();
        List<ValidationError> errors = new ArrayList<>();

        Set<String> categories = new TreeSet<>();
        for (String category : nullSafe(input.getCategories())) {
            if (WebFilterCatalog.REQUIRED_CATEGORY.equals(category)) {
                continue; // always applied while enabled, never stored (design D7)
            }
            if (!catalog.isCategory(category)) {
                errors.add(new ValidationError("categories", category, "plugin.webfilter.error.category"));
            } else {
                categories.add(category);
            }
        }

        Set<String> protectedDomains = new HashSet<>();
        String mdmHost = WebFilterValidator.normalizeDomain(configurationDAO.getBaseUrl());
        if (mdmHost != null) {
            protectedDomains.add(mdmHost);
        }
        WebFilterSettings settings = dao.getSettings(customerId);
        if (settings != null && WebFilterValidator.normalizeDomain(settings.getDnsDomain()) != null) {
            protectedDomains.add(WebFilterValidator.normalizeDomain(settings.getDnsDomain()));
        }

        Set<String> domainAllow = domains("domainAllow", input.getDomainAllow(), errors, null);
        Set<String> domainBlock = domains("domainBlock", input.getDomainBlock(), errors, protectedDomains);
        Set<String> appAllow = packages("appAllow", input.getAppAllow(), errors, java.util.Collections.<String>emptySet());
        Set<String> appBlock = packages("appBlock", input.getAppBlock(), errors, catalog.getProtectedPackages());
        String kioskApp = kioskMainApp(c);
        if (kioskApp != null && appBlock.remove(kioskApp)) {
            errors.add(new ValidationError("appBlock", kioskApp, "plugin.webfilter.error.package.kiosk"));
        }

        for (String d : domainAllow) {
            if (domainBlock.contains(d)) {
                errors.add(new ValidationError("domainBlock", d, "plugin.webfilter.error.both.lists"));
            }
        }
        for (String p : appAllow) {
            if (appBlock.contains(p)) {
                errors.add(new ValidationError("appBlock", p, "plugin.webfilter.error.both.lists"));
            }
        }
        if (!errors.isEmpty()) {
            return errors;
        }

        WebFilterPolicy policy = new WebFilterPolicy();
        policy.setCustomerId(customerId);
        policy.setConfigurationId(configurationId);
        policy.setEnabled(input.isEnabled());
        policy.setUpdatedAt(System.currentTimeMillis());
        policy.setUpdatedBy(SecurityContext.get().getCurrentUserName());

        List<WebFilterEntry> entries = new ArrayList<>();
        domainAllow.forEach(v -> entries.add(new WebFilterEntry(WebFilterEntry.KIND_DOMAIN, WebFilterEntry.LIST_ALLOW, v)));
        domainBlock.forEach(v -> entries.add(new WebFilterEntry(WebFilterEntry.KIND_DOMAIN, WebFilterEntry.LIST_BLOCK, v)));
        appAllow.forEach(v -> entries.add(new WebFilterEntry(WebFilterEntry.KIND_APP, WebFilterEntry.LIST_ALLOW, v)));
        appBlock.forEach(v -> entries.add(new WebFilterEntry(WebFilterEntry.KIND_APP, WebFilterEntry.LIST_BLOCK, v)));

        dao.savePolicy(policy, categories, entries);
        applyChanges(java.util.Collections.singletonList(configurationId));
        return errors;
    }

    private static Set<String> domains(String field, Collection<String> values, List<ValidationError> errors,
                                       Set<String> refused) {
        Set<String> result = new LinkedHashSet<>();
        for (String raw : nullSafe(values)) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            String d = WebFilterValidator.normalizeDomain(raw);
            if (d == null) {
                errors.add(new ValidationError(field, raw, "plugin.webfilter.error.domain"));
            } else if (refused != null && refused.contains(d)) {
                errors.add(new ValidationError(field, raw, "plugin.webfilter.error.domain.protected"));
            } else {
                result.add(d);
            }
        }
        return result;
    }

    private static Set<String> packages(String field, Collection<String> values, List<ValidationError> errors,
                                        Set<String> refused) {
        Set<String> result = new LinkedHashSet<>();
        for (String raw : nullSafe(values)) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            String p = raw.trim();
            if (!WebFilterValidator.isValidPackage(p)) {
                errors.add(new ValidationError(field, raw, "plugin.webfilter.error.package"));
            } else if (refused.contains(p)) {
                errors.add(new ValidationError(field, raw, "plugin.webfilter.error.package.protected"));
            } else {
                result.add(p);
            }
        }
        return result;
    }

    private static <T> Collection<T> nullSafe(Collection<T> c) {
        return c == null ? java.util.Collections.<T>emptyList() : c;
    }

    // ================================================================================================= app categories
    public List<WebFilterAppCategory> getAppCategories() {
        return dao.getAppCategories(currentCustomerId());
    }

    public List<ValidationError> addAppCategory(String packageName, String category) {
        List<ValidationError> errors = new ArrayList<>();
        String p = packageName == null ? "" : packageName.trim();
        if (!WebFilterValidator.isValidPackage(p)) {
            errors.add(new ValidationError("packageName", packageName, "plugin.webfilter.error.package"));
        } else if (catalog.getProtectedPackages().contains(p)) {
            errors.add(new ValidationError("packageName", packageName, "plugin.webfilter.error.package.protected"));
        }
        if (category == null || !catalog.isCategory(category) || WebFilterCatalog.REQUIRED_CATEGORY.equals(category)) {
            errors.add(new ValidationError("category", category, "plugin.webfilter.error.category"));
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        WebFilterAppCategory item = new WebFilterAppCategory();
        item.setCustomerId(currentCustomerId());
        item.setPackageName(p);
        item.setCategory(category);
        if (dao.addAppCategory(item)) {
            applyChanges(enabledConfigurations(item.getCustomerId()));
        }
        return errors;
    }

    public boolean removeAppCategory(int id) {
        int customerId = currentCustomerId();
        boolean removed = dao.removeAppCategory(id, customerId);
        if (removed) {
            applyChanges(enabledConfigurations(customerId));
        }
        return removed;
    }

    // ================================================================================================= settings
    public String getDnsDomain() {
        WebFilterSettings s = dao.getSettings(currentCustomerId());
        return s == null ? null : s.getDnsDomain();
    }

    public List<ValidationError> saveDnsDomain(String dnsDomain) {
        List<ValidationError> errors = new ArrayList<>();
        String value = null;
        if (dnsDomain != null && !dnsDomain.trim().isEmpty()) {
            value = WebFilterValidator.normalizeDomain(dnsDomain);
            if (value == null) {
                errors.add(new ValidationError("dnsDomain", dnsDomain, "plugin.webfilter.error.domain"));
                return errors;
            }
        }
        WebFilterSettings s = new WebFilterSettings();
        s.setCustomerId(currentCustomerId());
        s.setDnsDomain(value);
        dao.saveSettings(s);
        applyChanges(enabledConfigurations(s.getCustomerId()));
        return errors;
    }

    // ================================================================================================= propagation
    private List<Integer> enabledConfigurations(int customerId) {
        List<Integer> ids = new ArrayList<>();
        for (WebFilterPolicy p : dao.getPolicies(customerId)) {
            if (p.isEnabled()) {
                ids.add(p.getConfigurationId());
            }
        }
        return ids;
    }

    /**
     * <p>Regenerates the resolver files and asks the devices of the given configurations to sync now.</p>
     */
    private void applyChanges(Collection<Integer> configurationIds) {
        resolverWriter.writeAll();
        for (Integer id : configurationIds) {
            try {
                pushService.notifyDevicesOnUpdate(id);
            } catch (RuntimeException e) {
                log.error("Failed to notify devices of configuration {} about web filter changes", id, e);
            }
        }
    }
}
