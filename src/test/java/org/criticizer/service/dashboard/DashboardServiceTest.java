package org.criticizer.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

import org.criticizer.dto.admin.AdminStats;
import org.criticizer.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for DashboardService. */
@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Tests")
class DashboardServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private GameRepository gameRepository;

    @Mock private MovieRepository movieRepository;

    @Mock private BookRepository bookRepository;

    @Mock private ShowRepository showRepository;

    @InjectMocks private DashboardService dashboardService;

    @Nested
    @DisplayName("getAdminDashboardStats() Tests")
    class GetAdminDashboardStatsTests {

        @Test
        @DisplayName("Should return admin statistics with all counts")
        void shouldReturnAdminStats() {
            // Given
            when(userRepository.count()).thenReturn(100L);
            when(gameRepository.countTotal()).thenReturn(500L);
            when(movieRepository.countTotal()).thenReturn(750L);
            when(bookRepository.countTotal()).thenReturn(300L);
            when(showRepository.countTotal()).thenReturn(400L);

            // When
            AdminStats result = dashboardService.getAdminDashboardStats();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalUsers()).isEqualTo(100L);
            assertThat(result.getTotalGames()).isEqualTo(500L);
            assertThat(result.getTotalMovies()).isEqualTo(750L);
            assertThat(result.getTotalBooks()).isEqualTo(300L);
            assertThat(result.getTotalShows()).isEqualTo(400L);

            // Verify all repositories were called
            verify(userRepository).count();
            verify(gameRepository).countTotal();
            verify(movieRepository).countTotal();
            verify(bookRepository).countTotal();
            verify(showRepository).countTotal();
        }

        @Test
        @DisplayName("Should handle zero counts")
        void shouldHandleZeroCounts() {
            // Given
            when(userRepository.count()).thenReturn(0L);
            when(gameRepository.countTotal()).thenReturn(0L);
            when(movieRepository.countTotal()).thenReturn(0L);
            when(bookRepository.countTotal()).thenReturn(0L);
            when(showRepository.countTotal()).thenReturn(0L);

            // When
            AdminStats result = dashboardService.getAdminDashboardStats();

            // Then
            assertThat(result.getTotalUsers()).isZero();
            assertThat(result.getTotalGames()).isZero();
            assertThat(result.getTotalMovies()).isZero();
            assertThat(result.getTotalBooks()).isZero();
            assertThat(result.getTotalShows()).isZero();
        }

        @Test
        @DisplayName("Should handle large counts")
        void shouldHandleLargeCounts() {
            // Given
            when(userRepository.count()).thenReturn(1_000_000L);
            when(gameRepository.countTotal()).thenReturn(5_000_000L);
            when(movieRepository.countTotal()).thenReturn(10_000_000L);
            when(bookRepository.countTotal()).thenReturn(3_000_000L);
            when(showRepository.countTotal()).thenReturn(2_000_000L);

            // When
            AdminStats result = dashboardService.getAdminDashboardStats();

            // Then
            assertThat(result.getTotalUsers()).isEqualTo(1_000_000L);
            assertThat(result.getTotalGames()).isEqualTo(5_000_000L);
            assertThat(result.getTotalMovies()).isEqualTo(10_000_000L);
            assertThat(result.getTotalBooks()).isEqualTo(3_000_000L);
            assertThat(result.getTotalShows()).isEqualTo(2_000_000L);
        }

        @Test
        @DisplayName("Should call each repository exactly once")
        void shouldCallRepositoriesOnce() {
            // Given
            when(userRepository.count()).thenReturn(10L);
            when(gameRepository.countTotal()).thenReturn(20L);
            when(movieRepository.countTotal()).thenReturn(30L);
            when(bookRepository.countTotal()).thenReturn(40L);
            when(showRepository.countTotal()).thenReturn(50L);

            // When
            dashboardService.getAdminDashboardStats();

            // Then
            verify(userRepository, times(1)).count();
            verify(gameRepository, times(1)).countTotal();
            verify(movieRepository, times(1)).countTotal();
            verify(bookRepository, times(1)).countTotal();
            verify(showRepository, times(1)).countTotal();
        }

        @Test
        @DisplayName("Should handle mixed counts")
        void shouldHandleMixedCounts() {
            // Given - Some categories with data, some without
            when(userRepository.count()).thenReturn(50L);
            when(gameRepository.countTotal()).thenReturn(0L);
            when(movieRepository.countTotal()).thenReturn(200L);
            when(bookRepository.countTotal()).thenReturn(0L);
            when(showRepository.countTotal()).thenReturn(100L);

            // When
            AdminStats result = dashboardService.getAdminDashboardStats();

            // Then
            assertThat(result.getTotalUsers()).isEqualTo(50L);
            assertThat(result.getTotalGames()).isZero();
            assertThat(result.getTotalMovies()).isEqualTo(200L);
            assertThat(result.getTotalBooks()).isZero();
            assertThat(result.getTotalShows()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should return new instance on each call")
        void shouldReturnNewInstanceEachTime() {
            // Given
            when(userRepository.count()).thenReturn(10L);
            when(gameRepository.countTotal()).thenReturn(20L);
            when(movieRepository.countTotal()).thenReturn(30L);
            when(bookRepository.countTotal()).thenReturn(40L);
            when(showRepository.countTotal()).thenReturn(50L);

            // When
            AdminStats result1 = dashboardService.getAdminDashboardStats();
            AdminStats result2 = dashboardService.getAdminDashboardStats();

            // Then - Different instances but same values
            assertThat(result1).isNotSameAs(result2);
            assertThat(result1.getTotalUsers()).isEqualTo(result2.getTotalUsers());
            assertThat(result1.getTotalGames()).isEqualTo(result2.getTotalGames());
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("Scenario: New system with no data")
        void scenarioNewSystem() {
            // Given
            when(userRepository.count()).thenReturn(0L);
            when(gameRepository.countTotal()).thenReturn(0L);
            when(movieRepository.countTotal()).thenReturn(0L);
            when(bookRepository.countTotal()).thenReturn(0L);
            when(showRepository.countTotal()).thenReturn(0L);

            // When
            AdminStats result = dashboardService.getAdminDashboardStats();

            // Then - All zeros
            assertThat(result.getTotalUsers()).isZero();
            assertThat(result.getTotalGames()).isZero();
            assertThat(result.getTotalMovies()).isZero();
            assertThat(result.getTotalBooks()).isZero();
            assertThat(result.getTotalShows()).isZero();
        }

        @Test
        @DisplayName("Scenario: Growing platform")
        void scenarioGrowingPlatform() {
            // Given - Active platform with users adding content
            when(userRepository.count()).thenReturn(1000L);
            when(gameRepository.countTotal()).thenReturn(5000L);
            when(movieRepository.countTotal()).thenReturn(8000L);
            when(bookRepository.countTotal()).thenReturn(3000L);
            when(showRepository.countTotal()).thenReturn(4000L);

            // When
            AdminStats result = dashboardService.getAdminDashboardStats();

            // Then - Reasonable distribution
            assertThat(result.getTotalUsers()).isEqualTo(1000L);

            // Total items: 20000
            long totalItems =
                    result.getTotalGames()
                            + result.getTotalMovies()
                            + result.getTotalBooks()
                            + result.getTotalShows();
            assertThat(totalItems).isEqualTo(20000L);

            // Average items per user: 20
            assertThat(totalItems / result.getTotalUsers()).isEqualTo(20L);
        }

        @Test
        @DisplayName("Scenario: Movies-heavy platform")
        void scenarioMoviesHeavyPlatform() {
            // Given - Platform where movies are most popular
            when(userRepository.count()).thenReturn(500L);
            when(gameRepository.countTotal()).thenReturn(1000L);
            when(movieRepository.countTotal()).thenReturn(10000L); // Most popular
            when(bookRepository.countTotal()).thenReturn(500L);
            when(showRepository.countTotal()).thenReturn(2000L);

            // When
            AdminStats result = dashboardService.getAdminDashboardStats();

            // Then - Movies dominate
            assertThat(result.getTotalMovies())
                    .isGreaterThan(result.getTotalGames())
                    .isGreaterThan(result.getTotalBooks())
                    .isGreaterThan(result.getTotalShows());
        }

        @Test
        @DisplayName("Scenario: Single user testing system")
        void scenarioSingleUserTesting() {
            // Given - Single user adding various content for testing
            when(userRepository.count()).thenReturn(1L);
            when(gameRepository.countTotal()).thenReturn(10L);
            when(movieRepository.countTotal()).thenReturn(15L);
            when(bookRepository.countTotal()).thenReturn(5L);
            when(showRepository.countTotal()).thenReturn(8L);

            // When
            AdminStats result = dashboardService.getAdminDashboardStats();

            // Then - One user with test data
            assertThat(result.getTotalUsers()).isEqualTo(1L);

            long totalItems =
                    result.getTotalGames()
                            + result.getTotalMovies()
                            + result.getTotalBooks()
                            + result.getTotalShows();
            assertThat(totalItems).isEqualTo(38L);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle maximum long values")
        void shouldHandleMaxLongValues() {
            // Given
            when(userRepository.count()).thenReturn(Long.MAX_VALUE);
            when(gameRepository.countTotal()).thenReturn(Long.MAX_VALUE);
            when(movieRepository.countTotal()).thenReturn(Long.MAX_VALUE);
            when(bookRepository.countTotal()).thenReturn(Long.MAX_VALUE);
            when(showRepository.countTotal()).thenReturn(Long.MAX_VALUE);

            // When & Then - Should not throw
            assertThatCode(() -> dashboardService.getAdminDashboardStats())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle users but no content")
        void shouldHandleUsersWithoutContent() {
            // Given - Users registered but haven't added content yet
            when(userRepository.count()).thenReturn(100L);
            when(gameRepository.countTotal()).thenReturn(0L);
            when(movieRepository.countTotal()).thenReturn(0L);
            when(bookRepository.countTotal()).thenReturn(0L);
            when(showRepository.countTotal()).thenReturn(0L);

            // When
            AdminStats result = dashboardService.getAdminDashboardStats();

            // Then
            assertThat(result.getTotalUsers()).isEqualTo(100L);
            long totalItems =
                    result.getTotalGames()
                            + result.getTotalMovies()
                            + result.getTotalBooks()
                            + result.getTotalShows();
            assertThat(totalItems).isZero();
        }

        @Test
        @DisplayName("Should work with read-only transaction")
        void shouldWorkWithReadOnlyTransaction() {
            // Given
            when(userRepository.count()).thenReturn(10L);
            when(gameRepository.countTotal()).thenReturn(20L);
            when(movieRepository.countTotal()).thenReturn(30L);
            when(bookRepository.countTotal()).thenReturn(40L);
            when(showRepository.countTotal()).thenReturn(50L);

            // When - Should work fine with read-only transaction
            AdminStats result = dashboardService.getAdminDashboardStats();

            // Then - No modifications attempted, just reads
            assertThat(result).isNotNull();
            verifyNoMoreInteractions(
                    userRepository,
                    gameRepository,
                    movieRepository,
                    bookRepository,
                    showRepository);
        }
    }
}
