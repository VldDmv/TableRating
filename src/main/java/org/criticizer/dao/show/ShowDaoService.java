package org.criticizer.dao.show;

import org.criticizer.constants.DbConstants;
import org.criticizer.dao.helper.AbstractMediaDao;
import org.criticizer.dao.helper.DaoHelperService;
import org.criticizer.dao.helper.EntityValidator;
import org.criticizer.entity.Genre;
import org.criticizer.entity.Show;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * MySQL implementation of ShowDao.
 */
public class ShowDaoService extends AbstractMediaDao<Show, Genre> implements ShowDao {
    private static final Logger log = LoggerFactory.getLogger(ShowDaoService.class);

    public ShowDaoService(EntityValidator validator, DaoHelperService daoHelper) {
        super(log, validator, daoHelper,
                DbConstants.Tables.SHOWS,
                DbConstants.Tables.SHOW_GENRES,
                DbConstants.Columns.SHOW_ID,
                DbConstants.Columns.GENRE_ID,
                DbConstants.Tables.GENRES);
    }

    @Override
    protected Show mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new Show(
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
    protected void setAssociations(Show show, List<Genre> genres) {
        show.setGenres(genres);
    }

    @Override
    protected int getEntityId(Show show) {
        return show.getId();
    }

    @Override
    protected String getEntityName() {
        return "Show";
    }

    @Override
    public List<Show> getUserShows(int userId) {
        return getUserEntities(userId);
    }

    @Override
    public int addShow(String name, int userId, int score, List<Integer> genreIds) {
        return addEntity(name, userId, score, genreIds);
    }

    @Override
    public void removeShow(String name, int userId) {
        removeEntity(name, userId);
    }

    @Override
    public void toggleShowStatus(String name, int userId) {
        toggleEntityStatus(name, userId);
    }

    @Override
    public void updateShowAndName(String oldName, String newName, int newScore,
                                  int userId, List<Integer> genreIds) {
        updateEntityAndName(oldName, newName, newScore, userId, genreIds);
    }

    @Override
    public boolean isShowExists(String name, int userId) {
        return isEntityExists(name, userId);
    }

    @Override
    public boolean getShowStatus(String name, int userId) {
        return getEntityStatus(name, userId);
    }

    @Override
    public void deleteShowsByUserId(int userId) {
        deleteEntitiesByUserId(userId);
    }

    @Override
    public int countTotal() {
        return super.countTotal();
    }

    @Override
    public List<Show> findShowsByUserId(int userId, Integer genreId, String searchTerm,
                                        int offset, int limit, String sortBy, String sortOrder) {
        return findEntities(userId, genreId, searchTerm, offset, limit, sortBy, sortOrder);
    }

    @Override
    public int countTotalForUser(int userId, Integer genreId, String searchTerm) {
        return super.countTotalForUser(userId, genreId, searchTerm);
    }
}