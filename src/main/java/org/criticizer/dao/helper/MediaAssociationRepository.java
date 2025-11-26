package org.criticizer.dao.helper;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Handles entity-association relationships (Game-Tag, Movie-Genre, etc).
 */
public class MediaAssociationRepository {

    private final String associationTableName;
    private final String mediaIdColumn;
    private final String associationIdColumn;
    private final String associationEntityTableName;

    public MediaAssociationRepository(String associationTableName, String mediaIdColumn,
                                      String associationIdColumn, String associationEntityTableName) {
        this.associationTableName = associationTableName;
        this.mediaIdColumn = mediaIdColumn;
        this.associationIdColumn = associationIdColumn;
        this.associationEntityTableName = associationEntityTableName;
    }

    public String getAssociationIdColumn() {
        return associationIdColumn;
    }

    public String getMediaIdColumn() {
        return mediaIdColumn;
    }

    public String getAssociationTableName() {
        return associationTableName;
    }

    /**
     * Inserts associations for an entity
     */
    public void insert(Connection conn, int entityId, List<Integer> associationIds)
            throws SQLException {
        if (associationIds == null || associationIds.isEmpty()) return;

        String query = String.format(
                "INSERT INTO %s (%s, %s) VALUES (?, ?)",
                associationTableName, mediaIdColumn, associationIdColumn
        );

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (Integer associationId : associationIds) {
                stmt.setInt(1, entityId);
                stmt.setInt(2, associationId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /**
     * Deletes all associations for an entity
     */
    public void delete(Connection conn, int entityId) throws SQLException {
        String query = String.format(
                "DELETE FROM %s WHERE %s = ?",
                associationTableName, mediaIdColumn
        );
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, entityId);
            stmt.executeUpdate();
        }
    }

    /**
     * Deletes associations for multiple entities in batch
     */
    public void deleteBatch(Connection conn, List<Integer> entityIds) throws SQLException {
        if (entityIds == null || entityIds.isEmpty()) return;

        String placeholders = String.join(",", Collections.nCopies(entityIds.size(), "?"));
        String query = String.format(
                "DELETE FROM %s WHERE %s IN (%s)",
                associationTableName, mediaIdColumn, placeholders
        );

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < entityIds.size(); i++) {
                stmt.setInt(i + 1, entityIds.get(i));
            }
            stmt.executeUpdate();
        }
    }

    /**
     * Loads associations for multiple entities efficiently
     */
    public <A> Map<Integer, List<A>> loadForEntities(Connection conn, List<Integer> entityIds,
                                                     AssociationMapper<A> mapper)
            throws SQLException {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = String.join(",", Collections.nCopies(entityIds.size(), "?"));
        String query = String.format(
                "SELECT a.%s, assoc.* FROM %s a " +
                        "JOIN %s assoc ON a.%s = assoc.id WHERE a.%s IN (%s)",
                mediaIdColumn, associationTableName,
                associationEntityTableName, associationIdColumn, mediaIdColumn, placeholders
        );

        Map<Integer, List<A>> result = new HashMap<>();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < entityIds.size(); i++) {
                stmt.setInt(i + 1, entityIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int entityId = rs.getInt(mediaIdColumn);
                    A association = mapper.map(rs);
                    result.computeIfAbsent(entityId, k -> new ArrayList<>()).add(association);
                }
            }
        }
        return result;
    }

    /**
     * Functional interface for mapping ResultSet to association entity
     */
    @FunctionalInterface
    public interface AssociationMapper<A> {
        A map(ResultSet rs) throws SQLException;
    }
}
