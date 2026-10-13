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

package com.hmdm.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

/**
 * <p>Registra os endpoints WebSocket explicitamente, em vez de confiar que o container
 * encontre as anotacoes.</p>
 *
 * <p>Isto nao e' precaucao teorica. O <code>web.xml</code> desta aplicacao ainda declara a
 * DTD de Servlet 2.3, e na implantacao anterior o log provou que o Tomcat <b>nao</b> varreu
 * as anotacoes: as duas linhas "endpoint registrado" vieram deste listener. Sem ele o
 * painel abriria o socket, o container responderia 404, e nao haveria nada no log
 * explicando.</p>
 */
public class RemoteEndpointDeployer implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(RemoteEndpointDeployer.class);

    private static final String AGENT_PATH = "/ws/remote/agent/{number}";
    private static final String VIEWER_PATH = "/ws/remote/viewer/{number}";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Object attribute = event.getServletContext()
                .getAttribute("javax.websocket.server.ServerContainer");
        if (!(attribute instanceof ServerContainer)) {
            logger.error("Suporte remoto indisponivel: este container nao expoe um "
                    + "ServerContainer de WebSocket. O visualizador nao vai conectar.");
            return;
        }
        ServerContainer container = (ServerContainer) attribute;

        register(container, ServerEndpointConfig.Builder
                .create(RemoteAgentEndpoint.class, AGENT_PATH)
                .build());
        register(container, ServerEndpointConfig.Builder
                .create(RemoteViewerEndpoint.class, VIEWER_PATH)
                .configurator(new RemoteViewerEndpoint.Configurator())
                .build());
    }

    private void register(ServerContainer container, ServerEndpointConfig config) {
        try {
            container.addEndpoint(config);
            logger.info("Endpoint de suporte remoto registrado em {}", config.getPath());
        } catch (Exception e) {
            // Ja registrado pela anotacao: caso normal num container que varreu as classes.
            logger.debug("Endpoint {} ja havia sido registrado pelo container", config.getPath(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}
