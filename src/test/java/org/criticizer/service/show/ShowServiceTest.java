package org.criticizer.service.show;

import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.dto.show.ShowResponse;
import org.criticizer.entity.Genre;
import org.criticizer.entity.Show;
import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.repository.GenreRepository;
import org.criticizer.repository.ShowRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ShowService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ShowService Tests")
class ShowServiceTest {

    @Mock
    private ShowRepository showRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private ServiceValidator validator;

    @InjectMocks
    private ShowService showService;

    private Show testShow;
    private Genre testGenre;
    private final int TEST_USER_ID = 1;

    @BeforeEach
    void setUp() {
        testShow = new Show(1, "Breaking Bad", TEST_USER_ID, 98, false);
        testShow.setCoverUrl("http://example.com/bb.jpg");

        testGenre = new Genre(1, "Crime");
        testShow.setGenres(new HashSet<>(Arrays.asList(testGenre)));
    }

    // ==================== ADD ITEM TESTS ====================

    @Nested
    @DisplayName("addItem() Tests")
    class AddItemTests {

        @Test
        @DisplayName("Should successfully add show with genres")
        void shouldAddShowWithGenres() {
            // Given
            String name = "The Wire";
            List<Integer> genreIds = Arrays.asList(1, 2);
            Genre genre1 = new Genre(1, "Crime");
            Genre genre2 = new Genre(2, "Drama");

            when(validator.validateName(name, "Show name")).thenReturn(name);
            doNothing().when(validator).validateScore(95, TEST_USER_ID, "Show");
            when(showRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(false);
            when(showRepository.save(any(Show.class)))
                    .thenAnswer(invocation -> {
                        Show show = invocation.getArgument(0);
                        if (show.getId() == null) {
                            Show savedShow = new Show(100, show.getName(),
                                    show.getUserId(), show.getScore(), show.isCompleted());
                            savedShow.setCoverUrl(show.getCoverUrl());
                            savedShow.setGenres(show.getGenres());
                            return savedShow;
                        }
                        return show;
                    });
            when(genreRepository.findAllById(genreIds))
                    .thenReturn(Arrays.asList(genre1, genre2));

            // When
            showService.addItem(name, "url", TEST_USER_ID, 95, genreIds);

            // Then
            verify(validator).validateName(name, "Show name");
            verify(validator).validateScore(95, TEST_USER_ID, "Show");
            verify(showRepository, times(2)).save(any(Show.class));
            verify(genreRepository).findAllById(genreIds);
        }

        @Test
        @DisplayName("Should add show without genres")
        void shouldAddShowWithoutGenres() {
            // Given
            String name = "The Office";
            when(validator.validateName(name, "Show name")).thenReturn(name);
            when(showRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(false);
            when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArgument(0));

            // When
            showService.addItem(name, null, TEST_USER_ID, 88, null);

            // Then
            verify(showRepository).save(argThat(show ->
                    show.getName().equals(name) &&
                            show.getCoverUrl() == null
            ));
            verify(genreRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("Should throw when show already exists")
        void shouldThrowWhenShowExists() {
            // Given
            String name = "Breaking Bad";
            when(validator.validateName(name, "Show name")).thenReturn(name);
            when(showRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() ->
                    showService.addItem(name, null, TEST_USER_ID, 90, null))
                    .isInstanceOf(ItemAlreadyExistsException.class)
                    .hasMessageContaining(name);

            verify(showRepository, never()).save(any());
        }
    }

    // ==================== UPDATE ITEM TESTS ====================

    @Nested
    @DisplayName("updateItem() Tests")
    class UpdateItemTests {

        @Test
        @DisplayName("Should successfully update show")
        void shouldUpdateShow() {
            // Given
            String oldName = "Breaking Bad";
            String newName = "Breaking Bad: The Complete Series";
            String newCoverUrl = "newurl";
            int newScore = 99;

            when(validator.validateName(oldName, "Old Show name")).thenReturn(oldName);
            when(validator.validateName(newName, "New Show name")).thenReturn(newName);
            doNothing().when(validator).validateScore(newScore, TEST_USER_ID, "Show");

            when(showRepository.findByNameIgnoreCaseAndUserId(oldName, TEST_USER_ID))
                    .thenReturn(Optional.of(testShow));
            when(showRepository.existsByNameIgnoreCaseAndUserId(newName, TEST_USER_ID))
                    .thenReturn(false);
            when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArgument(0));

            // When
            showService.updateItem(oldName, newName, newCoverUrl, newScore, TEST_USER_ID, null);

            // Then
            verify(showRepository).save(argThat(show ->
                    show.getName().equals(newName) &&
                            show.getScore() == newScore &&
                            show.getCoverUrl().equals(newCoverUrl)
            ));
        }

        @Test
        @DisplayName("Should throw when show not found")
        void shouldThrowWhenShowNotFound() {
            // Given
            when(validator.validateName(anyString(), anyString())).thenReturn("name");
            when(showRepository.findByNameIgnoreCaseAndUserId(anyString(), eq(TEST_USER_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    showService.updateItem("old", "new", null, 80, TEST_USER_ID, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should update genres when provided")
        void shouldUpdateGenres() {
            // Given
            List<Integer> newGenreIds = Arrays.asList(2, 3);
            Genre genre2 = new Genre(2, "Thriller");
            Genre genre3 = new Genre(3, "Drama");

            when(validator.validateName(anyString(), anyString())).thenReturn("name");
            when(showRepository.findByNameIgnoreCaseAndUserId(anyString(), eq(TEST_USER_ID)))
                    .thenReturn(Optional.of(testShow));
            when(genreRepository.findAllById(newGenreIds))
                    .thenReturn(Arrays.asList(genre2, genre3));
            when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArgument(0));

            // When
            showService.updateItem("old", "new", null, 90, TEST_USER_ID, newGenreIds);

            // Then
            verify(genreRepository).findAllById(newGenreIds);
            verify(showRepository).save(argThat(show ->
                    show.getGenres().size() == 2 &&
                            show.getGenres().contains(genre2) &&
                            show.getGenres().contains(genre3)
            ));
        }
    }

    // ==================== REMOVE ITEM TESTS ====================

    @Nested
    @DisplayName("removeItem() Tests")
    class RemoveItemTests {

        @Test
        @DisplayName("Should successfully remove show")
        void shouldRemoveShow() {
            // Given
            String name = "Breaking Bad";
            when(validator.validateName(name, "Show name")).thenReturn(name);
            when(showRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testShow));
            doNothing().when(showRepository).delete(testShow);

            // When
            showService.removeItem(name, TEST_USER_ID);

            // Then
            verify(showRepository).delete(testShow);
        }

        @Test
        @DisplayName("Should throw when show not found")
        void shouldThrowWhenShowNotFound() {
            // Given
            String name = "Nonexistent Show";
            when(validator.validateName(name, "Show name")).thenReturn(name);
            when(showRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> showService.removeItem(name, TEST_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(showRepository, never()).delete(any());
        }
    }

    // ==================== TOGGLE STATUS TESTS ====================

    @Nested
    @DisplayName("toggleStatus() Tests")
    class ToggleStatusTests {

        @Test
        @DisplayName("Should toggle from unwatched to watched")
        void shouldToggleToWatched() {
            // Given
            testShow.setCompleted(false);
            when(validator.validateName("Breaking Bad", "Show name"))
                    .thenReturn("Breaking Bad");
            when(showRepository.findByNameIgnoreCaseAndUserId("Breaking Bad", TEST_USER_ID))
                    .thenReturn(Optional.of(testShow));
            when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArgument(0));

            // When
            ShowResponse result = showService.toggleStatus("Breaking Bad", TEST_USER_ID);

            // Then
            assertThat(result.isCompleted()).isTrue();
            verify(showRepository).save(argThat(Show::isCompleted));
        }

        @Test
        @DisplayName("Should toggle from watched to unwatched")
        void shouldToggleToUnwatched() {
            // Given
            testShow.setCompleted(true);
            when(validator.validateName("Breaking Bad", "Show name"))
                    .thenReturn("Breaking Bad");
            when(showRepository.findByNameIgnoreCaseAndUserId("Breaking Bad", TEST_USER_ID))
                    .thenReturn(Optional.of(testShow));
            when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArgument(0));

            // When
            ShowResponse result = showService.toggleStatus("Breaking Bad", TEST_USER_ID);

            // Then
            assertThat(result.isCompleted()).isFalse();
            verify(showRepository).save(argThat(show -> !show.isCompleted()));
        }
    }

    // ==================== GET USER ITEMS PAGE TESTS ====================

    @Nested
    @DisplayName("getUserItemsPage() Tests")
    class GetUserItemsPageTests {

        @Test
        @DisplayName("Should return paginated shows")
        void shouldReturnPaginatedShows() {
            // Given
            ServiceValidator.PaginationParams params =
                    new ServiceValidator.PaginationParams(1, 10, 0);

            when(validator.validatePagination(1, 10)).thenReturn(params);
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            Page<Integer> showIds = new PageImpl<>(
                    Arrays.asList(1, 2),
                    PageRequest.of(0, 10),
                    2
            );
            when(showRepository.findItemIds(eq(TEST_USER_ID), isNull(), isNull(), any()))
                    .thenReturn(showIds);

            Show show1 = new Show(1, "Show 1", TEST_USER_ID, 80, false);
            Show show2 = new Show(2, "Show 2", TEST_USER_ID, 85, false);
            when(showRepository.findByIdsWithCategories(Arrays.asList(1, 2)))
                    .thenReturn(Arrays.asList(show1, show2));

            // When
            var result = showService.getUserItemsPage(
                    TEST_USER_ID, 1, 10, null, null, "name", "asc"
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getCurrentPage()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should filter by genre")
        void shouldFilterByGenre() {
            // Given
            ServiceValidator.PaginationParams params =
                    new ServiceValidator.PaginationParams(1, 10, 0);

            when(validator.validatePagination(1, 10)).thenReturn(params);
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            Page<Integer> showIds = new PageImpl<>(
                    List.of(1),
                    PageRequest.of(0, 10),
                    1
            );
            when(showRepository.findItemIds(
                    eq(TEST_USER_ID), eq(1), isNull(), any()))
                    .thenReturn(showIds);

            when(showRepository.findByIdsWithCategories(List.of(1)))
                    .thenReturn(List.of(testShow));

            // When
            var result = showService.getUserItemsPage(
                    TEST_USER_ID, 1, 10, 1, null, "name", "asc"
            );

            // Then
            assertThat(result.getItems()).hasSize(1);
            verify(showRepository).findItemIds(eq(TEST_USER_ID), eq(1), isNull(), any());
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
            String name = "Breaking Bad";
            String newCover = "http://example.com/new-cover.jpg";

            when(validator.validateName(name, "Show name")).thenReturn(name);
            when(showRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testShow));
            when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArgument(0));

            // When
            showService.updateCover(name, newCover, TEST_USER_ID);

            // Then
            verify(showRepository).save(argThat(show ->
                    show.getCoverUrl().equals(newCover)
            ));
        }

        @Test
        @DisplayName("Should remove cover when null provided")
        void shouldRemoveCover() {
            // Given
            String name = "Breaking Bad";

            when(validator.validateName(name, "Show name")).thenReturn(name);
            when(showRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testShow));
            when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArgument(0));

            // When
            showService.updateCover(name, null, TEST_USER_ID);

            // Then
            verify(showRepository).save(argThat(show ->
                    show.getCoverUrl() == null
            ));
        }
    }

    // ==================== RESPONSE MAPPING TESTS ====================

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should map show with genres correctly")
        void shouldMapShowWithGenres() {
            // Given
            Genre genre2 = new Genre(2, "Drama");
            Genre genre3 = new Genre(3, "Thriller");
            testShow.setGenres(new HashSet<>(Arrays.asList(testGenre, genre2, genre3)));

            when(validator.validateName("Breaking Bad", "Show name"))
                    .thenReturn("Breaking Bad");
            when(showRepository.findByNameIgnoreCaseAndUserId("Breaking Bad", TEST_USER_ID))
                    .thenReturn(Optional.of(testShow));
            when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArgument(0));

            // When
            ShowResponse result = showService.toggleStatus("Breaking Bad", TEST_USER_ID);

            // Then
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getName()).isEqualTo("Breaking Bad");
            assertThat(result.getCoverUrl()).isEqualTo("http://example.com/bb.jpg");
            assertThat(result.getScore()).isEqualTo(98);
            assertThat(result.getGenres()).hasSize(3);
            assertThat(result.getGenres())
                    .extracting(GenreResponse::getName)
                    .containsExactlyInAnyOrder("Crime", "Drama", "Thriller");
        }

