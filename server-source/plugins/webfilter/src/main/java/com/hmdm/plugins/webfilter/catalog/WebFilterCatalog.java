package com.hmdm.plugins.webfilter.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>The fixed category catalog of the web filter (design D5): site sources per category, the initial application
 * catalog and the packages of the MDM itself that must never be blocked. Loaded once from the plugin resources.</p>
 */
@Singleton
public class WebFilterCatalog {

    /**
     * <p>The category which is always blocked while a policy is enabled, so that DNS-over-HTTPS cannot bypass the
     * filter (PRD-05).</p>
     */
    public static final String REQUIRED_CATEGORY = "doh";

    private final List<String> categoryIds = new ArrayList<>();
    private final Map<String, List<String>> siteSources = new LinkedHashMap<>();
    private final Map<String, Set<String>> appsByCategory = new LinkedHashMap<>();
    private final Set<String> protectedPackages = new LinkedHashSet<>();
    private final JsonNode attribution;

    public WebFilterCatalog() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode sites = read(mapper, "/webfilter-catalog.json");
        JsonNode apps = read(mapper, "/webfilter-app-catalog.json");

        for (JsonNode c : sites.get("categories")) {
            String id = c.get("id").asText();
            categoryIds.add(id);
            List<String> urls = new ArrayList<>();
            c.get("sources").forEach(u -> urls.add(u.asText()));
            siteSources.put(id, Collections.unmodifiableList(urls));
            appsByCategory.put(id, new LinkedHashSet<>());
        }
        attribution = sites.get("attribution");

        apps.get("protectedPackages").forEach(p -> protectedPackages.add(p.asText()));
        apps.get("categories").fields().forEachRemaining(e -> {
            if (!appsByCategory.containsKey(e.getKey())) {
                throw new IllegalStateException("App catalog references unknown category " + e.getKey());
            }
            e.getValue().forEach(p -> appsByCategory.get(e.getKey()).add(p.asText()));
        });
    }

    private static JsonNode read(ObjectMapper mapper, String resource) {
        try (InputStream in = WebFilterCatalog.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + resource);
            }
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + resource, e);
        }
    }

    public List<String> getCategoryIds() {
        return Collections.unmodifiableList(categoryIds);
    }

    public boolean isCategory(String id) {
        return siteSources.containsKey(id);
    }

    public List<String> getSiteSources(String category) {
        return siteSources.getOrDefault(category, Collections.emptyList());
    }

    public Set<String> getCatalogApps(String category) {
        return Collections.unmodifiableSet(appsByCategory.getOrDefault(category, Collections.emptySet()));
    }

    public Set<String> getProtectedPackages() {
        return Collections.unmodifiableSet(protectedPackages);
    }

    public JsonNode getAttribution() {
        return attribution;
    }
}
