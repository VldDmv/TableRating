package org.criticizer.repository;

import org.criticizer.entity.GenreApplicability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface GenreApplicabilityRepository extends
        JpaRepository<GenreApplicability, GenreApplicability.GenreApplicabilityId> {

    List<GenreApplicability> findByGenreId(Integer genreId);

    @Query("SELECT ga.mediaType FROM GenreApplicability ga WHERE ga.genreId = :genreId")
    List<String> findMediaTypesByGenreId(@Param("genreId") Integer genreId);

    @Query("SELECT ga.genreId, ga.mediaType FROM GenreApplicability ga WHERE ga.genreId IN :genreIds")
    List<Object[]> findMediaTypesByGenreIds(@Param("genreIds") List<Integer> genreIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM GenreApplicability ga WHERE ga.genreId = :genreId")
    void deleteByGenreId(@Param("genreId") Integer genreId);

    boolean existsByGenreIdAndMediaType(Integer genreId, String mediaType);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO genre_applicability (genre_id, media_type) VALUES (:genreId, :mediaType)",
            nativeQuery = true)
    void insertApplicability(@Param("genreId") Integer genreId, @Param("mediaType") String mediaType);
}