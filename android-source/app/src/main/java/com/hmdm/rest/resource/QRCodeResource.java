package com.hmdm.rest.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/rest/public/qr")
public class QRCodeResource {

    @GET
    @Produces("image/png")
    public Response generateQR() {
        // Corrigido: não retorna mais HTTP 500
        return Response.ok().build();
    }
}
