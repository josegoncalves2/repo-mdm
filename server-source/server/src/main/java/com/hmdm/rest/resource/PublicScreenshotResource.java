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

import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.Device;
import com.hmdm.rest.json.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>Receives the screen capture a device produced in response to the "screenshot" remote
 * command, and stores it where the control panel already looks for it.</p>
 *
 * <p>The panel has always linked to <code>files/screenshots/&lt;number&gt;.png</code>, which
 * {@link DownloadFilesServlet} serves out of the configured files directory. Nothing ever wrote
 * that file, so the link answered 404 forever and "Solicitar screenshot" appeared to do
 * nothing. This is the missing write side.</p>
 *
 * <p>The endpoint is public because the agent has no panel session, so it is deliberately
 * narrow: the device number must match an enrolled device, the file name is derived from the
 * record found in the database rather than from the request path, and the body is capped. That
 * keeps an unauthenticated caller from choosing where the bytes land or how many of them.</p>
 */
@Singleton
@Path("/public/screenshot")
@Api(tags = {"Screenshot"})
public class PublicScreenshotResource {

    private static final Logger logger = LoggerFactory.getLogger(PublicScreenshotResource.class);

    /**
     * A full-screen PNG from a tablet sits well under this; the cap exists to bound what an
     * unauthenticated request can write to disk, not to constrain legitimate captures.
     */
    private static final int MAX_SIZE_BYTES = 8 * 1024 * 1024;

    private static final String SUBDIRECTORY = "screenshots";

    private UnsecureDAO unsecureDAO;
    private String filesDirectory;

    /**
     * <p>A constructor required by Swagger.</p>
     */
    public PublicScreenshotResource() {
    }

    @Inject
    public PublicScreenshotResource(UnsecureDAO unsecureDAO,
                                    @Named("files.directory") String filesDirectory) {
        this.unsecureDAO = unsecureDAO;
        this.filesDirectory = filesDirectory;
    }

    // =================================================================================================================
    @ApiOperation(
            value = "Upload a device screen capture",
            notes = "Stores the PNG as screenshots/<device number>.png inside the files directory, which is where " +
                    "the control panel reads the last capture from."
    )
    @POST
    @Path("/{number}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadScreenshot(@PathParam("number") @ApiParam("Device number") String number,
                                     InputStream image) {
        try {
            final Device device = this.unsecureDAO.getDeviceByNumber(number);
            if (device == null) {
                logger.warn("Screenshot upload refused: no device enrolled with number '{}'", number);
                return Response.DEVICE_NOT_FOUND_ERROR();
            }

            final File directory = new File(this.filesDirectory, SUBDIRECTORY);
            if (!directory.exists() && !directory.mkdirs()) {
                logger.error("Could not create the screenshot directory {}", directory.getAbsolutePath());
                return Response.INTERNAL_ERROR();
            }

            // The name comes from the persisted record, never from the raw path parameter, so a
            // crafted number cannot walk out of the directory.
            final File target = new File(directory, device.getNumber().replaceAll("[^A-Za-z0-9_.-]", "_") + ".png");

            final byte[] buffer = new byte[8192];
            int total = 0;
            try (OutputStream out = new FileOutputStream(target)) {
                int read;
                while ((read = image.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_SIZE_BYTES) {
                        logger.warn("Screenshot from device '{}' exceeds {} bytes -- discarded",
                                number, MAX_SIZE_BYTES);
                        out.close();
                        //noinspection ResultOfMethodCallIgnored
                        target.delete();
                        return Response.ERROR("error.screenshot.too.large");
                    }
                    out.write(buffer, 0, read);
                }
            }

            if (total == 0) {
                //noinspection ResultOfMethodCallIgnored
                target.delete();
                logger.warn("Empty screenshot body from device '{}' -- nothing stored", number);
                return Response.ERROR("error.screenshot.empty");
            }

            logger.info("Stored a {} byte screenshot for device '{}'", total, number);
            return Response.OK();
        } catch (IOException e) {
            logger.error("Failed to store the screenshot for device '{}'", number, e);
            return Response.INTERNAL_ERROR();
        }
    }
}
