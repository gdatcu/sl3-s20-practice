package com.hapifyme.api.models;

/** Body pentru POST /user/login.php. Atentie: API-ul clasic accepta DOAR username-ul (nu email-ul). */
public class LoginRequest {
    private final String username;
    private final String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
