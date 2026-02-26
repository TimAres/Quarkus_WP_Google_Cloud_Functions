package org.acme;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class InvoiceResourceTest {

    @InjectMock
    VisionService visionServiceMock;

    @Test
    public void testProcessInvoiceEndpointWithValidAuth() {
        InvoiceData dummyResponse = new InvoiceData("TEST_STORE", "01.01.2026", "99,99 €");
        Mockito.when(visionServiceMock.extractInvoiceData(Mockito.anyString())).thenReturn(dummyResponse);

        given()
                .header("X-Auth-Token", "DummyTestSecret123")
                .body("dGhpcyBpcyBhIGZha2UgaW1hZ2U=")
                .contentType("text/plain")
                .when()
                .post("/invoice")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body(containsString("TEST_STORE"));
    }

    @Test
    public void testProcessInvoiceEndpointUnauthorized() {
        given()
                .header("X-Auth-Token", "FalschesPasswort")
                .body("any")
                .contentType("text/plain")
                .when()
                .post("/invoice")
                .then()
                .statusCode(401)
                .body(containsString("Unauthorized"));
    }

    @Test
    public void testIndexHtmlServed() {
        given()
                .when()
                .get("/")
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString("Bon-Scanner"));
    }
}
