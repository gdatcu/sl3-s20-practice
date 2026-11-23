package com.hapifyme.api.utils;

/** Date unice pentru fiecare rulare. */
public final class DataGenerator {

    private DataGenerator() { }

    public static String emailUnic() {
        return "test_user_" + System.currentTimeMillis() + "@hapifyme.com";
    }

    public static String parolaTest() {
        return "Pass1234!";
    }
}
