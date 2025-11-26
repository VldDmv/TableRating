package org.criticizer.dao.user;

import org.criticizer.constants.DbConstants;
import org.criticizer.dao.helper.QueryBuilder;
import org.criticizer.entity.AdminStats;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.util.DataSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for managing users in the database.
 */
public class UserDaoService implements UserDao {
    private static final Logger log = LoggerFactory.getLogger(UserDaoService.class);

    private static final String FIND_BY_NAME_QUERY = String.format(
            "SELECT %s, %s, %s, %s, %s FROM %s WHERE LOWER(%s) = LOWER(?)",
            DbConstants.Columns.ID, DbConstants.Columns.NAME, DbConstants.Columns.PASSWORD,
            DbConstants.Columns.ROLE, DbConstants.Columns.PROFILE_IS_PUBLIC,
            DbConstants.Tables.USERS,
            DbConstants.Columns.NAME
    );

    private static final String INSERT_USER_QUERY = String.format(
            "INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?)",
            DbConstants.Tables.USERS,
            DbConstants.Columns.NAME, DbConstants.Columns.PASSWORD, DbConstants.Columns.ROLE
    );

    private static final String GET_ALL_QUERY = String.format(
            "SELECT %s, %s, %s, %s, %s FROM %s ORDER BY %s ASC",
            DbConstants.Columns.ID, DbConstants.Columns.NAME, DbConstants.Columns.PASSWORD,
            DbConstants.Columns.ROLE, DbConstants.Columns.PROFILE_IS_PUBLIC,
            DbConstants.Tables.USERS,
            DbConstants.Columns.ID
    );

    private static final String UPDATE_ROLE_QUERY = String.format(
            "UPDATE %s SET %s = ? WHERE %s = ?",
            DbConstants.Tables.USERS, DbConstants.Columns.ROLE, DbConstants.Columns.ID
    );

    private static final String DELETE_USER_QUERY = String.format(
            "DELETE FROM %s WHERE %s = ?",
            DbConstants.Tables.USERS, DbConstants.Columns.ID
    );

    private static final String UPDATE_PRIVACY_QUERY = String.format(
            "UPDATE %s SET %s = ? WHERE %s = ?",
            DbConstants.Tables.USERS, DbConstants.Columns.PROFILE_IS_PUBLIC, DbConstants.Columns.ID
    );

