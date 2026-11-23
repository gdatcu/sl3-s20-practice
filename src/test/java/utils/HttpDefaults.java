package utils;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.awaitility.Awaitility;

import java.io.IOException;

/**
 * Setari HTTP comune pentru TOATE testele (aplicate automat de HttpDefaultsListener).
 *
 * De ce: la rularea suitei prin internet, conexiunile noi catre apps.qualiadept.eu se blocau
 * uneori 7-69 de secunde. Fara timeout, un singur request blocat consuma tot atMost-ul din Awaitility.
 *
 *  - conexiunile sunt refolosite (keep-alive) -> mult mai putine conexiuni TLS noi pe secunda;
 *  - connect timeout 5 s + pana la 3 reincercari DOAR pentru conectare (requestul nu a plecat inca);
 *  - socket (read) timeout 10 s;
 *  - cookie-urile sunt ignorate: clientul refolosit NU are voie sa amestece sesiunile Anei si ale lui Bogdan;
 *  - Awaitility ignora IOException-urile (timeout-uri de retea) si reincearca la urmatorul poll.
 */
public final class HttpDefaults {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int MAX_CONNECT_RETRIES = 3;

    private static volatile boolean httpConfigured = false;

    private HttpDefaults() { }

    /** Idempotent: se poate apela de oricate ori. */
    public static void apply() {
        if (!httpConfigured) {
            synchronized (HttpDefaults.class) {
                if (!httpConfigured) {
                    RestAssured.config = RestAssured.config().httpClient(
                            HttpClientConfig.httpClientConfig()
                                    .httpClientFactory(HttpDefaults::createClient)
                                    .reuseHttpClientInstance()
                                    .setParam(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
                                    .setParam(HttpConnectionParams.CONNECTION_TIMEOUT, CONNECT_TIMEOUT_MS)
                                    .setParam(HttpConnectionParams.SO_TIMEOUT, READ_TIMEOUT_MS));
                    httpConfigured = true;
                }
            }
        }
        // Awaitility.reset() (din AsyncApiTest) sterge si aceasta setare, deci o reaplicam la fiecare clasa
        Awaitility.ignoreExceptionsByDefaultMatching(e -> e instanceof IOException);
    }

    @SuppressWarnings("deprecation")   // API-ul HttpClient 4.x folosit intern de REST Assured 5.x
    private static DefaultHttpClient createClient() {
        PoolingClientConnectionManager pool = new PoolingClientConnectionManager();
        pool.setMaxTotal(20);
        pool.setDefaultMaxPerRoute(10);   // sigur si cand Awaitility face poll dintr-un alt thread

        DefaultHttpClient client = new DefaultHttpClient(pool);
        HttpParams params = client.getParams();
        HttpConnectionParams.setConnectionTimeout(params, CONNECT_TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(params, READ_TIMEOUT_MS);
        params.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);

        // Reincercam DOAR conectarea esuata (cererea nu a ajuns la server, deci e sigur de repetat)
        client.setHttpRequestRetryHandler((IOException ex, int executionCount, org.apache.http.protocol.HttpContext ctx) ->
                executionCount <= MAX_CONNECT_RETRIES
                        && (ex instanceof ConnectTimeoutException || ex instanceof NoHttpResponseException));
        return client;
    }
}
