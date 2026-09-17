package com.hmdm.plugins.webfilter.service;

import com.hmdm.plugins.webfilter.persistence.WebFilterCategoryMapper;

public class AppCategoryService {

    private final WebFilterCategoryMapper categoryMapper;

    public AppCategoryService(WebFilterCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }
}