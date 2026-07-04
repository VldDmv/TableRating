package org.criticizer.service.ioport;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.criticizer.dto.ioport.ExportRow;
import org.criticizer.entity.Book;
import org.criticizer.entity.Game;
import org.criticizer.entity.Genre;
import org.criticizer.entity.Movie;
import org.criticizer.entity.Show;
import org.criticizer.entity.Tag;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.repository.BookRepository;
import org.criticizer.repository.GameRepository;
import org.criticizer.repository.MovieRepository;
import org.criticizer.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exports a user's media collection (games/movies/books/shows) as a flat list of rows for CSV
 * download.
 */
@Service
public class ExportService {

    public static final List<String> CATEGORIES = List.of("games", "movies", "books", "shows");

    private final GameRepository gameRepository;
    private final MovieRepository movieRepository;
    private final BookRepository bookRepository;
    private final ShowRepository showRepository;

    public ExportService(
            GameRepository gameRepository,
            MovieRepository movieRepository,
            BookRepository bookRepository,
            ShowRepository showRepository) {
        this.gameRepository = gameRepository;
        this.movieRepository = movieRepository;
        this.bookRepository = bookRepository;
        this.showRepository = showRepository;
    }

    @Transactional(readOnly = true)
    public List<ExportRow> exportItems(String category, Integer userId) {
        return switch (validateCategory(category)) {
            case "games" ->
                    gameRepository.findByUserIdWithTags(userId).stream()
                            .map(this::toRow)
                            .sorted(
                                    Comparator.comparing(
                                            ExportRow::name, String.CASE_INSENSITIVE_ORDER))
                            .toList();
            case "movies" ->
                    movieRepository.findByUserIdWithGenres(userId).stream()
                            .map(this::toRow)
                            .sorted(
                                    Comparator.comparing(
                                            ExportRow::name, String.CASE_INSENSITIVE_ORDER))
                            .toList();
            case "books" ->
                    bookRepository.findByUserIdWithGenres(userId).stream()
                            .map(this::toRow)
                            .sorted(
                                    Comparator.comparing(
                                            ExportRow::name, String.CASE_INSENSITIVE_ORDER))
                            .toList();
            case "shows" ->
                    showRepository.findByUserIdWithGenres(userId).stream()
                            .map(this::toRow)
                            .sorted(
                                    Comparator.comparing(
                                            ExportRow::name, String.CASE_INSENSITIVE_ORDER))
                            .toList();
            default -> List.of();
        };
    }

    private String validateCategory(String category) {
        String c = category == null ? "" : category.toLowerCase();
        if (!CATEGORIES.contains(c)) {
            throw new InvalidInputException("Unknown category: " + category);
        }
        return c;
    }

    private ExportRow toRow(Game g) {
        return new ExportRow(
                g.getName(), g.getScore(), g.isCompleted(), g.getCoverUrl(), names(g.getTags()));
    }

    private ExportRow toRow(Movie m) {
        return new ExportRow(
                m.getName(),
                m.getScore(),
                m.isCompleted(),
                m.getCoverUrl(),
                genreNames(m.getGenres()));
    }

    private ExportRow toRow(Book b) {
        return new ExportRow(
                b.getName(),
                b.getScore(),
                b.isCompleted(),
                b.getCoverUrl(),
                genreNames(b.getGenres()));
    }

    private ExportRow toRow(Show s) {
        return new ExportRow(
                s.getName(),
                s.getScore(),
                s.isCompleted(),
                s.getCoverUrl(),
                genreNames(s.getGenres()));
    }

    private List<String> names(Set<Tag> tags) {
        return tags == null
                ? List.of()
                : tags.stream().map(Tag::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private List<String> genreNames(Set<Genre> genres) {
        return genres == null
                ? List.of()
                : genres.stream()
                        .map(Genre::getName)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();
    }
}
