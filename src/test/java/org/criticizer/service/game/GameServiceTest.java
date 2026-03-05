package org.criticizer.service.game;

import org.criticizer.dto.game.GameResponse;
import org.criticizer.entity.Game;
import org.criticizer.entity.Tag;
import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.repository.GameRepository;
import org.criticizer.repository.TagRepository;
import org.criticizer.service.helper.ServiceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for GameService.
 * Tests the concrete implementation of AbstractMediaService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameService Tests")
class GameServiceTest {

    private final int TEST_USER_ID = 1;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private ServiceValidator validator;
    @InjectMocks
    private GameService gameService;
    private Game testGame;
    private Tag testTag;

    @BeforeEach
    void setUp() {
        testGame = new Game(1, "The Witcher 3", TEST_USER_ID, 95, false);
        testGame.setCoverUrl("http://example.com/cover.jpg");

        testTag = new Tag(1, "RPG");
        testGame.setTags(new HashSet<>(List.of(testTag)));
    }

    // ==================== ADD ITEM TESTS ====================

    @Nested
    @DisplayName("addItem() Tests")
    class AddItemTests {

        @Test
        @DisplayName("Should successfully add game with tags")
        void shouldAddGameWithTags() {
            // Given
            String name = "Elden Ring";
            String coverUrl = "http://example.com/elden.jpg";
            int score = 90;
            List<Integer> tagIds = Arrays.asList(1, 2);

            Tag tag1 = new Tag(1, "RPG");
            Tag tag2 = new Tag(2, "Open World");

            when(validator.validateName(name, "Game name")).thenReturn(name);
            doNothing().when(validator).validateScore(score, TEST_USER_ID, "Game");
            when(gameRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(false);

            // Mock saving entity and returning it with ID
            when(gameRepository.save(any(Game.class)))
                    .thenAnswer(invocation -> {
                        Game game = invocation.getArgument(0);
                        if (game.getId() == null) {
                            // First save - set ID
                            Game savedGame = new Game(100, game.getName(), game.getUserId(),
                                    game.getScore(), game.isCompleted());
                            savedGame.setCoverUrl(game.getCoverUrl());
                            savedGame.setTags(game.getTags());
                            return savedGame;
                        }
                        return game;
                    });

            when(tagRepository.findAllById(tagIds)).thenReturn(Arrays.asList(tag1, tag2));

            // When
            gameService.addItem(name, coverUrl, TEST_USER_ID, score, tagIds);

            // Then
            verify(validator).validateName(name, "Game name");
            verify(validator).validateScore(score, TEST_USER_ID, "Game");
            verify(gameRepository).existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID);
            verify(tagRepository).findAllById(tagIds);

            // Verify save was called twice (once for entity, once for tags)
            verify(gameRepository, times(2)).save(argThat(game ->
                    game.getName().equals(name) &&
                            game.getCoverUrl().equals(coverUrl) &&
                            game.getScore() == score &&
                            game.getUserId() == TEST_USER_ID
            ));
        }

