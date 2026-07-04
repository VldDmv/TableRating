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
    private final boolean completed;
    private final List<GenreResponse> genres;

    public ShowResponse(
            int id,
            String name,
            String coverUrl,
            int score,
            boolean completed,
            List<GenreResponse> genres) {
        this.id = id;
        this.name = name;
        this.coverUrl = coverUrl;
        this.score = score;
        this.completed = completed;
        this.genres = genres != null ? List.copyOf(genres) : List.of();
    }

    public static ShowResponse from(Show show) {
        return new ShowResponse(
                show.getId(),
                show.getName(),
                show.getCoverUrl(),
                show.getScore(),
                show.isCompleted(),
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

    public boolean isCompleted() {
        return completed;
    }

    public List<GenreResponse> getGenres() {
        return genres;
    }
}
