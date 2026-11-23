package ro.qatester.api.v1.async;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ro.qatester.api.v1.models.AuthData;

import java.time.Duration;
import java.util.Map;

import static io.restassured.RestAssured.withArgs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Capitolul 5.9 / 5.10 - solutiile celor 5 exercitii.
 * priority pastreaza ordinea exercitiilor (altfel TestNG le ruleaza alfabetic).
 */
public class HapifyMeAsyncExercitiiTest extends AsyncApiTest {

    private static final long TS = System.currentTimeMillis();
    private String tokenAna, tokenBogdan, userAna, userBogdan;

    @BeforeClass(alwaysRun = true)
    public void pregatireDate() {
        AuthData ana = inregistreaza("Ana", "Tester" + TS, "ex_async_ana_" + TS + "@hapifyme.com");
        AuthData bogdan = inregistreaza("Bogdan", "Tester" + TS, "ex_async_bogdan_" + TS + "@hapifyme.com");
        tokenAna = ana.getToken();       userAna = ana.getUser().getUsername();
        tokenBogdan = bogdan.getToken(); userBogdan = bogdan.getUser().getUsername();

        // Conversatia trebuie sa existe pentru vedeCaScrie()
        ca(tokenAna).pathParam("username", userBogdan).body(Map.of("body", "Salut!"))
            .when().post("/messages/{username}")
            .then().spec(created);
    }

    @AfterClass(alwaysRun = true)
    public void curatenie() {
        if (tokenAna != null)    ca(tokenAna).when().post("/users/close-account");
        if (tokenBogdan != null) ca(tokenBogdan).when().post("/users/close-account");
    }

    // ---------------- Exercitiul 1: Prietenie asincrona ----------------
    @Test(priority = 1)
    public void testPrietenieAsincrona() {
        ca(tokenAna).pathParam("username", userBogdan)
            .when().post("/friends/request/{username}")
            .then().spec(ok).body("data.status", equalTo("sent"));

        asteapta("Bogdan are o cerere de prietenie in asteptare")
                .until(() -> contor(tokenBogdan, "pending_requests"), equalTo(1));

        ca(tokenBogdan).body(Map.of("from_user", userAna, "action", "accept"))
            .when().post("/friends/respond")
            .then().spec(ok).body("data.status", equalTo("friends"));

        asteapta("Ana si Bogdan sunt prieteni")
                .until(() -> ca(tokenAna).when().get("/auth/me")
                                .then().spec(ok)
                                .extract().<Integer>path("data.user.friends_count"),
                       equalTo(1));

        asteapta("Cererea dispare din lista lui Bogdan")
                .until(() -> contor(tokenBogdan, "pending_requests"), equalTo(0));

        // Raspuns la intrebare: la 23.11.2026, prietenia NU genera notificari (unread_count = 0)
        int notifBogdan = ca(tokenBogdan).when().get("/notifications")
                .then().spec(ok).extract().path("data.unread_count");
        System.out.println("Notificari necitite Bogdan dupa cererea de prietenie: " + notifBogdan);
    }

    // ---------------- Exercitiul 2: Masurarea TTL-ului ----------------
    @Test(priority = 2)
    public void testMasoaraTtlTyping() {
        ca(tokenAna).body(Map.of("user_to", userBogdan, "is_typing", true))
            .when().post("/messages/typing").then().spec(ok);
        long start = System.nanoTime();

        asteapta("Indicatorul expira")
                .pollInterval(Duration.ofMillis(500))
                .atMost(Duration.ofSeconds(15))
                .until(() -> vedeCaScrie(tokenBogdan, userAna), is(false));

        long ttlMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
        System.out.printf("TTL masurat: %d ms%n", ttlMs);
        // TTL-ul serverului e ~5 s, dar masuratoarea include si latenta retelei (prin internet: ~6-7 s)
        assertThat(ttlMs, both(greaterThanOrEqualTo(3000L)).and(lessThanOrEqualTo(10000L)));
    }

