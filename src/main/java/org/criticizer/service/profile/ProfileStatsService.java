package org.criticizer.service.profile;

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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes per-category rating statistics for a user's profile:
 * score distribution, completed vs not, and average score per tag/genre.
 */
@Service
public class ProfileStatsService {

    private final GameRepository gameRepository;
    private final MovieRepository movieRepository;
    private final BookRepository bookRepository;
    private final ShowRepository showRepository;

    public ProfileStatsService(GameRepository gameRepository,
                               MovieRepository movieRepository,
                               BookRepository bookRepository,
                               ShowRepository showRepository) {
        this.gameRepository = gameRepository;
        this.movieRepository = movieRepository;
        this.bookRepository = bookRepository;
        this.showRepository = showRepository;
    }

    private record Item(int score, boolean completed, List<String> categories) {}

    @Transactional(readOnly = true)
    public Map<String, Object> getStats(String category, Integer userId) {
        List<Item> items = loadItems(category, userId);

        // Score distribution across ten 10-point buckets: 1-10, 11-20, ... 91-100.
        int[] buckets = new int[10];
        long completed = 0;
        long scoreSum = 0;
        for (Item it : items) {
            int idx = Math.min(9, Math.max(0, (it.score() - 1) / 10));
            buckets[idx]++;
            if (it.completed()) completed++;
            scoreSum += it.score();
        }

        int count = items.size();
        double avgScore = count > 0 ? Math.round(scoreSum * 10.0 / count) / 10.0 : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        result.put("avgScore", avgScore);
        result.put("completed", completed);
        result.put("notCompleted", count - completed);
        result.put("scoreBuckets", buckets);
        result.put("categoryAverages", topCategoryAverages(items));
        return result;
    }

    /** Average score per tag/genre, sorted by average descending, capped at 8 entries. */
    private List<Map<String, Object>> topCategoryAverages(List<Item> items) {
        Map<String, long[]> agg = new LinkedHashMap<>(); // name -> [sum, count]
        for (Item it : items) {
            for (String name : it.categories()) {
                long[] sc = agg.computeIfAbsent(name, k -> new long[2]);
                sc[0] += it.score();
                sc[1]++;
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (var e : agg.entrySet()) {
            long sum = e.getValue()[0];
            long c = e.getValue()[1];
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", e.getKey());
            row.put("avg", Math.round(sum * 10.0 / c) / 10.0);
            row.put("count", c);
            rows.add(row);
        }
        rows.sort(Comparator.comparingDouble((Map<String, Object> r) -> (double) r.get("avg")).reversed());
        return rows.size() > 8 ? rows.subList(0, 8) : rows;
    }

    private List<Item> loadItems(String category, Integer userId) {
        return switch (validateCategory(category)) {
            case "games" -> gameRepository.findByUserIdWithTags(userId).stream()
                    .map(g -> new Item(g.getScore(), g.isCompleted(), tagNames(g.getTags())))
                    .toList();
            case "movies" -> movieRepository.findByUserIdWithGenres(userId).stream()
                    .map(m -> new Item(m.getScore(), m.isCompleted(), genreNames(m.getGenres())))
                    .toList();
            case "books" -> bookRepository.findByUserIdWithGenres(userId).stream()
                    .map(b -> new Item(b.getScore(), b.isCompleted(), genreNames(b.getGenres())))
                    .toList();
            case "shows" -> showRepository.findByUserIdWithGenres(userId).stream()
                    .map(s -> new Item(s.getScore(), s.isCompleted(), genreNames(s.getGenres())))
                    .toList();
            default -> List.of();
        };
    }

    private String validateCategory(String category) {
        String c = category == null ? "" : category.toLowerCase();
        if (!List.of("games", "movies", "books", "shows").contains(c)) {
            throw new InvalidInputException("Unknown category: " + category);
        }
        return c;
    }

    private List<String> tagNames(Set<Tag> tags) {
        return tags == null ? List.of() : tags.stream().map(Tag::getName).toList();
    }

    private List<String> genreNames(Set<Genre> genres) {
        return genres == null ? List.of() : genres.stream().map(Genre::getName).toList();
    }
}
