package org.criticizer.dao.book;

import org.criticizer.constants.DbConstants;
import org.criticizer.dao.helper.AbstractMediaDao;
import org.criticizer.dao.helper.DaoHelperService;
import org.criticizer.dao.helper.EntityValidator;
import org.criticizer.entity.Book;
import org.criticizer.entity.Genre;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * MySQL implementation of BookDao.
 */
public class BookDaoService extends AbstractMediaDao<Book, Genre> implements BookDao {
    private static final Logger log = LoggerFactory.getLogger(BookDaoService.class);


    public BookDaoService(EntityValidator validator, DaoHelperService daoHelper) {
        super(log, validator, daoHelper,
                DbConstants.Tables.BOOKS,
                DbConstants.Tables.BOOK_GENRES,
                DbConstants.Columns.BOOK_ID,
                DbConstants.Columns.GENRE_ID,
                DbConstants.Tables.GENRES);
    }

    @Override
    protected Book mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new Book(
                rs.getInt(DbConstants.Columns.ID),
                rs.getString(DbConstants.Columns.NAME),
                rs.getInt(DbConstants.Columns.USER_ID),
                rs.getInt(DbConstants.Columns.SCORE),
                rs.getBoolean(DbConstants.Columns.COMPLETED)
        );
    }

    @Override
    protected Genre mapResultSetToAssociation(ResultSet rs) throws SQLException {
        return new Genre(
                rs.getInt(DbConstants.Columns.ID),
                rs.getString(DbConstants.Columns.NAME)
        );
    }

    @Override
    protected void setAssociations(Book book, List<Genre> genres) {
        book.setGenres(genres);
    }

    @Override
    protected int getEntityId(Book book) {
        return book.getId();
    }

    @Override
    protected String getEntityName() {
        return "Book";
    }

    @Override
    public List<Book> getUserBooks(int userId) {
        return getUserEntities(userId);
    }

    @Override
    public int addBook(String name, int userId, int score, List<Integer> genreIds) {
        return addEntity(name, userId, score, genreIds);
    }

    @Override
    public void removeBook(String name, int userId) {
        removeEntity(name, userId);
    }

    @Override
    public void toggleBookStatus(String name, int userId) {
        toggleEntityStatus(name, userId);
    }

    @Override
    public void updateBookAndName(String oldName, String newName, int newScore,
                                  int userId, List<Integer> genreIds) {
        updateEntityAndName(oldName, newName, newScore, userId, genreIds);
    }

    @Override
    public boolean isBookExists(String name, int userId) {
        return isEntityExists(name, userId);
    }

    @Override
    public boolean getBookStatus(String name, int userId) {
        return getEntityStatus(name, userId);
    }

    @Override
    public void deleteBooksByUserId(int userId) {
        deleteEntitiesByUserId(userId);
    }

    @Override
    public int countTotal() {
        return super.countTotal();
    }

    @Override
    public List<Book> findBooksByUserId(int userId, Integer genreId, String searchTerm,
                                        int offset, int limit, String sortBy, String sortOrder) {
        return findEntities(userId, genreId, searchTerm, offset, limit, sortBy, sortOrder);
    }

    @Override
    public int countTotalForUser(int userId, Integer genreId, String searchTerm) {
        return super.countTotalForUser(userId, genreId, searchTerm);
    }
}