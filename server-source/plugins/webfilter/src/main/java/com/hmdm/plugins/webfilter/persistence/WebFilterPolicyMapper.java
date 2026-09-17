package com.hmdm.plugins.webfilter.persistence;

import javax.sql.DataSource;

public class WebFilterPolicyMapper {

    private final DataSource dataSource;

    public WebFilterPolicyMapper(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}