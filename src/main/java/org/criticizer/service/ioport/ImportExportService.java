package org.criticizer.service.ioport;

import org.criticizer.dto.ioport.ExportRow;
import org.criticizer.dto.ioport.ImportResult;
import org.criticizer.entity.Book;
import org.criticizer.entity.Game;
import org.criticizer.entity.Genre;
import org.criticizer.entity.Movie;
import org.criticizer.entity.Show;
import org.criticizer.entity.Tag;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.repository.BookRepository;
import org.criticizer.repository.GameRepository;
import org.criticizer.repository.GenreRepository;
import org.criticizer.repository.MovieRepository;
import org.criticizer.repository.ShowRepository;
import org.criticizer.repository.TagRepository;
import org.criticizer.service.book.BookService;
import org.criticizer.service.game.GameService;
import org.criticizer.service.movie.MovieService;
import org.criticizer.service.show.ShowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Exports a user's media collection and imports it back from CSV/JSON.
 *
 * <p>Category names that don't exist in the global Tag/Genre tables are
 * silently dropped on import — items still get created with the categories
 * that did resolve.
 */
@Service
public class ImportExportService {

    private static final Logger log = LoggerFactory.getLogger(ImportExportService.class);

    public static final List<String> CATEGORIES = List.of("games", "movies", "books", "shows");

    private final GameRepository gameRepository;
    private final MovieRepository movieRepository;
    private final BookRepository bookRepository;
    private final ShowRepository showRepository;
    private final TagRepository tagRepository;
    private final GenreRepository genreRepository;

    private final GameService gameService;
    private final MovieService movieService;
    private final BookService bookService;
    private final ShowService showService;

    public ImportExportService(GameRepository gameRepository,
                               MovieRepository movieRepository,
                               BookRepository bookRepository,
                               ShowRepository showRepository,
                               TagRepository tagRepository,
                               GenreRepository genreRepository,
                               GameService gameService,
                               MovieService movieService,
                               BookService bookService,
                               ShowService showService) {
        this.gameRepository = gameRepository;
        this.movieRepository = movieRepository;
        this.bookRepository = bookRepository;
        this.showRepository = showRepository;
        this.tagRepository = tagRepository;
        this.genreRepository = genreRepository;
        this.gameService = gameService;
        this.movieService = movieService;
        this.bookService = bookService;
        this.showService = showService;
    }

    @Transactional(readOnly = true)
    public List<ExportRow> exportItems(String category, Integer userId) {
        return switch (validateCategory(category)) {
            case "games" -> gameRepository.findByUserId(userId).stream()
                    .map(this::toRow)
                    .sorted(Comparator.comparing(ExportRow::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            case "movies" -> movieRepository.findByUserId(userId).stream()
                    .map(this::toRow)
                    .sorted(Comparator.comparing(ExportRow::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            case "books" -> bookRepository.findByUserId(userId).stream()
                    .map(this::toRow)
                    .sorted(Comparator.comparing(ExportRow::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            case "shows" -> showRepository.findByUserId(userId).stream()
                    .map(this::toRow)
                    .sorted(Comparator.comparing(ExportRow::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            default -> List.of();
        };
    }

    public ImportResult importItems(String category, Integer userId, List<ExportRow> rows) {
        String cat = validateCategory(category);
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (ExportRow row : rows) {
            try {
                List<Integer> categoryIds = resolveCategoryIds(cat, row.categories());
                switch (cat) {
                    case "games" -> gameService.addItem(
                            row.name(), row.coverUrl(), userId, row.score(), categoryIds);
                    case "movies" -> movieService.addItem(
                            row.name(), row.coverUrl(), userId, row.score(), categoryIds);
                    case "books" -> bookService.addItem(
                            row.name(), row.coverUrl(), userId, row.score(), categoryIds);
                    case "shows" -> showService.addItem(
                            row.name(), row.coverUrl(), userId, row.score(), categoryIds);
                    default -> { /* unreachable */ }
                }
                if (row.completed()) {
                    toggleByName(cat, row.name(), userId);
                }
                imported++;
            } catch (Exception e) {
                skipped++;
                String msg = "Row '" + row.name() + "': " + e.getMessage();
                log.debug("Import skip — {}", msg);
                if (errors.size() < 20) {
                    errors.add(msg);
                }
            }
        }

        log.info("Import {} for user {}: imported={}, skipped={}", cat, userId, imported, skipped);
        return new ImportResult(imported, skipped, errors);
    }

    private void toggleByName(String category, String name, Integer userId) {
        switch (category) {
            case "games" -> gameService.toggleStatus(name, userId);
            case "movies" -> movieService.toggleStatus(name, userId);
            case "books" -> bookService.toggleStatus(name, userId);
            case "shows" -> showService.toggleStatus(name, userId);
            default -> { /* unreachable */ }
        }
    }

    private List<Integer> resolveCategoryIds(String category, List<String> names) {
        if (names == null || names.isEmpty()) return List.of();
        List<Integer> ids = new ArrayList<>();
        for (String n : names) {
            if (n == null || n.isBlank()) continue;
            String trimmed = n.trim();
            if ("games".equals(category)) {
                tagRepository.findByNameIgnoreCase(trimmed).ifPresent(t -> ids.add(t.getId()));
            } else {
                genreRepository.findByNameIgnoreCase(trimmed).ifPresent(g -> ids.add(g.getId()));
            }
        }
        return ids;
    }

    private String validateCategory(String category) {
        String c = category == null ? "" : category.toLowerCase();
        if (!CATEGORIES.contains(c)) {
            throw new InvalidInputException("Unknown category: " + category);
        }
        return c;
    }

    private ExportRow toRow(Game g) {
        return new ExportRow(g.getName(), g.getScore(), g.isCompleted(), g.getCoverUrl(),
                names(g.getTags()));
    }

    private ExportRow toRow(Movie m) {
        return new ExportRow(m.getName(), m.getScore(), m.isCompleted(), m.getCoverUrl(),
                genreNames(m.getGenres()));
    }

    private ExportRow toRow(Book b) {
        return new ExportRow(b.getName(), b.getScore(), b.isCompleted(), b.getCoverUrl(),
                genreNames(b.getGenres()));
    }

    private ExportRow toRow(Show s) {
        return new ExportRow(s.getName(), s.getScore(), s.isCompleted(), s.getCoverUrl(),
                genreNames(s.getGenres()));
    }

    private List<String> names(Set<Tag> tags) {
        return tags == null ? List.of()
                : tags.stream().map(Tag::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private List<String> genreNames(Set<Genre> genres) {
        return genres == null ? List.of()
                : genres.stream().map(Genre::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