        @Test
        @DisplayName("Should map show without genres")
        void shouldMapShowWithoutGenres() {
            // Given
            testShow.setGenres(new HashSet<>());

            when(validator.validateName("Breaking Bad", "Show name"))
                    .thenReturn("Breaking Bad");
            when(showRepository.findByNameIgnoreCaseAndUserId("Breaking Bad", TEST_USER_ID))
                    .thenReturn(Optional.of(testShow));
            when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArgument(0));

            // When
            ShowResponse result = showService.toggleStatus("Breaking Bad", TEST_USER_ID);

            // Then
            assertThat(result.getGenres()).isEmpty();
        }

        @Test
        @DisplayName("Should include all show properties in response")
        void shouldIncludeAllProperties() {
            // Given
            testShow.setCompleted(true);
            String name = "Breaking Bad";

            when(validator.validateName(name, "Show name")).thenReturn(name);
            when(showRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testShow));
            when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArgument(0));

            // When
            ShowResponse result = showService.toggleStatus(name, TEST_USER_ID);

            // Then
            assertThat(result.getId()).isEqualTo(testShow.getId());
            assertThat(result.getName()).isEqualTo(testShow.getName());
            assertThat(result.getCoverUrl()).isEqualTo(testShow.getCoverUrl());
            assertThat(result.getScore()).isEqualTo(testShow.getScore());
            assertThat(result.isCompleted()).isFalse();
        }
    }

    // ==================== UTILITY METHODS TESTS ====================

    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {

        @Test
        @DisplayName("Should return correct media type")
        void shouldReturnCorrectMediaType() {
            assertThat(showService.getMediaType()).isEqualTo("shows");
        }

        @Test
        @DisplayName("Should check if show exists")
        void shouldCheckIfShowExists() {
            when(validator.validateName("Breaking Bad", "Show name"))
                    .thenReturn("Breaking Bad");
            when(showRepository.existsByNameIgnoreCaseAndUserId("Breaking Bad", TEST_USER_ID))
                    .thenReturn(true);

            boolean result = showService.isItemExists("Breaking Bad", TEST_USER_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should get show completion status")
        void shouldGetShowStatus() {
            // Given
            testShow.setCompleted(true);

            when(validator.validateName("Breaking Bad", "Show name"))
                    .thenReturn("Breaking Bad");
            when(showRepository.findByNameIgnoreCaseAndUserId("Breaking Bad", TEST_USER_ID))
                    .thenReturn(Optional.of(testShow));

            // When
            boolean result = showService.getItemStatus("Breaking Bad", TEST_USER_ID);

            // Then
            assertThat(result).isTrue();
        }
    }
}