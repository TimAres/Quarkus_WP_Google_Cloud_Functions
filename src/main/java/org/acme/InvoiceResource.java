package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Path("/")
public class InvoiceResource {

    @Inject
    VisionService visionService;

    @ConfigProperty(name = "APP_PASSWORD", defaultValue = "DummyTestSecret123")
    String appPassword;

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response index() {
        return serveIndexHtml();
    }

    @GET
    @Path("/index.html")
    @Produces(MediaType.TEXT_HTML)
    public Response indexHtml() {
        return serveIndexHtml();
    }

    private Response serveIndexHtml() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("META-INF/resources/index.html")) {
            if (in == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("index.html not found").build();
            }
            String html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return Response.ok(html).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/invoice")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response invoice(String base64Data, @HeaderParam("X-Auth-Token") String token) {
        if (token == null || !appPassword.equals(token)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Unauthorized: Invalid token.\"}").build();
        }
        try {
            InvoiceData result = visionService.extractInvoiceData(base64Data);
            String json = String.format("{\"store\":\"%s\", \"date\":\"%s\", \"total\":\"%s\"}",
                    escapeJson(result.getStore()),
                    escapeJson(result.getDate()),
                    escapeJson(result.getTotal()));
            return Response.ok(json).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + escapeJson(e.getMessage()) + "\"}").build();
        }
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
