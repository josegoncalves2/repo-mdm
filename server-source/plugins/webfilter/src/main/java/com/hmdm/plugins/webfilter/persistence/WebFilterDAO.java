package com.hmdm.plugins.webfilter.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterAppCategory;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterDelivery;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterEntry;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterEvent;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterPolicy;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterSettings;
import com.hmdm.plugins.webfilter.persistence.mapper.WebFilterMapper;
import org.mybatis.guice.transactional.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * <p>Persistence of the web filter plugin. The customer ID is always explicit: REST resources pass the customer of the
 * authenticated user, the sync hook passes the customer of the device, the background task iterates all customers.</p>
 */
@Singleton
public class WebFilterDAO {

    private final WebFilterMapper mapper;

    @Inject
    public WebFilterDAO(WebFilterMapper mapper) {
        this.mapper = mapper;
    }

    public WebFilterPolicy getPolicy(int customerId, int configurationId) {
        return mapper.findPolicy(customerId, configurationId);
    }

    public List<WebFilterPolicy> getPolicies(int customerId) {
        return mapper.findPoliciesByCustomer(customerId);
    }

    public List<WebFilterPolicy> getAllEnabledPolicies() {
        return mapper.findAllEnabledPolicies();
    }

    public List<String> getCategories(int policyId) {
        return mapper.findCategories(policyId);
    }

    public List<WebFilterEntry> getEntries(int policyId) {
        return mapper.findEntries(policyId);
    }

    /**
     * <p>Creates or replaces the policy of a configuration with its categories and entries, atomically.</p>
     */
    @Transactional
    public WebFilterPolicy savePolicy(WebFilterPolicy policy, Collection<String> categories,
                                      Collection<WebFilterEntry> entries) {
        WebFilterPolicy existing = mapper.findPolicy(policy.getCustomerId(), policy.getConfigurationId());
        if (existing == null) {
            mapper.insertPolicy(policy);
        } else {
            policy.setId(existing.getId());
            mapper.updatePolicy(policy);
        }
        mapper.deleteCategories(policy.getId());
        for (String category : categories) {
            mapper.insertCategory(policy.getId(), category);
        }
        mapper.deleteEntries(policy.getId());
        for (WebFilterEntry entry : entries) {
            mapper.insertEntry(policy.getId(), entry);
        }
        return policy;
    }

    public List<WebFilterAppCategory> getAppCategories(int customerId) {
        return mapper.findAppCategories(customerId);
    }

    public boolean addAppCategory(WebFilterAppCategory item) {
        return mapper.insertAppCategory(item) > 0;
    }

    public boolean removeAppCategory(int id, int customerId) {
        return mapper.deleteAppCategory(id, customerId) > 0;
    }

    public List<String> getLockedHistory(int policyId) {
        return mapper.findLockedHistory(policyId);
    }

    public void addLockedHistory(int policyId, Collection<String> packages) {
        for (String pkg : packages) {
            mapper.insertLockedHistory(policyId, pkg);
        }
    }

    public WebFilterSettings getSettings(int customerId) {
        return mapper.findSettings(customerId);
    }

    public void saveDelivery(WebFilterDelivery delivery) {
        mapper.saveDelivery(delivery);
    }

    public List<WebFilterDelivery> getDeliveries(int customerId) {
        return mapper.findDeliveries(customerId);
    }

    /**
     * <p>Stores a blocked access, unless the same device already reported the same site in the last
     * <code>dedupMillis</code> (a page reload is not a new attempt) or it exceeded <code>maxPerHour</code>.</p>
     *
     * @return <code>true</code> if the event was stored.
     */
    public boolean addEvent(WebFilterEvent event, long dedupMillis, int maxPerHour) {
        long now = event.getCreatedAt();
        if (mapper.countRecentEvents(event.getDeviceId(), event.getHost(), now - dedupMillis) > 0
                || mapper.countDeviceEvents(event.getDeviceId(), now - 3600_000L) >= maxPerHour) {
            return false;
        }
        mapper.insertEvent(event);
        return true;
    }

    public List<WebFilterEvent> searchEvents(int customerId, String ip, String device, String site, int limit) {
        return mapper.searchEvents(customerId, like(ip), like(device), like(site), limit);
    }

    private static String like(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return "%" + value.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    public List<WebFilterEvent> getRecentEvents(int customerId, int limit) {
        return mapper.findRecentEvents(customerId, limit);
    }

    public int countEvents(int customerId, long since) {
        return mapper.countEvents(customerId, since);
    }

    public List<WebFilterEvent> getEventsPage(int customerId, int offset, int limit) {
        return mapper.findEventsPage(customerId, offset, limit);
    }

    public List<WebFilterEvent> searchEventsPage(int customerId, String ip, String device, String site,
                                                 int offset, int limit) {
        return mapper.searchEventsPage(customerId, like(ip), like(device), like(site), offset, limit);
    }

    public int countSearchEvents(int customerId, String ip, String device, String site) {
        return mapper.countSearchEvents(customerId, like(ip), like(device), like(site));
    }

    public int countAllEvents(int customerId) {
        return mapper.countAllEvents(customerId);
    }

    public int deleteEvents(int customerId, List<Integer> ids) {
        return ids == null || ids.isEmpty() ? 0 : mapper.deleteEvents(customerId, ids);
    }

    public int deleteAllEvents(int customerId) {
        return mapper.deleteAllEvents(customerId);
    }

    public String activeServerUrl() {
        try {
            return mapper.activeServerUrl();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public int purgeEvents(long before) {
        return mapper.purgeEvents(before);
    }

    public void saveSettings(WebFilterSettings settings) {
        mapper.saveSettings(settings);
    }
}
