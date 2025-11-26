package org.criticizer.dao.helper;

import org.criticizer.constants.DbConstants;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles basic entity CRUD operations.
 */
public class MediaEntityRepository {

    private final String tableName;
    private final Logger log;

    public MediaEntityRepository(String tableName, Logger log) {
        this.tableName = tableName;
        this.log = log;
    }

    /**
     * Checks if entity with given name exists for user
     */
    public boolean exists(Connection conn, String name, int userId) throws SQLException {
        String query = String.format(
                "SELECT %s FROM %s WHERE LOWER(%s) = LOWER(?) AND %s = ?",
                DbConstants.Columns.ID,
                tableName,
                DbConstants.Columns.NAME, DbConstants.Columns.USER_ID
        );

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setInt(2, userId);
            return stmt.executeQuery().next();
        }
    }

    /**
     * Checks for duplicate entity name
     */
    public void checkDuplicate(Connection conn, String name, int userId, Integer excludeId,
                               String entityName) throws SQLException {
        String query = String.format(
                "SELECT %s FROM %s WHERE LOWER(%s) = LOWER(?) AND %s = ?" +
                        (excludeId != null ? " AND " + DbConstants.Columns.ID + " != ?" : ""),
                DbConstants.Columns.ID,
                tableName,
                DbConstants.Columns.NAME, DbConstants.Columns.USER_ID
        );

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setInt(2, userId);
            if (excludeId != null) {
                stmt.setInt(3, excludeId);
            }
            if (stmt.executeQuery().next()) {
                throw new ItemAlreadyExistsException(entityName, name);
            }
        }
    }

    /**
     * Inserts new entity and returns generated ID
     */
    public int insert(Connection conn, String name, int userId, int score, String entityName)
            throws SQLException {
        String query = String.format(
                "INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, FALSE)",
                tableName,
                DbConstants.Columns.NAME, DbConstants.Columns.USER_ID,
                DbConstants.Columns.SCORE, DbConstants.Columns.COMPLETED
        );

        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setInt(2, userId);
            stmt.setInt(3, score);
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    log.error("Creating {} failed, no ID obtained", entityName);
                    throw new DatabaseException("create " + entityName.toLowerCase(),
                            new SQLException("No generated key returned"));
                }
            }
        }
    }

    /**
     * Updates entity name, score, and completion status
     */
    public void update(Connection conn, int id, String name, int score, boolean completed)
            throws SQLException {
        String query = String.format(
                "UPDATE %s SET %s = ?, %s = ?, %s = ? WHERE %s = ?",
                tableName,
                DbConstants.Columns.NAME, DbConstants.Columns.SCORE,
                DbConstants.Columns.COMPLETED, DbConstants.Columns.ID
        );

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setInt(2, score);
            stmt.setBoolean(3, completed);
            stmt.setInt(4, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Deletes entity by ID
     */
    public void delete(Connection conn, int entityId) throws SQLException {
        String query = String.format("DELETE FROM %s WHERE %s = ?",
                tableName, DbConstants.Columns.ID);
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, entityId);
            stmt.executeUpdate();
        }
    }

    /**
     * Deletes all entities for a user
     */
    public void deleteByUserId(Connection conn, int userId) throws SQLException {
        String query = String.format("DELETE FROM %s WHERE %s = ?",
                tableName, DbConstants.Columns.USER_ID);
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Finds entity ID by name and userId
     */
    public int findEntityId(Connection conn, String name, int userId, String entityName)
            throws SQLException {
        String query = String.format(
                "SELECT %s FROM %s WHERE LOWER(%s) = LOWER(?) AND %s = ?",
                DbConstants.Columns.ID,
                tableName,
                DbConstants.Columns.NAME, DbConstants.Columns.USER_ID
        );

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(DbConstants.Columns.ID);
                } else {
                    throw new ResourceNotFoundException(entityName, name);
                }
            }
        }
    }

    /**
     * Finds entity ID and completion status
     */
    public EntityInfo findEntityInfo(Connection conn, String name, int userId, String entityName)
            throws SQLException {
        String query = String.format(
                "SELECT %s, %s FROM %s WHERE LOWER(%s) = LOWER(?) AND %s = ?",
                DbConstants.Columns.ID, DbConstants.Columns.COMPLETED,
                tableName,
                DbConstants.Columns.NAME, DbConstants.Columns.USER_ID
        );

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new EntityInfo(
                            rs.getInt(DbConstants.Columns.ID),
                            rs.getBoolean(DbConstants.Columns.COMPLETED)
                    );
                } else {
                    throw new ResourceNotFoundException(entityName, name);
                }
            }
        }
    }

    /**
     * Finds all entity IDs for a user
     */
    public List<Integer> findAllEntityIds(Connection conn, int userId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String query = String.format("SELECT %s FROM %s WHERE %s = ?",
                DbConstants.Columns.ID, tableName, DbConstants.Columns.USER_ID);

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(DbConstants.Columns.ID));
                }
            }
        }
        return ids;
    }

    /**
     * Helper class to hold entity info
     */
    public record EntityInfo(int id, boolean completed) {
    }
}