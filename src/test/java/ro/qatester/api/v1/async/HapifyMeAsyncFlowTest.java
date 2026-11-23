package ro.qatester.api.v1.async;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ro.qatester.api.v1.models.AuthData;
import ro.qatester.api.v1.models.LoginRequestV1;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.withArgs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/** Capitolul 5.3 - fluxul asincron in 6 etape pe hapifyMe REST API v1. */
public class HapifyMeAsyncFlowTest extends AsyncApiTest {

    // ---------- Date de test UNICE pentru fiecare rulare ----------
    private static final long TS = System.currentTimeMillis();
    private static final String EMAIL_ANA = "ana_async_" + TS + "@hapifyme.com";
    private static final String EMAIL_BOGDAN = "bogdan_async_" + TS + "@hapifyme.com";

    // ---------- "Stafeta" ----------
    private String tokenAna, tokenBogdan, userAna, userBogdan;
    private int postId, messageId;

    @BeforeClass(alwaysRun = true)
    public void pregatireActori() {
        AuthData ana = inregistreaza("Ana", "Tester" + TS, EMAIL_ANA);
        AuthData bogdan = inregistreaza("Bogdan", "Tester" + TS, EMAIL_BOGDAN);
        tokenAna = ana.getToken();
        userAna = ana.getUser().getUsername();
        tokenBogdan = bogdan.getToken();
        userBogdan = bogdan.getUser().getUsername();
    }

    @AfterClass(alwaysRun = true)   // plasa de siguranta: ruleaza chiar daca testele pica
    public void inchideConturi() {
        if (tokenAna != null)    ca(tokenAna).when().post("/users/close-account");
        if (tokenBogdan != null) ca(tokenBogdan).when().post("/users/close-account");
    }

    @Test(description = "Comentariu cu @mention -> notificarea Anei -> mark-read -> badge 0")
    public void etapa01_notificareMention() {
        postId = ca(tokenAna)
                .body(Map.of("body", "Testam Awaitility pe v1 #qaschool" + TS))
            .when().post("/posts")
            .then().spec(created)
            .extract().path("data.id");

        // TRIGGER: Bogdan comenteaza si o mentioneaza pe Ana
        ca(tokenBogdan).pathParam("id", postId)
                .body(Map.of("body", "Super! @" + userAna))
            .when().post("/posts/{id}/comments")
            .then().spec(created);

        // ASTEPTARE: until(Callable, Matcher) RETURNEAZA valoarea care a indeplinit conditia
        List<String> mesaje = asteapta("Notificarea de comentariu ajunge la Ana")
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> ca(tokenAna).when().get("/notifications")
                                .then().spec(ok)
                                .extract().jsonPath().getList("data.notifications.message", String.class),
                       hasItem(containsString("commented on your post")));

        assertThat(mesaje, hasSize(1));                                  // cont nou -> o singura notificare
        assertThat(contor(tokenAna, "unread_notifications"), equalTo(1));

        // Marcam totul ca citit si asteptam ca badge-ul sa ajunga la 0
        ca(tokenAna).when().post("/notifications/mark-read").then().spec(ok);

