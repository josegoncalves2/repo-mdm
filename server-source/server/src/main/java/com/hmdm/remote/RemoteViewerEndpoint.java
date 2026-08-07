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

import com.hmdm.persistence.domain.User;
import com.hmdm.rest.filter.AuthFilter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

/**
 * <p>Lado do operador numa sessao de suporte remoto: o navegador recebe a tela do aparelho
 * como video e envia de volta toque, arrasto e digitacao.</p>
 *
 * <p>A autenticacao reaproveita o login do proprio painel. Um handshake WebSocket e' uma
 * requisicao HTTP comum, entao carrega o cookie de sessao; o {@link Configurator} tira o
 * usuario autenticado da {@link HttpSession} no momento do handshake. Fazer isso no
 * handshake, e nao na primeira mensagem, garante que um socket anonimo nunca chegue a ser
 * inscrito num fluxo.</p>
 *
 * <p>Estar logado nao basta. Ver a tela exige {@code device.remote_access.view}; enviar
 * toque e digitacao exige {@code device.remote_access.control}. Sao permissoes diferentes
 * de proposito: acompanhar um atendimento e operar o aparelho de outra pessoa nao sao a
 * mesma responsabilidade.</p>
 */
@ServerEndpoint(value = "/ws/remote/viewer/{number}",
        configurator = RemoteViewerEndpoint.Configurator.class)
public class RemoteViewerEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RemoteViewerEndpoint.class);

    static final String USER_PROPERTY = "hmdm.remote.user";

    /** Teto de texto por comando: um toque e' minusculo, digitacao razoavel tambem. */
    private static final int MAX_TEXT_LENGTH = 4096;

    private String deviceNumber;
    private boolean mayControl;

    public static class Configurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request,
                                    HandshakeResponse response) {
            Object httpSession = request.getHttpSession();
            if (httpSession instanceof HttpSession) {
                Object credentials = ((HttpSession) httpSession).getAttribute(AuthFilter.sessionCredentials);
                if (credentials instanceof User) {
                    config.getUserProperties().put(USER_PROPERTY, credentials);
                }
            }
        }
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config, @PathParam("number") String number) {
        this.deviceNumber = number;
        session.setMaxIdleTimeout(0);
        session.setMaxTextMessageBufferSize(MAX_TEXT_LENGTH * 2);

        final Object user = config.getUserProperties().get(USER_PROPERTY);
        if (!(user instanceof User)) {
            closeWith(session, CloseReason.CloseCodes.VIOLATED_POLICY, "nao autenticado");
            return;
        }
        if (!hasAny((User) user, "edit_devices", "device.remote_access.view", "device.remote_access.control")) {
            logger.warn("Espectador recusado para '{}': {} sem permissao de acesso remoto",
                    number, ((User) user).getLogin());
            closeWith(session, CloseReason.CloseCodes.VIOLATED_POLICY, "sem permissao");
            return;
        }
        this.mayControl = hasAny((User) user, "edit_devices", "device.remote_access.control");

        if (!RemoteSessionHub.getInstance().attachViewer(number, session)) {
            logger.warn("Espectador recusado para '{}': nenhuma sessao aberta no hub", number);
            closeWith(session, CloseReason.CloseCodes.GOING_AWAY, "nenhuma sessao aberta para este aparelho");
        }
    }

    /**
     * <p>Comandos de entrada vindos do painel.</p>
     *
     * <p>Sao validados aqui, e nao apenas no aparelho: o agente confia no servidor, entao o
     * servidor tem de ser o ponto onde uma mensagem malformada para. As coordenadas chegam
     * normalizadas entre 0 e 1, o que mantem o painel independente da resolucao do
     * aparelho e evita que uma rotacao no meio da sessao jogue o toque no lugar errado.</p>
     */
    @OnMessage
    public void onCommand(String message, Session session) {
        if (message == null || message.length() > MAX_TEXT_LENGTH) {
            return;
        }
        if (!mayControl) {
            reject(session, "sem permissao para controlar este aparelho");
            return;
        }
        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type");
            JSONObject command = new JSONObject();
            command.put("type", type);

            switch (type) {
                case "tap":
                case "longpress":
                    command.put("x", unit(json.optDouble("x", -1)));
                    command.put("y", unit(json.optDouble("y", -1)));
                    break;
                case "swipe":
                    command.put("x1", unit(json.optDouble("x1", -1)));
                    command.put("y1", unit(json.optDouble("y1", -1)));
                    command.put("x2", unit(json.optDouble("x2", -1)));
                    command.put("y2", unit(json.optDouble("y2", -1)));
                    command.put("duration", Math.max(20, Math.min(json.optLong("duration", 250), 10_000)));
                    break;
                case "text": {
                    String text = json.optString("text", "");
                    if (text.length() > 500) {
                        reject(session, "texto longo demais");
                        return;
                    }
                    command.put("text", text);
                    break;
                }
                case "key": {
                    String name = json.optString("name", "");
                    // Lista fechada: o agente traduz nome em acao global, e aceitar nome
                    // arbitrario so' aumentaria a superficie sem oferecer nada.
                    if (!name.equals("back") && !name.equals("home")
                            && !name.equals("recents") && !name.equals("notifications")) {
                        reject(session, "tecla desconhecida");
                        return;
                    }
                    command.put("name", name);
                    break;
                }
                default:
                    return;
            }

            if (!RemoteSessionHub.getInstance().relayCommand(deviceNumber, command.toString())) {
                reject(session, "o aparelho nao esta conectado");
            }
        } catch (Exception e) {
            logger.debug("Comando ilegivel do painel para '{}'", deviceNumber, e);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        RemoteSessionHub.getInstance().detachViewer(deviceNumber, session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.debug("Espectador falhou para '{}'", deviceNumber, error);
        RemoteSessionHub.getInstance().detachViewer(deviceNumber, session);
    }

    // =================================================================================================================

    private static double unit(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static void reject(Session session, String reason) {
        try {
            JSONObject json = new JSONObject();
            json.put("type", "input-result");
            json.put("applied", false);
            json.put("reason", reason);
            session.getAsyncRemote().sendText(json.toString());
        } catch (Exception ignored) {
        }
    }

    /**
     * O mesmo teste que o recurso REST aplica, avaliado aqui contra o usuario carregado no
     * socket porque um WebSocket nao tem SecurityContext por requisicao.
     */
    private static boolean hasAny(User user, String... permissions) {
        if (user.getUserRole() == null) {
            return false;
        }
        if (user.getUserRole().isSuperAdmin()) {
            return true;
        }
        if (user.getUserRole().getPermissions() == null) {
            return false;
        }
        return user.getUserRole().getPermissions().stream().anyMatch(granted -> {
            for (String permission : permissions) {
                if (permission.equalsIgnoreCase(granted.getName())) {
                    return true;
                }
            }
            return false;
        });
    }

    private static void closeWith(Session session, CloseReason.CloseCodes code, String reason) {
        try {
            session.close(new CloseReason(code, reason));
        } catch (Exception e) {
            logger.debug("Falha ao fechar um socket de espectador recusado", e);
        }
    }
}
