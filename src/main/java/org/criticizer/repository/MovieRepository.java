package org.criticizer.repository;

import org.criticizer.entity.Genre;
import org.criticizer.entity.Movie;
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
public interface MovieRepository extends MediaRepository<Movie> {

    @Query("SELECT m.id FROM Movie m " +
            "LEFT JOIN m.genres g " +
            "WHERE m.userId = :userId " +
            "AND (:categoryId IS NULL OR g.id = :categoryId) " +
            "AND (:searchTerm IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:minScore IS NULL OR m.score >= :minScore) " +
            "AND (:maxScore IS NULL OR m.score <= :maxScore) " +
            "GROUP BY m.id")
    Page<Integer> findItemIds(
            @Param("userId") Integer userId,
            @Param("categoryId") Integer categoryId,
            @Param("searchTerm") String searchTerm,
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore,
            Pageable pageable
    );

    @Query("SELECT DISTINCT m FROM Movie m " +
            "LEFT JOIN FETCH m.genres " +
            "WHERE m.id IN :ids")
    List<Movie> findByIdsWithCategories(@Param("ids") List<Integer> ids);

    @EntityGraph(attributePaths = {"genres"})
    @Query("SELECT DISTINCT m FROM Movie m WHERE m.userId = :userId")
    List<Movie> findByUserIdWithGenres(@Param("userId") Integer userId);

    @EntityGraph(attributePaths = {"genres"})
    @Query("SELECT DISTINCT m FROM Movie m WHERE LOWER(m.name) = LOWER(:name) AND m.userId = :userId")
    Optional<Movie> findByNameAndUserIdWithGenres(
            @Param("name") String name,
            @Param("userId") Integer userId
    );

    List<Movie> findAllByGenresContaining(Genre genre);

    /**
     * Remove all references to a genre from movie_genres join table.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM movie_genres WHERE genre_id = :genreId", nativeQuery = true)
    void removeGenreFromAll(@Param("genreId") Integer genreId);
}