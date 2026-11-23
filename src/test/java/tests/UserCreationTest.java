package tests;

import org.testng.annotations.Test;
import utils.WaitUtils;

/**
 * Capitolul 3.3 - utilizarea WaitUtils.
 * Varianta din curs folosea Math.random() > 0.3, ceea ce face testul sa pice aleator
 * (~3% din rulari: 0.7^10). Aici statusul devine ACTIVE determinist, dupa 3 secunde.
 */
public class UserCreationTest {

    private volatile long momentCreare;

    private void triggerUserCreation() {
        System.out.println("Creating new user...");
        momentCreare = System.currentTimeMillis();
    }

    /** Simuleaza un backend: PENDING in primele 3 secunde, apoi ACTIVE */
    private String getUserStatus() {
        return System.currentTimeMillis() - momentCreare < 3000 ? "PENDING" : "ACTIVE";
    }

    @Test
    public void testUserCreation() {
        triggerUserCreation();

        // O singura linie curata!
        WaitUtils.waitForCondition("Userul sa devina activ", () -> getUserStatus().equals("ACTIVE"));
    }
}