    // ---------------- Exercitiul 3: ApiPollerV1 ----------------
    @Test(priority = 3)
    public void testApiPollerV1() {
        ca(tokenAna).pathParam("username", userBogdan).body(Map.of("body", "Ping"))
            .when().post("/messages/{username}").then().spec(created);

        int necitite = ApiPollerV1.asteaptaValoare("Bogdan are mesaje necitite",
                tokenBogdan, "/auth/me", "data.user.counts.unread_messages", greaterThanOrEqualTo(1));

        assertThat(necitite, greaterThanOrEqualTo(1));
    }

    // ---------------- Exercitiul 4: Refresh vs. logout ----------------
    @Test(priority = 4)
    public void testRefreshVsLogout() {
        // Utilizator separat: la final ii inchidem contul, fara sa-i afectam pe Ana si Bogdan
        AuthData carol = inregistreaza("Carol", "Tester" + TS, "ex_async_carol_" + TS + "@hapifyme.com");
        String tokenVechi = carol.getToken();

        String tokenNou = ca(tokenVechi).when().post("/auth/refresh")
                .then().spec(ok).extract().path("data.token");

        // 1. Token-ul vechi functioneaza in continuare dupa refresh
        ca(tokenVechi).when().get("/auth/me").then().spec(ok);

        // 2. Logout cu tokenNou -> doar tokenNou este revocat
        ca(tokenNou).body(Map.of()).when().post("/auth/logout").then().spec(ok);
        asteapta("tokenNou este revocat")
                .until(() -> ca(tokenNou).when().get("/auth/me").statusCode(), equalTo(401));
        ca(tokenVechi).when().get("/auth/me").then().spec(ok);

        // 3. Dupa close-account, orice token al contului primeste 403
        ca(tokenVechi).when().post("/users/close-account")
            .then().spec(ok).body("data.status", equalTo("closed"));
        ca(tokenVechi).when().get("/auth/me")
            .then().spec(eroare(403))
                .body("message", equalTo("Unauthorized: This account has been closed"));
    }

    // ---------------- Exercitiul 5: de ce messages_seen = 0? ----------------
    @Test(priority = 5)
    public void testInvestigatieMessagesSeen() {
        int inainte = contor(tokenBogdan, "unread_messages");

        // (1) Ana trimite mesajul
        int msgId = ca(tokenAna).pathParam("username", userBogdan).body(Map.of("body", "Investigatie"))
            .when().post("/messages/{username}")
            .then().spec(created).extract().path("data.id");

        // (2) Badge-ul lui Bogdan creste
        asteapta("unread_messages creste cu 1")
                .until(() -> contor(tokenBogdan, "unread_messages"), equalTo(inainte + 1));

        // (3) Lista de conversatii NU marcheaza nimic ca citit
        ca(tokenBogdan).when().get("/messages/conversations").then().spec(ok);
        assertThat(contor(tokenBogdan, "unread_messages"), equalTo(inainte + 1));

        // (4) GET pe istoric: raspunsul arata opened = false...
        ca(tokenBogdan).pathParam("username", userAna)
            .when().get("/messages/{username}")
            .then().spec(ok)
                .body("data.find { it.id == %s }.opened", withArgs(msgId), equalTo(false));

        // (5) ...dar a marcat mesajele ca citite (efect secundar al unui GET)
        asteapta("Badge-ul ajunge la 0 dupa un simplu GET")
                .until(() -> contor(tokenBogdan, "unread_messages"), equalTo(0));

        // (6) seen nu mai are ce marca
        ca(tokenBogdan).pathParam("username", userAna)
            .when().post("/messages/{username}/seen")
            .then().spec(ok).body("data.messages_seen", equalTo(0));

        // Control: mesaj nou + seen DIRECT (fara GET) -> messages_seen = 1
        ca(tokenAna).pathParam("username", userBogdan).body(Map.of("body", "Control"))
            .when().post("/messages/{username}").then().spec(created);
        ca(tokenBogdan).pathParam("username", userAna)
            .when().post("/messages/{username}/seen")
            .then().spec(ok).body("data.messages_seen", equalTo(1));
    }
}
