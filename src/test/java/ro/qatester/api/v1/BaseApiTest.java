package ro.qatester.api.v1;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.testng.annotations.BeforeClass;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/** Sesiunea 19, Capitolul 6.3 - fundatia pentru API-ul v1 (JWT + Specs). */
public abstract class BaseApiTest {

    protected static final String BASE_URI = "https://apps.qualiadept.eu/hapifyme/api/v1";

    protected RequestSpecification requestSpec;
    protected ResponseSpecification ok;        // 200 + "success": true
    protected ResponseSpecification created;   // 201 + "success": true

    @BeforeClass(alwaysRun = true)
    public void initSpecs() {
        // Printeaza cererea si raspunsul DOAR cand un test pica
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(BASE_URI)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();

        ok = new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .expectBody("success", equalTo(true))
                .build();

        created = new ResponseSpecBuilder()
                .expectStatusCode(201)
                .expectBody("success", equalTo(true))
                .build();
    }

    /** Cerere fara autentificare (endpoint-uri publice) */
    protected RequestSpecification anonim() {
        return given().spec(requestSpec);
    }

    /** Cerere autentificata: adauga automat "Authorization: Bearer <token>" */
    protected RequestSpecification ca(String token) {
        return given().spec(requestSpec).auth().oauth2(token);
    }

    /** Orice eroare v1: { "success": false, "status": cod, "message": "..." } */
    protected ResponseSpecification eroare(int statusCode) {
        return new ResponseSpecBuilder()
                .expectStatusCode(statusCode)
                .expectBody("success", equalTo(false))
                .expectBody("status", equalTo(statusCode))
                .expectBody("message", not(emptyOrNullString()))
                .build();
    }
}
