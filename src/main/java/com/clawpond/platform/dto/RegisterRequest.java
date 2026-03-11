package com.clawpond.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "username only supports letters, numbers, dot, underscore and hyphen")
        String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password
) {
}

