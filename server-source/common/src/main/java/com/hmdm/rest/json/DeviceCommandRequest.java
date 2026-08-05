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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Map;

/**
 * <p>A remote command asked for by the control panel.</p>
 *
 * <p>The action is a logical name from {@link com.hmdm.service.RemoteCommand}, not a push
 * message type: the mapping from one to the other belongs to the server, so the browser
 * never has to know the wire protocol spoken to the device agent.</p>
 */
@ApiModel(description = "A remote command to be delivered to a device")
public class DeviceCommandRequest implements Serializable {

    private static final long serialVersionUID = -8098765127716400913L;

    @ApiModelProperty("Logical command name, e.g. lock_screen")
    private String action;

    @ApiModelProperty("Command parameters, if the command takes any")
    private Map<String, Object> params;

    public DeviceCommandRequest() {
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "DeviceCommandRequest{" +
                "action='" + action + '\'' +
                ", params=" + params +
                '}';
    }
}
