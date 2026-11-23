package utils;

import java.util.concurrent.Callable;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/** Capitolul 3.3 - metoda helper (DRY). */
public final class WaitUtils {

    private WaitUtils() { }

    /** Metoda generica pentru orice conditie */
    public static void waitForCondition(String alias, Callable<Boolean> condition) {
        await()
                .alias(alias)
                .atMost(10, SECONDS)
                .pollInterval(1, SECONDS)
                .ignoreExceptions()
                .until(condition);
    }
}
