package com.hmdm.plugins.webfilter.persistence;

import javax.sql.DataSource;

public class AppCategoryMapper {

    private final DataSource dataSource;

    public AppCategoryMapper(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}