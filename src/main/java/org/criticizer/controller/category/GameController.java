package org.criticizer.controller.category;

import org.criticizer.controller.helper.AbstractMediaController;
import org.criticizer.dto.game.CreateGameRequest;
import org.criticizer.dto.game.GameResponse;
import org.criticizer.dto.game.UpdateGameRequest;
import org.criticizer.entity.Game;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.game.GameService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST Controller for Game management. All endpoints require authentication. */
@RestController
@RequestMapping("/api/games")
public class GameController
        extends AbstractMediaController<Game, GameResponse, CreateGameRequest, UpdateGameRequest> {

    public GameController(GameService gameService, SecurityUtil securityUtil) {
        super(gameService, securityUtil);
    }

    @Override
    protected String getEntityName() {
        return "Game";
    }

    @Override
    protected GameResponse convertToResponse(Game entity) {
        return GameResponse.from(entity);
    }
}
