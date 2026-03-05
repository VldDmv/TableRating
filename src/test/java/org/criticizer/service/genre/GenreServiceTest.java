package org.criticizer.service.genre;

import org.criticizer.dto.genre.CreateGenreRequest;
import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.dto.genre.UpdateGenreRequest;
import org.criticizer.entity.Genre;
import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.exceptions.validation.EmptyNameException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for GenreService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GenreService Tests")
class GenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private GenreApplicabilityRepository applicabilityRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ShowRepository showRepository;

    @InjectMocks
    private GenreService genreService;

    private Genre testGenre;

    @BeforeEach
    void setUp() {
        testGenre = new Genre(1, "Action");
    }

    // ==================== GET ALL GENRES TESTS ====================

    @Nested
    @DisplayName("getAllGenres() Tests")
    class GetAllGenresTests {

        @Test
        @DisplayName("Should return all genres with media types")
        void shouldReturnAllGenres() {
            // Given
            Genre genre1 = new Genre(1, "Action");
            Genre genre2 = new Genre(2, "Drama");

            when(genreRepository.findAllByOrderByNameAsc()).thenReturn(Arrays.asList(genre1, genre2));

            List<Object[]> batchData = Arrays.asList(
                    new Object[]{1, "movie"},
                    new Object[]{1, "show"},
                    new Object[]{2, "book"}
            );
            when(applicabilityRepository.findMediaTypesByGenreIds(anyList())).thenReturn(batchData);

            // When
            List<GenreResponse> result = genreService.getAllGenres();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.stream().filter(r -> r.getId() == 1).findFirst().get().getMediaTypes())
                    .containsExactlyInAnyOrder("movie", "show");
        }

        @Test
        @DisplayName("Should return empty list when no genres exist")
        void shouldReturnEmptyListWhenNoGenres() {
            // Given
            when(genreRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

            // When
            List<GenreResponse> result = genreService.getAllGenres();

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ==================== GET AVAILABLE GENRES FOR TESTS ====================

    @Nested
    @DisplayName("getAvailableGenresFor() Tests")
    class GetAvailableGenresForTests {

        @Test
        @DisplayName("Should return genres available for movie")
        void shouldReturnGenresForMovie() {
            // Given
            when(genreRepository.findAvailableGenresFor("movie")).thenReturn(List.of(testGenre));


            when(applicabilityRepository.findMediaTypesByGenreIds(anyList()))
                    .thenReturn(List.of(new Object[]{1, "movie"}, new Object[]{1, "shared"}));

            // When
            List<GenreResponse> result = genreService.getAvailableGenresFor("movie");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Action");
        }

        @Test
        @DisplayName("Should throw InvalidInputException for invalid media type")
        void shouldThrowForInvalidMediaType() {
            // When & Then
            assertThatThrownBy(() -> genreService.getAvailableGenresFor("invalid"))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("mediaType");
        }

        @Test
        @DisplayName("Should accept all valid media types")
        void shouldAcceptValidMediaTypes() {
            // Given

            when(genreRepository.findAvailableGenresFor(anyString()))
                    .thenReturn(List.of(testGenre));
            when(applicabilityRepository.findMediaTypesByGenreIds(any()))
                    .thenReturn(Collections.singletonList(new Object[]{1, "movie"}));

            // When & Then
            assertThatCode(() -> {
                genreService.getAvailableGenresFor("movie");
                genreService.getAvailableGenresFor("book");
                genreService.getAvailableGenresFor("show");
                genreService.getAvailableGenresFor("shared");
            }).doesNotThrowAnyException();
        }
    }
    // ==================== CREATE GENRE TESTS ====================

    @Nested
    @DisplayName("createGenre() Tests")
    class CreateGenreTests {

        @Test
        @DisplayName("Should successfully create genre with media types")
        void shouldCreateGenre() {
            // Given
            CreateGenreRequest request = new CreateGenreRequest("Sci-Fi", Arrays.asList("movie", "book"));
            Genre savedGenre = new Genre(1, "Sci-Fi");

            when(genreRepository.existsByNameIgnoreCase("Sci-Fi")).thenReturn(false);
            when(genreRepository.save(any(Genre.class))).thenReturn(savedGenre);

            when(applicabilityRepository.findMediaTypesByGenreId(1))
                    .thenReturn(Arrays.asList("movie", "book"));

            // When
            GenreResponse result = genreService.createGenre(request);

            // Then
            assertThat(result.getName()).isEqualTo("Sci-Fi");
            verify(genreRepository).save(any(Genre.class));

            verify(applicabilityRepository).insertApplicability(1, "movie");
            verify(applicabilityRepository).insertApplicability(1, "book");
        }
        @Test
        @DisplayName("Should default to 'shared' when no media types provided")
        void shouldDefaultToShared() {
            // Given
            CreateGenreRequest request = new CreateGenreRequest("Comedy", null);
            Genre savedGenre = new Genre(1, "Comedy");

            when(genreRepository.existsByNameIgnoreCase("Comedy")).thenReturn(false);
            when(genreRepository.save(any(Genre.class))).thenReturn(savedGenre);
            when(applicabilityRepository.findMediaTypesByGenreId(1))
                    .thenReturn(List.of("shared"));

            // When
            GenreResponse result = genreService.createGenre(request);

            // Then
            assertThat(result.getMediaTypes()).containsExactly("shared");
            verify(applicabilityRepository).insertApplicability(1, "shared");
        }

        @Test
        @DisplayName("Should throw when genre name is empty")
        void shouldThrowWhenNameIsEmpty() {
            // Given
            CreateGenreRequest request = new CreateGenreRequest("  ", List.of("movie"));

            // When & Then
            assertThatThrownBy(() -> genreService.createGenre(request))
                    .isInstanceOf(EmptyNameException.class);

            verify(genreRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when genre already exists")
        void shouldThrowWhenGenreExists() {
            // Given
            CreateGenreRequest request = new CreateGenreRequest("Action", List.of("movie"));
            when(genreRepository.existsByNameIgnoreCase("Action")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> genreService.createGenre(request))
                    .isInstanceOf(ItemAlreadyExistsException.class)
                    .hasMessageContaining("Action");

            verify(genreRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw for invalid media type")
        void shouldThrowForInvalidMediaType() {
            // Given
            CreateGenreRequest request = new CreateGenreRequest(
                    "Horror",
                    Arrays.asList("movie", "invalid")
            );

            // When & Then
            assertThatThrownBy(() -> genreService.createGenre(request))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("mediaType");
        }

        @Test
        @DisplayName("Should trim and deduplicate media types")
        void shouldTrimAndDeduplicateMediaTypes() {
            // Given
            CreateGenreRequest request = new CreateGenreRequest(
                    "Thriller",
                    Arrays.asList("MOVIE", "movie", "Movie", "book")
            );

            Genre savedGenre = new Genre(1, "Thriller");

            when(genreRepository.existsByNameIgnoreCase("Thriller")).thenReturn(false);
            when(genreRepository.save(any(Genre.class))).thenReturn(savedGenre);
            when(applicabilityRepository.findMediaTypesByGenreId(1))
                    .thenReturn(Arrays.asList("movie", "book"));

            // When
            genreService.createGenre(request);

            // Then
            // Verify that each unique type was inserted exactly once
            verify(applicabilityRepository).insertApplicability(1, "movie");
            verify(applicabilityRepository).insertApplicability(1, "book");
            verify(applicabilityRepository, times(2)).insertApplicability(eq(1), anyString());
        }
    }

    // ==================== UPDATE GENRE TESTS ====================

    @Nested
    @DisplayName("updateGenre() Tests")
    class UpdateGenreTests {

        @Test
        @DisplayName("Should successfully update genre name and media types")
        void shouldUpdateGenre() {
            // Given
            List<String> newMediaTypes = Arrays.asList("movie", "show", "book");
            UpdateGenreRequest request = new UpdateGenreRequest(
                    1,
                    "Updated Action",
                    newMediaTypes
            );

            when(genreRepository.findById(1)).thenReturn(Optional.of(testGenre));
            when(genreRepository.findByNameIgnoreCase("Updated Action")).thenReturn(Optional.empty());
            when(genreRepository.save(any(Genre.class))).thenReturn(new Genre(1, "Updated Action"));

            // Stubbing for the final response assembly
            when(applicabilityRepository.findMediaTypesByGenreId(1)).thenReturn(newMediaTypes);

            // When
            GenreResponse result = genreService.updateGenre(request);

            // Then
            assertThat(result.getName()).isEqualTo("Updated Action");
            assertThat(result.getMediaTypes()).containsExactlyInAnyOrder("movie", "show", "book");

            verify(genreRepository).save(argThat(genre ->
                    genre.getName().equals("Updated Action")
            ));

            verify(applicabilityRepository).deleteByGenreId(1);
            // Verifying that each new type was inserted
            for (String type : newMediaTypes) {
                verify(applicabilityRepository).insertApplicability(1, type);
            }
        }

        @Test
        @DisplayName("Should throw when genre not found")
        void shouldThrowWhenGenreNotFound() {
            // Given
            UpdateGenreRequest request = new UpdateGenreRequest(999, "New Name", List.of("movie"));
            when(genreRepository.findById(999)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> genreService.updateGenre(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Should throw when new name conflicts with existing genre")
        void shouldThrowWhenNameConflicts() {
            // Given
            Genre existingGenre = new Genre(2, "Drama");
            UpdateGenreRequest request = new UpdateGenreRequest(1, "Drama", List.of("movie"));

            when(genreRepository.findById(1)).thenReturn(Optional.of(testGenre));
            when(genreRepository.findByNameIgnoreCase("Drama"))
                    .thenReturn(Optional.of(existingGenre));

            // When & Then
            assertThatThrownBy(() -> genreService.updateGenre(request))
                    .isInstanceOf(ItemAlreadyExistsException.class)
                    .hasMessageContaining("Drama");
        }

        @Test
        @DisplayName("Should allow updating same genre name (case change)")
        void shouldAllowCaseChange() {
            // Given
            UpdateGenreRequest request = new UpdateGenreRequest(1, "ACTION", List.of("movie"));

            when(genreRepository.findById(1)).thenReturn(Optional.of(testGenre));
            when(genreRepository.findByNameIgnoreCase("ACTION"))
                    .thenReturn(Optional.of(testGenre));  // Same ID
            when(genreRepository.save(any(Genre.class))).thenReturn(testGenre);
            when(applicabilityRepository.findMediaTypesByGenreId(1))
                    .thenReturn(List.of("movie"));

            // When & Then
            assertThatCode(() -> genreService.updateGenre(request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should cascade remove genre from items when media type removed during update")
        void shouldCascadeRemoveFromItems() {
            // Given
            UpdateGenreRequest request = new UpdateGenreRequest(
                    1,
                    "Action",
                    List.of("movie") // "book" and "show" are being removed
            );

            when(genreRepository.findById(1)).thenReturn(Optional.of(testGenre));
            when(genreRepository.findByNameIgnoreCase("Action")).thenReturn(Optional.empty());
            when(genreRepository.save(any(Genre.class))).thenReturn(testGenre);

            when(applicabilityRepository.findMediaTypesByGenreId(1))
                    .thenReturn(Arrays.asList("movie", "book", "show"))
                    .thenReturn(List.of("movie"));

            // When
            genreService.updateGenre(request);

            // Then
            // verify cleanup for removed types
            verify(bookRepository).removeGenreFromAll(1);
            verify(showRepository).removeGenreFromAll(1);

            // verify that 'movie' was kept
            verify(movieRepository, never()).removeGenreFromAll(1);
        }
        }

    // ==================== DELETE GENRE TESTS ====================

    @Nested
    @DisplayName("deleteGenre() Tests")
    class DeleteGenreTests {

        @Test
        @DisplayName("Should successfully delete genre and cascade remove from all items")
        void shouldDeleteGenreWithCascade() {
            // Given
            when(genreRepository.findById(1)).thenReturn(Optional.of(testGenre));

            // When
            genreService.deleteGenre(1);

            // Then
            verify(movieRepository).removeGenreFromAll(1);
            verify(bookRepository).removeGenreFromAll(1);
            verify(showRepository).removeGenreFromAll(1);
            verify(applicabilityRepository).deleteByGenreId(1);
            verify(genreRepository).deleteById(1);
        }

        @Test
        @DisplayName("Should throw when genre not found")
        void shouldThrowWhenGenreNotFound() {
            // Given
            when(genreRepository.findById(999)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> genreService.deleteGenre(999))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");

            verify(genreRepository, never()).deleteById(any());
        }
    }

    // ==================== IS GENRE IN USE TESTS ====================

    @Nested
    @DisplayName("isGenreInUse() Tests")
    class IsGenreInUseTests {

        @Test
        @DisplayName("Should return true when genre is used in movies")
        void shouldReturnTrueForMovies() {
            // Given
            when(genreRepository.countMoviesWithGenre(1)).thenReturn(5L);

            // When
            boolean result = genreService.isGenreInUse(1);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when genre is used in books")
        void shouldReturnTrueForBooks() {
            // Given
            when(genreRepository.countMoviesWithGenre(1)).thenReturn(0L);
            when(genreRepository.countBooksWithGenre(1)).thenReturn(3L);

            // When
            boolean result = genreService.isGenreInUse(1);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when genre is not used")
        void shouldReturnFalseWhenNotUsed() {
            // Given
            when(genreRepository.countMoviesWithGenre(1)).thenReturn(0L);
            when(genreRepository.countBooksWithGenre(1)).thenReturn(0L);
            when(genreRepository.countShowsWithGenre(1)).thenReturn(0L);

            // When
            boolean result = genreService.isGenreInUse(1);

            // Then
            assertThat(result).isFalse();
        }
    }

    // ==================== GET GENRES FOR SPECIFIC MEDIA TESTS ====================

    @Nested
    @DisplayName("getGenresFor[Media]() Tests")
    class GetGenresForMediaTests {

        @Test
        @DisplayName("Should return genres for movie")
        void shouldReturnGenresForMovie() {
            // Given
            when(genreRepository.findByMovieId(1))
                    .thenReturn(Arrays.asList(testGenre));

            // When
            List<GenreResponse> result = genreService.getGenresForMovie(1);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Action");
        }

        @Test
        @DisplayName("Should return genres for book")
        void shouldReturnGenresForBook() {
            // Given
            when(genreRepository.findByBookId(1))
                    .thenReturn(Arrays.asList(testGenre));

            // When
            List<GenreResponse> result = genreService.getGenresForBook(1);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Action");
        }

        @Test
        @DisplayName("Should return genres for show")
        void shouldReturnGenresForShow() {
            // Given
            when(genreRepository.findByShowId(1))
                    .thenReturn(Arrays.asList(testGenre));

            // When
            List<GenreResponse> result = genreService.getGenresForShow(1);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Action");
        }

        @Test
        @DisplayName("Should return empty list when no genres assigned")
        void shouldReturnEmptyListWhenNoGenres() {
            // Given
            when(genreRepository.findByMovieId(1)).thenReturn(List.of());

            // When
            List<GenreResponse> result = genreService.getGenresForMovie(1);

            // Then
            assertThat(result).isEmpty();
        }
    }
}