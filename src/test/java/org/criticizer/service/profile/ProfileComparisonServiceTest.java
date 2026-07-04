package org.criticizer.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.criticizer.entity.Book;
import org.criticizer.entity.Game;
import org.criticizer.entity.Movie;
import org.criticizer.entity.Show;
import org.criticizer.entity.User;
import org.criticizer.repository.BookRepository;
import org.criticizer.repository.GameRepository;
import org.criticizer.repository.MovieRepository;
import org.criticizer.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for ProfileComparisonService: matching, score diffs and compatibility. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProfileComparisonService Tests")
class ProfileComparisonServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private MovieRepository movieRepository;
    @Mock private BookRepository bookRepository;
    @Mock private ShowRepository showRepository;

    private ProfileComparisonService service;

    private User me;
    private User other;

    @BeforeEach
    void setUp() {
        service =
                new ProfileComparisonService(
                        gameRepository, movieRepository, bookRepository, showRepository);

        me = new User("alice", "password");
        me.setId(1);
        other = new User("bob", "password");
        other.setId(2);

        // Default: empty lists everywhere; individual tests override.
        when(gameRepository.findByUserIdWithTags(anyInt())).thenReturn(List.of());
        when(movieRepository.findByUserIdWithGenres(anyInt())).thenReturn(List.of());
        when(bookRepository.findByUserIdWithGenres(anyInt())).thenReturn(List.of());
        when(showRepository.findByUserIdWithGenres(anyInt())).thenReturn(List.of());
    }

    private static Game game(int id, String name, int userId, int score) {
        return new Game(id, name, userId, score, true);
    }

    @Test
    @DisplayName("Should match common items case-insensitively and compute diffs")
    void shouldMatchCommonItems() {
        when(gameRepository.findByUserIdWithTags(1))
                .thenReturn(
                        List.of(
                                game(1, "Celeste", 1, 90),
                                game(2, "Hades", 1, 80),
                                game(3, "Only Mine", 1, 50)));
        when(gameRepository.findByUserIdWithTags(2))
                .thenReturn(
                        List.of(
                                game(4, "CELESTE", 2, 70),
                                game(5, "hades", 2, 85),
                                game(6, "Only Theirs", 2, 60)));

        Map<String, Object> result = service.compare(me, other);

        assertThat(result.get("me")).isEqualTo("alice");
        assertThat(result.get("other")).isEqualTo("bob");
        assertThat(result.get("commonCount")).isEqualTo(2);

        @SuppressWarnings("unchecked")
        Map<String, Object> games =
                ((Map<String, Map<String, Object>>) result.get("categories")).get("games");

        assertThat(games.get("myCount")).isEqualTo(3);
        assertThat(games.get("theirCount")).isEqualTo(3);
        assertThat(games.get("commonCount")).isEqualTo(2);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) games.get("items");

        // Sorted by absolute diff descending: Celeste (|20|) before Hades (|-5|)
        assertThat(items).hasSize(2);
        assertThat(items.get(0).get("name")).isEqualTo("Celeste");
        assertThat(items.get(0).get("myScore")).isEqualTo(90);
        assertThat(items.get(0).get("theirScore")).isEqualTo(70);
        assertThat(items.get(0).get("diff")).isEqualTo(20);
        assertThat(items.get(1).get("name")).isEqualTo("Hades");
        assertThat(items.get(1).get("diff")).isEqualTo(-5);
    }

    @Test
    @DisplayName("Should compute per-category and overall compatibility")
    void shouldComputeCompatibility() {
        when(gameRepository.findByUserIdWithTags(1)).thenReturn(List.of(game(1, "Celeste", 1, 90)));
        when(gameRepository.findByUserIdWithTags(2)).thenReturn(List.of(game(2, "Celeste", 2, 70)));
        when(movieRepository.findByUserIdWithGenres(1))
                .thenReturn(List.of(new Movie(1, "Heat", 1, 95, true)));
        when(movieRepository.findByUserIdWithGenres(2))
                .thenReturn(List.of(new Movie(2, "Heat", 2, 85, true)));

        Map<String, Object> result = service.compare(me, other);

        // Two common items, diffs 20 and 10 -> overall 100 - 15 = 85.0
        assertThat(result.get("commonCount")).isEqualTo(2);
        assertThat(result.get("compatibility")).isEqualTo(85.0);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> categories =
                (Map<String, Map<String, Object>>) result.get("categories");
        assertThat(categories.get("games").get("compatibility")).isEqualTo(80.0);
        assertThat(categories.get("games").get("avgDiff")).isEqualTo(20.0);
        assertThat(categories.get("movies").get("compatibility")).isEqualTo(90.0);
    }

    @Test
    @DisplayName("Should return null compatibility when nothing is in common")
    void shouldHandleNoOverlap() {
        when(bookRepository.findByUserIdWithGenres(1))
                .thenReturn(List.of(new Book(1, "Dune", 1, 88, true)));
        when(bookRepository.findByUserIdWithGenres(2))
                .thenReturn(List.of(new Book(2, "1984", 2, 92, true)));

        Map<String, Object> result = service.compare(me, other);

        assertThat(result.get("commonCount")).isEqualTo(0);
        assertThat(result.get("compatibility")).isNull();

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> categories =
                (Map<String, Map<String, Object>>) result.get("categories");
        assertThat(categories.get("books").get("myCount")).isEqualTo(1);
        assertThat(categories.get("books").get("theirCount")).isEqualTo(1);
        assertThat(categories.get("books").get("commonCount")).isEqualTo(0);
        assertThat(categories.get("books").get("compatibility")).isNull();
        assertThat(categories.get("books").get("avgDiff")).isNull();
    }

    @Test
    @DisplayName("Should include all four categories in the result")
    void shouldIncludeAllCategories() {
        when(showRepository.findByUserIdWithGenres(1))
                .thenReturn(List.of(new Show(1, "The Wire", 1, 97, true)));
        when(showRepository.findByUserIdWithGenres(2))
                .thenReturn(List.of(new Show(2, "the wire", 2, 97, true)));

        Map<String, Object> result = service.compare(me, other);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> categories =
                (Map<String, Map<String, Object>>) result.get("categories");
        assertThat(categories.keySet()).containsExactly("games", "movies", "books", "shows");

        // Identical scores -> perfect compatibility
        assertThat(categories.get("shows").get("compatibility")).isEqualTo(100.0);
        assertThat(result.get("compatibility")).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should trim and lower-case names before matching")
    void shouldNormalizeNames() {
        when(gameRepository.findByUserIdWithTags(1))
                .thenReturn(List.of(game(1, "  Hollow Knight ", 1, 90)));
        when(gameRepository.findByUserIdWithTags(2))
                .thenReturn(List.of(game(2, "hollow knight", 2, 92)));

        Map<String, Object> result = service.compare(me, other);

        assertThat(result.get("commonCount")).isEqualTo(1);
    }
}
