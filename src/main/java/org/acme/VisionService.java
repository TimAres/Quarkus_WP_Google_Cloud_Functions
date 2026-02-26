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
            // ARCHITECTURE DECISION: Force standard HTTP/JSON (REST) instead of gRPC.
            // This prevents crashes in GraalVM native images where dynamic gRPC/Netty loading is problematic.
            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setTransportChannelProvider(ImageAnnotatorSettings.defaultHttpJsonTransportProviderBuilder().build())
                    .build();

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {

                // 1. Decode the Base64 image string provided by the frontend
                byte[] decodedBytes = Base64.getDecoder().decode(base64Image);
                ByteString imgBytes = ByteString.copyFrom(decodedBytes);

                Image img = Image.newBuilder().setContent(imgBytes).build();

                // 2. Use DOCUMENT_TEXT_DETECTION for better results on dense receipt text
                Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();

                AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                        .addFeatures(feat)
                        .setImage(img)
                        .build();

                List<AnnotateImageRequest> requests = new ArrayList<>();
                requests.add(request);

                // 3. Request OCR analysis from Google Cloud Vision
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
                List<AnnotateImageResponse> responses = response.getResponsesList();

                for (AnnotateImageResponse res : responses) {
                    if (res.hasError()) {
                        System.err.println("Google API Error: " + res.getError().getMessage());
                        return new InvoiceData("Google API Error", "", "");
                    }

                    // 4. Pass the full text result to our robust parser
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

    /**
     * Enhanced Parsing Logic: Specifically adjusted for German receipts (Aldi, Apotheke, Rewe, etc.)
     */
    private InvoiceData parseInvoiceText(String rawText) {
        String store = "Unknown";
        String date = "Unknown";
        String total = "Unknown";

        String[] lines = rawText.split("\n");

        // STEP 1: Store Detection
        // Usually the first non-empty line that doesn't contain technical artifacts
        for (int i = 0; i < Math.min(5, lines.length); i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.contains("←") || line.toLowerCase().contains("ebon")) {
                continue;
            }
            store = line;
            break;
        }

        // STEP 2: Iterate lines to find Date and Total Amount
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Date Detection: Matches DD.MM.YYYY and DD.MM.YY (e.g., 02.02.26)
            if (line.matches(".*\\d{2}\\.\\d{2}\\.(20)?\\d{2}.*") && date.equals("Unknown")) {
                date = line;
            }

            // Total Amount Detection: Scan for common German keywords
            String upperLine = line.toUpperCase();
            if (upperLine.contains("SUMME") || upperLine.contains("GESAMT") ||
                    upperLine.contains("ZU ZAHLEN") || upperLine.contains("TOTAL")) {

                // Once keyword is found, look at the same line and the next two lines for a price
                for (int j = i; j <= i + 2 && j < lines.length; j++) {
                    // Match numbers like 16,59 or 22.50
                    if (lines[j].matches(".*\\d+[.,]\\d{2}.*")) {
                        total = lines[j].trim() + " €";
                        break;
                    }
                }
            }
        }

        return new InvoiceData(store, date, total);
    }
}