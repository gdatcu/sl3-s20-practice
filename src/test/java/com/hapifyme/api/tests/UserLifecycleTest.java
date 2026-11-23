package com.hapifyme.api.tests;

import com.hapifyme.api.models.*;
import com.hapifyme.api.utils.ApiPoller;
import com.hapifyme.api.utils.ConfigManager;
import com.hapifyme.api.utils.DataGenerator;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Capitolul 4 - SOLUTIE DE REFERINTA pentru tema (API clasic).
 * Register -> Async Check (Awaitility) -> Confirm -> Login -> Read -> Update -> Delete -> Negative Check
 *
 * Comportamente reale observate la 23.11.2026 (diferite de textul temei):
 *  - GET /user/get_profile.php cere ?user_id=... (fara el: "User ID is required.")
 *  - erorile vin cu HTTP 200 si "status": "error" (ex: profil sters -> "User not found."),
 *    nu cu 404/401 -> Negative Check-ul valideaza body-ul
 *  - login-ul accepta doar username-ul; email-ul primeste 401
 */
public class UserLifecycleTest {

    private static final String PRENUME = "Tema";
    private static final String NUME = "Tester";

    private RequestSpecification spec;
    private final String email = DataGenerator.emailUnic();
    private final String parola = DataGenerator.parolaTest();

    // "stafeta" intre pasi
    private String apiKey, userId, username, confirmationToken, bearerToken;
    private boolean profilSters = false;

    @BeforeClass
    public void setUp() {
        spec = new RequestSpecBuilder()
                .setBaseUri(ConfigManager.get("base.uri"))
                .setContentType(ContentType.JSON)
                .build();
    }

    @Test(description = "1. Create: POST /user/register.php")
    public void pas1_register() {
        RegisterResponse r = given().spec(spec)
                .body(new UserRequest(PRENUME, NUME, email, parola))
            .when().post("/user/register.php")
            .then().statusCode(201)
            .extract().as(RegisterResponse.class);

        assertThat(r.getStatus(), equalTo("success"));
        assertThat(r.getApiKey(), not(emptyOrNullString()));
        assertThat(r.getUserId(), not(emptyOrNullString()));

        apiKey = r.getApiKey();
        userId = r.getUserId();
        username = r.getUsername();   // generat de server: tema_tester, tema_tester_1, ...
    }

    @Test(dependsOnMethods = "pas1_register", description = "2. Async Check: Awaitility pe retrieve_token.php")
    public void pas2_asteaptaConfirmationToken() {
        confirmationToken = ApiPoller.asteaptaConfirmationToken(ConfigManager.get("base.uri"), apiKey, email);
        assertThat(confirmationToken, matchesPattern("[a-f0-9]{32}"));
    }

    @Test(dependsOnMethods = "pas2_asteaptaConfirmationToken", description = "3. Confirm: GET /user/confirm_email.php")
    public void pas3_confirmaContul() {
        given().spec(spec)
                .queryParam("token", confirmationToken)
            .when().get("/user/confirm_email.php")
            .then().statusCode(200)
                .body("status", equalTo("success"))
                .body("message", containsString("confirmed"));
    }

    @Test(dependsOnMethods = "pas3_confirmaContul", description = "4. Authenticate: POST /user/login.php")
    public void pas4_login() {
        LoginResponse r = given().spec(spec)
                .header("Authorization", apiKey)
                .body(new LoginRequest(username, parola))
            .when().post("/user/login.php")
            .then().statusCode(200)
            .extract().as(LoginResponse.class);

        assertThat(r.getMessage(), equalTo("Login successful"));
        assertThat(r.getUser().getEmail(), equalTo(email));
        bearerToken = r.getToken();
        assertThat(bearerToken.split("\\."), arrayWithSize(3)); // JWT = header.payload.semnatura
    }

    @Test(dependsOnMethods = "pas4_login", description = "5. Read & Validate: GET /user/get_profile.php")
    public void pas5_citesteProfilul() {
        ProfileResponse r = given().spec(spec)
                .header("Authorization", apiKey)
                .queryParam("user_id", userId)
            .when().get("/user/get_profile.php")
            .then().statusCode(200)
            .extract().as(ProfileResponse.class);

        assertThat(r.getStatus(), equalTo("success"));
        assertThat(r.getUser().getEmail(), equalTo(email));
        assertThat(r.getUser().getFirstName(), equalTo(PRENUME));
        assertThat(r.getUser().getLastName(), equalTo(NUME));
    }

    @Test(dependsOnMethods = "pas5_citesteProfilul", description = "6. Update: PUT /user/update_profile.php")
    public void pas6_actualizeazaProfilul() {
        given().spec(spec)
                .header("Authorization", apiKey)
                .body(new UpdateProfileRequest(Integer.parseInt(userId), "TemaUpdated", NUME, email, "updated.jpg"))
            .when().put("/user/update_profile.php")
            .then().statusCode(200)
                .body("status", equalTo("success"))
                .body("message", containsString("updated"));

        // Verificam ca modificarea chiar a fost salvata
        given().spec(spec)
                .header("Authorization", apiKey)
                .queryParam("user_id", userId)
            .when().get("/user/get_profile.php")
            .then().statusCode(200)
                .body("user.first_name", equalTo("TemaUpdated"))
                .body("user.profile_pic", equalTo("updated.jpg"));
    }

    @Test(dependsOnMethods = "pas6_actualizeazaProfilul", description = "7. Delete: DELETE /user/delete_profile.php")
    public void pas7_stergeProfilul() {
        given().spec(spec)
                .header("Authorization", "Bearer " + bearerToken)
            .when().delete("/user/delete_profile.php")
            .then().statusCode(200)
                .body("status", equalTo("success"))
                .body("message", equalTo("User profile deleted successfully."));
        profilSters = true;
    }

    /** Plasa de siguranta: daca un pas a picat inainte de pas7, stergem totusi contul creat. */
    @AfterClass(alwaysRun = true)
    public void curatenie() {
        if (profilSters || username == null) {
            return;
        }
        try {
            String token = bearerToken;
            if (token == null) {   // login-ul clasic functioneaza si inainte de confirmarea email-ului
                token = given().spec(spec)
                        .header("Authorization", apiKey)
                        .body(new LoginRequest(username, parola))
                    .when().post("/user/login.php")
                    .then().extract().path("token");
            }
            given().spec(spec)
                    .header("Authorization", "Bearer " + token)
                .when().delete("/user/delete_profile.php");
            System.out.println("Curatenie: contul " + username + " a fost sters in @AfterClass");
        } catch (Throwable e) {
            System.out.println("Curatenie esuata pentru " + username + ": " + e.getMessage());
        }
    }

    @Test(dependsOnMethods = "pas7_stergeProfilul", description = "8. Negative Check: profilul nu mai exista")
    public void pas8_verificareNegativa() {
        given().spec(spec)
                .header("Authorization", apiKey)
                .queryParam("user_id", userId)
            .when().get("/user/get_profile.php")
            .then()
                .statusCode(200)                          // ATENTIE: API-ul clasic raspunde 200 si la eroare
                .body("status", equalTo("error"))
                .body("message", equalTo("User not found."));

        given().spec(spec)
                .header("Authorization", apiKey)
                .body(new LoginRequest(username, parola))
            .when().post("/user/login.php")
            .then()
                .statusCode(401)
                .body("message", equalTo("Invalid username or password"));
    }
}
