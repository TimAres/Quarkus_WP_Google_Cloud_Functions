package org.acme;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@ApplicationScoped
public class VisionService {

    public InvoiceData extractInvoiceData(String base64Image) {
        try {
            // ARCHITECTURE DECISION:
            // We explicitly force the Google Cloud Vision Client to use standard HTTP/JSON (REST)
            // instead of the default gRPC protocol.
            // Why? Because GraalVM (Native Image) aggressively removes dynamic gRPC/Netty classes
            // during the Ahead-of-Time (AOT) compilation, which would cause a crash in Cloud Run.
            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setTransportChannelProvider(ImageAnnotatorSettings.defaultHttpJsonTransportProviderBuilder().build())
                    .build();

            // Initialize the client with our custom REST settings
            try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {

                // 1. Convert the Base64 string back into raw bytes
                byte[] decodedBytes = Base64.getDecoder().decode(base64Image);
                ByteString imgBytes = ByteString.copyFrom(decodedBytes);

                // 2. Build the Image object required by the Google Vision API
                Image img = Image.newBuilder().setContent(imgBytes).build();

                // 3. Set the mode: We want document text detection for dense texts like invoices
                Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();

                // 4. Assemble the request
                AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                        .addFeatures(feat)
                        .setImage(img)
                        .build();

                List<AnnotateImageRequest> requests = new ArrayList<>();
                requests.add(request);

                // 5. Fire the request to the Google API
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
                List<AnnotateImageResponse> responses = response.getResponsesList();

                // 6. Evaluate the response
                for (AnnotateImageResponse res : responses) {
                    if (res.hasError()) {
                        System.err.println("Error from Google API: " + res.getError().getMessage());
                        return new InvoiceData("Google API Error", "", "");
                    }

                    // Extract the raw text and pass it to our custom parser
                    String rawText = res.getFullTextAnnotation().getText();
                    return parseInvoiceText(rawText);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            return new InvoiceData("Exception", e.getMessage(), "");
        }

        return new InvoiceData("No text found", "", "");
    }

    // Helper method: Parses the raw text to extract store, date, and total amount
    private InvoiceData parseInvoiceText(String rawText) {
        String store = "Unknown";
        String date = "Unknown";
        String total = "Unknown";

        // Split the entire text at line breaks
        String[] lines = rawText.split("\n");

        // We check the first up to 5 lines to find the store name
        for (int i = 0; i < Math.min(5, lines.length); i++) {
            String line = lines[i].trim();

            // If the line contains arrows, "eBon", or is empty, it's garbage -> skip it
            if (line.contains("←") || line.contains("eBon") || line.isEmpty()) {
                continue;
            }

            // We take the very first clean line as the store name
            store = line;
            break;
        }

        // Iterate over all lines to find the date and total
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Search for date using a regex pattern (Pattern: DD.MM.YYYY)
            if (line.matches(".*\\d{2}\\.\\d{2}\\.\\d{4}.*") && date.equals("Unknown")) {
                date = line;
            }

            // Search for the total amount (We look for "SUMME" and check the following 2 lines)
            if (line.toUpperCase().contains("SUMME")) {
                for (int j = i + 1; j <= i + 2 && j < lines.length; j++) {
                    // We look for a number with a comma and exactly two decimal places (e.g., 16,59)
                    if (lines[j].matches(".*\\d+,\\d{2}.*")) {
                        total = lines[j].trim() + " €";
                        break; // Break the inner loop once the total is found
                    }
                }
            }
        }

        return new InvoiceData(store, date, total);
    }
}