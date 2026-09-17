package com.hmdm.plugins.webfilter.persistence;

import javax.sql.DataSource;

public class WebFilterLockedHistoryMapper {

    private final DataSource dataSource;

    public WebFilterLockedHistoryMapper(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}