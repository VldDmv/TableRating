package org.criticizer.service;

import org.criticizer.dao.game.GameDao;
import org.criticizer.entity.Game;
import org.criticizer.service.game.GameServiceImpl;
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
public class GameServiceImplTest {

    @Mock
    private GameDao mockGameDao;

    @Mock
    private ServiceValidator mockValidator;

    @InjectMocks
    private GameServiceImpl gameService;

    private final int testUserId = 1;
    private final String testGameName = "Test Game";
    private final String testOldName = "Old Game Name";
    private final String testNewName = "New Game Name";

    @Test
    void getUserGames_shouldReturnGamesListFromDao() {
        List<Game> expectedGames = Arrays.asList(
                new Game(1, "Game 1", testUserId, 90, false),
                new Game(2, "Game 2", testUserId, 85, true)
        );
        when(mockGameDao.getUserGames(testUserId)).thenReturn(expectedGames);

        List<Game> actualGames = gameService.getUserGames(testUserId);

        assertEquals(expectedGames, actualGames, "Should return the list of games provided by DAO.");
        verify(mockGameDao).getUserGames(testUserId);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void getUserGames_whenDaoReturnsEmptyList_shouldReturnEmptyList() {
        when(mockGameDao.getUserGames(testUserId)).thenReturn(Collections.emptyList());

        List<Game> actualGames = gameService.getUserGames(testUserId);

        assertTrue(actualGames.isEmpty(), "Should return an empty list if DAO provides one.");
        verify(mockGameDao).getUserGames(testUserId);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void getUserGames_whenDaoThrowsException_shouldPropagateException() {
        when(mockGameDao.getUserGames(testUserId)).thenThrow(new RuntimeException("DAO Error"));

        assertThrows(RuntimeException.class, () -> gameService.getUserGames(testUserId),
                "Should propagate RuntimeException from DAO.");
        verify(mockGameDao).getUserGames(testUserId);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void getGameStatus_shouldReturnStatusFromDao() {
        when(mockGameDao.getGameStatus(testGameName, testUserId)).thenReturn(true);

        boolean status = gameService.getGameStatus(testGameName, testUserId);

        assertTrue(status, "Should return true when DAO returns true.");
        verify(mockGameDao).getGameStatus(testGameName, testUserId);

        when(mockGameDao.getGameStatus(testGameName, testUserId)).thenReturn(false);

        status = gameService.getGameStatus(testGameName, testUserId);

        assertFalse(status, "Should return false when DAO returns false.");
        verify(mockGameDao, times(2)).getGameStatus(testGameName, testUserId);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void updateGameAndName_shouldCallDaoMethodWithNullTagIds() {
        int newScore = 90;

        doNothing().when(mockValidator).validateScore(newScore, testUserId, "Game");
        doNothing().when(mockGameDao).updateGameAndName(testOldName, testNewName, newScore, testUserId, null);

        gameService.updateGameAndName(testOldName, testNewName, newScore, testUserId, null);

        verify(mockValidator).validateScore(newScore, testUserId, "Game");
        verify(mockGameDao).updateGameAndName(testOldName, testNewName, newScore, testUserId, null);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void updateGameAndName_withTagIds_shouldCallDaoMethod() {
        int newScore = 90;
        List<Integer> tagIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(newScore, testUserId, "Game");
        doNothing().when(mockGameDao).updateGameAndName(testOldName, testNewName, newScore, testUserId, tagIds);

        gameService.updateGameAndName(testOldName, testNewName, newScore, testUserId, tagIds);

        verify(mockValidator).validateScore(newScore, testUserId, "Game");
        verify(mockGameDao).updateGameAndName(testOldName, testNewName, newScore, testUserId, tagIds);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void updateGameAndName_withInvalidScore_shouldThrowIllegalArgumentException() {
        int invalidScore = 101;
        List<Integer> tagIds = Arrays.asList(1, 2);

        doThrow(new IllegalArgumentException("Score must be between 1 and 100."))
                .when(mockValidator).validateScore(invalidScore, testUserId, "Game");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> gameService.updateGameAndName(testOldName, testNewName, invalidScore, testUserId, tagIds));

        assertEquals("Score must be between 1 and 100.", exception.getMessage());
        verify(mockValidator).validateScore(invalidScore, testUserId, "Game");
        verifyNoInteractions(mockGameDao);
    }

    @Test
    void updateGameAndName_whenDaoThrowsIllegalArgumentException_shouldPropagate() {
        int newScore = 90;
        List<Integer> tagIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(newScore, testUserId, "Game");
        doThrow(new IllegalArgumentException("DAO: Invalid name")).when(mockGameDao)
                .updateGameAndName(testOldName, testNewName, newScore, testUserId, tagIds);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> gameService.updateGameAndName(testOldName, testNewName, newScore, testUserId, tagIds));

        assertEquals("DAO: Invalid name", ex.getMessage());
        verify(mockValidator).validateScore(newScore, testUserId, "Game");
        verify(mockGameDao).updateGameAndName(testOldName, testNewName, newScore, testUserId, tagIds);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void isGameExists_shouldReturnExistenceFromDao() {
        when(mockGameDao.isGameExists(testGameName, testUserId)).thenReturn(true);

        assertTrue(gameService.isGameExists(testGameName, testUserId));
        verify(mockGameDao).isGameExists(testGameName, testUserId);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void addGame_validScoreWithNullTagIds_shouldCallDaoAddGame() {
        int validScore = 50;

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Game");
        when(mockGameDao.addGame(testGameName, testUserId, validScore, null)).thenReturn(1);

        gameService.addGame(testGameName, testUserId, validScore, null);

        verify(mockValidator).validateScore(validScore, testUserId, "Game");
        verify(mockGameDao).addGame(testGameName, testUserId, validScore, null);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void addGame_validScoreWithTagIds_shouldCallDaoAddGame() {
        int validScore = 50;
        List<Integer> tagIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Game");
        when(mockGameDao.addGame(testGameName, testUserId, validScore, tagIds)).thenReturn(1);

        gameService.addGame(testGameName, testUserId, validScore, tagIds);

        verify(mockValidator).validateScore(validScore, testUserId, "Game");
        verify(mockGameDao).addGame(testGameName, testUserId, validScore, tagIds);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void addGame_scoreTooLow_shouldThrowIllegalArgumentExceptionAndNotCallDao() {
        int invalidScore = 0;
        List<Integer> tagIds = Arrays.asList(1, 2);

        doThrow(new IllegalArgumentException("The rating must be between 1 and 100."))
                .when(mockValidator).validateScore(invalidScore, testUserId, "Game");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> gameService.addGame(testGameName, testUserId, invalidScore, tagIds));

        assertEquals("The rating must be between 1 and 100.", exception.getMessage());
        verify(mockValidator).validateScore(invalidScore, testUserId, "Game");
        verifyNoInteractions(mockGameDao);
    }

    @Test
    void addGame_scoreTooHigh_shouldThrowIllegalArgumentExceptionAndNotCallDao() {
        int invalidScore = 101;
        List<Integer> tagIds = Arrays.asList(1, 2);

        doThrow(new IllegalArgumentException("The rating must be between 1 and 100."))
                .when(mockValidator).validateScore(invalidScore, testUserId, "Game");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> gameService.addGame(testGameName, testUserId, invalidScore, tagIds));

        assertEquals("The rating must be between 1 and 100.", exception.getMessage());
        verify(mockValidator).validateScore(invalidScore, testUserId, "Game");
        verifyNoInteractions(mockGameDao);
    }

    @Test
    void addGame_scoreAtLowerBoundary_shouldCallDaoAddGame() {
        int score = 1;
        List<Integer> tagIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(score, testUserId, "Game");
        when(mockGameDao.addGame(testGameName, testUserId, score, tagIds)).thenReturn(1);

        gameService.addGame(testGameName, testUserId, score, tagIds);

        verify(mockValidator).validateScore(score, testUserId, "Game");
        verify(mockGameDao).addGame(testGameName, testUserId, score, tagIds);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void addGame_scoreAtUpperBoundary_shouldCallDaoAddGame() {
        int score = 100;
        List<Integer> tagIds = Arrays.asList(1, 2);

        doNothing().when(mockValidator).validateScore(score, testUserId, "Game");
        when(mockGameDao.addGame(testGameName, testUserId, score, tagIds)).thenReturn(1);

        gameService.addGame(testGameName, testUserId, score, tagIds);

        verify(mockValidator).validateScore(score, testUserId, "Game");
        verify(mockGameDao).addGame(testGameName, testUserId, score, tagIds);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void addGame_daoThrowsIllegalArgumentException_shouldPropagate() {
        int validScore = 75;
        List<Integer> tagIds = Arrays.asList(1, 2);
        String daoExceptionMessage = "DAO: Game already exists";

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Game");
        doThrow(new IllegalArgumentException(daoExceptionMessage)).when(mockGameDao)
                .addGame(testGameName, testUserId, validScore, tagIds);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> gameService.addGame(testGameName, testUserId, validScore, tagIds));

        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockValidator).validateScore(validScore, testUserId, "Game");
        verify(mockGameDao).addGame(testGameName, testUserId, validScore, tagIds);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void addGame_daoThrowsOtherRuntimeException_shouldPropagate() {
        int validScore = 75;
        List<Integer> tagIds = Arrays.asList(1, 2);
        String daoExceptionMessage = "DAO: Database connection error";

        doNothing().when(mockValidator).validateScore(validScore, testUserId, "Game");
        doThrow(new RuntimeException(daoExceptionMessage)).when(mockGameDao)
                .addGame(testGameName, testUserId, validScore, tagIds);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> gameService.addGame(testGameName, testUserId, validScore, tagIds));

        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockValidator).validateScore(validScore, testUserId, "Game");
        verify(mockGameDao).addGame(testGameName, testUserId, validScore, tagIds);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void removeGame_shouldCallDaoRemoveGame() {
        doNothing().when(mockGameDao).removeGame(testGameName, testUserId);

        gameService.removeGame(testGameName, testUserId);

        verify(mockGameDao).removeGame(testGameName, testUserId);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void removeGame_daoThrowsIllegalArgumentException_shouldPropagate() {
        String daoExceptionMessage = "DAO: Game not found";
        doThrow(new IllegalArgumentException(daoExceptionMessage)).when(mockGameDao)
                .removeGame(testGameName, testUserId);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> gameService.removeGame(testGameName, testUserId));
        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockGameDao).removeGame(testGameName, testUserId);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void toggleGameStatus_shouldCallDaoToggleGameStatus() {
        doNothing().when(mockGameDao).toggleGameStatus(testGameName, testUserId);

        gameService.toggleGameStatus(testGameName, testUserId);

        verify(mockGameDao).toggleGameStatus(testGameName, testUserId);
        verifyNoMoreInteractions(mockGameDao);
    }

    @Test
    void toggleGameStatus_daoThrowsIllegalArgumentException_shouldPropagate() {
        String daoExceptionMessage = "DAO: Game not found for toggle";
        doThrow(new IllegalArgumentException(daoExceptionMessage)).when(mockGameDao)
                .toggleGameStatus(testGameName, testUserId);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> gameService.toggleGameStatus(testGameName, testUserId));
        assertEquals(daoExceptionMessage, exception.getMessage());
        verify(mockGameDao).toggleGameStatus(testGameName, testUserId);
        verifyNoMoreInteractions(mockGameDao);
    }
}