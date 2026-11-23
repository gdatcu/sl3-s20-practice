package ro.qatester.api.v1.async;

import org.hamcrest.Matcher;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

/** Capitolul 5.10 - Solutie Exercitiul 3: "Paznicul Universal" pentru API-ul v1. */
public final class ApiPollerV1 {

    private static final String BASE_URI = "https://apps.qualiadept.eu/hapifyme/api/v1";

    private ApiPollerV1() { }   // clasa utilitara - nu se instantiaza

    /**
     * Face GET pe path (cu Bearer token) pana cand valoarea de la jsonPath respecta matcher-ul.
     * @return valoarea care a indeplinit conditia
     */
    public static <T> T asteaptaValoare(String alias, String token, String path,
                                        String jsonPath, Matcher<? super T> matcher) {
        return await(alias)
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofSeconds(1))
                .ignoreExceptions()
                .until(() -> given()
                                .baseUri(BASE_URI)
                                .auth().oauth2(token)
                                .accept("application/json")
                             .when()
                                .get(path)
                             .then()
                                .statusCode(200)
                                .extract().<T>path(jsonPath),
                       matcher);
    }
}
