package org.criticizer.service.ioport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import org.criticizer.dto.ioport.ExportRow;
import org.criticizer.entity.Game;
import org.criticizer.entity.Genre;
import org.criticizer.entity.Movie;
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
@DisplayName("ExportService Tests")
class ExportServiceTest {

    private static final int USER_ID = 5;

    @Mock private GameRepository gameRepository;
    @Mock private MovieRepository movieRepository;
    @Mock private BookRepository bookRepository;
    @Mock private ShowRepository showRepository;

    @InjectMocks private ExportService exportService;

    @Test
    @DisplayName("Exports games sorted by name with tag names")
    void exportsGamesSortedWithTags() {
        Game witcher = new Game(1, "The Witcher 3", USER_ID, 95, true);
        witcher.setCoverUrl("http://cover/w3.jpg");
        witcher.setTags(new HashSet<>(List.of(new Tag(1, "RPG"), new Tag(2, "Action"))));

        Game doom = new Game(2, "Doom", USER_ID, 88, false);
        doom.setTags(new HashSet<>());

        when(gameRepository.findByUserIdWithTags(USER_ID)).thenReturn(List.of(witcher, doom));

        List<ExportRow> rows = exportService.exportItems("games", USER_ID);

        assertThat(rows).hasSize(2);
        // Sorted case-insensitively by name: Doom before The Witcher 3.
        assertThat(rows.get(0).name()).isEqualTo("Doom");
        assertThat(rows.get(1).name()).isEqualTo("The Witcher 3");

        ExportRow w3 = rows.get(1);
        assertThat(w3.score()).isEqualTo(95);
        assertThat(w3.completed()).isTrue();
        assertThat(w3.coverUrl()).isEqualTo("http://cover/w3.jpg");
        // Categories are sorted alphabetically.
        assertThat(w3.categories()).containsExactly("Action", "RPG");
        assertThat(rows.get(0).categories()).isEmpty();
    }

    @Test
    @DisplayName("Exports movies via the genre-eager finder")
    void exportsMoviesWithGenres() {
        Movie matrix = new Movie(1, "The Matrix", USER_ID, 96, true);
        matrix.setGenres(new HashSet<>(List.of(new Genre(1, "Sci-Fi"))));
        when(movieRepository.findByUserIdWithGenres(USER_ID)).thenReturn(List.of(matrix));

        List<ExportRow> rows = exportService.exportItems("movies", USER_ID);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).categories()).containsExactly("Sci-Fi");
    }

    @Test
    @DisplayName("Empty collection yields an empty list")
    void emptyCollection() {
        when(showRepository.findByUserIdWithGenres(USER_ID)).thenReturn(List.of());
        assertThat(exportService.exportItems("shows", USER_ID)).isEmpty();
    }

    @Test
    @DisplayName("Unknown category is rejected")
    void unknownCategoryRejected() {
        assertThatThrownBy(() -> exportService.exportItems("comics", USER_ID))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("Category is case-insensitive")
    void categoryCaseInsensitive() {
        when(gameRepository.findByUserIdWithTags(USER_ID)).thenReturn(List.of());
        assertThat(exportService.exportItems("GAMES", USER_ID)).isEmpty();
    }
}
