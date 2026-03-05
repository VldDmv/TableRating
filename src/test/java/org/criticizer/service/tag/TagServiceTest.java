package org.criticizer.service.tag;

import org.criticizer.dto.tag.CreateTagRequest;
import org.criticizer.dto.tag.TagResponse;
import org.criticizer.dto.tag.UpdateTagRequest;
import org.criticizer.entity.Game;
import org.criticizer.entity.Tag;
import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.exceptions.validation.EmptyNameException;
import org.criticizer.repository.GameRepository;
import org.criticizer.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TagService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TagService Tests")
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private TagService tagService;

    private Tag testTag;

    @BeforeEach
    void setUp() {
        testTag = new Tag(1, "RPG");
    }

    // ==================== GET ALL TAGS TESTS ====================

    @Nested
    @DisplayName("getAllTags() Tests")
    class GetAllTagsTests {

        @Test
        @DisplayName("Should return all tags sorted by name")
        void shouldReturnAllTagsSorted() {
            // Given
            Tag tag1 = new Tag(1, "Action");
            Tag tag2 = new Tag(2, "RPG");
            Tag tag3 = new Tag(3, "Strategy");

            when(tagRepository.findAllByOrderByNameAsc())
                    .thenReturn(Arrays.asList(tag1, tag2, tag3));

            // When
            List<TagResponse> result = tagService.getAllTags();

            // Then
            assertThat(result).hasSize(3);
            assertThat(result)
                    .extracting(TagResponse::getName)
                    .containsExactly("Action", "RPG", "Strategy");
        }

        @Test
        @DisplayName("Should return empty list when no tags exist")
        void shouldReturnEmptyListWhenNoTags() {
            // Given
            when(tagRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

            // When
            List<TagResponse> result = tagService.getAllTags();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should map Tag entity to TagResponse correctly")
        void shouldMapCorrectly() {
            // Given
            when(tagRepository.findAllByOrderByNameAsc())
                    .thenReturn(List.of(testTag));

            // When
            List<TagResponse> result = tagService.getAllTags();

            // Then
            assertThat(result).hasSize(1);
            TagResponse response = result.get(0);
            assertThat(response.getId()).isEqualTo(1);
            assertThat(response.getName()).isEqualTo("RPG");
        }
    }

    // ==================== GET TAGS FOR GAME TESTS ====================

    @Nested
    @DisplayName("getTagsForGame() Tests")
    class GetTagsForGameTests {

        @Test
        @DisplayName("Should return tags for specific game")
        void shouldReturnTagsForGame() {
            // Given
            Tag tag1 = new Tag(1, "RPG");
            Tag tag2 = new Tag(2, "Open World");

            when(tagRepository.findByGameId(1))
                    .thenReturn(Arrays.asList(tag1, tag2));

            // When
            List<TagResponse> result = tagService.getTagsForGame(1);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(TagResponse::getName)
                    .containsExactlyInAnyOrder("RPG", "Open World");
        }

        @Test
        @DisplayName("Should return empty list when game has no tags")
        void shouldReturnEmptyListWhenNoTags() {
            // Given
            when(tagRepository.findByGameId(1)).thenReturn(List.of());

            // When
            List<TagResponse> result = tagService.getTagsForGame(1);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle different game IDs")
        void shouldHandleDifferentGameIds() {
            // Given
            when(tagRepository.findByGameId(1))
                    .thenReturn(List.of(new Tag(1, "RPG")));
            when(tagRepository.findByGameId(2))
                    .thenReturn(List.of(new Tag(2, "Action")));

            // When
            List<TagResponse> result1 = tagService.getTagsForGame(1);
            List<TagResponse> result2 = tagService.getTagsForGame(2);

            // Then
            assertThat(result1).hasSize(1);
            assertThat(result1.get(0).getName()).isEqualTo("RPG");
            assertThat(result2).hasSize(1);
            assertThat(result2.get(0).getName()).isEqualTo("Action");
        }
    }

    // ==================== CREATE TAG TESTS ====================

    @Nested
    @DisplayName("createTag() Tests")
    class CreateTagTests {

        @Test
        @DisplayName("Should successfully create new tag")
        void shouldCreateNewTag() {
            // Given
            CreateTagRequest request = new CreateTagRequest("Strategy");
            Tag savedTag = new Tag(1, "Strategy");

            when(tagRepository.existsByNameIgnoreCase("Strategy")).thenReturn(false);
            when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

            // When
            TagResponse result = tagService.createTag(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getName()).isEqualTo("Strategy");

            verify(tagRepository).existsByNameIgnoreCase("Strategy");
            verify(tagRepository).save(argThat(tag ->
                    tag.getName().equals("Strategy")
            ));
        }

        @Test
        @DisplayName("Should trim whitespace from tag name")
        void shouldTrimWhitespace() {
            // Given
            CreateTagRequest request = new CreateTagRequest("  Action  ");
            Tag savedTag = new Tag(1, "Action");

            when(tagRepository.existsByNameIgnoreCase("Action")).thenReturn(false);
            when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

            // When
            TagResponse result = tagService.createTag(request);

            // Then
            assertThat(result.getName()).isEqualTo("Action");
            verify(tagRepository).save(argThat(tag ->
                    tag.getName().equals("Action") && !tag.getName().contains(" ")
            ));
        }

        @Test
        @DisplayName("Should throw when tag name is empty")
        void shouldThrowWhenNameIsEmpty() {
            // Given
            CreateTagRequest request = new CreateTagRequest("  ");

            // When & Then
            assertThatThrownBy(() -> tagService.createTag(request))
                    .isInstanceOf(EmptyNameException.class)
                    .hasMessageContaining("Tag name");

            verify(tagRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when tag name is null")
        void shouldThrowWhenNameIsNull() {
            // Given
            CreateTagRequest request = new CreateTagRequest(null);

            // When & Then
            assertThatThrownBy(() -> tagService.createTag(request))
                    .isInstanceOf(EmptyNameException.class);

            verify(tagRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when tag already exists")
        void shouldThrowWhenTagExists() {
            // Given
            CreateTagRequest request = new CreateTagRequest("RPG");
            when(tagRepository.existsByNameIgnoreCase("RPG")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> tagService.createTag(request))
                    .isInstanceOf(ItemAlreadyExistsException.class)
                    .hasMessageContaining("RPG");

            verify(tagRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should be case-insensitive when checking duplicates")
        void shouldBeCaseInsensitiveForDuplicates() {
            // Given
            CreateTagRequest request = new CreateTagRequest("rpg");
            when(tagRepository.existsByNameIgnoreCase("rpg")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> tagService.createTag(request))
                    .isInstanceOf(ItemAlreadyExistsException.class);
        }
    }

    // ==================== UPDATE TAG TESTS ====================

    @Nested
    @DisplayName("updateTag() Tests")
    class UpdateTagTests {

        @Test
        @DisplayName("Should successfully update tag name")
        void shouldUpdateTagName() {
            // Given
            UpdateTagRequest request = new UpdateTagRequest(1, "Action-Adventure");
            Tag updatedTag = new Tag(1, "Action-Adventure");

            when(tagRepository.findById(1)).thenReturn(Optional.of(testTag));
            when(tagRepository.findByNameIgnoreCase("Action-Adventure"))
                    .thenReturn(Optional.empty());
            when(tagRepository.save(any(Tag.class))).thenReturn(updatedTag);

            // When
            TagResponse result = tagService.updateTag(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getName()).isEqualTo("Action-Adventure");

            verify(tagRepository).save(argThat(tag ->
                    tag.getId().equals(1) &&
                            tag.getName().equals("Action-Adventure")
            ));
        }

        @Test
        @DisplayName("Should trim whitespace when updating")
        void shouldTrimWhitespace() {
            // Given
            UpdateTagRequest request = new UpdateTagRequest(1, "  Strategy  ");
            Tag updatedTag = new Tag(1, "Strategy");

            when(tagRepository.findById(1)).thenReturn(Optional.of(testTag));
            when(tagRepository.findByNameIgnoreCase("Strategy"))
                    .thenReturn(Optional.empty());
            when(tagRepository.save(any(Tag.class))).thenReturn(updatedTag);

            // When
            TagResponse result = tagService.updateTag(request);

            // Then
            assertThat(result.getName()).isEqualTo("Strategy");
            verify(tagRepository).save(argThat(tag ->
                    !tag.getName().contains(" ")
            ));
        }

        @Test
        @DisplayName("Should throw when tag not found")
        void shouldThrowWhenTagNotFound() {
            // Given
            UpdateTagRequest request = new UpdateTagRequest(999, "New Name");
            when(tagRepository.findById(999)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.updateTag(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");

            verify(tagRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when new name is empty")
        void shouldThrowWhenNewNameIsEmpty() {
            // Given
            UpdateTagRequest request = new UpdateTagRequest(1, "   ");

            // When & Then
            assertThatThrownBy(() -> tagService.updateTag(request))
                    .isInstanceOf(EmptyNameException.class);

            verify(tagRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw when new name conflicts with existing tag")
        void shouldThrowWhenNameConflicts() {
            // Given
            UpdateTagRequest request = new UpdateTagRequest(1, "Action");
            Tag existingTag = new Tag(2, "Action");

            when(tagRepository.findById(1)).thenReturn(Optional.of(testTag));
            when(tagRepository.findByNameIgnoreCase("Action"))
                    .thenReturn(Optional.of(existingTag));

            // When & Then
            assertThatThrownBy(() -> tagService.updateTag(request))
                    .isInstanceOf(ItemAlreadyExistsException.class)
                    .hasMessageContaining("Action");

            verify(tagRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow updating same tag with case change")
        void shouldAllowCaseChange() {
            // Given
            UpdateTagRequest request = new UpdateTagRequest(1, "rpg");

            when(tagRepository.findById(1)).thenReturn(Optional.of(testTag));
            when(tagRepository.findByNameIgnoreCase("rpg"))
                    .thenReturn(Optional.of(testTag)); // Same ID

            Tag updatedTag = new Tag(1, "rpg");
            when(tagRepository.save(any(Tag.class))).thenReturn(updatedTag);

            // When & Then
            assertThatCode(() -> tagService.updateTag(request))
                    .doesNotThrowAnyException();

            verify(tagRepository).save(any(Tag.class));
        }
    }

    // ==================== DELETE TAG TESTS ====================

    @Nested
    @DisplayName("deleteTag() Tests")
    class DeleteTagTests {

        @Test
        @DisplayName("Should delete tag and call removeTagFromAll")
        void shouldDeleteTagWithCascade() {
            // Given
            when(tagRepository.findById(1)).thenReturn(Optional.of(testTag));
            doNothing().when(gameRepository).removeTagFromAll(1);
            doNothing().when(tagRepository).deleteById(1);

            // When
            tagService.deleteTag(1);

            // Then
            verify(gameRepository).removeTagFromAll(1);
            verify(gameRepository, never()).save(any(Game.class));
            verify(tagRepository).deleteById(1);
        }

        @Test
        @DisplayName("Should call removeTagFromAll even when tag is not used by any games")
        void shouldDeleteUnusedTag() {
            // Given
            when(tagRepository.findById(1)).thenReturn(Optional.of(testTag));
            doNothing().when(gameRepository).removeTagFromAll(1);
            doNothing().when(tagRepository).deleteById(1);

            // When
            tagService.deleteTag(1);

            // Then
            verify(gameRepository).removeTagFromAll(1);
            verify(tagRepository).deleteById(1);
        }

        @Test
        @DisplayName("Should throw when tag not found")
        void shouldThrowWhenTagNotFound() {
            // Given
            when(tagRepository.findById(999)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.deleteTag(999))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");

            verify(tagRepository, never()).deleteById(any());
            verify(gameRepository, never()).removeTagFromAll(any());
        }

        @Test
        @DisplayName("Should delete in correct order: remove from games first, then delete tag")
        void shouldDeleteInCorrectOrder() {
            // Given
            when(tagRepository.findById(1)).thenReturn(Optional.of(testTag));
            doNothing().when(gameRepository).removeTagFromAll(1);
            doNothing().when(tagRepository).deleteById(1);

            // When
            tagService.deleteTag(1);

            // Then - removeTagFromAll must happen before deleteById
            var inOrder = inOrder(gameRepository, tagRepository);
            inOrder.verify(gameRepository).removeTagFromAll(1);
            inOrder.verify(tagRepository).deleteById(1);
        }
    }

    // ==================== IS TAG IN USE TESTS ====================

    @Nested
    @DisplayName("isTagInUse() Tests")
    class IsTagInUseTests {

        @Test
        @DisplayName("Should return true when tag is used by games")
        void shouldReturnTrueWhenInUse() {
            // Given
            when(tagRepository.isTagInUse(1)).thenReturn(true);

            // When
            boolean result = tagService.isTagInUse(1);

            // Then
            assertThat(result).isTrue();
            verify(tagRepository).isTagInUse(1);
        }

        @Test
        @DisplayName("Should return false when tag is not used")
        void shouldReturnFalseWhenNotInUse() {
            // Given
            when(tagRepository.isTagInUse(1)).thenReturn(false);

            // When
            boolean result = tagService.isTagInUse(1);

            // Then
            assertThat(result).isFalse();
            verify(tagRepository).isTagInUse(1);
        }

        @Test
        @DisplayName("Should check usage for different tag IDs")
        void shouldCheckDifferentTags() {
            // Given
            when(tagRepository.isTagInUse(1)).thenReturn(true);
            when(tagRepository.isTagInUse(2)).thenReturn(false);

            // When
            boolean result1 = tagService.isTagInUse(1);
            boolean result2 = tagService.isTagInUse(2);

            // Then
            assertThat(result1).isTrue();
            assertThat(result2).isFalse();
        }
    }

    // ==================== EDGE CASES TESTS ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle tag with special characters")
        void shouldHandleSpecialCharacters() {
            // Given
            CreateTagRequest request = new CreateTagRequest("Sci-Fi/Fantasy");
            Tag savedTag = new Tag(1, "Sci-Fi/Fantasy");

            when(tagRepository.existsByNameIgnoreCase("Sci-Fi/Fantasy"))
                    .thenReturn(false);
            when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

            // When
            TagResponse result = tagService.createTag(request);

            // Then
            assertThat(result.getName()).isEqualTo("Sci-Fi/Fantasy");
        }

        @Test
        @DisplayName("Should handle tag with numbers")
        void shouldHandleNumbers() {
            // Given
            CreateTagRequest request = new CreateTagRequest("4X Strategy");
            Tag savedTag = new Tag(1, "4X Strategy");

            when(tagRepository.existsByNameIgnoreCase("4X Strategy"))
                    .thenReturn(false);
            when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

            // When
            TagResponse result = tagService.createTag(request);

            // Then
            assertThat(result.getName()).isEqualTo("4X Strategy");
        }

        @Test
        @DisplayName("Should handle very long tag name")
        void shouldHandleLongName() {
            // Given
            String longName = "A".repeat(255);
            CreateTagRequest request = new CreateTagRequest(longName);
            Tag savedTag = new Tag(1, longName);

            when(tagRepository.existsByNameIgnoreCase(longName)).thenReturn(false);
            when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

            // When
            TagResponse result = tagService.createTag(request);

            // Then
            assertThat(result.getName()).hasSize(255);
        }

        @Test
        @DisplayName("Should handle single character tag name")
        void shouldHandleSingleCharacter() {
            // Given
            CreateTagRequest request = new CreateTagRequest("A");
            Tag savedTag = new Tag(1, "A");

            when(tagRepository.existsByNameIgnoreCase("A")).thenReturn(false);
            when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

            // When
            TagResponse result = tagService.createTag(request);

            // Then
            assertThat(result.getName()).isEqualTo("A");
        }
    }
}