package org.criticizer.dto.user;

import java.time.LocalDateTime;

/**
 * Public user profile response with statistics.
 */
public class UserPublicResponse {

    private final String name;
    private final int gamesCount;
    private final int moviesCount;
    private final int booksCount;
    private final int showsCount;
    private final int totalItems;
    private final boolean profileIsPublic;
    private final LocalDateTime createdAt;

    public UserPublicResponse(String name, int gamesCount, int moviesCount,
                              int booksCount, int showsCount, int totalItems,
                              boolean profileIsPublic, LocalDateTime createdAt) {
        this.name = name;
        this.gamesCount = gamesCount;
        this.moviesCount = moviesCount;
        this.booksCount = booksCount;
        this.showsCount = showsCount;
        this.totalItems = totalItems;
        this.profileIsPublic = profileIsPublic;
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public int getGamesCount() {
        return gamesCount;
    }

    public int getMoviesCount() {
        return moviesCount;
    }

    public int getBooksCount() {
        return booksCount;
    }

    public int getShowsCount() {
        return showsCount;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public boolean isProfileIsPublic() {
        return profileIsPublic;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}