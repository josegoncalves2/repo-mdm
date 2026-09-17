package com.hmdm.plugins.webfilter.persistence;

import javax.sql.DataSource;

public class WebFilterSettingsMapper {

    private final DataSource dataSource;

    public WebFilterSettingsMapper(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}