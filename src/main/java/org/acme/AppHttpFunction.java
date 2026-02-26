package org.acme;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedWriter;
import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class AppHttpFunction implements HttpFunction {

    @Inject
    VisionService visionService;

    // QUARKUS MAGIC: Liest die Cloud-Variable "APP_PASSWORD" automatisch aus.
    // Falls sie nicht existiert (wie beim lokalen Test), wird der Dummy-Wert genommen!
    @ConfigProperty(name = "APP_PASSWORD", defaultValue = "DummyTestSecret123")
    String appPassword;

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {

        // 1. SECURITY GATE
        Optional<String> tokenOpt = request.getFirstHeader("X-Auth-Token");
        String token = tokenOpt.orElse("");

        if (!appPassword.equals(token)) {
            response.setStatusCode(HttpURLConnection.HTTP_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized: Invalid token.\"}");
            return;
        }

        // 2. PAYLOAD AUSLESEN
        String base64Data = request.getReader().lines().collect(Collectors.joining());

        // 3. PROCESS
        try {
            InvoiceData result = visionService.extractInvoiceData(base64Data);

            response.setContentType("application/json");
            BufferedWriter writer = response.getWriter();

            String jsonResponse = String.format("{\"store\":\"%s\", \"date\":\"%s\", \"total\":\"%s\"}",
                    escapeJson(result.getStore()),
                    escapeJson(result.getDate()),
                    escapeJson(result.getTotal()));

            writer.write(jsonResponse);

        } catch (Exception e) {
            response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"");
    }
}