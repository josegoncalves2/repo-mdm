package com.hmdm.plugins.webfilter.persistence;

import javax.sql.DataSource;

public class WebFilterEntryMapper {

    private final DataSource dataSource;

    public WebFilterEntryMapper(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}