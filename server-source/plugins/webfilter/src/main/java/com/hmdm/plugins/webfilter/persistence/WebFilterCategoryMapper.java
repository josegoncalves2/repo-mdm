package com.hmdm.plugins.webfilter.persistence;

import javax.sql.DataSource;

public class WebFilterCategoryMapper {

    private final DataSource dataSource;

    public WebFilterCategoryMapper(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}