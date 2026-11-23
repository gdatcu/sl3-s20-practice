package com.hapifyme.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Body pentru POST /user/register.php */
public class UserRequest {

    @JsonProperty("first_name")
    private final String firstName;

    @JsonProperty("last_name")
    private final String lastName;

    private final String email;
    private final String password;

    public UserRequest(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
