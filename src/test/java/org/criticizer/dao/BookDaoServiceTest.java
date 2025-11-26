package org.criticizer.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.criticizer.dao.book.BookDao;
import org.criticizer.dao.helper.DaoFactory;
import org.criticizer.dao.helper.DaoFactoryService;
import org.criticizer.dao.user.UserDao;
import org.criticizer.entity.Book;
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
public class BookDaoServiceTest {
    private static final Logger log = LoggerFactory.getLogger(BookDaoServiceTest.class);

    private UserDao userDao;
    private BookDao bookDao;

    private int testUser1Id;
    private int testUser2Id;

    private static final String USER1_NAME = "bTestUser1_Books";
    private static final String USER2_NAME = "bTestUser2_Books";

    @BeforeAll
    void setUpAllTests() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test_book_db;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=FALSE");
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
        this.bookDao = daoFactory.getBookDao();
    }

    @BeforeEach
    void setUpData() {
        try (Connection conn = DataSourceProvider.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE;");
            stmt.execute("TRUNCATE TABLE book_genres RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE books RESTART IDENTITY;");
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
    void testAddBook_Success() {
        log.info("TEST: testAddBook_Success - START");
        bookDao.addBook("1984", testUser1Id, 95, null);
        assertTrue(bookDao.isBookExists("1984", testUser1Id));
        List<Book> books = bookDao.getUserBooks(testUser1Id);
        assertEquals(1, books.size());
        Book addedBook = books.get(0);
        assertEquals("1984", addedBook.getName());
        assertEquals(95, addedBook.getScore());
        assertFalse(addedBook.isCompleted(), "New book should be not completed by default.");
        assertEquals(testUser1Id, addedBook.getUserId());
        log.info("TEST: testAddBook_Success - END");
    }

    @Test
    void testAddBook_DuplicateNameForSameUser_ThrowsItemAlreadyExistsException() {
        log.info("TEST: testAddBook_DuplicateNameForSameUser_ThrowsItemAlreadyExistsException - START");
        bookDao.addBook("Dune", testUser1Id, 80, null);
        Exception exception = assertThrows(ItemAlreadyExistsException.class, () -> bookDao.addBook("Dune", testUser1Id, 85, null));
        assertTrue(exception.getMessage().contains("already exists") || exception.getMessage().contains("Book"));
        log.info("TEST: testAddBook_DuplicateNameForSameUser_ThrowsItemAlreadyExistsException - END");
    }

    @Test
    void testAddBook_SameNameForDifferentUser_Success() {
        log.info("TEST: testAddBook_SameNameForDifferentUser_Success - START");
        bookDao.addBook("Pride and Prejudice", testUser1Id, 90, null);
        assertDoesNotThrow(() -> bookDao.addBook("Pride and Prejudice", testUser2Id, 88, null));
        assertTrue(bookDao.isBookExists("Pride and Prejudice", testUser1Id));
        assertTrue(bookDao.isBookExists("Pride and Prejudice", testUser2Id));
        log.info("TEST: testAddBook_SameNameForDifferentUser_Success - END");
    }

    @Test
    void testAddBook_InvalidScore_TooLow_ThrowsInvalidScoreException() {
        log.info("TEST: testAddBook_InvalidScore_TooLow_ThrowsInvalidScoreException - START");
        Exception exception = assertThrows(InvalidScoreException.class, () -> bookDao.addBook("Low Score Book", testUser1Id, 0, null));
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 100") || exception.getMessage().contains("1-100"));
        log.info("TEST: testAddBook_InvalidScore_TooLow_ThrowsInvalidScoreException - END");
    }

    @Test
    void testAddBook_InvalidScore_TooHigh_ThrowsInvalidScoreException() {
        log.info("TEST: testAddBook_InvalidScore_TooHigh_ThrowsInvalidScoreException - START");
        Exception exception = assertThrows(InvalidScoreException.class, () -> bookDao.addBook("High Score Book", testUser1Id, 101, null));
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 100") || exception.getMessage().contains("1-100"));
        log.info("TEST: testAddBook_TooHigh_ThrowsInvalidScoreException - END");
    }

    @Test
    void testGetUserBooks_NoBooks_ReturnsEmptyList() {
        log.info("TEST: testGetUserBooks_NoBooks_ReturnsEmptyList - START");
        List<Book> books = bookDao.getUserBooks(testUser1Id);
        assertTrue(books.isEmpty());
        log.info("TEST: testGetUserBooks_NoBooks_ReturnsEmptyList - END");
    }

    @Test
    void testGetUserBooks_WithBooks_ReturnsCorrectBooks() {
        log.info("TEST: testGetUserBooks_WithBooks_ReturnsCorrectBooks - START");
        bookDao.addBook("Book 1", testUser1Id, 70, null);
        bookDao.addBook("Book 2", testUser1Id, 80, null);

        List<Book> books = bookDao.getUserBooks(testUser1Id);
        assertEquals(2, books.size());
        assertTrue(books.stream().anyMatch(b -> b.getName().equals("Book 1") && b.getScore() == 70 && b.getUserId() == testUser1Id));
        assertTrue(books.stream().anyMatch(b -> b.getName().equals("Book 2") && b.getScore() == 80 && b.getUserId() == testUser1Id));
        log.info("TEST: testGetUserBooks_WithBooks_ReturnsCorrectBooks - END");
    }

    @Test
    void testGetUserBooks_ForUserWithNoBooks_WhenOtherUserHasBooks() {
        log.info("TEST: testGetUserBooks_ForUserWithNoBooks_WhenOtherUserHasBooks - START");
        bookDao.addBook("User2's Book", testUser2Id, 90, null);
        List<Book> user1Books = bookDao.getUserBooks(testUser1Id);
        assertTrue(user1Books.isEmpty());
        log.info("TEST: testGetUserBooks_ForUserWithNoBooks_WhenOtherUserHasBooks - END");
    }

    @Test
    void testRemoveBook_Success() {
        log.info("TEST: testRemoveBook_Success - START");
        bookDao.addBook("To Be Removed", testUser1Id, 50, null);
        assertTrue(bookDao.isBookExists("To Be Removed", testUser1Id));

        assertDoesNotThrow(() -> bookDao.removeBook("To Be Removed", testUser1Id));
        assertFalse(bookDao.isBookExists("To Be Removed", testUser1Id));
        log.info("TEST: testRemoveBook_Success - END");
    }

    @Test
    void testRemoveBook_NonExistentBook_ThrowsResourceNotFoundException() {
        log.info("TEST: testRemoveBook_NonExistentBook_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> bookDao.removeBook("Non Existent Book", testUser1Id));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Book"));
        log.info("TEST: testRemoveBook_NonExistentBook_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testIsBookExists_BookExists_ReturnsTrue() {
        log.info("TEST: testIsBookExists_BookExists_ReturnsTrue - START");
        bookDao.addBook("Existing Book", testUser1Id, 75, null);
        assertTrue(bookDao.isBookExists("Existing Book", testUser1Id));
        log.info("TEST: testIsBookExists_BookExists_ReturnsTrue - END");
    }

    @Test
    void testIsBookExists_BookDoesNotExist_ReturnsFalse() {
        log.info("TEST: testIsBookExists_BookDoesNotExist_ReturnsFalse - START");
        assertFalse(bookDao.isBookExists("Ghost Book", testUser1Id));
        log.info("TEST: testIsBookExists_BookDoesNotExist_ReturnsFalse - END");
    }

    @Test
    void testIsBookExists_BookExistsForDifferentUser_ReturnsFalse() {
        log.info("TEST: testIsBookExists_BookExistsForDifferentUser_ReturnsFalse - START");
        bookDao.addBook("Shared Name Book", testUser2Id, 80, null);
        assertFalse(bookDao.isBookExists("Shared Name Book", testUser1Id));
        log.info("TEST: testIsBookExists_BookExistsForDifferentUser_ReturnsFalse - END");
    }

    @Test
    void testUpdateBookAndName_Success_NameAndScoreChange() {
        log.info("TEST: testUpdateBookAndName_Success_NameAndScoreChange - START");
        bookDao.addBook("Old Name", testUser1Id, 60, null);
        assertDoesNotThrow(() -> bookDao.updateBookAndName("Old Name", "New Name", 70, testUser1Id, null));

        assertFalse(bookDao.isBookExists("Old Name", testUser1Id));
        assertTrue(bookDao.isBookExists("New Name", testUser1Id));
        List<Book> books = bookDao.getUserBooks(testUser1Id);
        assertEquals(1, books.size());
        Book updatedBook = books.get(0);
        assertEquals("New Name", updatedBook.getName());
        assertEquals(70, updatedBook.getScore());
        assertEquals(testUser1Id, updatedBook.getUserId());
        log.info("TEST: testUpdateBookAndName_Success_NameAndScoreChange - END");
    }

    @Test
    void testUpdateBookAndName_Success_OnlyScoreChange() {
        log.info("TEST: testUpdateBookAndName_Success_OnlyScoreChange - START");
        bookDao.addBook("Constant Name", testUser1Id, 60, null);
        assertDoesNotThrow(() -> bookDao.updateBookAndName("Constant Name", "Constant Name", 70, testUser1Id, null));

        assertTrue(bookDao.isBookExists("Constant Name", testUser1Id));
        List<Book> books = bookDao.getUserBooks(testUser1Id);
        assertEquals(1, books.size());
        Book updatedBook = books.get(0);
        assertEquals("Constant Name", updatedBook.getName());
        assertEquals(70, updatedBook.getScore());
        log.info("TEST: testUpdateBookAndName_Success_OnlyScoreChange - END");
    }

    @Test
    void testUpdateBookAndName_NonExistentBook_ThrowsResourceNotFoundException() {
        log.info("TEST: testUpdateBookAndName_NonExistentBook_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> bookDao.updateBookAndName("Phantom Book", "New Phantom", 50, testUser1Id, null));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Book"));
        log.info("TEST: testUpdateBookAndName_NonExistentBook_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testUpdateBookAndName_NewNameExistsForSameUser_ThrowsItemAlreadyExistsException() {
        log.info("TEST: testUpdateBookAndName_NewNameExistsForSameUser_ThrowsItemAlreadyExistsException - START");
        bookDao.addBook("Book A", testUser1Id, 50, null);
        bookDao.addBook("Book B", testUser1Id, 55, null);

        Exception exception = assertThrows(ItemAlreadyExistsException.class, () -> bookDao.updateBookAndName("Book A", "Book B", 60, testUser1Id, null));
        assertTrue(exception.getMessage().contains("already exists") || exception.getMessage().contains("Book"));
        log.info("TEST: testUpdateBookAndName_NewNameExistsForSameUser_ThrowsItemAlreadyExistsException - END");
    }

    @Test
    void testUpdateBookAndName_InvalidScore_ThrowsInvalidScoreException() {
        log.info("TEST: testUpdateBookAndName_InvalidScore_ThrowsInvalidScoreException - START");
        bookDao.addBook("Update Score Book", testUser1Id, 50, null);
        Exception exception = assertThrows(InvalidScoreException.class, () -> bookDao.updateBookAndName("Update Score Book", "Update Score Book New", 101, testUser1Id, null));
        assertTrue(exception.getMessage().contains("Rating must be between 1 and 100") || exception.getMessage().contains("1-100"));
        log.info("TEST: testUpdateBookAndName_InvalidScore_ThrowsInvalidScoreException - END");
    }

    @Test
    void testUpdateBookAndName_EmptyOldName_ThrowsEmptyNameException() {
        log.info("TEST: testUpdateBookAndName_EmptyOldName_ThrowsEmptyNameException - START");
        Exception exception = assertThrows(EmptyNameException.class, () -> bookDao.updateBookAndName("  ", "New Name", 50, testUser1Id, null));
        assertTrue(exception.getMessage().contains("cannot be empty") || exception.getMessage().contains("Book"));
        log.info("TEST: testUpdateBookAndName_EmptyOldName_ThrowsEmptyNameException - END");
    }

    @Test
    void testUpdateBookAndName_EmptyNewName_ThrowsEmptyNameException() {
        log.info("TEST: testUpdateBookAndName_EmptyNewName_ThrowsEmptyNameException - START");
        bookDao.addBook("Valid Old Name", testUser1Id, 50, null);
        Exception exception = assertThrows(EmptyNameException.class, () -> bookDao.updateBookAndName("Valid Old Name", "  ", 50, testUser1Id, null));
        assertTrue(exception.getMessage().contains("cannot be empty") || exception.getMessage().contains("Book"));
        log.info("TEST: testUpdateBookAndName_EmptyNewName_ThrowsEmptyNameException - END");
    }

    @Test
    void testUpdateBookAndName_NoActualChange_CompletesSuccessfully() {
        log.info("TEST: testUpdateBookAndName_NoActualChange_CompletesSuccessfully - START");
        String bookName = "No Change Book";
        int bookScore = 77;
        bookDao.addBook(bookName, testUser1Id, bookScore, null);
        assertDoesNotThrow(() -> bookDao.updateBookAndName(bookName, bookName, bookScore, testUser1Id, null));
        List<Book> books = bookDao.getUserBooks(testUser1Id);
        assertEquals(1, books.size());
        assertEquals(bookName, books.get(0).getName());
        assertEquals(bookScore, books.get(0).getScore());
        log.info("TEST: testUpdateBookAndName_NoActualChange_CompletesSuccessfully - END");
    }

    @Test
    void testGetBookStatus_BookExists_ReturnsCorrectStatus() {
        log.info("TEST: testGetBookStatus_BookExists_ReturnsCorrectStatus - START");
        bookDao.addBook("Status Book", testUser1Id, 80, null);
        assertFalse(bookDao.getBookStatus("Status Book", testUser1Id));
        log.info("TEST: testGetBookStatus_BookExists_ReturnsCorrectStatus - END");
    }

    @Test
    void testGetBookStatus_NonExistentBook_ThrowsResourceNotFoundException() {
        log.info("TEST: testGetBookStatus_NonExistentBook_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> bookDao.getBookStatus("Unknown Status Book", testUser1Id));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Book"));
        log.info("TEST: testGetBookStatus_NonExistentBook_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testToggleBookStatus_FromFalseToTrue() {
        log.info("TEST: testToggleBookStatus_FromFalseToTrue - START");
        bookDao.addBook("Toggle Me", testUser1Id, 70, null);
        assertFalse(bookDao.getBookStatus("Toggle Me", testUser1Id));

        assertDoesNotThrow(() -> bookDao.toggleBookStatus("Toggle Me", testUser1Id));
        assertTrue(bookDao.getBookStatus("Toggle Me", testUser1Id));
        log.info("TEST: testToggleBookStatus_FromFalseToTrue - END");
    }

    @Test
    void testToggleBookStatus_FromTrueToFalse() {
        log.info("TEST: testToggleBookStatus_FromTrueToFalse - START");
        bookDao.addBook("Toggle Me Twice", testUser1Id, 70, null);
        bookDao.toggleBookStatus("Toggle Me Twice", testUser1Id);
        assertTrue(bookDao.getBookStatus("Toggle Me Twice", testUser1Id));

        assertDoesNotThrow(() -> bookDao.toggleBookStatus("Toggle Me Twice", testUser1Id));
        assertFalse(bookDao.getBookStatus("Toggle Me Twice", testUser1Id));
        log.info("TEST: testToggleBookStatus_FromTrueToFalse - END");
    }

    @Test
    void testToggleBookStatus_NonExistentBook_ThrowsResourceNotFoundException() {
        log.info("TEST: testToggleBookStatus_NonExistentBook_ThrowsResourceNotFoundException - START");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> bookDao.toggleBookStatus("Non Existent Toggle", testUser1Id));
        assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains("Book"));
        log.info("TEST: testToggleBookStatus_NonExistentBook_ThrowsResourceNotFoundException - END");
    }

    @Test
    void testDeleteBooksByUserId_UserHasBooks_DeletesAllBooks() {
        log.info("TEST: testDeleteBooksByUserId_UserHasBooks_DeletesAllBooks - START");
        bookDao.addBook("Book A del", testUser1Id, 50, null);
        bookDao.addBook("Book B del", testUser1Id, 60, null);
        assertEquals(2, bookDao.getUserBooks(testUser1Id).size());

        assertDoesNotThrow(() -> bookDao.deleteBooksByUserId(testUser1Id));
        assertTrue(bookDao.getUserBooks(testUser1Id).isEmpty());
        log.info("TEST: testDeleteBooksByUserId_UserHasBooks_DeletesAllBooks - END");
    }

    @Test
    void testDeleteBooksByUserId_UserHasNoBooks_CompletesSilently() {
        log.info("TEST: testDeleteBooksByUserId_UserHasNoBooks_CompletesSilently - START");
        assertTrue(bookDao.getUserBooks(testUser1Id).isEmpty());
        assertDoesNotThrow(() -> bookDao.deleteBooksByUserId(testUser1Id));
        assertTrue(bookDao.getUserBooks(testUser1Id).isEmpty());
        log.info("TEST: testDeleteBooksByUserId_UserHasNoBooks_CompletesSilently - END");
    }

    @Test
    void testDeleteBooksByUserId_DoesNotDeleteBooksOfOtherUsers() {
        log.info("TEST: testDeleteBooksByUserId_DoesNotDeleteBooksOfOtherUsers - START");
        bookDao.addBook("User1 Book ToDelete", testUser1Id, 50, null);
        bookDao.addBook("User2 Book ToKeep", testUser2Id, 60, null);

        assertDoesNotThrow(() -> bookDao.deleteBooksByUserId(testUser1Id));

        assertTrue(bookDao.getUserBooks(testUser1Id).isEmpty());
        List<Book> user2Books = bookDao.getUserBooks(testUser2Id);
        assertEquals(1, user2Books.size());
        assertEquals("User2 Book ToKeep", user2Books.get(0).getName());
        assertEquals(testUser2Id, user2Books.get(0).getUserId());
        log.info("TEST: testDeleteBooksByUserId_DoesNotDeleteBooksOfOtherUsers - END");
    }

    @Test
    void testUserDeletion_CascadesToAssociatedBooks() {
        log.info("TEST: testUserDeletion_CascadesToAssociatedBooks - START");
        User cascadeTestUserEntity = new User(0, "CascadeDeleteUser_Books", "cascadePass123", Role.USER, false);
        userDao.addUser(cascadeTestUserEntity);
        cascadeTestUserEntity = userDao.findUserByName(cascadeTestUserEntity.getName());
        assertNotNull(cascadeTestUserEntity, "Cascade test user should be created and found.");

        long cascadeUserEntityId = cascadeTestUserEntity.getId();
        int cascadeUserBookDaoId = (int) cascadeUserEntityId;
        log.debug("Cascade test user created with ID_long: {}, ID_int: {}", cascadeUserEntityId, cascadeUserBookDaoId);

        bookDao.addBook("Cascade Book 1", cascadeUserBookDaoId, 70, null);
        bookDao.addBook("Cascade Book 2", cascadeUserBookDaoId, 75, null);
        log.debug("Added 2 books for cascade test user ID_int: {}", cascadeUserBookDaoId);

        List<Book> booksBeforeDeletion = bookDao.getUserBooks(cascadeUserBookDaoId);
        assertEquals(2, booksBeforeDeletion.size(), "Cascade test user should have 2 books before user deletion.");

        log.debug("Attempting to delete cascade test user with ID_long: {}", cascadeUserEntityId);
        userDao.deleteUser((int) cascadeUserEntityId);
        log.debug("Cascade test user (ID_long: {}) deleted operation completed.", cascadeUserEntityId);

        List<Book> booksAfterUserDeletion = bookDao.getUserBooks(cascadeUserBookDaoId);
        assertTrue(booksAfterUserDeletion.isEmpty(), "Books of the deleted user (ID_int: " + cascadeUserBookDaoId + ") should be removed by cascade.");
        log.debug("Checked books after user deletion for ID_int: {}, list is empty as expected.", cascadeUserBookDaoId);

        User userAfterDeletion = userDao.findUserByName(cascadeTestUserEntity.getName());
        assertNull(userAfterDeletion, "Cascade test user '" + cascadeTestUserEntity.getName() + "' should no longer exist in the database.");
        log.debug("Checked user existence after deletion, user is null as expected.");
        log.info("TEST: testUserDeletion_CascadesToAssociatedBooks - END");
    }

    @Test
    void testUpdateBookAndName_DoesNotChangeCompletedStatus() {
        log.info("TEST: testUpdateBookAndName_DoesNotChangeCompletedStatus - START");
        String bookName = "StatusPreservationBook";
        int initialScore = 80;

        bookDao.addBook(bookName, testUser1Id, initialScore, null);
        Book book = bookDao.getUserBooks(testUser1Id).stream().filter(b -> b.getName().equals(bookName)).findFirst().orElse(null);
        assertNotNull(book);
        assertFalse(book.isCompleted(), "Book should initially be not completed.");
        log.debug("Book '{}' added, completed status: {}", bookName, book.isCompleted());

        bookDao.toggleBookStatus(bookName, testUser1Id);
        book = bookDao.getUserBooks(testUser1Id).stream().filter(b -> b.getName().equals(bookName)).findFirst().orElse(null);
        assertNotNull(book);
        assertTrue(book.isCompleted(), "Book status should be toggled to true.");
        log.debug("Book '{}' status toggled, completed status: {}", bookName, book.isCompleted());

        String updatedBookName = "UpdatedStatusPreservationBook";
        int updatedScore = 85;
        bookDao.updateBookAndName(bookName, updatedBookName, updatedScore, testUser1Id, null);
        log.debug("Book '{}' updated to '{}' with score {}", bookName, updatedBookName, updatedScore);

        Book updatedBook = bookDao.getUserBooks(testUser1Id).stream().filter(b -> b.getName().equals(updatedBookName)).findFirst().orElse(null);
        assertNotNull(updatedBook, "Updated book should be found.");
        assertTrue(updatedBook.isCompleted(), "Completed status should remain true after name and score update.");
        assertEquals(updatedBookName, updatedBook.getName());
        assertEquals(updatedScore, updatedBook.getScore());
        log.debug("Book '{}' after update, completed status: {}", updatedBookName, updatedBook.isCompleted());

        bookDao.toggleBookStatus(updatedBookName, testUser1Id);
        updatedBook = bookDao.getUserBooks(testUser1Id).stream().filter(b -> b.getName().equals(updatedBookName)).findFirst().orElse(null);
        assertNotNull(updatedBook);
        assertFalse(updatedBook.isCompleted(), "Book status should be toggled back to false.");
        log.debug("Book '{}' status toggled again, completed status: {}", updatedBookName, updatedBook.isCompleted());

        int finalScore = 90;
        bookDao.updateBookAndName(updatedBookName, updatedBookName, finalScore, testUser1Id, null);
        log.debug("Book '{}' score updated to {}", updatedBookName, finalScore);

        Book finalStateBook = bookDao.getUserBooks(testUser1Id).stream().filter(b -> b.getName().equals(updatedBookName)).findFirst().orElse(null);
        assertNotNull(finalStateBook);
        assertFalse(finalStateBook.isCompleted(), "Completed status should remain false after score-only update.");
        assertEquals(updatedBookName, finalStateBook.getName());
        assertEquals(finalScore, finalStateBook.getScore());
        log.debug("Book '{}' after final update, completed status: {}", updatedBookName, finalStateBook.isCompleted());
        log.info("TEST: testUpdateBookAndName_DoesNotChangeCompletedStatus - END");
    }

    @Test
    void testGetUserBooks_ReturnsCorrectCompletedStatus() {
        log.info("TEST: testGetUserBooks_ReturnsCorrectCompletedStatus - START");
        String bookNameCompleted = "Completed Book Status Test";
        String bookNameInProgress = "In-Progress Book Status Test";

        bookDao.addBook(bookNameCompleted, testUser1Id, 90, null);
        bookDao.toggleBookStatus(bookNameCompleted, testUser1Id);
        log.debug("Added and marked as completed: {}", bookNameCompleted);

        bookDao.addBook(bookNameInProgress, testUser1Id, 85, null);
        log.debug("Added as in-progress: {}", bookNameInProgress);

        List<Book> userBooks = bookDao.getUserBooks(testUser1Id);
        assertEquals(2, userBooks.size(), "Should retrieve two books for the user.");

        Book completedBook = userBooks.stream().filter(b -> b.getName().equals(bookNameCompleted)).findFirst().orElse(null);
        assertNotNull(completedBook, "Completed book should be found in the list.");
        assertTrue(completedBook.isCompleted(), "The '" + bookNameCompleted + "' should have completed status as true.");
        assertEquals(90, completedBook.getScore());
        log.debug("Verified '{}', completed: {}, score: {}", completedBook.getName(), completedBook.isCompleted(), completedBook.getScore());

        Book inProgressBook = userBooks.stream().filter(b -> b.getName().equals(bookNameInProgress)).findFirst().orElse(null);
        assertNotNull(inProgressBook, "In-progress book should be found in the list.");
        assertFalse(inProgressBook.isCompleted(), "The '" + bookNameInProgress + "' should have completed status as false.");
        assertEquals(85, inProgressBook.getScore());
        log.debug("Verified '{}', completed: {}, score: {}", inProgressBook.getName(), inProgressBook.isCompleted(), inProgressBook.getScore());
        log.info("TEST: testGetUserBooks_ReturnsCorrectCompletedStatus - END");
    }

    @Test
    void testAddBook_NamesWithLeadingTrailingSpaces_TrimmedCorrectly() {
        log.info("TEST: testAddBook_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - START");
        String nameWithSpaces = "  Spaced Book Name Test  ";
        String expectedTrimmedName = "Spaced Book Name Test";
        int score = 77;

        bookDao.addBook(nameWithSpaces, testUser1Id, score, null);
        log.debug("Attempted to add book with name: '{}'", nameWithSpaces);

        assertTrue(bookDao.isBookExists(expectedTrimmedName, testUser1Id), "Book should exist with trimmed name.");
        assertTrue(bookDao.isBookExists(nameWithSpaces, testUser1Id), "Book should also exist when checking with original spaced name.");

        List<Book> books = bookDao.getUserBooks(testUser1Id);
        Book addedBook = books.stream().filter(b -> b.getName().equals(expectedTrimmedName)).findFirst().orElse(null);
        assertNotNull(addedBook, "Added book with trimmed name not found.");
        assertEquals(expectedTrimmedName, addedBook.getName());
        assertEquals(score, addedBook.getScore());
        log.debug("Book found with name '{}' and score {}", addedBook.getName(), addedBook.getScore());

        Exception e = assertThrows(ItemAlreadyExistsException.class, () -> bookDao.addBook(expectedTrimmedName, testUser1Id, score + 1, null));
        assertTrue(e.getMessage().contains("already exists") || e.getMessage().contains("Book"));
        log.debug("Attempt to add duplicate (trimmed) name threw expected exception: {}", e.getMessage());

        Exception e2 = assertThrows(ItemAlreadyExistsException.class, () -> bookDao.addBook("  " + expectedTrimmedName + "  ", testUser1Id, score + 2, null));
        assertTrue(e2.getMessage().contains("already exists") || e2.getMessage().contains("Book"));
        log.debug("Attempt to add spaced name (which trims to duplicate) threw expected exception: {}", e2.getMessage());
        log.info("TEST: testAddBook_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - END");
    }

    @Test
    void testUpdateBookAndName_NamesWithLeadingTrailingSpaces_TrimmedCorrectly() {
        log.info("TEST: testUpdateBookAndName_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - START");
        String oldName = "OriginalTrimTestForUpdate";
        String newNameWithSpaces = "  Updated Trim Test For Update  ";
        String expectedTrimmedName = "Updated Trim Test For Update";
        int initialScore = 60;
        int updatedScore = 65;

        bookDao.addBook(oldName, testUser1Id, initialScore, null);
        log.debug("Added initial book: '{}'", oldName);

        bookDao.updateBookAndName(oldName, newNameWithSpaces, updatedScore, testUser1Id, null);
        log.debug("Attempted to update '{}' to '{}'", oldName, newNameWithSpaces);

        assertFalse(bookDao.isBookExists(oldName, testUser1Id), "Old book name should not exist after update.");
        assertTrue(bookDao.isBookExists(expectedTrimmedName, testUser1Id), "Book should exist with new trimmed name.");

        Book updatedBook = bookDao.getUserBooks(testUser1Id).stream()
                .filter(b -> b.getName().equals(expectedTrimmedName))
                .findFirst().orElse(null);
        assertNotNull(updatedBook, "Updated book with trimmed name not found.");
        assertEquals(expectedTrimmedName, updatedBook.getName());
        assertEquals(updatedScore, updatedBook.getScore());
        log.debug("Updated book found with name '{}' and score {}", updatedBook.getName(), updatedBook.getScore());

        String anotherBookName = "Another Book For Trim Update";
        bookDao.addBook(anotherBookName, testUser1Id, 70, null);
        log.debug("Added another book: '{}'", anotherBookName);

        Exception e = assertThrows(ItemAlreadyExistsException.class,
                () -> bookDao.updateBookAndName(expectedTrimmedName, "  " + anotherBookName + "  ", updatedScore + 5, testUser1Id, null));
        assertTrue(e.getMessage().contains("already exists") || e.getMessage().contains("Book"));
        log.debug("Attempt to update to a name that trims to a duplicate of another existing book threw: {}", e.getMessage());
        log.info("TEST: testUpdateBookAndName_NamesWithLeadingTrailingSpaces_TrimmedCorrectly - END");
    }

    @Test
    void testFindBooksByUserId_NoFilters() {
        log.info("TEST: testFindBooksByUserId_NoFilters - START");
        bookDao.addBook("1984", testUser1Id, 90, null);
        bookDao.addBook("Literally", testUser1Id, 85, null);
        bookDao.addBook("Pride and Prejudice", testUser2Id, 80, null);

        List<Book> books = bookDao.findBooksByUserId(testUser1Id, null, null, 0, 10, null, null);
        assertEquals(2, books.size(), "Should find 2 books for user 1");
        assertTrue(books.stream().anyMatch(b -> b.getName().equals("Literally")), "Literally should be present");
        assertTrue(books.stream().anyMatch(b -> b.getName().equals("1984")), "1984 should be present");

        int count = bookDao.countTotalForUser(testUser1Id, null, null);
        assertEquals(2, count, "Count should be 2 for user 1");
        log.info("TEST: testFindBooksByUserId_NoFilters - END");
    }
}