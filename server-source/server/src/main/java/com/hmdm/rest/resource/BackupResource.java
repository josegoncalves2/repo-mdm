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

import com.hmdm.rest.json.BackupInfo;
import com.hmdm.rest.json.RestoreResult;
import com.hmdm.rest.json.Response;
import com.hmdm.security.SecurityContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;
import org.apache.poi.util.IOUtils;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * <p>Manages full PostgreSQL database backups: create, list, download, delete and restore.
 * Backups are stored at {@link #BACKUP_DIR}, a path outside the application container's own
 * filesystem (mounted from the host), so they survive container recreation.</p>
 *
 * <p>Every operation here is gated behind the "settings" permission (same gate as
 * {@link SettingsResource}): a restore overwrites the entire live database, so this needs
 * to stay restricted to admin-level roles, not every panel user.</p>
 */
@Api(tags = {"Backup"}, authorizations = {@Authorization("Bearer Token")})
@Singleton
@Path("/private/backup")
public class BackupResource {

    private static final Logger log = LoggerFactory.getLogger(BackupResource.class);

    private static final File BACKUP_DIR = new File("/opt/hmdm/backups");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @com.google.inject.Inject private com.hmdm.service.backup.BackupArchiveService archives;

    public BackupResource() {
    }

    // =================================================================================================================
    @ApiOperation(
            value = "List backups",
            notes = "Lists the database backup files stored on the server",
            response = BackupInfo.class,
            responseContainer = "List"
    )
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        if (!SecurityContext.get().hasPermission("settings")) {
            return Response.PERMISSION_DENIED();
        }
        try {
            List<BackupInfo> result = new ArrayList<>();
            File[] files = BACKUP_DIR.isDirectory() ? BACKUP_DIR.listFiles((dir, name) -> (name.endsWith(".sql") || name.endsWith(".zip"))) : null;
            if (files != null) {
                for (File file : files) {
                    result.add(toBackupInfo(file));
                }
            }
            result.sort(Comparator.comparing(BackupInfo::getCreatedAt).reversed());
            return Response.OK(result);
        } catch (Exception e) {
            log.error("Unexpected error when listing database backups", e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Create a backup",
            notes = "Runs pg_dump against the live database and stores the result",
            response = BackupInfo.class
    )
    @POST
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@javax.ws.rs.QueryParam("scope") @javax.ws.rs.DefaultValue("full") String scope) {
        if (!SecurityContext.get().hasPermission("settings")) {
            return Response.PERMISSION_DENIED();
        }
        try {
            BackupInfo info = toBackupInfo(archives.create(scope, false));
            return Response.OK(info);
        } catch (Exception e) {
            log.error("Unexpected error when creating a database backup", e);
            return Response.ERROR("Backup failed: " + e.getMessage());
        }
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Delete a backup",
            notes = "Deletes a database backup file from the server"
    )
    @DELETE
    @Path("/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response remove(@PathParam("filename") String filename) {
        if (!SecurityContext.get().hasPermission("settings")) {
            return Response.PERMISSION_DENIED();
        }
        File file = resolve(filename);
        if (file == null || !file.isFile()) {
            return Response.ERROR("Backup not found");
        }
        if (!file.delete()) {
            return Response.ERROR("Could not delete the backup file");
        }
        return Response.OK();
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Download a backup",
            notes = "Downloads the raw SQL dump of a database backup",
            responseHeaders = {@ResponseHeader(name = "Content-Disposition")}
    )
    @GET
    @Path("/{filename}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public javax.ws.rs.core.Response download(@PathParam("filename") @ApiParam("The backup file name") String filename) {
        if (!SecurityContext.get().hasPermission("settings")) {
            return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.FORBIDDEN).build();
        }
        File file = resolve(filename);
        if (file == null || !file.isFile()) {
            return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
        }
        ContentDisposition contentDisposition = ContentDisposition.type("attachment").fileName(file.getName()).creationDate(new Date()).build();
        return javax.ws.rs.core.Response.ok((StreamingOutput) output -> {
            try (InputStream input = new FileInputStream(file)) {
                IOUtils.copy(input, output);
                output.flush();
            }
        }).header("Content-Disposition", contentDisposition).build();
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Restore a backup",
            notes = "Overwrites the live database with the contents of a backup file. Takes a safety " +
                    "snapshot of the current state first.",
            response = RestoreResult.class
    )
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{filename}/restore")
    public Response restore(@PathParam("filename") String filename) {
        if (!SecurityContext.get().hasPermission("settings")) {
            return Response.PERMISSION_DENIED();
        }
        File target = resolve(filename);
        if (target == null || !target.isFile()) {
            return Response.ERROR("Backup not found");
        }
        try {
            if (filename.endsWith(".zip")) {
                return Response.OK(new RestoreResult(archives.restore(target)));
            }
            BackupInfo safety = dump("pre-restore-safety", true);
            try {
                runPsql(target);
            } catch (Exception restoreFailure) {
                log.error("Restore from {} failed; the live database may be in a partial state. " +
                        "The pre-restore safety backup is {}", filename, safety.getName(), restoreFailure);
                return Response.ERROR("Restore failed: " + restoreFailure.getMessage() +
                        ". A safety snapshot of the previous state was saved as " + safety.getName() + ".");
            }
            return Response.OK(new RestoreResult(safety.getName()));
        } catch (Exception e) {
            log.error("Unexpected error when restoring database backup {}", filename, e);
            return Response.INTERNAL_ERROR();
        }
    }

    // =================================================================================================================

    private BackupInfo dump(String label, boolean safety) throws Exception {
        if (!BACKUP_DIR.isDirectory() && !BACKUP_DIR.mkdirs()) {
            throw new IllegalStateException("Could not create backup directory " + BACKUP_DIR);
        }
        String name = (safety ? "safety-" : "") + label + "-" + TIMESTAMP_FORMAT.format(java.time.LocalDateTime.now()) + ".sql";
        File out = new File(BACKUP_DIR, name);

        ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", env("SQL_HOST", "postgresql"),
                "-U", env("SQL_USER", "hmdm"),
                "-d", env("SQL_BASE", "hmdm"),
                "--clean", "--if-exists"
        );
        pb.environment().put("PGPASSWORD", env("SQL_PASS", "hmdm"));
        pb.redirectErrorStream(false);
        pb.redirectOutput(out);
        Process process = pb.start();
        String stderr = readAll(process.getErrorStream());
        int exit = process.waitFor();
        if (exit != 0) {
            out.delete();
            throw new IllegalStateException("pg_dump exited with code " + exit + ": " + stderr);
        }

        return toBackupInfo(out, safety);
    }

    private void runPsql(File dumpFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "psql",
                "-h", env("SQL_HOST", "postgresql"),
                "-U", env("SQL_USER", "hmdm"),
                "-d", env("SQL_BASE", "hmdm"),
                "--single-transaction",
                "-v", "ON_ERROR_STOP=1",
                "-f", dumpFile.getAbsolutePath()
        );
        pb.environment().put("PGPASSWORD", env("SQL_PASS", "hmdm"));
        pb.redirectErrorStream(false);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process process = pb.start();
        String stderr = readAll(process.getErrorStream());
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("psql exited with code " + exit + ": " + stderr);
        }
    }

    /**
     * <p>Resolves a backup filename to a file inside {@link #BACKUP_DIR}, rejecting anything
     * that is not a bare filename (no path separators, no traversal).</p>
     */
    private File resolve(String filename) {
        if (filename == null || filename.isEmpty()
                || (!filename.endsWith(".sql") && !filename.endsWith(".zip"))
                || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return null;
        }
        return new File(BACKUP_DIR, filename);
    }

    private BackupInfo toBackupInfo(File file) {
        return toBackupInfo(file, file.getName().startsWith("safety-"));
    }

    private BackupInfo toBackupInfo(File file, boolean safety) {
        BackupInfo info = new BackupInfo();
        info.setName(file.getName());
        info.setSizeBytes(file.length());
        info.setCreatedAt(new Date(file.lastModified()));
        info.setPreRestoreSafetyBackup(safety);
        return info;
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isEmpty()) ? fallback : value;
    }

    private static String readAll(InputStream in) throws Exception {
        return new String(IOUtils.toByteArray(in), java.nio.charset.StandardCharsets.UTF_8);
    }
    @GET @Path("/schedule") @Produces(MediaType.APPLICATION_JSON)
    public Response schedule() {
        if (!SecurityContext.get().hasPermission("settings")) { return Response.PERMISSION_DENIED(); }
        try { return Response.OK(archives.getSchedule()); } catch (Exception e) { return Response.ERROR(e.getMessage()); }
    }
    @javax.ws.rs.PUT @Path("/schedule") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response saveSchedule(com.fasterxml.jackson.databind.node.ObjectNode value) {
        if (!SecurityContext.get().hasPermission("settings")) { return Response.PERMISSION_DENIED(); }
        try { return Response.OK(archives.saveSchedule(value)); } catch (Exception e) { return Response.ERROR(e.getMessage()); }
    }
    @POST @Path("/upload") @Consumes(MediaType.APPLICATION_OCTET_STREAM) @Produces(MediaType.APPLICATION_JSON)
    public Response upload(InputStream data) {
        if (!SecurityContext.get().hasPermission("settings")) { return Response.PERMISSION_DENIED(); }
        try { return Response.OK(toBackupInfo(archives.upload(data))); } catch (Exception e) { return Response.ERROR(e.getMessage()); }
    }
    @GET @Path("/{filename}/inspect") @Produces(MediaType.APPLICATION_JSON)
    public Response inspect(@PathParam("filename") String filename) {
        if (!SecurityContext.get().hasPermission("settings")) { return Response.PERMISSION_DENIED(); }
        File file = resolve(filename);
        if (file == null || !file.isFile()) { return Response.ERROR("Backup não encontrado"); }
        try { return Response.OK(archives.inspect(file)); } catch (Exception e) { return Response.ERROR(e.getMessage()); }
    }

}
