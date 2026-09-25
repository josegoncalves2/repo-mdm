package com.hmdm.plugins.webfilter.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import com.hmdm.plugins.webfilter.rest.json.SourceView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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

    @com.google.inject.Inject private com.hmdm.plugins.webfilter.persistence.mapper.WebFilterMapper sourceMapper;

    private final List<String> categoryIds = new ArrayList<>();
    private final Map<String, List<String>> siteSources = new LinkedHashMap<>();
    private final Map<String, List<String>> browserDomains = new LinkedHashMap<>();
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
            List<String> domains = new ArrayList<>();
            if (c.has("browserDomains")) {
                c.get("browserDomains").forEach(d -> domains.add(d.asText()));
            }
            browserDomains.put(id, Collections.unmodifiableList(domains));
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

    /**
     * <p>The lists of a category that go to the resolver: the active ones only.</p>
     */
    public List<String> getSiteSources(String category) {
        List<String> result = new ArrayList<>();
        for (SourceView source : getSourceEntries(category)) {
            if (source.isActive()) {
                result.add(source.getUrl());
            }
        }
        return result;
    }

    /**
     * <p>Every registered list of a category, active or not, in the saved order. While a category was never saved,
     * its lists are the ones of the catalog, all active.</p>
     */
    public List<SourceView> getSourceEntries(String category) {
        List<String> urls = siteSources.getOrDefault(category, Collections.emptyList());
        Set<String> inactive = Collections.emptySet();
        if (sourceMapper != null) {
            String saved = sourceMapper.sourceUrls(category);
            if (saved != null) {
                urls = lines(saved);
                inactive = new HashSet<>(lines(sourceMapper.inactiveSourceUrls(category)));
            }
        }
        List<SourceView> result = new ArrayList<>();
        for (String url : urls) {
            result.add(new SourceView(url, !inactive.contains(url)));
        }
        return result;
    }

    private static List<String> lines(String value) {
        return value == null || value.isEmpty() ? Collections.<String>emptyList() : Arrays.asList(value.split("\\n"));
    }

    /**
     * <p>The sites the managed browser blocks for a category (Chrome <code>URLBlocklist</code> filters).</p>
     */
    public List<String> getBrowserDomains(String category) {
        return browserDomains.getOrDefault(category, Collections.emptyList());
    }

    public Set<String> getCatalogApps(String category) {
        return Collections.unmodifiableSet(appsByCategory.getOrDefault(category, Collections.emptySet()));
    }

    public Set<String> getProtectedPackages() {
        return Collections.unmodifiableSet(protectedPackages);
    }

    /**
     * <p>Replaces the lists of a category. Blank rows are ignored; a URL repeated in the request is kept once and
     * stays active if any of its rows is active.</p>
     */
    public void saveSources(String category, List<SourceView> sources) {
        if (!isCategory(category) || sources == null || sources.size() > 30) { throw new IllegalArgumentException("Categoria ou lista inválida"); }
        Map<String, Boolean> normalized = new LinkedHashMap<>();
        for (SourceView source : sources) {
            String value = source == null || source.getUrl() == null ? "" : source.getUrl().trim();
            if (value.isEmpty()) {
                continue;
            }
            java.net.URI uri;
            try {
                uri = java.net.URI.create(value);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Endereço inválido: " + value);
            }
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null || value.length() > 2000) {
                throw new IllegalArgumentException("Use um endereço https:// válido: " + value);
            }
            normalized.merge(uri.toString(), source.isActive(), Boolean::logicalOr);
        }
        List<String> inactive = new ArrayList<>();
        normalized.forEach((url, active) -> {
            if (!active) {
                inactive.add(url);
            }
        });
        sourceMapper.saveSourceUrls(category, String.join("\n", normalized.keySet()), String.join("\n", inactive));
    }

    public JsonNode getAttribution() {
        return attribution;
    }
}
