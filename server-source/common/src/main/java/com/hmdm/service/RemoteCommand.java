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

package com.hmdm.service;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>The catalog of remote commands this server can deliver to a device agent.</p>
 *
 * <p>Each entry binds three things that used to be spread across the browser, the REST
 * resource and the agent:</p>
 * <ul>
 *     <li>the <b>action</b>, the logical name the control panel asks for;</li>
 *     <li>the <b>push type</b>, the message type the Android agent actually understands
 *     (see com.hmdm.launcher.json.PushMessage on the device side) - these two are not the
 *     same string, and keeping the translation here is what stops the panel from having to
 *     know the wire protocol;</li>
 *     <li>the <b>permission</b> that authorises it, so a command cannot be issued by an
 *     operator who is not allowed to issue it.</li>
 * </ul>
 *
 * <p>The panel builds its buttons from {@link #supportedActions()} rather than from its own
 * hardcoded list, so a build that gains or loses a command never disagrees with the UI.</p>
 *
 * <p>Starting and stopping the live remote-access session is deliberately <b>not</b> in this
 * catalog. Those two messages carry a one-time relay token that only the server can mint, so
 * they are issued by com.hmdm.rest.resource.RemoteScreenResource instead of through the generic
 * command endpoint - routing them here would mean handing out a session that no relay slot is
 * waiting for.</p>
 */
public enum RemoteCommand {

    LOCK_SCREEN("lock_screen", "lockKiosk", "device.lifecycle.lock"),
    UNLOCK_SCREEN("unlock_screen", "exitKiosk", "device.lifecycle.unlock"),
    REBOOT("reboot", "reboot", "device.lifecycle.reboot"),
    WIPE("wipe", "wipe", "device.lifecycle.wipe"),
    MESSAGE("message", "textMessage", "device.remote_access.control"),
    SET_CONFIG("set_config", "configUpdated", "device.remote_access.control"),
    ADMIN_PANEL("admin_panel", "adminPanel", "device.remote_access.control"),
    PERMISSIVE_MODE("permissive_mode", "permissiveMode", "device.remote_access.control"),
    GRANT_PERMISSIONS("grant_permissions", "grantPermissions", "device.remote_access.control"),
    RUN_APP("run_app", "runApp", "device.remote_access.control"),
    UNINSTALL_APP("uninstall_app", "uninstallApp", "device.remote_access.control"),
    CLEAR_APP_DATA("clear_app_data", "clearAppData", "device.remote_access.control"),
    DELETE_FILE("delete_file", "deleteFile", "device.remote_access.control"),
    DELETE_DIR("delete_dir", "deleteDir", "device.remote_access.control"),
    PURGE_DIR("purge_dir", "purgeDir", "device.remote_access.control"),
    CLEAR_DOWNLOADS("clear_downloads", "clearDownloadHistory", "device.remote_access.control"),
    RUN_COMMAND("run_command", "runCommand", "device.remote_access.control"),
    INTENT("intent", "intent", "device.remote_access.control"),
    BROADCAST("broadcast", "broadcast", "device.remote_access.control");

    public static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_PATH_LENGTH = 1024;
    private static final int MAX_COMMAND_LENGTH = 2048;

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$");
    private static final Pattern ACTION_PATTERN = Pattern.compile("^[a-zA-Z0-9_.]{1,255}$");

    private final String action;
    private final String pushType;
    private final String permission;

    RemoteCommand(String action, String pushType, String permission) {
        this.action = action;
        this.pushType = pushType;
        this.permission = permission;
    }

    public String getAction() {
        return action;
    }

    public String getPushType() {
        return pushType;
    }

    public String getPermission() {
        return permission;
    }

    public static Optional<RemoteCommand> byAction(String action) {
        return Arrays.stream(values())
                .filter(command -> command.action.equals(action))
                .findFirst();
    }

    public static Set<String> supportedActions() {
        return Arrays.stream(values())
                .map(RemoteCommand::getAction)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * <p>Turns the parameters supplied by the panel into the JSON payload the agent expects,
     * rejecting anything malformed before it reaches the device.</p>
     *
     * <p>Validation lives here rather than in the REST resource because the payload shape and
     * its constraints are properties of the command itself. The browser mirrors these checks
     * to give an immediate message, but this is the authority: a caller bypassing the UI gets
     * the same treatment.</p>
     *
     * @param params parameters as received, may be null for a command that takes none.
     * @return the payload as a JSON string, or null when the command carries no payload.
     * @throws IllegalArgumentException when a parameter is missing, empty, too long or malformed.
     */
    public String buildPayload(Map<String, Object> params) {
        switch (this) {
            case MESSAGE: {
                String text = requireText(params, "text", MAX_MESSAGE_LENGTH);
                JSONObject payload = new JSONObject();
                payload.put("text", text);
                Integer duration = optionalInt(params, "duration");
                if (duration != null) {
                    if (duration < 1 || duration > 3600) {
                        throw new IllegalArgumentException("Parameter 'duration' must be between 1 and 3600 seconds");
                    }
                    payload.put("duration", duration.intValue());
                }
                return payload.toString();
            }

            case RUN_APP:
            case UNINSTALL_APP:
            case CLEAR_APP_DATA: {
                String pkg = requireText(params, "pkg", 255);
                if (!PACKAGE_PATTERN.matcher(pkg).matches()) {
                    throw new IllegalArgumentException("Parameter 'pkg' is not a valid Android package name: " + pkg);
                }
                JSONObject payload = new JSONObject();
                payload.put("pkg", pkg);
                return payload.toString();
            }

            case DELETE_FILE:
            case DELETE_DIR:
            case PURGE_DIR: {
                String path = requireText(params, "path", MAX_PATH_LENGTH);
                if (path.contains("..")) {
                    throw new IllegalArgumentException("Parameter 'path' must not contain relative segments");
                }
                JSONObject payload = new JSONObject();
                payload.put("path", path);
                return payload.toString();
            }

            case RUN_COMMAND: {
                String command = requireText(params, "command", MAX_COMMAND_LENGTH);
                JSONObject payload = new JSONObject();
                payload.put("command", command);
                return payload.toString();
            }

            case INTENT:
            case BROADCAST: {
                String intentAction = requireText(params, "action", 255);
                if (!ACTION_PATTERN.matcher(intentAction).matches()) {
                    throw new IllegalArgumentException("Parameter 'action' is not a valid intent action: " + intentAction);
                }
                JSONObject payload = new JSONObject();
                payload.put("action", intentAction);
                Object data = params == null ? null : params.get("data");
                if (data != null && !data.toString().trim().isEmpty()) {
                    payload.put("data", data.toString().trim());
                }
                return payload.toString();
            }

            case WIPE: {
                Object externalStorage = params == null ? null : params.get("externalStorage");
                JSONObject payload = new JSONObject();
                payload.put("externalStorage", Boolean.parseBoolean(String.valueOf(externalStorage)));
                return payload.toString();
            }

            default:
                // Commands that carry no parameters travel with a null payload; the agent
                // reads the message type alone.
                return null;
        }
    }

    private static String requireText(Map<String, Object> params, String key, int maxLength) {
        Object raw = params == null ? null : params.get(key);
        if (raw == null) {
            throw new IllegalArgumentException("Parameter '" + key + "' is required");
        }
        String value = String.valueOf(raw).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Parameter '" + key + "' must not be empty");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException("Parameter '" + key + "' exceeds the maximum length of "
                    + maxLength + " characters");
        }
        return value;
    }

    private static Integer optionalInt(Map<String, Object> params, String key) {
        Object raw = params == null ? null : params.get(key);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + key + "' must be an integer");
        }
    }
}
