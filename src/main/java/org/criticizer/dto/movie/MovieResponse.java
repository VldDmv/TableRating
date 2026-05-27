package org.criticizer.dto.movie;

import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.entity.Movie;

import java.util.List;

/**
 * Response DTO for a movie.
 */
public class MovieResponse {

    private final int id;
    private final String name;
    private final String coverUrl;
    private final int score;
    private final boolean completed;
    private final List<GenreResponse> genres;

    public MovieResponse(int id, String name, String coverUrl, int score,
                         boolean completed, List<GenreResponse> genres) {
        this.id = id;
        this.name = name;
        this.coverUrl = coverUrl;
        this.score = score;
        this.completed = completed;
        this.genres = genres != null ? List.copyOf(genres) : List.of();
    }

    public static MovieResponse from(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getName(),
                movie.getCoverUrl(),
                movie.getScore(),
                movie.isCompleted(),
                movie.getGenres().stream()
                        .map(GenreResponse::from)
                        .toList()
        );
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public int getScore() {
        return score;
    }

    public boolean isCompleted() {
        return completed;
    }

    public List<GenreResponse> getGenres() {
        return genres;
    }
}