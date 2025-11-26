package org.criticizer.entity;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Represents a user in the system with attributes for identification, authentication, and role-based access.
 */
public class User {
    private static final String DUMMY_HASH =
            BCrypt.hashpw("dummy_password_12345", BCrypt.gensalt());
    private final String name;
    private final String password;
    private final Role role;
    private int id;
    private boolean profileIsPublic;


    //Constructs a new User with all attributes

    public User(int id, String name, String password, Role role, boolean profileIsPublic) {
        if (name == null || password == null || role == null) {
            throw new IllegalArgumentException("User arguments cannot be null");
        }
        this.id = id;
        this.name = name;
        this.password = password;
        this.role = role;
        this.profileIsPublic = profileIsPublic;

    }

    //Constructs a new User with default role USER and private profile.
    public User(String name, String password) {
        if (name == null || password == null) {
            throw new IllegalArgumentException("User arguments cannot be null");
        }
        this.name = name;
        this.password = password;
        this.role = Role.USER;
        this.profileIsPublic = false;
    }


    public String getName() {
        return name;
    }

    public boolean checkPassword(String txtPassword) {
        String hashToCheck = !this.password.isEmpty()
                ? this.password
                : DUMMY_HASH;

        String passwordToCheck = (txtPassword != null)
                ? txtPassword
                : "";

        try {
            boolean matches = BCrypt.checkpw(passwordToCheck, hashToCheck);
            return matches && !this.password.isEmpty();

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String getPasswordHashInternal() {
        return password;
    }

    public int getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public boolean isProfileIsPublic() {
        return profileIsPublic;
    }

    public void setProfileIsPublic(boolean profileIsPublic) {
        this.profileIsPublic = profileIsPublic;
    }

    @Override
    public String toString() {
        return "User{name='" + name + "', password='<PROTECTED>'" + ", role=" + role + "}";
    }
}
