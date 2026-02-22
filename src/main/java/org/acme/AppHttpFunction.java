package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/invoice")
public class AppHttpFunction {

    @Inject
    VisionService visionService;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public InvoiceData processInvoice(String base64Image) {
        return visionService.extractInvoiceData(base64Image);
    }
}