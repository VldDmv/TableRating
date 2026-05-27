package org.criticizer.repository;

import org.criticizer.entity.Game;
import org.criticizer.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends MediaRepository<Game> {

    @Query("SELECT g.id FROM Game g " +
            "LEFT JOIN g.tags t " +
            "WHERE g.userId = :userId " +
            "AND (:categoryId IS NULL OR t.id = :categoryId) " +
            "AND (:searchTerm IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:minScore IS NULL OR g.score >= :minScore) " +
            "AND (:maxScore IS NULL OR g.score <= :maxScore) " +
            "AND (:completed IS NULL OR g.completed = :completed) " +
            "GROUP BY g.id")
    Page<Integer> findItemIds(
            @Param("userId") Integer userId,
            @Param("categoryId") Integer categoryId,
            @Param("searchTerm") String searchTerm,
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore,
            @Param("completed") Boolean completed,
            Pageable pageable
    );

    @Query("SELECT DISTINCT g FROM Game g " +
            "LEFT JOIN FETCH g.tags " +
            "WHERE g.id IN :ids")
    List<Game> findByIdsWithCategories(@Param("ids") List<Integer> ids);

    @EntityGraph(attributePaths = {"tags"})
    @Query("SELECT DISTINCT g FROM Game g WHERE g.userId = :userId")
    List<Game> findByUserIdWithTags(@Param("userId") Integer userId);

    @EntityGraph(attributePaths = {"tags"})
    @Query("SELECT DISTINCT g FROM Game g WHERE LOWER(g.name) = LOWER(:name) AND g.userId = :userId")
    Optional<Game> findByNameAndUserIdWithTags(
            @Param("name") String name,
            @Param("userId") Integer userId
    );

    @Query("SELECT COUNT(g) > 0 FROM Game g JOIN g.tags t WHERE t.id = :tagId")
    boolean isTagInUse(@Param("tagId") Integer tagId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM game_tags WHERE tag_id = :tagId", nativeQuery = true)
    void removeTagFromAll(@Param("tagId") Integer tagId);
}