package org.criticizer.dao.helper;

import org.criticizer.constants.DbConstants;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.util.DataSourceProvider;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Abstract base class for media-related DAOs (Game, Book, Movie, Show).
 *
 * @param <T> The entity type (Game, Book, Movie, Show)
 * @param <A> The association type (Tag or Genre)
 */
public abstract class AbstractMediaDao<T, A> {

    protected final Logger log;
    private final String tableName;
    private final EntityValidator validator;
    private final MediaEntityRepository entityRepo;
    private final MediaAssociationRepository associationRepo;
    private final DaoHelperService daoHelper;
    private static final int ASSOCIATION_BATCH_SIZE = 500;

    protected AbstractMediaDao(Logger log, EntityValidator validator, DaoHelperService daoHelper,
                               String tableName, String associationTableName, String mediaIdColumn,
                               String associationIdColumn, String associationEntityTableName) {
        this.log = log;
        this.tableName = tableName;
        this.validator = validator;
        this.daoHelper = daoHelper;
        this.entityRepo = new MediaEntityRepository(tableName, log);
        this.associationRepo = new MediaAssociationRepository(
                associationTableName, mediaIdColumn, associationIdColumn,
                associationEntityTableName
        );
    }

    // Template methods - implemented by subclasses
    protected abstract T mapResultSetToEntity(ResultSet rs) throws SQLException;
    protected abstract A mapResultSetToAssociation(ResultSet rs) throws SQLException;
    protected abstract void setAssociations(T entity, List<A> associations);
    protected abstract int getEntityId(T entity);
    protected abstract String getEntityName();


