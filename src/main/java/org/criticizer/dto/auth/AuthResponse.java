package org.criticizer.dto.auth;

import org.criticizer.entity.Role;

/** Response DTO for successful authentication Contains user information after login/register */
public record AuthResponse(String username, Role role, boolean profileIsPublic, String message) {
    public static AuthResponse success(
            String username, Role role, boolean profileIsPublic, String message) {
        return new AuthResponse(username, role, profileIsPublic, message);
    }
}
