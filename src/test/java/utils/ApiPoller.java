package utils;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

/** Capitolul 3.7 - Solutie Exercitiu Bonus: "Paznicul Universal" pentru API-ul clasic. */
public final class ApiPoller {

    private ApiPoller() { }

    /**
     * Asteapta ca un endpoint sa returneze un anumit status.
     * @param url            Endpoint-ul de verificat (URL complet)
     * @param expectedStatus Valoarea asteptata pentru campul "status"
     * @param apiKey         Cheia API (header Authorization)
     */
    public static void pollForStatus(String url, String expectedStatus, String apiKey) {
        System.out.println("Polling URL: " + url + " expecting status: " + expectedStatus);

        await()
                .alias("Asteptare status " + expectedStatus + " pe " + url)
                .atMost(20, SECONDS)
                .pollInterval(2, SECONDS) // Verificam la fiecare 2 secunde
                .untilAsserted(() ->
                        given()
                                .header("Authorization", apiKey)
                        .when()
                                .get(url)
                        .then()
                                .statusCode(200)                           // requestul tehnic e OK
                                .body("status", equalTo(expectedStatus))); // business logic

        System.out.println("Status confirmat!");
    }
}
