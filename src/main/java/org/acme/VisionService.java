package org.acme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class VisionService {

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "google.vision.api.key")
    String apiKey;

    public InvoiceData extractInvoiceData(String base64Image) {
        try {
            // 1. Build JSON request payload
            ObjectNode requestRoot = objectMapper.createObjectNode();
            ArrayNode requests = requestRoot.putArray("requests");
            ObjectNode request = requests.addObject();

            ObjectNode image = request.putObject("image");
            image.put("content", base64Image);

            ArrayNode features = request.putArray("features");
            ObjectNode feature = features.addObject();
            feature.put("type", "DOCUMENT_TEXT_DETECTION");

            String jsonPayload = objectMapper.writeValueAsString(requestRoot);

            // 2. Use classic HttpURLConnection (100% GraalVM safe, strictly synchronous, no thread pools)
            URL url = new URI("https://vision.googleapis.com/v1/images:annotate?key=" + apiKey).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // Write payload
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 3. Send and Parse
            int statusCode = conn.getResponseCode();
            if (statusCode != 200) {
                return new InvoiceData("Google API HTTP Error", String.valueOf(statusCode), "");
            }

            String responseBody;
            try (InputStream is = conn.getInputStream()) {
                responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            JsonNode responseRoot = objectMapper.readTree(responseBody);
            JsonNode responsesNode = responseRoot.path("responses").get(0);

            String rawText = responsesNode.path("fullTextAnnotation").path("text").asText();
            if (rawText.isEmpty()) {
                return new InvoiceData("No text found", "", "");
            }

            return parseInvoiceText(rawText);

        } catch (Exception e) {
            return new InvoiceData("Exception", e.getMessage(), "");
        }
    }

    private InvoiceData parseInvoiceText(String rawText) {
        String store = "Unknown";
        String date = "Unknown";
        String total = "Unknown";
        String[] lines = rawText.split("\n");

        for (int i = 0; i < Math.min(5, lines.length); i++) {
            String line = lines[i].trim();
            if (!line.isEmpty() && !line.contains("←")) {
                store = line;
                break;
            }
        }

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.matches(".*\\d{2}\\.\\d{2}\\.(20)?\\d{2}.*") && date.equals("Unknown")) {
                date = trimmed;
            }
            if (trimmed.toUpperCase().matches(".*(SUMME|GESAMT|TOTAL).*") || trimmed.matches(".*\\d+[.,]\\d{2}.*")) {
                if (trimmed.matches(".*\\d+[.,]\\d{2}.*")) {
                    total = trimmed + " €";
                }
            }
        }
        return new InvoiceData(store, date, total);
    }
}