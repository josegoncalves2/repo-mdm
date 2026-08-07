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

package com.hmdm.guice.module;

import com.google.inject.servlet.ServletModule;
import com.hmdm.rest.filter.HstsFilter;
import com.hmdm.rest.filter.PublicIPFilter;
import com.hmdm.rest.resource.*;
import com.hmdm.security.jwt.rest.JWTAuthResource;

public class PublicRestModule extends ServletModule {
    public PublicRestModule() {
    }

    protected void configureServlets() {
        this.filter("*").through(HstsFilter.class);
        this.filter("/rest/public/*").through(PublicIPFilter.class);
        // Quem chama e' o aparelho, nao o painel, entao vale o mesmo filtro de IP dos
        // demais enderecos de aparelho -- ainda que o caminho nao comece por /rest/public.
        this.filter("/rest/plugins/devicereset/public/*").through(PublicIPFilter.class);
        this.serve("/files/*").with(DownloadFilesServlet.class);
        this.bind(AuthResource.class);
        this.bind(JWTAuthResource.class);
        this.bind(PublicResource.class);
        this.bind(SyncResource.class);
        this.bind(DeviceResetResource.class);
        this.bind(PublicFilesResource.class);
        this.bind(QRCodeResource.class);
        this.bind(StatsResource.class);
    }
}