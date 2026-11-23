package com.hapifyme.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Raspunsul la GET /user/get_profile.php?user_id=... */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileResponse {
    private String status;
    private String message;
    private Profile user;

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Profile getUser() { return user; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        private String id;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        private String username;
        private String email;

        @JsonProperty("profile_pic")
        private String profilePic;

        public String getId() { return id; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getProfilePic() { return profilePic; }
    }
}
