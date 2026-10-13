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

package com.hmdm.rest.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

/**
 * <p>A DTO carrying the result of a database restore operation.</p>
 */
@ApiModel(description = "Result of a database restore operation")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestoreResult {

    private String safetyBackup;

    public RestoreResult() {
    }

    public RestoreResult(String safetyBackup) {
        this.safetyBackup = safetyBackup;
    }

    public String getSafetyBackup() {
        return safetyBackup;
    }

    public void setSafetyBackup(String safetyBackup) {
        this.safetyBackup = safetyBackup;
    }
}