    @Override
    public User findUserByName(String name) {
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_NAME_QUERY)) {

            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            log.error("Database error finding user by name", e);
            throw new DatabaseException("find user by name", e);
        }
        return null;
    }

    @Override
    public void addUser(User user) {
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_USER_QUERY, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getPasswordHashInternal());
            stmt.setString(3, user.getRole().name());

            log.debug("Adding user: {}", user.getName());
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                log.error("Creating user failed, no rows affected");
                throw new DatabaseException("add user", new SQLException("No rows affected"));
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    log.debug("User {} added with ID: {}", user.getName(), generatedKeys.getInt(1));
                } else {
                    log.warn("Creating user succeeded but failed to retrieve generated ID");
                }
            }

            log.info("User successfully added: {}", user.getName());

        } catch (SQLException e) {
            log.error("Database error adding user", e);
            throw new DatabaseException("add user", e);
        }
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();

        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_ALL_QUERY);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            log.debug("Fetched {} users from database", users.size());
        } catch (SQLException e) {
            log.error("Database error fetching all users", e);
            throw new DatabaseException("fetch all users", e);
        }
        return users;
    }

    @Override
    public void updateUserRole(int userId, Role newRole) {
        executeUpdate(UPDATE_ROLE_QUERY,
                stmt -> {
                    stmt.setString(1, newRole.name());
                    stmt.setInt(2, userId);
                },
                affectedRows -> {
                    if (affectedRows > 0) {
                        log.info("Successfully updated role for user ID {} to {}", userId, newRole.name());
                    } else {
                        log.warn("Could not update role for user ID {}: User not found", userId);
                    }
                },
                "update user role"
        );
    }

    @Override
    public List<User> findUsers(String searchTerm, int offset, int limit, boolean publicOnly) {
        List<User> users = new ArrayList<>();

        QueryBuilder queryBuilder = new QueryBuilder()
                .select(String.format("%s, %s, %s, %s, %s",
                        DbConstants.Columns.ID, DbConstants.Columns.NAME, DbConstants.Columns.PASSWORD,
                        DbConstants.Columns.ROLE, DbConstants.Columns.PROFILE_IS_PUBLIC))
                .from(DbConstants.Tables.USERS);

        boolean hasSearchTerm = searchTerm != null && !searchTerm.trim().isEmpty();

        if (hasSearchTerm) {
            queryBuilder.where(String.format("%s LIKE ?", DbConstants.Columns.NAME),
                    "%" + searchTerm.trim() + "%");
        }

        if (publicOnly) {
            queryBuilder.where(String.format("%s = TRUE", DbConstants.Columns.PROFILE_IS_PUBLIC));
        }

        queryBuilder.orderBy(String.format("%s ASC", DbConstants.Columns.ID)).limit(limit, offset);

        log.debug("Executing query: {}", queryBuilder.build());

        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = queryBuilder.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            log.error("Database error finding users", e);
            throw new DatabaseException("find users", e);
        }
        return users;
    }

    @Override
    public int countUsers(String searchTerm, boolean publicOnly) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .select("COUNT(*)")
                .from(DbConstants.Tables.USERS);

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            queryBuilder.where(String.format("%s LIKE ?", DbConstants.Columns.NAME),
                    "%" + searchTerm.trim() + "%");
        }

        if (publicOnly) {
            queryBuilder.where(String.format("%s = TRUE", DbConstants.Columns.PROFILE_IS_PUBLIC));
        }

        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = queryBuilder.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Database error counting users", e);
            throw new DatabaseException("count users", e);
        }
        return 0;
    }

    @Override
    public void deleteUser(int userId) {
        log.debug("Attempting to delete user with ID: {}", userId);

        executeUpdate(DELETE_USER_QUERY,
                stmt -> stmt.setInt(1, userId),
                affectedRows -> {
                    if (affectedRows > 0) {
                        log.info("Successfully deleted user with ID: {}", userId);
                    } else {
                        log.warn("Could not delete user with ID {}: User not found", userId);
                    }
                },
                "delete user"
        );
    }

    @Override
    public void updateUserPrivacy(int userId, boolean isPublic) {
        log.debug("Updating privacy for user {}: {}", userId, isPublic);

        executeUpdate(UPDATE_PRIVACY_QUERY,
                stmt -> {
                    stmt.setBoolean(1, isPublic);
                    stmt.setInt(2, userId);
                },
                affectedRows -> {
                    if (affectedRows == 0) {
                        log.warn("Failed to update privacy settings for user {}, user not found", userId);
                    } else {
                        log.info("Successfully updated privacy setting for user {} to {}", userId, isPublic);
                    }
                },
                "update user privacy"
        );
    }

    @Override
    public AdminStats getAdminStatistics() {
        AdminStats stats = new AdminStats();

        String sql =
                "SELECT 'users' as type, COUNT(*) as count FROM " + DbConstants.Tables.USERS +
                        " UNION ALL " +
                        "SELECT 'games', COUNT(*) FROM " + DbConstants.Tables.GAMES +
                        " UNION ALL " +
                        "SELECT 'movies', COUNT(*) FROM " + DbConstants.Tables.MOVIES +
                        " UNION ALL " +
                        "SELECT 'books', COUNT(*) FROM " + DbConstants.Tables.BOOKS +
                        " UNION ALL " +
                        "SELECT 'shows', COUNT(*) FROM " + DbConstants.Tables.SHOWS;

        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String type = rs.getString("type");
                int count = rs.getInt("count");

                switch (type) {
                    case "users" -> stats.setTotalUsers(count);
                    case "games" -> stats.setTotalGames(count);
                    case "movies" -> stats.setTotalMovies(count);
                    case "books" -> stats.setTotalBooks(count);
                    case "shows" -> stats.setTotalShows(count);
                }
            }

            log.debug("Admin statistics fetched: {}", stats);

        } catch (SQLException e) {
            log.error("Database error fetching admin statistics", e);
            throw new DatabaseException("fetch admin statistics", e);
        }

        return stats;
    }

    // ========== Private Helper Methods ==========

    /**
     * Maps a ResultSet row to a User object with proper error handling
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt(DbConstants.Columns.ID);
        String userName = rs.getString(DbConstants.Columns.NAME);
        String passwordHash = rs.getString(DbConstants.Columns.PASSWORD);
        String roleStr = rs.getString(DbConstants.Columns.ROLE);
        boolean profileIsPublic = rs.getBoolean(DbConstants.Columns.PROFILE_IS_PUBLIC);

        Role userRole = parseRole(roleStr, id);

        return new User(id, userName, passwordHash, userRole, profileIsPublic);
    }

    /**
     * Safely parses a role string with fallback to USER
     */
    private Role parseRole(String roleStr, int userId) {
        if (roleStr == null) {
            log.warn("NULL role value found for user id '{}'. Defaulting to USER", userId);
            return Role.USER;
        }

        try {
            return Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid role value '{}' found for user id '{}'. Defaulting to USER", roleStr, userId);
            return Role.USER;
        }
    }

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
     * Generic method to execute UPDATE/DELETE statements with consistent error handling
     */
    @FunctionalInterface
    private interface StatementSetter {
        void setParameters(PreparedStatement stmt) throws SQLException;
    }

    @FunctionalInterface
    private interface ResultHandler {
        void handle(int affectedRows);
    }
}