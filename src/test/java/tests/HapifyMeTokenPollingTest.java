package tests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.ClassicApi;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Capitolul 2.8 - Solutie Exercitiul 4 (polling pe hapifyMe).
 * Folosim until(Callable, Matcher) ca sa obtinem si VALOAREA token-ului.
 */
public class HapifyMeTokenPollingTest {

    private ClassicApi.TestUser user;

    @BeforeClass
    public void setUp() {
        user = ClassicApi.inregistreaza("Polling", "Tester");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() {
        ClassicApi.sterge(user);
    }

    @Test
    public void testPollingConfirmationToken() {
        String token = await()
                .alias("confirmation_token apare pentru " + user.email)
                .atMost(10, SECONDS)
                .pollInterval(1, SECONDS)
                .until(() -> given()
                                .baseUri(ClassicApi.BASE_URI)
                                .header("Authorization", user.apiKey)
                                .queryParam("username_or_email", user.email)
                            .when()
                                .get("/user/retrieve_token.php")
                            .then()
                                .statusCode(200)
                                .extract().<String>path("confirmation_token"),
                       notNullValue());

        // Token-ul din polling este acelasi cu cel primit direct la register
        assertThat(token, equalTo(user.confirmationToken));
    }
}
