package tests;

import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/** Capitolul 1.9 - Solutie Exercitiul 2: primul wait, fara API. */
public class AsyncTest {

    // volatile: variabila este scrisa de alt thread; fara volatile, testul poate sa nu vada schimbarea
    private static volatile boolean isUserActive = false;

    @Test
    public void testAsyncVariableChange() {
        isUserActive = false;

        // 1. Pornim un proces asincron (simulare server lent)
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Simulam procesare de 3 secunde (sleep e OK aici: simulam serverul, nu testul)
                isUserActive = true;
                System.out.println("DEBUG: Statusul utilizatorului a fost actualizat la TRUE!");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // 2. Asteptam inteligent schimbarea starii
        System.out.println("Start Awaitility: Asteptam ca userul sa devina activ...");

        await()
                .atMost(5, SECONDS)                        // Timp maxim de asteptare
                .pollInterval(500, TimeUnit.MILLISECONDS)  // Verificam la fiecare 0.5s
                .until(() -> isUserActive);                // Conditia de iesire

        System.out.println("Succes: Test trecut! Userul este activ.");
    }
}
