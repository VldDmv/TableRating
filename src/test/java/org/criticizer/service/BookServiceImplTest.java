package org.criticizer.service;

import org.criticizer.dao.book.BookDao;
import org.criticizer.entity.Book;
import org.criticizer.service.book.BookServiceImpl;
import org.criticizer.service.helper.ServiceValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceImplTest {

    @Mock
    private BookDao mockBookDao;

    @Mock
    private ServiceValidator mockValidator;

    @InjectMocks
    private BookServiceImpl bookService;

    private final int testUserId = 1;
    private final String testBookName = "Test Book";
    private final String testOldName = "Old Book Name";
    private final String testNewName = "New Book Name";

    @Test
    void getUserBooks_shouldReturnBooksListFromDao() {
        List<Book> expectedBooks = Arrays.asList(
                new Book(1, "Book 1", testUserId, 90, false),
                new Book(2, "Book 2", testUserId, 85, true)
        );
        when(mockBookDao.getUserBooks(testUserId)).thenReturn(expectedBooks);

        List<Book> actualBooks = bookService.getUserBooks(testUserId);

        assertEquals(expectedBooks, actualBooks, "Should return the list of books provided by DAO.");
        verify(mockBookDao).getUserBooks(testUserId);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void getUserBooks_whenDaoReturnsEmptyList_shouldReturnEmptyList() {
        when(mockBookDao.getUserBooks(testUserId)).thenReturn(Collections.emptyList());

        List<Book> actualBooks = bookService.getUserBooks(testUserId);

        assertTrue(actualBooks.isEmpty(), "Should return an empty list if DAO provides one.");
        verify(mockBookDao).getUserBooks(testUserId);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void getUserBooks_whenDaoThrowsException_shouldPropagateException() {
        when(mockBookDao.getUserBooks(testUserId)).thenThrow(new RuntimeException("DAO Error"));

        assertThrows(RuntimeException.class, () -> bookService.getUserBooks(testUserId),
                "Should propagate RuntimeException from DAO.");
        verify(mockBookDao).getUserBooks(testUserId);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void getBookStatus_shouldReturnStatusFromDao() {
        when(mockBookDao.getBookStatus(testBookName, testUserId)).thenReturn(true);

        boolean status = bookService.getBookStatus(testBookName, testUserId);

        assertTrue(status, "Should return true when DAO returns true.");
        verify(mockBookDao).getBookStatus(testBookName, testUserId);

        when(mockBookDao.getBookStatus(testBookName, testUserId)).thenReturn(false);

        status = bookService.getBookStatus(testBookName, testUserId);

        assertFalse(status, "Should return false when DAO returns false.");
        verify(mockBookDao, times(2)).getBookStatus(testBookName, testUserId);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void updateBookAndName_shouldCallDaoMethodWithNullGenreIds() {
        int newScore = 90;

        doNothing().when(mockValidator).validateScore(newScore, testUserId, "Book");
        doNothing().when(mockBookDao).updateBookAndName(testOldName, testNewName, newScore, testUserId, null);

        bookService.updateBookAndName(testOldName, testNewName, newScore, testUserId, null);

        verify(mockValidator).validateScore(newScore, testUserId, "Book");
        verify(mockBookDao).updateBookAndName(testOldName, testNewName, newScore, testUserId, null);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void updateBookAndName_withGenreIds_shouldCallDaoMethod() {
        int newScore = 90;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(newScore, testUserId, "Book");
        doNothing().when(mockBookDao).updateBookAndName(testOldName, testNewName, newScore, testUserId, genreIds);

        bookService.updateBookAndName(testOldName, testNewName, newScore, testUserId, genreIds);

        verify(mockValidator).validateScore(newScore, testUserId, "Book");
        verify(mockBookDao).updateBookAndName(testOldName, testNewName, newScore, testUserId, genreIds);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void updateBookAndName_withInvalidScore_shouldThrowIllegalArgumentException() {
        int invalidScore = 101;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doThrow(new IllegalArgumentException("Score must be between 1 and 100."))
                .when(mockValidator).validateScore(invalidScore, testUserId, "Book");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookService.updateBookAndName(testOldName, testNewName, invalidScore, testUserId, genreIds));

        assertEquals("Score must be between 1 and 100.", exception.getMessage());
        verify(mockValidator).validateScore(invalidScore, testUserId, "Book");
        verifyNoInteractions(mockBookDao);
    }

    @Test
    void updateBookAndName_whenDaoThrowsIllegalArgumentException_shouldPropagate() {
        int newScore = 90;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(newScore, testUserId, "Book");
        doThrow(new IllegalArgumentException("DAO: Invalid name")).when(mockBookDao)
                .updateBookAndName(testOldName, testNewName, newScore, testUserId, genreIds);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookService.updateBookAndName(testOldName, testNewName, newScore, testUserId, genreIds));

        assertEquals("DAO: Invalid name", ex.getMessage());
        verify(mockValidator).validateScore(newScore, testUserId, "Book");
        verify(mockBookDao).updateBookAndName(testOldName, testNewName, newScore, testUserId, genreIds);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void isBookExists_shouldReturnExistenceFromDao() {
        when(mockBookDao.isBookExists(testBookName, testUserId)).thenReturn(true);

        assertTrue(bookService.isBookExists(testBookName, testUserId));
        verify(mockBookDao).isBookExists(testBookName, testUserId);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void addBook_validScoreWithNullGenreIds_shouldCallDaoAddBook() {
        int validScore = 50;

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Book");
        when(mockBookDao.addBook(testBookName, testUserId, validScore, null)).thenReturn(1);

        bookService.addBook(testBookName, testUserId, validScore, null);

        verify(mockValidator).validateScore(validScore, testUserId, "Book");
        verify(mockBookDao).addBook(testBookName, testUserId, validScore, null);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void addBook_validScoreWithGenreIds_shouldCallDaoAddBook() {
        int validScore = 50;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Book");
        when(mockBookDao.addBook(testBookName, testUserId, validScore, genreIds)).thenReturn(1);

        bookService.addBook(testBookName, testUserId, validScore, genreIds);

        verify(mockValidator).validateScore(validScore, testUserId, "Book");
        verify(mockBookDao).addBook(testBookName, testUserId, validScore, genreIds);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void addBook_scoreTooLow_shouldThrowIllegalArgumentExceptionAndNotCallDao() {
        int invalidScore = 0;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doThrow(new IllegalArgumentException("The rating must be between 1 and 100."))
                .when(mockValidator).validateScore(invalidScore, testUserId, "Book");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookService.addBook(testBookName, testUserId, invalidScore, genreIds));

        assertEquals("The rating must be between 1 and 100.", exception.getMessage());
        verify(mockValidator).validateScore(invalidScore, testUserId, "Book");
        verifyNoInteractions(mockBookDao);
    }

    @Test
    void addBook_scoreTooHigh_shouldThrowIllegalArgumentExceptionAndNotCallDao() {
        int invalidScore = 101;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doThrow(new IllegalArgumentException("The rating must be between 1 and 100."))
                .when(mockValidator).validateScore(invalidScore, testUserId, "Book");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookService.addBook(testBookName, testUserId, invalidScore, genreIds));

        assertEquals("The rating must be between 1 and 100.", exception.getMessage());
        verify(mockValidator).validateScore(invalidScore, testUserId, "Book");
        verifyNoInteractions(mockBookDao);
    }

    @Test
    void addBook_scoreAtLowerBoundary_shouldCallDaoAddBook() {
        int score = 1;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(score, testUserId, "Book");
        when(mockBookDao.addBook(testBookName, testUserId, score, genreIds)).thenReturn(1);

        bookService.addBook(testBookName, testUserId, score, genreIds);

        verify(mockValidator).validateScore(score, testUserId, "Book");
        verify(mockBookDao).addBook(testBookName, testUserId, score, genreIds);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void addBook_scoreAtUpperBoundary_shouldCallDaoAddBook() {
        int score = 100;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(score, testUserId, "Book");
        when(mockBookDao.addBook(testBookName, testUserId, score, genreIds)).thenReturn(1);

        bookService.addBook(testBookName, testUserId, score, genreIds);

        verify(mockValidator).validateScore(score, testUserId, "Book");
        verify(mockBookDao).addBook(testBookName, testUserId, score, genreIds);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void addBook_daoThrowsIllegalArgumentException_shouldPropagate() {
        int validScore = 75;
        List<Integer> genreIds = Arrays.asList(1, 2);
        String daoExceptionMessage = "DAO: Book already exists";

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Book");
        doThrow(new IllegalArgumentException(daoExceptionMessage)).when(mockBookDao)
                .addBook(testBookName, testUserId, validScore, genreIds);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookService.addBook(testBookName, testUserId, validScore, genreIds));

        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockValidator).validateScore(validScore, testUserId, "Book");
        verify(mockBookDao).addBook(testBookName, testUserId, validScore, genreIds);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void addBook_daoThrowsOtherRuntimeException_shouldPropagate() {
        int validScore = 75;
        List<Integer> genreIds = Arrays.asList(1, 2);
        String daoExceptionMessage = "DAO: Database connection error";

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Book");
        doThrow(new RuntimeException(daoExceptionMessage)).when(mockBookDao)
                .addBook(testBookName, testUserId, validScore, genreIds);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookService.addBook(testBookName, testUserId, validScore, genreIds));

        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockValidator).validateScore(validScore, testUserId, "Book");
        verify(mockBookDao).addBook(testBookName, testUserId, validScore, genreIds);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void removeBook_shouldCallDaoRemoveBook() {
        doNothing().when(mockBookDao).removeBook(testBookName, testUserId);

        bookService.removeBook(testBookName, testUserId);

        verify(mockBookDao).removeBook(testBookName, testUserId);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void removeBook_daoThrowsIllegalArgumentException_shouldPropagate() {
        String daoExceptionMessage = "DAO: Book not found";
        doThrow(new IllegalArgumentException(daoExceptionMessage)).when(mockBookDao)
                .removeBook(testBookName, testUserId);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookService.removeBook(testBookName, testUserId));
        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockBookDao).removeBook(testBookName, testUserId);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void toggleBookStatus_shouldCallDaoToggleBookStatus() {
        doNothing().when(mockBookDao).toggleBookStatus(testBookName, testUserId);

        bookService.toggleBookStatus(testBookName, testUserId);

        verify(mockBookDao).toggleBookStatus(testBookName, testUserId);
        verifyNoMoreInteractions(mockBookDao);
    }

    @Test
    void toggleBookStatus_daoThrowsIllegalArgumentException_shouldPropagate() {
        String daoExceptionMessage = "DAO: Book not found for toggle";
        doThrow(new IllegalArgumentException(daoExceptionMessage)).when(mockBookDao)
                .toggleBookStatus(testBookName, testUserId);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookService.toggleBookStatus(testBookName, testUserId));
        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockBookDao).toggleBookStatus(testBookName, testUserId);
        verifyNoMoreInteractions(mockBookDao);
    }
}