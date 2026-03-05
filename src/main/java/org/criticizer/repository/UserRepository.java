package org.criticizer.repository;

import org.criticizer.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for User entity.
 * Replaces the old UserDao with automatic CRUD implementation.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Find user by username (case-insensitive).
     * Used for login and checking duplicates.
     */
    Optional<User> findByNameIgnoreCase(String name);

    /**
     * Check if user exists by username (case-insensitive).
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Find users with pagination.
     * If publicOnly is true, only returns users with public profiles.
     *
     * @param publicOnly - filter by profile visibility
     * @param pageable   - pagination parameters
     */
    @Query("SELECT u FROM User u WHERE :publicOnly = false OR u.profileIsPublic = true")
    Page<User> findUsers(@Param("publicOnly") boolean publicOnly, Pageable pageable);

    /**
     * Search users by username with pagination.
     *
     * @param searchTerm - username search term (supports partial match)
     * @param publicOnly - filter by profile visibility
     * @param pageable   - pagination parameters
     */
    @Query("SELECT u FROM User u " +
            "WHERE (:searchTerm IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:publicOnly = false OR u.profileIsPublic = true)")
    Page<User> searchUsers(
            @Param("searchTerm") String searchTerm,
            @Param("publicOnly") boolean publicOnly,
            Pageable pageable
    );

    /**
     * Count users matching search criteria.
     */
    @Query("SELECT COUNT(u) FROM User u " +
            "WHERE (:searchTerm IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:publicOnly = false OR u.profileIsPublic = true)")
    long countUsers(
            @Param("searchTerm") String searchTerm,
            @Param("publicOnly") boolean publicOnly
    );
}