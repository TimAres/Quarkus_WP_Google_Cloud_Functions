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

/**
 * Service class responsible for communicating with the Google Cloud Vision API
 * and extracting structured invoice data (store, date, total) from raw images.
 */
@ApplicationScoped
public class VisionService {

    /**
     * Sends a base64 encoded image to the Google Vision API and extracts the text.
     * * @param base64Image The image string in base64 format received from the frontend
     * @return InvoiceData record containing the parsed store, date, and total amount
     */
    public InvoiceData extractInvoiceData(String base64Image) {
        try {
            // Decode the base64 string back into raw image bytes
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            Image image = Image.newBuilder().setContent(ByteString.copyFrom(imageBytes)).build();

            // Use DOCUMENT_TEXT_DETECTION which is optimized for dense text and documents like receipts
            Feature feature = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();

            // Build the request object for the API
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .setImage(image)
                    .addFeatures(feature)
                    .build();

            // Create the client using Application Default Credentials (ADC) / Service Account identity
            // The try-with-resources block ensures the client is closed properly to prevent memory leaks
            try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {

                // Send the request to Google Cloud
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));

                // Check if the API returned any valid text responses
                if (response.getResponsesCount() == 0 || !response.getResponses(0).hasFullTextAnnotation()) {
                    return new InvoiceData("No text found", "", "");
                }

                // Extract the raw, unformatted text block from the response
                String rawText = response.getResponses(0).getFullTextAnnotation().getText();
                if (rawText == null || rawText.isBlank()) {
                    return new InvoiceData("No text found", "", "");
                }

                // Pass the raw text to our custom parsing logic
                return parseInvoiceText(rawText);
            }
        } catch (Exception e) {
            // Catch any processing or network exceptions and return them gracefully to the frontend
            return new InvoiceData("Exception", e.getMessage(), "");
        }
    }

    /**
     * Parses the raw, unformatted text returned by the Vision API using Regex and heuristics
     * to find the store name, date, and total amount.
     * * @param rawText The complete text block recognized by the OCR
     * @return InvoiceData record with structured information
     */
    private InvoiceData parseInvoiceText(String rawText) {
        String store = "Unknown";
        String date = "Unknown";
        String total = "Unknown";

        // Split the raw text block into individual lines for line-by-line analysis
        String[] lines = rawText.split("\n");

        // 1. Extract the Store Name:
        // We assume the store name is located at the very top of the receipt.
        // Therefore, we only check the first 5 lines and grab the first non-empty line.
        for (int i = 0; i < Math.min(5, lines.length); i++) {
            String line = lines[i].trim();
            // Ignore empty lines and lines containing garbage characters like '←'
            if (!line.isEmpty() && !line.contains("←")) {
                store = line;
                break;
            }
        }

        // 2. Extract Date and Total Amount:
        // Iterate through all lines to find patterns matching dates and prices.
        for (String line : lines) {
            String trimmed = line.trim();

            // Regex check for common German date formats (e.g., DD.MM.YY or DD.MM.YYYY)
            // Only capture the first matched date to avoid picking up arbitrary numbers
            if (trimmed.matches(".*\\d{2}\\.\\d{2}\\.(20)?\\d{2}.*") && date.equals("Unknown")) {
                date = trimmed;
            }

            // Regex check for the Total Amount:
            // Look for signal words like SUMME, GESAMT, TOTAL, or lines containing a currency pattern (digits, comma, 2 digits)
            if (trimmed.toUpperCase().matches(".*(SUMME|GESAMT|TOTAL).*") || trimmed.matches(".*\\d+[.,]\\d{2}.*")) {
                // If the line contains an actual price pattern, format it and assign it as the total
                if (trimmed.matches(".*\\d+[.,]\\d{2}.*")) {
                    total = trimmed + " €";
                }
            }
        }

        return new InvoiceData(store, date, total);
    }
}