        asteapta("Badge-ul de notificari al Anei ajunge la 0")
                .until(() -> contor(tokenAna, "unread_notifications"), equalTo(0));
    }

    @Test(dependsOnMethods = "etapa01_notificareMention",
          description = "Mesaj direct -> unread_messages = 1 la Bogdan")
    public void etapa02_contorMesajeNecitite() {
        messageId = ca(tokenAna).pathParam("username", userBogdan)
                .body(Map.of("body", "Salut! Testam Awaitility?"))
            .when().post("/messages/{username}")
            .then().spec(created)
                .body("data.opened", equalTo(false))
            .extract().path("data.id");

        // Varianta 1: o singura valoare -> until(...)
        int necitite = asteapta("Bogdan are 1 mesaj necitit")
                .until(() -> contor(tokenBogdan, "unread_messages"), equalTo(1));

        // Varianta 2: mai multe verificari care trebuie sa treaca IMPREUNA -> untilAsserted(...)
        asteapta("Conversatia cu Ana apare in lista lui Bogdan")
                .untilAsserted(() ->
                    ca(tokenBogdan).when().get("/messages/conversations")
                    .then().spec(ok)
                        .body("data.find { it.partner.username == '%s' }.unread_count",
                              withArgs(userAna), equalTo(1))
                        .body("data.find { it.partner.username == '%s' }.latest_message.id",
                              withArgs(userAna), equalTo(messageId)));

        assertThat(necitite, equalTo(1));
    }

    @Test(dependsOnMethods = "etapa02_contorMesajeNecitite",
          description = "Indicatorul 'scrie...' ramane activ, apoi EXPIRA singur (TTL ~ 5 s)")
    public void etapa03_typingExpiraAutomat() {
        long start = System.currentTimeMillis();
        ca(tokenAna).body(Map.of("user_to", userBogdan, "is_typing", true))
            .when().post("/messages/typing")
            .then().spec(ok);

        // 1. during(): conditia trebuie sa ramana ADEVARATA pe toata durata ferestrei
        asteapta("Indicatorul ramane activ cel putin 2 secunde")
                .during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(4))          // atMost trebuie sa fie mai mare decat during
                .until(() -> vedeCaScrie(tokenBogdan, userAna), is(true));

        // 2. Indicatorul dispare fara nicio alta cerere din partea Anei
        asteapta("Indicatorul 'Ana scrie...' expira")
                .atMost(Duration.ofSeconds(10))
                .until(() -> vedeCaScrie(tokenBogdan, userAna), is(false));

        long ttl = System.currentTimeMillis() - start;
        System.out.println("TTL indicator typing: " + ttl + " ms");
        assertThat(ttl, both(greaterThanOrEqualTo(3000L)).and(lessThan(10000L)));
    }

    @Test(dependsOnMethods = "etapa03_typingExpiraAutomat",
          description = "atLeast(): indicatorul NU expira in mai putin de 3 secunde")
    public void etapa04_typingNuDisparePreaDevreme() {
        ca(tokenAna).body(Map.of("user_to", userBogdan, "is_typing", true))
            .when().post("/messages/typing").then().spec(ok);

        // Daca is_typing devine false INAINTE de 3 s -> testul pica ("earlier than expected")
        asteapta("Indicatorul expira, dar nu mai devreme de 3 secunde")
                .atLeast(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(10))
                .until(() -> vedeCaScrie(tokenBogdan, userAna), is(false));

        // CONTRA-EXEMPLU: oprirea EXPLICITA este sincrona -> NU avem nevoie de Awaitility
        ca(tokenAna).body(Map.of("user_to", userBogdan, "is_typing", true))
            .when().post("/messages/typing").then().spec(ok);
        ca(tokenAna).body(Map.of("user_to", userBogdan, "is_typing", false))
            .when().post("/messages/typing").then().spec(ok);

        assertThat(vedeCaScrie(tokenBogdan, userAna), is(false));   // verificare directa, imediata
    }

    @Test(dependsOnMethods = "etapa04_typingNuDisparePreaDevreme",
          description = "Bogdan deschide conversatia -> mesajul Anei devine opened = true")
    public void etapa05_mesajCitit() {
        // Bogdan deschide conversatia. Atentie: este un GET!
        ca(tokenBogdan).pathParam("username", userAna)
                .queryParam("page", 1).queryParam("limit", 20)
            .when().get("/messages/{username}")
            .then().spec(ok)
                // raspunsul arata starea DE DINAINTE de citire
                .body("data.find { it.id == %s }.opened", withArgs(messageId), equalTo(false));

        // Din perspectiva Anei, mesajul trebuie sa apara ca citit
        asteapta("Mesajul Anei apare ca citit")
                .untilAsserted(() ->
                    ca(tokenAna).pathParam("username", userBogdan)
                        .when().get("/messages/{username}")
                        .then().spec(ok)
                        .body("data.find { it.id == %s }.opened", withArgs(messageId), equalTo(true)));

        asteapta("Badge-ul de mesaje al lui Bogdan ajunge la 0")
                .until(() -> contor(tokenBogdan, "unread_messages"), equalTo(0));

        // seen nu mai are ce marca: GET-ul de mai sus a facut deja treaba
        ca(tokenBogdan).pathParam("username", userAna)
            .when().post("/messages/{username}/seen")
            .then().spec(ok)
                .body("data.status", equalTo("seen"))
                .body("data.messages_seen", equalTo(0));
    }

    @Test(dependsOnMethods = "etapa05_mesajCitit",
          description = "POST /auth/logout revoca DOAR token-ul sesiunii curente")
    public void etapa06_logoutRevocaSesiunea() {
        // O a doua sesiune pentru Ana (ex: telefonul); prima sesiune = token-ul de la register
        String tokenTelefon = anonim().body(new LoginRequestV1(EMAIL_ANA, PAROLA))
            .when().post("/auth/login")
            .then().spec(ok)
            .extract().path("data.token");

        ca(tokenTelefon).body(Map.of())
            .when().post("/auth/logout")
            .then().spec(ok).body("data.status", equalTo("logged_out"));

        // until() pe status code: in sisteme distribuite revocarea poate intarzia (cache, replici)
        asteapta("Token-ul sesiunii de pe telefon este revocat")
                .atMost(Duration.ofSeconds(10))
                .until(() -> ca(tokenTelefon).when().get("/auth/me").statusCode(), equalTo(401));

        // Sesiunea "laptop" trebuie sa ramana valida - o folosim si in @AfterClass
        ca(tokenAna).when().get("/auth/me")
            .then().spec(ok).body("data.user.username", equalTo(userAna));
    }
}
