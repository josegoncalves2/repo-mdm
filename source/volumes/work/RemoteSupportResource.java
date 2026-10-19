/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hmdm.notification.PushService
 *  com.hmdm.notification.persistence.domain.PushMessage
 *  com.hmdm.persistence.DeviceDAO
 *  com.hmdm.persistence.domain.Device
 *  com.hmdm.rest.json.Response
 *  com.hmdm.security.SecurityContext
 *  io.swagger.annotations.Api
 *  io.swagger.annotations.ApiOperation
 *  io.swagger.annotations.ApiParam
 *  io.swagger.annotations.Authorization
 *  javax.inject.Inject
 *  javax.inject.Named
 *  javax.inject.Singleton
 *  javax.ws.rs.Consumes
 *  javax.ws.rs.GET
 *  javax.ws.rs.POST
 *  javax.ws.rs.Path
 *  javax.ws.rs.PathParam
 *  javax.ws.rs.Produces
 *  javax.ws.rs.QueryParam
 *  org.json.JSONObject
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.hmdm.rest.resource;

import com.hmdm.notification.PushService;
import com.hmdm.notification.persistence.domain.PushMessage;
import com.hmdm.notification.persistence.mapper.NotificationMapper;
import java.util.List;
import com.hmdm.persistence.DeviceDAO;
import com.hmdm.persistence.domain.Device;
import com.hmdm.remote.RemoteSessionHub;
import com.hmdm.rest.json.Response;
import com.hmdm.security.SecurityContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Api(tags={"Remote support"}, authorizations={@Authorization(value="Bearer Token")})
@Singleton
@Path(value="/private/remote-support")
public class RemoteSupportResource {
    private static final Logger log = LoggerFactory.getLogger(RemoteSupportResource.class);
    private static final String PERMISSION_VIEW = "device.remote_access.view";
    private static final String PERMISSION_CONTROL = "device.remote_access.control";
    private static final int DEFAULT_FPS = 20;
    private static final int DEFAULT_BITRATE = 6000000;
    private static final int DEFAULT_MAX_WIDTH = 1280;
    private static final long POLL_WAIT_MS = 20000L;
    private static final long ONLINE_THRESHOLD_MS = 300000L;
    private DeviceDAO deviceDAO;
    private PushService pushService;
    private NotificationMapper notificationMapper;
    private String baseUrl;

    public RemoteSupportResource() {
    }

    @Inject
    public RemoteSupportResource(DeviceDAO deviceDAO, PushService pushService, NotificationMapper notificationMapper, @Named(value="base.url") String baseUrl) {
        this.notificationMapper = notificationMapper;
        this.deviceDAO = deviceDAO;
        this.pushService = pushService;
        this.baseUrl = baseUrl;
    }

    @ApiOperation(value="Start a remote support session", notes="Reserves a relay slot, mints the one-time token the agent must present, and calls the device. Accepted even when the device is currently offline: the call is queued as a pending push and delivered whenever the device syncs again (see PENDING_OFFLINE_TIMEOUT_MS in RemoteSessionHub).")
    @POST
    @Path(value="/{id}/start")
    @Produces(value={"application/json"})
    public Response start(@PathParam(value="id") @ApiParam(value="Device ID") Integer id) {
        try {
            Device device = this.deviceDAO.getDeviceById(id);
            if (device == null) {
                return Response.DEVICE_NOT_FOUND_ERROR();
            }
            if (!this.mayControl()) {
                log.error("Tentativa nao autorizada de abrir suporte remoto no aparelho #{}", (Object)id);
                return Response.PERMISSION_DENIED();
            }
            boolean deviceOnline = this.isDeviceOnline(device);
            RemoteSessionHub.RemoteSession session = RemoteSessionHub.getInstance().open(device.getId(), device.getNumber(), deviceOnline);
            JSONObject payload = new JSONObject();
            payload.put("url", (Object)this.agentSocketUrl(device.getNumber()));
            payload.put("token", (Object)session.getToken());
            payload.put("format", (Object)"h264");
            payload.put("fps", 20);
            payload.put("bitrate", 6000000);
            payload.put("maxWidth", 1280);
            payload.put("issuedAt", System.currentTimeMillis());
            this.discardPendingRemoteCalls(device.getId());
            PushMessage message = new PushMessage();
            message.setDeviceId(device.getId().intValue());
            message.setMessageType("remoteScreenStart");
            message.setPayload(payload.toString());
            this.pushService.send(message);
            HashMap<String, Object> result = new HashMap<String, Object>();
            result.put("deviceId", device.getId());
            result.put("deviceNumber", device.getNumber());
            result.put("socket", this.viewerSocketPath(device.getNumber()));
            result.put("pending", !deviceOnline);
            result.put("requestedAt", session.getCreatedAt());
            log.info("Suporte remoto chamado para o aparelho '{}' (online no pedido={})", (Object)device.getNumber(), (Object)deviceOnline);
            return Response.OK(result);
        }
        catch (Exception e) {
            log.error("Falha ao abrir suporte remoto no aparelho #{}", (Object)id, (Object)e);
            return Response.INTERNAL_ERROR();
        }
    }

