package utils;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Helper pentru API-ul CLASIC hapifyMe (fisiere .php, API Key static).
 *
 * De ce exista: cheia API si email-ul hardcodate in Capitolele 2.2 si 3.7 nu mai sunt valide
 * pe serverul nou (raspuns 404 "No active API key found..."). Fiecare test isi creeaza
 * acum propriul utilizator si il sterge la final.
 */
public final class ClassicApi {

    public static final String BASE_URI = "https://apps.qualiadept.eu/hapifyme/api";
    public static final String PAROLA = "Pass1234!";

    private ClassicApi() { }

    /** Datele unui utilizator creat prin POST /user/register.php */
    public static class TestUser {
        public final String email;
        public final String username;      // generat de server (ex: ana_classic, ana_classic_1)
        public final String userId;        // vine ca String ("157")
        public final String apiKey;
        public final String confirmationToken;

        TestUser(String email, JsonPath r) {
            this.email = email;
            this.username = r.getString("username");
            this.userId = r.getString("user_id");
            this.apiKey = r.getString("api_key");
            this.confirmationToken = r.getString("confirmation_token");
        }
    }

    /** POST /user/register.php cu un email unic */
    public static TestUser inregistreaza(String prenume, String nume) {
        String email = prenume.toLowerCase() + "_" + System.currentTimeMillis() + "@hapifyme.com";
        JsonPath r = given()
                .baseUri(BASE_URI)
                .contentType(ContentType.JSON)
                .body(Map.of("first_name", prenume, "last_name", nume, "email", email, "password", PAROLA))
            .when()
                .post("/user/register.php")
            .then()
                .statusCode(201)
                .extract().jsonPath();
        return new TestUser(email, r);
    }

    /** POST /user/login.php -> token Bearer. Atentie: login-ul clasic accepta DOAR username-ul. */
    public static String login(TestUser user) {
        return given()
                .baseUri(BASE_URI)
                .header("Authorization", user.apiKey)
                .contentType(ContentType.JSON)
                .body(Map.of("username", user.username, "password", PAROLA))
            .when()
                .post("/user/login.php")
            .then()
                .statusCode(200)
                .extract().path("token");
    }

    /** Curatenie: login + DELETE /user/delete_profile.php cu Bearer token. Nu arunca exceptii. */
    public static void sterge(TestUser user) {
        if (user == null) {
            return;
        }
        try {
            String bearer = login(user);
            given()
                    .baseUri(BASE_URI)
                    .header("Authorization", "Bearer " + bearer)
                .when()
                    .delete("/user/delete_profile.php");
        } catch (Throwable e) {
            System.out.println("Curatenie esuata pentru " + user.username + ": " + e.getMessage());
        }
    }
}
