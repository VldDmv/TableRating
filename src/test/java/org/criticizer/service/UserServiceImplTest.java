package org.criticizer.service;

import com.zaxxer.hikari.HikariDataSource;
import org.criticizer.constants.DbConstants;
import org.criticizer.dao.user.UserDao;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.exceptions.data.UserAlreadyExistsException;
import org.criticizer.exceptions.data.UserNotFoundException;
import org.criticizer.exceptions.security.InsufficientPermissionsException;
import org.criticizer.exceptions.security.InvalidCredentialsException;
import org.criticizer.exceptions.security.OperationNotPermittedException;
import org.criticizer.service.helper.MediaDelService;
import org.criticizer.service.helper.ServiceValidator;
import org.criticizer.service.user.UserPageResult;
import org.criticizer.service.user.UserServiceImpl;
import org.criticizer.util.DataSourceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserDao userDaoMock;

    @Mock
    private MediaDelService mediaDelServiceMock;

    @Mock
    private ServiceValidator validatorMock;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;
    @Mock
    private HikariDataSource mockHikariDataSource;

    @Mock
    private Connection mockConnection;

    @Mock
    private org.criticizer.dao.helper.DaoHelperService daoHelperMock;

    @InjectMocks
    private UserServiceImpl userService;


    @BeforeEach
    void setUpDataSource() throws SQLException {
        DataSourceProvider.initialize(mockHikariDataSource);
        Mockito.lenient().when(mockHikariDataSource.getConnection()).thenReturn(mockConnection);
        Mockito.lenient().doNothing().when(mockConnection).setAutoCommit(anyBoolean());
        Mockito.lenient().doNothing().when(mockConnection).commit();
        Mockito.lenient().doNothing().when(mockConnection).rollback();
        Mockito.lenient().doNothing().when(mockConnection).close();
        Mockito.lenient().when(mockConnection.prepareStatement(anyString()))
                .thenReturn(mockPreparedStatement);
        Mockito.lenient().doNothing().when(mockPreparedStatement).setInt(anyInt(), anyInt());
        Mockito.lenient().doNothing().when(mockPreparedStatement).setString(anyInt(), anyString());
        Mockito.lenient().when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        Mockito.lenient().when(mockPreparedStatement.executeUpdate()).thenReturn(1);
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    @DisplayName("successful registration of a new user")
    void registerUser_whenNewUser_shouldAddUserWithDefaultRole() {
        String name = "testuser";
        String password = "password123";

        when(validatorMock.validateUsername(name)).thenReturn(name);
        doNothing().when(validatorMock).validatePassword(password);
        when(userDaoMock.findUserByName(name)).thenReturn(null);

        userService.registerUser(name, password);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDaoMock, times(1)).addUser(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser, "The User object must not be null");
        assertEquals(name, capturedUser.getName(), "Username must match");
        assertNotNull(capturedUser.getPasswordHashInternal(), "Password (hash) must not be null");
        assertTrue(capturedUser.getPasswordHashInternal().startsWith("$2a$"), "Password must be a bcrypt hash");
        assertEquals(Role.USER, capturedUser.getRole(), "The default role should be USER");
    }

    @Test
    @DisplayName("registering a user with an existing name should throw UserAlreadyExistsException")
    void registerUser_whenUserExists_shouldThrowUserAlreadyExistsException() {
        String existingName = "existingUser";
        String password = "password123";
        User existingUser = new User(existingName, "somehash");

        when(validatorMock.validateUsername(existingName)).thenReturn(existingName);
        doNothing().when(validatorMock).validatePassword(password);
        when(userDaoMock.findUserByName(existingName)).thenReturn(existingUser);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class,
                () -> userService.registerUser(existingName, password));

        assertTrue(exception.getUserMessage().contains("Username already exists"));
        verify(userDaoMock, never()).addUser(any(User.class));
    }

    @Test
    @DisplayName("registering with an empty name should throw an exception")
    void registerUser_whenNameIsEmpty_shouldThrowIllegalArgumentException() {
        String name = "";
        String password = "password123";

        when(validatorMock.validateUsername(name)).thenThrow(new IllegalArgumentException("Username cannot be empty"));

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(name, password));

        verify(userDaoMock, never()).findUserByName(anyString());
        verify(userDaoMock, never()).addUser(any(User.class));
    }

    @Test
    @DisplayName("registering with a short password should throw an exception")
    void registerUser_whenPasswordIsShort_shouldThrowIllegalArgumentException() {
        String name = "testuser";
        String password = "123";

        when(validatorMock.validateUsername(name)).thenReturn(name);
        doThrow(new IllegalArgumentException("Password must be at least 6 characters"))
                .when(validatorMock).validatePassword(password);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(name, password));

        assertEquals("Password must be at least 6 characters", exception.getMessage());
        verify(userDaoMock, never()).addUser(any(User.class));
        verify(userDaoMock, never()).findUserByName(anyString());
    }

    @Test
    @DisplayName("registering with whitespace-only name should throw an exception")
    void registerUser_whenNameIsWhitespaceOnly_shouldThrowIllegalArgumentException() {
        String name = "   ";
        String password = "password123";

        when(validatorMock.validateUsername(name))
                .thenThrow(new IllegalArgumentException("Name cannot be empty"));

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(name, password));

        verify(userDaoMock, never()).addUser(any(User.class));
    }

    @Test
    @DisplayName("registering with null password should throw an exception")
    void registerUser_whenPasswordIsNull_shouldThrowIllegalArgumentException() {
        String name = "testuser";

        when(validatorMock.validateUsername(name)).thenReturn(name);
        doThrow(new IllegalArgumentException("Password must be at least 6 characters"))
                .when(validatorMock).validatePassword(null);

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(name, null));

        verify(userDaoMock, never()).addUser(any(User.class));
    }

    // ==================== GET USER ID TESTS ====================

    @Test
    @DisplayName("getUserId should return an ID when the user is found")
    void getUserId_whenUserFound_shouldReturnUserId() {
        String testUserName = "existentUser";
        int expectedUserId = 1;

        User mockedUser = new User(expectedUserId, testUserName, "hashedPassword", Role.USER, false);
        when(userDaoMock.findUserByName(testUserName)).thenReturn(mockedUser);

        int actualUserId = userService.getUserId(testUserName);

        assertEquals(expectedUserId, actualUserId, "The returned ID must match the expected one");
        verify(userDaoMock, times(1)).findUserByName(testUserName);
    }

    @Test
    @DisplayName("getUserId should throw UserNotFoundException when user is not found")
    void getUserId_whenUserNotFound_shouldThrowUserNotFoundException() {
        String testUserName = "nonExistentUser";
        when(userDaoMock.findUserByName(testUserName)).thenReturn(null);

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.getUserId(testUserName));

        String expectedMessage = "User not found";
        String actualMessage = exception.getUserMessage();
        assertTrue(actualMessage.contains(expectedMessage));
        verify(userDaoMock, times(1)).findUserByName(testUserName);
    }

    // ==================== GET USER TESTS ====================

    @Test
    @DisplayName("getUser should return a User object when a user is found")
    void getUser_whenUserFound_shouldReturnUserObject() {
        String testUserName = "existentUser";
        int userId = 1;

        User expectedUser = new User(userId, testUserName, "hashedPassword", Role.USER, false);
        when(userDaoMock.findUserByName(testUserName)).thenReturn(expectedUser);

        User actualUser = userService.getUser(testUserName);

        assertNotNull(actualUser, "The returned User object must not be null");
        assertSame(expectedUser, actualUser, "The same instance of the User object must be returned");
        verify(userDaoMock, times(1)).findUserByName(testUserName);
    }

    @Test
    @DisplayName("getUser should throw UserNotFoundException when user is not found")
    void getUser_whenUserNotFound_shouldThrowUserNotFoundException() {
        String testUserName = "nonExistentUser";
        when(userDaoMock.findUserByName(testUserName)).thenReturn(null);

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.getUser(testUserName));

        String expectedMessage = "User not found";
        String actualMessage = exception.getUserMessage();
        assertTrue(actualMessage.contains(expectedMessage));
        verify(userDaoMock, times(1)).findUserByName(testUserName);
    }

    // ==================== AUTHENTICATION TESTS ====================

    @Test
    @DisplayName(" authenticate should succeed with correct credentials")
    void authenticate_withCorrectCredentials_shouldReturnUser() {
        String username = "testuser";
        String password = "password123";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        User mockUser = new User(1, username, hashedPassword, Role.USER, false);
        when(userDaoMock.findUserByName(username)).thenReturn(mockUser);

        User result = userService.authenticate(username, password);

        assertNotNull(result);
        assertEquals(username, result.getName());
        verify(userDaoMock, times(1)).findUserByName(username);
    }

    @Test
    @DisplayName(" authenticate should fail with incorrect password")
    void authenticate_withIncorrectPassword_shouldThrowInvalidCredentialsException() {
        String username = "testuser";
        String correctPassword = "password123";
        String wrongPassword = "wrongpassword";
        String hashedPassword = BCrypt.hashpw(correctPassword, BCrypt.gensalt());

        User mockUser = new User(1, username, hashedPassword, Role.USER, false);
        when(userDaoMock.findUserByName(username)).thenReturn(mockUser);

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> userService.authenticate(username, wrongPassword));

        assertTrue(exception.getUserMessage().contains("Invalid username or password"));
        verify(userDaoMock, times(1)).findUserByName(username);
    }

    @Test
    @DisplayName(" authenticate should fail with non-existent user")
    void authenticate_withNonExistentUser_shouldThrowInvalidCredentialsException() {
        String username = "nonexistent";
        String password = "password123";

        when(userDaoMock.findUserByName(username)).thenReturn(null);

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> userService.authenticate(username, password));

        assertTrue(exception.getUserMessage().contains("Invalid username or password"));
        verify(userDaoMock, times(1)).findUserByName(username);
    }

    // ==================== PASSWORD VALIDATION TESTS ====================

    @Test
    @DisplayName("user.checkPassword should return true for correct password")
    void userCheckPassword_whenPasswordIsCorrect_shouldReturnTrue() {
        String plainPassword = "password123";
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        User user = new User(1, "testuser", hashedPassword, Role.USER, false);

        assertTrue(user.checkPassword(plainPassword),
                "User.checkPassword should return true for correct password");
    }

    @Test
    @DisplayName("user.checkPassword should return false for incorrect password")
    void userCheckPassword_whenPasswordIsIncorrect_shouldReturnFalse() {
        String correctPassword = "password123";
        String incorrectPassword = "wrongpassword";
        String hashedPassword = BCrypt.hashpw(correctPassword, BCrypt.gensalt());
        User user = new User(1, "testuser", hashedPassword, Role.USER, false);

        assertFalse(user.checkPassword(incorrectPassword),
                "User.checkPassword should return false for incorrect password");
    }

    @Test
    @DisplayName("user.checkPassword should return false when password is null")
    void userCheckPassword_whenPasswordIsNull_shouldReturnFalse() {
        String hashedPassword = BCrypt.hashpw("password123", BCrypt.gensalt());
        User user = new User(1, "testuser", hashedPassword, Role.USER, false);

        assertFalse(user.checkPassword(null),
                "User.checkPassword should return false when password is null");
    }

    @Test
    @DisplayName("user.checkPassword should return false for empty password")
    void userCheckPassword_whenPasswordIsEmpty_shouldReturnFalse() {
        String hashedPassword = BCrypt.hashpw("password123", BCrypt.gensalt());
        User user = new User(1, "testuser", hashedPassword, Role.USER, false);

        assertFalse(user.checkPassword(""),
                "User.checkPassword should return false for empty password");
    }

    @Test
    @DisplayName("user.checkPassword should handle exactly 6 character passwords")
    void userCheckPassword_whenPasswordIsExactly6Characters_shouldReturnTrue() {
        String password = "pass12";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(1, "testuser", hashedPassword, Role.USER, false);

        assertTrue(user.checkPassword(password),
                "User.checkPassword should work with 6 character passwords");
    }

    // ==================== LIST ALL USERS TESTS ====================

    @Test
    @DisplayName("listAllUsers should return a list of users when users exist")
    void listAllUsers_whenUsersExist_shouldReturnUserList() {
        User user1 = new User(1, "userOne", "pass1", Role.USER, false);
        User user2 = new User(2, "userTwo", "pass2", Role.ADMIN, false);
        List<User> expectedUsers = Arrays.asList(user1, user2);

        when(userDaoMock.getAllUsers()).thenReturn(expectedUsers);

        List<User> actualUsers = userService.listAllUsers();

        assertNotNull(actualUsers, "The returned list of users should not be null.");
        assertSame(expectedUsers, actualUsers, "The service should return the exact list instance from the DAO.");
        verify(userDaoMock, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("listAllUsers should return an empty list when no users exist")
    void listAllUsers_whenNoUsersExist_shouldReturnEmptyList() {
        List<User> emptyUserList = Collections.emptyList();
        when(userDaoMock.getAllUsers()).thenReturn(emptyUserList);

        List<User> actualUsers = userService.listAllUsers();

        assertNotNull(actualUsers, "The returned list of users should not be null, even if empty.");
        assertTrue(actualUsers.isEmpty(), "The returned list should be empty when no users exist.");
        assertSame(emptyUserList, actualUsers, "The service should return the exact empty list instance from the DAO.");
        verify(userDaoMock, times(1)).getAllUsers();
    }

    // ==================== CHANGE USER ROLE TESTS ====================

    @Test
    @DisplayName("changeUserRole should succeed when admin changes another user's role")
    void changeUserRole_whenAdminChangesAnotherUserRole_shouldSucceed() throws SQLException {
        int initiatorId = 1;
        User adminInitiator = new User(initiatorId, "adminUser", "adminPass", Role.ADMIN, false);
        int targetUserId = 2;
        Role newRole = Role.USER;

        doAnswer(invocation -> {
            org.criticizer.dao.helper.DaoHelperService.TransactionOperation operation =
                    invocation.getArgument(0);
            operation.execute(mockConnection);
            return null;
        }).when(daoHelperMock).executeInTransaction(any(), any());

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(DbConstants.Columns.ID)).thenReturn(initiatorId);
        when(mockResultSet.getString(DbConstants.Columns.NAME)).thenReturn("adminUser");
        when(mockResultSet.getString(DbConstants.Columns.PASSWORD)).thenReturn("adminPass");
        when(mockResultSet.getString(DbConstants.Columns.ROLE)).thenReturn("ADMIN");
        when(mockResultSet.getBoolean(DbConstants.Columns.PROFILE_IS_PUBLIC)).thenReturn(false);

        assertDoesNotThrow(() -> userService.changeUserRole(targetUserId, newRole, adminInitiator));

        verify(mockConnection, atLeastOnce()).prepareStatement(anyString());
        verify(mockPreparedStatement, atLeastOnce()).executeUpdate();
    }


    @Test
    @DisplayName("changeUserRole should throw InsufficientPermissionsException when initiator is not admin")
    void changeUserRole_whenInitiatorIsNotAdmin_shouldThrowInsufficientPermissionsException() throws SQLException {
        int initiatorId = 1;
        User nonAdminInitiator = new User(initiatorId, "normalUser", "userPass", Role.USER, false);
        int targetUserId = 2;
        Role newRole = Role.ADMIN;

        doAnswer(invocation -> {
            org.criticizer.dao.helper.DaoHelperService.TransactionOperation operation =
                    invocation.getArgument(0);
            operation.execute(mockConnection);
            return null;
        }).when(daoHelperMock).executeInTransaction(any(), any());

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(DbConstants.Columns.ID)).thenReturn(initiatorId);
        when(mockResultSet.getString(DbConstants.Columns.NAME)).thenReturn("normalUser");
        when(mockResultSet.getString(DbConstants.Columns.PASSWORD)).thenReturn("userPass");
        when(mockResultSet.getString(DbConstants.Columns.ROLE)).thenReturn("USER");
        when(mockResultSet.getBoolean(DbConstants.Columns.PROFILE_IS_PUBLIC)).thenReturn(false);

        InsufficientPermissionsException exception = assertThrows(
                InsufficientPermissionsException.class,
                () -> userService.changeUserRole(targetUserId, newRole, nonAdminInitiator)
        );

        assertEquals("You don't have permission to perform this action", exception.getUserMessage());

        verify(daoHelperMock, times(1)).executeInTransaction(any(), any());
    }

    @Test
    @DisplayName("changeUserRole should throw OperationNotPermittedException when admin tries to demote self to USER")
    void changeUserRole_whenAdminDemotesSelfToUser_shouldThrowOperationNotPermittedException()
            throws SQLException {
        int adminId = 1;
        User adminInitiator = new User(adminId, "adminUser", "adminPass", Role.ADMIN, false);
        Role newRole = Role.USER;

        doAnswer(invocation -> {
            org.criticizer.dao.helper.DaoHelperService.TransactionOperation operation =
                    invocation.getArgument(0);
            operation.execute(mockConnection);
            return null;
        }).when(daoHelperMock).executeInTransaction(any(), any());

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(DbConstants.Columns.ID)).thenReturn(adminId);
        when(mockResultSet.getString(DbConstants.Columns.NAME)).thenReturn("adminUser");
        when(mockResultSet.getString(DbConstants.Columns.PASSWORD)).thenReturn("adminPass");
        when(mockResultSet.getString(DbConstants.Columns.ROLE)).thenReturn("ADMIN");
        when(mockResultSet.getBoolean(DbConstants.Columns.PROFILE_IS_PUBLIC)).thenReturn(false);

        OperationNotPermittedException exception = assertThrows(
                OperationNotPermittedException.class,
                () -> userService.changeUserRole(adminId, newRole, adminInitiator)
        );

        assertEquals(
                "This operation is not allowed: Administrator cannot remove their own admin role",
                exception.getUserMessage()
        );

        verify(daoHelperMock, times(1)).executeInTransaction(any(), any());
    }

    @Test
    @DisplayName("changeUserRole should succeed when admin 'changes' own role to ADMIN")
    void changeUserRole_whenAdminChangesOwnRoleToAdmin_shouldSucceed() throws SQLException {
        int adminId = 1;
        User adminInitiator = new User(adminId, "adminUser", "adminPass", Role.ADMIN, false);
        Role newRole = Role.ADMIN;

        doAnswer(invocation -> {
            org.criticizer.dao.helper.DaoHelperService.TransactionOperation operation =
                    invocation.getArgument(0);
            operation.execute(mockConnection);
            return null;
        }).when(daoHelperMock).executeInTransaction(any(), any());

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(DbConstants.Columns.ID)).thenReturn(adminId);
        when(mockResultSet.getString(DbConstants.Columns.NAME)).thenReturn("adminUser");
        when(mockResultSet.getString(DbConstants.Columns.PASSWORD)).thenReturn("adminPass");
        when(mockResultSet.getString(DbConstants.Columns.ROLE)).thenReturn("ADMIN");
        when(mockResultSet.getBoolean(DbConstants.Columns.PROFILE_IS_PUBLIC)).thenReturn(false);

        assertDoesNotThrow(() -> userService.changeUserRole(adminId, newRole, adminInitiator));

        verify(mockConnection, atLeastOnce()).prepareStatement(anyString());
        verify(mockPreparedStatement, atLeastOnce()).executeUpdate();
    }

    @Test
    @DisplayName("changeUserRole should succeed when admin changes another admin's role to USER")
    void changeUserRole_whenAdminChangesAnotherAdminRoleToUser_shouldSucceed() throws SQLException {
        int initiatorId = 1;
        User adminInitiator = new User(initiatorId, "adminUserOne", "pass1", Role.ADMIN, false);
        int targetAdminUserId = 2;
        Role newRole = Role.USER;

        doAnswer(invocation -> {
            org.criticizer.dao.helper.DaoHelperService.TransactionOperation operation =
                    invocation.getArgument(0);
            operation.execute(mockConnection);
            return null;
        }).when(daoHelperMock).executeInTransaction(any(), any());

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(DbConstants.Columns.ID)).thenReturn(initiatorId);
        when(mockResultSet.getString(DbConstants.Columns.NAME)).thenReturn("adminUserOne");
        when(mockResultSet.getString(DbConstants.Columns.PASSWORD)).thenReturn("pass1");
        when(mockResultSet.getString(DbConstants.Columns.ROLE)).thenReturn("ADMIN");
        when(mockResultSet.getBoolean(DbConstants.Columns.PROFILE_IS_PUBLIC)).thenReturn(false);

        assertDoesNotThrow(() -> userService.changeUserRole(targetAdminUserId, newRole, adminInitiator));

        verify(mockConnection, atLeastOnce()).prepareStatement(anyString());
        verify(mockPreparedStatement, atLeastOnce()).executeUpdate();
    }

    @Test
    @DisplayName("changeUserRole should throw exception if admin lost privileges during transaction")
    void changeUserRole_whenAdminLostPrivileges_shouldThrowException() throws SQLException {
        int initiatorId = 1;
        User adminInitiator = new User(initiatorId, "adminUser", "adminPass", Role.ADMIN, false);
        int targetUserId = 2;
        Role newRole = Role.USER;

        doAnswer(invocation -> {
            org.criticizer.dao.helper.DaoHelperService.TransactionOperation operation =
                    invocation.getArgument(0);
            operation.execute(mockConnection);
            return null;
        }).when(daoHelperMock).executeInTransaction(any(), any());

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(DbConstants.Columns.ID)).thenReturn(initiatorId);
        when(mockResultSet.getString(DbConstants.Columns.NAME)).thenReturn("adminUser");
        when(mockResultSet.getString(DbConstants.Columns.PASSWORD)).thenReturn("adminPass");
        when(mockResultSet.getString(DbConstants.Columns.ROLE)).thenReturn("USER");
        when(mockResultSet.getBoolean(DbConstants.Columns.PROFILE_IS_PUBLIC)).thenReturn(false);

        InsufficientPermissionsException exception = assertThrows(
                InsufficientPermissionsException.class,
                () -> userService.changeUserRole(targetUserId, newRole, adminInitiator)
        );

        assertEquals("You don't have permission to perform this action", exception.getUserMessage());
    }

    // ==================== DELETE USER TESTS ====================

    @Test
    @DisplayName("deleteUser should throw InsufficientPermissionsException when initiator is not admin")
    void deleteUser_whenInitiatorIsNotAdmin_shouldThrowInsufficientPermissionsException() {
        int initiatorId = 1;
        User nonAdminInitiator = new User(initiatorId, "normalUser", "userPass", Role.USER, false);
        int targetUserId = 2;

        InsufficientPermissionsException exception = assertThrows(InsufficientPermissionsException.class, () ->
                userService.deleteUser(targetUserId, nonAdminInitiator));

        assertEquals("You don't have permission to perform this action", exception.getUserMessage());
        verify(mediaDelServiceMock, never()).deleteAllMediaForUser(anyInt());
        verify(userDaoMock, never()).deleteUser(anyInt());
    }

    @Test
    @DisplayName("deleteUser should throw InsufficientPermissionsException when initiator is null")
    void deleteUser_whenInitiatorIsNull_shouldThrowInsufficientPermissionsException() {
        int targetUserId = 2;

        InsufficientPermissionsException exception = assertThrows(InsufficientPermissionsException.class, () ->
                userService.deleteUser(targetUserId, null));

        assertEquals("You don't have permission to perform this action", exception.getUserMessage());
        verify(mediaDelServiceMock, never()).deleteAllMediaForUser(anyInt());
        verify(userDaoMock, never()).deleteUser(anyInt());
    }

    @Test
    @DisplayName("deleteUser should throw OperationNotPermittedException when admin tries to delete self")
    void deleteUser_whenAdminDeletesSelf_shouldThrowOperationNotPermittedException() {
        int adminId = 1;
        User adminInitiator = new User(adminId, "adminUser", "adminPass", Role.ADMIN, false);

        OperationNotPermittedException exception = assertThrows(OperationNotPermittedException.class, () ->
                userService.deleteUser(adminId, adminInitiator));

        assertEquals("This operation is not allowed: You cannot delete your own account.", exception.getUserMessage());
        verify(mediaDelServiceMock, never()).deleteAllMediaForUser(anyInt());
        verify(userDaoMock, never()).deleteUser(anyInt());
    }

    @Test
    @DisplayName("deleteUser should successfully delete user and their media when admin deletes another user")
    void deleteUser_whenAdminDeletesAnotherUser_shouldSucceed() {
        int adminId = 1;
        int targetUserId = 2;
        User adminInitiator = new User(adminId, "adminUser", "adminPass", Role.ADMIN, false);

        doAnswer(invocation -> {
            org.criticizer.dao.helper.DaoHelperService.TransactionOperation operation = invocation.getArgument(0);
            operation.execute(mockConnection);
            return null;
        }).when(daoHelperMock).executeInTransaction(any(), any());

        userService.deleteUser(targetUserId, adminInitiator);

        verify(mediaDelServiceMock, times(1)).deleteAllMediaForUser(targetUserId);
        verify(userDaoMock, times(1)).deleteUser(targetUserId);
    }

    @Test
    @DisplayName("deleteUser should rollback transaction when media deletion fails")
    void deleteUser_whenMediaDeletionFails_shouldRollbackTransaction() {
        int adminId = 1;
        int targetUserId = 2;
        User adminInitiator = new User(adminId, "adminUser", "adminPass", Role.ADMIN, false);

        doAnswer(invocation -> {
            org.criticizer.dao.helper.DaoHelperService.TransactionOperation operation = invocation.getArgument(0);
            operation.execute(mockConnection);
            return null;
        }).when(daoHelperMock).executeInTransaction(any(), any());

        doThrow(new RuntimeException("Media deletion failed"))
                .when(mediaDelServiceMock).deleteAllMediaForUser(targetUserId);
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.deleteUser(targetUserId, adminInitiator));

        assertTrue(exception.getMessage().contains("Media deletion failed"));
        verify(userDaoMock, never()).deleteUser(targetUserId);
    }

    @Test
    @DisplayName("deleteUser should rollback transaction when user deletion fails")
    void deleteUser_whenUserDeletionFails_shouldRollbackTransaction() {
        int adminId = 1;
        int targetUserId = 2;
        User adminInitiator = new User(adminId, "adminUser", "adminPass", Role.ADMIN, false);
        String errorMsg = "Database operation failed: Database error";

        doThrow(new DatabaseException("deleteUser", new RuntimeException(errorMsg)))
                .when(daoHelperMock).executeInTransaction(any(), any());

        DatabaseException exception = assertThrows(DatabaseException.class, () ->
                userService.deleteUser(targetUserId, adminInitiator));

        assertTrue(exception.getMessage().contains("deleteUser"));
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause().getMessage().contains(errorMsg));
    }

    // ==================== GET USERS PAGE TESTS ====================

    @Test
    @DisplayName("getUsersPage should return paginated users with valid parameters")
    void getUsersPage_whenValidParameters_shouldReturnPagedResult() {
        String searchTerm = "test";
        int page = 1;
        int pageSize = 10;
        boolean publicOnly = false;

        User user1 = new User(1, "testuser1", "pass1", Role.USER, false);
        User user2 = new User(2, "testuser2", "pass2", Role.USER, false);
        List<User> expectedUsers = Arrays.asList(user1, user2);

        when(validatorMock.validatePagination(page, pageSize))
                .thenReturn(new ServiceValidator.PaginationParams(page, pageSize, 0));
        when(validatorMock.sanitizeSearchTerm(searchTerm)).thenReturn(searchTerm);
        when(userDaoMock.findUsers(searchTerm, 0, pageSize, publicOnly)).thenReturn(expectedUsers);
        when(userDaoMock.countUsers(searchTerm, publicOnly)).thenReturn(2);

        UserPageResult<User> result = userService.getUsersPage(searchTerm, page, pageSize, publicOnly);

        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        assertEquals(2, result.getTotalItems());
        assertEquals(page, result.getCurrentPage());
        assertEquals(pageSize, result.getPageSize());
        verify(userDaoMock).findUsers(searchTerm, 0, pageSize, publicOnly);
        verify(userDaoMock).countUsers(searchTerm, publicOnly);
    }

    @Test
    @DisplayName("getUsersPage should handle empty search term")
    void getUsersPage_whenSearchTermIsEmpty_shouldReturnAllUsers() {
        String searchTerm = "";
        int page = 1;
        int pageSize = 10;

        when(validatorMock.validatePagination(page, pageSize))
                .thenReturn(new ServiceValidator.PaginationParams(page, pageSize, 0));
        when(validatorMock.sanitizeSearchTerm(searchTerm)).thenReturn(null);
        when(userDaoMock.findUsers(null, 0, pageSize, false)).thenReturn(Collections.emptyList());
        when(userDaoMock.countUsers(null, false)).thenReturn(0);

        UserPageResult<User> result = userService.getUsersPage(searchTerm, page, pageSize, false);

        assertNotNull(result);
        verify(validatorMock).sanitizeSearchTerm(searchTerm);
        verify(userDaoMock).findUsers(null, 0, pageSize, false);
    }

    @Test
    @DisplayName("getUsersPage should filter public users only when publicOnly is true")
    void getUsersPage_whenPublicOnlyIsTrue_shouldFilterPublicUsers() {
        String searchTerm = "user";
        int page = 1;
        int pageSize = 10;
        boolean publicOnly = true;

        User publicUser = new User(1, "publicUser", "pass", Role.USER, true);
        List<User> expectedUsers = Collections.singletonList(publicUser);

        when(validatorMock.validatePagination(page, pageSize))
                .thenReturn(new ServiceValidator.PaginationParams(page, pageSize, 0));
        when(validatorMock.sanitizeSearchTerm(searchTerm)).thenReturn(searchTerm);
        when(userDaoMock.findUsers(searchTerm, 0, pageSize, true)).thenReturn(expectedUsers);
        when(userDaoMock.countUsers(searchTerm, true)).thenReturn(1);

        UserPageResult<User> result = userService.getUsersPage(searchTerm, page, pageSize, publicOnly);

        assertEquals(1, result.getItems().size());
        verify(userDaoMock).findUsers(searchTerm, 0, pageSize, true);
    }

    // ==================== UPDATE USER PRIVACY TESTS ====================

    @Test
    @DisplayName("updateUserPrivacy should update user privacy to public")
    void updateUserPrivacy_whenSettingPublic_shouldUpdatePrivacy() {
        int userId = 1;
        boolean isPublic = true;

        userService.updateUserPrivacy(userId, isPublic);

        verify(userDaoMock, times(1)).updateUserPrivacy(userId, isPublic);
    }

    @Test
    @DisplayName("updateUserPrivacy should update user privacy to private")
    void updateUserPrivacy_whenSettingPrivate_shouldUpdatePrivacy() {
        int userId = 1;
        boolean isPublic = false;

        userService.updateUserPrivacy(userId, isPublic);

        verify(userDaoMock, times(1)).updateUserPrivacy(userId, isPublic);
    }
}