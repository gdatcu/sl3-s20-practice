package tests;

import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * Capitolul 3.1 - ignoreException().
 * Varianta din curs returna mereu "test@example.com"; aici simulam un API care
 * returneaza null la primele apeluri, ca sa vedem efectiv exceptia ignorata.
 */
public class TestWithIgnoredExceptions {

    private final AtomicInteger apeluri = new AtomicInteger();

    /** Primele 3 apeluri: null (API-ul inca nu a creat userul). Apoi: email valid. */
    private String getEmailFromApi() {
        return apeluri.incrementAndGet() <= 3 ? null : "test@example.com";
    }

    @Test
    public void testWithIgnoredExceptions() {
        apeluri.set(0);

        await()
                .atMost(10, SECONDS)
                .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .ignoreException(NullPointerException.class) // ignoram NPE cat timp email-ul e null
                .untilAsserted(() -> {
                    String email = getEmailFromApi();
                    if (!email.contains("@")) {                // NPE cand email == null -> ignorata
                        throw new AssertionError("Email invalid inca");
                    }
                });

        System.out.println("Email valid dupa " + apeluri.get() + " apeluri");
    }
}
