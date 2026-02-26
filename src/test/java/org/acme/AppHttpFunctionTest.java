package org.acme;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.inject.Inject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
public class AppHttpFunctionTest {

    // Wir injizieren deine Funktion direkt, um sie wie eine normale Klasse zu testen
    @Inject
    AppHttpFunction function;

    @InjectMock
    VisionService visionServiceMock;

    @Test
    public void testProcessInvoiceEndpointWithValidAuth() throws Exception {
        // 1. Arrange (Vorbereiten)
        InvoiceData dummyResponse = new InvoiceData("TEST_STORE", "01.01.2026", "99,99 €");
        Mockito.when(visionServiceMock.extractInvoiceData(anyString())).thenReturn(dummyResponse);

        // Wir simulieren die Google Cloud Klassen, statt echte HTTP-Requests zu feuern
        HttpRequest request = Mockito.mock(HttpRequest.class);
        HttpResponse response = Mockito.mock(HttpResponse.class);

        // Wir tun so, als hätte der Request das korrekte Dummy-Passwort und einen Body
        Mockito.when(request.getFirstHeader("X-Auth-Token")).thenReturn(Optional.of("DummyTestSecret123"));
        Mockito.when(request.getReader()).thenReturn(new BufferedReader(new StringReader("dGhpcyBpcyBhIGZha2UgaW1hZ2U=")));

        // Wir fangen die Antwort ab, um sie später prüfen zu können
        StringWriter stringWriter = new StringWriter();
        BufferedWriter writer = new BufferedWriter(stringWriter);
        Mockito.when(response.getWriter()).thenReturn(writer);

        // 2. Act (Ausführen)
        function.service(request, response);
        writer.flush();

        // 3. Assert (Prüfen)
        // Hat unsere Funktion den richtigen Content-Type gesetzt und das JSON geschrieben?
        Mockito.verify(response).setContentType("application/json");
        assertTrue(stringWriter.toString().contains("TEST_STORE"));
    }

    @Test
    public void testProcessInvoiceEndpointUnauthorized() throws Exception {
        // 1. Arrange
        HttpRequest request = Mockito.mock(HttpRequest.class);
        HttpResponse response = Mockito.mock(HttpResponse.class);

        // Wir schicken absichtlich ein falsches Passwort mit
        Mockito.when(request.getFirstHeader("X-Auth-Token")).thenReturn(Optional.of("FalschesPasswort"));

        StringWriter stringWriter = new StringWriter();
        BufferedWriter writer = new BufferedWriter(stringWriter);
        Mockito.when(response.getWriter()).thenReturn(writer);

        // 2. Act
        function.service(request, response);
        writer.flush();

        // 3. Assert
        // Hat unsere Funktion das sofort abgeblockt und einen 401 Fehler geworfen?
        Mockito.verify(response).setStatusCode(401);
        assertTrue(stringWriter.toString().contains("Unauthorized"));
    }
}