package ro.qatester.api.v1.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Continutul cheii "data" la register / login / refresh (v1). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthData {
    private String token;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private long expiresIn;

    private UserDto user;

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public UserDto getUser() { return user; }
}
