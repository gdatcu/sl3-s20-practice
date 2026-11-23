package tests;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

/** Capitolul 1.8 - Exercitiul 1: dependenta Awaitility este disponibila in proiect. */
public class ConfigurareTest {

    @Test
    public void testAwaitilityEsteDisponibil() {
        ConditionFactory await = Awaitility.await();
        assertNotNull(await, "Awaitility nu este pe classpath - verificati pom.xml si dati Reload Maven Project");
    }
}
