package tests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.ClassicApi;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

/**
 * Capitolul 3.2 - Logica negativa si conditii combinate, pe date REALE.
 *
 * Comportament observat (23.11.2026): dupa GET /user/confirm_email.php,
 * retrieve_token.php raspunde cu "confirmation_token": null -> asteptam DISPARITIA token-ului.
 *
 * Atentie: in curs apare await().untilFalse(() -> ...). In Awaitility 4.x, untilFalse()
 * primeste un AtomicBoolean, NU o lambda - varianta din curs nu compileaza.
 */
public class NegativeLogicTest {

    private ClassicApi.TestUser user;

    @BeforeClass
    public void setUp() {
        user = ClassicApi.inregistreaza("Negativ", "Tester");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() {
        ClassicApi.sterge(user);
    }

    private boolean tokenulEsteInca(String username) {
        String token = given()
                .baseUri(ClassicApi.BASE_URI)
                .header("Authorization", user.apiKey)
                .queryParam("username_or_email", username)
            .when()
                .get("/user/retrieve_token.php")
            .then()
                .statusCode(200)
                .extract().path("confirmation_token");
        return token != null;
    }

    @Test(priority = 1, description = "Asteptarea disparitiei: token-ul devine null dupa confirmare")
    public void testTokenulDispareDupaConfirmare() {
        given()
                .baseUri(ClassicApi.BASE_URI)
                .queryParam("token", user.confirmationToken)
            .when()
                .get("/user/confirm_email.php")
            .then()
                .statusCode(200)
                .body("status", equalTo("success"));

        // Varianta 1: until(Callable, Matcher)
        await().atMost(5, SECONDS).until(() -> tokenulEsteInca(user.email), equalTo(false));

        // Varianta 2: untilFalse(AtomicBoolean) - starea este actualizata de altcineva (aici: un thread)
        AtomicBoolean tokenValid = new AtomicBoolean(true);
        new Thread(() -> tokenValid.set(tokenulEsteInca(user.username))).start();
        await().atMost(5, SECONDS).untilFalse(tokenValid);
    }

    @Test(priority = 2, description = "Conditie combinata: first_name SI profile_pic actualizate")
    public void testConditieCombinata() {
        given()
                .baseUri(ClassicApi.BASE_URI)
                .header("Authorization", user.apiKey)
                .contentType("application/json")
                .body(java.util.Map.of("user_id", Integer.parseInt(user.userId),
                        "first_name", "NegativUpdated", "last_name", "Tester",
                        "email", user.email, "profile_pic", "nou.jpg"))
            .when()
                .put("/user/update_profile.php")
            .then()
                .statusCode(200);

        await().atMost(5, SECONDS).until(() -> {
            io.restassured.path.json.JsonPath profil = given()
                    .baseUri(ClassicApi.BASE_URI)
                    .header("Authorization", user.apiKey)
                    .queryParam("user_id", user.userId)
                .when()
                    .get("/user/get_profile.php")
                .then()
                    .extract().jsonPath();
            return "NegativUpdated".equals(profil.getString("user.first_name"))
                    && "nou.jpg".equals(profil.getString("user.profile_pic"));
        });
    }
}
