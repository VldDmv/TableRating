package org.criticizer.service;

import org.criticizer.dao.movie.MovieDao;
import org.criticizer.entity.Movie;
import org.criticizer.service.helper.ServiceValidator;
import org.criticizer.service.movie.MovieServiceImpl;
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
public class MovieServiceImplTest {

    @Mock
    private MovieDao mockMovieDao;

    @Mock
    private ServiceValidator mockValidator;

    @InjectMocks
    private MovieServiceImpl movieService;

    private final int testUserId = 1;
    private final String testMovieName = "Test Movie";
    private final String testOldName = "Old Movie Name";
    private final String testNewName = "New Movie Name";

    @Test
    void getUserMovies_shouldReturnMoviesListFromDao() {
        List<Movie> expectedMovies = Arrays.asList(
                new Movie(1, "Movie 1", testUserId, 90, false),
                new Movie(2, "Movie 2", testUserId, 85, true)
        );
        when(mockMovieDao.getUserMovies(testUserId)).thenReturn(expectedMovies);

        List<Movie> actualMovies = movieService.getUserMovies(testUserId);

        assertEquals(expectedMovies, actualMovies, "Should return the list of movies provided by DAO.");
        verify(mockMovieDao).getUserMovies(testUserId);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void getUserMovies_whenDaoReturnsEmptyList_shouldReturnEmptyList() {
        when(mockMovieDao.getUserMovies(testUserId)).thenReturn(Collections.emptyList());

        List<Movie> actualMovies = movieService.getUserMovies(testUserId);

        assertTrue(actualMovies.isEmpty(), "Should return an empty list if DAO provides one.");
        verify(mockMovieDao).getUserMovies(testUserId);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void getUserMovies_whenDaoThrowsException_shouldPropagateException() {
        when(mockMovieDao.getUserMovies(testUserId)).thenThrow(new RuntimeException("DAO Error"));

        assertThrows(RuntimeException.class, () -> movieService.getUserMovies(testUserId),
                "Should propagate RuntimeException from DAO.");
        verify(mockMovieDao).getUserMovies(testUserId);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void getMovieStatus_shouldReturnStatusFromDao() {
        when(mockMovieDao.getMovieStatus(testMovieName, testUserId)).thenReturn(true);

        boolean status = movieService.getMovieStatus(testMovieName, testUserId);

        assertTrue(status, "Should return true when DAO returns true.");
        verify(mockMovieDao).getMovieStatus(testMovieName, testUserId);

        when(mockMovieDao.getMovieStatus(testMovieName, testUserId)).thenReturn(false);

        status = movieService.getMovieStatus(testMovieName, testUserId);

        assertFalse(status, "Should return false when DAO returns false.");
        verify(mockMovieDao, times(2)).getMovieStatus(testMovieName, testUserId);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void updateMovieAndName_shouldCallDaoMethodWithNullGenreIds() {
        int newScore = 90;

        doNothing().when(mockValidator).validateScore(newScore, testUserId, "Movie");
        doNothing().when(mockMovieDao).updateMovieAndName(testOldName, testNewName, newScore, testUserId, null);

        movieService.updateMovieAndName(testOldName, testNewName, newScore, testUserId, null);

        verify(mockValidator).validateScore(newScore, testUserId, "Movie");
        verify(mockMovieDao).updateMovieAndName(testOldName, testNewName, newScore, testUserId, null);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void updateMovieAndName_withGenreIds_shouldCallDaoMethod() {
        int newScore = 90;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(newScore, testUserId, "Movie");
        doNothing().when(mockMovieDao).updateMovieAndName(testOldName, testNewName, newScore, testUserId, genreIds);

        movieService.updateMovieAndName(testOldName, testNewName, newScore, testUserId, genreIds);

        verify(mockValidator).validateScore(newScore, testUserId, "Movie");
        verify(mockMovieDao).updateMovieAndName(testOldName, testNewName, newScore, testUserId, genreIds);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void updateMovieAndName_withInvalidScore_shouldThrowIllegalArgumentException() {
        int invalidScore = 101;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doThrow(new IllegalArgumentException("Score must be between 1 and 100."))
                .when(mockValidator).validateScore(invalidScore, testUserId, "Movie");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> movieService.updateMovieAndName(testOldName, testNewName, invalidScore, testUserId, genreIds));

        assertEquals("Score must be between 1 and 100.", exception.getMessage());
        verify(mockValidator).validateScore(invalidScore, testUserId, "Movie");
        verifyNoInteractions(mockMovieDao);
    }

    @Test
    void updateMovieAndName_whenDaoThrowsIllegalArgumentException_shouldPropagate() {
        int newScore = 90;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(newScore, testUserId, "Movie");
        doThrow(new IllegalArgumentException("DAO: Invalid name")).when(mockMovieDao)
                .updateMovieAndName(testOldName, testNewName, newScore, testUserId, genreIds);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> movieService.updateMovieAndName(testOldName, testNewName, newScore, testUserId, genreIds));

        assertEquals("DAO: Invalid name", ex.getMessage());
        verify(mockValidator).validateScore(newScore, testUserId, "Movie");
        verify(mockMovieDao).updateMovieAndName(testOldName, testNewName, newScore, testUserId, genreIds);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void isMovieExists_shouldReturnExistenceFromDao() {
        when(mockMovieDao.isMovieExists(testMovieName, testUserId)).thenReturn(true);

        assertTrue(movieService.isMovieExists(testMovieName, testUserId));
        verify(mockMovieDao).isMovieExists(testMovieName, testUserId);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void addMovie_validScoreWithNullGenreIds_shouldCallDaoAddMovie() {
        int validScore = 50;

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Movie");
        when(mockMovieDao.addMovie(testMovieName, testUserId, validScore, null)).thenReturn(1);

        movieService.addMovie(testMovieName, testUserId, validScore, null);

        verify(mockValidator).validateScore(validScore, testUserId, "Movie");
        verify(mockMovieDao).addMovie(testMovieName, testUserId, validScore, null);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void addMovie_validScoreWithGenreIds_shouldCallDaoAddMovie() {
        int validScore = 50;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Movie");
        when(mockMovieDao.addMovie(testMovieName, testUserId, validScore, genreIds)).thenReturn(1);

        movieService.addMovie(testMovieName, testUserId, validScore, genreIds);

        verify(mockValidator).validateScore(validScore, testUserId, "Movie");
        verify(mockMovieDao).addMovie(testMovieName, testUserId, validScore, genreIds);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void addMovie_scoreTooLow_shouldThrowIllegalArgumentExceptionAndNotCallDao() {
        int invalidScore = 0;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doThrow(new IllegalArgumentException("The rating must be between 1 and 100."))
                .when(mockValidator).validateScore(invalidScore, testUserId, "Movie");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> movieService.addMovie(testMovieName, testUserId, invalidScore, genreIds));

        assertEquals("The rating must be between 1 and 100.", exception.getMessage());
        verify(mockValidator).validateScore(invalidScore, testUserId, "Movie");
        verifyNoInteractions(mockMovieDao);
    }

    @Test
    void addMovie_scoreTooHigh_shouldThrowIllegalArgumentExceptionAndNotCallDao() {
        int invalidScore = 101;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doThrow(new IllegalArgumentException("The rating must be between 1 and 100."))
                .when(mockValidator).validateScore(invalidScore, testUserId, "Movie");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> movieService.addMovie(testMovieName, testUserId, invalidScore, genreIds));

        assertEquals("The rating must be between 1 and 100.", exception.getMessage());
        verify(mockValidator).validateScore(invalidScore, testUserId, "Movie");
        verifyNoInteractions(mockMovieDao);
    }

    @Test
    void addMovie_scoreAtLowerBoundary_shouldCallDaoAddMovie() {
        int score = 1;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(score, testUserId, "Movie");
        when(mockMovieDao.addMovie(testMovieName, testUserId, score, genreIds)).thenReturn(1);

        movieService.addMovie(testMovieName, testUserId, score, genreIds);

        verify(mockValidator).validateScore(score, testUserId, "Movie");
        verify(mockMovieDao).addMovie(testMovieName, testUserId, score, genreIds);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void addMovie_scoreAtUpperBoundary_shouldCallDaoAddMovie() {
        int score = 100;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(score, testUserId, "Movie");
        when(mockMovieDao.addMovie(testMovieName, testUserId, score, genreIds)).thenReturn(1);

        movieService.addMovie(testMovieName, testUserId, score, genreIds);

        verify(mockValidator).validateScore(score, testUserId, "Movie");
        verify(mockMovieDao).addMovie(testMovieName, testUserId, score, genreIds);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void addMovie_daoThrowsIllegalArgumentException_shouldPropagate() {
        int validScore = 75;
        List<Integer> genreIds = Arrays.asList(1, 2);
        String daoExceptionMessage = "DAO: Movie already exists";

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Movie");
        doThrow(new IllegalArgumentException(daoExceptionMessage)).when(mockMovieDao)
                .addMovie(testMovieName, testUserId, validScore, genreIds);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> movieService.addMovie(testMovieName, testUserId, validScore, genreIds));

        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockValidator).validateScore(validScore, testUserId, "Movie");
        verify(mockMovieDao).addMovie(testMovieName, testUserId, validScore, genreIds);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void addMovie_daoThrowsOtherRuntimeException_shouldPropagate() {
        int validScore = 75;
        List<Integer> genreIds = Arrays.asList(1, 2);
        String daoExceptionMessage = "DAO: Database connection error";

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Movie");
        doThrow(new RuntimeException(daoExceptionMessage)).when(mockMovieDao)
                .addMovie(testMovieName, testUserId, validScore, genreIds);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> movieService.addMovie(testMovieName, testUserId, validScore, genreIds));

        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockValidator).validateScore(validScore, testUserId, "Movie");
        verify(mockMovieDao).addMovie(testMovieName, testUserId, validScore, genreIds);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void removeMovie_shouldCallDaoRemoveMovie() {
        doNothing().when(mockMovieDao).removeMovie(testMovieName, testUserId);

        movieService.removeMovie(testMovieName, testUserId);

        verify(mockMovieDao).removeMovie(testMovieName, testUserId);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void removeMovie_daoThrowsIllegalArgumentException_shouldPropagate() {
        String daoExceptionMessage = "DAO: Movie not found";
        doThrow(new IllegalArgumentException(daoExceptionMessage)).when(mockMovieDao)
                .removeMovie(testMovieName, testUserId);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> movieService.removeMovie(testMovieName, testUserId));
        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockMovieDao).removeMovie(testMovieName, testUserId);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void toggleMovieStatus_shouldCallDaoToggleMovieStatus() {
        doNothing().when(mockMovieDao).toggleMovieStatus(testMovieName, testUserId);

        movieService.toggleMovieStatus(testMovieName, testUserId);

        verify(mockMovieDao).toggleMovieStatus(testMovieName, testUserId);
        verifyNoMoreInteractions(mockMovieDao);
    }

    @Test
    void toggleMovieStatus_daoThrowsIllegalArgumentException_shouldPropagate() {
        String daoExceptionMessage = "DAO: Movie not found for toggle";
        doThrow(new IllegalArgumentException(daoExceptionMessage)).when(mockMovieDao)
                .toggleMovieStatus(testMovieName, testUserId);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> movieService.toggleMovieStatus(testMovieName, testUserId));
        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockMovieDao).toggleMovieStatus(testMovieName, testUserId);
        verifyNoMoreInteractions(mockMovieDao);
    }
}