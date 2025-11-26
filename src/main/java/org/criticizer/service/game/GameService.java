package org.criticizer.service.game;

import org.criticizer.entity.Game;
import org.criticizer.service.user.UserPageResult;

import java.util.List;

public interface GameService {
    List<Game> getUserGames(int userId);

    void addGame(String name, int userId, int score, List<Integer> tagIds);

    void removeGame(String name, int userId);

    void toggleGameStatus(String name, int userId);

    void updateGameAndName(String oldName, String newName, int newScore, int userId, List<Integer> tagIds);

    boolean isGameExists(String name, int userId);

    boolean getGameStatus(String name, int userId);

    UserPageResult<Game> getUserGamesPage(int userId, int page, int pageSize, Integer tagId, String searchTerm, String sortBy, String sortOrder);

}