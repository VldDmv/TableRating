package org.criticizer.servlets.items;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.criticizer.entity.Game;
import org.criticizer.service.game.GameService;
import org.criticizer.service.tag.TagService;
import org.criticizer.service.user.UserPageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.criticizer.constants.AttribConstants.*;

/**
 * Servlet for managing Games.
 */
@WebServlet("/games")
public class GamesServlet extends AbstractCategoryServlet<Game, GameService> {
    private static final Logger log = LoggerFactory.getLogger(GamesServlet.class);

    public GamesServlet() {
        super(log);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Get GameService using ServletHelper
        this.service = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.GAME_SERVICE,
                GameService.class
        );

        log.info("GamesServlet initialized successfully");
    }

    @Override
    protected String getCategoryName() {
        return Categories.GAMES;
    }

    @Override
    protected String getEntityNameSingular() {
        return EntityNames.GAME_SINGULAR;
    }

    @Override
    protected String getEntityNamePlural() {
        return EntityNames.GAME_PLURAL;
    }

    @Override
    protected Map<String, String> getParamNames() {
        return Map.of(
                "addItemName", "gameName",
                "addItemScore", "gameScore",
                "addItemTagIds", "gameTagIds",
                "removeItem", "removeGame",
                "toggleItemStatus", "toggleGameStatus",
                "oldItemName", "oldGameName",
                "updatedItemName", "updatedGameName",
                "updatedItemScore", "updatedGameScore",
                "updatedItemTagIds", "updatedGameTagIds"
        );
    }

    @Override
    protected String getAddFormId() {
        return "add-game-form";
    }

    @Override
    protected void setAssociations(HttpServletRequest request) {
        // Get TagService safely using ServletHelper
        TagService tagService = ServletHelper.getService(
                request,
                ServiceNames.TAG_SERVICE,
                TagService.class
        );

        // Set tags for the JSP
        request.setAttribute(RequestAttributes.ALL_TAGS, tagService.getAllTags());
    }

    @Override
    protected UserPageResult<Game> getPage(int userId, int page, int pageSize,
                                           Integer tagId, String searchTerm,
                                           String sortBy, String sortOrder) {
        return service.getUserGamesPage(userId, page, pageSize, tagId, searchTerm, sortBy, sortOrder);
    }

    @Override
    protected void addItem(String name, int userId, int score, List<Integer> tagIds) {
        service.addGame(name, userId, score, tagIds);
    }

    @Override
    protected void removeItem(String name, int userId) {
        service.removeGame(name, userId);
    }

    @Override
    protected void updateItem(String oldName, String newName, int newScore,
                              int userId, List<Integer> tagIds) {
        service.updateGameAndName(oldName, newName, newScore, userId, tagIds);
    }

    @Override
    protected boolean getItemStatus(String name, int userId) {
        return service.getGameStatus(name, userId);
    }

    @Override
    protected void toggleItemStatus(String name, int userId) {
        service.toggleGameStatus(name, userId);
    }
}