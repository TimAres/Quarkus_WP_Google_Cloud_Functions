package org.acme;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Base64;
import java.util.List;

@ApplicationScoped
public class VisionService {

    public InvoiceData extractInvoiceData(String base64Image) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            Image image = Image.newBuilder().setContent(ByteString.copyFrom(imageBytes)).build();
            Feature feature = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .setImage(image)
                    .addFeatures(feature)
                    .build();

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));
                if (response.getResponsesCount() == 0 || !response.getResponses(0).hasFullTextAnnotation()) {
                    return new InvoiceData("No text found", "", "");
                }
                String rawText = response.getResponses(0).getFullTextAnnotation().getText();
                if (rawText == null || rawText.isBlank()) {
                    return new InvoiceData("No text found", "", "");
                }
                return parseInvoiceText(rawText);
            }
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
