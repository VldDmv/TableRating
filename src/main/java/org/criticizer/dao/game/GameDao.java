package org.criticizer.dao.game;

import org.criticizer.entity.Game;

import java.util.List;


public interface GameDao {
    List<Game> getUserGames(int userId);

    int addGame(String name, int userId, int score, List<Integer> tagIds);

    void removeGame(String name, int userId);

    void toggleGameStatus(String name, int userId);

    void updateGameAndName(String oldName, String newName, int newScore, int userId, List<Integer> tagIds);

    boolean isGameExists(String name, int userId);

    boolean getGameStatus(String name, int userId);

    void deleteGamesByUserId(int userId);

    int countTotal();

    List<Game> findGamesByUserId(int userId, Integer tagId, String searchTerm, int offset, int limit, String sortBy, String sortOrder);

    int countTotalForUser(int userId, Integer tagId, String searchTerm);
}