        @Test
        @DisplayName("Should add game without tags")
        void shouldAddGameWithoutTags() {
            // Given
            String name = "Portal 2";
            when(validator.validateName(name, "Game name")).thenReturn(name);
            when(gameRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(false);
            when(gameRepository.save(any(Game.class)))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            gameService.addItem(name, null, TEST_USER_ID, 85, null);

            // Then
            verify(gameRepository).save(argThat(game ->
                    game.getName().equals(name) &&
                            game.getCoverUrl() == null
            ));
            verify(tagRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("Should throw when game name already exists for user")
        void shouldThrowWhenGameExists() {
            // Given
            String name = "Existing Game";
            when(validator.validateName(name, "Game name")).thenReturn(name);
            when(gameRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() ->
                    gameService.addItem(name, null, TEST_USER_ID, 80, null))
                    .isInstanceOf(ItemAlreadyExistsException.class)
                    .hasMessageContaining(name);

            verify(gameRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should validate score before adding")
        void shouldValidateScore() {
            // Given
            String name = "Test Game";
            int invalidScore = 150;

            when(validator.validateName(name, "Game name")).thenReturn(name);
            doThrow(new org.criticizer.exceptions.validation.InvalidScoreException(
                    invalidScore, "Game"))
                    .when(validator).validateScore(invalidScore, TEST_USER_ID, "Game");

            // When & Then
            assertThatThrownBy(() ->
                    gameService.addItem(name, null, TEST_USER_ID, invalidScore, null))
                    .isInstanceOf(org.criticizer.exceptions.validation.InvalidScoreException.class);

            verify(gameRepository, never()).save(any());
        }
    }

    // ==================== UPDATE ITEM TESTS ====================

    @Nested
    @DisplayName("updateItem() Tests")
    class UpdateItemTests {

        @Test
        @DisplayName("Should successfully update game")
        void shouldUpdateGame() {
            // Given
            String oldName = "Old Name";
            String newName = "New Name";
            String newCoverUrl = "http://example.com/new.jpg";
            int newScore = 88;
            List<Integer> newTagIds = List.of(2);

            Tag newTag = new Tag(2, "Action");

            when(validator.validateName(oldName, "Old Game name")).thenReturn(oldName);
            when(validator.validateName(newName, "New Game name")).thenReturn(newName);
            doNothing().when(validator).validateScore(newScore, TEST_USER_ID, "Game");

            when(gameRepository.findByNameIgnoreCaseAndUserId(oldName, TEST_USER_ID))
                    .thenReturn(Optional.of(testGame));
            when(gameRepository.existsByNameIgnoreCaseAndUserId(newName, TEST_USER_ID))
                    .thenReturn(false);
            when(tagRepository.findAllById(newTagIds)).thenReturn(List.of(newTag));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            // When
            gameService.updateItem(oldName, newName, newCoverUrl, newScore,
                    TEST_USER_ID, newTagIds);

            // Then
            verify(gameRepository).save(argThat(game ->
                    game.getName().equals(newName) &&
                            game.getCoverUrl().equals(newCoverUrl) &&
                            game.getScore() == newScore &&
                            game.getTags().contains(newTag)
            ));
        }

        @Test
        @DisplayName("Should throw when old game not found")
        void shouldThrowWhenOldGameNotFound() {
            // Given
            String oldName = "Nonexistent";
            String newName = "New Name";

            when(validator.validateName(oldName, "Old Game name")).thenReturn(oldName);
            when(validator.validateName(newName, "New Game name")).thenReturn(newName);
            when(gameRepository.findByNameIgnoreCaseAndUserId(oldName, TEST_USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    gameService.updateItem(oldName, newName, null, 80, TEST_USER_ID, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(oldName);
        }

        @Test
        @DisplayName("Should throw when new name conflicts with existing game")
        void shouldThrowWhenNewNameConflicts() {
            // Given
            String oldName = "Game 1";
            String newName = "Game 2";

            when(validator.validateName(oldName, "Old Game name")).thenReturn(oldName);
            when(validator.validateName(newName, "New Game name")).thenReturn(newName);
            when(gameRepository.findByNameIgnoreCaseAndUserId(oldName, TEST_USER_ID))
                    .thenReturn(Optional.of(testGame));
            when(gameRepository.existsByNameIgnoreCaseAndUserId(newName, TEST_USER_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() ->
                    gameService.updateItem(oldName, newName, null, 80, TEST_USER_ID, null))
                    .isInstanceOf(ItemAlreadyExistsException.class)
                    .hasMessageContaining(newName);
        }

        @Test
        @DisplayName("Should allow updating same game with case change")
        void shouldAllowCaseChange() {
            // Given
            String oldName = "game";
            String newName = "GAME";

            when(validator.validateName(oldName, "Old Game name")).thenReturn(oldName);
            when(validator.validateName(newName, "New Game name")).thenReturn(newName);
            when(gameRepository.findByNameIgnoreCaseAndUserId(oldName, TEST_USER_ID))
                    .thenReturn(Optional.of(testGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            // When & Then (should not throw)
            assertThatCode(() ->
                    gameService.updateItem(oldName, newName, null, 80, TEST_USER_ID, null))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== REMOVE ITEM TESTS ====================

    @Nested
    @DisplayName("removeItem() Tests")
    class RemoveItemTests {

        @Test
        @DisplayName("Should successfully remove game")
        void shouldRemoveGame() {
            // Given
            String name = "The Witcher 3";
            when(validator.validateName(name, "Game name")).thenReturn(name);
            when(gameRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testGame));
            doNothing().when(gameRepository).delete(testGame);

            // When
            gameService.removeItem(name, TEST_USER_ID);

            // Then
            verify(gameRepository).delete(testGame);
        }

        @Test
        @DisplayName("Should throw when game not found")
        void shouldThrowWhenGameNotFound() {
            // Given
            String name = "Nonexistent Game";
            when(validator.validateName(name, "Game name")).thenReturn(name);
            when(gameRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> gameService.removeItem(name, TEST_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(name);

            verify(gameRepository, never()).delete(any());
        }
    }

    // ==================== TOGGLE STATUS TESTS ====================

    @Nested
    @DisplayName("toggleStatus() Tests")
    class ToggleStatusTests {

        @Test
        @DisplayName("Should toggle completion status from false to true")
        void shouldToggleToCompleted() {
            // Given
            testGame.setCompleted(false);
            String name = "The Witcher 3";

            when(validator.validateName(name, "Game name")).thenReturn(name);
            when(gameRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            // When
            GameResponse result = gameService.toggleStatus(name, TEST_USER_ID);

            // Then
            assertThat(result.isCompleted()).isTrue();
            verify(gameRepository).save(argThat(game -> game.isCompleted()));
        }

        @Test
        @DisplayName("Should toggle completion status from true to false")
        void shouldToggleToIncomplete() {
            // Given
            testGame.setCompleted(true);
            String name = "The Witcher 3";

            when(validator.validateName(name, "Game name")).thenReturn(name);
            when(gameRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            // When
            GameResponse result = gameService.toggleStatus(name, TEST_USER_ID);

            // Then
            assertThat(result.isCompleted()).isFalse();
            verify(gameRepository).save(argThat(game -> !game.isCompleted()));
        }
    }

    // ==================== GET USER ITEMS PAGE TESTS ====================

    @Nested
    @DisplayName("getUserItemsPage() Tests")
    class GetUserItemsPageTests {

        @Test
        @DisplayName("Should return paginated games")
        void shouldReturnPaginatedGames() {
            // Given
            ServiceValidator.PaginationParams params =
                    new ServiceValidator.PaginationParams(1, 10, 0);

            when(validator.validatePagination(1, 10)).thenReturn(params);
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            // Mock ID pagination step
            Page<Integer> gameIds = new PageImpl<>(
                    Arrays.asList(1, 2),
                    PageRequest.of(0, 10),
                    2
            );
            when(gameRepository.findItemIds(eq(TEST_USER_ID), isNull(), isNull(), any()))
                    .thenReturn(gameIds);

            // Mock fetching full entities
            Game game1 = new Game(1, "Game 1", TEST_USER_ID, 80, false);
            Game game2 = new Game(2, "Game 2", TEST_USER_ID, 85, false);
            when(gameRepository.findByIdsWithCategories(Arrays.asList(1, 2)))
                    .thenReturn(Arrays.asList(game1, game2));

            // When
            var result = gameService.getUserItemsPage(
                    TEST_USER_ID, 1, 10, null, null, "name", "asc"
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getCurrentPage()).isEqualTo(1);
            assertThat(result.getTotalItems()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should filter by search term")
        void shouldFilterBySearchTerm() {
            // Given
            ServiceValidator.PaginationParams params =
                    new ServiceValidator.PaginationParams(1, 10, 0);

            when(validator.validatePagination(1, 10)).thenReturn(params);
            when(validator.sanitizeSearchTerm("witcher")).thenReturn("witcher");

            Page<Integer> gameIds = new PageImpl<>(
                    List.of(1),
                    PageRequest.of(0, 10),
                    1
            );
            when(gameRepository.findItemIds(
                    eq(TEST_USER_ID), isNull(), eq("witcher"), any()))
                    .thenReturn(gameIds);

            when(gameRepository.findByIdsWithCategories(List.of(1)))
                    .thenReturn(List.of(testGame));

            // When
            var result = gameService.getUserItemsPage(
                    TEST_USER_ID, 1, 10, null, "witcher", "name", "asc"
            );

            // Then
            assertThat(result.getItems()).hasSize(1);
            verify(validator).sanitizeSearchTerm("witcher");
        }

        @Test
        @DisplayName("Should filter by tag")
        void shouldFilterByTag() {
            // Given
            ServiceValidator.PaginationParams params =
                    new ServiceValidator.PaginationParams(1, 10, 0);

            when(validator.validatePagination(1, 10)).thenReturn(params);
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            Page<Integer> gameIds = new PageImpl<>(
                    List.of(1),
                    PageRequest.of(0, 10),
                    1
            );
            when(gameRepository.findItemIds(
                    eq(TEST_USER_ID), eq(1), isNull(), any()))
                    .thenReturn(gameIds);

            when(gameRepository.findByIdsWithCategories(List.of(1)))
                    .thenReturn(List.of(testGame));

            // When
            var result = gameService.getUserItemsPage(
                    TEST_USER_ID, 1, 10, 1, null, "name", "asc"
            );

            // Then
            assertThat(result.getItems()).hasSize(1);
            verify(gameRepository).findItemIds(eq(TEST_USER_ID), eq(1), isNull(), any());
        }
    }

    // ==================== UPDATE COVER TESTS ====================

    @Nested
    @DisplayName("updateCover() Tests")
    class UpdateCoverTests {

        @Test
        @DisplayName("Should update cover URL")
        void shouldUpdateCoverUrl() {
            // Given
            String name = "The Witcher 3";
            String newCoverUrl = "http://example.com/new-cover.jpg";

            when(validator.validateName(name, "Game name")).thenReturn(name);
            when(gameRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            // When
            gameService.updateCover(name, newCoverUrl, TEST_USER_ID);

            // Then
            verify(gameRepository).save(argThat(game ->
                    game.getCoverUrl().equals(newCoverUrl)
            ));
        }

        @Test
        @DisplayName("Should remove cover when URL is null")
        void shouldRemoveCoverWhenNull() {
            // Given
            String name = "The Witcher 3";

            when(validator.validateName(name, "Game name")).thenReturn(name);
            when(gameRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            // When
            gameService.updateCover(name, null, TEST_USER_ID);

            // Then
            verify(gameRepository).save(argThat(game ->
                    game.getCoverUrl() == null
            ));
        }

        @Test
        @DisplayName("Should remove cover when URL is empty")
        void shouldRemoveCoverWhenEmpty() {
            // Given
            String name = "The Witcher 3";

            when(validator.validateName(name, "Game name")).thenReturn(name);
            when(gameRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testGame));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            // When
            gameService.updateCover(name, "   ", TEST_USER_ID);

            // Then
            verify(gameRepository).save(argThat(game ->
                    game.getCoverUrl() == null
            ));
        }
    }

    // ==================== UTILITY METHODS TESTS ====================

    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {

        @Test
        @DisplayName("Should check if item exists")
        void shouldCheckIfItemExists() {
            // Given
            String name = "The Witcher 3";
            when(validator.validateName(name, "Game name")).thenReturn(name);
            when(gameRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(true);

            // When
            boolean result = gameService.isItemExists(name, TEST_USER_ID);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should get item status")
        void shouldGetItemStatus() {
            // Given
            String name = "The Witcher 3";
            testGame.setCompleted(true);

            when(validator.validateName(name, "Game name")).thenReturn(name);
            when(gameRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testGame));

            // When
            boolean result = gameService.getItemStatus(name, TEST_USER_ID);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return correct media type")
        void shouldReturnCorrectMediaType() {
            // When
            String mediaType = gameService.getMediaType();

            // Then
            assertThat(mediaType).isEqualTo("games");
        }
    }
}