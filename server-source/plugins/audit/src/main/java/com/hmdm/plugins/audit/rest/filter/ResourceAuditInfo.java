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

package com.hmdm.plugins.audit.rest.filter;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>An enumeration over the resources which are targets for audit tracking.</p>
 *
 * @author isv
 */
public enum ResourceAuditInfo {
    LOGIN("POST", "/rest/public/auth/login", true, "plugin.audit.action.user.login", true, true),
    JWT_LOGIN("POST", "/rest/public/jwt/login", true, "plugin.audit.action.jwt.login", true, false),
    UPDATE_DEVICE("PUT", "/rest/private/devices", true, "plugin.audit.action.update.device", true, true),
    REMOVE_DEVICE("DELETE", "/rest/private/devices", false, "plugin.audit.action.remove.device", true, true),
    UPDATE_CONFIG("PUT", "/rest/private/configurations", true, "plugin.audit.action.update.configuration", true, true),
    COPY_CONFIG("PUT", "/rest/private/configurations/copy", true, "plugin.audit.action.copy.configuration", true, true),
    REMOVE_CONFIG("DELETE", "/rest/private/configurations", false, "plugin.audit.action.remove.configuration", true, true),
    UPDATE_APP("PUT", "/rest/private/applications/android", true, "plugin.audit.action.update.application", true, true),
    UPDATE_WEBAPP("PUT", "/rest/private/applications/web", true, "plugin.audit.action.update.webapp", true, true),
    REMOVE_APP("DELETE", "/rest/private/applications", false, "plugin.audit.action.remove.application", true, true),
    UPDATE_APP_VERSION("PUT", "/rest/private/applications/versions", true, "plugin.audit.action.update.version", true, true),
    REMOVE_APP_VERSION("DELETE", "/rest/private/applications/versions", false, "plugin.audit.action.remove.version", true, true),
    UPDATE_FILE("POST", "/rest/private/web-ui-files/move", true, "plugin.audit.action.update.file", true, true),
    REMOVE_FILE("POST", "/rest/private/web-ui-files/remove", true, "plugin.audit.action.remove.file", true, true),
    UPDATE_APP_CONFIG("POST", "/rest/private/applications/configurations", true, "plugin.audit.action.update.app.config", true, true),
    UPDATE_VERSION_CONFIG("POST", "/rest/private/applications/version/configurations", true, "plugin.audit.action.version.config", true, true),
    UPDATE_DESIGN("POST", "/rest/private/settings/design", true, "plugin.audit.action.update.design", true, true),
    UPDATE_USERROLES("POST", "/rest/private/settings/userRoles", false, "plugin.audit.action.update.user.roles", true, true),
    UPDATE_LANGUAGE("POST", "/rest/private/settings/lang", true, "plugin.audit.action.update.language", true, true),
    UPDATE_PLUGINS("POST", "/rest/plugin/main/private/disabled", true, "plugin.audit.action.update.plugins", true, true),
    UPDATE_USER("PUT", "/rest/private/users", true, "plugin.audit.action.update.user", true, true),
    REMOVE_USER("DELETE", "/rest/private/users", false, "plugin.audit.action.remove.user", true, true),
    UPDATE_GROUP("PUT", "/rest/private/groups", true, "plugin.audit.action.update.group", true, true),
    REMOVE_GROUP("DELETE", "/rest/private/groups", false, "plugin.audit.action.remove.group", true, true),
    PASSWORD_CHANGED("PUT", "/rest/private/users/current", true, "plugin.audit.action.password.changed", true, true),
    DEVICE_PASSWORD_RESET("PUT", "/rest/plugins/devicereset/private/password", true, "plugin.audit.action.password.reset", true, true),
    DEVICE_FACTORY_RESET("PUT", "/rest/plugins/devicereset/private/reset", false, "plugin.audit.action.device.reset", false, true),
    DEVICE_LOCK("PUT", "/rest/plugins/devicereset/private/lock", true, "plugin.audit.action.device.lock", true, true),
    UPDATE_UPDATE("POST", "/rest/private/update", true, "plugin.audit.action.update.update", true, true),

    // Comandos remotos: sao acoes que mudam o estado do aparelho (ate' apagar o aparelho
    // inteiro), entao precisam ficar registradas com quem pediu e com o corpo da requisicao.
    // O curinga no meio da URI cobre o id do dispositivo.
    DEVICE_REMOTE_COMMAND("POST", "/rest/private/devices/*/command", true, "plugin.audit.action.device.remote.command", true, true),
    // O corpo do force kiosk e' sempre vazio, entao nao ha' o que registrar dele.
    DEVICE_FORCE_KIOSK("POST", "/rest/private/devices/*/lock", true, "plugin.audit.action.device.force.kiosk", false, true),

