package org.criticizer.dao.movie;

import org.criticizer.constants.DbConstants;
import org.criticizer.dao.helper.AbstractMediaDao;
import org.criticizer.dao.helper.DaoHelperService;
import org.criticizer.dao.helper.EntityValidator;
import org.criticizer.entity.Genre;
import org.criticizer.entity.Movie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * MySQL implementation of MovieDao.
 */
public class MovieDaoService extends AbstractMediaDao<Movie, Genre> implements MovieDao {
    private static final Logger log = LoggerFactory.getLogger(MovieDaoService.class);

    public MovieDaoService(EntityValidator validator, DaoHelperService daoHelper) {
        super(log, validator, daoHelper,
                DbConstants.Tables.MOVIES,
                DbConstants.Tables.MOVIE_GENRES,
                DbConstants.Columns.MOVIE_ID,
                DbConstants.Columns.GENRE_ID,
                DbConstants.Tables.GENRES);
    }


    @Override
    protected Movie mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new Movie(
                rs.getInt(DbConstants.Columns.ID),
                rs.getString(DbConstants.Columns.NAME),
                rs.getInt(DbConstants.Columns.USER_ID),
                rs.getInt(DbConstants.Columns.SCORE),
                rs.getBoolean(DbConstants.Columns.COMPLETED)
        );
    }

    @Override
    protected Genre mapResultSetToAssociation(ResultSet rs) throws SQLException {
        return new Genre(
                rs.getInt(DbConstants.Columns.ID),
                rs.getString(DbConstants.Columns.NAME)
        );
    }

    @Override
    protected void setAssociations(Movie movie, List<Genre> genres) {
        movie.setGenres(genres);
    }

    @Override
    protected int getEntityId(Movie movie) {
        return movie.getId();
    }

    @Override
    protected String getEntityName() {
        return "Movie";
    }


    @Override
    public List<Movie> getUserMovies(int userId) {
        return getUserEntities(userId);
    }

    @Override
    public int addMovie(String name, int userId, int score, List<Integer> genreIds) {
        return addEntity(name, userId, score, genreIds);
    }

    @Override
    public void removeMovie(String name, int userId) {
        removeEntity(name, userId);
    }

    @Override
    public void toggleMovieStatus(String name, int userId) {
        toggleEntityStatus(name, userId);
    }

    @Override
    public void updateMovieAndName(String oldName, String newName, int newScore,
                                   int userId, List<Integer> genreIds) {
        updateEntityAndName(oldName, newName, newScore, userId, genreIds);
    }

    @Override
    public boolean isMovieExists(String name, int userId) {
        return isEntityExists(name, userId);
    }

    @Override
    public boolean getMovieStatus(String name, int userId) {
        return getEntityStatus(name, userId);
    }

    @Override
    public void deleteMoviesByUserId(int userId) {
        deleteEntitiesByUserId(userId);
    }

    @Override
    public int countTotal() {
        return super.countTotal();
    }

    @Override
    public List<Movie> findMoviesByUserId(int userId, Integer genreId, String searchTerm,
                                          int offset, int limit, String sortBy, String sortOrder) {
        return findEntities(userId, genreId, searchTerm, offset, limit, sortBy, sortOrder);
    }

    @Override
    public int countTotalForUser(int userId, Integer genreId, String searchTerm) {
        return super.countTotalForUser(userId, genreId, searchTerm);
    }
}