package org.criticizer.controller.category;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.criticizer.dto.book.BookResponse;
import org.criticizer.entity.Book;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.book.BookService;
import org.criticizer.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookController Tests")
class BookControllerTest {

    @Mock private BookService bookService;

    @Mock private SecurityUtil securityUtil;

    @InjectMocks private BookController controller;

    @Test
    @DisplayName("Should return correct entity name")
    void shouldReturnCorrectEntityName() {
        assertEquals("Book", controller.getEntityName());
    }

    @Test
    @DisplayName("Should convert Book to BookResponse")
    void shouldConvertBookToResponse() {
        // Given
        Book book = TestDataBuilder.createBook(1, "Test Book", 1, 90);

        // When
        BookResponse response = controller.convertToResponse(book);

        // Then
        assertEquals(book.getId(), response.getId());
        assertEquals(book.getName(), response.getName());
    }
}
