package ro.qatester.api.v1.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Body pentru POST /auth/login (v1). log_email accepta email SAU username. */
public class LoginRequestV1 {

    @JsonProperty("log_email")
    private final String logEmail;

    @JsonProperty("log_password")
    private final String logPassword;

    public LoginRequestV1(String logEmail, String logPassword) {
        this.logEmail = logEmail;
        this.logPassword = logPassword;
    }

    public String getLogEmail() { return logEmail; }
    public String getLogPassword() { return logPassword; }
}
