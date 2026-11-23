package com.hapifyme.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Body pentru PUT /user/update_profile.php */
public class UpdateProfileRequest {

    @JsonProperty("user_id")
    private final int userId;

    @JsonProperty("first_name")
    private final String firstName;

    @JsonProperty("last_name")
    private final String lastName;

    private final String email;

    @JsonProperty("profile_pic")
    private final String profilePic;

    public UpdateProfileRequest(int userId, String firstName, String lastName, String email, String profilePic) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.profilePic = profilePic;
    }

    public int getUserId() { return userId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getProfilePic() { return profilePic; }
}
