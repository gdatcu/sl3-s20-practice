package com.hapifyme.api.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Citeste src/test/resources/config.properties (o singura data). */
public final class ConfigManager {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties lipseste din src/test/resources");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Nu pot citi config.properties", e);
        }
    }

    private ConfigManager() { }

    /** Valoarea din fisier; poate fi suprascrisa din linia de comanda: -Dbase.uri=... */
    public static String get(String cheie) {
        return System.getProperty(cheie, PROPS.getProperty(cheie));
    }
}
