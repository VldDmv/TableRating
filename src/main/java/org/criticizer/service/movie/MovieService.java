package org.criticizer.service.movie;

import org.criticizer.entity.Movie;
import org.criticizer.service.user.UserPageResult;

import java.util.List;

public interface MovieService {
    List<Movie> getUserMovies(int userId);

    void addMovie(String name, int userId, int score, List<Integer> genreIds);

    void removeMovie(String name, int userId);

    void toggleMovieStatus(String name, int userId);

    void updateMovieAndName(String oldName, String newName, int newScore, int userId, List<Integer> genreIds);

    boolean isMovieExists(String name, int userId);

    boolean getMovieStatus(String name, int userId);

    UserPageResult<Movie> getUserMoviesPage(int userId, int page, int pageSize, Integer genreId, String searchTerm, String sortBy, String sortOrder);

}
