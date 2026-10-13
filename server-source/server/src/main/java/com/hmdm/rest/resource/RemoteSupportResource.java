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

package com.hmdm.rest.resource;

import com.hmdm.notification.PushService;
import com.hmdm.notification.persistence.domain.PushMessage;
import com.hmdm.persistence.DeviceDAO;
import com.hmdm.persistence.domain.Device;
import com.hmdm.remote.RemoteSessionHub;
import com.hmdm.rest.json.Response;
import com.hmdm.security.SecurityContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Abre e encerra as sessoes de suporte remoto que o operador conduz pelo painel.</p>
 *
 * <p>Abrir uma sessao emite um token de uso unico, entrega-o ao aparelho dentro do push, e
 * deixa uma vaga em {@link RemoteSessionHub} para o uplink de video do agente. Nada e'
 * gravado no diretorio de arquivos: o fluxo e' repassado ao vivo e esquecido.</p>
 *
 * <p>O push vai com um tipo que o launcher oficial nao conhece
 * ({@code remoteScreenStart}). Isso e' proposital: o launcher 6.36 repassa por broadcast
 * todo tipo que nao conhece, e e' assim que o agente de suporte -- um aplicativo separado,
 * instalado como app gerenciado -- recebe o chamado sem que uma linha do launcher precise
 * mudar.</p>
 */
@Api(tags = {"Remote support"}, authorizations = {@Authorization("Bearer Token")})
@Singleton
@Path("/private/remote-support")
public class RemoteSupportResource {

    private static final Logger log = LoggerFactory.getLogger(RemoteSupportResource.class);

    private static final String PERMISSION_VIEW = "device.remote_access.view";
    private static final String PERMISSION_CONTROL = "device.remote_access.control";

    /**
     * Parametros do encoder, enviados junto com o chamado em vez de compilados no agente,
     * para que o fluxo possa ser ajustado para um link lento sem gerar um APK novo.
     */
    private static final int DEFAULT_FPS = 20;
    private static final int DEFAULT_BITRATE = 6_000_000;
    private static final int DEFAULT_MAX_WIDTH = 1280;

    /**
     * Quanto uma requisicao de quadros espera antes de voltar vazia. Longo o bastante para
     * nao virar polling agressivo, curto o bastante para nao esbarrar no timeout do proxy.
     */
    private static final long POLL_WAIT_MS = 20_000L;

    /**
     * <p>Mesmo limiar que {@code DeviceView.getOnlineThresholdSec()} usa para pintar o status
     * "online"/"offline" na lista de aparelhos (5 minutos, porque {@code lastUpdate} agora e'
     * tocado a cada consulta do long polling). Duplicado aqui -- e nao chamado dali -- porque
     * {@code DeviceView} fica fora dos arquivos que esta mudanca tem autorizacao para tocar, e
     * {@link DeviceDAO#getDeviceById} nao preenche {@code statusCode} (a coluna so' e' calculada
     * nas consultas de listagem). Se o limiar de "online" mudar num lugar, tem de mudar no
     * outro -- ambos citam o mesmo numero de proposito, para o `grep` achar os dois.</p>
     */
    private static final long ONLINE_THRESHOLD_MS = 5 * 60_000L;

    private DeviceDAO deviceDAO;
    private PushService pushService;
    private String baseUrl;

    /**
     * <p>A constructor required by Swagger.</p>
     */
    public RemoteSupportResource() {
    }

