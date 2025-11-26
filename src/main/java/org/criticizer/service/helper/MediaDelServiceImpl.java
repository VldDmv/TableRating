package org.criticizer.service.helper;

import org.criticizer.dao.book.BookDao;
import org.criticizer.dao.game.GameDao;
import org.criticizer.dao.helper.DaoHelperService;
import org.criticizer.dao.movie.MovieDao;
import org.criticizer.dao.show.ShowDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaDelServiceImpl implements MediaDelService {
    private static final Logger log = LoggerFactory.getLogger(MediaDelServiceImpl.class);

    private final GameDao gameDao;
    private final ShowDao showDao;
    private final MovieDao movieDao;
    private final BookDao bookDao;
    private final DaoHelperService daoHelper;

    public MediaDelServiceImpl(GameDao gameDao, ShowDao showDao,
                               MovieDao movieDao, BookDao bookDao,
                               DaoHelperService daoHelper) {
        this.gameDao = gameDao;
        this.showDao = showDao;
        this.movieDao = movieDao;
        this.bookDao = bookDao;
        this.daoHelper = daoHelper;
    }


    @Override
    public void deleteAllMediaForUser(int userId) {
        log.debug("Deleting all media for user ID {} in transaction", userId);

        daoHelper.executeInTransaction(conn -> {

            gameDao.deleteGamesByUserId(userId);
            log.trace("Deleted games for user {}", userId);

            showDao.deleteShowsByUserId(userId);
            log.trace("Deleted shows for user {}", userId);

            movieDao.deleteMoviesByUserId(userId);
            log.trace("Deleted movies for user {}", userId);

            bookDao.deleteBooksByUserId(userId);
            log.trace("Deleted books for user {}", userId);

        }, log);

        log.info("Successfully deleted all media for user ID {}", userId);
    }
}