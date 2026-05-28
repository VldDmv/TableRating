package org.criticizer.service.profile;

import org.criticizer.entity.User;
import org.springframework.stereotype.Service;

/** Service for centralized profile access control logic. */
@Service
public class ProfileAccessService {

    /**
     * Check if user can view a profile. Profile is viewable if it's public OR current user is the
     * owner.
     *
     * @param profileOwner The user whose profile is being accessed
     * @param currentUsername Username of the user trying to access (null if not authenticated)
     * @return true if profile can be viewed
     */
    public boolean canViewProfile(User profileOwner, String currentUsername) {
        if (profileOwner.isProfileIsPublic()) {
            return true;
        }

        return currentUsername != null && currentUsername.equalsIgnoreCase(profileOwner.getName());
    }

    /**
     * Check access and return comprehensive context.
     *
     * @param profileOwner The user whose profile is being accessed
     * @param currentUsername Username of the user trying to access
     * @return ProfileAccessContext with all access information
     */
    public ProfileAccessContext checkAccess(User profileOwner, String currentUsername) {
        boolean isOwner =
                currentUsername != null && currentUsername.equalsIgnoreCase(profileOwner.getName());
        boolean canView = profileOwner.isProfileIsPublic() || isOwner;

        return new ProfileAccessContext(isOwner, canView, profileOwner);
    }

    /**
     * Immutable context object containing profile access information.
     *
     * @param isOwner True if current user owns the profile
     * @param canView True if current user can view the profile
     * @param profileOwner The user whose profile is being accessed
     */
    public record ProfileAccessContext(boolean isOwner, boolean canView, User profileOwner) {
        /** Throws exception if profile cannot be viewed. */
        public void requireViewAccess() {
            if (!canView) {
                throw new org.criticizer.exceptions.security.InsufficientPermissionsException(
                        "VIEW_PRIVATE_PROFILE");
            }
        }
    }
}
