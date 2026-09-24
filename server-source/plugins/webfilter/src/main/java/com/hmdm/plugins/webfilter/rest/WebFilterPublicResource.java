package com.hmdm.plugins.webfilter.rest;

import com.hmdm.plugins.webfilter.service.WebFilterService;
import com.hmdm.rest.json.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * <p>Device-facing endpoints of the Web Filter. The device does not have a console session,
 * so this follows the same public-device pattern used by sync/deviceinfo resources: the
 * operation is limited to recording a blocked URL for an existing device number.</p>
 */
@Singleton
@Path("/plugins/webfilter/public")
@Api(tags = {"Web Filter device API"})
@Produces(MediaType.APPLICATION_JSON)
public class WebFilterPublicResource {

    private WebFilterService service;

    public WebFilterPublicResource() {
    }

    @Inject
    public WebFilterPublicResource(WebFilterService service) {
        this.service = service;
    }

    @POST
    @Path("/blocked/{number}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reportBlocked(@PathParam("number") @ApiParam("Device number") String number,
                                  Map<String, Object> body) {
        Object raw = body == null ? null : body.get("url");
        String url = raw == null ? null : String.valueOf(raw);
        return service.reportBlocked(number, url) ? Response.OK() : Response.ERROR("plugin.webfilter.error.report");
    }
}
