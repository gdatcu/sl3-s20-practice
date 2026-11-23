package com.hapifyme.api.utils;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;

/**
 * ApiPoller pentru Tema (Capitolul 4). Fata de varianta din Capitolul 3.7,
 * metoda RETURNEAZA valoarea asteptata (confirmation_token), ca s-o putem folosi la confirmare.
 */
public final class ApiPoller {

    private ApiPoller() { }

    public static String asteaptaConfirmationToken(String baseUri, String apiKey, String usernameOrEmail) {
        return await()
                .alias("confirmation_token generat pentru " + usernameOrEmail)
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(() -> given()
                                .baseUri(baseUri)
                                .header("Authorization", apiKey)
                                .queryParam("username_or_email", usernameOrEmail)
                            .when()
                                .get("/user/retrieve_token.php")
                            .then()
                                .statusCode(200)
                                .extract().<String>path("confirmation_token"),
                       notNullValue());
    }
}
