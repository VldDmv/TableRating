package org.criticizer.service.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.criticizer.dto.book.BookResponse;
import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.entity.Book;
import org.criticizer.entity.Genre;
import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.repository.BookRepository;
import org.criticizer.repository.GenreRepository;
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

/** Unit tests for BookService. */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Tests")
class BookServiceTest {

    @Mock private BookRepository bookRepository;

    @Mock private GenreRepository genreRepository;

    @Mock private ServiceValidator validator;

    @InjectMocks private BookService bookService;

    private Book testBook;
    private Genre testGenre;
    private final int TEST_USER_ID = 1;

    @BeforeEach
    void setUp() {
        testBook = new Book(1, "1984", TEST_USER_ID, 95, false);
        testBook.setCoverUrl("http://example.com/1984.jpg");

        testGenre = new Genre(1, "Dystopian");
        testBook.setGenres(new HashSet<>(Arrays.asList(testGenre)));
    }

    // ==================== ADD ITEM TESTS ====================

    @Nested
    @DisplayName("addItem() Tests")
    class AddItemTests {

        @Test
        @DisplayName("Should successfully add book with genres")
        void shouldAddBookWithGenres() {
            // Given
            String name = "Brave New World";
            List<Integer> genreIds = Arrays.asList(1, 2);
            Genre genre1 = new Genre(1, "Dystopian");
            Genre genre2 = new Genre(2, "Sci-Fi");

            when(validator.validateName(name, "Book name")).thenReturn(name);
            doNothing().when(validator).validateScore(90, TEST_USER_ID, "Book");
            when(bookRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(false);
            when(bookRepository.save(any(Book.class)))
                    .thenAnswer(
                            invocation -> {
                                Book book = invocation.getArgument(0);
                                if (book.getId() == null) {
                                    Book savedBook =
                                            new Book(
                                                    100,
                                                    book.getName(),
                                                    book.getUserId(),
                                                    book.getScore(),
                                                    book.isCompleted());
                                    savedBook.setCoverUrl(book.getCoverUrl());
                                    savedBook.setGenres(book.getGenres());
                                    return savedBook;
                                }
                                return book;
                            });
            when(genreRepository.findAllById(genreIds)).thenReturn(Arrays.asList(genre1, genre2));

            // When
            bookService.addItem(name, "url", TEST_USER_ID, 90, genreIds);

            // Then
            verify(validator).validateName(name, "Book name");
            verify(validator).validateScore(90, TEST_USER_ID, "Book");
            verify(bookRepository, times(2)).save(any(Book.class));
            verify(genreRepository).findAllById(genreIds);
        }

        @Test
        @DisplayName("Should add book without genres")
        void shouldAddBookWithoutGenres() {
            // Given
            String name = "Fahrenheit 451";
            when(validator.validateName(name, "Book name")).thenReturn(name);
            when(bookRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(false);
            when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

            // When
            bookService.addItem(name, null, TEST_USER_ID, 88, null);

            // Then
            verify(bookRepository)
                    .save(
                            argThat(
                                    book ->
                                            book.getName().equals(name)
                                                    && book.getCoverUrl() == null));
            verify(genreRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("Should throw when book already exists")
        void shouldThrowWhenBookExists() {
            // Given
            String name = "1984";
            when(validator.validateName(name, "Book name")).thenReturn(name);
            when(bookRepository.existsByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> bookService.addItem(name, null, TEST_USER_ID, 90, null))
                    .isInstanceOf(ItemAlreadyExistsException.class);
            verify(bookRepository, never()).save(any());
        }
    }

    // ==================== UPDATE ITEM TESTS ====================

    @Nested
    @DisplayName("updateItem() Tests")
    class UpdateItemTests {

        @Test
        @DisplayName("Should successfully update book")
        void shouldUpdateBook() {
            // Given
            String oldName = "1984";
            String newName = "Nineteen Eighty-Four";
            String newCoverUrl = "http://example.com/new.jpg";
            int newScore = 96;
            List<Integer> newGenreIds = Arrays.asList(2);

            Genre newGenre = new Genre(2, "Classic");

            when(validator.validateName(oldName, "Old Book name")).thenReturn(oldName);
            when(validator.validateName(newName, "New Book name")).thenReturn(newName);
            doNothing().when(validator).validateScore(newScore, TEST_USER_ID, "Book");

            when(bookRepository.findByNameIgnoreCaseAndUserId(oldName, TEST_USER_ID))
                    .thenReturn(Optional.of(testBook));
            when(bookRepository.existsByNameIgnoreCaseAndUserId(newName, TEST_USER_ID))
                    .thenReturn(false);
            when(genreRepository.findAllById(newGenreIds)).thenReturn(List.of(newGenre));
            when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

            // When
            bookService.updateItem(
                    oldName, newName, newCoverUrl, newScore, TEST_USER_ID, newGenreIds);

            // Then
            verify(bookRepository)
                    .save(
                            argThat(
                                    book ->
                                            book.getName().equals(newName)
                                                    && book.getCoverUrl().equals(newCoverUrl)
                                                    && book.getScore() == newScore
                                                    && book.getGenres().contains(newGenre)));
        }

        @Test
        @DisplayName("Should throw when book not found")
        void shouldThrowWhenBookNotFound() {
            // Given
            when(validator.validateName(anyString(), anyString())).thenReturn("name");
            when(bookRepository.findByNameIgnoreCaseAndUserId(anyString(), eq(TEST_USER_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(
                            () ->
                                    bookService.updateItem(
                                            "old", "new", null, 80, TEST_USER_ID, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== REMOVE ITEM TESTS ====================

    @Nested
    @DisplayName("removeItem() Tests")
    class RemoveItemTests {

        @Test
        @DisplayName("Should successfully remove book")
        void shouldRemoveBook() {
            // Given
            String name = "1984";
            when(validator.validateName(name, "Book name")).thenReturn(name);
            when(bookRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testBook));
            doNothing().when(bookRepository).delete(testBook);

            // When
            bookService.removeItem(name, TEST_USER_ID);

            // Then
            verify(bookRepository).delete(testBook);
        }

        @Test
        @DisplayName("Should throw when book not found")
        void shouldThrowWhenBookNotFound() {
            // Given
            String name = "Nonexistent Book";
            when(validator.validateName(name, "Book name")).thenReturn(name);
            when(bookRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> bookService.removeItem(name, TEST_USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(bookRepository, never()).delete(any());
        }
    }

    // ==================== TOGGLE STATUS TESTS ====================

    @Nested
    @DisplayName("toggleStatus() Tests")
    class ToggleStatusTests {

        @Test
        @DisplayName("Should toggle from unread to read")
        void shouldToggleToCompleted() {
            // Given
            testBook.setCompleted(false);
            when(validator.validateName("1984", "Book name")).thenReturn("1984");
            when(bookRepository.findByNameIgnoreCaseAndUserId("1984", TEST_USER_ID))
                    .thenReturn(Optional.of(testBook));
            when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

            // When
            BookResponse result = bookService.toggleStatus("1984", TEST_USER_ID);

            // Then
            assertThat(result.isCompleted()).isTrue();
            verify(bookRepository).save(argThat(Book::isCompleted));
        }

        @Test
        @DisplayName("Should toggle from read to unread")
        void shouldToggleToIncomplete() {
            // Given
            testBook.setCompleted(true);
            when(validator.validateName("1984", "Book name")).thenReturn("1984");
            when(bookRepository.findByNameIgnoreCaseAndUserId("1984", TEST_USER_ID))
                    .thenReturn(Optional.of(testBook));
            when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

            // When
            BookResponse result = bookService.toggleStatus("1984", TEST_USER_ID);

            // Then
            assertThat(result.isCompleted()).isFalse();
            verify(bookRepository).save(argThat(book -> !book.isCompleted()));
        }
    }

    // ==================== GET USER ITEMS PAGE TESTS ====================

    @Nested
    @DisplayName("getUserItemsPage() Tests")
    class GetUserItemsPageTests {

        @Test
        @DisplayName("Should return paginated books")
        void shouldReturnPaginatedBooks() {
            // Given
            ServiceValidator.PaginationParams params =
                    new ServiceValidator.PaginationParams(1, 10, 0);

            when(validator.validatePagination(1, 10)).thenReturn(params);
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            Page<Integer> bookIds = new PageImpl<>(Arrays.asList(1, 2), PageRequest.of(0, 10), 2);
            when(bookRepository.findItemIds(
                            eq(TEST_USER_ID), isNull(), isNull(), any(), any(), any()))
                    .thenReturn(bookIds);

            Book book1 = new Book(1, "Book 1", TEST_USER_ID, 80, false);
            Book book2 = new Book(2, "Book 2", TEST_USER_ID, 85, false);
            when(bookRepository.findByIdsWithCategories(Arrays.asList(1, 2)))
                    .thenReturn(Arrays.asList(book1, book2));

            // When
            var result =
                    bookService.getUserItemsPage(TEST_USER_ID, 1, 10, null, null, "name", "asc");

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

            Page<Integer> bookIds = new PageImpl<>(List.of(1), PageRequest.of(0, 10), 1);
            when(bookRepository.findItemIds(eq(TEST_USER_ID), eq(1), isNull(), any(), any(), any()))
                    .thenReturn(bookIds);

            when(bookRepository.findByIdsWithCategories(List.of(1))).thenReturn(List.of(testBook));

            // When
            var result = bookService.getUserItemsPage(TEST_USER_ID, 1, 10, 1, null, "name", "asc");

            // Then
            assertThat(result.getItems()).hasSize(1);
            verify(bookRepository)
                    .findItemIds(eq(TEST_USER_ID), eq(1), isNull(), any(), any(), any());
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
            String name = "1984";
            String newCoverUrl = "http://example.com/new-cover.jpg";

            when(validator.validateName(name, "Book name")).thenReturn(name);
            when(bookRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testBook));
            when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

            // When
            bookService.updateCover(name, newCoverUrl, TEST_USER_ID);

            // Then
            verify(bookRepository).save(argThat(book -> book.getCoverUrl().equals(newCoverUrl)));
        }

        @Test
        @DisplayName("Should remove cover when URL is null")
        void shouldRemoveCoverWhenNull() {
            // Given
            String name = "1984";

            when(validator.validateName(name, "Book name")).thenReturn(name);
            when(bookRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testBook));
            when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

            // When
            bookService.updateCover(name, null, TEST_USER_ID);

            // Then
            verify(bookRepository).save(argThat(book -> book.getCoverUrl() == null));
        }
    }

    // ==================== RESPONSE MAPPING TESTS ====================

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should map book with genres correctly")
        void shouldMapBookWithGenres() {
            // Given
            Genre genre2 = new Genre(2, "Fiction");
            testBook.setGenres(new HashSet<>(Arrays.asList(testGenre, genre2)));

            when(validator.validateName("1984", "Book name")).thenReturn("1984");
            when(bookRepository.findByNameIgnoreCaseAndUserId("1984", TEST_USER_ID))
                    .thenReturn(Optional.of(testBook));
            when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

            // When
            BookResponse result = bookService.toggleStatus("1984", TEST_USER_ID);

            // Then
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getName()).isEqualTo("1984");
            assertThat(result.getGenres()).hasSize(2);
            assertThat(result.getGenres())
                    .extracting(GenreResponse::getName)
                    .containsExactlyInAnyOrder("Dystopian", "Fiction");
        }

        @Test
        @DisplayName("Should map book without genres to response")
        void shouldMapBookWithoutGenres() {
            // Given
            testBook.setGenres(new HashSet<>());
            String name = "1984";

            when(validator.validateName(name, "Book name")).thenReturn(name);
            when(bookRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testBook));
            when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

            // When
            BookResponse result = bookService.toggleStatus(name, TEST_USER_ID);

            // Then
            assertThat(result.getGenres()).isEmpty();
        }

        @Test
        @DisplayName("Should include all book properties in response")
        void shouldIncludeAllProperties() {
            // Given
            testBook.setCompleted(true);
            String name = "1984";

            when(validator.validateName(name, "Book name")).thenReturn(name);
            when(bookRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testBook));
            when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

            // When
            BookResponse result = bookService.toggleStatus(name, TEST_USER_ID);

            // Then
            assertThat(result.getId()).isEqualTo(testBook.getId());
            assertThat(result.getName()).isEqualTo(testBook.getName());
            assertThat(result.getCoverUrl()).isEqualTo(testBook.getCoverUrl());
            assertThat(result.getScore()).isEqualTo(testBook.getScore());
            assertThat(result.isCompleted()).isFalse();
        }
    }

    // ==================== UTILITY METHODS TESTS ====================

    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {

        @Test
        @DisplayName("Should check if book exists")
        void shouldCheckIfBookExists() {
            when(validator.validateName("1984", "Book name")).thenReturn("1984");
            when(bookRepository.existsByNameIgnoreCaseAndUserId("1984", TEST_USER_ID))
                    .thenReturn(true);

            boolean result = bookService.isItemExists("1984", TEST_USER_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should get book status")
        void shouldGetBookStatus() {
            // Given
            String name = "1984";
            testBook.setCompleted(true);

            when(validator.validateName(name, "Book name")).thenReturn(name);
            when(bookRepository.findByNameIgnoreCaseAndUserId(name, TEST_USER_ID))
                    .thenReturn(Optional.of(testBook));

            // When
            boolean result = bookService.getItemStatus(name, TEST_USER_ID);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return correct media type")
        void shouldReturnCorrectMediaType() {
            assertThat(bookService.getMediaType()).isEqualTo("books");
        }
    }
}