    @ApiOperation(value="Stop a remote support session", notes="Tells the device to stop capturing and drops the relay slot with every viewer attached to it.")
    @POST
    @Path(value="/{id}/stop")
    @Produces(value={"application/json"})
    public Response stop(@PathParam(value="id") @ApiParam(value="Device ID") Integer id) {
        try {
            Device device = this.deviceDAO.getDeviceById(id);
            if (device == null) {
                return Response.DEVICE_NOT_FOUND_ERROR();
            }
            if (!this.mayControl()) {
                return Response.PERMISSION_DENIED();
            }
            PushMessage message = new PushMessage();
            message.setDeviceId(device.getId().intValue());
            message.setMessageType("remoteScreenStop");
            this.discardPendingRemoteCalls(device.getId());
            this.pushService.send(message);
            RemoteSessionHub.getInstance().close(device.getNumber());
            return Response.OK();
        }
        catch (Exception e) {
            log.error("Falha ao encerrar suporte remoto no aparelho #{}", (Object)id, (Object)e);
            return Response.INTERNAL_ERROR();
        }
    }

    @ApiOperation(value="Remote support session state", notes="Reports whether the device is streaming, at what resolution, and whether input is available.")
    @GET
    @Path(value="/{id}/status")
    @Produces(value={"application/json"})
    public Response status(@PathParam(value="id") @ApiParam(value="Device ID") Integer id) {
        try {
            Device device = this.deviceDAO.getDeviceById(id);
            if (device == null) {
                return Response.DEVICE_NOT_FOUND_ERROR();
            }
            if (!this.mayView()) {
                return Response.PERMISSION_DENIED();
            }
            RemoteSessionHub.RemoteSession session = RemoteSessionHub.getInstance().find(device.getNumber());
            HashMap<String, Object> result = new HashMap<String, Object>();
            result.put("deviceId", device.getId());
            result.put("deviceNumber", device.getNumber());
            result.put("socket", this.viewerSocketPath(device.getNumber()));
            result.put("open", session != null);
            result.put("streaming", session != null && session.isStreaming());
            result.put("pending", session != null && !session.isStreaming());
            result.put("requestedAt", session == null ? null : Long.valueOf(session.getCreatedAt()));
            result.put("input", session != null && session.isInputAvailable());
            result.put("width", session == null ? 0 : session.getWidth());
            result.put("height", session == null ? 0 : session.getHeight());
            result.put("viewers", session == null ? 0 : session.getViewerCount());
            result.put("frames", session == null ? 0L : session.getFrames());
            result.put("bytes", session == null ? 0L : session.getBytes());
            result.put("mayControl", this.mayControl());
            return Response.OK(result);
        }
        catch (Exception e) {
            log.error("Falha ao ler o estado do suporte remoto do aparelho #{}", (Object)id, (Object)e);
            return Response.INTERNAL_ERROR();
        }
    }

