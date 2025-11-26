package org.criticizer.service.game;

import org.criticizer.dao.game.GameDao;
import org.criticizer.entity.Game;
import org.criticizer.service.helper.ServiceValidator;
import org.criticizer.service.user.UserPageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of the GameService interface for managing game-related operations.
 */
public class GameServiceImpl implements GameService {
    private static final Logger log = LoggerFactory.getLogger(GameServiceImpl.class);
    private final ServiceValidator validator;
    private final GameDao gameDao;

    /**
     * Constructs a new GameServiceImpl with the specified DAO and validator.
     */
    public GameServiceImpl(GameDao gameDao, ServiceValidator validator) {
        this.gameDao = gameDao;
        this.validator = validator;
    }

    /**
     * Retrieves all games associated with a user.
     */
    @Override
    public List<Game> getUserGames(int userId) {
        return gameDao.getUserGames(userId);
    }

    /**
     * Checks the completion status of a game for a user.
     */
    @Override
    public boolean getGameStatus(String name, int userId) {
        return gameDao.getGameStatus(name, userId);
    }

    /**
     * Updates a game's name, score, and associated tags for a user.
     */
    @Override
    public void updateGameAndName(String oldName, String newName, int newScore, int userId, List<Integer> tagIds) {
        validator.validateScore(newScore, userId, "Game");

        log.info("Updating game '{}' to '{}' with score {} for user {}", oldName, newName, newScore, userId);
        gameDao.updateGameAndName(oldName, newName, newScore, userId, tagIds);
    }

    /**
     * Checks if a game exists for a user.
     */
    @Override
    public boolean isGameExists(String name, int userId) {
        return gameDao.isGameExists(name, userId);
    }

    /**
     * Adds a new game for a user with the specified score and tags.
     */
    @Override
    public void addGame(String name, int userId, int score, List<Integer> tagIds) {
        validator.validateScore(score, userId, "Game");

        log.info("Adding game '{}' with score {} for user {}", name, score, userId);
        gameDao.addGame(name, userId, score, tagIds);
    }

    /**
     * Retrieves a paginated list of games for a user.
     */
    @Override
    public UserPageResult<Game> getUserGamesPage(int userId, int page, int pageSize,
                                                 Integer tagId, String searchTerm,
                                                 String sortBy, String sortOrder) {
        // Validate and sanitize pagination parameters
        ServiceValidator.PaginationParams params = validator.validatePagination(page, pageSize);
        String sanitizedSearch = validator.sanitizeSearchTerm(searchTerm);

        // Fetch paginated data
        List<Game> gamesOnPage = gameDao.findGamesByUserId(
                userId, tagId, sanitizedSearch,
                params.offset(), params.pageSize(),
                sortBy, sortOrder
        );

        // Get total count for pagination
        int totalGames = gameDao.countTotalForUser(userId, tagId, sanitizedSearch);

        return new UserPageResult<>(gamesOnPage, totalGames, params.page(), params.pageSize());
    }

    /**
     * Removes a game for a user.
     */
    @Override
    public void removeGame(String name, int userId) {
        log.info("Removing game '{}' for user {}", name, userId);
        gameDao.removeGame(name, userId);
    }

    /**
     * Toggles the completion status of a game for a user.
     */
    @Override
    public void toggleGameStatus(String name, int userId) {
        log.debug("Toggling status for game '{}' for user {}", name, userId);
        gameDao.toggleGameStatus(name, userId);
    }
}