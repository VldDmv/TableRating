package org.criticizer.dao.game;

import org.criticizer.constants.DbConstants;
import org.criticizer.dao.helper.AbstractMediaDao;
import org.criticizer.dao.helper.DaoHelperService;
import org.criticizer.dao.helper.EntityValidator;
import org.criticizer.entity.Game;
import org.criticizer.entity.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * MySQL implementation of GameDao.
 */
public class GameDaoService extends AbstractMediaDao<Game, Tag> implements GameDao {
    private static final Logger log = LoggerFactory.getLogger(GameDaoService.class);


    public GameDaoService(EntityValidator validator, DaoHelperService daoHelper) {
        super(log, validator, daoHelper,
                DbConstants.Tables.GAMES,
                DbConstants.Tables.GAME_TAGS,
                DbConstants.Columns.GAME_ID,
                DbConstants.Columns.TAG_ID,
                DbConstants.Tables.TAGS);
    }

    @Override
    protected Game mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new Game(
                rs.getInt(DbConstants.Columns.ID),
                rs.getString(DbConstants.Columns.NAME),
                rs.getInt(DbConstants.Columns.USER_ID),
                rs.getInt(DbConstants.Columns.SCORE),
                rs.getBoolean(DbConstants.Columns.COMPLETED)
        );
    }

    @Override
    protected Tag mapResultSetToAssociation(ResultSet rs) throws SQLException {
        return new Tag(
                rs.getInt(DbConstants.Columns.ID),
                rs.getString(DbConstants.Columns.NAME)
        );
    }

    @Override
    protected void setAssociations(Game game, List<Tag> tags) {
        game.setTags(tags);
    }

    @Override
    protected int getEntityId(Game game) {
        return game.getId();
    }

    @Override
    protected String getEntityName() {
        return "Game";
    }

    @Override
    public List<Game> getUserGames(int userId) {
        return getUserEntities(userId);
    }

    @Override
    public int addGame(String name, int userId, int score, List<Integer> tagIds) {
        return addEntity(name, userId, score, tagIds);
    }

    @Override
    public void removeGame(String name, int userId) {
        removeEntity(name, userId);
    }

    @Override
    public void toggleGameStatus(String name, int userId) {
        toggleEntityStatus(name, userId);
    }

    @Override
    public void updateGameAndName(String oldName, String newName, int newScore,
                                  int userId, List<Integer> tagIds) {
        updateEntityAndName(oldName, newName, newScore, userId, tagIds);
    }

    @Override
    public boolean isGameExists(String name, int userId) {
        return isEntityExists(name, userId);
    }

    @Override
    public boolean getGameStatus(String name, int userId) {
        return getEntityStatus(name, userId);
    }

    @Override
    public void deleteGamesByUserId(int userId) {
        deleteEntitiesByUserId(userId);
    }

    @Override
    public int countTotal() {
        return super.countTotal();
    }

    @Override
    public List<Game> findGamesByUserId(int userId, Integer tagId, String searchTerm,
                                        int offset, int limit, String sortBy, String sortOrder) {
        return findEntities(userId, tagId, searchTerm, offset, limit, sortBy, sortOrder);
    }

    @Override
    public int countTotalForUser(int userId, Integer tagId, String searchTerm) {
        return super.countTotalForUser(userId, tagId, searchTerm);
    }
}