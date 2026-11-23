package com.hapifyme.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {
    private String status;
    private String message;
    private String token;
    private UserDetails user;

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
    public UserDetails getUser() { return user; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserDetails {
        private String id;
        private String username;
        private String email;

        public String getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
    }
}
