package org.criticizer.dao.helper;

import org.criticizer.dao.book.BookDao;
import org.criticizer.dao.game.GameDao;
import org.criticizer.dao.genre.GenreDao;
import org.criticizer.dao.movie.MovieDao;
import org.criticizer.dao.show.ShowDao;
import org.criticizer.dao.tag.TagDao;
import org.criticizer.dao.user.UserDao;

public abstract class DaoFactory {

    protected DaoFactory() {
    }

    public abstract UserDao getUserDao();

    public abstract GameDao getGameDao();

    public abstract ShowDao getShowDao();

    public abstract BookDao getBookDao();

    public abstract MovieDao getMovieDao();

    public abstract TagDao getTagDao();

    public abstract GenreDao getGenreDao();
}