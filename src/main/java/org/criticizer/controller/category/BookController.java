package org.criticizer.controller.category;

import org.criticizer.controller.helper.AbstractMediaController;
import org.criticizer.dto.book.BookResponse;
import org.criticizer.dto.book.CreateBookRequest;
import org.criticizer.dto.book.UpdateBookRequest;
import org.criticizer.entity.Book;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.book.BookService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController extends AbstractMediaController<
        Book,
        BookResponse,
        CreateBookRequest,
        UpdateBookRequest> {

    public BookController(BookService bookService, SecurityUtil securityUtil) {
        super(bookService, securityUtil);
    }

    @Override
    protected String getEntityName() {
        return "Book";
    }


    @Override
    protected BookResponse convertToResponse(Book entity) {
        return BookResponse.from(entity);
    }
}