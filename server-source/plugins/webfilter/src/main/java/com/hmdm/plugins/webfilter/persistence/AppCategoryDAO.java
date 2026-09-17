package com.hmdm.plugins.webfilter.persistence;

import javax.sql.DataSource;

public class AppCategoryDAO {

    private final DataSource dataSource;

    public AppCategoryDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}