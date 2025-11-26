package org.criticizer.service.movie;

import org.criticizer.dao.movie.MovieDao;
import org.criticizer.entity.Movie;
import org.criticizer.service.helper.ServiceValidator;
import org.criticizer.service.user.UserPageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of the MovieService interface for managing movie-related operations.
 */
public class MovieServiceImpl implements MovieService {
    private static final Logger log = LoggerFactory.getLogger(MovieServiceImpl.class);
    private final ServiceValidator validator;
    private final MovieDao movieDao;

    public MovieServiceImpl(MovieDao movieDao, ServiceValidator validator) {
        this.movieDao = movieDao;
        this.validator = validator;
    }

    @Override
    public List<Movie> getUserMovies(int userId) {
        return movieDao.getUserMovies(userId);
    }

    @Override
    public boolean getMovieStatus(String name, int userId) {
        return movieDao.getMovieStatus(name, userId);
    }

    @Override
    public void updateMovieAndName(String oldName, String newName, int newScore, int userId, List<Integer> genreIds) {
        validator.validateScore(newScore, userId, "Movie");

        log.info("Updating movie '{}' to '{}' with score {} for user {}", oldName, newName, newScore, userId);
        movieDao.updateMovieAndName(oldName, newName, newScore, userId, genreIds);
    }

    @Override
    public boolean isMovieExists(String name, int userId) {
        return movieDao.isMovieExists(name, userId);
    }

    @Override
    public void addMovie(String name, int userId, int score, List<Integer> genreIds) {
        validator.validateScore(score, userId, "Movie");

        log.info("Adding movie '{}' with score {} for user {}", name, score, userId);
        movieDao.addMovie(name, userId, score, genreIds);
    }

    @Override
    public UserPageResult<Movie> getUserMoviesPage(int userId, int page, int pageSize,
                                                   Integer genreId, String searchTerm,
                                                   String sortBy, String sortOrder) {
        ServiceValidator.PaginationParams params = validator.validatePagination(page, pageSize);
        String sanitizedSearch = validator.sanitizeSearchTerm(searchTerm);

        List<Movie> moviesOnPage = movieDao.findMoviesByUserId(
                userId, genreId, sanitizedSearch,
                params.offset(), params.pageSize(),
                sortBy, sortOrder
        );

        int totalMovies = movieDao.countTotalForUser(userId, genreId, sanitizedSearch);

        return new UserPageResult<>(moviesOnPage, totalMovies, params.page(), params.pageSize());
    }

    @Override
    public void removeMovie(String name, int userId) {
        log.info("Removing movie '{}' for user {}", name, userId);
        movieDao.removeMovie(name, userId);
    }

    @Override
    public void toggleMovieStatus(String name, int userId) {
        log.debug("Toggling status for movie '{}' for user {}", name, userId);
        movieDao.toggleMovieStatus(name, userId);
    }
}