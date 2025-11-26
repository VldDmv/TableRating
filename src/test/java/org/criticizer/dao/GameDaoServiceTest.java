package org.criticizer.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.criticizer.dao.game.GameDao;
import org.criticizer.dao.helper.DaoFactory;
import org.criticizer.dao.helper.DaoFactoryService;
import org.criticizer.dao.user.UserDao;
import org.criticizer.entity.Game;
import org.criticizer.entity.Role;
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
public class GameDaoServiceTest {
    private static final Logger log = LoggerFactory.getLogger(GameDaoServiceTest.class);

    private UserDao userDao;
    private GameDao gameDao;

    private int testUser1Id;
    private int testUser2Id;

    private static final String USER1_NAME = "gTestUser1_Games";
    private static final String USER2_NAME = "gTestUser2_Games";

    @BeforeAll
    void setUpAllTests() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test_game_db;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=FALSE");
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
        this.gameDao = daoFactory.getGameDao();
    }

    @BeforeEach
    void setUpData() {
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE;");
            stmt.execute("TRUNCATE TABLE game_tags RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE games RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE users RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE tags RESTART IDENTITY;");
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
    void testAddGame_Success() {
        log.info("TEST: testAddGame_Success - START");
        gameDao.addGame("The Witcher 3", testUser1Id, 95, null);
        assertTrue(gameDao.isGameExists("The Witcher 3", testUser1Id));
        List<Game> games = gameDao.getUserGames(testUser1Id);
        assertEquals(1, games.size());
        Game addedGame = games.get(0);
        assertEquals("The Witcher 3", addedGame.getName());
        assertEquals(95, addedGame.getScore());
        assertFalse(addedGame.isCompleted(), "New game should be not completed by default.");
        assertEquals(testUser1Id, addedGame.getUserId());
        log.info("TEST: testAddGame_Success - END");
    }

    @Test
    void testAddGame_DuplicateNameForSameUser_ThrowsItemAlreadyExistsException() {
        log.info("TEST: testAddGame_DuplicateNameForSameUser_ThrowsItemAlreadyExistsException - START");
        gameDao.addGame("Cyberpunk 2077", testUser1Id, 80, null);
        Exception exception = assertThrows(ItemAlreadyExistsException.class, () -> gameDao.addGame("Cyberpunk 2077", testUser1Id, 85, null));
        assertTrue(exception.getMessage().contains("already exists") || exception.getMessage().contains("Game"));
        log.info("TEST: testAddGame_DuplicateNameForSameUser_ThrowsItemAlreadyExistsException - END");
    }

    @Test
    void testAddGame_SameNameForDifferentUser_Success() {
        log.info("TEST: testAddGame_SameNameForDifferentUser_Success - START");
        gameDao.addGame("Skyrim", testUser1Id, 90, null);
        assertDoesNotThrow(() -> gameDao.addGame("Skyrim", testUser2Id, 88, null));
        assertTrue(gameDao.isGameExists("Skyrim", testUser1Id));
        assertTrue(gameDao.isGameExists("Skyrim", testUser2Id));
        log.info("TEST: testAddGame_SameNameForDifferentUser_Success - END");
    }

    @Test
    void testAddGame_InvalidScore_TooLow_ThrowsInvalidScoreException() {
        log.info("TEST: testAddGame_InvalidScore_TooLow_ThrowsInvalidScoreException - START");
        Exception exception = assertThrows(InvalidScoreException.class, () -> gameDao.addGame("Low Score Game", testUser1Id, 0, null));
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 100") || exception.getMessage().contains("1-100"));
        log.info("TEST: testAddGame_InvalidScore_TooLow_ThrowsInvalidScoreException - END");
    }

    @Test
    void testAddGame_InvalidScore_TooHigh_ThrowsInvalidScoreException() {
        log.info("TEST: testAddGame_InvalidScore_TooHigh_ThrowsInvalidScoreException - START");
        Exception exception = assertThrows(InvalidScoreException.class, () -> gameDao.addGame("High Score Game", testUser1Id, 101, null));
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 100") || exception.getMessage().contains("1-100"));
        log.info("TEST: testAddGame_InvalidScore_TooHigh_ThrowsInvalidScoreException - END");
    }

    @Test
    void testGetUserGames_NoGames_ReturnsEmptyList() {
        log.info("TEST: testGetUserGames_NoGames_ReturnsEmptyList - START");
        List<Game> games = gameDao.getUserGames(testUser1Id);
        assertTrue(games.isEmpty());
        log.info("TEST: testGetUserGames_NoGames_ReturnsEmptyList - END");
    }

    @Test
    void testGetUserGames_WithGames_ReturnsCorrectGames() {
        log.info("TEST: testGetUserGames_WithGames_ReturnsCorrectGames - START");
        gameDao.addGame("Game 1", testUser1Id, 70, null);
        gameDao.addGame("Game 2", testUser1Id, 80, null);

        List<Game> games = gameDao.getUserGames(testUser1Id);
        assertEquals(2, games.size());
        assertTrue(games.stream().anyMatch(g -> g.getName().equals("Game 1") && g.getScore() == 70 && g.getUserId() == testUser1Id));
        assertTrue(games.stream().anyMatch(g -> g.getName().equals("Game 2") && g.getScore() == 80 && g.getUserId() == testUser1Id));
        log.info("TEST: testGetUserGames_WithGames_ReturnsCorrectGames - END");
    }

    @Test
    void testGetUserGames_ForUserWithNoGames_WhenOtherUserHasGames() {
        log.info("TEST: testGetUserGames_ForUserWithNoGames_WhenOtherUserHasGames - START");
        gameDao.addGame("User2's Game", testUser2Id, 90, null);
        List<Game> user1Games = gameDao.getUserGames(testUser1Id);
        assertTrue(user1Games.isEmpty());
        log.info("TEST: testGetUserGames_ForUserWithNoGames_WhenOtherUserHasGames - END");
    }

    @Test
    void testRemoveGame_Success() {
        log.info("TEST: testRemoveGame_Success - START");
        gameDao.addGame("To Be Removed", testUser1Id, 50, null);
        assertTrue(gameDao.isGameExists("To Be Removed", testUser1Id));

        assertDoesNotThrow(() -> gameDao.removeGame("To Be Removed", testUser1Id));
        assertFalse(gameDao.isGameExists("To Be Removed", testUser1Id));
        log.info("TEST: testRemoveGame_Success - END");
    }

    @Test
    void testRemoveGame_NonExistentGame_ThrowsResourceNotFoundException() {
        log.info("TEST: testRemoveGame_NonExistentGame_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> gameDao.removeGame("Non Existent Game", testUser1Id));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Game"));
        log.info("TEST: testRemoveGame_NonExistentGame_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testIsGameExists_GameExists_ReturnsTrue() {
        log.info("TEST: testIsGameExists_GameExists_ReturnsTrue - START");
        gameDao.addGame("Existing Game", testUser1Id, 75, null);
        assertTrue(gameDao.isGameExists("Existing Game", testUser1Id));
        log.info("TEST: testIsGameExists_GameExists_ReturnsTrue - END");
    }

    @Test
    void testIsGameExists_GameDoesNotExist_ReturnsFalse() {
        log.info("TEST: testIsGameExists_GameDoesNotExist_ReturnsFalse - START");
        assertFalse(gameDao.isGameExists("Ghost Game", testUser1Id));
        log.info("TEST: testIsGameExists_GameDoesNotExist_ReturnsFalse - END");
    }

    @Test
    void testIsGameExists_GameExistsForDifferentUser_ReturnsFalse() {
        log.info("TEST: testIsGameExists_GameExistsForDifferentUser_ReturnsFalse - START");
        gameDao.addGame("Shared Name Game", testUser2Id, 80, null);
        assertFalse(gameDao.isGameExists("Shared Name Game", testUser1Id));
        log.info("TEST: testIsGameExists_GameExistsForDifferentUser_ReturnsFalse - END");
    }

    @Test
    void testUpdateGameAndName_Success_NameAndScoreChange() {
        log.info("TEST: testUpdateGameAndName_Success_NameAndScoreChange - START");
        gameDao.addGame("Old Name", testUser1Id, 60, null);
        assertDoesNotThrow(() -> gameDao.updateGameAndName("Old Name", "New Name", 70, testUser1Id, null));

        assertFalse(gameDao.isGameExists("Old Name", testUser1Id));
        assertTrue(gameDao.isGameExists("New Name", testUser1Id));
        List<Game> games = gameDao.getUserGames(testUser1Id);
        assertEquals(1, games.size());
        Game updatedGame = games.get(0);
        assertEquals("New Name", updatedGame.getName());
        assertEquals(70, updatedGame.getScore());
        assertEquals(testUser1Id, updatedGame.getUserId());
        log.info("TEST: testUpdateGameAndName_Success_NameAndScoreChange - END");
    }

    @Test
    void testUpdateGameAndName_Success_OnlyScoreChange() {
        log.info("TEST: testUpdateGameAndName_Success_OnlyScoreChange - START");
        gameDao.addGame("Constant Name", testUser1Id, 60, null);
        assertDoesNotThrow(() -> gameDao.updateGameAndName("Constant Name", "Constant Name", 70, testUser1Id, null));

        assertTrue(gameDao.isGameExists("Constant Name", testUser1Id));
        List<Game> games = gameDao.getUserGames(testUser1Id);
        assertEquals(1, games.size());
        Game updatedGame = games.get(0);
        assertEquals("Constant Name", updatedGame.getName());
        assertEquals(70, updatedGame.getScore());
        log.info("TEST: testUpdateGameAndName_Success_OnlyScoreChange - END");
    }

    @Test
    void testUpdateGameAndName_NonExistentGame_ThrowsResourceNotFoundException() {
        log.info("TEST: testUpdateGameAndName_NonExistentGame_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> gameDao.updateGameAndName("Phantom Game", "New Phantom", 50, testUser1Id, null));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Game"));
        log.info("TEST: testUpdateGameAndName_NonExistentGame_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testUpdateGameAndName_NewNameExistsForSameUser_ThrowsItemAlreadyExistsException() {
        log.info("TEST: testUpdateGameAndName_NewNameExistsForSameUser_ThrowsItemAlreadyExistsException - START");
        gameDao.addGame("Game A", testUser1Id, 50, null);
        gameDao.addGame("Game B", testUser1Id, 55, null);

        Exception exception = assertThrows(ItemAlreadyExistsException.class, () -> gameDao.updateGameAndName("Game A", "Game B", 60, testUser1Id, null));
        assertTrue(exception.getMessage().contains("already exists") || exception.getMessage().contains("Game"));

        log.info("TEST: testUpdateGameAndName_NewNameExistsForSameUser_ThrowsItemAlreadyExistsException - END");
    }

    @Test
    void testUpdateGameAndName_InvalidScore_ThrowsInvalidScoreException() {
        log.info("TEST: testUpdateGameAndName_InvalidScore_ThrowsInvalidScoreException - START");
        gameDao.addGame("Update Score Game", testUser1Id, 50, null);
        Exception exception = assertThrows(InvalidScoreException.class, () -> gameDao.updateGameAndName("Update Score Game", "Update Score Game New", 101, testUser1Id, null));
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 100") || exception.getMessage().contains("1-100"));

        log.info("TEST: testUpdateGameAndName_InvalidScore_ThrowsInvalidScoreException - END");
    }

    @Test
    void testUpdateGameAndName_EmptyOldName_ThrowsEmptyNameException() {
        log.info("TEST: testUpdateGameAndName_EmptyOldName_ThrowsEmptyNameException - START");
        Exception exception = assertThrows(EmptyNameException.class, () -> gameDao.updateGameAndName("  ", "New Name", 50, testUser1Id, null));
        assertTrue(exception.getMessage().contains("cannot be empty") || exception.getMessage().contains("Game"));
        log.info("TEST: testUpdateGameAndName_EmptyOldName_ThrowsEmptyNameException - END");
    }

    @Test
    void testUpdateGameAndName_EmptyNewName_ThrowsEmptyNameException() {
        log.info("TEST: testUpdateGameAndName_EmptyNewName_ThrowsEmptyNameException - START");
        gameDao.addGame("Valid Old Name", testUser1Id, 50, null);
        Exception exception = assertThrows(EmptyNameException.class, () -> gameDao.updateGameAndName("Valid Old Name", "  ", 50, testUser1Id, null));
        assertTrue(exception.getMessage().contains("cannot be empty") || exception.getMessage().contains("Game"));

        log.info("TEST: testUpdateGameAndName_EmptyNewName_ThrowsEmptyNameException - END");
    }

    @Test
    void testUpdateGameAndName_NoActualChange_CompletesSuccessfully() {
        log.info("TEST: testUpdateGameAndName_NoActualChange_CompletesSuccessfully - START");
        String gameName = "No Change Game";
        int gameScore = 77;
        gameDao.addGame(gameName, testUser1Id, gameScore, null);
        assertDoesNotThrow(() -> gameDao.updateGameAndName(gameName, gameName, gameScore, testUser1Id, null));
        List<Game> games = gameDao.getUserGames(testUser1Id);
        assertEquals(1, games.size());
        assertEquals(gameName, games.get(0).getName());
        assertEquals(gameScore, games.get(0).getScore());
        log.info("TEST: testUpdateGameAndName_NoActualChange_CompletesSuccessfully - END");
    }

    @Test
    void testGetGameStatus_GameExists_ReturnsCorrectStatus() {
        log.info("TEST: testGetGameStatus_GameExists_ReturnsCorrectStatus - START");
        gameDao.addGame("Status Game", testUser1Id, 80, null);
        assertFalse(gameDao.getGameStatus("Status Game", testUser1Id));
        log.info("TEST: testGetGameStatus_GameExists_ReturnsCorrectStatus - END");
    }

    @Test
    void testGetGameStatus_NonExistentGame_ThrowsResourceNotFoundException() {
        log.info("TEST: testGetGameStatus_NonExistentGame_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> gameDao.getGameStatus("Unknown Status Game", testUser1Id));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Game"));
        log.info("TEST: testGetGameStatus_NonExistentGame_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testToggleGameStatus_FromFalseToTrue() {
        log.info("TEST: testToggleGameStatus_FromFalseToTrue - START");
        gameDao.addGame("Toggle Me", testUser1Id, 70, null);
        assertFalse(gameDao.getGameStatus("Toggle Me", testUser1Id));

        assertDoesNotThrow(() -> gameDao.toggleGameStatus("Toggle Me", testUser1Id));
        assertTrue(gameDao.getGameStatus("Toggle Me", testUser1Id));
        log.info("TEST: testToggleGameStatus_FromFalseToTrue - END");
    }

    @Test
    void testToggleGameStatus_FromTrueToFalse() {
        log.info("TEST: testToggleGameStatus_FromTrueToFalse - START");
        gameDao.addGame("Toggle Me Twice", testUser1Id, 70, null);
        gameDao.toggleGameStatus("Toggle Me Twice", testUser1Id);
        assertTrue(gameDao.getGameStatus("Toggle Me Twice", testUser1Id));

        assertDoesNotThrow(() -> gameDao.toggleGameStatus("Toggle Me Twice", testUser1Id));
        assertFalse(gameDao.getGameStatus("Toggle Me Twice", testUser1Id));
        log.info("TEST: testToggleGameStatus_FromTrueToFalse - END");
    }

    @Test
    void testToggleGameStatus_NonExistentGame_ThrowsResourceNotFoundException() {
        log.info("TEST: testToggleGameStatus_NonExistentGame_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> gameDao.toggleGameStatus("Non Existent Toggle", testUser1Id));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Game"));
        log.info("TEST: testToggleGameStatus_NonExistentGame_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testDeleteGamesByUserId_UserHasGames_DeletesAllGames() {
        log.info("TEST: testDeleteGamesByUserId_UserHasGames_DeletesAllGames - START");
        gameDao.addGame("Game A del", testUser1Id, 50, null);
        gameDao.addGame("Game B del", testUser1Id, 60, null);
        assertEquals(2, gameDao.getUserGames(testUser1Id).size());

        assertDoesNotThrow(() -> gameDao.deleteGamesByUserId(testUser1Id));
        assertTrue(gameDao.getUserGames(testUser1Id).isEmpty());
        log.info("TEST: testDeleteGamesByUserId_UserHasGames_DeletesAllGames - END");
    }

    @Test
    void testDeleteGamesByUserId_UserHasNoGames_CompletesSilently() {
        log.info("TEST: testDeleteGamesByUserId_UserHasNoGames_CompletesSilently - START");
        assertTrue(gameDao.getUserGames(testUser1Id).isEmpty());
        assertDoesNotThrow(() -> gameDao.deleteGamesByUserId(testUser1Id));
        assertTrue(gameDao.getUserGames(testUser1Id).isEmpty());
        log.info("TEST: testDeleteGamesByUserId_UserHasNoGames_CompletesSilently - END");
    }

    @Test
    void testDeleteGamesByUserId_DoesNotDeleteGamesOfOtherUsers() {
        log.info("TEST: testDeleteGamesByUserId_DoesNotDeleteGamesOfOtherUsers - START");
        gameDao.addGame("User1 Game ToDelete", testUser1Id, 50, null);
        gameDao.addGame("User2 Game ToKeep", testUser2Id, 60, null);

        assertDoesNotThrow(() -> gameDao.deleteGamesByUserId(testUser1Id));

        assertTrue(gameDao.getUserGames(testUser1Id).isEmpty());
        List<Game> user2Games = gameDao.getUserGames(testUser2Id);
        assertEquals(1, user2Games.size());
        assertEquals("User2 Game ToKeep", user2Games.get(0).getName());
        assertEquals(testUser2Id, user2Games.get(0).getUserId());
        log.info("TEST: testDeleteGamesByUserId_DoesNotDeleteGamesOfOtherUsers - END");
    }

    @Test
    void testUserDeletion_CascadesToAssociatedGames() {
        log.info("TEST: testUserDeletion_CascadesToAssociatedGames - START");
        User cascadeTestUserEntity = new User(0, "CascadeDeleteUser_Games", "cascadePass123", Role.USER, false);
        userDao.addUser(cascadeTestUserEntity);
        cascadeTestUserEntity = userDao.findUserByName(cascadeTestUserEntity.getName());
        assertNotNull(cascadeTestUserEntity, "Cascade test user should be created and found.");

        long cascadeUserEntityId = cascadeTestUserEntity.getId();
        int cascadeUserGameDaoId = (int) cascadeUserEntityId;
        log.debug("Cascade test user created with ID_long: {}, ID_int: {}", cascadeUserEntityId, cascadeUserGameDaoId);

        gameDao.addGame("Cascade Game 1", cascadeUserGameDaoId, 70, null);
        gameDao.addGame("Cascade Game 2", cascadeUserGameDaoId, 75, null);
        log.debug("Added 2 games for cascade test user ID_int: {}", cascadeUserGameDaoId);

        List<Game> gamesBeforeDeletion = gameDao.getUserGames(cascadeUserGameDaoId);
        assertEquals(2, gamesBeforeDeletion.size(), "Cascade test user should have 2 games before user deletion.");

        log.debug("Attempting to delete cascade test user with ID_long: {}", cascadeUserEntityId);
        userDao.deleteUser((int) cascadeUserEntityId);
        log.debug("Cascade test user (ID_long: {}) deleted operation completed.", cascadeUserEntityId);

        List<Game> gamesAfterUserDeletion = gameDao.getUserGames(cascadeUserGameDaoId);
        assertTrue(gamesAfterUserDeletion.isEmpty(), "Games of the deleted user (ID_int: " + cascadeUserGameDaoId + ") should be removed by cascade.");
        log.debug("Checked games after user deletion for ID_int: {}, list is empty as expected.", cascadeUserGameDaoId);

        User userAfterDeletion = userDao.findUserByName(cascadeTestUserEntity.getName());
        assertNull(userAfterDeletion, "Cascade test user '" + cascadeTestUserEntity.getName() + "' should no longer exist in the database.");
        log.debug("Checked user existence after deletion, user is null as expected.");
        log.info("TEST: testUserDeletion_CascadesToAssociatedGames - END");
    }

    @Test
    void testUpdateGameAndName_DoesNotChangeCompletedStatus() {
        log.info("TEST: testUpdateGameAndName_DoesNotChangeCompletedStatus - START");
        String gameName = "StatusPreservationGame";
        int initialScore = 80;

        gameDao.addGame(gameName, testUser1Id, initialScore, null);
        Game game = gameDao.getUserGames(testUser1Id).stream().filter(g -> g.getName().equals(gameName)).findFirst().orElse(null);
        assertNotNull(game);
        assertFalse(game.isCompleted(), "Game should initially be not completed.");
        log.debug("Game '{}' added, completed status: {}", gameName, game.isCompleted());

        gameDao.toggleGameStatus(gameName, testUser1Id);
        game = gameDao.getUserGames(testUser1Id).stream().filter(g -> g.getName().equals(gameName)).findFirst().orElse(null);
        assertNotNull(game);
        assertTrue(game.isCompleted(), "Game status should be toggled to true.");
        log.debug("Game '{}' status toggled, completed status: {}", gameName, game.isCompleted());

        String updatedGameName = "UpdatedStatusPreservationGame";
        int updatedScore = 85;
        gameDao.updateGameAndName(gameName, updatedGameName, updatedScore, testUser1Id, null);
        log.debug("Game '{}' updated to '{}' with score {}", gameName, updatedGameName, updatedScore);

        Game updatedGame = gameDao.getUserGames(testUser1Id).stream().filter(g -> g.getName().equals(updatedGameName)).findFirst().orElse(null);
        assertNotNull(updatedGame, "Updated game should be found.");
        assertTrue(updatedGame.isCompleted(), "Completed status should remain true after name and score update.");
        assertEquals(updatedGameName, updatedGame.getName());
        assertEquals(updatedScore, updatedGame.getScore());
        log.debug("Game '{}' after update, completed status: {}", updatedGameName, updatedGame.isCompleted());

        gameDao.toggleGameStatus(updatedGameName, testUser1Id);
        updatedGame = gameDao.getUserGames(testUser1Id).stream().filter(g -> g.getName().equals(updatedGameName)).findFirst().orElse(null);
        assertNotNull(updatedGame);
        assertFalse(updatedGame.isCompleted(), "Game status should be toggled back to false.");
        log.debug("Game '{}' status toggled again, completed status: {}", updatedGameName, updatedGame.isCompleted());

        int finalScore = 90;
        gameDao.updateGameAndName(updatedGameName, updatedGameName, finalScore, testUser1Id, null);
        log.debug("Game '{}' score updated to {}", updatedGameName, finalScore);

        Game finalStateGame = gameDao.getUserGames(testUser1Id).stream().filter(g -> g.getName().equals(updatedGameName)).findFirst().orElse(null);
        assertNotNull(finalStateGame);
        assertFalse(finalStateGame.isCompleted(), "Completed status should remain false after score-only update.");
        assertEquals(updatedGameName, finalStateGame.getName());
        assertEquals(finalScore, finalStateGame.getScore());
        log.debug("Game '{}' after final update, completed status: {}", updatedGameName, finalStateGame.isCompleted());
        log.info("TEST: testUpdateGameAndName_DoesNotChangeCompletedStatus - END");
    }

    @Test
    void testGetUserGames_ReturnsCorrectCompletedStatus() {
        log.info("TEST: testGetUserGames_ReturnsCorrectCompletedStatus - START");
        String gameNameCompleted = "Completed Game Status Test";
        String gameNameInProgress = "In-Progress Game Status Test";

        gameDao.addGame(gameNameCompleted, testUser1Id, 90, null);
        gameDao.toggleGameStatus(gameNameCompleted, testUser1Id);
        log.debug("Added and marked as completed: {}", gameNameCompleted);

        gameDao.addGame(gameNameInProgress, testUser1Id, 85, null);
        log.debug("Added as in-progress: {}", gameNameInProgress);

        List<Game> userGames = gameDao.getUserGames(testUser1Id);
        assertEquals(2, userGames.size(), "Should retrieve two games for the user.");

        Game completedGame = userGames.stream().filter(g -> g.getName().equals(gameNameCompleted)).findFirst().orElse(null);
        assertNotNull(completedGame, "Completed game should be found in the list.");
        assertTrue(completedGame.isCompleted(), "The '" + gameNameCompleted + "' should have completed status as true.");
        assertEquals(90, completedGame.getScore());
        log.debug("Verified '{}', completed: {}, score: {}", completedGame.getName(), completedGame.isCompleted(), completedGame.getScore());

        Game inProgressGame = userGames.stream().filter(g -> g.getName().equals(gameNameInProgress)).findFirst().orElse(null);
        assertNotNull(inProgressGame, "In-progress game should be found in the list.");
        assertFalse(inProgressGame.isCompleted(), "The '" + gameNameInProgress + "' should have completed status as false.");
        assertEquals(85, inProgressGame.getScore());
        log.debug("Verified '{}', completed: {}, score: {}", inProgressGame.getName(), inProgressGame.isCompleted(), inProgressGame.getScore());
        log.info("TEST: testGetUserGames_ReturnsCorrectCompletedStatus - END");
    }


    @Test
    void testAddGame_NamesWithLeadingTrailingSpaces_TrimmedCorrectly() {
        log.info("TEST: testAddGame_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - START");
        String nameWithSpaces = "  Spaced Game Name Test  ";
        String expectedTrimmedName = "Spaced Game Name Test";
        int score = 77;

        gameDao.addGame(nameWithSpaces, testUser1Id, score, null);
        log.debug("Attempted to add game with name: '{}'", nameWithSpaces);

        assertTrue(gameDao.isGameExists(expectedTrimmedName, testUser1Id), "Game should exist with trimmed name.");
        assertTrue(gameDao.isGameExists(nameWithSpaces, testUser1Id), "Game should also exist when checking with original spaced name.");

        List<Game> games = gameDao.getUserGames(testUser1Id);
        Game addedGame = games.stream().filter(b -> b.getName().equals(expectedTrimmedName)).findFirst().orElse(null);
        assertNotNull(addedGame, "Added game with trimmed name not found.");
        assertEquals(expectedTrimmedName, addedGame.getName());
        assertEquals(score, addedGame.getScore());
        log.debug("Game found with name '{}' and score {}", addedGame.getName(), addedGame.getScore());

        Exception e = assertThrows(ItemAlreadyExistsException.class, () -> gameDao.addGame(expectedTrimmedName, testUser1Id, score + 1, null));
        assertTrue(e.getMessage().contains("already exists") || e.getMessage().contains("Game"));
        log.debug("Attempt to add duplicate (trimmed) name threw expected exception: {}", e.getMessage());

        Exception e2 = assertThrows(ItemAlreadyExistsException.class, () -> gameDao.addGame("  " + expectedTrimmedName + "  ", testUser1Id, score + 2, null));
        assertTrue(e2.getMessage().contains("already exists") || e2.getMessage().contains("Game"));
        log.debug("Attempt to add spaced name (which trims to duplicate) threw expected exception: {}", e2.getMessage());
        log.info("TEST: testAddGame_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - END");
    }

    @Test
    void testUpdateGameAndName_NamesWithLeadingTrailingSpaces_TrimmedCorrectly() {
        log.info("TEST: testAddGame_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - START");
        String oldName = "OriginalTrimTestForUpdate";
        String newNameWithSpaces = "  Updated Trim Test For Update  ";
        String expectedTrimmedName = "Updated Trim Test For Update";
        int initialScore = 60;
        int updatedScore = 65;

        gameDao.addGame(oldName, testUser1Id, initialScore, null);
        log.debug("Added initial game: '{}'", oldName);

        gameDao.updateGameAndName(oldName, newNameWithSpaces, updatedScore, testUser1Id, null);
        log.debug("Attempted to update '{}' to '{}'", oldName, newNameWithSpaces);

        assertFalse(gameDao.isGameExists(oldName, testUser1Id), "Old game name should not exist after update.");
        assertTrue(gameDao.isGameExists(expectedTrimmedName, testUser1Id), "Game should exist with new trimmed name.");

        Game updatedGame = gameDao.getUserGames(testUser1Id).stream()
                .filter(b -> b.getName().equals(expectedTrimmedName))
                .findFirst().orElse(null);
        assertNotNull(updatedGame, "Updated game with trimmed name not found.");
        assertEquals(expectedTrimmedName, updatedGame.getName());
        assertEquals(updatedScore, updatedGame.getScore());
        log.debug("Updated  found with name '{}' and score {}", updatedGame.getName(), updatedGame.getScore());

        String anotherGameName = "Another Game For Trim Update";
        gameDao.addGame(anotherGameName, testUser1Id, 70, null);
        log.debug("Added another game: '{}'", anotherGameName);

        Exception e = assertThrows(ItemAlreadyExistsException.class,
                () -> gameDao.updateGameAndName(expectedTrimmedName, "  " + anotherGameName + "  ", updatedScore + 5, testUser1Id, null));
        assertTrue(e.getMessage().contains("already exists") || e.getMessage().contains("Game"));
        log.debug("Attempt to update to a name that trims to a duplicate of another existing game threw: {}", e.getMessage());
        log.info("TEST: testUpdateGameAndName_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - END");
    }

}