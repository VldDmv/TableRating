package org.criticizer.constants;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Centralized enumeration for all media types in the system. Named ContentCategory to avoid
 * conflict with org.springframework.http.MediaType.
 */
public enum ContentCategory {
    GAMES("games", "game", "Game", "Games"),
    MOVIES("movies", "movie", "Movie", "Movies"),
    BOOKS("books", "book", "Book", "Books"),
    SHOWS("shows", "show", "Show", "Shows");

    private final String plural;
    private final String singular;
    private final String displayName;
    private final String displayPlural;

    ContentCategory(String plural, String singular, String displayName, String displayPlural) {
        this.plural = plural;
        this.singular = singular;
        this.displayName = displayName;
        this.displayPlural = displayPlural;
    }

    public String getPlural() {
        return plural;
    }

    public String getSingular() {
        return singular;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayPlural() {
        return displayPlural;
    }

    public CategoryType getCategoryType() {
        return this == GAMES ? CategoryType.TAG : CategoryType.GENRE;
    }

    public String getApiPath() {
        return "/api/" + plural;
    }

    public String getCategoryApiPath() {
        return "/api/category/" + plural;
    }

    public static ContentCategory fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Content category cannot be null or empty");
        }

        String normalized = value.trim().toLowerCase();

        for (ContentCategory type : values()) {
            if (type.plural.equals(normalized) || type.singular.equals(normalized)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Invalid content category: '" + value + "'. Valid types: " + getValidTypesString());
    }

    public static boolean isValid(String value) {
        try {
            fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String getValidTypesString() {
        return Arrays.stream(values())
                .map(ContentCategory::getPlural)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return plural;
    }

    public enum CategoryType {
        TAG("tags", "tagIds"),
        GENRE("genres", "genreIds");

        private final String name;
        private final String paramName;

        CategoryType(String name, String paramName) {
            this.name = name;
            this.paramName = paramName;
        }

        public String getName() {
            return name;
        }

        public String getParamName() {
            return paramName;
        }
    }
}
