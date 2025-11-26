package org.criticizer.dao.movie;


import org.criticizer.entity.Movie;

import java.util.List;

public interface MovieDao {
    List<Movie> getUserMovies(int userId);

    int addMovie(String name, int userId, int score, List<Integer> genreIds);

    void removeMovie(String name, int userId);

    void toggleMovieStatus(String name, int userId);

    void updateMovieAndName(String oldName, String newName, int newScore, int userId, List<Integer> genreIds);

    boolean isMovieExists(String name, int userId);

    boolean getMovieStatus(String name, int userId);

    void deleteMoviesByUserId(int userId);

    int countTotal();

    List<Movie> findMoviesByUserId(int userId, Integer genreId, String searchTerm, int offset, int limit, String sortBy, String sortOrder);

    int countTotalForUser(int userId, Integer genreId, String searchTerm);
}