    @ApiOperation(value="Poll screen frames over HTTP", notes="Long-polling transport for the live screen, used when a reverse proxy does not forward WebSocket upgrades.")
    @GET
    @Path(value="/{id}/frames")
    @Produces(value={"application/json"})
    public Response frames(@PathParam(value="id") @ApiParam(value="Device ID") Integer id, @QueryParam(value="since") Long since) {
        try {
            Device device = this.deviceDAO.getDeviceById(id);
            if (device == null) {
                return Response.DEVICE_NOT_FOUND_ERROR();
            }
            if (!this.mayView()) {
                return Response.PERMISSION_DENIED();
            }
            RemoteSessionHub.PolledFrames polled = RemoteSessionHub.getInstance().poll(device.getNumber(), since == null ? 0L : since, 20000L);
            ArrayList<String> encoded = new ArrayList<String>(polled.frames.size());
            for (byte[] frame : polled.frames) {
                encoded.add(Base64.getEncoder().encodeToString(frame));
            }
            HashMap<String, Object> result = new HashMap<String, Object>();
            result.put("cursor", polled.cursor);
            result.put("streaming", polled.streaming);
            result.put("width", polled.width);
            result.put("height", polled.height);
            result.put("input", polled.input);
            result.put("frames", encoded);
            return Response.OK(result);
        }
        catch (Exception e) {
            log.error("Falha ao entregar quadros do aparelho #{}", (Object)id, (Object)e);
            return Response.INTERNAL_ERROR();
        }
    }

    @ApiOperation(value="Send an input command over HTTP", notes="Same commands the viewer socket accepts, for the HTTP transport.")
    @POST
    @Path(value="/{id}/input")
    @Consumes(value={"application/json"})
    @Produces(value={"application/json"})
    public Response input(@PathParam(value="id") @ApiParam(value="Device ID") Integer id, Map<String, Object> command) {
        try {
            Device device = this.deviceDAO.getDeviceById(id);
            if (device == null) {
                return Response.DEVICE_NOT_FOUND_ERROR();
            }
            if (!this.mayControl()) {
                return Response.PERMISSION_DENIED();
            }
            if (command == null || command.get("type") == null) {
                return Response.ERROR((String)"error.remote.command.unsupported");
            }
            if (!RemoteSessionHub.getInstance().relayCommand(device.getNumber(), new JSONObject(command).toString())) {
                return Response.ERROR((String)"error.remote.command.unsupported");
            }
            return Response.OK();
        }
        catch (Exception e) {
            log.error("Falha ao enviar entrada ao aparelho #{}", (Object)id, (Object)e);
            return Response.INTERNAL_ERROR();
        }
    }

    private boolean mayControl() {
        return SecurityContext.get().hasPermission("edit_devices") || SecurityContext.get().hasPermission(PERMISSION_CONTROL);
    }

    private boolean mayView() {
        return this.mayControl() || SecurityContext.get().hasPermission(PERMISSION_VIEW);
    }

    /*
     * Com push por polling o aparelho pode ficar minutos sem buscar mensagens; quando busca,
     * recebia de uma vez todos os start/stop acumulados, cada start com um token diferente,
     * e cada um abria um pedido de consentimento que derrubava o anterior. Um chamado novo
     * torna os anteriores obsoletos: marcamos como entregues para nunca chegarem ao aparelho.
     */
    private void discardPendingRemoteCalls(int deviceId) {
        try {
            List<PushMessage> pending = this.notificationMapper.getPendingMessagesById(deviceId);
            ArrayList<Integer> stale = new ArrayList<Integer>();
            if (pending != null) {
                for (PushMessage m : pending) {
                    String type = m.getMessageType();
                    if ("remoteScreenStart".equals(type) || "remoteScreenStop".equals(type)) {
                        stale.add(m.getId());
                    }
                }
            }
            if (!stale.isEmpty()) {
                this.notificationMapper.markMessagesAsDelivered(stale);
                log.info("Descartados {} chamados remotos pendentes do aparelho #{}", (Object)stale.size(), (Object)deviceId);
            }
        }
        catch (Exception e) {
            log.warn("Falha ao descartar chamados remotos pendentes do aparelho #{}", (Object)deviceId, (Object)e);
        }
    }

    private boolean isDeviceOnline(Device device) {
        Long lastUpdate = device.getLastUpdate();
        return lastUpdate != null && lastUpdate > 0L && System.currentTimeMillis() - lastUpdate < 300000L;
    }

    private String agentSocketUrl(String number) {
        String base;
        String string = base = this.baseUrl == null ? "" : this.baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.startsWith("https://")) {
            base = "wss://" + base.substring("https://".length());
        } else if (base.startsWith("http://")) {
            base = "ws://" + base.substring("http://".length());
        }
        return base + "/ws/remote/agent/" + RemoteSupportResource.urlEncode(number);
    }

    private String viewerSocketPath(String number) {
        return "ws/remote/viewer/" + RemoteSupportResource.urlEncode(number);
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        }
        catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
