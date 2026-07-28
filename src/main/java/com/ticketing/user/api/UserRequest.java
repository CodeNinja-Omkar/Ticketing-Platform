package com.ticketing.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(
        @NotBlank(message = "fullname is required")
        String fullname,

        @NotBlank(message = "email is required")
        @Email(message = "Email must be valid")
        String email
) {

}
