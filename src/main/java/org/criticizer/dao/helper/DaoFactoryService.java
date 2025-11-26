package org.criticizer.dao.helper;

import org.criticizer.dao.book.BookDao;
import org.criticizer.dao.book.BookDaoService;
import org.criticizer.dao.game.GameDao;
import org.criticizer.dao.game.GameDaoService;
import org.criticizer.dao.genre.GenreDao;
import org.criticizer.dao.genre.GenreDaoService;
import org.criticizer.dao.movie.MovieDao;
import org.criticizer.dao.movie.MovieDaoService;
import org.criticizer.dao.show.ShowDao;
import org.criticizer.dao.show.ShowDaoService;
import org.criticizer.dao.tag.TagDao;
import org.criticizer.dao.tag.TagDaoService;
import org.criticizer.dao.user.UserDao;
import org.criticizer.dao.user.UserDaoService;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating MySQL-specific DAO instances.
 */
public class DaoFactoryService extends DaoFactory {

    private final DaoHelperService daoHelper;
    private final EntityValidator validator;
    private final UserDao userDao;
    private final GameDao gameDao;
    private final BookDao bookDao;
    private final MovieDao movieDao;
    private final ShowDao showDao;
    private final TagDao tagDao;
    private final GenreDao genreDao;

    public DaoFactoryService() {
        this.daoHelper = new DaoHelperService();
        this.validator = new EntityValidator(LoggerFactory.getLogger(EntityValidator.class));
        this.userDao = new UserDaoService();
        this.gameDao = new GameDaoService(validator, daoHelper);
        this.bookDao = new BookDaoService(validator, daoHelper);
        this.movieDao = new MovieDaoService(validator, daoHelper);
        this.showDao = new ShowDaoService(validator, daoHelper);
        this.tagDao = new TagDaoService();
        this.genreDao = new GenreDaoService(daoHelper);
    }

    @Override
    public UserDao getUserDao() {
        return userDao;
    }

    @Override
    public GameDao getGameDao() {
        return gameDao;
    }

    @Override
    public MovieDao getMovieDao() {
        return movieDao;
    }

    @Override
    public ShowDao getShowDao() {
        return showDao;
    }

    @Override
    public BookDao getBookDao() {
        return bookDao;
    }

    @Override
    public TagDao getTagDao() {
        return tagDao;
    }

    @Override
    public GenreDao getGenreDao() {
        return genreDao;
    }
}