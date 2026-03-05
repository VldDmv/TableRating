package org.criticizer.controller.category;

import org.criticizer.dto.game.GameResponse;
import org.criticizer.entity.Game;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.game.GameService;
import org.criticizer.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameController Tests")
class GameControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private GameController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    @DisplayName("Should return correct entity name")
    void shouldReturnCorrectEntityName() {
        assertEquals("Game", controller.getEntityName());
    }

    @Test
    @DisplayName("Should convert Game to GameResponse")
    void shouldConvertGameToResponse() {
        // Given
        Game game = TestDataBuilder.createGame(1, "Test Game", 1, 85);

        // When
        GameResponse response = controller.convertToResponse(game);

        // Then
        assertEquals(game.getId(), response.getId());
        assertEquals(game.getName(), response.getName());
        assertEquals(game.getScore(), response.getScore());
    }
}
