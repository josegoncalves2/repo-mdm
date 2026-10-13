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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * <p>Lado do aparelho numa sessao de suporte remoto: o agente com.hwmdm.remote conecta aqui,
 * empurra o video da tela e recebe de volta os comandos de toque e digitacao.</p>
 *
 * <p>O agente nao e' um navegador e nao tem sessao de painel, entao ele se autentica com o
 * token de uso unico que o servidor emitiu quando um operador autorizado abriu a sessao, e
 * que so' foi entregue dentro do push enderecado aquele aparelho. Conexao sem token
 * correspondente e' fechada antes de um unico byte ser repassado.</p>
 */
@ServerEndpoint("/ws/remote/agent/{number}")
public class RemoteAgentEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RemoteAgentEndpoint.class);

    /**
     * Um quadro codificado de tablet tem dezenas de KB; o teto so' limita o que um agente
     * defeituoso ou hostil poderia empurrar de uma vez para o buffer do relay.
     */
    private static final int MAX_FRAME_BYTES = 4 * 1024 * 1024;

    private String deviceNumber;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config, @PathParam("number") String number) {
        this.deviceNumber = number;
        session.setMaxBinaryMessageBufferSize(MAX_FRAME_BYTES);
        session.setMaxTextMessageBufferSize(16 * 1024);
        // Agente que para de enviar e' agente cujo aparelho caiu da rede. O timeout ocioso
        // e' o que libera a sessao nesse caso, ja que nenhum frame de fechamento chega.
        session.setMaxIdleTimeout(60_000L);

        final String token = firstValue(session.getRequestParameterMap(), "token");
        if (RemoteSessionHub.getInstance().attachAgent(number, token, session) == null) {
            closeWith(session, CloseReason.CloseCodes.VIOLATED_POLICY, "sessao nao autorizada");
        }
    }

    @OnMessage
    public void onFrame(ByteBuffer frame, Session session) {
        if (frame == null || !frame.hasRemaining()) {
            return;
        }
        byte[] payload = new byte[frame.remaining()];
        frame.get(payload);
        RemoteSessionHub.getInstance().relayFrame(deviceNumber, payload);
    }

    @OnMessage
    public void onControl(String message, Session session) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type");

            if ("stream".equals(type)) {
                RemoteSessionHub.getInstance().describeStream(deviceNumber,
                        json.optInt("width", 0),
                        json.optInt("height", 0),
                        json.optBoolean("input", false));
            } else if ("input-result".equals(type)) {
                // O agente informa se o toque pegou. Repassar isso ao painel e' o que evita
                // o operador clicar numa imagem que nao responde sem saber por que.
                RemoteSessionHub.getInstance().relayToViewers(deviceNumber, message);
            } else if ("stop".equals(type)) {
                logger.info("Agente encerrou a sessao de '{}': {}",
                        deviceNumber, json.optString("reason", "sem motivo informado"));
                RemoteSessionHub.getInstance().close(deviceNumber);
            }
        } catch (Exception e) {
            logger.warn("Mensagem de controle ilegivel do aparelho '{}'", deviceNumber, e);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        RemoteSessionHub.getInstance().detachAgent(deviceNumber, session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.warn("Uplink falhou para o aparelho '{}'", deviceNumber, error);
        RemoteSessionHub.getInstance().detachAgent(deviceNumber, session);
    }

    private static String firstValue(Map<String, List<String>> parameters, String name) {
        if (parameters == null) {
            return null;
        }
        List<String> values = parameters.get(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    private static void closeWith(Session session, CloseReason.CloseCodes code, String reason) {
        try {
            session.close(new CloseReason(code, reason));
        } catch (Exception e) {
            logger.debug("Falha ao fechar um socket de agente recusado", e);
        }
    }
}
