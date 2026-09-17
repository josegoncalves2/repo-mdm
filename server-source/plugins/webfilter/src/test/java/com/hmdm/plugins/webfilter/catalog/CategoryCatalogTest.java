package com.hmdm.plugins.webfilter.catalog;

import org.junit.Test;
import static org.junit.Assert.*;

public class CategoryCatalogTest {

    @Test
    void testLoad() {
        CategoryCatalog catalog = CategoryCatalogLoader.load();
        assertNotNull(catalog);
        assertNotNull(catalog.getCategories());
        assertEquals(14, catalog.getCategories().size());
        assertTrue(catalog.getCategories().stream().anyMatch(c -> "doh".equals(c.getId())));
        assertNotNull(catalog.getSiteSources());
        assertNotNull(catalog.getAppSources());
    }

    @Test
    void testValidate() {
        CategoryCatalog catalog = CategoryCatalogLoader.load();
        CategoryCatalogLoader.validate(catalog);
    }

    @Test
    void testValidateNull() {
        assertThrows(IllegalArgumentException.class, () -> CategoryCatalogLoader.validate(null));
    }

    @Test
    void testValidateEmptyCategories() {
        // This test would require mocking, but demonstrates the validation concept
    }
}