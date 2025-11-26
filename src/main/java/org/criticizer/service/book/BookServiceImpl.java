package org.criticizer.service.book;

import org.criticizer.dao.book.BookDao;
import org.criticizer.entity.Book;
import org.criticizer.service.helper.ServiceValidator;
import org.criticizer.service.user.UserPageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of the BookService interface for managing book-related operations.
 */
public class BookServiceImpl implements BookService {
    private static final Logger log = LoggerFactory.getLogger(BookServiceImpl.class);
    private final ServiceValidator validator;
    private final BookDao bookDao;

    public BookServiceImpl(BookDao bookDao, ServiceValidator validator) {
        this.bookDao = bookDao;
        this.validator = validator;
    }

    @Override
    public List<Book> getUserBooks(int userId) {
        return bookDao.getUserBooks(userId);
    }

    @Override
    public boolean getBookStatus(String name, int userId) {
        return bookDao.getBookStatus(name, userId);
    }

    @Override
    public void updateBookAndName(String oldName, String newName, int newScore, int userId, List<Integer> genreIds) {
        validator.validateScore(newScore, userId, "Book");

        log.info("Updating book '{}' to '{}' with score {} for user {}", oldName, newName, newScore, userId);
        bookDao.updateBookAndName(oldName, newName, newScore, userId, genreIds);
    }

    @Override
    public boolean isBookExists(String name, int userId) {
        return bookDao.isBookExists(name, userId);
    }

    @Override
    public void addBook(String name, int userId, int score, List<Integer> genreIds) {
        validator.validateScore(score, userId, "Book");

        log.info("Adding book '{}' with score {} for user {}", name, score, userId);
        bookDao.addBook(name, userId, score, genreIds);
    }

    @Override
    public UserPageResult<Book> getUserBooksPage(int userId, int page, int pageSize,
                                                 Integer genreId, String searchTerm,
                                                 String sortBy, String sortOrder) {
        ServiceValidator.PaginationParams params = validator.validatePagination(page, pageSize);
        String sanitizedSearch = validator.sanitizeSearchTerm(searchTerm);

        List<Book> booksOnPage = bookDao.findBooksByUserId(
                userId, genreId, sanitizedSearch,
                params.offset(), params.pageSize(),
                sortBy, sortOrder
        );

        int totalBooks = bookDao.countTotalForUser(userId, genreId, sanitizedSearch);

        return new UserPageResult<>(booksOnPage, totalBooks, params.page(), params.pageSize());
    }

    @Override
    public void removeBook(String name, int userId) {
        log.info("Removing book '{}' for user {}", name, userId);
        bookDao.removeBook(name, userId);
    }

    @Override
    public void toggleBookStatus(String name, int userId) {
        log.debug("Toggling status for book '{}' for user {}", name, userId);
        bookDao.toggleBookStatus(name, userId);
    }
}