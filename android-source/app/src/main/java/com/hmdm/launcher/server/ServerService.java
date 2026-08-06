/*
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Copyright (C) 2019 Headwind Solutions LLC (http://h-sms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hmdm.launcher.server;


import com.hmdm.launcher.db.LocationTable;
import com.hmdm.launcher.json.DetailedInfo;
import com.hmdm.launcher.json.DetailedInfoConfigResponse;
import com.hmdm.launcher.json.DeviceEnrollOptions;
import com.hmdm.launcher.json.DeviceInfo;
import com.hmdm.launcher.json.PushResponse;
import com.hmdm.launcher.json.RemoteLogConfigResponse;
import com.hmdm.launcher.json.RemoteLogItem;
import com.hmdm.launcher.json.ServerConfigResponse;

import java.util.List;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ServerService {

    static final String REQUEST_SIGNATURE_HEADER = "X-Request-Signature";
    static final String CPU_ARCH_HEADER = "X-CPU-Arch";

    @POST("{project}/rest/public/sync/configuration/{number}")
    Call<ResponseBody> enrollAndGetServerConfigRaw(@Path("project") String project,
                                                   @Path("number") String number,
                                                   @Header(REQUEST_SIGNATURE_HEADER) String signature,
                                                   @Header(CPU_ARCH_HEADER) String cpuArch,
                                                   @Body DeviceEnrollOptions createOptions);

    @GET("{project}/rest/public/sync/configuration/{number}")
    Call<ResponseBody> getServerConfigRaw(@Path("project") String project,
                                          @Path("number") String number,
                                          @Header(REQUEST_SIGNATURE_HEADER) String signature,
                                          @Header(CPU_ARCH_HEADER) String cpuArch);

    @POST("{project}/rest/public/sync/configuration/{number}")
    Call<ServerConfigResponse> enrollAndGetServerConfig(@Path("project") String project,
                                                        @Path("number") String number,
                                                        @Header(REQUEST_SIGNATURE_HEADER) String signature,
                                                        @Header(CPU_ARCH_HEADER) String cpuArch,
                                                        @Body DeviceEnrollOptions createOptions);

    @GET("{project}/rest/public/sync/configuration/{number}")
    Call<ServerConfigResponse> getServerConfig(@Path("project") String project,
                                               @Path("number") String number,
                                               @Header(REQUEST_SIGNATURE_HEADER) String signature,
                                               @Header(CPU_ARCH_HEADER) String cpuArch);

    @POST("{project}/rest/public/sync/info")
    @Headers("Content-Type: application/json")
    Call<ResponseBody> sendDevice(@Path("project") String project, @Body DeviceInfo deviceInfo);

    @GET("{project}/rest/notifications/device/{number}")
    Call<PushResponse> queryPushNotifications(@Path("project") String project,
                                              @Path("number") String number,
                                              @Header(REQUEST_SIGNATURE_HEADER) String signature);

    @GET("{project}/rest/notification/polling/{number}")
    Call<PushResponse> queryPushLongPolling(@Path("project") String project,
                                            @Path("number") String number,
                                            @Header(REQUEST_SIGNATURE_HEADER) String signature);

    @GET( "{project}/rest/plugins/devicelog/log/rules/{number}" )
    Call<RemoteLogConfigResponse> getRemoteLogConfig(@Path("project") String project, @Path("number") String number);

    @POST("{project}/rest/plugins/devicelog/log/list/{number}")
    @Headers("Content-Type: application/json")
    Call<ResponseBody> sendLogs(@Path("project") String project, @Path("number") String number, @Body List<RemoteLogItem> logItems);

    @PUT("{project}/rest/plugins/deviceinfo/deviceinfo/public/{number}")
    @Headers("Content-Type: application/json")
    Call<ResponseBody> sendDetailedInfo(@Path("project") String project, @Path("number") String number, @Body List<DetailedInfo> infoItems);

    /**
     * Envia a captura de tela feita a pedido do painel. O corpo e' o PNG cru: o servidor grava
     * como screenshots/{number}.png dentro do diretorio de arquivos, que e' de onde o painel
     * ja' le' a imagem.
     */
    @POST("{project}/rest/public/screenshot/{number}")
    Call<ResponseBody> uploadScreenshot(@Path("project") String project,
                                        @Path("number") String number,
                                        @Body RequestBody image);

    @PUT("{project}/rest/plugins/devicelocations/public/update/{number}")
    @Headers("Content-Type: application/json")
    Call<ResponseBody> sendLocations(@Path("project") String project, @Path("number") String number, @Body List<LocationTable.Location> locationItems);

    @GET( "{project}/rest/plugins/deviceinfo/deviceinfo-plugin-settings/device/{number}" )
    Call<DetailedInfoConfigResponse> getDetailedInfoConfig(@Path("project") String project, @Path("number") String number);

    @POST("{project}/rest/plugins/devicereset/public/{number}")
    @Headers("Content-Type: application/json")
    Call<ResponseBody> confirmDeviceReset(@Path("project") String project, @Path("number") String number, @Body DeviceInfo deviceInfo);

    @POST("{project}/rest/plugins/devicereset/public/reboot/{number}")
    @Headers("Content-Type: application/json")
    Call<ResponseBody> confirmReboot(@Path("project") String project, @Path("number") String number, @Body DeviceInfo deviceInfo);

    @POST("{project}/rest/plugins/devicereset/public/password/{number}")
    @Headers("Content-Type: application/json")
    Call<ResponseBody> confirmPasswordReset(@Path("project") String project, @Path("number") String number, @Body DeviceInfo deviceInfo);

    // Acknowledges a text message pushed by the messaging plugin. Without this the message
    // stays "Sent" in the panel forever, with no way to tell it ever reached the device.
    // Status values match com.hmdm.plugins.messaging.persistence.domain.Message:
    // 0 = sent, 1 = delivered, 2 = read.
    @GET("{project}/rest/plugins/messaging/public/status/{id}/{status}")
    Call<ResponseBody> confirmMessageStatus(@Path("project") String project,
                                            @Path("id") int id,
                                            @Path("status") int status);

}
