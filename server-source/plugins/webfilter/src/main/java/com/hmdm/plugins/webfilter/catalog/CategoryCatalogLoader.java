package com.hmdm.plugins.webfilter.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CategoryCatalogLoader {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static CategoryCatalog load() {
        try (InputStream is = CategoryCatalogLoader.class.getResourceAsStream("/webfilter-catalog.json")) {
            if (is == null) {
                throw new RuntimeException("webfilter-catalog.json not found in classpath");
            }
            return mapper.readValue(is, CategoryCatalog.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load category catalog", e);
        }
    }

    public static void validate(CategoryCatalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("Category catalog is null");
        }
        List<CategoryCatalog.Category> categories = catalog.getCategories();
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("Category catalog has no categories");
        }
        long uniqueIds = categories.stream().map(CategoryCatalog.Category::getId).distinct().count();
        if (uniqueIds != categories.size()) {
            throw new IllegalArgumentException("Category catalog has duplicate category IDs");
        }
        boolean dohFound = categories.stream().anyMatch(c -> "doh".equals(c.getId()));
        if (!dohFound) {
            throw new IllegalArgumentException("Category catalog must contain 'doh' category");
        }
        if (catalog.getSiteSources() == null) {
            throw new IllegalArgumentException("Category catalog siteSources is null");
        }
        if (catalog.getAppSources() == null) {
            throw new IllegalArgumentException("Category catalog appSources is null");
        }
    }
}