package org.criticizer.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request DTO for user registration */
public record RegisterRequest(
        @NotBlank(message = "Username is required")
                @Size(max = 50, message = "Username must not exceed 50 characters")
                String username,
        @NotBlank(message = "Password is required")
                @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
                String password,
        @NotBlank(message = "Password confirmation is required") String confirmPassword) {
    /** Validates that password and confirmPassword match */
    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
