package org.criticizer.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

/** Base repository interface for all media entities. */
@NoRepositoryBean
public interface MediaRepository<T> extends JpaRepository<T, Integer> {

    List<T> findByUserId(Integer userId);

    Optional<T> findByNameIgnoreCaseAndUserId(String name, Integer userId);

    boolean existsByNameIgnoreCaseAndUserId(String name, Integer userId);

    void deleteByUserId(Integer userId);

    @Query("SELECT COUNT(e) FROM #{#entityName} e")
    long countTotal();

    /** Two-step pagination — must be implemented in each concrete repository. */
    Page<Integer> findItemIds(
            @Param("userId") Integer userId,
            @Param("categoryId") Integer categoryId,
            @Param("searchTerm") String searchTerm,
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore,
            Pageable pageable);

    List<T> findByIdsWithCategories(@Param("ids") List<Integer> ids);

    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.userId = :userId")
    long countByUserId(@Param("userId") Integer userId);

    /** Batch count per user — avoids N+1 on public users list. */
    @Query(
            "SELECT e.userId, COUNT(e) FROM #{#entityName} e WHERE e.userId IN :userIds GROUP BY"
                    + " e.userId")
    List<Object[]> countByUserIdsRaw(@Param("userIds") List<Integer> userIds);

    default Map<Integer, Long> countByUserIds(List<Integer> userIds) {
        return countByUserIdsRaw(userIds).stream()
                .collect(
                        Collectors.toMap(
                                row -> ((Number) row[0]).intValue(),
                                row -> ((Number) row[1]).longValue()));
    }

    // ── Kept for any callers outside DashboardService ─────────────────────────

    @Query("SELECT COALESCE(AVG(e.score), 0.0) FROM #{#entityName} e WHERE e.userId = :userId")
    Double getAverageScoreByUserId(@Param("userId") Integer userId);

    @Query("SELECT COALESCE(MAX(e.score), 0) FROM #{#entityName} e WHERE e.userId = :userId")
    Integer getMaxScoreByUserId(@Param("userId") Integer userId);

    @Query("SELECT COALESCE(MIN(e.score), 0) FROM #{#entityName} e WHERE e.userId = :userId")
    Integer getMinScoreByUserId(@Param("userId") Integer userId);

    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.userId = :userId AND e.completed = true")
    long countCompletedByUserId(@Param("userId") Integer userId);

    @Query(
            """
            SELECT
                COUNT(e),
                COALESCE(AVG(e.score), 0.0),
                COALESCE(MAX(e.score), 0),
                COALESCE(MIN(e.score), 0),
                SUM(CASE WHEN e.completed = true THEN 1L ELSE 0L END)
            FROM #{#entityName} e
            WHERE e.userId = :userId
            """)
    List<Object[]> getStatsByUserId(@Param("userId") Integer userId);
}
