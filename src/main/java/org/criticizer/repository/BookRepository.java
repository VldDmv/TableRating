package org.criticizer.repository;

import org.criticizer.entity.Book;
import org.criticizer.entity.Genre;
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
public interface BookRepository extends MediaRepository<Book> {

    @Query("SELECT b.id FROM Book b " +
            "LEFT JOIN b.genres g " +
            "WHERE b.userId = :userId " +
            "AND (:categoryId IS NULL OR g.id = :categoryId) " +
            "AND (:searchTerm IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:minScore IS NULL OR b.score >= :minScore) " +
            "AND (:maxScore IS NULL OR b.score <= :maxScore) " +
            "GROUP BY b.id")
    Page<Integer> findItemIds(
            @Param("userId") Integer userId,
            @Param("categoryId") Integer categoryId,
            @Param("searchTerm") String searchTerm,
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore,
            Pageable pageable
    );

    @Query("SELECT DISTINCT b FROM Book b " +
            "LEFT JOIN FETCH b.genres " +
            "WHERE b.id IN :ids")
    List<Book> findByIdsWithCategories(@Param("ids") List<Integer> ids);

    @EntityGraph(attributePaths = {"genres"})
    @Query("SELECT DISTINCT b FROM Book b WHERE b.userId = :userId")
    List<Book> findByUserIdWithGenres(@Param("userId") Integer userId);

    @EntityGraph(attributePaths = {"genres"})
    @Query("SELECT DISTINCT b FROM Book b WHERE LOWER(b.name) = LOWER(:name) AND b.userId = :userId")
    Optional<Book> findByNameAndUserIdWithGenres(
            @Param("name") String name,
            @Param("userId") Integer userId
    );

    List<Book> findAllByGenresContaining(Genre genre);

    /**
     * Remove all references to a genre from book_genres join table.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM book_genres WHERE genre_id = :genreId", nativeQuery = true)
    void removeGenreFromAll(@Param("genreId") Integer genreId);
}