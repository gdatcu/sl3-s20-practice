package ro.qatester.api.v1.async;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionEvaluationLogger;
import org.awaitility.core.ConditionFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import ro.qatester.api.models.RegisterRequest;
import ro.qatester.api.v1.BaseApiTest;
import ro.qatester.api.v1.models.AuthData;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

/** Capitolul 5.2 - Awaitility peste BaseApiTest (Sesiunea 19). */
public abstract class AsyncApiTest extends BaseApiTest {

    protected static final String PAROLA = "Qa!Test2026";

    /** Warm-up: nu pornim suita pana cand API-ul si baza de date nu raspund */
    @BeforeSuite(alwaysRun = true)
    public void asteaptaCaApiulSaFieGata() {
        await("API-ul hapifyMe v1 este healthy")
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(5))
                .ignoreExceptions()               // ConnectException / SocketTimeout la pornirea mediului
                .untilAsserted(() ->
                    given().baseUri(BASE_URI)     // requestSpec nu exista inca in @BeforeSuite
                    .when().get("/health")
                    .then()
                        .statusCode(200)          // 503 = "Database degraded" -> mai incercam
                        .body("data.status", equalTo("healthy"))
                        .body("data.database", equalTo("connected")));
    }

    /** Valori implicite pentru TOATE asteptarile din clasele copil */
    @BeforeClass(alwaysRun = true)
    public void configureazaAwaitility() {
        Awaitility.setDefaultTimeout(Duration.ofSeconds(15));      // implicit: 10 s
        Awaitility.setDefaultPollInterval(Duration.ofMillis(500)); // implicit: 100 ms
        Awaitility.setDefaultPollDelay(Duration.ZERO);             // prima verificare imediat
    }

    @AfterClass(alwaysRun = true)
    public void reseteazaAwaitility() {
        Awaitility.reset();   // setarile sunt STATICE: nu le lasam sa "curga" in alte clase
    }

    /** await() cu alias + log pentru fiecare evaluare a conditiei */
    protected ConditionFactory asteapta(String alias) {
        return await(alias).conditionEvaluationListener(new ConditionEvaluationLogger());
    }

    // ---------- Metode ajutatoare pentru citirea starii ----------

    protected AuthData inregistreaza(String prenume, String nume, String email) {
        return anonim()
                .body(new RegisterRequest(prenume, nume, email, PAROLA))
            .when().post("/auth/register")
            .then().spec(created)
            .extract().jsonPath().getObject("data", AuthData.class);
    }

    /** Un contor din GET /auth/me: unread_messages | unread_notifications | pending_requests */
    protected int contor(String token, String nume) {
        return ca(token).when().get("/auth/me")
                .then().spec(ok)
                .extract().path("data.user.counts." + nume);
    }

    /** Observatorul vede indicatorul "<username> scrie..." in lista lui de conversatii? */
    protected boolean vedeCaScrie(String tokenObservator, String usernameCareScrie) {
        Object valoare = ca(tokenObservator).when().get("/messages/conversations")
                .then().spec(ok)
                .extract().jsonPath()
                .get("data.find { it.partner.username == '" + usernameCareScrie + "' }.is_typing");
        return Boolean.TRUE.equals(valoare);   // conversatie inexistenta -> false, nu NullPointerException
    }
}