    @Inject
    public RemoteSupportResource(DeviceDAO deviceDAO,
                                 PushService pushService,
                                 @Named("base.url") String baseUrl) {
        this.deviceDAO = deviceDAO;
        this.pushService = pushService;
        this.baseUrl = baseUrl;
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Start a remote support session",
            notes = "Reserves a relay slot, mints the one-time token the agent must present, and calls the device. "
                    + "Accepted even when the device is currently offline: the call is queued as a pending push and "
                    + "delivered whenever the device syncs again (see PENDING_OFFLINE_TIMEOUT_MS in RemoteSessionHub)."
    )
    @POST
    @Path("/{id}/start")
    @Produces(MediaType.APPLICATION_JSON)
    public Response start(@PathParam("id") @ApiParam("Device ID") Integer id) {
        try {
            final Device device = this.deviceDAO.getDeviceById(id);
            if (device == null) {
                return Response.DEVICE_NOT_FOUND_ERROR();
            }
            if (!mayControl()) {
                log.error("Tentativa nao autorizada de abrir suporte remoto no aparelho #{}", id);
                return Response.PERMISSION_DENIED();
            }

            /*
             * Item 3 da queixa: o painel nao pode mais desistir so' porque o aparelho esta'
             * offline agora. O pedido e' aceito de qualquer forma -- a vaga fica reservada no
             * hub com um prazo mais longo (abaixo), e o push sai do mesmo jeito de sempre. O
             * mecanismo de push generico (PushSenderPolling -> NotificationDAO.send) ja' grava
             * em `pendingpushes` quando o aparelho nao esta' com uma consulta de long polling
             * em aberto, e entrega assim que ele voltar a sincronizar -- isto nao e' codigo
             * novo, e' o mesmo caminho que qualquer outro push deste servidor ja' usa; o que
             * faltava era o hub nao descartar a sessao/token antes disso acontecer.
             */
            boolean deviceOnline = isDeviceOnline(device);

            RemoteSessionHub.RemoteSession session =
                    RemoteSessionHub.getInstance().open(device.getId(), device.getNumber(), deviceOnline);

            JSONObject payload = new JSONObject();
            payload.put("url", agentSocketUrl(device.getNumber()));
            payload.put("token", session.getToken());
            payload.put("format", "h264");
            payload.put("fps", DEFAULT_FPS);
            payload.put("bitrate", DEFAULT_BITRATE);
            payload.put("maxWidth", DEFAULT_MAX_WIDTH);

            PushMessage message = new PushMessage();
            message.setDeviceId(device.getId());
            message.setMessageType("remoteScreenStart");
            message.setPayload(payload.toString());
            this.pushService.send(message);

            Map<String, Object> result = new HashMap<>();
            result.put("deviceId", device.getId());
            result.put("deviceNumber", device.getNumber());
            result.put("socket", viewerSocketPath(device.getNumber()));
            // O painel usa isto para mostrar "aguardando o aparelho conectar" com o horario do
            // pedido, em vez de nao fazer nada quando o aparelho esta' offline no clique.
            result.put("pending", !deviceOnline);
            result.put("requestedAt", session.getCreatedAt());
            log.info("Suporte remoto chamado para o aparelho '{}' (online no pedido={})",
                    device.getNumber(), deviceOnline);
            return Response.OK(result);
        } catch (Exception e) {
            log.error("Falha ao abrir suporte remoto no aparelho #{}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Stop a remote support session",
            notes = "Tells the device to stop capturing and drops the relay slot with every viewer attached to it."
    )
    @POST
    @Path("/{id}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stop(@PathParam("id") @ApiParam("Device ID") Integer id) {
        try {
            final Device device = this.deviceDAO.getDeviceById(id);
            if (device == null) {
                return Response.DEVICE_NOT_FOUND_ERROR();
            }
            if (!mayControl()) {
                return Response.PERMISSION_DENIED();
            }

            PushMessage message = new PushMessage();
            message.setDeviceId(device.getId());
            message.setMessageType("remoteScreenStop");
            this.pushService.send(message);

            // A vaga cai primeiro: o operador pediu para parar, entao o espectador tem de
            // parar de receber agora, chegue ou nao o push a um aparelho ja offline.
            RemoteSessionHub.getInstance().close(device.getNumber());
            return Response.OK();
        } catch (Exception e) {
            log.error("Falha ao encerrar suporte remoto no aparelho #{}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Remote support session state",
            notes = "Reports whether the device is streaming, at what resolution, and whether input is available."
    )
    @GET
    @Path("/{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response status(@PathParam("id") @ApiParam("Device ID") Integer id) {
        try {
            final Device device = this.deviceDAO.getDeviceById(id);
            if (device == null) {
                return Response.DEVICE_NOT_FOUND_ERROR();
            }
            if (!mayView()) {
                return Response.PERMISSION_DENIED();
            }

            RemoteSessionHub.RemoteSession session =
                    RemoteSessionHub.getInstance().find(device.getNumber());

            Map<String, Object> result = new HashMap<>();
            result.put("deviceId", device.getId());
            result.put("deviceNumber", device.getNumber());
            result.put("socket", viewerSocketPath(device.getNumber()));
            result.put("open", session != null);
            result.put("streaming", session != null && session.isStreaming());
            /*
             * Item 4: e' assim que o painel descobre, ao carregar (F5, troca de aba, ou um
             * navegador novo), que ja' existe uma sessao viva para este aparelho e deve
             * REATAR em vez de abrir uma sessao nova. "pending" cobre tanto "o agente ainda
             * nao conectou" quanto "o pedido esta' esperando o aparelho sincronizar" -- dos
             * dois lados o painel mostra o mesmo estado de espera, com requestedAt.
             */
            result.put("pending", session != null && !session.isStreaming());
            result.put("requestedAt", session == null ? null : session.getCreatedAt());
            result.put("input", session != null && session.isInputAvailable());
            result.put("width", session == null ? 0 : session.getWidth());
            result.put("height", session == null ? 0 : session.getHeight());
            result.put("viewers", session == null ? 0 : session.getViewerCount());
            result.put("frames", session == null ? 0L : session.getFrames());
            result.put("bytes", session == null ? 0L : session.getBytes());
            result.put("mayControl", mayControl());
            return Response.OK(result);
        } catch (Exception e) {
            log.error("Falha ao ler o estado do suporte remoto do aparelho #{}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Poll screen frames over HTTP",
            notes = "Long-polling transport for the live screen, used when a reverse proxy does not forward "
                    + "WebSocket upgrades."
    )
    @GET
    @Path("/{id}/frames")
    @Produces(MediaType.APPLICATION_JSON)
    public Response frames(@PathParam("id") @ApiParam("Device ID") Integer id,
                           @QueryParam("since") Long since) {
        try {
            final Device device = this.deviceDAO.getDeviceById(id);
            if (device == null) {
                return Response.DEVICE_NOT_FOUND_ERROR();
            }
            if (!mayView()) {
                return Response.PERMISSION_DENIED();
            }

            RemoteSessionHub.PolledFrames polled = RemoteSessionHub.getInstance()
                    .poll(device.getNumber(), since == null ? 0L : since, POLL_WAIT_MS);

            java.util.List<String> encoded = new java.util.ArrayList<>(polled.frames.size());
            for (byte[] frame : polled.frames) {
                encoded.add(java.util.Base64.getEncoder().encodeToString(frame));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("cursor", polled.cursor);
            result.put("streaming", polled.streaming);
            result.put("width", polled.width);
            result.put("height", polled.height);
            result.put("input", polled.input);
            result.put("frames", encoded);
            return Response.OK(result);
        } catch (Exception e) {
            log.error("Falha ao entregar quadros do aparelho #{}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Send an input command over HTTP",
            notes = "Same commands the viewer socket accepts, for the HTTP transport."
    )
    @POST
    @Path("/{id}/input")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response input(@PathParam("id") @ApiParam("Device ID") Integer id,
                          Map<String, Object> command) {
        try {
            final Device device = this.deviceDAO.getDeviceById(id);
            if (device == null) {
                return Response.DEVICE_NOT_FOUND_ERROR();
            }
            if (!mayControl()) {
                return Response.PERMISSION_DENIED();
            }
            if (command == null || command.get("type") == null) {
                return Response.ERROR("error.remote.command.unsupported");
            }
            // A validacao dos limites e a lista fechada de teclas ficam no agente e no
            // endpoint WebSocket; aqui repassamos o comando ja normalizado pelo painel.
            if (!RemoteSessionHub.getInstance().relayCommand(device.getNumber(),
                    new JSONObject(command).toString())) {
                return Response.ERROR("error.remote.command.unsupported");
            }
            return Response.OK();
        } catch (Exception e) {
            log.error("Falha ao enviar entrada ao aparelho #{}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =================================================================================================================

    private boolean mayControl() {
        return SecurityContext.get().hasPermission("edit_devices")
                || SecurityContext.get().hasPermission(PERMISSION_CONTROL);
    }

    private boolean mayView() {
        return mayControl() || SecurityContext.get().hasPermission(PERMISSION_VIEW);
    }

    /**
     * <p>Mesma regra que a lista de aparelhos usa para pintar "ONLINE"/"OFFLINE" (ver
     * {@code ONLINE_THRESHOLD_MS} acima), aplicada aqui para decidir se o pedido de suporte
     * remoto entra direto ou fica pendente esperando o aparelho sincronizar.</p>
     */
    private boolean isDeviceOnline(Device device) {
        Long lastUpdate = device.getLastUpdate();
        return lastUpdate != null && lastUpdate > 0
                && (System.currentTimeMillis() - lastUpdate) < ONLINE_THRESHOLD_MS;
    }

    /**
     * <p>URL absoluta que o agente disca, derivada da URL base configurada, para que o
     * aparelho procure o mesmo servidor com o qual ja sincroniza.</p>
     *
     * <p>Painel em https tem de produzir uplink em wss: um WebSocket em texto claro
     * carregaria a tela do aparelho aberta por uma rede que nao controlamos.</p>
     */
    private String agentSocketUrl(String number) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.startsWith("https://")) {
            base = "wss://" + base.substring("https://".length());
        } else if (base.startsWith("http://")) {
            base = "ws://" + base.substring("http://".length());
        }
        return base + "/ws/remote/agent/" + urlEncode(number);
    }

    /** Relativo de proposito: o navegador abre contra a origem de onde ja foi servido. */
    private String viewerSocketPath(String number) {
        return "ws/remote/viewer/" + urlEncode(number);
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
}
