package org.criticizer.dto.genre;

import org.criticizer.entity.Genre;

import java.util.List;

/**
 * Response DTO for a genre.
 */
public class GenreResponse {

    private final Integer id;
    private final String name;
    private final List<String> mediaTypes;

    public GenreResponse(Integer id, String name, List<String> mediaTypes) {
        this.id = id;
        this.name = name;
        this.mediaTypes = mediaTypes != null ? List.copyOf(mediaTypes) : List.of();
    }

    public static GenreResponse from(Genre genre, List<String> mediaTypes) {
        return new GenreResponse(genre.getId(), genre.getName(), mediaTypes);
    }

    public static GenreResponse from(Genre genre) {
        return new GenreResponse(genre.getId(), genre.getName(), List.of());
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getMediaTypes() {
        return mediaTypes;
    }
}