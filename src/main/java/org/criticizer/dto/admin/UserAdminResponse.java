package org.criticizer.dto.admin;

import org.criticizer.entity.Role;
import org.criticizer.entity.User;

/**
 * Response DTO for a user in the admin panel.
 */
public class UserAdminResponse {

    private final int id;
    private final String name;
    private final Role role;
    private final boolean profileIsPublic;

    public UserAdminResponse(int id, String name, Role role, boolean profileIsPublic) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.profileIsPublic = profileIsPublic;
    }

    public static UserAdminResponse from(User user) {
        return new UserAdminResponse(
                user.getId(),
                user.getName(),
                user.getRole(),
                user.isProfileIsPublic()
        );
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public boolean isProfileIsPublic() { return profileIsPublic; }
}