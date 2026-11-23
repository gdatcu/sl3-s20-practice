package tests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.ApiPoller;
import utils.ClassicApi;

/** Capitolul 3.7 - Exemplu de utilizare ApiPoller (cu date create pe loc, nu hardcodate). */
public class RetrieveTokenStatusTest {

    private ClassicApi.TestUser user;

    @BeforeClass
    public void setUp() {
        user = ClassicApi.inregistreaza("Poller", "Tester");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() {
        ClassicApi.sterge(user);
    }

    @Test
    public void testRetrieveTokenStatus() {
        String statusUrl = ClassicApi.BASE_URI + "/user/retrieve_token.php?username_or_email=" + user.email;
        ApiPoller.pollForStatus(statusUrl, "success", user.apiKey);
    }
}
