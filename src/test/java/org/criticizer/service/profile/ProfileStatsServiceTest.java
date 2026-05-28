package org.criticizer.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.criticizer.entity.Game;
import org.criticizer.entity.Tag;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.repository.BookRepository;
import org.criticizer.repository.GameRepository;
import org.criticizer.repository.MovieRepository;
import org.criticizer.repository.ShowRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileStatsService Tests")
class ProfileStatsServiceTest {

    private static final int USER_ID = 5;

    @Mock private GameRepository gameRepository;
    @Mock private MovieRepository movieRepository;
    @Mock private BookRepository bookRepository;
    @Mock private ShowRepository showRepository;

    @InjectMocks private ProfileStatsService statsService;

    private Game game(int id, String name, int score, boolean completed, Tag... tags) {
        Game g = new Game(id, name, USER_ID, score, completed);
        g.setTags(new HashSet<>(List.of(tags)));
        return g;
    }

    @Test
    @DisplayName("Aggregates count, average and completion split")
    void aggregatesBasics() {
        when(gameRepository.findByUserIdWithTags(USER_ID))
                .thenReturn(
                        List.of(
                                game(1, "A", 10, true),
                                game(2, "B", 20, false),
                                game(3, "C", 90, true)));

        Map<String, Object> stats = statsService.getStats("games", USER_ID);

        assertThat(stats.get("count")).isEqualTo(3);
        assertThat((double) stats.get("avgScore")).isEqualTo(40.0); // (10+20+90)/3
        assertThat(stats.get("completed")).isEqualTo(2L);
        assertThat(stats.get("notCompleted")).isEqualTo(1L);
    }

    @Test
    @DisplayName("Score buckets place items in 10-point bins, edges included")
    void scoreBuckets() {
        when(gameRepository.findByUserIdWithTags(USER_ID))
                .thenReturn(
                        List.of(
                                game(1, "min", 1, false), // bucket 0  (1-10)
                                game(2, "ten", 10, false), // bucket 0  (1-10)
                                game(3, "eleven", 11, false), // bucket 1  (11-20)
                                game(4, "max", 100, false) // bucket 9  (91-100)
                                ));

        int[] buckets = (int[]) statsService.getStats("games", USER_ID).get("scoreBuckets");

        assertThat(buckets).hasSize(10);
        assertThat(buckets[0]).isEqualTo(2);
        assertThat(buckets[1]).isEqualTo(1);
        assertThat(buckets[9]).isEqualTo(1);
    }

    @Test
    @DisplayName("Category averages are grouped by tag and sorted by average desc")
    @SuppressWarnings("unchecked")
    void categoryAverages() {
        Tag rpg = new Tag(1, "RPG");
        Tag casual = new Tag(2, "Casual");
        when(gameRepository.findByUserIdWithTags(USER_ID))
                .thenReturn(
                        List.of(
                                game(1, "A", 90, true, rpg),
                                game(2, "B", 80, true, rpg),
                                game(3, "C", 20, false, casual)));

        var rows =
                (List<Map<String, Object>>)
                        statsService.getStats("games", USER_ID).get("categoryAverages");

        assertThat(rows).hasSize(2);
        // RPG avg 85 should come before Casual avg 20.
        assertThat(rows.get(0).get("name")).isEqualTo("RPG");
        assertThat((double) rows.get(0).get("avg")).isEqualTo(85.0);
        assertThat(rows.get(0).get("count")).isEqualTo(2L);
        assertThat(rows.get(1).get("name")).isEqualTo("Casual");
    }

    @Test
    @DisplayName("Empty collection returns zeroed stats")
    void emptyStats() {
        when(showRepository.findByUserIdWithGenres(USER_ID)).thenReturn(List.of());

        Map<String, Object> stats = statsService.getStats("shows", USER_ID);

        assertThat(stats.get("count")).isEqualTo(0);
        assertThat((double) stats.get("avgScore")).isEqualTo(0.0);
        assertThat(stats.get("completed")).isEqualTo(0L);
        assertThat((int[]) stats.get("scoreBuckets")).containsOnly(0);
    }

    @Test
    @DisplayName("Unknown category is rejected")
    void unknownCategoryRejected() {
        assertThatThrownBy(() -> statsService.getStats("comics", USER_ID))
                .isInstanceOf(InvalidInputException.class);
    }
}
