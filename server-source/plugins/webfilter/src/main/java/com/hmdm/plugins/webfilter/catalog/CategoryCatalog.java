package com.hmdm.plugins.webfilter.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class CategoryCatalog {
    private final List<Category> categories;
    private final Map<String, List<String>> siteSources;
    private final Map<String, List<String>> appSources;

    @JsonCreator
    public CategoryCatalog(
            @JsonProperty("categories") List<Category> categories,
            @JsonProperty("siteSources") Map<String, List<String>> siteSources,
            @JsonProperty("appSources") Map<String, List<String>> appSources) {
        this.categories = categories;
        this.siteSources = siteSources;
        this.appSources = appSources;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Map<String, List<String>> getSiteSources() {
        return siteSources;
    }

    public Map<String, List<String>> getAppSources() {
        return appSources;
    }

    public static class Category {
        private final String id;
        private final String name;
        private final boolean required;

        @JsonCreator
        public Category(
                @JsonProperty("id") String id,
                @JsonProperty("name") String name,
                @JsonProperty("required") boolean required) {
            this.id = id;
            this.name = name;
            this.required = required;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isRequired() {
            return required;
        }
    }
}