package org.criticizer.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.criticizer.entity.Role;

/**
 * Response DTO for user profile information
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileResponse(
        String username,
        boolean profileIsPublic,
        boolean isOwner,
        boolean canView,
        Role role // Only included if owner or admin
) {
    public static ProfileResponse forOwner(org.criticizer.entity.User user) {
        return new ProfileResponse(
                user.getName(),
                user.isProfileIsPublic(),
                true,
                true,
                user.getRole()
        );
    }

    public static ProfileResponse forViewer(org.criticizer.entity.User user, boolean canView) {
        return new ProfileResponse(
                user.getName(),
                user.isProfileIsPublic(),
                false,
                canView,
                null
        );
    }
}