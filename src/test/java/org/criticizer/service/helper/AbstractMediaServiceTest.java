package org.criticizer.service.helper;

import org.criticizer.dto.helper.PageResponse;
import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.exceptions.validation.EmptyNameException;
import org.criticizer.exceptions.validation.InvalidScoreException;
import org.criticizer.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AbstractMediaService.
 * Tests the core business logic that all media services inherit.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractMediaService Tests")
class AbstractMediaServiceTest {

    @Mock
    private MediaRepository<TestMediaEntity> repository;

    @Mock
    private ServiceValidator validator;

    @Captor
    private ArgumentCaptor<TestMediaEntity> entityCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private TestMediaService service;

    private static final int USER_ID = 1;
    private static final String ENTITY_NAME = "TestItem";
    private static final String COVER_URL = "https://example.com/cover.jpg";
    private static final int SCORE = 85;

    @BeforeEach
    void setUp() {
        service = new TestMediaService(repository, validator);
    }

    // ==================== GET USER ITEMS PAGE TESTS ====================

    @Nested
    @DisplayName("getUserItemsPage() Tests")
    class GetUserItemsPageTests {

        @Test
        @DisplayName("Should return paginated items with two-step query")
        void shouldReturnPaginatedItems() {
            // Given
            when(validator.validatePagination(1, 10))
                    .thenReturn(new ServiceValidator.PaginationParams(1, 10, 0));
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            List<Integer> itemIds = List.of(1, 2, 3);
            Page<Integer> idsPage = new PageImpl<>(itemIds, PageRequest.of(0, 10), 3);
            when(repository.findItemIds(eq(USER_ID), isNull(), isNull(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(idsPage);

            List<TestMediaEntity> entities = List.of(
                    createEntity(1, "Item1", 90),
                    createEntity(2, "Item2", 85),
                    createEntity(3, "Item3", 80)
            );
            when(repository.findByIdsWithCategories(itemIds)).thenReturn(entities);

            // When
            PageResponse<TestMediaEntity> result = service.getUserItemsPage(
                    USER_ID, 1, 10, null, null, "name", "asc"
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(3);
            assertThat(result.getCurrentPage()).isEqualTo(1);
            assertThat(result.getTotalItems()).isEqualTo(3);

            verify(repository).findItemIds(eq(USER_ID), isNull(), isNull(), any(), any(), any(), any(Pageable.class));
            verify(repository).findByIdsWithCategories(itemIds);
        }

        @Test
        @DisplayName("Should filter by category")
        void shouldFilterByCategory() {
            // Given
            when(validator.validatePagination(1, 10))
                    .thenReturn(new ServiceValidator.PaginationParams(1, 10, 0));
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            Page<Integer> idsPage = new PageImpl<>(List.of(1), PageRequest.of(0, 10), 1);
            when(repository.findItemIds(eq(USER_ID), eq(5), isNull(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(idsPage);

            List<TestMediaEntity> entities = List.of(createEntity(1, "FilteredItem", 90));
            when(repository.findByIdsWithCategories(List.of(1))).thenReturn(entities);

            // When
            service.getUserItemsPage(USER_ID, 1, 10, 5, null, "name", "asc");

            // Then
            verify(repository).findItemIds(eq(USER_ID), eq(5), isNull(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should search by term")
        void shouldSearchByTerm() {
            // Given
            when(validator.validatePagination(1, 10))
                    .thenReturn(new ServiceValidator.PaginationParams(1, 10, 0));
            when(validator.sanitizeSearchTerm("dark")).thenReturn("dark");

            Page<Integer> idsPage = new PageImpl<>(List.of(1), PageRequest.of(0, 10), 1);
            when(repository.findItemIds(eq(USER_ID), isNull(), eq("dark"), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(idsPage);

            List<TestMediaEntity> entities = List.of(createEntity(1, "Dark Souls", 95));
            when(repository.findByIdsWithCategories(List.of(1))).thenReturn(entities);

            // When
            service.getUserItemsPage(USER_ID, 1, 10, null, "dark", "name", "asc");

            // Then
            verify(repository).findItemIds(eq(USER_ID), isNull(), eq("dark"), any(), any(), any(), any(Pageable.class));
            verify(validator).sanitizeSearchTerm("dark");
        }

        @Test
        @DisplayName("Should return empty page when no items found")
        void shouldReturnEmptyPage() {
            // Given
            when(validator.validatePagination(1, 10))
                    .thenReturn(new ServiceValidator.PaginationParams(1, 10, 0));
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            Page<Integer> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(repository.findItemIds(eq(USER_ID), isNull(), isNull(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When
            PageResponse<TestMediaEntity> result = service.getUserItemsPage(
                    USER_ID, 1, 10, null, null, "name", "asc"
            );

            // Then
            assertThat(result.getItems()).isEmpty();
            assertThat(result.getTotalItems()).isZero();
            verify(repository, never()).findByIdsWithCategories(anyList());
        }

        @Test
        @DisplayName("Should sort by score descending")
        void shouldSortByScoreDesc() {
            // Given
            when(validator.validatePagination(1, 10))
                    .thenReturn(new ServiceValidator.PaginationParams(1, 10, 0));
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            List<Integer> sortedItemIds = List.of(2, 3, 1);
            Page<Integer> idsPage = new PageImpl<>(sortedItemIds, PageRequest.of(0, 10), 3);

            when(repository.findItemIds(eq(USER_ID), isNull(), isNull(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(idsPage);

            List<TestMediaEntity> entities = List.of(
                    createEntity(1, "Low", 60),
                    createEntity(2, "High", 95),
                    createEntity(3, "Medium", 75)
            );
            when(repository.findByIdsWithCategories(anyList())).thenReturn(entities);

            // When
            PageResponse<TestMediaEntity> result = service.getUserItemsPage(
                    USER_ID, 1, 10, null, null, "score", "desc"
            );

            // Then
            assertThat(result.getItems().get(0).getScore()).isEqualTo(95);
            assertThat(result.getItems().get(1).getScore()).isEqualTo(75);
            assertThat(result.getItems().get(2).getScore()).isEqualTo(60);
        }

        @Test
        @DisplayName("Should sort by name ascending (default)")
        void shouldSortByNameAsc() {
            // Given
            when(validator.validatePagination(1, 10))
                    .thenReturn(new ServiceValidator.PaginationParams(1, 10, 0));
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            List<Integer> sortedItemIds = List.of(2, 3, 1);
            Page<Integer> idsPage = new PageImpl<>(sortedItemIds, PageRequest.of(0, 10), 3);

            when(repository.findItemIds(eq(USER_ID), isNull(), isNull(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(idsPage);

            List<TestMediaEntity> entities = List.of(
                    createEntity(1, "Zebra", 80),
                    createEntity(2, "Alpha", 85),
                    createEntity(3, "Beta", 90)
            );
            when(repository.findByIdsWithCategories(anyList())).thenReturn(entities);

            // When
            PageResponse<TestMediaEntity> result = service.getUserItemsPage(
                    USER_ID, 1, 10, null, null, "name", "asc"
            );

            // Then
            assertThat(result.getItems().get(0).getName()).isEqualTo("Alpha");
            assertThat(result.getItems().get(1).getName()).isEqualTo("Beta");
            assertThat(result.getItems().get(2).getName()).isEqualTo("Zebra");
        }

        @Test
        @DisplayName("Should handle invalid sort column gracefully")
        void shouldHandleInvalidSortColumn() {
            // Given
            when(validator.validatePagination(1, 10))
                    .thenReturn(new ServiceValidator.PaginationParams(1, 10, 0));
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            Page<Integer> idsPage = new PageImpl<>(List.of(1), PageRequest.of(0, 10), 1);
            when(repository.findItemIds(eq(USER_ID), isNull(), isNull(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(idsPage);

            List<TestMediaEntity> entities = List.of(createEntity(1, "Item", 80));
            when(repository.findByIdsWithCategories(List.of(1))).thenReturn(entities);

            // When - invalid sort column should default to "name"
            PageResponse<TestMediaEntity> result = service.getUserItemsPage(
                    USER_ID, 1, 10, null, null, "invalid_column", "asc"
            );

            // Then - should not throw, defaults to name
            assertThat(result.getItems()).hasSize(1);
        }
    }

    // ==================== ADD ITEM TESTS ====================

    @Nested
    @DisplayName("addItem() Tests")
    class AddItemTests {

        @Test
        @DisplayName("Should add new item successfully")
        void shouldAddNewItem() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);
            when(repository.existsByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID)).thenReturn(false);

            TestMediaEntity savedEntity = createEntity(1, ENTITY_NAME, SCORE);
            when(repository.save(any(TestMediaEntity.class))).thenReturn(savedEntity);

            // When
            service.addItem(ENTITY_NAME, COVER_URL, USER_ID, SCORE, List.of());

            // Then
            verify(repository, times(1)).save(any(TestMediaEntity.class));
            verify(validator).validateName(ENTITY_NAME, "TestMedia name");
            verify(validator).validateScore(SCORE, USER_ID, "TestMedia");
            verify(repository).existsByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID);
        }

        @Test
        @DisplayName("Should add item with categories using two-save pattern")
        void shouldAddItemWithCategories() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);
            when(repository.existsByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID)).thenReturn(false);

            TestMediaEntity savedEntity = createEntity(1, ENTITY_NAME, SCORE);
            when(repository.save(any(TestMediaEntity.class))).thenReturn(savedEntity);

            List<Integer> categoryIds = List.of(1, 2, 3);

            // When
            service.addItem(ENTITY_NAME, COVER_URL, USER_ID, SCORE, categoryIds);

            // Then - Should save TWICE (first for ID, second for categories)
            verify(repository, times(2)).save(any(TestMediaEntity.class));
            assertThat(service.lastAssignedCategoryIds).isEqualTo(categoryIds);
        }

        @Test
        @DisplayName("Should throw when item already exists")
        void shouldThrowWhenItemExists() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);
            when(repository.existsByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() ->
                    service.addItem(ENTITY_NAME, COVER_URL, USER_ID, SCORE, List.of())
            )
                    .isInstanceOf(ItemAlreadyExistsException.class)
                    .hasMessageContaining(ENTITY_NAME);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should validate name before adding")
        void shouldValidateNameBeforeAdding() {
            // Given
            when(validator.validateName("  ", "TestMedia name"))
                    .thenThrow(new EmptyNameException("TestMedia name"));

            // When & Then
            assertThatThrownBy(() ->
                    service.addItem("  ", COVER_URL, USER_ID, SCORE, List.of())
            )
                    .isInstanceOf(EmptyNameException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should validate score before adding")
        void shouldValidateScoreBeforeAdding() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);

            doThrow(new InvalidScoreException(150))
                    .when(validator).validateScore(150, USER_ID, "TestMedia");

            // When & Then
            assertThatThrownBy(() ->
                    service.addItem(ENTITY_NAME, COVER_URL, USER_ID, 150, List.of())
            )
                    .isInstanceOf(InvalidScoreException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should trim name before checking existence")
        void shouldTrimNameBeforeChecking() {
            // Given
            String nameWithSpaces = "  Item Name  ";
            when(validator.validateName(nameWithSpaces, "TestMedia name")).thenReturn("Item Name");
            when(repository.existsByNameIgnoreCaseAndUserId("Item Name", USER_ID)).thenReturn(false);

            TestMediaEntity savedEntity = createEntity(1, "Item Name", SCORE);
            when(repository.save(any(TestMediaEntity.class))).thenReturn(savedEntity);

            // When
            service.addItem(nameWithSpaces, COVER_URL, USER_ID, SCORE, List.of());

            // Then
            verify(repository).existsByNameIgnoreCaseAndUserId("Item Name", USER_ID);
        }
    }

    // ==================== UPDATE ITEM TESTS ====================

    @Nested
    @DisplayName("updateItem() Tests")
    class UpdateItemTests {

        @Test
        @DisplayName("Should update item successfully")
        void shouldUpdateItem() {
            // Given
            String oldName = "OldName";
            String newName = "NewName";

            when(validator.validateName(oldName, "Old TestMedia name")).thenReturn(oldName);
            when(validator.validateName(newName, "New TestMedia name")).thenReturn(newName);

            TestMediaEntity existingEntity = createEntity(1, oldName, 75);
            when(repository.findByNameIgnoreCaseAndUserId(oldName, USER_ID))
                    .thenReturn(Optional.of(existingEntity));
            when(repository.existsByNameIgnoreCaseAndUserId(newName, USER_ID)).thenReturn(false);

            // When
            service.updateItem(oldName, newName, COVER_URL, SCORE, USER_ID, List.of());

            // Then
            verify(repository).save(entityCaptor.capture());
            TestMediaEntity savedEntity = entityCaptor.getValue();

            assertThat(savedEntity.getName()).isEqualTo(newName);
            assertThat(savedEntity.getScore()).isEqualTo(SCORE);
            assertThat(savedEntity.getCoverUrl()).isEqualTo(COVER_URL);
        }

        @Test
        @DisplayName("Should update item with new categories")
        void shouldUpdateItemWithCategories() {
            // Given
            String oldName = "OldName";
            String newName = "NewName";
            List<Integer> categoryIds = List.of(1, 2);

            when(validator.validateName(oldName, "Old TestMedia name")).thenReturn(oldName);
            when(validator.validateName(newName, "New TestMedia name")).thenReturn(newName);

            TestMediaEntity existingEntity = createEntity(1, oldName, 75);
            when(repository.findByNameIgnoreCaseAndUserId(oldName, USER_ID))
                    .thenReturn(Optional.of(existingEntity));
            when(repository.existsByNameIgnoreCaseAndUserId(newName, USER_ID)).thenReturn(false);

            // When
            service.updateItem(oldName, newName, COVER_URL, SCORE, USER_ID, categoryIds);

            // Then
            assertThat(service.lastAssignedCategoryIds).isEqualTo(categoryIds);
            verify(repository).save(any(TestMediaEntity.class));
        }

        @Test
        @DisplayName("Should allow same name update (case insensitive)")
        void shouldAllowSameNameUpdate() {
            // Given
            String oldName = "ItemName";
            String newName = "ITEMNAME"; // Same but different case

            when(validator.validateName(oldName, "Old TestMedia name")).thenReturn(oldName);
            when(validator.validateName(newName, "New TestMedia name")).thenReturn(newName);

            TestMediaEntity existingEntity = createEntity(1, oldName, 75);
            when(repository.findByNameIgnoreCaseAndUserId(oldName, USER_ID))
                    .thenReturn(Optional.of(existingEntity));

            // When
            service.updateItem(oldName, newName, COVER_URL, SCORE, USER_ID, List.of());

            // Then - Should not check for existence (same name)
            verify(repository, never()).existsByNameIgnoreCaseAndUserId(newName, USER_ID);
            verify(repository).save(any(TestMediaEntity.class));
        }

        @Test
        @DisplayName("Should throw when new name already exists")
        void shouldThrowWhenNewNameExists() {
            // Given
            String oldName = "OldName";
            String newName = "ExistingName";

            when(validator.validateName(oldName, "Old TestMedia name")).thenReturn(oldName);
            when(validator.validateName(newName, "New TestMedia name")).thenReturn(newName);

            TestMediaEntity existingEntity = createEntity(1, oldName, 75);
            when(repository.findByNameIgnoreCaseAndUserId(oldName, USER_ID))
                    .thenReturn(Optional.of(existingEntity));
            when(repository.existsByNameIgnoreCaseAndUserId(newName, USER_ID)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() ->
                    service.updateItem(oldName, newName, COVER_URL, SCORE, USER_ID, List.of())
            )
                    .isInstanceOf(ItemAlreadyExistsException.class)
                    .hasMessageContaining(newName);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when old name not found")
        void shouldThrowWhenOldNameNotFound() {
            // Given
            String oldName = "NonExistent";
            String newName = "NewName";

            when(validator.validateName(oldName, "Old TestMedia name")).thenReturn(oldName);
            when(validator.validateName(newName, "New TestMedia name")).thenReturn(newName);

            when(repository.findByNameIgnoreCaseAndUserId(oldName, USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    service.updateItem(oldName, newName, COVER_URL, SCORE, USER_ID, List.of())
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(oldName);

            verify(repository, never()).save(any());
        }
    }

    // ==================== REMOVE ITEM TESTS ====================

    @Nested
    @DisplayName("removeItem() Tests")
    class RemoveItemTests {

        @Test
        @DisplayName("Should remove item successfully")
        void shouldRemoveItem() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);

            TestMediaEntity entity = createEntity(1, ENTITY_NAME, SCORE);
            when(repository.findByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID))
                    .thenReturn(Optional.of(entity));

            // When
            service.removeItem(ENTITY_NAME, USER_ID);

            // Then
            verify(repository).delete(entity);
        }

        @Test
        @DisplayName("Should throw when item not found")
        void shouldThrowWhenItemNotFound() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);
            when(repository.findByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.removeItem(ENTITY_NAME, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(ENTITY_NAME);

            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("Should validate name before removing")
        void shouldValidateNameBeforeRemoving() {
            // Given
            when(validator.validateName("", "TestMedia name"))
                    .thenThrow(new EmptyNameException("TestMedia name"));

            // When & Then
            assertThatThrownBy(() -> service.removeItem("", USER_ID))
                    .isInstanceOf(EmptyNameException.class);

            verify(repository, never()).delete(any());
        }
    }

    // ==================== TOGGLE STATUS TESTS ====================

    @Nested
    @DisplayName("toggleStatus() Tests")
    class ToggleStatusTests {

        @Test
        @DisplayName("Should toggle status from false to true")
        void shouldToggleFromFalseToTrue() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);

            TestMediaEntity entity = createEntity(1, ENTITY_NAME, SCORE);
            entity.setCompleted(false);

            when(repository.findByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(repository.save(any(TestMediaEntity.class))).thenReturn(entity);

            // When
            service.toggleStatus(ENTITY_NAME, USER_ID);

            // Then
            verify(repository).save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().isCompleted()).isTrue();
        }

        @Test
        @DisplayName("Should toggle status from true to false")
        void shouldToggleFromTrueToFalse() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);

            TestMediaEntity entity = createEntity(1, ENTITY_NAME, SCORE);
            entity.setCompleted(true);

            when(repository.findByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID))
                    .thenReturn(Optional.of(entity));
            when(repository.save(any(TestMediaEntity.class))).thenReturn(entity);

            // When
            service.toggleStatus(ENTITY_NAME, USER_ID);

            // Then
            verify(repository).save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().isCompleted()).isFalse();
        }

        @Test
        @DisplayName("Should throw when item not found for toggle")
        void shouldThrowWhenNotFoundForToggle() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);
            when(repository.findByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.toggleStatus(ENTITY_NAME, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(repository, never()).save(any());
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
            String newCoverUrl = "https://example.com/new-cover.jpg";
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);

            TestMediaEntity entity = createEntity(1, ENTITY_NAME, SCORE);
            when(repository.findByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID))
                    .thenReturn(Optional.of(entity));

            // When
            service.updateCover(ENTITY_NAME, newCoverUrl, USER_ID);

            // Then
            verify(repository).save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().getCoverUrl()).isEqualTo(newCoverUrl);
        }

        @Test
        @DisplayName("Should remove cover when URL is null")
        void shouldRemoveCoverWhenNull() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);

            TestMediaEntity entity = createEntity(1, ENTITY_NAME, SCORE);
            entity.setCoverUrl("old-cover.jpg");
            when(repository.findByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID))
                    .thenReturn(Optional.of(entity));

            // When
            service.updateCover(ENTITY_NAME, null, USER_ID);

            // Then
            verify(repository).save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().getCoverUrl()).isNull();
        }

        @Test
        @DisplayName("Should remove cover when URL is empty")
        void shouldRemoveCoverWhenEmpty() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);

            TestMediaEntity entity = createEntity(1, ENTITY_NAME, SCORE);
            entity.setCoverUrl("old-cover.jpg");
            when(repository.findByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID))
                    .thenReturn(Optional.of(entity));

            // When
            service.updateCover(ENTITY_NAME, "  ", USER_ID);

            // Then
            verify(repository).save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().getCoverUrl()).isNull();
        }

        @Test
        @DisplayName("Should throw when item not found for cover update")
        void shouldThrowWhenNotFoundForCoverUpdate() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);
            when(repository.findByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    service.updateCover(ENTITY_NAME, COVER_URL, USER_ID)
            )
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }

