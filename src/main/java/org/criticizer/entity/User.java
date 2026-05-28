package org.criticizer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @JsonIgnore
    @Column(name = "password", nullable = false, length = 128)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, columnDefinition = "VARCHAR(20)")
    private Role role;

    @Column(name = "profile_is_public", nullable = false)
    private boolean profileIsPublic;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public User() {}

    public User(Integer id, String name, String password, Role role, boolean profileIsPublic) {
        if (name == null || password == null || role == null) {
            throw new IllegalArgumentException("User arguments cannot be null");
        }
        this.id = id;
        this.name = name;
        this.password = password;
        this.role = role;
        this.profileIsPublic = profileIsPublic;
    }

    public User(String name, String password) {
        if (name == null || password == null) {
            throw new IllegalArgumentException("User arguments cannot be null");
        }
        this.name = name;
        this.password = password;
        this.role = Role.USER;
        this.profileIsPublic = false;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isProfileIsPublic() {
        return profileIsPublic;
    }

    public void setProfileIsPublic(boolean profileIsPublic) {
        this.profileIsPublic = profileIsPublic;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    /**
     * Equality based on database ID. Consistent with how Tag and Genre implement equals/hashCode.
     * Handles null ID (transient entities not yet persisted).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{id="
                + id
                + ", name='"
                + name
                + "', password='<PROTECTED>', role="
                + role
                + ", createdAt="
                + createdAt
                + "}";
    }
}
