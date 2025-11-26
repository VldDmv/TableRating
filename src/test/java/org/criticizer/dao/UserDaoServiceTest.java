package org.criticizer.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.criticizer.dao.user.UserDao;
import org.criticizer.dao.user.UserDaoService;
import org.criticizer.entity.AdminStats;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.util.DataSourceProvider;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserDaoServiceTest {

    private static final Logger log = LoggerFactory.getLogger(UserDaoServiceTest.class);
    private UserDao userDao;

    @BeforeAll
    void setUpAll() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test_user_db;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        DataSourceProvider.initialize(new HikariDataSource(config));
        log.info("Test DataSource for UserDao initialized.");

        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("RUNSCRIPT FROM 'classpath:schema.sql'");
            log.info("Database schema created successfully for UserDao tests.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test schema for UserDao", e);
        }
        userDao = new UserDaoService();
    }

    @AfterAll
    void tearDownAll() {
        DataSourceProvider.close();
        log.info("Test DataSource for UserDao closed.");
    }

    @BeforeEach
    void setUp() {
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE;");
            stmt.execute("TRUNCATE TABLE game_tags RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE movie_genres RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE book_genres RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE show_genres RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE genre_applicability RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE games RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE movies RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE books RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE shows RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE users RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE tags RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE genres RESTART IDENTITY;");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE;");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear tables in @BeforeEach", e);
        }
    }

    private void addFiveTestUsers() {
        userDao.addUser(new User("Alice Test", "passAlice"));
        userDao.addUser(new User("Bob Test", "passBob"));
        userDao.addUser(new User("Charlie User", "passCharlie"));
        userDao.addUser(new User("Diana Test", "passDiana"));
        userDao.addUser(new User("Edward User", "passEdward"));
    }

    @Test
    @DisplayName("addUser should insert a new user and findUserByName should retrieve it")
    void addUser_and_findUserByName_shouldWorkCorrectly() {
        log.info("TEST: addUser_and_findUserByName_shouldWorkCorrectly - START");
        User newUser = new User("testuser", "password123_hashed");
        userDao.addUser(newUser);
        User foundUser = userDao.findUserByName("testuser");

        assertNotNull(foundUser, "User should be found by name after being added.");
        assertEquals("testuser", foundUser.getName(), "User name should match.");

        Assertions.assertEquals(Role.USER, foundUser.getRole(), "User role should default to USER.");
        assertFalse(foundUser.isProfileIsPublic(), "Profile should be private by default.");
        assertTrue(foundUser.getId() > 0, "User ID should be generated and positive.");
        log.info("TEST: addUser_and_findUserByName_shouldWorkCorrectly - END");
    }

    @Test
    @DisplayName("findUserByName should be case-insensitive and return null for non-existent user")
    void findUserByName_isCaseInsensitiveAndHandlesNotFound() {
        log.info("TEST: findUserByName_isCaseInsensitiveAndHandlesNotFound - START");
        userDao.addUser(new User("CaseTest", "password"));
        User foundUser = userDao.findUserByName("casetest");
        assertNotNull(foundUser, "User should be found regardless of case.");

        User nonExistentUser = userDao.findUserByName("nonexistent");
        assertNull(nonExistentUser, "Searching for a non-existent user should return null.");
        log.info("TEST: findUserByName_isCaseInsensitiveAndHandlesNotFound - END");
    }

    @Test
    @DisplayName("getAllUsers should return all users when they exist")
    void getAllUsers_whenUsersExist_shouldReturnAllUsers() {
        log.info("TEST: getAllUsers_whenUsersExist_shouldReturnAllUsers - START");
        addFiveTestUsers();
        List<User> users = userDao.getAllUsers();
        assertEquals(5, users.size(), "Should return all 5 added users.");
        log.info("TEST: getAllUsers_whenUsersExist_shouldReturnAllUsers - END");
    }

    @Test
    @DisplayName("getAllUsers should return an empty list when no users are in the database")
    void getAllUsers_whenNoUsers_shouldReturnEmptyList() {
        log.info("TEST: getAllUsers_whenNoUsers_shouldReturnEmptyList - START");
        List<User> users = userDao.getAllUsers();
        assertNotNull(users, "The returned list should not be null.");
        assertTrue(users.isEmpty(), "The list of users should be empty.");
        log.info("TEST: getAllUsers_whenNoUsers_shouldReturnEmptyList - END");
    }

    @Test
    @DisplayName("updateUserRole should update the role of an existing user")
    void updateUserRole_whenUserExists_shouldUpdateRoleInDatabase() {
        log.info("TEST: updateUserRole_whenUserExists_shouldUpdateRoleInDatabase - START");
        userDao.addUser(new User("roleTester", "p"));
        User user = userDao.findUserByName("roleTester");
        assertEquals(Role.USER, user.getRole());

        userDao.updateUserRole(user.getId(), Role.ADMIN);
        User updatedUser = userDao.findUserByName("roleTester");
        assertEquals(Role.ADMIN, updatedUser.getRole());
        log.info("TEST: updateUserRole_whenUserExists_shouldUpdateRoleInDatabase - END");
    }

    @Test
    @DisplayName("updateUserPrivacy should change the privacy flag of an existing user")
    void updateUserPrivacy_shouldChangePrivacyFlag() {
        log.info("TEST: updateUserPrivacy_shouldChangePrivacyFlag - START");
        userDao.addUser(new User("privacyTester", "p"));
        User user = userDao.findUserByName("privacyTester");
        assertFalse(user.isProfileIsPublic());

        userDao.updateUserPrivacy(user.getId(), true);
        User updatedUser = userDao.findUserByName("privacyTester");
        assertTrue(updatedUser.isProfileIsPublic());
        log.info("TEST: updateUserPrivacy_shouldChangePrivacyFlag - END");
    }

    @Test
    @DisplayName("deleteUser should remove an existing user from the database")
    void deleteUser_whenUserExists_shouldRemoveUser() {
        log.info("TEST: deleteUser_whenUserExists_shouldRemoveUser - START");
        userDao.addUser(new User("toDelete", "p"));
        User user = userDao.findUserByName("toDelete");
        assertNotNull(user);

        userDao.deleteUser(user.getId());
        User deletedUser = userDao.findUserByName("toDelete");
        assertNull(deletedUser);
        log.info("TEST: deleteUser_whenUserExists_shouldRemoveUser - END");
    }

    @Test
    @DisplayName("findUsers should return paginated and searchable results")
    void findUsers_withPaginationAndSearch_shouldReturnCorrectResults() {
        log.info("TEST: findUsers_withPaginationAndSearch_shouldReturnCorrectResults - START");
        addFiveTestUsers();

        List<User> searchResults = userDao.findUsers("Test", 0, 10, false);
        assertEquals(3, searchResults.size());
        assertTrue(searchResults.stream().allMatch(u -> u.getName().contains("Test")));

        List<User> page1 = userDao.findUsers("User", 0, 1, false);
        assertEquals(1, page1.size());
        assertEquals("Charlie User", page1.get(0).getName());

        List<User> page2 = userDao.findUsers("User", 1, 1, false);
        assertEquals(1, page2.size());
        assertEquals("Edward User", page2.get(0).getName());
        log.info("TEST: findUsers_withPaginationAndSearch_shouldReturnCorrectResults - END");
    }

    @Test
    @DisplayName("countUsers should return correct count for a search term")
    void countUsers_withSearchTerm_shouldReturnCorrectCount() {
        log.info("TEST: countUsers_withSearchTerm_shouldReturnCorrectCount - START");
        addFiveTestUsers();
        int count = userDao.countUsers("User", false);
        assertEquals(2, count);
        log.info("TEST: countUsers_withSearchTerm_shouldReturnCorrectCount - END");
    }


    @Test
    @DisplayName(" getAdminStatistics should return all entity counts in one query")
    void getAdminStatistics_shouldReturnAllCounts() {
        log.info("TEST: getAdminStatistics_shouldReturnAllCounts - START");
        addFiveTestUsers();

        AdminStats stats = userDao.getAdminStatistics();

        assertNotNull(stats, "AdminStats should not be null");
        assertEquals(5, stats.getTotalUsers(), "Should count 5 users");
        assertEquals(0, stats.getTotalGames(), "Should count 0 games (none added)");
        assertEquals(0, stats.getTotalMovies(), "Should count 0 movies (none added)");
        assertEquals(0, stats.getTotalBooks(), "Should count 0 books (none added)");
        assertEquals(0, stats.getTotalShows(), "Should count 0 shows (none added)");

        log.info("Admin statistics: {}", stats);
        log.info("TEST: getAdminStatistics_shouldReturnAllCounts - END");
    }

    @Test
    @DisplayName(" getAdminStatistics should return zeros for empty database")
    void getAdminStatistics_withEmptyDatabase_shouldReturnZeros() {
        log.info("TEST: getAdminStatistics_withEmptyDatabase_shouldReturnZeros - START");

        AdminStats stats = userDao.getAdminStatistics();

        assertNotNull(stats, "AdminStats should not be null");
        assertEquals(0, stats.getTotalUsers(), "Should count 0 users");
        assertEquals(0, stats.getTotalGames(), "Should count 0 games");
        assertEquals(0, stats.getTotalMovies(), "Should count 0 movies");
        assertEquals(0, stats.getTotalBooks(), "Should count 0 books");
        assertEquals(0, stats.getTotalShows(), "Should count 0 shows");

        log.info("TEST: getAdminStatistics_withEmptyDatabase_shouldReturnZeros - END");
    }
}