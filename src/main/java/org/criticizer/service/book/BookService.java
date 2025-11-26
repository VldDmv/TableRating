package org.criticizer.service.book;

import org.criticizer.entity.Book;
import org.criticizer.service.user.UserPageResult;

import java.util.List;

public interface BookService {
    List<Book> getUserBooks(int userId);

    void addBook(String name, int userId, int score, List<Integer> genreIds);

    void removeBook(String name, int userId);

    void toggleBookStatus(String name, int userId);

    void updateBookAndName(String oldName, String newName, int newScore, int userId, List<Integer> genreIds);

    boolean isBookExists(String name, int userId);

    boolean getBookStatus(String name, int userId);

    UserPageResult<Book> getUserBooksPage(int userId, int page, int pageSize, Integer genreId, String searchTerm, String sortBy, String sortOrder);

}
