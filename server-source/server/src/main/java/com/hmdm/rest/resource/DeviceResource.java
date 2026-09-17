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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.hmdm.notification.PushService;
import com.hmdm.notification.persistence.domain.PushMessage;
import com.hmdm.persistence.*;
import com.hmdm.remote.DeviceResetHub;
import com.hmdm.service.RemoteCommand;
import com.hmdm.persistence.domain.*;
import com.hmdm.rest.json.*;
import com.hmdm.rest.json.view.devicelist.DeviceListView;
import com.hmdm.rest.json.view.devicelist.DeviceView;
import com.hmdm.security.SecurityContext;
import com.hmdm.security.SecurityException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Api(tags = {"Device"}, authorizations = {@Authorization("Bearer Token")})
@Singleton
@Path("/private/devices")
public class DeviceResource {

    private static final Logger log = LoggerFactory.getLogger(DeviceResource.class);

    private DeviceDAO deviceDAO;
    private ConfigurationDAO configurationDAO;
    private PushService pushService;
    private ConfigurationFileDAO configurationFileDAO;
    private CommonDAO commonDAO;
    private UnsecureDAO unsecureDAO;

    /**
     * <p>A constructor required by Swagger.</p>
     */
    public DeviceResource() {
    }

    @Inject
    public DeviceResource(DeviceDAO deviceDAO,
                          ConfigurationDAO configurationDAO,
                          PushService pushService,
                          ConfigurationFileDAO configurationFileDAO,
                          CommonDAO commonDAO,
                          UnsecureDAO unsecureDAO) {
        this.deviceDAO = deviceDAO;
        this.configurationDAO = configurationDAO;
        this.pushService = pushService;
        this.configurationFileDAO = configurationFileDAO;
        this.commonDAO = commonDAO;
        this.unsecureDAO = unsecureDAO;
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Search devices",
            notes = "Search devices meeting the specified filter value",
            response = DeviceListView.class
    )
    @POST
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllDevices(DeviceSearchRequest request) {
        PaginatedData<Device> devices = this.deviceDAO.getAllDevices(request);
        Map<Integer, List<Application>> configIdToApplicationsMap = new HashMap<>();
        Map<Integer, List<ConfigurationFile>> configIdToFilesMap = new HashMap<>();
        Map<Integer, Configuration> configIdToConfigurationsMap = new HashMap<>();
        for (Device device : devices.getItems()) {
            final Integer deviceConfigurationId = device.getConfigurationId();

            Configuration dbConfig = null;
            if (!configIdToConfigurationsMap.containsKey(deviceConfigurationId)) {
                dbConfig = configurationDAO.getConfigurationById(deviceConfigurationId);
                if (dbConfig == null) {
                    log.error("Device " + device.getNumber() + ": configuration does not exist: " + deviceConfigurationId);
                    device.setConfigurationId(null);     // Will be filtered out when converting to DeviceView
                    continue;
                }
            }

            if (!configIdToApplicationsMap.containsKey(deviceConfigurationId)) {
                configIdToApplicationsMap.put(deviceConfigurationId, this.configurationDAO.getConfigurationApplications(deviceConfigurationId));
            }
            if (!configIdToFilesMap.containsKey(deviceConfigurationId)) {
                configIdToFilesMap.put(deviceConfigurationId, this.configurationFileDAO.getConfigurationFiles(deviceConfigurationId));
            }

            if (!configIdToConfigurationsMap.containsKey(deviceConfigurationId)) {
                // Here we keep only required properties
                Configuration configuration = new Configuration();
                configuration.setId(deviceConfigurationId);
                configuration.setName(device.getConfigName());
                configuration.setPermissive(dbConfig.getPermissive());
                configuration.setKeepaliveTime(dbConfig.getKeepaliveTime());
                if (dbConfig.getMainAppId() != null && dbConfig.getMainAppId() > 0 &&
                        dbConfig.getEventReceivingComponent() != null && dbConfig.getEventReceivingComponent().length() > 0) {
                    configuration.setQrCodeKey(dbConfig.getQrCodeKey());
                    configuration.setBaseUrl(this.configurationDAO.getBaseUrl());
                }
                configuration.setApplications(configIdToApplicationsMap.get(deviceConfigurationId));
                configuration.setFiles(configIdToFilesMap.get(deviceConfigurationId));

                configIdToConfigurationsMap.put(deviceConfigurationId, configuration);
            }

            device.setConfiguration(configIdToConfigurationsMap.get(deviceConfigurationId));
        }

        final List<DeviceView> deviceViews = devices.getItems().stream()
                .filter(d -> d.getConfigurationId() != null)
                .map(DeviceView::new)
                .collect(Collectors.toList());
        PaginatedData<DeviceView> devicesPage = new PaginatedData<>(deviceViews, devices.getTotalItemsCount());

        DeviceListView view = new DeviceListView(configIdToConfigurationsMap.values(), devicesPage);

        return Response.OK(view);

    }

