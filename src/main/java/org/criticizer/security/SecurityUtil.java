package org.criticizer.security;

import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.exceptions.security.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class for retrieving the current authenticated user.
 * <p>
 * Reads the User entity directly from the AuthenticatedUser principal —
 * no database query is made. The entity was loaded once during authentication
 * by UserDetailsServiceImpl and cached in the Security context for the
 * duration of the request.
 */
@Component
public class SecurityUtil {

    /**
     * Returns the currently authenticated User entity.
     *
     * @throws UnauthorizedException if no user is authenticated
     */
    public User getCurrentUser() {
        AuthenticatedUser principal = getAuthenticatedPrincipal();
        if (principal == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        return principal.getUser();
    }

    /**
     * Returns the username of the current user, or null if not authenticated.
     */
    public String getCurrentUsername() {
        AuthenticatedUser principal = getAuthenticatedPrincipal();
        return principal != null ? principal.getUsername() : null;
    }

    public boolean isAuthenticated() {
        return getAuthenticatedPrincipal() != null;
    }

    public boolean isAdmin() {
        AuthenticatedUser principal = getAuthenticatedPrincipal();
        return principal != null && principal.getUser().getRole() == Role.ADMIN;
    }

    private AuthenticatedUser getAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }

        return null;
    }
}