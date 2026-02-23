package org.acme;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class AppHttpFunctionTest {

    // We don't want to call the real Google Vision API during our CI/CD pipeline runs.
    // It would cost money and would require us to expose our secret JSON credentials on GitHub.
    // Therefore, I use @InjectMock to replace the real VisionService with a dummy/mock.
    @InjectMock
    VisionService visionServiceMock;

    @Test
    public void testProcessInvoiceEndpoint() {
        // 1. Preparation (Arrange)
        // Here I define exactly what our mock should return when the endpoint is called.
        // This simulates a successful, correctly parsed Google Vision API response.
        InvoiceData dummyResponse = new InvoiceData("TEST_STORE", "01.01.2026", "99,99 €");

        // I tell Mockito: Whenever the extractInvoiceData method is called with ANY string,
        // please intercept the call and return our dummyResponse instead of making a real API call.
        Mockito.when(visionServiceMock.extractInvoiceData(Mockito.anyString()))
                .thenReturn(dummyResponse);

        // 2. Execution & Assertion (Act & Assert)
        // Now we simulate an actual HTTP POST request to our function, just like Postman or our frontend would do.
        given()
                .header("Content-Type", "text/plain")
                // I'm sending a simple Base64 string ("this is a fake image") to trigger the endpoint.
                .body("dGhpcyBpcyBhIGZha2UgaW1hZ2U=")
                .when()
                .post("/invoice")
                .then()
                // First, we expect a successful HTTP 200 OK status code.
                .statusCode(200)
                // Finally, I verify that the returned JSON matches our mocked data exactly.
                // This proves that our REST endpoint correctly serializes the Java Record to JSON.
                .body("store", is("TEST_STORE"))
                .body("date", is("01.01.2026"))
                .body("total", is("99,99 €"));
    }
}