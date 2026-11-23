package tests;

import org.testng.annotations.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.fieldIn;
import static org.hamcrest.Matchers.equalTo;

/** Capitolul 2.5 - Accesarea campurilor private (White Box) cu fieldIn(). */
public class PrivateFieldWaitTest {

    /** Clasa "aplicatiei": campul isActive este privat si nu are getter. */
    static class User {
        private volatile boolean isActive = false;

        void activeazaInFundal() {
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    isActive = true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    @Test
    public void testAsteaptaCampPrivat() {
        User user = new User();
        user.activeazaInFundal();

        await()
                .atMost(5, SECONDS)
                .until(fieldIn(user).ofType(boolean.class).andWithName("isActive"), equalTo(true));
    }
}
