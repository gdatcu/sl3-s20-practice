package tests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.ClassicApi;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;

/** Capitolul 2.3 (pollDelay vs pollInterval) si 2.4 (alias) pe un endpoint real. */
public class PollDelayVsPollIntervalTest {

    private ClassicApi.TestUser user;

    @BeforeClass
    public void setUp() {
        user = ClassicApi.inregistreaza("Delay", "Tester");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() {
        ClassicApi.sterge(user);
    }

    @Test
    public void testPollDelaySiAlias() {
        await()
                .alias("Asteptare Token Confirmare Email") // Mesaj custom in caz de eroare
                .atMost(20, SECONDS)      // Timeout total
                .pollDelay(2, SECONDS)    // Primele 2s asteptam pasiv (procesare email)
                .pollInterval(3, SECONDS) // Apoi verificam din 3 in 3 secunde
                .untilAsserted(() ->
                        given()
                                .baseUri(ClassicApi.BASE_URI)
                                .header("Authorization", user.apiKey)
                                .queryParam("username_or_email", user.email)
                        .when()
                                .get("/user/retrieve_token.php")
                        .then()
                                .statusCode(200)
                                .body("confirmation_token", notNullValue()));
    }
}
