package utils;

import org.testng.IClassListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestClass;

/**
 * Aplica HttpDefaults inainte de fiecare suita si de fiecare clasa de test.
 * Inregistrat in toate fisierele testng-*.xml si prin META-INF/services
 * (deci functioneaza si la "Run" pe o singura clasa din IntelliJ sau cu mvn -Dtest=...).
 */
public class HttpDefaultsListener implements ISuiteListener, IClassListener {

    @Override
    public void onStart(ISuite suite) {
        HttpDefaults.apply();
    }

    @Override
    public void onBeforeClass(ITestClass testClass) {
        HttpDefaults.apply();
    }

    @Override
    public void onAfterClass(ITestClass testClass) {
        // nimic
    }
}
