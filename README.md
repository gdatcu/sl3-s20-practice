# Sesiunea 20 – Awaitility pe hapifyMe: toate exercițiile

Proiect Maven (Java 17, REST Assured 5.3.0, TestNG 7.8.0, Jackson 2.15.2, Awaitility 4.2.0) cu soluțiile tuturor exercițiilor din **Curs 20 – Material auxiliar**, plus clasele din Sesiunea 19 de care depinde Capitolul 5.

## Rulare

```bash
mvn test                                                            # toate suitele
mvn test -Dsurefire.suiteXmlFiles=src/test/resources/testng-cap1-3.xml   # Capitolele 1-3
mvn test -Dsurefire.suiteXmlFiles=src/test/resources/testng-tema.xml     # Capitolul 4 (tema)
mvn test -Dsurefire.suiteXmlFiles=src/test/resources/testng-async.xml    # Capitolul 5
mvn test -Dtest=AsyncTest                                           # o singură clasă
```

În IntelliJ: click dreapta pe un fișier `testng-*.xml` → **Run**, sau pe o clasă de test → **Run**.

Fiecare test își creează utilizatorii la început și îi șterge / închide la final (`@AfterClass(alwaysRun = true)`).

## Unde găsiți fiecare exercițiu

| Capitol | Exercițiu / secțiune | Clasă |
|---|---|---|
| 1.8 | Exercițiul 1: Configurare | `tests.ConfigurareTest` + `pom.xml` |
| 1.9 | Exercițiul 2: Primul wait (fără API) | `tests.AsyncTest` |
| 2.2 | `untilAsserted()` pe `retrieve_token.php` | `tests.RetrieveConfirmationTokenAsyncTest` |
| 2.3 – 2.4 | `pollDelay` vs `pollInterval`, `alias` | `tests.PollDelayVsPollIntervalTest` |
| 2.5 | Câmpuri private (`fieldIn`) | `tests.PrivateFieldWaitTest` |
| 2.8 | Exercițiul 3: HttpBin delay | `tests.ApiDelayTest` |
| 2.8 | Exercițiul 4: Polling pe hapifyMe | `tests.HapifyMeTokenPollingTest` |
| 3.1 | `ignoreException()` | `tests.TestWithIgnoredExceptions` |
| 3.2 | Logică negativă, condiții combinate | `tests.NegativeLogicTest` |
| 3.3 | Metoda helper (DRY) | `utils.WaitUtils` + `tests.UserCreationTest` |
| 3.4 | Fișiere și baze de date | `tests.FileAndDatabaseWaitTest` |
| 3.7 | Exercițiu Bonus: ApiPoller | `utils.ApiPoller` + `tests.RetrieveTokenStatusTest` |
| 4 | Tema (API clasic) – soluție de referință | `com.hapifyme.api.tests.UserLifecycleTest` |
| 5.2 | `AsyncApiTest` | `ro.qatester.api.v1.async.AsyncApiTest` |
| 5.3 | Fluxul asincron (6 etape) | `ro.qatester.api.v1.async.HapifyMeAsyncFlowTest` |
| 5.4 | Răspuns lent ≠ asincron | `ro.qatester.api.v1.async.LinkPreviewTimingTest` |
| 5.9 – 5.10 | Exercițiile 1–5 | `ro.qatester.api.v1.async.HapifyMeAsyncExercitiiTest` |
| 5.10 | Exercițiul 3: ApiPollerV1 | `ro.qatester.api.v1.async.ApiPollerV1` |
| Anexa 5.A | Tema pe API-ul v1 – soluție de referință | `ro.qatester.api.v1.tema.UserLifecycleV1Test` |
| Sesiunea 19 | `BaseApiTest`, `RegisterRequest`, `LoginRequestV1`, `AuthData`, `UserDto` | `ro.qatester.api...` |

> **Pentru instructor:** pachetele `com.hapifyme.api` și `ro.qatester.api.v1.tema` conțin soluțiile temei. Ștergeți-le (și `testng-tema.xml`) înainte de a da proiectul studenților, dacă tema nu a fost încă predată.

## Diferențe față de material (comportament real, 23.11.2026)

1. **Cheia API și email-ul hardcodate** din 2.2 și 3.7 nu mai există pe server (`404 "No active API key found..."`). Testele își creează utilizatorul cu `utils.ClassicApi`.
2. **`untilFalse(() -> ...)` din 3.2 nu compilează**: în Awaitility 4.x `untilFalse()` primește un `AtomicBoolean`. Proiectul arată ambele variante corecte (`until(..., equalTo(false))` și `untilFalse(AtomicBoolean)`).
3. **3.3 `Math.random() > 0.3`** făcea testul să pice aleator (~3% din rulări). Statusul devine acum `ACTIVE` determinist, după 3 secunde.
4. **Variabila `isUserActive` din 1.9** este acum `volatile`: este scrisă de alt thread.
5. **Tema (Capitolul 4), API-ul clasic:**
   - `GET /user/get_profile.php` cere `?user_id=...` (fără el: `"User ID is required."`);
   - erorile vin cu **HTTP 200** și `"status": "error"` (profil șters → `"User not found."`), nu cu 404/401;
   - login-ul acceptă doar **username-ul** (email → 401) și funcționează și înainte de confirmarea email-ului;
   - `confirmation_token` vine direct în răspunsul de register; după confirmare, `retrieve_token.php` întoarce `null`.

## Stabilitate la rulare prin internet

`utils.HttpDefaults` (aplicat automat de `utils.HttpDefaultsListener`, înregistrat în toate fișierele `testng-*.xml` și în `META-INF/services`) setează pentru toate testele:

- refolosirea conexiunilor (keep-alive) și ignorarea cookie-urilor;
- timeout de conectare 5 s, cu până la 3 reîncercări doar pentru conectare, și timeout de citire 10 s;
- Awaitility ignoră erorile de rețea (`IOException`) și reîncearcă la următorul poll.

În `pom.xml`, `argLine` forțează IPv4 (`-Djava.net.preferIPv4Stack=true`). Clasele din curs (`BaseApiTest`, `AsyncApiTest`) au rămas identice cu materialul.

## Atenție la rulare

- Testele lovesc API-uri reale (`apps.qualiadept.eu`, `httpbin.org`): aveți nevoie de internet.
- Capitolul 5 are teste care așteaptă expirarea reală a indicatorului „scrie...” (~5 s fiecare), deci suita durează câteva minute.
