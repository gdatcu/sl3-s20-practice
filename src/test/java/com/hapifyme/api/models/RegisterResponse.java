package com.hapifyme.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Raspunsul real (23.11.2026) la POST /user/register.php - status 201 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterResponse {
    private String status;
    private String message;

    @JsonProperty("api_key")
    private String apiKey;

    @JsonProperty("confirmation_token")
    private String confirmationToken;

    private String username;

    @JsonProperty("user_id")
    private String userId;          // serverul trimite "157" (String)

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getApiKey() { return apiKey; }
    public String getConfirmationToken() { return confirmationToken; }
    public String getUsername() { return username; }
    public String getUserId() { return userId; }
}
