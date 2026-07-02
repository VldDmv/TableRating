package org.criticizer.repository;

import org.criticizer.entity.Genre;
import org.criticizer.entity.Show;
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
public interface ShowRepository extends MediaRepository<Show> {

    @Query("SELECT s.id FROM Show s " +
            "LEFT JOIN s.genres g " +
            "WHERE s.userId = :userId " +
            "AND (:categoryId IS NULL OR g.id = :categoryId) " +
            "AND (:searchTerm IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:minScore IS NULL OR s.score >= :minScore) " +
            "AND (:maxScore IS NULL OR s.score <= :maxScore) " +
            "GROUP BY s.id")
    Page<Integer> findItemIds(
            @Param("userId") Integer userId,
            @Param("categoryId") Integer categoryId,
            @Param("searchTerm") String searchTerm,
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore,
            Pageable pageable
    );

    @Query("SELECT DISTINCT s FROM Show s " +
            "LEFT JOIN FETCH s.genres " +
            "WHERE s.id IN :ids")
    List<Show> findByIdsWithCategories(@Param("ids") List<Integer> ids);

    @EntityGraph(attributePaths = {"genres"})
    @Query("SELECT DISTINCT s FROM Show s WHERE s.userId = :userId")
    List<Show> findByUserIdWithGenres(@Param("userId") Integer userId);

    @EntityGraph(attributePaths = {"genres"})
    @Query("SELECT DISTINCT s FROM Show s WHERE LOWER(s.name) = LOWER(:name) AND s.userId = :userId")
    Optional<Show> findByNameAndUserIdWithGenres(
            @Param("name") String name,
            @Param("userId") Integer userId
    );

    List<Show> findAllByGenresContaining(Genre genre);

    /**
     * Remove all references to a genre from show_genres join table.
     * Single DELETE instead of N separate UPDATE queries.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM show_genres WHERE genre_id = :genreId", nativeQuery = true)
    void removeGenreFromAll(@Param("genreId") Integer genreId);
}