    /**
     * Retrieves all entities for a user with their associations
     */
    public List<T> getUserEntities(int userId) {
        List<T> entities = new ArrayList<>();
        String query = String.format(
                "SELECT %s, %s, %s, %s, %s FROM %s WHERE %s = ?",
                DbConstants.Columns.ID, DbConstants.Columns.NAME, DbConstants.Columns.USER_ID,
                DbConstants.Columns.SCORE, DbConstants.Columns.COMPLETED,
                tableName,
                DbConstants.Columns.USER_ID
        );

        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entities.add(mapResultSetToEntity(rs));
                }
            }

            if (!entities.isEmpty()) {
                loadAssociationsForEntities(conn, entities);
            }

        } catch (SQLException e) {
            log.error("Database error loading entities for user {}", userId, e);
            throw new DatabaseException("load user entities", e);
        }
        return entities;
    }

    /**
     * Adds a new entity with associations
     */
    public int addEntity(String name, int userId, int score, List<Integer> associationIds) {
        final String trimmedName = validator.validateAndTrimName(name, getEntityName(), "Add");
        validator.validateScore(score, userId, getEntityName());

        final int[] newEntityId = new int[1];
        daoHelper.executeInTransaction(conn -> {
            entityRepo.checkDuplicate(conn, trimmedName, userId, null, getEntityName());
            newEntityId[0] = entityRepo.insert(conn, trimmedName, userId, score, getEntityName());
            associationRepo.insert(conn, newEntityId[0], associationIds);
        }, log);

        return newEntityId[0];
    }

    /**
     * Removes an entity and its associations
     */
    public void removeEntity(String name, int userId) {
        final String trimmedName = validator.validateAndTrimName(name, getEntityName(), "Remove");

        daoHelper.executeInTransaction(conn -> {
            int entityId = entityRepo.findEntityId(conn, trimmedName, userId, getEntityName());
            associationRepo.delete(conn, entityId);
            entityRepo.delete(conn, entityId);
        }, log);
    }

    /**
     * Checks if entity exists
     */
    public boolean isEntityExists(String name, int userId) {
        final String trimmedName = validator.validateAndTrimName(name, getEntityName(), "Check");
        try (Connection conn = DataSourceProvider.getDataSource().getConnection()) {
            return entityRepo.exists(conn, trimmedName, userId);
        } catch (SQLException e) {
            log.error("Database error checking entity existence", e);
            throw new DatabaseException("check entity existence", e);
        }
    }

    /**
     * Updates entity name, score, and associations
     */
    public void updateEntityAndName(String oldName, String newName, int newScore,
                                    int userId, List<Integer> associationIds) {
        final String tOldName = validator.validateAndTrimName(oldName, getEntityName(), "Update-Old");
        final String tNewName = validator.validateAndTrimName(newName, getEntityName(), "Update-New");
        validator.validateScore(newScore, userId, getEntityName());

        daoHelper.executeInTransaction(conn -> {
            MediaEntityRepository.EntityInfo info =
                    entityRepo.findEntityInfo(conn, tOldName, userId, getEntityName());

            if (!tOldName.equalsIgnoreCase(tNewName)) {
                entityRepo.checkDuplicate(conn, tNewName, userId, info.id(), getEntityName());
            }

            entityRepo.update(conn, info.id(), tNewName, newScore, info.completed());
            associationRepo.delete(conn, info.id());
            associationRepo.insert(conn, info.id(), associationIds);
        }, log);
    }

    /**
     * Gets completion status of entity
     */
    public boolean getEntityStatus(String name, int userId) {
        final String trimmedName = validator.validateAndTrimName(name, getEntityName(), "GetStatus");
        String query = String.format(
                "SELECT %s FROM %s WHERE LOWER(%s) = LOWER(?) AND %s = ?",
                DbConstants.Columns.COMPLETED,
                tableName,
                DbConstants.Columns.NAME, DbConstants.Columns.USER_ID
        );

        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, trimmedName);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(DbConstants.Columns.COMPLETED);
                } else {
                    throw new ResourceNotFoundException(getEntityName(), trimmedName);
                }
            }
        } catch (SQLException e) {
            log.error("Database error retrieving entity status", e);
            throw new DatabaseException("retrieve entity status", e);
        }
    }

    /**
     * Toggles completion status
     */
    public void toggleEntityStatus(String name, int userId) {
        final String trimmedName = validator.validateAndTrimName(name, getEntityName(), "Toggle");
        String query = String.format(
                "UPDATE %s SET %s = NOT %s WHERE LOWER(%s) = LOWER(?) AND %s = ?",
                tableName,
                DbConstants.Columns.COMPLETED, DbConstants.Columns.COMPLETED,
                DbConstants.Columns.NAME, DbConstants.Columns.USER_ID
        );

        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, trimmedName);
            stmt.setInt(2, userId);
            if (stmt.executeUpdate() == 0) {
                throw new ResourceNotFoundException(getEntityName(), trimmedName);
            }
        } catch (SQLException e) {
            log.error("Database error toggling entity status", e);
            throw new DatabaseException("toggle entity status", e);
        }
    }

    /**
     * Deletes all entities for a user
     */
    public void deleteEntitiesByUserId(int userId) {
        daoHelper.executeInTransaction(conn -> {
            List<Integer> idsToDelete = entityRepo.findAllEntityIds(conn, userId);
            if (!idsToDelete.isEmpty()) {
                associationRepo.deleteBatch(conn, idsToDelete);
                entityRepo.deleteByUserId(conn, userId);
            }
        }, log);
    }

    /**
     * Count total entities
     */
    public int countTotal() {
        String query = String.format("SELECT COUNT(*) FROM %s", tableName);
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Database error counting entities", e);
            throw new DatabaseException("count entities", e);
        }
        return 0;
    }

    /**
     * Find entities with filtering and pagination
     */
    public List<T> findEntities(int userId, Integer associationId, String searchTerm,
                                int offset, int limit, String sortBy, String sortOrder) {
        List<T> entities = new ArrayList<>();

        QueryBuilder queryBuilder = new QueryBuilder()
                .select("DISTINCT e.*")
                .from(tableName + " e");

        if (associationId != null && associationId > 0) {
            queryBuilder.join(
                    associationRepo.getAssociationTableName() + " a",
                    "e." + DbConstants.Columns.ID + " = a." + associationRepo.getMediaIdColumn() +
                            " AND a." + associationRepo.getAssociationIdColumn() + " = ?",
                    associationId
            );
        }

        queryBuilder.where("e." + DbConstants.Columns.USER_ID + " = ?", userId);

        if (searchTerm != null && !searchTerm.isEmpty()) {
            String escapedSearch = escapeForLike(searchTerm.toLowerCase());
            queryBuilder.where("LOWER(e." + DbConstants.Columns.NAME + ") LIKE ?",
                    "%" + escapedSearch + "%");
        }

        String safeSortBy = validateSortColumn(sortBy);
        String safeSortOrder = DbConstants.QueryDefaults.DEFAULT_SORT_ORDER.equalsIgnoreCase(sortOrder) ?
                "ASC" : "DESC";
        queryBuilder.orderBy("e." + safeSortBy + " " + safeSortOrder).limit(limit, offset);

        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = queryBuilder.prepareStatement(conn)) {

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entities.add(mapResultSetToEntity(rs));
                }
            }

            if (!entities.isEmpty()) {
                loadAssociationsForEntities(conn, entities);
            }
        } catch (SQLException e) {
            log.error("Database error fetching paginated entities for user {}", userId, e);
            throw new DatabaseException("fetch paginated entities", e);
        }
        return entities;
    }


    /**
     * Count entities with filtering
     */
    public int countTotalForUser(int userId, Integer associationId, String searchTerm) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .select("COUNT(DISTINCT e." + DbConstants.Columns.ID + ")")
                .from(tableName + " e");

        if (associationId != null && associationId > 0) {
            queryBuilder.join(
                    associationRepo.getAssociationTableName() + " a",
                    "e." + DbConstants.Columns.ID + " = a." + associationRepo.getMediaIdColumn() +
                            " AND a." + associationRepo.getAssociationIdColumn() + " = ?",
                    associationId
            );
        }

        queryBuilder.where("e." + DbConstants.Columns.USER_ID + " = ?", userId);

        if (searchTerm != null && !searchTerm.isEmpty()) {
            String escapedSearch = escapeForLike(searchTerm.toLowerCase());
            queryBuilder.where("LOWER(e." + DbConstants.Columns.NAME + ") LIKE ?",
                    "%" + escapedSearch + "%");
        }

        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = queryBuilder.prepareStatement(conn)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Database error counting entities for user {}", userId, e);
            throw new DatabaseException("count user entities", e);
        }
        return 0;
    }

    // ==================== Private Helper Methods ====================
    private void loadAssociationsForEntities(Connection conn, List<T> entities)
            throws SQLException {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        List<Integer> entityIds = entities.stream()
                .map(this::getEntityId)
                .toList();

        Map<Integer, List<A>> associationMap = new HashMap<>();

        if (entityIds.size() <= ASSOCIATION_BATCH_SIZE) {

            associationMap = associationRepo.loadForEntities(
                    conn, entityIds, this::mapResultSetToAssociation
            );
        } else {
            log.debug("Loading associations for {} entities in batches of {}",
                    entityIds.size(), ASSOCIATION_BATCH_SIZE);

            for (int i = 0; i < entityIds.size(); i += ASSOCIATION_BATCH_SIZE) {
                int end = Math.min(i + ASSOCIATION_BATCH_SIZE, entityIds.size());
                List<Integer> batch = entityIds.subList(i, end);

                Map<Integer, List<A>> batchResult = associationRepo.loadForEntities(
                        conn, batch, this::mapResultSetToAssociation
                );

                associationMap.putAll(batchResult);
            }
        }

        for (T entity : entities) {
            setAssociations(entity,
                    associationMap.getOrDefault(getEntityId(entity), Collections.emptyList()));
        }
    }

    private String validateSortColumn(String sortBy) {
        if (sortBy == null) {
            return DbConstants.Columns.NAME;
        }
        if (DbConstants.Columns.NAME.equalsIgnoreCase(sortBy)) {
            return DbConstants.Columns.NAME;
        }
        if (DbConstants.Columns.SCORE.equalsIgnoreCase(sortBy)) {
            return DbConstants.Columns.SCORE;
        }
        if (DbConstants.Columns.COMPLETED.equalsIgnoreCase(sortBy)) {
            return DbConstants.Columns.COMPLETED;
        }
        log.debug("Unknown sort column '{}', using default '{}'",
                sortBy, DbConstants.Columns.NAME);
        return DbConstants.Columns.NAME;
    }
    private String escapeForLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}