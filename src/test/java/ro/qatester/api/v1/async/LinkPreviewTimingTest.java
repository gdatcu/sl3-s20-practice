package ro.qatester.api.v1.async;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import org.testng.annotations.Test;
import ro.qatester.api.v1.BaseApiTest;

import static org.hamcrest.Matchers.*;

/** Capitolul 5.4 - raspuns LENT, dar SINCRON: .time() + timeout HTTP, fara Awaitility. */
public class LinkPreviewTimingTest extends BaseApiTest {

    @Test(description = "GET /links/preview - raspuns LENT, dar SINCRON")
    public void linkPreviewLent() {
        anonim()
                .config(RestAssured.config().httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 5000)    // max 5 s pentru conectare
                        .setParam("http.socket.timeout", 15000)))     // max 15 s fara date -> exceptie
                .queryParam("url", "https://httpbin.org/delay/3")     // pagina tinta raspunde in 3 s
            .when()
                .get("/links/preview")
            .then()
                .spec(ok)
                .body("data.domain", equalTo("httpbin.org"))
                .time(both(greaterThan(3000L)).and(lessThan(15000L)));  // SLA in milisecunde
    }
}
