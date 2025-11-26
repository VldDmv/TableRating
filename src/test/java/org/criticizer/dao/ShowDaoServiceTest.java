package org.criticizer.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.criticizer.dao.helper.DaoFactory;
import org.criticizer.dao.helper.DaoFactoryService;
import org.criticizer.dao.show.ShowDao;
import org.criticizer.dao.user.UserDao;
import org.criticizer.entity.Role;
import org.criticizer.entity.Show;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.exceptions.validation.EmptyNameException;
import org.criticizer.exceptions.validation.InvalidScoreException;
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
public class ShowDaoServiceTest {
    private static final Logger log = LoggerFactory.getLogger(ShowDaoServiceTest.class);

    private UserDao userDao;
    private ShowDao showDao;

    private int testUser1Id;
    private int testUser2Id;

    private static final String USER1_NAME = "sTestUser1_Shows";
    private static final String USER2_NAME = "sTestUser2_Shows";

    @BeforeAll
    void setUpAllTests() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test_show_db;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        HikariDataSource testDataSource = new HikariDataSource(config);
        DataSourceProvider.initialize(testDataSource);
        log.info("Test DataSource initialized.");

        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("RUNSCRIPT FROM 'classpath:schema.sql'");
            log.info("Database schema created successfully for tests.");
        } catch (Exception e) {
            log.error("Failed to create test schema", e);
            throw new RuntimeException("Failed to create test schema", e);
        }
        DaoFactory daoFactory = new DaoFactoryService();
        this.userDao = daoFactory.getUserDao();
        this.showDao = daoFactory.getShowDao();
    }

    @BeforeEach
    void setUpData() {
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE;");
            stmt.execute("TRUNCATE TABLE show_genres RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE shows RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE users RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE genres RESTART IDENTITY;");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE;");
        } catch (SQLException e) {
            log.error("Error clearing tables in @BeforeEach", e);
            throw new RuntimeException(e);
        }

        User user1 = new User(USER1_NAME, "password123");
        userDao.addUser(user1);
        User foundUser1 = userDao.findUserByName(USER1_NAME);
        assertNotNull(foundUser1);
        testUser1Id = foundUser1.getId();

        User user2 = new User(USER2_NAME, "password456");
        userDao.addUser(user2);
        User foundUser2 = userDao.findUserByName(USER2_NAME);
        assertNotNull(foundUser2);
        testUser2Id = foundUser2.getId();

        log.debug("Data setup complete for users: {} (ID:{}) and {} (ID:{})", USER1_NAME, testUser1Id, USER2_NAME, testUser2Id);
    }

    @AfterAll
    void tearDownAllTests() {
        DataSourceProvider.close();
        log.info("Test DataSource closed in @AfterAll.");
    }

    @Test
    void testAddShow_Success() {
        log.info("TEST: testAddShow_Success - START");
        showDao.addShow("Breaking Bad", testUser1Id, 95, null);
        assertTrue(showDao.isShowExists("Breaking Bad", testUser1Id));
        List<Show> shows = showDao.getUserShows(testUser1Id);
        assertEquals(1, shows.size());
        Show addedShow = shows.get(0);
        assertEquals("Breaking Bad", addedShow.getName());
        assertEquals(95, addedShow.getScore());
        assertFalse(addedShow.isCompleted(), "New show should be not completed by default.");
        assertEquals(testUser1Id, addedShow.getUserId());
        log.info("TEST: testAddShow_Success - END");
    }

    @Test
    void testAddShow_DuplicateNameForSameUser_ThrowsItemAlreadyExistsException() {
        log.info("TEST: testAddShow_DuplicateNameForSameUser_ThrowsItemAlreadyExistsException - START");
        showDao.addShow("Stranger Things", testUser1Id, 80, null);
        Exception exception = assertThrows(ItemAlreadyExistsException.class, () -> showDao.addShow("Stranger Things", testUser1Id, 85, null));
        assertTrue(exception.getMessage().contains("already exists") || exception.getMessage().contains("Show"));
        log.info("TEST: testAddShow_DuplicateNameForSameUser_ThrowsItemAlreadyExistsException - END");
    }

    @Test
    void testAddShow_SameNameForDifferentUser_Success() {
        log.info("TEST: testAddShow_SameNameForDifferentUser_Success - START");
        showDao.addShow("The Office", testUser1Id, 90, null);
        assertDoesNotThrow(() -> showDao.addShow("The Office", testUser2Id, 88, null));
        assertTrue(showDao.isShowExists("The Office", testUser1Id));
        assertTrue(showDao.isShowExists("The Office", testUser2Id));
        log.info("TEST: testAddShow_SameNameForDifferentUser_Success - END");
    }

    @Test
    void testAddShow_InvalidScore_TooLow_ThrowsInvalidScoreException() {
        log.info("TEST: testAddShow_InvalidScore_TooLow_ThrowsInvalidScoreException - START");
        Exception exception = assertThrows(InvalidScoreException.class, () -> showDao.addShow("Low Score Show", testUser1Id, 0, null));
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 100") || exception.getMessage().contains("1-100"));
        log.info("TEST: testAddShow_InvalidScore_TooLow_ThrowsInvalidScoreException - END");
    }

    @Test
    void testAddShow_InvalidScore_TooHigh_ThrowsInvalidScoreException() {
        log.info("TEST: testAddShow_InvalidScore_TooHigh_ThrowsInvalidScoreException - START");
        Exception exception = assertThrows(InvalidScoreException.class, () -> showDao.addShow("High Score Show", testUser1Id, 101, null));
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 100") || exception.getMessage().contains("1-100"));
        log.info("TEST: testAddShow_InvalidScore_TooHigh_ThrowsInvalidScoreException - END");
    }

    @Test
    void testGetUserShows_NoShows_ReturnsEmptyList() {
        log.info("TEST: testGetUserShows_NoShows_ReturnsEmptyList - START");
        List<Show> shows = showDao.getUserShows(testUser1Id);
        assertTrue(shows.isEmpty());
        log.info("TEST: testGetUserShows_NoShows_ReturnsEmptyList - END");
    }

    @Test
    void testGetUserShows_WithShows_ReturnsCorrectShows() {
        log.info("TEST: testGetUserShows_WithShows_ReturnsCorrectShows - START");
        showDao.addShow("Show 1", testUser1Id, 70, null);
        showDao.addShow("Show 2", testUser1Id, 80, null);

        List<Show> shows = showDao.getUserShows(testUser1Id);
        assertEquals(2, shows.size());
        assertTrue(shows.stream().anyMatch(s -> s.getName().equals("Show 1") && s.getScore() == 70 && s.getUserId() == testUser1Id));
        assertTrue(shows.stream().anyMatch(s -> s.getName().equals("Show 2") && s.getScore() == 80 && s.getUserId() == testUser1Id));
        log.info("TEST: testGetUserShows_WithShows_ReturnsCorrectShows - END");
    }

    @Test
    void testGetUserShows_ForUserWithNoShows_WhenOtherUserHasShows() {
        log.info("TEST: testGetUserShows_ForUserWithNoShows_WhenOtherUserHasShows - START");
        showDao.addShow("User2's Show", testUser2Id, 90, null);
        List<Show> user1Shows = showDao.getUserShows(testUser1Id);
        assertTrue(user1Shows.isEmpty());
        log.info("TEST: testGetUserShows_ForUserWithNoShows_WhenOtherUserHasShows - END");
    }

    @Test
    void testRemoveShow_Success() {
        log.info("TEST: testRemoveShow_Success - START");
        showDao.addShow("To Be Removed", testUser1Id, 50, null);
        assertTrue(showDao.isShowExists("To Be Removed", testUser1Id));

        assertDoesNotThrow(() -> showDao.removeShow("To Be Removed", testUser1Id));
        assertFalse(showDao.isShowExists("To Be Removed", testUser1Id));
        log.info("TEST: testRemoveShow_Success - END");
    }

    @Test
    void testRemoveShow_NonExistentShow_ThrowsResourceNotFoundException() {
        log.info("TEST: testRemoveShow_NonExistentShow_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> showDao.removeShow("Non Existent Show", testUser1Id));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Show"));
        log.info("TEST: testRemoveShow_NonExistentShow_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testIsShowExists_ShowExists_ReturnsTrue() {
        log.info("TEST: testIsShowExists_ShowExists_ReturnsTrue - START");
        showDao.addShow("Existing Show", testUser1Id, 75, null);
        assertTrue(showDao.isShowExists("Existing Show", testUser1Id));
        log.info("TEST: testIsShowExists_ShowExists_ReturnsTrue - END");
    }

    @Test
    void testIsShowExists_ShowDoesNotExist_ReturnsFalse() {
        log.info("TEST: testIsShowExists_ShowDoesNotExist_ReturnsFalse - START");
        assertFalse(showDao.isShowExists("Ghost Show", testUser1Id));
        log.info("TEST: testIsShowExists_ShowDoesNotExist_ReturnsFalse - END");
    }

    @Test
    void testIsShowExists_ShowExistsForDifferentUser_ReturnsFalse() {
        log.info("TEST: testIsShowExists_ShowExistsForDifferentUser_ReturnsFalse - START");
        showDao.addShow("Shared Name Show", testUser2Id, 80, null);
        assertFalse(showDao.isShowExists("Shared Name Show", testUser1Id));
        log.info("TEST: testIsShowExists_ShowExistsForDifferentUser_ReturnsFalse - END");
    }

    @Test
    void testUpdateShowAndName_Success_NameAndScoreChange() {
        log.info("TEST: testUpdateShowAndName_Success_NameAndScoreChange - START");
        showDao.addShow("Old Name", testUser1Id, 60, null);
        assertDoesNotThrow(() -> showDao.updateShowAndName("Old Name", "New Name", 70, testUser1Id, null));

        assertFalse(showDao.isShowExists("Old Name", testUser1Id));
        assertTrue(showDao.isShowExists("New Name", testUser1Id));
        List<Show> shows = showDao.getUserShows(testUser1Id);
        assertEquals(1, shows.size());
        Show updatedShow = shows.get(0);
        assertEquals("New Name", updatedShow.getName());
        assertEquals(70, updatedShow.getScore());
        assertEquals(testUser1Id, updatedShow.getUserId());
        log.info("TEST: testUpdateShowAndName_Success_NameAndScoreChange - END");
    }

    @Test
    void testUpdateShowAndName_Success_OnlyScoreChange() {
        log.info("TEST: testUpdateShowAndName_Success_OnlyScoreChange - START");
        showDao.addShow("Constant Name", testUser1Id, 60, null);
        assertDoesNotThrow(() -> showDao.updateShowAndName("Constant Name", "Constant Name", 70, testUser1Id, null));

        assertTrue(showDao.isShowExists("Constant Name", testUser1Id));
        List<Show> shows = showDao.getUserShows(testUser1Id);
        assertEquals(1, shows.size());
        Show updatedShow = shows.get(0);
        assertEquals("Constant Name", updatedShow.getName());
        assertEquals(70, updatedShow.getScore());
        log.info("TEST: testUpdateShowAndName_Success_OnlyScoreChange - END");
    }

    @Test
    void testUpdateShowAndName_NonExistentShow_ThrowsResourceNotFoundException() {
        log.info("TEST: testUpdateShowAndName_NonExistentShow_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> showDao.updateShowAndName("Phantom Show", "New Phantom", 50, testUser1Id, null));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Show"));
        log.info("TEST: testUpdateShowAndName_NonExistentShow_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testUpdateShowAndName_NewNameExistsForSameUser_ThrowsResourceNotFoundException() {
        log.info("TEST: testUpdateShowAndName_NewNameExistsForSameUser_ThrowsResourceNotFoundException - START");
        showDao.addShow("Show A", testUser1Id, 50, null);
        showDao.addShow("Show B", testUser1Id, 55, null);

        Exception exception = assertThrows(ItemAlreadyExistsException.class, () -> showDao.updateShowAndName("Show A", "Show B", 60, testUser1Id, null));
        assertTrue(exception.getMessage().contains("already exists") || exception.getMessage().contains("Show"));
        log.info("TEST: testUpdateShowAndName_NewNameExistsForSameUser_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testUpdateShowAndName_InvalidScore_ThrowsInvalidScoreException() {
        log.info("TEST: testUpdateShowAndName_InvalidScore_ThrowsInvalidScoreException - START");
        showDao.addShow("Update Score Show", testUser1Id, 50, null);
        Exception exception = assertThrows(InvalidScoreException.class, () -> showDao.updateShowAndName("Update Score Show", "Update Score Show New", 101, testUser1Id, null));
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 100") || exception.getMessage().contains("1-100"));
        log.info("TEST: testUpdateShowAndName_InvalidScore_ThrowsInvalidScoreException - END");
    }

    @Test
    void testUpdateShowAndName_EmptyOldName_ThrowsEmptyNameException() {
        log.info("TEST: testUpdateShowAndName_EmptyOldName_ThrowsEmptyNameException - START");
        Exception exception = assertThrows(EmptyNameException.class, () -> showDao.updateShowAndName("  ", "New Name", 50, testUser1Id, null));
        assertTrue(exception.getMessage().contains("cannot be empty") || exception.getMessage().contains("Show"));
        log.info("TEST: testUpdateShowAndName_EmptyOldName_ThrowsEmptyNameException - END");
    }

    @Test
    void testUpdateShowAndName_EmptyNewName_ThrowsEmptyNameException() {
        log.info("TEST: testUpdateShowAndName_EmptyNewName_ThrowsEmptyNameException - START");
        showDao.addShow("Valid Old Name", testUser1Id, 50, null);
        Exception exception = assertThrows(EmptyNameException.class, () -> showDao.updateShowAndName("Valid Old Name", "  ", 50, testUser1Id, null));
        assertTrue(exception.getMessage().contains("cannot be empty") || exception.getMessage().contains("Show"));
        log.info("TEST: testUpdateShowAndName_EmptyNewName_ThrowsEmptyNameException - END");
    }

    @Test
    void testUpdateShowAndName_NoActualChange_CompletesSuccessfully() {
        log.info("TEST: testUpdateShowAndName_NoActualChange_CompletesSuccessfully - START");
        String showName = "No Change Show";
        int showScore = 77;
        showDao.addShow(showName, testUser1Id, showScore, null);
        assertDoesNotThrow(() -> showDao.updateShowAndName(showName, showName, showScore, testUser1Id, null));
        List<Show> shows = showDao.getUserShows(testUser1Id);
        assertEquals(1, shows.size());
        assertEquals(showName, shows.get(0).getName());
        assertEquals(showScore, shows.get(0).getScore());
        log.info("TEST: testUpdateShowAndName_NoActualChange_CompletesSuccessfully - END");
    }

    @Test
    void testGetShowStatus_ShowExists_ReturnsCorrectStatus() {
        log.info("TEST: testGetShowStatus_ShowExists_ReturnsCorrectStatus - START");
        showDao.addShow("Status Show", testUser1Id, 80, null);
        assertFalse(showDao.getShowStatus("Status Show", testUser1Id));
        log.info("TEST: testGetShowStatus_ShowExists_ReturnsCorrectStatus - END");
    }

    @Test
    void testGetShowStatus_NonExistentShow_ThrowsResourceNotFoundException() {
        log.info("TEST: testGetShowStatus_NonExistentShow_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> showDao.getShowStatus("Unknown Status Show", testUser1Id));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Show"));
        log.info("TEST: testGetShowStatus_NonExistentShow_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testToggleShowStatus_FromFalseToTrue() {
        log.info("TEST: testToggleShowStatus_FromFalseToTrue - START");
        showDao.addShow("Toggle Me", testUser1Id, 70, null);
        assertFalse(showDao.getShowStatus("Toggle Me", testUser1Id));

        assertDoesNotThrow(() -> showDao.toggleShowStatus("Toggle Me", testUser1Id));
        assertTrue(showDao.getShowStatus("Toggle Me", testUser1Id));
        log.info("TEST: testToggleShowStatus_FromFalseToTrue - END");
    }

    @Test
    void testToggleShowStatus_FromTrueToFalse() {
        log.info("TEST: testToggleShowStatus_FromTrueToFalse - START");
        showDao.addShow("Toggle Me Twice", testUser1Id, 70, null);
        showDao.toggleShowStatus("Toggle Me Twice", testUser1Id);
        assertTrue(showDao.getShowStatus("Toggle Me Twice", testUser1Id));

        assertDoesNotThrow(() -> showDao.toggleShowStatus("Toggle Me Twice", testUser1Id));
        assertFalse(showDao.getShowStatus("Toggle Me Twice", testUser1Id));
        log.info("TEST: testToggleShowStatus_FromTrueToFalse - END");
    }

    @Test
    void testToggleShowStatus_NonExistentShow_ThrowsResourceNotFoundException() {
        log.info("TEST: testToggleShowStatus_NonExistentShow_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> showDao.toggleShowStatus("Non Existent Toggle", testUser1Id));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Show"));
        log.info("TEST: testToggleShowStatus_NonExistentShow_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testDeleteShowsByUserId_UserHasShows_DeletesAllShows() {
        log.info("TEST: testDeleteShowsByUserId_UserHasShows_DeletesAllShows - START");
        showDao.addShow("Show A del", testUser1Id, 50, null);
        showDao.addShow("Show B del", testUser1Id, 60, null);
        assertEquals(2, showDao.getUserShows(testUser1Id).size());

        assertDoesNotThrow(() -> showDao.deleteShowsByUserId(testUser1Id));
        assertTrue(showDao.getUserShows(testUser1Id).isEmpty());
        log.info("TEST: testDeleteShowsByUserId_UserHasShows_DeletesAllShows - END");
    }

    @Test
    void testDeleteShowsByUserId_UserHasNoShows_CompletesSilently() {
        log.info("TEST: testDeleteShowsByUserId_UserHasNoShows_CompletesSilently - START");
        assertTrue(showDao.getUserShows(testUser1Id).isEmpty());
        assertDoesNotThrow(() -> showDao.deleteShowsByUserId(testUser1Id));
        assertTrue(showDao.getUserShows(testUser1Id).isEmpty());
        log.info("TEST: testDeleteShowsByUserId_UserHasNoShows_CompletesSilently - END");
    }

    @Test
    void testDeleteShowsByUserId_DoesNotDeleteShowsOfOtherUsers() {
        log.info("TEST: testDeleteShowsByUserId_DoesNotDeleteShowsOfOtherUsers - START");
        showDao.addShow("User1 Show ToDelete", testUser1Id, 50, null);
        showDao.addShow("User2 Show ToKeep", testUser2Id, 60, null);

        assertDoesNotThrow(() -> showDao.deleteShowsByUserId(testUser1Id));

        assertTrue(showDao.getUserShows(testUser1Id).isEmpty());
        List<Show> user2Shows = showDao.getUserShows(testUser2Id);
        assertEquals(1, user2Shows.size());
        assertEquals("User2 Show ToKeep", user2Shows.get(0).getName());
        assertEquals(testUser2Id, user2Shows.get(0).getUserId());
        log.info("TEST: testDeleteShowsByUserId_DoesNotDeleteShowsOfOtherUsers - END");
    }

    @Test
    void testUserDeletion_CascadesToAssociatedShows() {
        log.info("TEST: testUserDeletion_CascadesToAssociatedShows - START");
        User cascadeTestUserEntity = new User(0, "CascadeDeleteUser_Shows", "cascadePass123", Role.USER, false);
        userDao.addUser(cascadeTestUserEntity);
        cascadeTestUserEntity = userDao.findUserByName(cascadeTestUserEntity.getName());
        assertNotNull(cascadeTestUserEntity, "Cascade test user should be created and found.");

        long cascadeUserEntityId = cascadeTestUserEntity.getId();
        int cascadeUserShowDaoId = (int) cascadeUserEntityId;
        log.debug("Cascade test user created with ID_long: {}, ID_int: {}", cascadeUserEntityId, cascadeUserShowDaoId);

        showDao.addShow("Cascade Show 1", cascadeUserShowDaoId, 70, null);
        showDao.addShow("Cascade Show 2", cascadeUserShowDaoId, 75, null);
        log.debug("Added 2 shows for cascade test user ID_int: {}", cascadeUserShowDaoId);

        List<Show> showsBeforeDeletion = showDao.getUserShows(cascadeUserShowDaoId);
        assertEquals(2, showsBeforeDeletion.size(), "Cascade test user should have 2 shows before user deletion.");

        log.debug("Attempting to delete cascade test user with ID_long: {}", cascadeUserEntityId);
        userDao.deleteUser((int) cascadeUserEntityId);
        log.debug("Cascade test user (ID_long: {}) deleted operation completed.", cascadeUserEntityId);

        List<Show> showsAfterUserDeletion = showDao.getUserShows(cascadeUserShowDaoId);
        assertTrue(showsAfterUserDeletion.isEmpty(), "Shows of the deleted user (ID_int: " + cascadeUserShowDaoId + ") should be removed by cascade.");
        log.debug("Checked shows after user deletion for ID_int: {}, list is empty as expected.", cascadeUserShowDaoId);

        User userAfterDeletion = userDao.findUserByName(cascadeTestUserEntity.getName());
        assertNull(userAfterDeletion, "Cascade test user '" + cascadeTestUserEntity.getName() + "' should no longer exist in the database.");
        log.debug("Checked user existence after deletion, user is null as expected.");
        log.info("TEST: testUserDeletion_CascadesToAssociatedShows - END");
    }

    @Test
    void testUpdateShowAndName_DoesNotChangeCompletedStatus() {
        log.info("TEST: testUpdateShowAndName_DoesNotChangeCompletedStatus - START");
        String showName = "StatusPreservationShow";
        int initialScore = 80;

        showDao.addShow(showName, testUser1Id, initialScore, null);
        Show show = showDao.getUserShows(testUser1Id).stream().filter(s -> s.getName().equals(showName)).findFirst().orElse(null);
        assertNotNull(show);
        assertFalse(show.isCompleted(), "Show should initially be not completed.");
        log.debug("Show '{}' added, completed status: {}", showName, show.isCompleted());

        showDao.toggleShowStatus(showName, testUser1Id);
        show = showDao.getUserShows(testUser1Id).stream().filter(s -> s.getName().equals(showName)).findFirst().orElse(null);
        assertNotNull(show);
        assertTrue(show.isCompleted(), "Show status should be toggled to true.");
        log.debug("Show '{}' status toggled, completed status: {}", showName, show.isCompleted());

        String updatedShowName = "UpdatedStatusPreservationShow";
        int updatedScore = 85;
        showDao.updateShowAndName(showName, updatedShowName, updatedScore, testUser1Id, null);
        log.debug("Show '{}' updated to '{}' with score {}", showName, updatedShowName, updatedScore);

        Show updatedShow = showDao.getUserShows(testUser1Id).stream().filter(s -> s.getName().equals(updatedShowName)).findFirst().orElse(null);
        assertNotNull(updatedShow, "Updated show should be found.");
        assertTrue(updatedShow.isCompleted(), "Completed status should remain true after name and score update.");
        assertEquals(updatedShowName, updatedShow.getName());
        assertEquals(updatedScore, updatedShow.getScore());
        log.debug("Show '{}' after update, completed status: {}", updatedShowName, updatedShow.isCompleted());

        showDao.toggleShowStatus(updatedShowName, testUser1Id);
        updatedShow = showDao.getUserShows(testUser1Id).stream().filter(s -> s.getName().equals(updatedShowName)).findFirst().orElse(null);
        assertNotNull(updatedShow);
        assertFalse(updatedShow.isCompleted(), "Show status should be toggled back to false.");
        log.debug("Show '{}' status toggled again, completed status: {}", updatedShowName, updatedShow.isCompleted());

        int finalScore = 90;
        showDao.updateShowAndName(updatedShowName, updatedShowName, finalScore, testUser1Id, null);
        log.debug("Show '{}' score updated to {}", updatedShowName, finalScore);

        Show finalStateShow = showDao.getUserShows(testUser1Id).stream().filter(s -> s.getName().equals(updatedShowName)).findFirst().orElse(null);
        assertNotNull(finalStateShow);
        assertFalse(finalStateShow.isCompleted(), "Completed status should remain false after score-only update.");
        assertEquals(updatedShowName, finalStateShow.getName());
        assertEquals(finalScore, finalStateShow.getScore());
        log.debug("Show '{}' after final update, completed status: {}", updatedShowName, finalStateShow.isCompleted());
        log.info("TEST: testUpdateShowAndName_DoesNotChangeCompletedStatus - END");
    }

    @Test
    void testGetUserShows_ReturnsCorrectCompletedStatus() {
        log.info("TEST: testGetUserShows_ReturnsCorrectCompletedStatus - START");
        String showNameCompleted = "Completed Show Status Test";
        String showNameInProgress = "In-Progress Show Status Test";

        showDao.addShow(showNameCompleted, testUser1Id, 90, null);
        showDao.toggleShowStatus(showNameCompleted, testUser1Id);
        log.debug("Added and marked as completed: {}", showNameCompleted);

        showDao.addShow(showNameInProgress, testUser1Id, 85, null);
        log.debug("Added as in-progress: {}", showNameInProgress);

        List<Show> userShows = showDao.getUserShows(testUser1Id);
        assertEquals(2, userShows.size(), "Should retrieve two shows for the user.");

        Show completedShow = userShows.stream().filter(s -> s.getName().equals(showNameCompleted)).findFirst().orElse(null);
        assertNotNull(completedShow, "Completed show should be found in the list.");
        assertTrue(completedShow.isCompleted(), "The '" + showNameCompleted + "' should have completed status as true.");
        assertEquals(90, completedShow.getScore());
        log.debug("Verified '{}', completed: {}, score: {}", completedShow.getName(), completedShow.isCompleted(), completedShow.getScore());

        Show inProgressShow = userShows.stream().filter(s -> s.getName().equals(showNameInProgress)).findFirst().orElse(null);
        assertNotNull(inProgressShow, "In-progress show should be found in the list.");
        assertFalse(inProgressShow.isCompleted(), "The '" + showNameInProgress + "' should have completed status as false.");
        assertEquals(85, inProgressShow.getScore());
        log.debug("Verified '{}', completed: {}, score: {}", inProgressShow.getName(), inProgressShow.isCompleted(), inProgressShow.getScore());
        log.info("TEST: testGetUserShows_ReturnsCorrectCompletedStatus - END");
    }

    @Test
    void testAddShow_NamesWithLeadingTrailingSpaces_TrimmedCorrectly() {
        log.info("TEST: testAddShow_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - START");
        String nameWithSpaces = "  Spaced Show Name Test  ";
        String expectedTrimmedName = "Spaced Show Name Test";
        int score = 77;

        showDao.addShow(nameWithSpaces, testUser1Id, score, null);
        log.debug("Attempted to add show with name: '{}'", nameWithSpaces);

        assertTrue(showDao.isShowExists(expectedTrimmedName, testUser1Id), "Show should exist with trimmed name.");
        assertTrue(showDao.isShowExists(nameWithSpaces, testUser1Id), "Show should also exist when checking with original spaced name.");

        List<Show> shows = showDao.getUserShows(testUser1Id);
        Show addedShow = shows.stream().filter(b -> b.getName().equals(expectedTrimmedName)).findFirst().orElse(null);
        assertNotNull(addedShow, "Added show with trimmed name not found.");
        assertEquals(expectedTrimmedName, addedShow.getName());
        assertEquals(score, addedShow.getScore());
        log.debug("Show found with name '{}' and score {}", addedShow.getName(), addedShow.getScore());

        Exception e = assertThrows(ItemAlreadyExistsException.class, () -> showDao.addShow(expectedTrimmedName, testUser1Id, score + 1, null));
        assertTrue(e.getMessage().contains("already exists") || e.getMessage().contains("Show"));
        log.debug("Attempt to add duplicate (trimmed) name threw expected exception: {}", e.getMessage());

        Exception e2 = assertThrows(ItemAlreadyExistsException.class, () -> showDao.addShow("  " + expectedTrimmedName + "  ", testUser1Id, score + 2, null));
        assertTrue(e2.getMessage().contains("already exists") || e2.getMessage().contains("Show"));
        log.debug("Attempt to add spaced name (which trims to duplicate) threw expected exception: {}", e2.getMessage());
        log.info("TEST: testAddShow_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - END");
    }

    @Test
    void testUpdateShowAndName_NamesWithLeadingTrailingSpaces_TrimmedCorrectly() {
        log.info("TEST: testUpdateShowAndName_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - START");
        String oldName = "OriginalTrimTestForUpdate";
        String newNameWithSpaces = "  Updated Trim Test For Update  ";
        String expectedTrimmedName = "Updated Trim Test For Update";
        int initialScore = 60;
        int updatedScore = 65;

        showDao.addShow(oldName, testUser1Id, initialScore, null);
        log.debug("Added initial show: '{}'", oldName);

        showDao.updateShowAndName(oldName, newNameWithSpaces, updatedScore, testUser1Id, null);
        log.debug("Attempted to update '{}' to '{}'", oldName, newNameWithSpaces);

        assertFalse(showDao.isShowExists(oldName, testUser1Id), "Old show name should not exist after update.");
        assertTrue(showDao.isShowExists(expectedTrimmedName, testUser1Id), "Show should exist with new trimmed name.");

        Show updatedShow = showDao.getUserShows(testUser1Id).stream()
                .filter(b -> b.getName().equals(expectedTrimmedName))
                .findFirst().orElse(null);
        assertNotNull(updatedShow, "Updated show with trimmed name not found.");
        assertEquals(expectedTrimmedName, updatedShow.getName());
        assertEquals(updatedScore, updatedShow.getScore());
        log.debug("Updated show found with name '{}' and score {}", updatedShow.getName(), updatedShow.getScore());

        String anotherShowName = "Another Show For Trim Update";
        showDao.addShow(anotherShowName, testUser1Id, 70, null);
        log.debug("Added another show: '{}'", anotherShowName);

        Exception e = assertThrows(ItemAlreadyExistsException.class,
                () -> showDao.updateShowAndName(expectedTrimmedName, "  " + anotherShowName + "  ", updatedScore + 5, testUser1Id, null));
        assertTrue(e.getMessage().contains("already exists") || e.getMessage().contains("Show"));
        log.debug("Attempt to update to a name that trims to a duplicate of another existing show threw: {}", e.getMessage());
        log.info("TEST: testUpdateShowAndName_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - END");
    }

    @Test
    void testFindShowsByUserId_NoFilters() {
        log.info("TEST: testFindShowsByUserId_NoFilters - START");
        showDao.addShow("Breaking Bad", testUser1Id, 90, null);
        showDao.addShow("Stranger Things", testUser1Id, 85, null);
        showDao.addShow("The Office", testUser2Id, 80, null);

        List<Show> shows = showDao.findShowsByUserId(testUser1Id, null, null, 0, 10, null, null);
        assertEquals(2, shows.size(), "Should find 2 shows for user 1");
        assertTrue(shows.stream().anyMatch(s -> s.getName().equals("Breaking Bad")), "Breaking Bad should be present");
        assertTrue(shows.stream().anyMatch(s -> s.getName().equals("Stranger Things")), "Stranger Things should be present");

        int count = showDao.countTotalForUser(testUser1Id, null, null);
        assertEquals(2, count, "Count should be 2 for user 1");
        log.info("TEST: testFindShowsByUserId_NoFilters - END");
    }
}
