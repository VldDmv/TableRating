package org.criticizer.dao.book;

import org.criticizer.entity.Book;

import java.util.List;

public interface BookDao {
    List<Book> getUserBooks(int userId);

    int addBook(String name, int userId, int score, List<Integer> genreIds);

    void removeBook(String name, int userId);

    void toggleBookStatus(String name, int userId);

    void updateBookAndName(String oldName, String newName, int newScore, int userId, List<Integer> genreIds);

    boolean isBookExists(String name, int userId);

    boolean getBookStatus(String name, int userId);

    void deleteBooksByUserId(int userId);

    int countTotal();

    List<Book> findBooksByUserId(int userId, Integer genreId, String searchTerm, int offset, int limit, String sortBy, String sortOrder);

    int countTotalForUser(int userId, Integer genreId, String searchTerm);
}
