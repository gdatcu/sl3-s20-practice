package tests;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/** Capitolul 3.4 - Awaitility dincolo de API: fisiere si baze de date (simulate local). */
public class FileAndDatabaseWaitTest {

    @Test(description = "Exemplu 1: asteptarea unui fisier descarcat")
    public void testAsteaptaFisier() throws IOException {
        Path report = Files.createTempDirectory("awaitility").resolve("report.pdf");

        // Simulam o descarcare care dureaza 2 secunde
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Files.writeString(report, "%PDF-1.4 continut raport");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();

        // Asteptam pana fisierul exista SI are continut (nu e gol)
        await().atMost(30, SECONDS).until(() -> Files.exists(report) && Files.size(report) > 0);
    }

    /** Inlocuitor simplu pentru databaseHelper din curs: un "tabel" in memorie. */
    static class FakeDatabaseHelper {
        private final List<String> users = new CopyOnWriteArrayList<>();

        void insereazaCuIntarziere(String email, long ms) {
            new Thread(() -> {
                try {
                    Thread.sleep(ms);
                    users.add(email);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        int getRecordCount(String email) {
            return (int) users.stream().filter(email::equals).count();
        }
    }

    @Test(description = "Exemplu 2: verificarea in baza de date (simulata)")
    public void testAsteaptaInregistrareInDb() {
        FakeDatabaseHelper databaseHelper = new FakeDatabaseHelper();
        databaseHelper.insereazaCuIntarziere("test@example.com", 1500);

        await().atMost(5, SECONDS).until(() -> databaseHelper.getRecordCount("test@example.com") == 1);
    }
}
