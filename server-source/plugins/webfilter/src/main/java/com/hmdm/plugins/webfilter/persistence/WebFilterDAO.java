package com.hmdm.plugins.webfilter.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterAppCategory;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterEntry;
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

    public void saveSettings(WebFilterSettings settings) {
        mapper.saveSettings(settings);
    }
}
