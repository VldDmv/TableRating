package org.criticizer.entity;

/**
 * Data Transfer Object for admin dashboard statistics.
 * Contains aggregate counts of all major entities in the system.
 */
public class AdminStats {
    private int totalUsers;
    private int totalGames;
    private int totalMovies;
    private int totalBooks;
    private int totalShows;

    public AdminStats() {
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }

    public int getTotalMovies() {
        return totalMovies;
    }

    public void setTotalMovies(int totalMovies) {
        this.totalMovies = totalMovies;
    }

    public int getTotalBooks() {
        return totalBooks;
    }

    public void setTotalBooks(int totalBooks) {
        this.totalBooks = totalBooks;
    }

    public int getTotalShows() {
        return totalShows;
    }

    public void setTotalShows(int totalShows) {
        this.totalShows = totalShows;
    }

    @Override
    public String toString() {
        return "AdminStats{" +
                "totalUsers=" + totalUsers +
                ", totalGames=" + totalGames +
                ", totalMovies=" + totalMovies +
                ", totalBooks=" + totalBooks +
                ", totalShows=" + totalShows +
                '}';
    }
}