package org.criticizer.repository;

import org.criticizer.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Genre entity.
 * Contains ONLY data access methods - NO business logic.
 */
@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {

    Optional<Genre> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Genre> findAllByOrderByNameAsc();

    /**
     * Find genres available for specific media type.
     * Includes 'shared' genres and media-specific genres.
     */
    @Query("SELECT DISTINCT g FROM Genre g " +
            "JOIN GenreApplicability ga ON g.id = ga.genreId " +
            "WHERE ga.mediaType = :mediaType OR ga.mediaType = 'shared' " +
            "ORDER BY g.name ASC")
    List<Genre> findAvailableGenresFor(@Param("mediaType") String mediaType);


    /**
     * Find genres for a movie using JPQL.
     */
    @Query("SELECT g FROM Movie m JOIN m.genres g WHERE m.id = :movieId ORDER BY g.name")
    List<Genre> findByMovieId(@Param("movieId") Integer movieId);

    /**
     * Find genres for a book using JPQL.
     */
    @Query("SELECT g FROM Book b JOIN b.genres g WHERE b.id = :bookId ORDER BY g.name")
    List<Genre> findByBookId(@Param("bookId") Integer bookId);

    /**
     * Find genres for a show using JPQL.
     */
    @Query("SELECT g FROM Show s JOIN s.genres g WHERE s.id = :showId ORDER BY g.name")
    List<Genre> findByShowId(@Param("showId") Integer showId);

    // ============= Usage Check Queries =============

    /**
     * Check if genre is used by any movies.
     * Returns count for efficiency.
     */
    @Query("SELECT COUNT(m) FROM Movie m JOIN m.genres g WHERE g.id = :genreId")
    long countMoviesWithGenre(@Param("genreId") Integer genreId);

    /**
     * Check if genre is used by any books.
     */
    @Query("SELECT COUNT(b) FROM Book b JOIN b.genres g WHERE g.id = :genreId")
    long countBooksWithGenre(@Param("genreId") Integer genreId);

    /**
     * Check if genre is used by any shows.
     */
    @Query("SELECT COUNT(s) FROM Show s JOIN s.genres g WHERE g.id = :genreId")
    long countShowsWithGenre(@Param("genreId") Integer genreId);

}