package org.criticizer.service.dashboard;

import org.criticizer.dto.admin.AdminStats;
import org.criticizer.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service for dashboard statistics.
 *
 * Performance improvement in getUserDashboardStats():
 * Before: 5 queries per media type × 4 types = 20 queries per dashboard load.
 * After:  1 query  per media type × 4 types =  4 queries per dashboard load.
 *
 * Uses MediaRepository.getStatsByUserId() which returns Object[5]:
 * [count, avgScore, maxScore, minScore, completedCount]
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final MovieRepository movieRepository;
    private final BookRepository bookRepository;
    private final ShowRepository showRepository;

    public DashboardService(UserRepository userRepository,
                            GameRepository gameRepository,
                            MovieRepository movieRepository,
                            BookRepository bookRepository,
                            ShowRepository showRepository) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.movieRepository = movieRepository;
        this.bookRepository = bookRepository;
        this.showRepository = showRepository;
    }

    /**
     * Returns aggregate counts for the admin dashboard.
     * 5 queries — already optimal.
     */
    public AdminStats getAdminDashboardStats() {
        log.debug("Fetching admin dashboard statistics");

        long totalUsers  = userRepository.count();
        long totalGames  = gameRepository.countTotal();
        long totalMovies = movieRepository.countTotal();
        long totalBooks  = bookRepository.countTotal();
        long totalShows  = showRepository.countTotal();

        log.debug("Admin stats: {} users, {} games, {} movies, {} books, {} shows",
                totalUsers, totalGames, totalMovies, totalBooks, totalShows);

        return AdminStats.of(totalUsers, totalGames, totalMovies, totalBooks, totalShows);
    }

    /**
     * Returns per-media-type stats for a user's dashboard.
     * 4 queries total (one aggregation per media type).
     */
    public UserDashboardStats getUserDashboardStats(Integer userId) {
        log.debug("Fetching dashboard stats for user {}", userId);

        return new UserDashboardStats(
                buildMediaStats(gameRepository, userId),
                buildMediaStats(movieRepository, userId),
                buildMediaStats(bookRepository, userId),
                buildMediaStats(showRepository, userId)
        );
    }

    /**
     * Single aggregation query → Map with all stats.
     * Object[0] = COUNT, [1] = AVG, [2] = MAX, [3] = MIN, [4] = SUM(completed)
     */
    private Map<String, Object> buildMediaStats(MediaRepository<?> repository, Integer userId) {
        Object[] row = repository.getStatsByUserId(userId).get(0);

        long   count     = ((Number) row[0]).longValue();
        double avg       = ((Number) row[1]).doubleValue();
        int    max       = ((Number) row[2]).intValue();
        int    min       = ((Number) row[3]).intValue();
        long   completed = row[4] != null ? ((Number) row[4]).longValue() : 0L;

        return Map.of(
                "count",          count,
                "avgScore",       Math.round(avg * 10.0) / 10.0,
                "maxScore",       max,
                "minScore",       min,
                "completedCount", completed,
                "completionRate", count > 0 ? Math.round(completed * 100.0 / count) : 0
        );
    }

    public record UserDashboardStats(
            Map<String, Object> gamesStats,
            Map<String, Object> moviesStats,
            Map<String, Object> booksStats,
            Map<String, Object> showsStats
    ) {}
}