    // Acoes administrativas do console que nao eram registradas (item m20 do FISCAL 2026-09-29).
    // A ordem importa: findAuditInfo devolve a PRIMEIRA regra que casa, entao as exatas vem
    // antes das de prefixo que as cobririam (ex.: /users/current antes de /users/).
    DEVICE_DESCRIPTION("POST", "/rest/private/devices/*/description", true, "plugin.audit.action.update.device", true, true),
    DEVICE_APP_SETTINGS("POST", "/rest/private/devices/*/applicationSettings", true, "plugin.audit.action.device.app.settings", true, true),
    DEVICE_DELETE_BULK("POST", "/rest/private/devices/deleteBulk", true, "plugin.audit.action.remove.device", true, true),
    DEVICE_GROUP_BULK("POST", "/rest/private/devices/groupBulk", true, "plugin.audit.action.update.device", true, true),
    CONFIG_APP_UPGRADE("PUT", "/rest/private/configurations/*/application/*/upgrade", true, "plugin.audit.action.update.configuration", true, true),
    UPDATE_ROLE("PUT", "/rest/private/roles", true, "plugin.audit.action.update.role", true, true),
    REMOVE_ROLE("DELETE", "/rest/private/roles/", false, "plugin.audit.action.remove.role", true, true),
    UPDATE_ICON("PUT", "/rest/private/icons", true, "plugin.audit.action.update.icon", true, true),
    REMOVE_ICON("DELETE", "/rest/private/icons/", false, "plugin.audit.action.remove.icon", true, true),
    SUPERADMIN_PASSWORD("PUT", "/rest/private/users/superadmin/password", true, "plugin.audit.action.password.changed", false, true),
    UPDATE_USER_OTHER("PUT", "/rest/private/users/", false, "plugin.audit.action.update.user", true, true),
    UPDATE_CUSTOMER("PUT", "/rest/private/customers", true, "plugin.audit.action.update.customer", true, true),
    REMOVE_CUSTOMER("DELETE", "/rest/private/customers/", false, "plugin.audit.action.remove.customer", true, true),
    UPDATE_MISC_SETTINGS("POST", "/rest/private/settings/misc", true, "plugin.audit.action.update.settings", true, true),
    IMPORT_SETTINGS("POST", "/rest/private/settings/import", true, "plugin.audit.action.import.settings", false, true),
    BACKUP_CREATE("POST", "/rest/private/backup/create", true, "plugin.audit.action.backup.create", true, true),
    BACKUP_RESTORE("POST", "/rest/private/backup/*/restore", true, "plugin.audit.action.backup.restore", true, true),
    BACKUP_SCHEDULE("PUT", "/rest/private/backup/schedule", true, "plugin.audit.action.backup.schedule", true, true),
    BACKUP_REMOVE("DELETE", "/rest/private/backup/", false, "plugin.audit.action.backup.remove", true, true),
    UPLOAD_FILE("POST", "/rest/private/web-ui-files/update", true, "plugin.audit.action.update.file", true, true),
    REMOTE_SUPPORT_START("POST", "/rest/private/remote-support/*/start", true, "plugin.audit.action.remote.start", true, true),
    REMOTE_SUPPORT_STOP("POST", "/rest/private/remote-support/*/stop", true, "plugin.audit.action.remote.stop", true, true),
    LOGOUT("POST", "/rest/public/auth/logout", true, "plugin.audit.action.user.logout", false, false),
    MODULE_TOGGLE("POST", "/rest/plugins/moduleregistry/private/toggle", true, "plugin.audit.action.update.plugins", true, true),
    MESSAGE_SEND("POST", "/rest/plugins/messaging/private/send", true, "plugin.audit.action.message.send", true, true),
    MESSAGE_REMOVE("DELETE", "/rest/plugins/messaging/", false, "plugin.audit.action.message.remove", true, true),
    PUSH_SEND("POST", "/rest/plugins/push/private/send", true, "plugin.audit.action.push.send", true, true),
    PUSH_TASK("PUT", "/rest/plugins/push/private/task", true, "plugin.audit.action.push.task", true, true),
    PUSH_REMOVE("DELETE", "/rest/plugins/push/private/", false, "plugin.audit.action.push.remove", true, true),
    DEVICEINFO_SETTINGS("PUT", "/rest/plugins/deviceinfo/deviceinfo-plugin-settings/private", true, "plugin.audit.action.update.plugin.settings", true, true),
    DEVICELOG_SETTINGS("PUT", "/rest/plugins/devicelog/devicelog-plugin-settings/private", false, "plugin.audit.action.update.plugin.settings", true, true),
    DEVICELOG_RULE_REMOVE("DELETE", "/rest/plugins/devicelog/devicelog-plugin-settings/private/rule/", false, "plugin.audit.action.update.plugin.settings", true, true),
    WEBFILTER_UPDATE("PUT", "/rest/plugins/webfilter/private/", false, "plugin.audit.action.webfilter.update", true, false),
    WEBFILTER_REMOVE("DELETE", "/rest/plugins/webfilter/private/", false, "plugin.audit.action.webfilter.update", true, false);

