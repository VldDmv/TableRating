package org.criticizer.util;

import org.criticizer.dto.admin.AdminStats;
import org.criticizer.dto.helper.PageResponse;
import org.criticizer.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building test data objects.
 * Provides builder methods for creating test entities and DTOs.
 * Uses reflection to bypass protected constructors for testing purposes.
 */
public class TestDataBuilder {

    // ============= User Builders =============

    public static User createUser(int id, String name, Role role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setPassword("$2a$10$encoded.password.hash");
        user.setRole(role);
        user.setProfileIsPublic(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    public static User createAdminUser() {
        return createUser(1, "admin", Role.ADMIN);
    }

    public static User createRegularUser() {
        return createUser(2, "testuser", Role.USER);
    }

    public static User createPrivateUser() {
        User user = createUser(3, "privateuser", Role.USER);
        user.setProfileIsPublic(false);
        return user;
    }

    // ============= Tag Builders =============

    public static Tag createTag(int id, String name) {
        try {
            Tag tag = createInstance(Tag.class);
            setField(tag, "id", id);
            setField(tag, "name", name);
            return tag;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Tag", e);
        }
    }

    public static List<Tag> createTags(int count) {
        List<Tag> tags = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            tags.add(createTag(i, "Tag" + i));
        }
        return tags;
    }

    // ============= Genre Builders =============

    public static Genre createGenre(int id, String name, String... mediaTypes) {
        try {
            Genre genre = createInstance(Genre.class);
            setField(genre, "id", id);
            setField(genre, "name", name);
            return genre;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Genre", e);
        }
    }

    public static List<Genre> createGenres(int count) {
        List<Genre> genres = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            genres.add(createGenre(i, "Genre" + i, "movie", "book", "show"));
        }
        return genres;
    }

    // ============= Game Builders =============

    public static Game createGame(int id, String name, int userId, int score) {
        try {
            Game game = createInstance(Game.class);
            setField(game, "id", id);
            setField(game, "name", name);
            setField(game, "userId", userId);
            setField(game, "score", score);
            setField(game, "completed", true);
            setField(game, "coverUrl", "https://example.com/cover.jpg");
            return game;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Game", e);
        }
    }

    public static List<Game> createGames(int count, int userId) {
        List<Game> games = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            games.add(createGame(i, "Game" + i, userId, 80 + i));
        }
        return games;
    }

    // ============= Movie Builders =============

    public static Movie createMovie(int id, String name, int userId, int score) {
        try {
            Movie movie = createInstance(Movie.class);
            setField(movie, "id", id);
            setField(movie, "name", name);
            setField(movie, "userId", userId);
            setField(movie, "score", score);
            setField(movie, "completed", true);
            setField(movie, "coverUrl", "https://example.com/poster.jpg");
            return movie;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Movie", e);
        }
    }

    public static List<Movie> createMovies(int count, int userId) {
        List<Movie> movies = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            movies.add(createMovie(i, "Movie" + i, userId, 70 + i));
        }
        return movies;
    }

    // ============= Book Builders =============

    public static Book createBook(int id, String name, int userId, int score) {
        try {
            Book book = createInstance(Book.class);
            setField(book, "id", id);
            setField(book, "name", name);
            setField(book, "userId", userId);
            setField(book, "score", score);
            setField(book, "completed", true);
            setField(book, "coverUrl", "https://example.com/book-cover.jpg");
            return book;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Book", e);
        }
    }

    public static List<Book> createBooks(int count, int userId) {
        List<Book> books = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            books.add(createBook(i, "Book" + i, userId, 85 + i));
        }
        return books;
    }

    // ============= Show Builders =============

    public static Show createShow(int id, String name, int userId, int score) {
        try {
            Show show = createInstance(Show.class);
            setField(show, "id", id);
            setField(show, "name", name);
            setField(show, "userId", userId);
            setField(show, "score", score);
            setField(show, "completed", true);
            setField(show, "coverUrl", "https://example.com/show-poster.jpg");
            return show;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Show", e);
        }
    }

    public static List<Show> createShows(int count, int userId) {
        List<Show> shows = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            shows.add(createShow(i, "Show" + i, userId, 75 + i));
        }
        return shows;
    }

    // ============= Page Response Builders =============

    public static <T> PageResponse<T> createPageResponse(List<T> items, int page, int size) {
        Page<T> springPage = new PageImpl<>(
                items,
                PageRequest.of(page - 1, size),
                items.size()
        );
        return PageResponse.of(springPage);
    }

    public static <T> PageResponse<T> createEmptyPageResponse(int page, int size) {
        return createPageResponse(List.of(), page, size);
    }

    // ============= Statistics Builders =============

    public static AdminStats createAdminStats() {
        return new AdminStats(
                100,  // totalUsers
                50,   // totalGames
                30,   // totalMovies
                20,   // totalBooks
                15    // totalShows
        );
    }

    // ============= Reflection Helpers =============

    /**
     * Helper method to safely instantiate objects with protected/private no-args constructors.
     */
    private static <T> T createInstance(Class<T> clazz) throws Exception {
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    /**
     * Helper method to set private/protected fields using reflection.
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(target, value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    /**
     * Find field in class hierarchy.
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}