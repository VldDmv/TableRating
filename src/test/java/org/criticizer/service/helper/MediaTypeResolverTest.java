package org.criticizer.service.helper;

import static org.assertj.core.api.Assertions.*;

import org.criticizer.constants.ContentCategory;
import org.criticizer.service.book.BookService;
import org.criticizer.service.game.GameService;
import org.criticizer.service.movie.MovieService;
import org.criticizer.service.show.ShowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for MediaTypeResolver. */
@ExtendWith(MockitoExtension.class)
@DisplayName("MediaTypeResolver Tests")
class MediaTypeResolverTest {

    @Mock private GameService gameService;

    @Mock private MovieService movieService;

    @Mock private BookService bookService;

    @Mock private ShowService showService;

    private MediaTypeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new MediaTypeResolver(gameService, movieService, bookService, showService);
    }

    // ==================== RESOLVE BY STRING TESTS ====================

    @Nested
    @DisplayName("resolve(String) Tests")
    class ResolveByStringTests {

        @Test
        @DisplayName("Should resolve 'games' to GameService")
        void shouldResolveGames() {
            // When
            AbstractMediaService<?, ?> result = resolver.resolve("games");

            // Then
            assertThat(result).isSameAs(gameService);
        }

        @Test
        @DisplayName("Should resolve 'movies' to MovieService")
        void shouldResolveMovies() {
            // When
            AbstractMediaService<?, ?> result = resolver.resolve("movies");

            // Then
            assertThat(result).isSameAs(movieService);
        }

        @Test
        @DisplayName("Should resolve 'books' to BookService")
        void shouldResolveBooks() {
            // When
            AbstractMediaService<?, ?> result = resolver.resolve("books");

            // Then
            assertThat(result).isSameAs(bookService);
        }

        @Test
        @DisplayName("Should resolve 'shows' to ShowService")
        void shouldResolveShows() {
            // When
            AbstractMediaService<?, ?> result = resolver.resolve("shows");

            // Then
            assertThat(result).isSameAs(showService);
        }

        @Test
        @DisplayName("Should be case-insensitive")
        void shouldBeCaseInsensitive() {
            // When & Then
            assertThat(resolver.resolve("GAMES")).isSameAs(gameService);
            assertThat(resolver.resolve("Games")).isSameAs(gameService);
            assertThat(resolver.resolve("gAmEs")).isSameAs(gameService);
            assertThat(resolver.resolve("MOVIES")).isSameAs(movieService);
            assertThat(resolver.resolve("BOOKS")).isSameAs(bookService);
            assertThat(resolver.resolve("SHOWS")).isSameAs(showService);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid type")
        void shouldThrowForInvalidType() {
            assertThatThrownBy(() -> resolver.resolve("invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid content category")
                    .hasMessageContaining("invalid");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for null")
        void shouldThrowForNull() {
            // When & Then
            assertThatThrownBy(() -> resolver.resolve((String) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }

        @Test
        @DisplayName("Should throw with helpful error message")
        void shouldThrowWithHelpfulMessage() {
            // When & Then
            assertThatThrownBy(() -> resolver.resolve("music"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("music")
                    .hasMessageContaining("Valid types: games, movies, books, shows");
        }

        @ParameterizedTest
        @ValueSource(strings = {"game", "movie", "book", "show"})
        @DisplayName("Should resolve singular forms successfully")
        void shouldResolveSingularForms(String input) {
            assertThatCode(() -> resolver.resolve(input)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"games", "GAMES", "Games", "gaMES"})
        @DisplayName("Should resolve games in any case")
        void shouldResolveGamesInAnyCase(String input) {
            assertThat(resolver.resolve(input)).isSameAs(gameService);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should throw for invalid variations")
        void shouldThrowForInvalidVariations(String input) {
            assertThatThrownBy(() -> resolver.resolve(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== RESOLVE BY ENUM TESTS ====================

    @Nested
    @DisplayName("resolve(MediaType) Tests")
    class ResolveByEnumTests {

        @Test
        @DisplayName("Should resolve GAMES enum")
        void shouldResolveGamesEnum() {
            // When
            AbstractMediaService<?, ?> result = resolver.resolve(ContentCategory.GAMES);

            // Then
            assertThat(result).isSameAs(gameService);
        }

        @Test
        @DisplayName("Should resolve MOVIES enum")
        void shouldResolveMoviesEnum() {
            // When
            AbstractMediaService<?, ?> result = resolver.resolve(ContentCategory.MOVIES);

            // Then
            assertThat(result).isSameAs(movieService);
        }

        @Test
        @DisplayName("Should resolve BOOKS enum")
        void shouldResolveBooksEnum() {
            // When
            AbstractMediaService<?, ?> result = resolver.resolve(ContentCategory.BOOKS);

            // Then
            assertThat(result).isSameAs(bookService);
        }

        @Test
        @DisplayName("Should resolve SHOWS enum")
        void shouldResolveShowsEnum() {
            // When
            AbstractMediaService<?, ?> result = resolver.resolve(ContentCategory.SHOWS);

            // Then
            assertThat(result).isSameAs(showService);
        }

        @Test
        @DisplayName("Should resolve all enum values")
        void shouldResolveAllEnumValues() {
            // When & Then - No exceptions should be thrown
            for (ContentCategory type : ContentCategory.values()) {
                assertThatCode(() -> resolver.resolve(type)).doesNotThrowAnyException();
            }
        }
    }

    // ==================== IS SUPPORTED TESTS ====================

    @Nested
    @DisplayName("isSupported() Tests")
    class IsSupportedTests {

        @Test
        @DisplayName("Should return true for valid media types")
        void shouldReturnTrueForValidTypes() {
            assertThat(resolver.isSupported("games")).isTrue();
            assertThat(resolver.isSupported("movies")).isTrue();
            assertThat(resolver.isSupported("books")).isTrue();
            assertThat(resolver.isSupported("shows")).isTrue();
        }

        @Test
        @DisplayName("Should return true for case variations")
        void shouldReturnTrueForCaseVariations() {
            assertThat(resolver.isSupported("GAMES")).isTrue();
            assertThat(resolver.isSupported("Games")).isTrue();
            assertThat(resolver.isSupported("gAmEs")).isTrue();
        }

        @Test
        @DisplayName("Should return false for invalid media types")
        void shouldReturnFalseForInvalidTypes() {
            assertThat(resolver.isSupported("invalid")).isFalse();
            assertThat(resolver.isSupported("music")).isFalse();
            assertThat(resolver.isSupported("podcasts")).isFalse();
        }

        @Test
        @DisplayName("Should return false for null")
        void shouldReturnFalseForNull() {
            assertThat(resolver.isSupported(null)).isFalse();
        }

        @Test
        @DisplayName("Should return false for empty string")
        void shouldReturnFalseForEmpty() {
            assertThat(resolver.isSupported("")).isFalse();
        }

        @Test
        @DisplayName("Should return false for whitespace")
        void shouldReturnFalseForWhitespace() {
            assertThat(resolver.isSupported("   ")).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"game", "movie", "book", "show"})
        @DisplayName("Should return true for singular forms")
        void shouldReturnTrueForSingular(String input) {
            assertThat(resolver.isSupported(input)).isTrue();
        }
    }

    // ==================== MEDIA TYPE ENUM TESTS ====================

    @Nested
    @DisplayName("MediaType Enum Tests")
    class MediaTypeEnumTests {

        @Test
        @DisplayName("Should have correct getPlural() for each enum")
        void shouldHaveCorrectValues() {
            assertThat(ContentCategory.GAMES.getPlural()).isEqualTo("games");
            assertThat(ContentCategory.MOVIES.getPlural()).isEqualTo("movies");
            assertThat(ContentCategory.BOOKS.getPlural()).isEqualTo("books");
            assertThat(ContentCategory.SHOWS.getPlural()).isEqualTo("shows");
        }

        @Test
        @DisplayName("Should parse string to enum correctly")
        void shouldParseStringToEnum() {
            assertThat(ContentCategory.fromString("games")).isEqualTo(ContentCategory.GAMES);
            assertThat(ContentCategory.fromString("movies")).isEqualTo(ContentCategory.MOVIES);
            assertThat(ContentCategory.fromString("books")).isEqualTo(ContentCategory.BOOKS);
            assertThat(ContentCategory.fromString("shows")).isEqualTo(ContentCategory.SHOWS);
        }

        @Test
        @DisplayName("Should be case-insensitive when parsing")
        void shouldBeCaseInsensitiveWhenParsing() {
            assertThat(ContentCategory.fromString("GAMES")).isEqualTo(ContentCategory.GAMES);
            assertThat(ContentCategory.fromString("Games")).isEqualTo(ContentCategory.GAMES);
            assertThat(ContentCategory.fromString("gAmEs")).isEqualTo(ContentCategory.GAMES);
        }

        @Test
        @DisplayName("Should throw when parsing invalid string")
        void shouldThrowWhenParsingInvalid() {
            assertThatThrownBy(() -> ContentCategory.fromString("invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid content category");
        }

        @Test
        @DisplayName("Should throw when parsing null")
        void shouldThrowWhenParsingNull() {
            assertThatThrownBy(() -> ContentCategory.fromString(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }

        @Test
        @DisplayName("Should have toString equal to value")
        void shouldHaveToStringEqualToValue() {
            for (ContentCategory type : ContentCategory.values()) {
                assertThat(type.toString()).isEqualTo(type.getPlural());
            }
        }

        @Test
        @DisplayName("Should have exactly 4 enum values")
        void shouldHaveExactlyFourValues() {
            assertThat(ContentCategory.values()).hasSize(4);
        }
    }

    // ==================== INTEGRATION TESTS ====================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should resolve same service for string and enum")
        void shouldResolveSameServiceForStringAndEnum() {
            // When
            AbstractMediaService<?, ?> fromString = resolver.resolve("games");
            AbstractMediaService<?, ?> fromEnum = resolver.resolve(ContentCategory.GAMES);

            // Then
            assertThat(fromString).isSameAs(fromEnum);
        }

        @Test
        @DisplayName("Should resolve different services for different types")
        void shouldResolveDifferentServices() {
            // When
            AbstractMediaService<?, ?> games = resolver.resolve("games");
            AbstractMediaService<?, ?> movies = resolver.resolve("movies");
            AbstractMediaService<?, ?> books = resolver.resolve("books");
            AbstractMediaService<?, ?> shows = resolver.resolve("shows");

            // Then - All different instances
            assertThat(games).isNotSameAs(movies);
            assertThat(games).isNotSameAs(books);
            assertThat(games).isNotSameAs(shows);
            assertThat(movies).isNotSameAs(books);
            assertThat(movies).isNotSameAs(shows);
            assertThat(books).isNotSameAs(shows);
        }

        @Test
        @DisplayName("Should resolve consistently across multiple calls")
        void shouldResolveConsistently() {
            // When
            AbstractMediaService<?, ?> first = resolver.resolve("games");
            AbstractMediaService<?, ?> second = resolver.resolve("games");
            AbstractMediaService<?, ?> third = resolver.resolve("GAMES");

            // Then - Always same instance
            assertThat(first).isSameAs(second);
            assertThat(second).isSameAs(third);
        }

        @Test
        @DisplayName("Should work with all supported media types")
        void shouldWorkWithAllSupportedTypes() {
            // Given
            String[] supportedTypes = {"games", "movies", "books", "shows"};

            // When & Then
            for (String type : supportedTypes) {
                assertThat(resolver.isSupported(type)).isTrue();
                assertThatCode(() -> resolver.resolve(type)).doesNotThrowAnyException();
            }
        }
    }

    // ==================== EDGE CASES ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle mixed case consistently")
        void shouldHandleMixedCase() {
            // When
            AbstractMediaService<?, ?> lower = resolver.resolve("games");
            AbstractMediaService<?, ?> upper = resolver.resolve("GAMES");
            AbstractMediaService<?, ?> mixed = resolver.resolve("GaMeS");

            // Then - All resolve to same service
            assertThat(lower).isSameAs(upper);
            assertThat(upper).isSameAs(mixed);
        }

        @Test
        @DisplayName("Should provide helpful error for common mistakes")
        void shouldProvideHelpfulErrors() {

            assertThatThrownBy(() -> resolver.resolve("gamez"))
                    .hasMessageContaining("Valid types: games, movies, books, shows");
        }

        @Test
        @DisplayName("Should not accept similar but wrong names")
        void shouldNotAcceptSimilarNames() {
            assertThat(resolver.isSupported("gaming")).isFalse();
            assertThat(resolver.isSupported("films")).isFalse();
        }
    }
}
