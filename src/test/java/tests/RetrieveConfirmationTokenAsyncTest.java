package tests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.ClassicApi;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Capitolul 2.2 - untilAsserted() pe GET /user/retrieve_token.php.
 * Diferenta fata de material: cheia API si email-ul nu mai sunt hardcodate (cele din curs
 * nu mai exista pe server) - le obtinem dintr-un register facut in @BeforeClass.
 */
public class RetrieveConfirmationTokenAsyncTest {

    private ClassicApi.TestUser user;

    @BeforeClass
    public void setUp() {
        user = ClassicApi.inregistreaza("Async", "Tester");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() {
        ClassicApi.sterge(user);
    }

    @Test
    public void testRetrieveConfirmationTokenAsync() {
        System.out.println("Incepem cautarea token-ului pentru: " + user.email);

        await()
                .alias("Așteptare Token Confirmare Email")
                .atMost(3, SECONDS)      // Renuntam dupa 10 secunde
                .pollInterval(1, SECONDS) // Verificam in fiecare secunda
                .untilAsserted(() ->
                        given()
                                .baseUri(ClassicApi.BASE_URI)
                                .header("Authorization", user.apiKey)        // Auth conform documentatiei
                                .queryParam("username_or_email", user.email) // Parametru obligatoriu
                                .log().all()
                        .when()
                                .get("/user/retrieve_token.php")
                        .then()
                                .statusCode(200)
                                .log().all()
                                .body("status", equalTo("successs"))
                                .body("confirmation_token", notNullValue())); // Conditia critica

        System.out.println("Token-ul a fost generat si gasit!");
    }
}
