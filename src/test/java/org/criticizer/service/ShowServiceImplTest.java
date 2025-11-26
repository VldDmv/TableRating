package org.criticizer.service;

import org.criticizer.dao.show.ShowDao;
import org.criticizer.entity.Show;
import org.criticizer.service.helper.ServiceValidator;
import org.criticizer.service.show.ShowServiceImpl;
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
public class ShowServiceImplTest {

    private final int testUserId = 1;
    private final String testShowName = "Test Show";
    private final String testOldName = "Old Show Name";
    private final String testNewName = "New Show Name";
    @Mock
    private ShowDao mockShowDao;
    @Mock
    private ServiceValidator mockValidator;
    @InjectMocks
    private ShowServiceImpl showService;

    @Test
    void getUserShows_shouldReturnShowsListFromDao() {
        List<Show> expectedShows = Arrays.asList(
                new Show(1, "Show 1", testUserId, 90, false),
                new Show(2, "Show 2", testUserId, 85, true)
        );
        when(mockShowDao.getUserShows(testUserId)).thenReturn(expectedShows);

        List<Show> actualShows = showService.getUserShows(testUserId);

        assertEquals(expectedShows, actualShows, "Should return the list of shows provided by DAO.");
        verify(mockShowDao).getUserShows(testUserId);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void getUserShows_whenDaoReturnsEmptyList_shouldReturnEmptyList() {
        when(mockShowDao.getUserShows(testUserId)).thenReturn(Collections.emptyList());

        List<Show> actualShows = showService.getUserShows(testUserId);

        assertTrue(actualShows.isEmpty(), "Should return an empty list if DAO provides one.");
        verify(mockShowDao).getUserShows(testUserId);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void getUserShows_whenDaoThrowsException_shouldPropagateException() {
        when(mockShowDao.getUserShows(testUserId)).thenThrow(new RuntimeException("DAO Error"));

        assertThrows(RuntimeException.class, () -> showService.getUserShows(testUserId),
                "Should propagate RuntimeException from DAO.");
        verify(mockShowDao).getUserShows(testUserId);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void getShowStatus_shouldReturnStatusFromDao() {
        when(mockShowDao.getShowStatus(testShowName, testUserId)).thenReturn(true);

        boolean status = showService.getShowStatus(testShowName, testUserId);

        assertTrue(status, "Should return true when DAO returns true.");
        verify(mockShowDao).getShowStatus(testShowName, testUserId);

        when(mockShowDao.getShowStatus(testShowName, testUserId)).thenReturn(false);

        status = showService.getShowStatus(testShowName, testUserId);

        assertFalse(status, "Should return false when DAO returns false.");
        verify(mockShowDao, times(2)).getShowStatus(testShowName, testUserId);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void updateShowAndName_shouldCallDaoMethodWithNullGenreIds() {
        int newScore = 90;

        doNothing().when(mockValidator).validateScore(newScore, testUserId, "Show");
        doNothing().when(mockShowDao).updateShowAndName(testOldName, testNewName, newScore, testUserId, null);

        showService.updateShowAndName(testOldName, testNewName, newScore, testUserId, null);

        verify(mockValidator).validateScore(newScore, testUserId, "Show");
        verify(mockShowDao).updateShowAndName(testOldName, testNewName, newScore, testUserId, null);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void updateShowAndName_withGenreIds_shouldCallDaoMethod() {
        int newScore = 90;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(newScore, testUserId, "Show");
        doNothing().when(mockShowDao).updateShowAndName(testOldName, testNewName, newScore, testUserId, genreIds);

        showService.updateShowAndName(testOldName, testNewName, newScore, testUserId, genreIds);

        verify(mockValidator).validateScore(newScore, testUserId, "Show");
        verify(mockShowDao).updateShowAndName(testOldName, testNewName, newScore, testUserId, genreIds);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void updateShowAndName_withInvalidScore_shouldThrowIllegalArgumentException() {
        int invalidScore = 101;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doThrow(new IllegalArgumentException("Score must be between 1 and 100."))
                .when(mockValidator).validateScore(invalidScore, testUserId, "Show");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> showService.updateShowAndName(testOldName, testNewName, invalidScore, testUserId, genreIds));

        assertEquals("Score must be between 1 and 100.", exception.getMessage());
        verify(mockValidator).validateScore(invalidScore, testUserId, "Show");
        verifyNoInteractions(mockShowDao);
    }

    @Test
    void updateShowAndName_whenDaoThrowsIllegalArgumentException_shouldPropagate() {
        int newScore = 90;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(newScore, testUserId, "Show");
        doThrow(new IllegalArgumentException("DAO: Invalid name")).when(mockShowDao)
                .updateShowAndName(testOldName, testNewName, newScore, testUserId, genreIds);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> showService.updateShowAndName(testOldName, testNewName, newScore, testUserId, genreIds));

        assertEquals("DAO: Invalid name", ex.getMessage());
        verify(mockValidator).validateScore(newScore, testUserId, "Show");
        verify(mockShowDao).updateShowAndName(testOldName, testNewName, newScore, testUserId, genreIds);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void isShowExists_shouldReturnExistenceFromDao() {
        when(mockShowDao.isShowExists(testShowName, testUserId)).thenReturn(true);

        assertTrue(showService.isShowExists(testShowName, testUserId));
        verify(mockShowDao).isShowExists(testShowName, testUserId);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void addShow_validScoreWithNullGenreIds_shouldCallDaoAddShow() {
        int validScore = 50;

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Show");
        when(mockShowDao.addShow(testShowName, testUserId, validScore, null)).thenReturn(1);

        showService.addShow(testShowName, testUserId, validScore, null);

        verify(mockValidator).validateScore(validScore, testUserId, "Show");
        verify(mockShowDao).addShow(testShowName, testUserId, validScore, null);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void addShow_validScoreWithGenreIds_shouldCallDaoAddShow() {
        int validScore = 50;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Show");
        when(mockShowDao.addShow(testShowName, testUserId, validScore, genreIds)).thenReturn(1);

        showService.addShow(testShowName, testUserId, validScore, genreIds);

        verify(mockValidator).validateScore(validScore, testUserId, "Show");
        verify(mockShowDao).addShow(testShowName, testUserId, validScore, genreIds);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void addShow_scoreTooLow_shouldThrowIllegalArgumentExceptionAndNotCallDao() {
        int invalidScore = 0;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doThrow(new IllegalArgumentException("The rating must be between 1 and 100."))
                .when(mockValidator).validateScore(invalidScore, testUserId, "Show");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> showService.addShow(testShowName, testUserId, invalidScore, genreIds));

        assertEquals("The rating must be between 1 and 100.", exception.getMessage());
        verify(mockValidator).validateScore(invalidScore, testUserId, "Show");
        verifyNoInteractions(mockShowDao);
    }

    @Test
    void addShow_scoreTooHigh_shouldThrowIllegalArgumentExceptionAndNotCallDao() {
        int invalidScore = 101;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doThrow(new IllegalArgumentException("The rating must be between 1 and 100."))
                .when(mockValidator).validateScore(invalidScore, testUserId, "Show");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> showService.addShow(testShowName, testUserId, invalidScore, genreIds));

        assertEquals("The rating must be between 1 and 100.", exception.getMessage());
        verify(mockValidator).validateScore(invalidScore, testUserId, "Show");
        verifyNoInteractions(mockShowDao);
    }

    @Test
    void addShow_scoreAtLowerBoundary_shouldCallDaoAddShow() {
        int score = 1;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(score, testUserId, "Show");
        when(mockShowDao.addShow(testShowName, testUserId, score, genreIds)).thenReturn(1);

        showService.addShow(testShowName, testUserId, score, genreIds);

        verify(mockValidator).validateScore(score, testUserId, "Show");
        verify(mockShowDao).addShow(testShowName, testUserId, score, genreIds);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void addShow_scoreAtUpperBoundary_shouldCallDaoAddShow() {
        int score = 100;
        List<Integer> genreIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(score, testUserId, "Show");
        when(mockShowDao.addShow(testShowName, testUserId, score, genreIds)).thenReturn(1);

        showService.addShow(testShowName, testUserId, score, genreIds);

        verify(mockValidator).validateScore(score, testUserId, "Show");
        verify(mockShowDao).addShow(testShowName, testUserId, score, genreIds);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void addShow_daoThrowsIllegalArgumentException_shouldPropagate() {
        int validScore = 75;
        List<Integer> genreIds = Arrays.asList(1, 2);
        String daoExceptionMessage = "DAO: Show already exists";

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Show");
        doThrow(new IllegalArgumentException(daoExceptionMessage)).when(mockShowDao)
                .addShow(testShowName, testUserId, validScore, genreIds);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> showService.addShow(testShowName, testUserId, validScore, genreIds));

        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockValidator).validateScore(validScore, testUserId, "Show");
        verify(mockShowDao).addShow(testShowName, testUserId, validScore, genreIds);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void addShow_daoThrowsOtherRuntimeException_shouldPropagate() {
        int validScore = 75;
        List<Integer> genreIds = Arrays.asList(1, 2);
        String daoExceptionMessage = "DAO: Database connection error";

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Show");
        doThrow(new RuntimeException(daoExceptionMessage)).when(mockShowDao)
                .addShow(testShowName, testUserId, validScore, genreIds);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> showService.addShow(testShowName, testUserId, validScore, genreIds));

        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockValidator).validateScore(validScore, testUserId, "Show");
        verify(mockShowDao).addShow(testShowName, testUserId, validScore, genreIds);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void removeShow_shouldCallDaoRemoveShow() {
        doNothing().when(mockShowDao).removeShow(testShowName, testUserId);

        showService.removeShow(testShowName, testUserId);

        verify(mockShowDao).removeShow(testShowName, testUserId);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void removeShow_daoThrowsIllegalArgumentException_shouldPropagate() {
        String daoExceptionMessage = "DAO: Show not found";
        doThrow(new IllegalArgumentException(daoExceptionMessage)).when(mockShowDao)
                .removeShow(testShowName, testUserId);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> showService.removeShow(testShowName, testUserId));
        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockShowDao).removeShow(testShowName, testUserId);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void toggleShowStatus_shouldCallDaoToggleShowStatus() {
        doNothing().when(mockShowDao).toggleShowStatus(testShowName, testUserId);

        showService.toggleShowStatus(testShowName, testUserId);

        verify(mockShowDao).toggleShowStatus(testShowName, testUserId);
        verifyNoMoreInteractions(mockShowDao);
    }

    @Test
    void toggleShowStatus_daoThrowsIllegalArgumentException_shouldPropagate() {
        String daoExceptionMessage = "DAO: Show not found for toggle";
        doThrow(new IllegalArgumentException(daoExceptionMessage)).when(mockShowDao)
                .toggleShowStatus(testShowName, testUserId);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> showService.toggleShowStatus(testShowName, testUserId));
        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockShowDao).toggleShowStatus(testShowName, testUserId);
        verifyNoMoreInteractions(mockShowDao);
    }
}