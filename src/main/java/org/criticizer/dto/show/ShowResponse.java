package org.criticizer.dto.show;

import java.util.List;
import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.entity.Show;

/** Response DTO for a show. */
public class ShowResponse {

    private final int id;
    private final String name;
    private final String coverUrl;
    private final int score;
    private final String status;
    private final List<GenreResponse> genres;

    public ShowResponse(
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

    public static ShowResponse from(Show show) {
        return new ShowResponse(
                show.getId(),
                show.getName(),
                show.getCoverUrl(),
                show.getScore(),
                show.getStatus().name(),
                show.getGenres().stream().map(GenreResponse::from).toList());
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
