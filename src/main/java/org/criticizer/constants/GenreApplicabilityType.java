package org.criticizer.constants;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enumeration for genre applicability types. Defines which media types a genre can be applied to.
 */
public enum GenreApplicabilityType {
    MOVIE("movie"),
    BOOK("book"),
    SHOW("show"),
    SHARED("shared");

    private final String value;

    GenreApplicabilityType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static GenreApplicabilityType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Genre applicability type cannot be null");
        }

        String normalized = value.trim().toLowerCase();

        for (GenreApplicabilityType type : values()) {
            if (type.value.equals(normalized)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Invalid genre applicability type: '"
                        + value
                        + "'. Valid types: "
                        + getValidTypesString());
    }

    public static boolean isValid(String value) {
        try {
            fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static List<String> getValidTypes() {
        return Arrays.stream(values())
                .map(GenreApplicabilityType::getValue)
                .collect(Collectors.toList());
    }

    public static String getValidTypesString() {
        return String.join(", ", getValidTypes());
    }

    public boolean isApplicableTo(ContentCategory category) {
        if (this == SHARED) {
            return true;
        }

        return switch (this) {
            case MOVIE -> category == ContentCategory.MOVIES;
            case BOOK -> category == ContentCategory.BOOKS;
            case SHOW -> category == ContentCategory.SHOWS;
            default -> false;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}
