package org.criticizer.service.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.dto.movie.MovieResponse;
import org.criticizer.entity.Genre;
import org.criticizer.entity.MediaStatus;
import org.criticizer.entity.Movie;
import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.repository.GenreRepository;
import org.criticizer.repository.MovieRepository;
import org.criticizer.service.helper.ServiceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** Unit tests for MovieService. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MovieService Tests")
class MovieServiceTest {

    @Mock private MovieRepository movieRepository;

    @Mock private GenreRepository genreRepository;

    @Mock private ServiceValidator validator;

    @InjectMocks private MovieService movieService;

    private Movie testMovie;
    private Genre testGenre;
    private final int TEST_USER_ID = 1;

    @BeforeEach
    void setUp() {
        testMovie = new Movie(1, "Inception", TEST_USER_ID, 90, MediaStatus.PLANNED);
        testMovie.setCoverUrl("http://example.com/inception.jpg");

        testGenre = new Genre(1, "Sci-Fi");
        testMovie.setGenres(new HashSet<>(Arrays.asList(testGenre)));
    }

    // ==================== ADD ITEM TESTS ====================

    @Nested
    @DisplayName("addItem() Tests")
    class AddItemTests {

        @Test
        @DisplayName("Should successfully add movie with genres")
        void shouldAddMovieWithGenres() {
            // Given
            String name = "The Matrix";
            String coverUrl = "http://example.com/matrix.jpg";
            int score = 95;
            List<Integer> genreIds = Arrays.asList(1, 2);

            Genre genre1 = new Genre(1, "Sci-Fi");
            Genre genre2 = new Genre(2, "Action");

            when(validator.validateName(name, "Movie name")).thenReturn(name);
            doNothing().when(validator).validateScore(score, TEST_USER_ID, "Movie");
            when(movieRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(false);

            when(movieRepository.save(any(Movie.class)))
                    .thenAnswer(
                            invocation -> {
                                Movie movie = invocation.getArgument(0);
                                if (movie.getId() == null) {
                                    Movie savedMovie =
                                            new Movie(
                                                    100,
                                                    movie.getName(),
                                                    movie.getUserId(),
                                                    movie.getScore(),
                                                    movie.getStatus());
                                    savedMovie.setCoverUrl(movie.getCoverUrl());
                                    savedMovie.setGenres(movie.getGenres());
                                    return savedMovie;
                                }
                                return movie;
                            });

            when(genreRepository.findAllById(genreIds)).thenReturn(Arrays.asList(genre1, genre2));

            // When
            movieService.addItem(name, coverUrl, TEST_USER_ID, score, genreIds);

            // Then
            verify(validator).validateName(name, "Movie name");
            verify(validator).validateScore(score, TEST_USER_ID, "Movie");
            verify(genreRepository).findAllById(genreIds);
            verify(movieRepository, times(2)).save(any(Movie.class));
        }

        @Test
        @DisplayName("Should add movie without genres")
        void shouldAddMovieWithoutGenres() {
            // Given
            String name = "Pulp Fiction";
            when(validator.validateName(name, "Movie name")).thenReturn(name);
            when(movieRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(false);
            when(movieRepository.save(any(Movie.class))).thenAnswer(i -> i.getArgument(0));

            // When
            movieService.addItem(name, null, TEST_USER_ID, 88, null);

            // Then
            verify(movieRepository)
                    .save(
                            argThat(
                                    movie ->
                                            movie.getName().equals(name)
                                                    && movie.getCoverUrl() == null));
            verify(genreRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("Should throw when movie already exists")
        void shouldThrowWhenMovieExists() {
            // Given
            String name = "Inception";
            when(validator.validateName(name, "Movie name")).thenReturn(name);
            when(movieRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> movieService.addItem(name, null, TEST_USER_ID, 90, null))
                    .isInstanceOf(ItemAlreadyExistsException.class)
                    .hasMessageContaining(name);

            verify(movieRepository, never()).save(any());
        }
    }

    // ==================== UPDATE ITEM TESTS ====================

    @Nested
    @DisplayName("updateItem() Tests")
    class UpdateItemTests {

        @Test
        @DisplayName("Should successfully update movie")
        void shouldUpdateMovie() {
            // Given
            String oldName = "Old Name";
            String newName = "New Name";
            String newCoverUrl = "http://example.com/new.jpg";
            int newScore = 92;
            List<Integer> newGenreIds = Arrays.asList(2);

            Genre newGenre = new Genre(2, "Drama");

            when(validator.validateName(oldName, "Old Movie name")).thenReturn(oldName);
            when(validator.validateName(newName, "New Movie name")).thenReturn(newName);
            doNothing().when(validator).validateScore(newScore, TEST_USER_ID, "Movie");

            when(movieRepository.findByNameIgnoreCaseAndUserId(oldName, TEST_USER_ID))
                    .thenReturn(Optional.of(testMovie));
            when(movieRepository.existsByNameIgnoreCaseAndUserId(newName, TEST_USER_ID))
                    .thenReturn(false);
            when(genreRepository.findAllById(newGenreIds)).thenReturn(List.of(newGenre));
            when(movieRepository.save(any(Movie.class))).thenAnswer(i -> i.getArgument(0));

            // When
            movieService.updateItem(
                    oldName, newName, newCoverUrl, newScore, TEST_USER_ID, newGenreIds);

            // Then
            verify(movieRepository)
                    .save(
                            argThat(
                                    movie ->
                                            movie.getName().equals(newName)
                                                    && movie.getCoverUrl().equals(newCoverUrl)
                                                    && movie.getScore() == newScore
                                                    && movie.getGenres().contains(newGenre)));
        }

        @Test
        @DisplayName("Should throw when movie not found")
        void shouldThrowWhenMovieNotFound() {
            // Given
            String oldName = "Nonexistent";
            String newName = "New Name";

            when(validator.validateName(oldName, "Old Movie name")).thenReturn(oldName);
            when(validator.validateName(newName, "New Movie name")).thenReturn(newName);
            when(movieRepository.findByNameIgnoreCaseAndUserId(oldName, TEST_USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(
                            () ->
                                    movieService.updateItem(
                                            oldName, newName, null, 80, TEST_USER_ID, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(oldName);
        }
    }

    // ==================== REMOVE ITEM TESTS ====================

    @Nested
    @DisplayName("removeItem() Tests")
    class RemoveItemTests {

        @Test
        @DisplayName("Should successfully remove movie")
        void shouldRemoveMovie() {
            // Given
            String name = "Inception";
            when(validator.validateName(name, "Movie name")).thenReturn(name);
            when(movieRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testMovie));
            doNothing().when(movieRepository).delete(testMovie);

            // When
            movieService.removeItem(name, TEST_USER_ID);

            // Then
            verify(movieRepository).delete(testMovie);
        }

        @Test
        @DisplayName("Should throw when movie not found")
        void shouldThrowWhenMovieNotFound() {
            // Given
            String name = "Nonexistent Movie";
            when(validator.validateName(name, "Movie name")).thenReturn(name);
            when(movieRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> movieService.removeItem(name, TEST_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(movieRepository, never()).delete(any());
        }
    }

    // ==================== TOGGLE STATUS TESTS ====================

    @Nested
    @DisplayName("toggleStatus() Tests")
    class ToggleStatusTests {

        @Test
        @DisplayName("Should advance PLANNED to IN_PROGRESS")
        void shouldAdvancePlannedToInProgress() {
            // Given
            testMovie.setStatus(MediaStatus.PLANNED);
            String name = "Inception";

            when(validator.validateName(name, "Movie name")).thenReturn(name);
            when(movieRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testMovie));
            when(movieRepository.save(any(Movie.class))).thenAnswer(i -> i.getArgument(0));

            // When
            MovieResponse result = movieService.toggleStatus(name, TEST_USER_ID);

            // Then
            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            verify(movieRepository)
                    .save(argThat(movie -> movie.getStatus() == MediaStatus.IN_PROGRESS));
        }

        @Test
        @DisplayName("Should advance COMPLETED to DROPPED")
        void shouldAdvanceCompletedToDropped() {
            // Given
            testMovie.setStatus(MediaStatus.COMPLETED);
            String name = "Inception";

            when(validator.validateName(name, "Movie name")).thenReturn(name);
            when(movieRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testMovie));
            when(movieRepository.save(any(Movie.class))).thenAnswer(i -> i.getArgument(0));

            // When
            MovieResponse result = movieService.toggleStatus(name, TEST_USER_ID);

            // Then
            assertThat(result.getStatus()).isEqualTo("DROPPED");
            verify(movieRepository)
                    .save(argThat(movie -> movie.getStatus() == MediaStatus.DROPPED));
        }
    }

    // ==================== GET USER ITEMS PAGE TESTS ====================

    @Nested
    @DisplayName("getUserItemsPage() Tests")
    class GetUserItemsPageTests {

        @Test
        @DisplayName("Should return paginated movies")
        void shouldReturnPaginatedMovies() {
            // Given
            ServiceValidator.PaginationParams params =
                    new ServiceValidator.PaginationParams(1, 10, 0);

            when(validator.validatePagination(1, 10)).thenReturn(params);
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            Page<Integer> movieIds = new PageImpl<>(Arrays.asList(1, 2), PageRequest.of(0, 10), 2);
            when(movieRepository.findItemIds(
                            eq(TEST_USER_ID), isNull(), isNull(), any(), any(), any()))
                    .thenReturn(movieIds);

            Movie movie1 = new Movie(1, "Movie 1", TEST_USER_ID, 80, MediaStatus.PLANNED);
            Movie movie2 = new Movie(2, "Movie 2", TEST_USER_ID, 85, MediaStatus.PLANNED);
            when(movieRepository.findByIdsWithCategories(Arrays.asList(1, 2)))
                    .thenReturn(Arrays.asList(movie1, movie2));

            // When
            var result =
                    movieService.getUserItemsPage(TEST_USER_ID, 1, 10, null, null, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getCurrentPage()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should filter by genre")
        void shouldFilterByGenre() {
            // Given
            ServiceValidator.PaginationParams params =
                    new ServiceValidator.PaginationParams(1, 10, 0);

            when(validator.validatePagination(1, 10)).thenReturn(params);
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            Page<Integer> movieIds = new PageImpl<>(List.of(1), PageRequest.of(0, 10), 1);
            when(movieRepository.findItemIds(
                            eq(TEST_USER_ID), eq(1), isNull(), any(), any(), any()))
                    .thenReturn(movieIds);

            when(movieRepository.findByIdsWithCategories(List.of(1)))
                    .thenReturn(List.of(testMovie));

            // When
            var result = movieService.getUserItemsPage(TEST_USER_ID, 1, 10, 1, null, "name", "asc");

            // Then
            assertThat(result.getItems()).hasSize(1);
            verify(movieRepository)
                    .findItemIds(eq(TEST_USER_ID), eq(1), isNull(), any(), any(), any());
        }
    }

    // ==================== UPDATE COVER TESTS ====================

    @Nested
    @DisplayName("updateCover() Tests")
    class UpdateCoverTests {

        @Test
        @DisplayName("Should update cover URL")
        void shouldUpdateCoverUrl() {
            // Given
            String name = "Inception";
            String newCoverUrl = "http://example.com/new-cover.jpg";

            when(validator.validateName(name, "Movie name")).thenReturn(name);
            when(movieRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testMovie));
            when(movieRepository.save(any(Movie.class))).thenAnswer(i -> i.getArgument(0));

            // When
            movieService.updateCover(name, newCoverUrl, TEST_USER_ID);

            // Then
            verify(movieRepository).save(argThat(movie -> movie.getCoverUrl().equals(newCoverUrl)));
        }

        @Test
        @DisplayName("Should remove cover when URL is null")
        void shouldRemoveCoverWhenNull() {
            // Given
            String name = "Inception";

            when(validator.validateName(name, "Movie name")).thenReturn(name);
            when(movieRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testMovie));
            when(movieRepository.save(any(Movie.class))).thenAnswer(i -> i.getArgument(0));

            // When
            movieService.updateCover(name, null, TEST_USER_ID);

            // Then
            verify(movieRepository).save(argThat(movie -> movie.getCoverUrl() == null));
        }
    }

    // ==================== RESPONSE MAPPING TESTS ====================

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should map movie with genres to response")
        void shouldMapMovieWithGenres() {
            // Given
            Genre genre1 = new Genre(1, "Sci-Fi");
            Genre genre2 = new Genre(2, "Action");
            testMovie.setGenres(new HashSet<>(Arrays.asList(genre1, genre2)));

            String name = "Inception";
            when(validator.validateName(name, "Movie name")).thenReturn(name);
            when(movieRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testMovie));
            when(movieRepository.save(any(Movie.class))).thenAnswer(i -> i.getArgument(0));

            // When
            MovieResponse result = movieService.toggleStatus(name, TEST_USER_ID);

            // Then
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getName()).isEqualTo("Inception");
            assertThat(result.getGenres()).hasSize(2);
            assertThat(result.getGenres())
                    .extracting(GenreResponse::getName)
                    .containsExactlyInAnyOrder("Sci-Fi", "Action");
        }

        @Test
        @DisplayName("Should map movie without genres to response")
        void shouldMapMovieWithoutGenres() {
            // Given
            testMovie.setGenres(new HashSet<>());
            String name = "Inception";

            when(validator.validateName(name, "Movie name")).thenReturn(name);
            when(movieRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testMovie));
            when(movieRepository.save(any(Movie.class))).thenAnswer(i -> i.getArgument(0));

            // When
            MovieResponse result = movieService.toggleStatus(name, TEST_USER_ID);

            // Then
            assertThat(result.getGenres()).isEmpty();
        }

        @Test
        @DisplayName("Should include all movie properties in response")
        void shouldIncludeAllProperties() {
            // Given
            testMovie.setStatus(MediaStatus.COMPLETED);
            String name = "Inception";

            when(validator.validateName(name, "Movie name")).thenReturn(name);
            when(movieRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testMovie));
            when(movieRepository.save(any(Movie.class))).thenAnswer(i -> i.getArgument(0));

            // When
            MovieResponse result = movieService.toggleStatus(name, TEST_USER_ID);

            // Then
            assertThat(result.getId()).isEqualTo(testMovie.getId());
            assertThat(result.getName()).isEqualTo(testMovie.getName());
            assertThat(result.getCoverUrl()).isEqualTo(testMovie.getCoverUrl());
            assertThat(result.getScore()).isEqualTo(testMovie.getScore());
            assertThat(result.getStatus()).isEqualTo("DROPPED");
        }
    }

    // ==================== UTILITY METHODS TESTS ====================

    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {

        @Test
        @DisplayName("Should check if movie exists")
        void shouldCheckIfMovieExists() {
            // Given
            String name = "Inception";
            when(validator.validateName(name, "Movie name")).thenReturn(name);
            when(movieRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(true);

            // When
            boolean result = movieService.isItemExists(name, TEST_USER_ID);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should get movie status")
        void shouldGetMovieStatus() {
            // Given
            String name = "Inception";
            testMovie.setStatus(MediaStatus.COMPLETED);

            when(validator.validateName(name, "Movie name")).thenReturn(name);
            when(movieRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testMovie));

            // When
            MediaStatus result = movieService.getItemStatus(name, TEST_USER_ID);

            // Then
            assertThat(result).isEqualTo(MediaStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should return correct media type")
        void shouldReturnCorrectMediaType() {
            // When
            String mediaType = movieService.getMediaType();

            // Then
            assertThat(mediaType).isEqualTo("movies");
        }
    }
}
