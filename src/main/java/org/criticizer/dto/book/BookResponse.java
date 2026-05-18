package org.criticizer.dto.book;

import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.entity.Book;
import org.criticizer.entity.MediaStatus;

import java.util.List;

/**
 * Response DTO for a book.
 */
public class BookResponse {

    private final int id;
    private final String name;
    private final String coverUrl;
    private final int score;
    private final boolean completed;
    private final MediaStatus status;
    private final List<GenreResponse> genres;

    public BookResponse(int id, String name, String coverUrl, int score,
                        boolean completed, MediaStatus status, List<GenreResponse> genres) {
        this.id = id;
        this.name = name;
        this.coverUrl = coverUrl;
        this.score = score;
        this.completed = completed;
        this.status = status == null ? MediaStatus.NONE : status;
        this.genres = genres != null ? List.copyOf(genres) : List.of();
    }

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getName(),
                book.getCoverUrl(),
                book.getScore(),
                book.isCompleted(),
                book.getStatus(),
                book.getGenres().stream()
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

    public MediaStatus getStatus() {
        return status;
    }

    public List<GenreResponse> getGenres() {
        return genres;
    }
}
