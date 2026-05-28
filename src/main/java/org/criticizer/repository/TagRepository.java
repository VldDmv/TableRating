package org.criticizer.repository;

import java.util.List;
import java.util.Optional;
import org.criticizer.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for Tag entity. Replaces the old TagDao with automatic CRUD
 * implementation.
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {

    Optional<Tag> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Tag> findAllByOrderByNameAsc();

    @Query("SELECT t FROM Game g " + "JOIN g.tags t " + "WHERE g.id = :gameId " + "ORDER BY t.name")
    List<Tag> findByGameId(@Param("gameId") Integer gameId);

    @Query("SELECT COUNT(g) > 0 FROM Game g " + "JOIN g.tags t " + "WHERE t.id = :tagId")
    boolean isTagInUse(@Param("tagId") Integer tagId);
}
