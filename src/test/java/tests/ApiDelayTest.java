package tests;

import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * Capitolul 2.8 - Solutie Exercitiul 3 (HttpBin Delay).
 * Nota: /delay/5 este un raspuns LENT, dar SINCRON. Vezi Capitolul 5.4 pentru
 * varianta recomandata (.time() + timeout HTTP) - aici pastram exercitiul din curs.
 */
public class ApiDelayTest {

    @Test
    public void testHttpBinDelay() {
        System.out.println("Start request cu delay...");

        await()
                .alias("Asteptare raspuns httpbin")
                .atMost(10, SECONDS)
                .pollInterval(1, SECONDS)
                .untilAsserted(() ->
                        given()
                                .baseUri("https://httpbin.org")
                        .when()
                                .get("/delay/5") // Serverul face sleep 5s
                        .then()
                                .statusCode(200));

        System.out.println("Raspuns primit!");
    }
}
