package org.acme;

/**
 * Record for the Data i want to extract from the invoice
 * @param store the store name, where the invoice was created
 * @param date the date of the invoice
 * @param total the total amount of the invoice
 */
public record InvoiceData(String store, String date, String total) {
}