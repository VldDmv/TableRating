package org.criticizer.service.user;

import org.criticizer.constants.DbConstants;
import org.criticizer.dao.helper.DaoHelperService;
import org.criticizer.dao.user.UserDao;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.UserAlreadyExistsException;
import org.criticizer.exceptions.data.UserNotFoundException;
import org.criticizer.exceptions.security.InsufficientPermissionsException;
import org.criticizer.exceptions.security.InvalidCredentialsException;
import org.criticizer.exceptions.security.OperationNotPermittedException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.service.helper.MediaDelService;
import org.criticizer.service.helper.ServiceValidator;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Implementation of the interface for managing user-related operations.
 */
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final User DUMMY_USER = createDummyUser();
    private final UserDao userDao;
    private final MediaDelService mediaDeletionService;
    private final ServiceValidator validator;
    private final DaoHelperService daoHelper;

    public UserServiceImpl(UserDao userDao, MediaDelService mediaDeletionService, ServiceValidator validator, DaoHelperService daoHelper) {
        this.userDao = userDao;
        this.mediaDeletionService = mediaDeletionService;
        this.validator = validator;
        this.daoHelper = daoHelper;
    }

    //Retrieves the ID of a user by their name
    @Override
    public int getUserId(String name) {
        User user = userDao.findUserByName(name);
        if (user == null) {
            log.warn("User not found by name '{}' in getUserId", name);

            throw new UserNotFoundException(name);
        }
        return user.getId();
    }

    //Retrieves a user by their name
    @Override
    public User getUser(String name) {
        User user = userDao.findUserByName(name);
        if (user == null) {
            log.warn("User not found by name '{}' in getUser", name);

            throw new UserNotFoundException(name);
        }
        return user;
    }

    /**
     * Authenticates a user.
     */
    @Override
    public User authenticate(String name, String password) {
        User user = userDao.findUserByName(name);

        User userToCheck = (user != null) ? user : DUMMY_USER;

        boolean passwordValid = userToCheck.checkPassword(password);

        boolean isDummy = (user == null);

        if (!passwordValid || isDummy) {
            log.warn("Failed authentication attempt for user '{}'", name);
            throw new InvalidCredentialsException("Invalid username or password");
        }

        log.info("User '{}' authenticated successfully", name);
        return user;
    }

    //Registers a new user with the specified name and password
    @Override
    public void registerUser(String name, String password) {

        String trimmedName = validator.validateUsername(name);
        validator.validatePassword(password);

        // checking if the same user exists
        if (userDao.findUserByName(trimmedName) != null) {
            log.warn("Registration failed for user '{}': user already exists", trimmedName);

            throw new UserAlreadyExistsException(trimmedName);
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(trimmedName, hashedPassword);
        userDao.addUser(user);
    }


    //Retrieves all users from the database
    @Override
    public List<User> listAllUsers() {
        log.debug("Listing all users.");
        return userDao.getAllUsers();
    }

    //Changes the role of a target user, restricted to administrators
    @Override
    public void changeUserRole(int targetUserId, Role newRole, User initiator) {

        if (newRole == null) {
            throw new InvalidInputException("newRole", "cannot be null");
        }

        daoHelper.executeInTransaction(conn -> {
            User currentInitiator = lockAndFetchUser(conn, initiator.getId());

            if (currentInitiator == null || currentInitiator.getRole() != Role.ADMIN) {
                log.warn("User {} does not have admin privileges or doesn't exist",
                        initiator.getId());
                throw new InsufficientPermissionsException("ADMIN");
            }

            if (currentInitiator.getId() == targetUserId && newRole == Role.USER) {
                throw new OperationNotPermittedException(
                        "changeUserRole",
                        "Administrator cannot remove their own admin role"
                );
            }

            updateUserRoleInternal(conn, targetUserId, newRole);

            log.info("Admin '{}' changed role for user {} to {}",
                    currentInitiator.getName(), targetUserId, newRole);

        }, log);
    }

    //Retrieves a paginated list of users, optionally filtered by search term and visibility
    @Override
    public UserPageResult<User> getUsersPage(String searchTerm, int page, int pageSize, boolean publicOnly) {
        ServiceValidator.PaginationParams params = validator.validatePagination(page, pageSize);
        String sanitizedSearch = validator.sanitizeSearchTerm(searchTerm);

        List<User> usersOnPage = userDao.findUsers(sanitizedSearch, params.offset(), params.pageSize(), publicOnly);
        int totalUserCount = userDao.countUsers(sanitizedSearch, publicOnly);

        return new UserPageResult<>(usersOnPage, totalUserCount, params.page(), params.pageSize());
    }

    //Deletes a user and their associated data, restricted to administrators
    @Override
    public void deleteUser(int targetUserId, User initiator) {
        if (initiator == null || initiator.getRole() != Role.ADMIN) {
            throw new InsufficientPermissionsException("ADMIN");
        }
        if (initiator.getId() == targetUserId) {
            throw new OperationNotPermittedException("deleteUser",
                    "You cannot delete your own account.");
        }

        log.info("Admin '{}' initiating deletion of user ID {}",
                initiator.getName(), targetUserId);

        daoHelper.executeInTransaction(conn -> {
            mediaDeletionService.deleteAllMediaForUser(targetUserId);
            userDao.deleteUser(targetUserId);
        }, log);

        log.info("Successfully completed deletion process for user ID {}", targetUserId);
    }

    public void updateUserPrivacy(int userId, boolean isPublic) {
        userDao.updateUserPrivacy(userId, isPublic);
    }

    private static User createDummyUser() {
        try {
            String dummyHash = BCrypt.hashpw("dummy_password_for_timing", BCrypt.gensalt());
            return new User(0, "dummy", dummyHash, Role.USER, false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create dummy user", e);
        }
    }

    private User lockAndFetchUser(Connection conn, int userId) throws SQLException {
        String query = String.format(
                "SELECT %s, %s, %s, %s, %s FROM %s WHERE %s = ? FOR UPDATE",
                DbConstants.Columns.ID, DbConstants.Columns.NAME, DbConstants.Columns.PASSWORD,
                DbConstants.Columns.ROLE, DbConstants.Columns.PROFILE_IS_PUBLIC,
                DbConstants.Tables.USERS,
                DbConstants.Columns.ID
        );

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt(DbConstants.Columns.ID),
                            rs.getString(DbConstants.Columns.NAME),
                            rs.getString(DbConstants.Columns.PASSWORD),
                            Role.valueOf(rs.getString(DbConstants.Columns.ROLE)),
                            rs.getBoolean(DbConstants.Columns.PROFILE_IS_PUBLIC)
                    );
                }
            }
        }
        return null;
    }

    private void updateUserRoleInternal(Connection conn, int userId, Role newRole)
            throws SQLException {
        String query = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ?",
                DbConstants.Tables.USERS, DbConstants.Columns.ROLE, DbConstants.Columns.ID
        );

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newRole.name());
            stmt.setInt(2, userId);
            int rows = stmt.executeUpdate();

            if (rows == 0) {
                log.warn("Failed to update role for user {}: not found", userId);
            }
        }
    }
}