    /**
     * <p>Method for the REST resource to track audit log for.</p>
     */
    private final String method;

    /**
     * <p>An URI for the REST resource to track audit log for.</p>
     */
    private final String uri;

    /**
     * <p>A flag indicating if exact match for the resource URI is required in order to have the incoming request get
     * audited.</p>
     */
    private final boolean uriExactMatch;

    /**
     * <p>A key in message resource bundle referring to description of action mapped to audited request.</p>
     */
    private final String auditLogAction;

    /**
     * <p>A flag indicating if request data must be saved as a payload.</p>
     */
    private final boolean payload;

    /**
     * <p>A flag indicating if response is in Headwind MDM standard format and should be checked for errors.</p>
     */
    private final boolean checkResponse;

    /**
     * <p>Constructs new <code>ResourceAuditInfo</code> instance. This implementation does nothing.</p>
     */
    ResourceAuditInfo(String method, String uri, boolean uriExactMatch, String auditLogAction, boolean payload, boolean checkResponse) {
        this.method = method;
        this.uri = uri;
        this.uriExactMatch = uriExactMatch;
        this.auditLogAction = auditLogAction;
        this.payload = payload;
        this.checkResponse = checkResponse;
    }

    /**
     * <p>Gets the auditor for the specified request/response chain.</p>
     *
     * @param request an incoming request to be processed.
     * @param response a response to be sent to client.
     * @param chain a filter chain.
     * @return an auditor for the specified request/response chain.
     */
    public ResourceAuditor getResourceAuditor(ServletRequest request, ServletResponse response, FilterChain chain,
                                              String proxyIps, String ipHeader) throws IOException {
        return new ResourceAuditor(auditLogAction, request, response, chain, payload, checkResponse, proxyIps, ipHeader);
    }

    /**
     * <p>Checks if specified request URI matches this audit rule.</p>
     *
     * @param requestUri an URI for the current request being processed by application.
     * @return <code>true</code> if there is a match; <code>false</code> otherwise.
     */
    private boolean matches(String requestMethod, String requestUri) {
        if (!this.method.equalsIgnoreCase(requestMethod)) {
            return false;
        }
        if (this.uri.indexOf('*') >= 0) {
            // "*" casa exatamente um segmento de caminho (ex.: o id do dispositivo). Antes a
            // regra era comparada com equals() literal, entao nenhum comando remoto nem o
            // force-kiosk era registrado desde que as regras com curinga foram criadas.
            final String[] pattern = this.uri.split("/", -1);
            final String[] actual = requestUri.split("/", -1);
            if (this.uriExactMatch ? actual.length != pattern.length : actual.length < pattern.length) {
                return false;
            }
            for (int i = 0; i < pattern.length; i++) {
                if (pattern[i].equals("*") ? actual[i].isEmpty() : !pattern[i].equals(actual[i])) {
                    return false;
                }
            }
            return true;
        }
        if (this.uriExactMatch) {
            return this.uri.equals(requestUri);
        } else {
            return requestUri.startsWith(this.uri);
        }
    }

    /**
     * <p>Finds the details for audit process (if any) to be performed against specified request URI.</p>
     *
     * @param requestUri an URI for the current request being processed by application.
     * @return the details for audit process to apply to processed request.
     */
    public static Optional<ResourceAuditInfo> findAuditInfo(String requestMethod, String requestUri) {
        return Stream.of(ResourceAuditInfo.values())
                .filter(info -> info.matches(requestMethod, requestUri))
                .findFirst();
    }
}
