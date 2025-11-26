package org.criticizer.dao.genre;

import org.criticizer.constants.DbConstants;
import org.criticizer.dao.helper.DaoHelperService;
import org.criticizer.entity.Genre;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.util.DataSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MySQL implementation of GenreDao
 */
public class GenreDaoService implements GenreDao {
    private static final Logger log = LoggerFactory.getLogger(GenreDaoService.class);
    private static final String[] MEDIA_TABLES = {
            DbConstants.Tables.MOVIE_GENRES,
            DbConstants.Tables.BOOK_GENRES,
            DbConstants.Tables.SHOW_GENRES
    };

    private final DaoHelperService daoHelper;

    public GenreDaoService(DaoHelperService daoHelper) {
        this.daoHelper = daoHelper;
    }

    @Override
    public List<Genre> getAllGenres() {
        String query = String.format(
                "SELECT g.%s, g.%s, GROUP_CONCAT(ga.%s SEPARATOR ',') as media_types " +
                        "FROM %s g " +
                        "LEFT JOIN %s ga ON g.%s = ga.%s " +
                        "GROUP BY g.%s, g.%s " +
                        "ORDER BY g.%s ASC",
                DbConstants.Columns.ID, DbConstants.Columns.NAME, DbConstants.Columns.MEDIA_TYPE,
                DbConstants.Tables.GENRES,
                DbConstants.Tables.GENRE_APPLICABILITY, DbConstants.Columns.ID, DbConstants.Columns.GENRE_ID,
                DbConstants.Columns.ID, DbConstants.Columns.NAME,
                DbConstants.Columns.NAME
        );

        return executeGenreQuery(query, stmt -> {
        }, "load all genres");
    }

    @Override
    public void addGenre(String genreName, List<String> mediaTypes) {
        daoHelper.executeInTransaction(conn -> {
            int newGenreId = insertGenre(conn, genreName);
            insertGenreApplicability(conn, newGenreId, mediaTypes);
            log.info("Successfully added genre: {} with media types: {}", genreName, mediaTypes);
        }, log);
    }

    @Override
    public void updateGenre(int genreId, String newGenreName, List<String> mediaTypes) {
        daoHelper.executeInTransaction(conn -> {
            updateGenreName(conn, genreId, newGenreName);
            deleteGenreApplicability(conn, genreId);
            insertGenreApplicability(conn, genreId, mediaTypes);
            log.info("Successfully updated genre ID {}", genreId);
        }, log);
    }