    // ==================== HELPER METHOD TESTS ====================

    @Nested
    @DisplayName("isItemExists() Tests")
    class IsItemExistsTests {

        @Test
        @DisplayName("Should return true when item exists")
        void shouldReturnTrueWhenExists() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);
            when(repository.existsByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID)).thenReturn(true);

            // When
            boolean result = service.isItemExists(ENTITY_NAME, USER_ID);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when item does not exist")
        void shouldReturnFalseWhenNotExists() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);
            when(repository.existsByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID)).thenReturn(false);

            // When
            boolean result = service.isItemExists(ENTITY_NAME, USER_ID);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getItemStatus() Tests")
    class GetItemStatusTests {

        @Test
        @DisplayName("Should return true when item is completed")
        void shouldReturnTrueWhenCompleted() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);

            TestMediaEntity entity = createEntity(1, ENTITY_NAME, SCORE);
            entity.setCompleted(true);
            when(repository.findByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID))
                    .thenReturn(Optional.of(entity));

            // When
            boolean result = service.getItemStatus(ENTITY_NAME, USER_ID);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when item is not completed")
        void shouldReturnFalseWhenNotCompleted() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);

            TestMediaEntity entity = createEntity(1, ENTITY_NAME, SCORE);
            entity.setCompleted(false);
            when(repository.findByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID))
                    .thenReturn(Optional.of(entity));

            // When
            boolean result = service.getItemStatus(ENTITY_NAME, USER_ID);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should throw when item not found for status check")
        void shouldThrowWhenNotFoundForStatus() {
            // Given
            when(validator.validateName(ENTITY_NAME, "TestMedia name")).thenReturn(ENTITY_NAME);
            when(repository.findByNameIgnoreCaseAndUserId(ENTITY_NAME, USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.getItemStatus(ENTITY_NAME, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== TEST IMPLEMENTATION CLASSES ====================

    /**
     * Test entity implementing MediaEntity interface
     */
    static class TestMediaEntity implements MediaEntity {
        private Integer id;
        private String name;
        private Integer userId;
        private Integer score;
        private boolean completed;
        private String coverUrl;

        @Override
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        @Override
        public Integer getScore() {
            return score;
        }

        @Override
        public void setScore(Integer score) {
            this.score = score;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }

        @Override
        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        @Override
        public String getCoverUrl() {
            return coverUrl;
        }

        @Override
        public void setCoverUrl(String coverUrl) {
            this.coverUrl = coverUrl;
        }
    }

    /**
     * Test service implementation
     */
    static class TestMediaService extends AbstractMediaService<TestMediaEntity, String> {
        List<Integer> lastAssignedCategoryIds;

        TestMediaService(MediaRepository<TestMediaEntity> repository, ServiceValidator validator) {
            super(repository, validator);
        }

        @Override
        protected String getEntityName() {
            return "TestMedia";
        }

        @Override
        protected TestMediaEntity createEntity(String name, String coverUrl, Integer userId, Integer score) {
            TestMediaEntity entity = new TestMediaEntity();
            entity.setName(name);
            entity.setCoverUrl(coverUrl);
            entity.setUserId(userId);
            entity.setScore(score);
            entity.setCompleted(false);
            return entity;
        }

        @Override
        protected void assignCategories(TestMediaEntity entity, List<Integer> categoryIds) {
            this.lastAssignedCategoryIds = categoryIds;
        }

        @Override
        protected String toResponse(TestMediaEntity entity) {
            return entity.getName();
        }

        @Override
        public String getMediaType() {
            return "test";
        }
    }

    /**
     * Helper method to create test entities
     */
    private TestMediaEntity createEntity(int id, String name, int score) {
        TestMediaEntity entity = new TestMediaEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setScore(score);
        entity.setUserId(USER_ID);
        entity.setCompleted(false);
        entity.setCoverUrl(COVER_URL);
        return entity;
    }
}