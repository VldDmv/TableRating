package org.criticizer.servlets.items;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.criticizer.entity.Book;
import org.criticizer.service.book.BookService;
import org.criticizer.service.genre.GenreService;
import org.criticizer.service.user.UserPageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.criticizer.constants.AttribConstants.*;

/**
 * Servlet for managing Books.
 */
@WebServlet("/books")
public class BooksServlet extends AbstractCategoryServlet<Book, BookService> {
    private static final Logger log = LoggerFactory.getLogger(BooksServlet.class);

    public BooksServlet() {
        super(log);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Get BookService using ServletHelper
        this.service = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.BOOK_SERVICE,
                BookService.class
        );

        log.info("BooksServlet initialized successfully");
    }

    @Override
    protected String getCategoryName() {
        return Categories.BOOKS;
    }

    @Override
    protected String getEntityNameSingular() {
        return EntityNames.BOOK_SINGULAR;
    }

    @Override
    protected String getEntityNamePlural() {
        return EntityNames.BOOK_PLURAL;
    }

    @Override
    protected Map<String, String> getParamNames() {
        return Map.of(
                "addItemName", "bookName",
                "addItemScore", "bookScore",
                "addItemTagIds", "bookGenreIds",
                "removeItem", "removeBook",
                "toggleItemStatus", "toggleBookStatus",
                "oldItemName", "oldBookName",
                "updatedItemName", "updatedBookName",
                "updatedItemScore", "updatedBookScore",
                "updatedItemTagIds", "updatedBookGenreIds"
        );
    }

    @Override
    protected String getAddFormId() {
        return "add-book-form";
    }

    @Override
    protected void setAssociations(HttpServletRequest request) {
        // Get GenreService safely using ServletHelper
        GenreService genreService = ServletHelper.getService(
                request,
                ServiceNames.GENRE_SERVICE,
                GenreService.class
        );

        // Set genres for the JSP
        request.setAttribute(RequestAttributes.ALL_GENRES,
                genreService.getAvailableGenresFor("book"));
    }

    @Override
    protected UserPageResult<Book> getPage(int userId, int page, int pageSize,
                                           Integer genreId, String searchTerm,
                                           String sortBy, String sortOrder) {
        return service.getUserBooksPage(userId, page, pageSize, genreId, searchTerm, sortBy, sortOrder);
    }

    @Override
    protected void addItem(String name, int userId, int score, List<Integer> genreIds) {
        service.addBook(name, userId, score, genreIds);
    }

    @Override
    protected void removeItem(String name, int userId) {
        service.removeBook(name, userId);
    }

    @Override
    protected void updateItem(String oldName, String newName, int newScore,
                              int userId, List<Integer> genreIds) {
        service.updateBookAndName(oldName, newName, newScore, userId, genreIds);
    }

    @Override
    protected boolean getItemStatus(String name, int userId) {
        return service.getBookStatus(name, userId);
    }

    @Override
    protected void toggleItemStatus(String name, int userId) {
        service.toggleBookStatus(name, userId);
    }
}