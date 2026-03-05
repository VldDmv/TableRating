package org.criticizer.dto.admin;

/**
 * DTO for admin dashboard statistics.
 */
public class AdminStats {

    private final long totalUsers;
    private final long totalGames;
    private final long totalMovies;
    private final long totalBooks;
    private final long totalShows;

    public AdminStats(long totalUsers, long totalGames, long totalMovies,
                      long totalBooks, long totalShows) {
        this.totalUsers = totalUsers;
        this.totalGames = totalGames;
        this.totalMovies = totalMovies;
        this.totalBooks = totalBooks;
        this.totalShows = totalShows;
    }

    public static AdminStats of(long totalUsers, long totalGames, long totalMovies,
                                long totalBooks, long totalShows) {
        return new AdminStats(totalUsers, totalGames, totalMovies, totalBooks, totalShows);
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalGames() {
        return totalGames;
    }

    public long getTotalMovies() {
        return totalMovies;
    }

    public long getTotalBooks() {
        return totalBooks;
    }

    public long getTotalShows() {
        return totalShows;
    }
}