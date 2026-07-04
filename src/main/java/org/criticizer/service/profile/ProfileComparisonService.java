package org.criticizer.service.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.criticizer.entity.MediaStatus;
import org.criticizer.entity.User;
import org.criticizer.repository.BookRepository;
import org.criticizer.repository.GameRepository;
import org.criticizer.repository.MovieRepository;
import org.criticizer.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compares two users' media lists: items they both rated (matched by name, case-insensitive), score
 * differences and a taste-compatibility percentage.
 *
 * <p>Compatibility is the average of {@code 100 - |scoreA - scoreB|} over the common items, so two
 * identical lists score 100 and maximally opposed ratings tend towards 1.
 */
@Service
public class ProfileComparisonService {

    // Biggest disagreements sort first, so the cap only trims the tail of
    // near-identical ratings.
    private static final int MAX_ROWS_PER_CATEGORY = 100;

    private static final List<String> CATEGORIES = List.of("games", "movies", "books", "shows");

    private final GameRepository gameRepository;
    private final MovieRepository movieRepository;
    private final BookRepository bookRepository;
    private final ShowRepository showRepository;

    public ProfileComparisonService(
            GameRepository gameRepository,
            MovieRepository movieRepository,
            BookRepository bookRepository,
            ShowRepository showRepository) {
        this.gameRepository = gameRepository;
        this.movieRepository = movieRepository;
        this.bookRepository = bookRepository;
        this.showRepository = showRepository;
    }

    private record Item(String name, int score, MediaStatus status) {}

    @Transactional(readOnly = true)
    public Map<String, Object> compare(User me, User other) {
        Map<String, Object> categories = new LinkedHashMap<>();
        long totalDiff = 0;
        int totalCommon = 0;

        for (String category : CATEGORIES) {
            List<Item> mine = loadItems(category, me.getId());
            List<Item> theirs = loadItems(category, other.getId());

            Map<String, Item> theirsByName = new LinkedHashMap<>();
            for (Item item : theirs) {
                theirsByName.putIfAbsent(normalize(item.name()), item);
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            long categoryDiff = 0;
            for (Item mineItem : mine) {
                Item theirItem = theirsByName.get(normalize(mineItem.name()));
                if (theirItem == null) continue;

                int diff = mineItem.score() - theirItem.score();
                categoryDiff += Math.abs(diff);

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", mineItem.name());
                row.put("myScore", mineItem.score());
                row.put("theirScore", theirItem.score());
                row.put("diff", diff);
                row.put("myStatus", mineItem.status().name());
                row.put("theirStatus", theirItem.status().name());
                rows.add(row);
            }

            rows.sort(
                    Comparator.comparingInt(
                                    (Map<String, Object> r) -> -Math.abs((int) r.get("diff")))
                            .thenComparing(r -> ((String) r.get("name")).toLowerCase(Locale.ROOT)));

            int common = rows.size();
            totalCommon += common;
            totalDiff += categoryDiff;

            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("myCount", mine.size());
            cat.put("theirCount", theirs.size());
            cat.put("commonCount", common);
            cat.put("avgDiff", common == 0 ? null : round1((double) categoryDiff / common));
            cat.put(
                    "compatibility",
                    common == 0 ? null : round1(100.0 - (double) categoryDiff / common));
            cat.put(
                    "items",
                    rows.size() > MAX_ROWS_PER_CATEGORY
                            ? rows.subList(0, MAX_ROWS_PER_CATEGORY)
                            : rows);
            categories.put(category, cat);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("me", me.getName());
        result.put("other", other.getName());
        result.put("commonCount", totalCommon);
        result.put(
                "compatibility",
                totalCommon == 0 ? null : round1(100.0 - (double) totalDiff / totalCommon));
        result.put("categories", categories);
        return result;
    }

    private List<Item> loadItems(String category, Integer userId) {
        return switch (category) {
            case "games" ->
                    gameRepository.findByUserIdWithTags(userId).stream()
                            .map(g -> new Item(g.getName(), g.getScore(), g.getStatus()))
                            .toList();
            case "movies" ->
                    movieRepository.findByUserIdWithGenres(userId).stream()
                            .map(m -> new Item(m.getName(), m.getScore(), m.getStatus()))
                            .toList();
            case "books" ->
                    bookRepository.findByUserIdWithGenres(userId).stream()
                            .map(b -> new Item(b.getName(), b.getScore(), b.getStatus()))
                            .toList();
            case "shows" ->
                    showRepository.findByUserIdWithGenres(userId).stream()
                            .map(s -> new Item(s.getName(), s.getScore(), s.getStatus()))
                            .toList();
            default -> List.of();
        };
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
