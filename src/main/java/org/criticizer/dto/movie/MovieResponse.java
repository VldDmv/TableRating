package org.criticizer.dto.movie;

import java.util.List;
import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.entity.Movie;

/** Response DTO for a movie. */
public class MovieResponse {

    private final int id;
    private final String name;
    private final String coverUrl;
    private final int score;
    private final String status;
    private final List<GenreResponse> genres;

    public MovieResponse(
            int id,
            String name,
            String coverUrl,
            int score,
            String status,
            List<GenreResponse> genres) {
        this.id = id;
        this.name = name;
        this.coverUrl = coverUrl;
        this.score = score;
        this.status = status;
        this.genres = genres != null ? List.copyOf(genres) : List.of();
    }

    public static MovieResponse from(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getName(),
                movie.getCoverUrl(),
                movie.getScore(),
                movie.getStatus().name(),
                movie.getGenres().stream().map(GenreResponse::from).toList());
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

    public String getStatus() {
        return status;
    }

    public List<GenreResponse> getGenres() {
        return genres;
    }
}
