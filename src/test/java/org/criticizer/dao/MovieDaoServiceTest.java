package org.criticizer.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.criticizer.dao.helper.DaoFactory;
import org.criticizer.dao.helper.DaoFactoryService;
import org.criticizer.dao.movie.MovieDao;
import org.criticizer.dao.user.UserDao;
import org.criticizer.entity.Movie;
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
public class MovieDaoServiceTest {
    private static final Logger log = LoggerFactory.getLogger(MovieDaoServiceTest.class);

    private UserDao userDao;
    private MovieDao movieDao;

    private int testUser1Id;
    private int testUser2Id;

    private static final String USER1_NAME = "mTestUser1_Movies";
    private static final String USER2_NAME = "mTestUser2_Movies";

    @BeforeAll
    void setUpAllTests() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test_movie_db;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=FALSE");
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
        this.movieDao = daoFactory.getMovieDao();
    }

    @BeforeEach
    void setUpData() {
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE;");
            stmt.execute("TRUNCATE TABLE movie_genres RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE movies RESTART IDENTITY;");
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
    void testAddMovie_Success() {
        log.info("TEST: testAddMovie_Success - START");
        movieDao.addMovie("Inception", testUser1Id, 95, null);
        assertTrue(movieDao.isMovieExists("Inception", testUser1Id));
        List<Movie> movies = movieDao.getUserMovies(testUser1Id);
        assertEquals(1, movies.size());
        Movie addedMovie = movies.get(0);
        assertEquals("Inception", addedMovie.getName());
        assertEquals(95, addedMovie.getScore());
        assertFalse(addedMovie.isCompleted(), "New movie should be not completed by default.");
        assertEquals(testUser1Id, addedMovie.getUserId());
        log.info("TEST: testAddMovie_Success - END");
    }

    @Test
    void testAddMovie_DuplicateNameForSameUser_ThrowsItemAlreadyExistsException() {
        log.info("TEST: testAddMovie_DuplicateNameForSameUser_ThrowsItemAlreadyExistsException - START");
        movieDao.addMovie("The Matrix", testUser1Id, 80, null);
        Exception exception = assertThrows(ItemAlreadyExistsException.class, () -> movieDao.addMovie("The Matrix", testUser1Id, 85, null));
        assertTrue(exception.getMessage().contains("already exists") || exception.getMessage().contains("Movie"));
        log.info("TEST: testAddMovie_DuplicateNameForSameUser_ThrowsItemAlreadyExistsException - END");
    }

    @Test
    void testAddMovie_SameNameForDifferentUser_Success() {
        log.info("TEST: testAddMovie_SameNameForDifferentUser_Success - START");
        movieDao.addMovie("Titanic", testUser1Id, 90, null);
        assertDoesNotThrow(() -> movieDao.addMovie("Titanic", testUser2Id, 88, null));
        assertTrue(movieDao.isMovieExists("Titanic", testUser1Id));
        assertTrue(movieDao.isMovieExists("Titanic", testUser2Id));
        log.info("TEST: testAddMovie_SameNameForDifferentUser_Success - END");
    }

    @Test
    void testAddMovie_InvalidScore_TooLow_ThrowsInvalidScoreException() {
        log.info("TEST: testAddMovie_InvalidScore_TooLow_ThrowsInvalidScoreException - START");
        Exception exception = assertThrows(InvalidScoreException.class, () -> movieDao.addMovie("Low Score Movie", testUser1Id, 0, null));
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 100") || exception.getMessage().contains("1-100"));
        log.info("TEST: testAddMovie_InvalidScore_TooLow_ThrowsInvalidScoreException - END");
    }

    @Test
    void testAddMovie_InvalidScore_TooHigh_ThrowsInvalidScoreException() {
        log.info("TEST: testAddMovie_InvalidScore_TooHigh_ThrowsInvalidScoreException - START");
        Exception exception = assertThrows(InvalidScoreException.class, () -> movieDao.addMovie("High Score Movie", testUser1Id, 101, null));
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 100") || exception.getMessage().contains("1-100"));
        log.info("TEST: testAddMovie_InvalidScore_TooHigh_ThrowsInvalidScoreException - END");
    }

    @Test
    void testGetUserMovies_NoMovies_ReturnsEmptyList() {
        log.info("TEST: testGetUserMovies_NoMovies_ReturnsEmptyList - START");
        List<Movie> movies = movieDao.getUserMovies(testUser1Id);
        assertTrue(movies.isEmpty());
        log.info("TEST: testGetUserMovies_NoMovies_ReturnsEmptyList - END");
    }

    @Test
    void testGetUserMovies_WithMovies_ReturnsCorrectMovies() {
        log.info("TEST: testGetUserMovies_WithMovies_ReturnsCorrectMovies - START");
        movieDao.addMovie("Movie 1", testUser1Id, 70, null);
        movieDao.addMovie("Movie 2", testUser1Id, 80, null);

        List<Movie> movies = movieDao.getUserMovies(testUser1Id);
        assertEquals(2, movies.size());
        assertTrue(movies.stream().anyMatch(m -> m.getName().equals("Movie 1") && m.getScore() == 70 && m.getUserId() == testUser1Id));
        assertTrue(movies.stream().anyMatch(m -> m.getName().equals("Movie 2") && m.getScore() == 80 && m.getUserId() == testUser1Id));
        log.info("TEST: testGetUserMovies_WithMovies_ReturnsCorrectMovies - END");
    }

    @Test
    void testGetUserMovies_ForUserWithNoMovies_WhenOtherUserHasMovies() {
        log.info("TEST: testGetUserMovies_ForUserWithNoMovies_WhenOtherUserHasMovies - START");
        movieDao.addMovie("User2's Movie", testUser2Id, 90, null);
        List<Movie> user1Movies = movieDao.getUserMovies(testUser1Id);
        assertTrue(user1Movies.isEmpty());
        log.info("TEST: testGetUserMovies_ForUserWithNoMovies_WhenOtherUserHasMovies - END");
    }

    @Test
    void testRemoveMovie_Success() {
        log.info("TEST: testRemoveMovie_Success - START");
        movieDao.addMovie("To Be Removed", testUser1Id, 50, null);
        assertTrue(movieDao.isMovieExists("To Be Removed", testUser1Id));

        assertDoesNotThrow(() -> movieDao.removeMovie("To Be Removed", testUser1Id));
        assertFalse(movieDao.isMovieExists("To Be Removed", testUser1Id));
        log.info("TEST: testRemoveMovie_Success - END");
    }

    @Test
    void testRemoveMovie_NonExistentMovie_ThrowsResourceNotFoundException() {
        log.info("TEST: testRemoveMovie_NonExistentMovie_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> movieDao.removeMovie("Non Existent Movie", testUser1Id));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Movie"));
        log.info("TEST: testRemoveMovie_NonExistentMovie_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testIsMovieExists_MovieExists_ReturnsTrue() {
        log.info("TEST: testIsMovieExists_MovieExists_ReturnsTrue - START");
        movieDao.addMovie("Existing Movie", testUser1Id, 75, null);
        assertTrue(movieDao.isMovieExists("Existing Movie", testUser1Id));
        log.info("TEST: testIsMovieExists_MovieExists_ReturnsTrue - END");
    }

    @Test
    void testIsMovieExists_MovieDoesNotExist_ReturnsFalse() {
        log.info("TEST: testIsMovieExists_MovieDoesNotExist_ReturnsFalse - START");
        assertFalse(movieDao.isMovieExists("Ghost Movie", testUser1Id));
        log.info("TEST: testIsMovieExists_MovieDoesNotExist_ReturnsFalse - END");
    }

    @Test
    void testIsMovieExists_MovieExistsForDifferentUser_ReturnsFalse() {
        log.info("TEST: testIsMovieExists_MovieExistsForDifferentUser_ReturnsFalse - START");
        movieDao.addMovie("Shared Name Movie", testUser2Id, 80, null);
        assertFalse(movieDao.isMovieExists("Shared Name Movie", testUser1Id));
        log.info("TEST: testIsMovieExists_MovieExistsForDifferentUser_ReturnsFalse - END");
    }

    @Test
    void testUpdateMovieAndName_Success_NameAndScoreChange() {
        log.info("TEST: testUpdateMovieAndName_Success_NameAndScoreChange - START");
        movieDao.addMovie("Old Name", testUser1Id, 60, null);
        assertDoesNotThrow(() -> movieDao.updateMovieAndName("Old Name", "New Name", 70, testUser1Id, null));

        assertFalse(movieDao.isMovieExists("Old Name", testUser1Id));
        assertTrue(movieDao.isMovieExists("New Name", testUser1Id));
        List<Movie> movies = movieDao.getUserMovies(testUser1Id);
        assertEquals(1, movies.size());
        Movie updatedMovie = movies.get(0);
        assertEquals("New Name", updatedMovie.getName());
        assertEquals(70, updatedMovie.getScore());
        assertEquals(testUser1Id, updatedMovie.getUserId());
        log.info("TEST: testUpdateMovieAndName_Success_NameAndScoreChange - END");
    }

    @Test
    void testUpdateMovieAndName_Success_OnlyScoreChange() {
        log.info("TEST: testUpdateMovieAndName_Success_OnlyScoreChange - START");
        movieDao.addMovie("Constant Name", testUser1Id, 60, null);
        assertDoesNotThrow(() -> movieDao.updateMovieAndName("Constant Name", "Constant Name", 70, testUser1Id, null));

        assertTrue(movieDao.isMovieExists("Constant Name", testUser1Id));
        List<Movie> movies = movieDao.getUserMovies(testUser1Id);
        assertEquals(1, movies.size());
        Movie updatedMovie = movies.get(0);
        assertEquals("Constant Name", updatedMovie.getName());
        assertEquals(70, updatedMovie.getScore());
        log.info("TEST: testUpdateMovieAndName_Success_OnlyScoreChange - END");
    }

    @Test
    void testUpdateMovieAndName_NonExistentMovie_ThrowsResourceNotFoundException() {
        log.info("TEST: testUpdateMovieAndName_NonExistentMovie_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> movieDao.updateMovieAndName("Phantom Movie", "New Phantom", 50, testUser1Id, null));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Movie"));
        log.info("TEST: testUpdateMovieAndName_NonExistentMovie_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testUpdateMovieAndName_NewNameExistsForSameUser_ThrowsItemAlreadyExistsException() {
        log.info("TEST: testUpdateMovieAndName_NewNameExistsForSameUser_ThrowsItemAlreadyExistsException - START");
        movieDao.addMovie("Movie A", testUser1Id, 50, null);
        movieDao.addMovie("Movie B", testUser1Id, 55, null);

        Exception exception = assertThrows(ItemAlreadyExistsException.class, () -> movieDao.updateMovieAndName("Movie A", "Movie B", 60, testUser1Id, null));
        assertTrue(exception.getMessage().contains("already exists") || exception.getMessage().contains("Movie"));
        log.info("TEST: testUpdateMovieAndName_NewNameExistsForSameUser_ThrowsItemAlreadyExistsException - END");
    }

    @Test
    void testUpdateMovieAndName_InvalidScore_ThrowsInvalidScoreException() {
        log.info("TEST: testUpdateMovieAndName_InvalidScore_ThrowsInvalidScoreException - START");
        movieDao.addMovie("Update Score Movie", testUser1Id, 50, null);
        Exception exception = assertThrows(InvalidScoreException.class, () -> movieDao.updateMovieAndName("Update Score Movie", "Update Score Movie New", 101, testUser1Id, null));
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 100") || exception.getMessage().contains("1-100"));
    }

    @Test
    void testUpdateMovieAndName_EmptyOldName_ThrowsEmptyNameException() {
        log.info("TEST: testUpdateMovieAndName_EmptyOldName_ThrowsEmptyNameException - START");
        Exception exception = assertThrows(EmptyNameException.class, () -> movieDao.updateMovieAndName("  ", "New Name", 50, testUser1Id, null));
        assertTrue(exception.getMessage().contains("cannot be empty") || exception.getMessage().contains("Movie"));
        log.info("TEST: testUpdateMovieAndName_EmptyOldName_ThrowsEmptyNameException - END");
    }

    @Test
    void testUpdateMovieAndName_EmptyNewName_ThrowsEmptyNameException() {
        log.info("TEST: testUpdateMovieAndName_EmptyNewName_ThrowsEmptyNameException - START");
        movieDao.addMovie("Valid Old Name", testUser1Id, 50, null);
        Exception exception = assertThrows(EmptyNameException.class, () -> movieDao.updateMovieAndName("Valid Old Name", "  ", 50, testUser1Id, null));
        assertTrue(exception.getMessage().contains("cannot be empty") || exception.getMessage().contains("Movie"));
        log.info("TEST: testUpdateMovieAndName_EmptyNewName_ThrowsEmptyNameException - END");
    }

    @Test
    void testUpdateMovieAndName_NoActualChange_CompletesSuccessfully() {
        log.info("TEST: testUpdateMovieAndName_NoActualChange_CompletesSuccessfully - START");
        String movieName = "No Change Movie";
        int movieScore = 77;
        movieDao.addMovie(movieName, testUser1Id, movieScore, null);
        assertDoesNotThrow(() -> movieDao.updateMovieAndName(movieName, movieName, movieScore, testUser1Id, null));
        List<Movie> movies = movieDao.getUserMovies(testUser1Id);
        assertEquals(1, movies.size());
        assertEquals(movieName, movies.get(0).getName());
        assertEquals(movieScore, movies.get(0).getScore());
        log.info("TEST: testUpdateMovieAndName_NoActualChange_CompletesSuccessfully - END");
    }

    @Test
    void testGetMovieStatus_MovieExists_ReturnsCorrectStatus() {
        log.info("TEST: testGetMovieStatus_MovieExists_ReturnsCorrectStatus - START");
        movieDao.addMovie("Status Movie", testUser1Id, 80, null);
        assertFalse(movieDao.getMovieStatus("Status Movie", testUser1Id));
        log.info("TEST: testGetMovieStatus_MovieExists_ReturnsCorrectStatus - END");
    }

    @Test
    void testGetMovieStatus_NonExistentMovie_ThrowsResourceNotFoundException() {
        log.info("TEST: testGetMovieStatus_NonExistentMovie_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> movieDao.getMovieStatus("Unknown Status Movie", testUser1Id));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Movie"));
        log.info("TEST: testGetMovieStatus_NonExistentMovie_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testToggleMovieStatus_FromFalseToTrue() {
        log.info("TEST: testToggleMovieStatus_FromFalseToTrue - START");
        movieDao.addMovie("Toggle Me", testUser1Id, 70, null);
        assertFalse(movieDao.getMovieStatus("Toggle Me", testUser1Id));

        assertDoesNotThrow(() -> movieDao.toggleMovieStatus("Toggle Me", testUser1Id));
        assertTrue(movieDao.getMovieStatus("Toggle Me", testUser1Id));
        log.info("TEST: testToggleMovieStatus_FromFalseToTrue - END");
    }

    @Test
    void testToggleMovieStatus_FromTrueToFalse() {
        log.info("TEST: testToggleMovieStatus_FromTrueToFalse - START");
        movieDao.addMovie("Toggle Me Twice", testUser1Id, 70, null);
        movieDao.toggleMovieStatus("Toggle Me Twice", testUser1Id);
        assertTrue(movieDao.getMovieStatus("Toggle Me Twice", testUser1Id));

        assertDoesNotThrow(() -> movieDao.toggleMovieStatus("Toggle Me Twice", testUser1Id));
        assertFalse(movieDao.getMovieStatus("Toggle Me Twice", testUser1Id));
        log.info("TEST: testToggleMovieStatus_FromTrueToFalse - END");
    }

    @Test
    void testToggleMovieStatus_NonExistentMovie_ThrowsResourceNotFoundException() {
        log.info("TEST: testToggleMovieStatus_NonExistentMovie_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> movieDao.toggleMovieStatus("Non Existent Toggle", testUser1Id));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Movie"));
        log.info("TEST: testToggleMovieStatus_NonExistentMovie_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testDeleteMoviesByUserId_UserHasMovies_DeletesAllMovies() {
        log.info("TEST: testDeleteMoviesByUserId_UserHasMovies_DeletesAllMovies - START");
        movieDao.addMovie("Movie A del", testUser1Id, 50, null);
        movieDao.addMovie("Movie B del", testUser1Id, 60, null);
        assertEquals(2, movieDao.getUserMovies(testUser1Id).size());

        assertDoesNotThrow(() -> movieDao.deleteMoviesByUserId(testUser1Id));
        assertTrue(movieDao.getUserMovies(testUser1Id).isEmpty());
        log.info("TEST: testDeleteMoviesByUserId_UserHasMovies_DeletesAllMovies - END");
    }

    @Test
    void testDeleteMoviesByUserId_UserHasNoMovies_CompletesSilently() {
        log.info("TEST: testDeleteMoviesByUserId_UserHasNoMovies_CompletesSilently - START");
        assertTrue(movieDao.getUserMovies(testUser1Id).isEmpty());
        assertDoesNotThrow(() -> movieDao.deleteMoviesByUserId(testUser1Id));
        assertTrue(movieDao.getUserMovies(testUser1Id).isEmpty());
        log.info("TEST: testDeleteMoviesByUserId_UserHasNoMovies_CompletesSilently - END");
    }

    @Test
    void testDeleteMoviesByUserId_DoesNotDeleteMoviesOfOtherUsers() {
        log.info("TEST: testDeleteMoviesByUserId_DoesNotDeleteMoviesOfOtherUsers - START");
        movieDao.addMovie("User1 Movie ToDelete", testUser1Id, 50, null);
        movieDao.addMovie("User2 Movie ToKeep", testUser2Id, 60, null);

        assertDoesNotThrow(() -> movieDao.deleteMoviesByUserId(testUser1Id));

        assertTrue(movieDao.getUserMovies(testUser1Id).isEmpty());
        List<Movie> user2Movies = movieDao.getUserMovies(testUser2Id);
        assertEquals(1, user2Movies.size());
        assertEquals("User2 Movie ToKeep", user2Movies.get(0).getName());
        assertEquals(testUser2Id, user2Movies.get(0).getUserId());
        log.info("TEST: testDeleteMoviesByUserId_DoesNotDeleteMoviesOfOtherUsers - END");
    }

    @Test
    void testUserDeletion_CascadesToAssociatedMovies() {
        log.info("TEST: testUserDeletion_CascadesToAssociatedMovies - START");
        User cascadeTestUserEntity = new User(0, "CascadeDeleteUser_Movies", "cascadePass123", Role.USER, false);
        userDao.addUser(cascadeTestUserEntity);
        cascadeTestUserEntity = userDao.findUserByName(cascadeTestUserEntity.getName());
        assertNotNull(cascadeTestUserEntity, "Cascade test user should be created and found.");

        long cascadeUserEntityId = cascadeTestUserEntity.getId();
        int cascadeUserMovieDaoId = (int) cascadeUserEntityId;
        log.debug("Cascade test user created with ID_long: {}, ID_int: {}", cascadeUserEntityId, cascadeUserMovieDaoId);

        movieDao.addMovie("Cascade Movie 1", cascadeUserMovieDaoId, 70, null);
        movieDao.addMovie("Cascade Movie 2", cascadeUserMovieDaoId, 75, null);
        log.debug("Added 2 movies for cascade test user ID_int: {}", cascadeUserMovieDaoId);

        List<Movie> moviesBeforeDeletion = movieDao.getUserMovies(cascadeUserMovieDaoId);
        assertEquals(2, moviesBeforeDeletion.size(), "Cascade test user should have 2 movies before user deletion.");

        log.debug("Attempting to delete cascade test user with ID_long: {}", cascadeUserEntityId);
        userDao.deleteUser((int) cascadeUserEntityId);
        log.debug("Cascade test user (ID_long: {}) deleted operation completed.", cascadeUserEntityId);

        List<Movie> moviesAfterUserDeletion = movieDao.getUserMovies(cascadeUserMovieDaoId);
        assertTrue(moviesAfterUserDeletion.isEmpty(), "Movies of the deleted user (ID_int: " + cascadeUserMovieDaoId + ") should be removed by cascade.");
        log.debug("Checked movies after user deletion for ID_int: {}, list is empty as expected.", cascadeUserMovieDaoId);

        User userAfterDeletion = userDao.findUserByName(cascadeTestUserEntity.getName());
        assertNull(userAfterDeletion, "Cascade test user '" + cascadeTestUserEntity.getName() + "' should no longer exist in the database.");
        log.debug("Checked user existence after deletion, user is null as expected.");
        log.info("TEST: testUserDeletion_CascadesToAssociatedMovies - END");
    }

    @Test
    void testUpdateMovieAndName_DoesNotChangeCompletedStatus() {
        log.info("TEST: testUpdateMovieAndName_DoesNotChangeCompletedStatus - START");
        String movieName = "StatusPreservationMovie";
        int initialScore = 80;

        movieDao.addMovie(movieName, testUser1Id, initialScore, null);
        Movie movie = movieDao.getUserMovies(testUser1Id).stream().filter(m -> m.getName().equals(movieName)).findFirst().orElse(null);
        assertNotNull(movie);
        assertFalse(movie.isCompleted(), "Movie should initially be not completed.");
        log.debug("Movie '{}' added, completed status: {}", movieName, movie.isCompleted());

        movieDao.toggleMovieStatus(movieName, testUser1Id);
        movie = movieDao.getUserMovies(testUser1Id).stream().filter(m -> m.getName().equals(movieName)).findFirst().orElse(null);
        assertNotNull(movie);
        assertTrue(movie.isCompleted(), "Movie status should be toggled to true.");
        log.debug("Movie '{}' status toggled, completed status: {}", movieName, movie.isCompleted());

        String updatedMovieName = "UpdatedStatusPreservationMovie";
        int updatedScore = 85;
        movieDao.updateMovieAndName(movieName, updatedMovieName, updatedScore, testUser1Id, null);
        log.debug("Movie '{}' updated to '{}' with score {}", movieName, updatedMovieName, updatedScore);

        Movie updatedMovie = movieDao.getUserMovies(testUser1Id).stream().filter(m -> m.getName().equals(updatedMovieName)).findFirst().orElse(null);
        assertNotNull(updatedMovie, "Updated movie should be found.");
        assertTrue(updatedMovie.isCompleted(), "Completed status should remain true after name and score update.");
        assertEquals(updatedMovieName, updatedMovie.getName());
        assertEquals(updatedScore, updatedMovie.getScore());
        log.debug("Movie '{}' after update, completed status: {}", updatedMovieName, updatedMovie.isCompleted());

        movieDao.toggleMovieStatus(updatedMovieName, testUser1Id);
        updatedMovie = movieDao.getUserMovies(testUser1Id).stream().filter(m -> m.getName().equals(updatedMovieName)).findFirst().orElse(null);
        assertNotNull(updatedMovie);
        assertFalse(updatedMovie.isCompleted(), "Movie status should be toggled back to false.");
        log.debug("Movie '{}' status toggled again, completed status: {}", updatedMovieName, updatedMovie.isCompleted());

        int finalScore = 90;
        movieDao.updateMovieAndName(updatedMovieName, updatedMovieName, finalScore, testUser1Id, null);
        log.debug("Movie '{}' score updated to {}", updatedMovieName, finalScore);

        Movie finalStateMovie = movieDao.getUserMovies(testUser1Id).stream().filter(m -> m.getName().equals(updatedMovieName)).findFirst().orElse(null);
        assertNotNull(finalStateMovie);
        assertFalse(finalStateMovie.isCompleted(), "Completed status should remain false after score-only update.");
        assertEquals(updatedMovieName, finalStateMovie.getName());
        assertEquals(finalScore, finalStateMovie.getScore());
        log.debug("Movie '{}' after final update, completed status: {}", updatedMovieName, finalStateMovie.isCompleted());
        log.info("TEST: testUpdateMovieAndName_DoesNotChangeCompletedStatus - END");
    }

    @Test
    void testGetUserMovies_ReturnsCorrectCompletedStatus() {
        log.info("TEST: testGetUserMovies_ReturnsCorrectCompletedStatus - START");
        String movieNameCompleted = "Completed Movie Status Test";
        String movieNameInProgress = "In-Progress Movie Status Test";

        movieDao.addMovie(movieNameCompleted, testUser1Id, 90, null);
        movieDao.toggleMovieStatus(movieNameCompleted, testUser1Id);
        log.debug("Added and marked as completed: {}", movieNameCompleted);

        movieDao.addMovie(movieNameInProgress, testUser1Id, 85, null);
        log.debug("Added as in-progress: {}", movieNameInProgress);

        List<Movie> userMovies = movieDao.getUserMovies(testUser1Id);
        assertEquals(2, userMovies.size(), "Should retrieve two movies for the user.");

        Movie completedMovie = userMovies.stream().filter(m -> m.getName().equals(movieNameCompleted)).findFirst().orElse(null);
        assertNotNull(completedMovie, "Completed movie should be found in the list.");
        assertTrue(completedMovie.isCompleted(), "The '" + movieNameCompleted + "' should have completed status as true.");
        assertEquals(90, completedMovie.getScore());
        log.debug("Verified '{}', completed: {}, score: {}", completedMovie.getName(), completedMovie.isCompleted(), completedMovie.getScore());

        Movie inProgressMovie = userMovies.stream().filter(m -> m.getName().equals(movieNameInProgress)).findFirst().orElse(null);
        assertNotNull(inProgressMovie, "In-progress movie should be found in the list.");
        assertFalse(inProgressMovie.isCompleted(), "The '" + movieNameInProgress + "' should have completed status as false.");
        assertEquals(85, inProgressMovie.getScore());
        log.debug("Verified '{}', completed: {}, score: {}", inProgressMovie.getName(), inProgressMovie.isCompleted(), inProgressMovie.getScore());
        log.info("TEST: testGetUserMovies_ReturnsCorrectCompletedStatus - END");
    }

    @Test
    void testAddMovie_NamesWithLeadingTrailingSpaces_TrimmedCorrectly() {
        log.info("TEST: testAddMovie_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - START");
        String nameWithSpaces = "  Spaced Movie Name Test  ";
        String expectedTrimmedName = "Spaced Movie Name Test";
        int score = 77;

        movieDao.addMovie(nameWithSpaces, testUser1Id, score, null);
        log.debug("Attempted to add movie with name: '{}'", nameWithSpaces);

        assertTrue(movieDao.isMovieExists(expectedTrimmedName, testUser1Id), "Movie should exist with trimmed name.");
        assertTrue(movieDao.isMovieExists(nameWithSpaces, testUser1Id), "Movie should also exist when checking with original spaced name.");

        List<Movie> movies = movieDao.getUserMovies(testUser1Id);
        Movie addedMovie = movies.stream().filter(b -> b.getName().equals(expectedTrimmedName)).findFirst().orElse(null);
        assertNotNull(addedMovie, "Added movie with trimmed name not found.");
        assertEquals(expectedTrimmedName, addedMovie.getName());
        assertEquals(score, addedMovie.getScore());
        log.debug("Movie found with name '{}' and score {}", addedMovie.getName(), addedMovie.getScore());

        Exception e = assertThrows(ItemAlreadyExistsException.class, () -> movieDao.addMovie(expectedTrimmedName, testUser1Id, score + 1, null));
        assertTrue(e.getMessage().contains("already exists") || e.getMessage().contains("Movie"));
        log.debug("Attempt to add duplicate (trimmed) name threw expected exception: {}", e.getMessage());

        Exception e2 = assertThrows(ItemAlreadyExistsException.class, () -> movieDao.addMovie("  " + expectedTrimmedName + "  ", testUser1Id, score + 2, null));
        assertTrue(e2.getMessage().contains("already exists") || e2.getMessage().contains("Movie"));
        log.debug("Attempt to add spaced name (which trims to duplicate) threw expected exception: {}", e2.getMessage());
        log.info("TEST: testAddMovie_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - END");
    }


    @Test
    void testUpdateMovieAndName_NamesWithLeadingTrailingSpaces_TrimmedCorrectly() {
        log.info("TEST: testUpdateMovieAndName_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - START");
        String oldName = "OriginalTrimTestForUpdate";
        String newNameWithSpaces = "  Updated Trim Test For Update  ";
        String expectedTrimmedName = "Updated Trim Test For Update";
        int initialScore = 60;
        int updatedScore = 65;

        movieDao.addMovie(oldName, testUser1Id, initialScore, null);
        log.debug("Added initial movie: '{}'", oldName);

        movieDao.updateMovieAndName(oldName, newNameWithSpaces, updatedScore, testUser1Id, null);
        log.debug("Attempted to update '{}' to '{}'", oldName, newNameWithSpaces);

        assertFalse(movieDao.isMovieExists(oldName, testUser1Id), "Old movie name should not exist after update.");
        assertTrue(movieDao.isMovieExists(expectedTrimmedName, testUser1Id), "Movie should exist with new trimmed name.");

        Movie updatedMovie = movieDao.getUserMovies(testUser1Id).stream()
                .filter(b -> b.getName().equals(expectedTrimmedName))
                .findFirst().orElse(null);
        assertNotNull(updatedMovie, "Updated movie with trimmed name not found.");
        assertEquals(expectedTrimmedName, updatedMovie.getName());
        assertEquals(updatedScore, updatedMovie.getScore());
        log.debug("Updated movie found with name '{}' and score {}", updatedMovie.getName(), updatedMovie.getScore());

        String anotherMovieName = "Another Movie For Trim Update";
        movieDao.addMovie(anotherMovieName, testUser1Id, 70, null);
        log.debug("Added another movie: '{}'", anotherMovieName);

        Exception e = assertThrows(ItemAlreadyExistsException.class,
                () -> movieDao.updateMovieAndName(expectedTrimmedName, "  " + anotherMovieName + "  ", updatedScore + 5, testUser1Id, null));
        assertTrue(e.getMessage().contains("already exists") || e.getMessage().contains("Movie"));
        log.debug("Attempt to update to a name that trims to a duplicate of another existing movie threw: {}", e.getMessage());
        log.info("TEST: testUpdateMovieAndName_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - END");
    }

    @Test
    void testFindMoviesByUserId_NoFilters() {
        log.info("TEST: testFindMoviesByUserId_NoFilters - START");
        movieDao.addMovie("The Matrix", testUser1Id, 90, null);
        movieDao.addMovie("Interstellar", testUser1Id, 85, null);
        movieDao.addMovie("Titanic", testUser2Id, 80, null);

        List<Movie> movies = movieDao.findMoviesByUserId(testUser1Id, null, null, 0, 10, null, null);
        assertEquals(2, movies.size(), "Should find 2 movies for user 1");
        assertTrue(movies.stream().anyMatch(m -> m.getName().equals("The Matrix")), "The Matrix should be present");
        assertTrue(movies.stream().anyMatch(m -> m.getName().equals("Interstellar")), "Interstellar should be present");

        int count = movieDao.countTotalForUser(testUser1Id, null, null);
        assertEquals(2, count, "Count should be 2 for user 1");
        log.info("TEST: testFindMoviesByUserId_NoFilters - END");
    }
}