    // =================================================================================================================
    @ApiOperation(
            value = "Get the device info by number",
            notes = "Get the device info by number",
            response = DeviceListView.class
    )
    @GET
    @Path("/number/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevice(@PathParam("number") @ApiParam("Device number") String number) {
        try {
            Device device = this.deviceDAO.getDeviceByNumber(number);
            DeviceView deviceView = new DeviceView(device);
            return Response.OK(deviceView);
        } catch (Exception e) {
            log.error("Cannot find device by number: " + number);
            return Response.DEVICE_NOT_FOUND_ERROR();
        }
    }

    // =================================================================================================================
    /**
     * <p>Gets the list of device ids/names matching the specified string filter for autocompletions.</p>
     *
     * @param filter a filter to be used for filtering the records.
     * @return a response with list of devices matching the specified filter.
     */
    @ApiOperation(value = "")
    @POST
    @Path("/autocomplete")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevicesForAutocomplete(String filter) {
        try {
            List<DeviceLookupItem> devices = this.deviceDAO.findDevices(filter, 10);
            return Response.OK(devices);
        } catch (Exception e) {
            log.error("Failed to search the devices due to unexpected error. Filter: {}", filter, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Create or update device",
            notes = "Create a new device (if id is not provided) or update existing one otherwise."
    )
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDevice(Device device) {
        try {
            final boolean canEditDevices = SecurityContext.get().hasPermission("device.profile.edit")
                || SecurityContext.get().hasPermission("edit_devices");

            if (!canEditDevices) {
                log.error("Unauthorized attempt to create or edit device",
                        SecurityException.onCustomerDataAccessViolation(device.getId(), "device"));
                return Response.PERMISSION_DENIED();
            }

            Device dbDevice;
            try {
                dbDevice = this.deviceDAO.getDeviceByNumber(device.getNumber());
            } catch (SecurityException e) {
                log.error("A different device with same number exists in other organization: {}", device.getNumber());
                return Response.DEVICE_EXISTS();
            }
            if (dbDevice != null && !dbDevice.getId().equals(device.getId())) {
                log.error("A different device with same number exists: {}", dbDevice);
                return Response.DEVICE_EXISTS();
            } else {
                dbDevice = this.deviceDAO.getDeviceById(device.getId());
                if (device.getId() != null) {
                    if (dbDevice != null) {
                        boolean notify = (dbDevice.getConfigurationId() != null &&
                                !dbDevice.getConfigurationId().equals(device.getConfigurationId())) ||
                                (dbDevice.getOldNumber() == null && device.getOldNumber() != null);
                        this.deviceDAO.updateDevice(device);
                        if (notify) {
                            this.pushService.notifyDeviceOnSettingUpdate(device.getId());
                        }
                    }
                } else if (device.getIds() != null) {
                    // This is a bulk request to update configurations for selected devices
                    Iterator it = device.getIds().iterator();

                    while (it.hasNext()) {
                        Integer id = (Integer) it.next();
                        dbDevice = this.deviceDAO.getDeviceById(id);
                        if (dbDevice != null) {
                            this.deviceDAO.updateDeviceConfiguration(id, device.getConfigurationId());
                            this.pushService.notifyDeviceOnSettingUpdate(dbDevice.getId());
                        }
                    }
                } else {
                    Settings settings = new Settings();
                    if (!unsecureDAO.isSingleCustomer()) {
                        commonDAO.loadCustomerSettings(settings);
                    }
                    if (settings.getDeviceLimit() == 0 || settings.getDeviceCount() < settings.getDeviceLimit()) {
                        device.setLastUpdate(0L);
                        this.deviceDAO.insertDevice(device);
                    } else {
                        log.warn("New device {} not added by customer {} due to the license limit", device.getNumber(),
                                SecurityContext.get().getCurrentCustomerId().get());
                        return Response.ERROR();
                    }
                }

                return Response.OK();
            }
        } catch (Exception e) {
            log.error("Unexpected error when saving/creating device", e);
            return Response.ERROR();
        }
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Delete device",
            notes = "Delete an existing device"
    )
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeDevice(@PathParam("id") @ApiParam("Device ID") Integer id) {
        final boolean canEditDevices = SecurityContext.get().hasPermission("device.profile.edit")
            || SecurityContext.get().hasPermission("edit_devices");

        if (!(canEditDevices)) {
            log.error("Unauthorized attempt to delete device",
                    SecurityException.onCustomerDataAccessViolation(id, "device"));
            return Response.PERMISSION_DENIED();
        }

        this.deviceDAO.removeDeviceById(id);
        return Response.OK();
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Delete bulk devices",
            notes = "Delete multiple devices at once"
    )
    @POST
    @Path("/deleteBulk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeBulkDevices(Device device) {
        final boolean canEditDevices = SecurityContext.get().hasPermission("device.profile.edit")
            || SecurityContext.get().hasPermission("edit_devices");

        if (!(canEditDevices)) {
            log.error("Unauthorized attempt to delete devices",
                    SecurityException.onCustomerDataAccessViolation(0, "device"));
            return Response.PERMISSION_DENIED();
        }

        if (device.getIds() != null) {
            // Device IDs are transferred in the "ids" parameter
            Iterator it = device.getIds().iterator();

            while (it.hasNext()) {
                Integer id = (Integer) it.next();
                this.deviceDAO.removeDeviceById(id);
            }
        }
        return Response.OK();
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Set or clear device groups in bulk"
    )
    @POST
    @Path("/groupBulk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDeviceGroupBulk(DeviceGroupBulkRequest request) {
        final boolean canEditDevices = SecurityContext.get().hasPermission("device.profile.edit")
            || SecurityContext.get().hasPermission("edit_devices");

        if (!(canEditDevices)) {
            log.error("Unauthorized attempt to delete devices",
                    SecurityException.onCustomerDataAccessViolation(0, "device"));
            return Response.PERMISSION_DENIED();
        }

        if (request.getIds() != null) {
            // Device IDs are transferred in the "ids" parameter
            Iterator it = request.getIds().iterator();

            while (it.hasNext()) {
                Integer id = (Integer) it.next();
                Device device = deviceDAO.getDeviceById(id);
                if (device == null) {
                    // Not found
                    continue;
                }
                List<LookupItem> groups = device.getGroups();
                groups.removeAll(request.getGroups());
                if (request.getAction().equals("set")) {
                    groups.addAll(request.getGroups());
                }
                deviceDAO.updateDevice(device);
                // No need to notify devices because changing a group doesn't affect a device
            }
        }
        return Response.OK();
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Get device application settings",
            notes = "Get application settings set at device level"
    )
    @GET
    @Path("/{id}/applicationSettings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeviceApplicationSettings(@PathParam("id") @ApiParam("Device ID") Integer id) {
        try {
            final List<ApplicationSetting> deviceApplicationSettings = this.deviceDAO.getDeviceApplicationSettings(id);
            return Response.OK(deviceApplicationSettings);
        } catch (Exception e) {
            log.error("Failed to retrieve the application settings for device #{}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Save device application settings",
            notes = "Save application settings set at device level"
    )
    @POST
    @Path("/{id}/applicationSettings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveDeviceApplicationSettings(@PathParam("id") @ApiParam("Device ID") Integer id,
                                                  List<ApplicationSetting> applicationSettings) {
        try {
            this.deviceDAO.saveDeviceApplicationSettings(id, applicationSettings);
            return Response.OK();
        } catch (Exception e) {
            log.error("Failed to save the application settings for device #{}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Notify device on update",
            notes = "Sends a notification to device on application settings update",
            response = Void.class
    )
    @POST
    @Path("/{id}/applicationSettings/notify")
    @Produces(MediaType.APPLICATION_JSON)
    public Response notifyDevicesOnUpdate(@PathParam("id") Integer id) {
        try {
            this.pushService.notifyDeviceOnApplicationSettingUpdate(id);
            return Response.OK();
        } catch (Exception e) {
            log.error("Failed to send notification on application settings update to device #{}", id, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Save device description",
            notes = "Updates existing device description"
    )
    @POST
    @Path("/{id}/description")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveDeviceDescription(@PathParam("id") @ApiParam("Device ID") Integer deviceId,
                                          String newDeviceDescription) {
        try {
            final boolean canEditDeviceDescription = SecurityContext.get().hasPermission("edit_device_desc");

            if (!canEditDeviceDescription) {
                log.error("Unauthorized attempt to edit device description",
                        SecurityException.onCustomerDataAccessViolation(deviceId, "device"));
                return Response.PERMISSION_DENIED();
            }

            this.deviceDAO.updateDeviceDescription(deviceId, newDeviceDescription);
            return Response.OK();
        } catch (Exception e) {
            log.error("Failed to save the description for device #{}", deviceId, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =================================================================================================================
    @ApiOperation(
            value = "List supported remote commands",
            notes = "Returns the commands this server build can actually deliver. The control panel renders its " +
                    "buttons from this list, so it can never offer an action the backend would reject."
    )
    @GET
    @Path("/commands")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSupportedCommands() {
        try {
            return Response.OK(RemoteCommand.supportedActions());
        } catch (Exception e) {
            log.error("Failed to list the supported remote commands", e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Send a remote command to a device",
            notes = "Resolves the logical command name into a push message understood by the device agent and " +
                    "delivers it over both the MQTT and the polling channels. The command is rejected unless the " +
                    "current user holds the RBAC permission bound to it."
    )
    @POST
    @Path("/{id}/command")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendDeviceCommand(@PathParam("id") @ApiParam("Device ID") Integer id,
                                      DeviceCommandRequest request) {
        return dispatchCommand(id, request == null ? null : request.getAction(),
                request == null ? null : request.getParams());
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Force a device back into kiosk mode",
            notes = "Shortcut for the 'lock_screen' remote command."
    )
    @POST
    @Path("/{id}/lock")
    @Produces(MediaType.APPLICATION_JSON)
    public Response forceKiosk(@PathParam("id") @ApiParam("Device ID") Integer id) {
        return dispatchCommand(id, RemoteCommand.LOCK_SCREEN.getAction(), null);
    }

    /**
     * <p>Resolves, authorises and delivers a remote command.</p>
     *
     * <p>An unknown action is refused with the list of the ones this build knows, because the
     * alternative - forwarding it to the agent anyway - produces a device that silently
     * ignores the message while the panel reports success.</p>
     */
    private Response dispatchCommand(Integer id, String action, Map<String, Object> params) {
        try {
            final Device dbDevice = this.deviceDAO.getDeviceById(id);
            if (dbDevice == null) {
                log.error("Remote command '{}' refused: device #{} does not exist", action, id);
                return Response.DEVICE_NOT_FOUND_ERROR();
            }

            final RemoteCommand command = RemoteCommand.byAction(action).orElse(null);
            if (command == null) {
                log.warn("Rejected unknown remote command '{}' for device #{}. Supported commands: {}",
                        action, id, RemoteCommand.supportedActions());
                return Response.ERROR("error.remote.command.unsupported");
            }

            // edit_devices is the blanket permission the legacy panel has always used; the
            // per-command permission is what allows a narrower operator role to exist.
            if (!SecurityContext.get().hasPermission("edit_devices")
                    && !SecurityContext.get().hasPermission(command.getPermission())) {
                log.error("Unauthorized attempt to send remote command '{}' to device #{}", action, id,
                        SecurityException.onCustomerDataAccessViolation(id, "device"));
                return Response.PERMISSION_DENIED();
            }

            final String payload;
            try {
                payload = command.buildPayload(params);
            } catch (IllegalArgumentException e) {
                log.warn("Rejected remote command '{}' for device #{}: {}", action, id, e.getMessage());
                return Response.ERROR(e.getMessage());
            }

            /*
             * Apagar o aparelho nao viaja como push. O launcher 6.36 nao tem o tipo 'wipe' -
             * a constante nao existe no binario, e o push era recebido e descartado. O que
             * ele implementa e' ler factoryReset ao atualizar a configuracao, entao armamos
             * o pedido e mandamos um configUpdated, que ele entende. O aparelho ainda
             * confirma em /rest/plugins/devicereset/public/{number} antes de apagar.
             */
            final String pushType;
            if (command == RemoteCommand.WIPE) {
                DeviceResetHub.getInstance().request(dbDevice.getNumber());
                pushType = RemoteCommand.SET_CONFIG.getPushType();
            } else {
                pushType = command.getPushType();
            }

            PushMessage message = new PushMessage();
            message.setDeviceId(dbDevice.getId());
            message.setMessageType(pushType);
            message.setPayload(command == RemoteCommand.WIPE ? null : payload);
            this.pushService.send(message);

            Map<String, Object> result = new HashMap<>();
            result.put("action", command.getAction());
            result.put("pushType", pushType);
            result.put("deviceId", dbDevice.getId());
            result.put("deviceNumber", dbDevice.getNumber());
            return Response.OK(result);
        } catch (Exception e) {
            log.error("Failed to send remote command '{}' to device #{}", action, id, e);
            return Response.INTERNAL_ERROR();
        }
    }
}