    @Override
    public void deleteGenre(int genreId) {
        String query = String.format("DELETE FROM %s WHERE %s = ?",
                DbConstants.Tables.GENRES, DbConstants.Columns.ID);
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, genreId);
            stmt.executeUpdate();
            log.info("Deleted genre with ID: {}", genreId);
        } catch (SQLException e) {
            log.error("Database error deleting genre ID {}", genreId, e);
            throw new DatabaseException("delete genre", e);
        }
    }

    @Override
    public boolean isGenreInUse(int genreId) {
        String queryTemplate = String.format("SELECT COUNT(*) FROM %%s WHERE %s = ?",
                DbConstants.Columns.GENRE_ID);

        try (Connection conn = DataSourceProvider.getDataSource().getConnection()) {
            for (String table : MEDIA_TABLES) {
                if (countUsages(conn, String.format(queryTemplate, table), genreId) > 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            log.error("Database error checking if genre ID {} is in use", genreId, e);
            throw new DatabaseException("check genre usage", e);
        }
        return false;
    }

    @Override
    public List<Genre> getAvailableGenresFor(String mediaType) {
        String query = String.format(
                "SELECT g.%s, g.%s, GROUP_CONCAT(ga_all.%s SEPARATOR ',') as media_types " +
                        "FROM %s g " +
                        "JOIN %s ga ON g.%s = ga.%s " +
                        "LEFT JOIN %s ga_all ON g.%s = ga_all.%s " +
                        "WHERE ga.%s = ? OR ga.%s = ? " +
                        "GROUP BY g.%s, g.%s " +
                        "ORDER BY g.%s ASC",
                DbConstants.Columns.ID, DbConstants.Columns.NAME, DbConstants.Columns.MEDIA_TYPE,
                DbConstants.Tables.GENRES,
                DbConstants.Tables.GENRE_APPLICABILITY, DbConstants.Columns.ID, DbConstants.Columns.GENRE_ID,
                DbConstants.Tables.GENRE_APPLICABILITY, DbConstants.Columns.ID, DbConstants.Columns.GENRE_ID,
                DbConstants.Columns.MEDIA_TYPE, DbConstants.Columns.MEDIA_TYPE,
                DbConstants.Columns.ID, DbConstants.Columns.NAME,
                DbConstants.Columns.NAME
        );

        return executeGenreQuery(query,
                stmt -> {
                    stmt.setString(1, mediaType);
                    stmt.setString(2, DbConstants.MediaTypes.SHARED);
                },
                "load available genres for media type");
    }

    @Override
    public List<Genre> getGenresForMovie(int movieId) {
        return getGenresForEntity(movieId, DbConstants.Tables.MOVIE_GENRES,
                DbConstants.Columns.MOVIE_ID, "Movie");
    }

    @Override
    public List<Genre> getGenresForBook(int bookId) {
        return getGenresForEntity(bookId, DbConstants.Tables.BOOK_GENRES,
                DbConstants.Columns.BOOK_ID, "Book");
    }

    @Override
    public List<Genre> getGenresForShow(int showId) {
        return getGenresForEntity(showId, DbConstants.Tables.SHOW_GENRES,
                DbConstants.Columns.SHOW_ID, "Show");
    }

    // ========== Private Helper Methods ==========

    private List<Genre> getGenresForEntity(int entityId, String joinTableName,
                                           String joinColumnName, String entityName) {
        String query = String.format(
                "SELECT g.%s, g.%s FROM %s g " +
                        "JOIN %s jg ON g.%s = jg.%s " +
                        "WHERE jg.%s = ? ORDER BY g.%s ASC",
                DbConstants.Columns.ID, DbConstants.Columns.NAME,
                DbConstants.Tables.GENRES,
                joinTableName,
                DbConstants.Columns.ID, DbConstants.Columns.GENRE_ID,
                joinColumnName,
                DbConstants.Columns.NAME
        );

        List<Genre> genres = new ArrayList<>();
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, entityId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    genres.add(new Genre(
                            rs.getInt(DbConstants.Columns.ID),
                            rs.getString(DbConstants.Columns.NAME)
                    ));
                }
            }
            log.debug("Loaded {} genres for {} ID {}", genres.size(), entityName, entityId);
        } catch (SQLException e) {
            log.error("Database error loading genres for {} ID {}", entityName, entityId, e);
            throw new DatabaseException("load genres for entity", e);
        }
        return genres;
    }

    private List<Genre> executeGenreQuery(String query, StatementSetter setter,
                                          String operationDescription) {
        List<Genre> genres = new ArrayList<>();
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            setter.setParameters(stmt);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Genre genre = new Genre(
                            rs.getInt(DbConstants.Columns.ID),
                            rs.getString(DbConstants.Columns.NAME)
                    );
                    String mediaTypesStr = rs.getString("media_types");
                    if (mediaTypesStr != null) {
                        genre.setMediaTypes(Arrays.asList(mediaTypesStr.split(",")));
                    }
                    genres.add(genre);
                }
            }
        } catch (SQLException e) {
            log.error("Database error during {}", operationDescription, e);
            throw new DatabaseException(operationDescription, e);
        }
        return genres;
    }

    private int insertGenre(Connection conn, String genreName) throws SQLException {
        String query = String.format("INSERT INTO %s (%s) VALUES (?)",
                DbConstants.Tables.GENRES, DbConstants.Columns.NAME);
        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, genreName);
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    log.error("Creating genre failed, no ID obtained");
                    throw new DatabaseException("create genre",
                            new SQLException("No generated key returned"));
                }
            }
        }
    }

    private void insertGenreApplicability(Connection conn, int genreId,
                                          List<String> mediaTypes) throws SQLException {
        if (mediaTypes == null || mediaTypes.isEmpty()) return;

        String query = String.format("INSERT INTO %s (%s, %s) VALUES (?, ?)",
                DbConstants.Tables.GENRE_APPLICABILITY,
                DbConstants.Columns.GENRE_ID, DbConstants.Columns.MEDIA_TYPE);
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (String mediaType : mediaTypes) {
                stmt.setInt(1, genreId);
                stmt.setString(2, mediaType);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void updateGenreName(Connection conn, int genreId, String newName)
            throws SQLException {
        String query = String.format("UPDATE %s SET %s = ? WHERE %s = ?",
                DbConstants.Tables.GENRES, DbConstants.Columns.NAME, DbConstants.Columns.ID);
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newName);
            stmt.setInt(2, genreId);
            stmt.executeUpdate();
        }
    }

    private void deleteGenreApplicability(Connection conn, int genreId) throws SQLException {
        String query = String.format("DELETE FROM %s WHERE %s = ?",
                DbConstants.Tables.GENRE_APPLICABILITY, DbConstants.Columns.GENRE_ID);
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, genreId);
            stmt.executeUpdate();
        }
    }

    private int countUsages(Connection conn, String query, int genreId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, genreId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @FunctionalInterface
    private interface StatementSetter {
        void setParameters(PreparedStatement stmt) throws SQLException;
    }
}