package org.criticizer.dao.tag;

import org.criticizer.constants.DbConstants;
import org.criticizer.entity.Tag;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.util.DataSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL implementation of TagDao
 * Manages tags associated with games in the database.
 */
public class TagDaoService implements TagDao {
    private static final Logger log = LoggerFactory.getLogger(TagDaoService.class);

    private static final String GET_ALL_TAGS_QUERY = String.format(
            "SELECT %s, %s FROM %s ORDER BY %s ASC",
            DbConstants.Columns.ID, DbConstants.Columns.NAME,
            DbConstants.Tables.TAGS,
            DbConstants.Columns.NAME
    );

    private static final String GET_TAGS_FOR_GAME_QUERY = String.format(
            "SELECT t.%s, t.%s FROM %s t JOIN %s gt ON t.%s = gt.%s " +
                    "WHERE gt.%s = ? ORDER BY t.%s ASC",
            DbConstants.Columns.ID, DbConstants.Columns.NAME,
            DbConstants.Tables.TAGS, DbConstants.Tables.GAME_TAGS,
            DbConstants.Columns.ID, DbConstants.Columns.TAG_ID,
            DbConstants.Columns.GAME_ID,
            DbConstants.Columns.NAME
    );

    private static final String INSERT_TAG_QUERY = String.format(
            "INSERT INTO %s (%s) VALUES (?)",
            DbConstants.Tables.TAGS, DbConstants.Columns.NAME
    );

    private static final String UPDATE_TAG_QUERY = String.format(
            "UPDATE %s SET %s = ? WHERE %s = ?",
            DbConstants.Tables.TAGS, DbConstants.Columns.NAME, DbConstants.Columns.ID
    );

    private static final String DELETE_TAG_QUERY = String.format(
            "DELETE FROM %s WHERE %s = ?",
            DbConstants.Tables.TAGS, DbConstants.Columns.ID
    );

    private static final String COUNT_TAG_USAGES_QUERY = String.format(
            "SELECT COUNT(*) FROM %s WHERE %s = ?",
            DbConstants.Tables.GAME_TAGS, DbConstants.Columns.TAG_ID
    );

    @Override
    public List<Tag> getAllTags() {
        return executeTagQuery(GET_ALL_TAGS_QUERY, stmt -> {
        }, "load all tags");
    }

    @Override
    public List<Tag> getTagsForGame(int gameId) {
        return executeTagQuery(GET_TAGS_FOR_GAME_QUERY,
                stmt -> stmt.setInt(1, gameId),
                "load tags for game");
    }

    @Override
    public void addTag(String tagName) {
        executeUpdate(INSERT_TAG_QUERY,
                stmt -> stmt.setString(1, tagName),
                affectedRows -> log.info("Added new tag: {}", tagName),
                "add tag");
    }

    @Override
    public void updateTag(int tagId, String newTagName) {
        executeUpdate(UPDATE_TAG_QUERY,
                stmt -> {
                    stmt.setString(1, newTagName);
                    stmt.setInt(2, tagId);
                },
                affectedRows -> log.info("Updated tag ID {} to new name '{}'", tagId, newTagName),
                "update tag");
    }

    @Override
    public void deleteTag(int tagId) {
        executeUpdate(DELETE_TAG_QUERY,
                stmt -> stmt.setInt(1, tagId),
                affectedRows -> log.info("Deleted tag with ID: {}", tagId),
                "delete tag");
    }

    @Override
    public boolean isTagInUse(int tagId) {
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(COUNT_TAG_USAGES_QUERY)) {
            stmt.setInt(1, tagId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            log.error("Database error checking if tag is in use", e);
            throw new DatabaseException("check tag usage", e);
        }
        return false;
    }

    // ========== Private Helper Methods ==========

    /**
     * Executes a query that returns a list of tags
     */
    private List<Tag> executeTagQuery(String query, StatementSetter setter,
                                      String operationDescription) {
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            setter.setParameters(stmt);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tags.add(new Tag(
                            rs.getInt(DbConstants.Columns.ID),
                            rs.getString(DbConstants.Columns.NAME)
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Database error during {}", operationDescription, e);
            throw new DatabaseException(operationDescription, e);
        }
        return tags;
    }

    /**
     * Executes an UPDATE or DELETE statement with consistent error handling
     */
    private void executeUpdate(String query, StatementSetter setter,
                               ResultHandler resultHandler, String operationDescription) {
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            setter.setParameters(stmt);
            int affectedRows = stmt.executeUpdate();
            resultHandler.handle(affectedRows);

        } catch (SQLException e) {
            log.error("Database error during {}", operationDescription, e);
            throw new DatabaseException(operationDescription, e);
        }
    }

    /**
     * Functional interface for setting prepared statement parameters
     */
    @FunctionalInterface
    private interface StatementSetter {
        void setParameters(PreparedStatement stmt) throws SQLException;
    }

    /**
     * Functional interface for handling query results
     */
    @FunctionalInterface
    private interface ResultHandler {
        void handle(int affectedRows);
    }
}