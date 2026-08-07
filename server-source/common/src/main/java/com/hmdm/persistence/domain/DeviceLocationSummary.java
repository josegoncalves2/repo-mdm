/*
 *
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Copyright (C) 2019 Headwind Solutions LLC (http://h-sms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hmdm.persistence.domain;

/**
 * <p>A row of the last known GPS location reported by a device, read from
 * <code>devices.infojson-&gt;'location'</code> (the agent writes it there on every sync, there is no
 * separate location-history table for the base agent).</p>
 */
public class DeviceLocationSummary {

    private int id;
    private String number;
    private String model;
    private String deviceIp;
    private Double lat;
    private Double lon;
    private Long locationTs;
    private long lastUpdate;
    private String configName;
    private String statusCode;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Long getLocationTs() {
        return locationTs;
    }

    public void setLocationTs(Long locationTs) {
        this.locationTs = locationTs;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * <p>The panel's device.info.deviceIp lookup expects a nested object, not a flat
     * column -- this is a read-only view over {@link #deviceIp} for JSON serialization,
     * it is not mapped by MyBatis (there is no matching setter).</p>
     */
    public java.util.Map<String, String> getInfo() {
        return java.util.Collections.singletonMap("deviceIp", deviceIp);
    }

    /**
     * <p>Alias for {@link #configName} -- the recent-devices widget reads
     * <code>device.configuration</code>, not <code>device.configName</code>.</p>
     */
    public String getConfiguration() {
        return configName;
    }
}
