
package org.criticizer.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.criticizer.dao.game.GameDao;
import org.criticizer.dao.helper.DaoFactory;
import org.criticizer.dao.helper.DaoFactoryService;
import org.criticizer.dao.tag.TagDao;
import org.criticizer.dao.user.UserDao;
import org.criticizer.entity.Tag;
import org.criticizer.entity.User;
import org.criticizer.util.DataSourceProvider;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TagDaoServiceTest {

    private static final Logger log = LoggerFactory.getLogger(TagDaoServiceTest.class);
    private TagDao tagDao;
    private GameDao gameDao;
    private UserDao userDao;

    private int testUserId;
    private int testGameId;

    @BeforeAll
    void setUpAll() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test_tag_db;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        DataSourceProvider.initialize(new HikariDataSource(config));
        log.info("Test DataSource for TagDao initialized.");

        try (Connection conn = DataSourceProvider.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("RUNSCRIPT FROM 'classpath:schema.sql'");
            log.info("Database schema created successfully for TagDao tests.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test schema", e);
        }
        DaoFactory daoFactory = new DaoFactoryService();
        this.tagDao = daoFactory.getTagDao();
        this.gameDao = daoFactory.getGameDao();
        this.userDao = daoFactory.getUserDao();
    }

    @BeforeEach
    void setUp() {
        try (Connection conn = DataSourceProvider.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE;");
            stmt.execute("TRUNCATE TABLE game_tags RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE games RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE users RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE tags RESTART IDENTITY;");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE;");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear tables", e);
        }
        userDao.addUser(new User("tagTestUser", "pass"));
        User user = userDao.findUserByName("tagTestUser");
        testUserId = user.getId();
        testGameId = gameDao.addGame("Test Game for Tags", testUserId, 80, null);
    }

    @AfterAll
    void tearDownAll() {
        DataSourceProvider.close();
        log.info("Test DataSource for TagDao closed.");
    }

    @Test
    @DisplayName("getAllTags should return empty list when no tags exist")
    void getAllTags_noTags_returnsEmptyList() {
        List<org.criticizer.entity.Tag> tags = tagDao.getAllTags();
        assertTrue(tags.isEmpty());
    }

    @Test
    @DisplayName("addTag and getAllTags should work correctly")
    void addAndGetAllTags() {
        tagDao.addTag("RPG");
        tagDao.addTag("Action");
        List<org.criticizer.entity.Tag> tags = tagDao.getAllTags();
        assertEquals(2, tags.size());
        assertEquals("Action", tags.get(0).getName());
        assertEquals("RPG", tags.get(1).getName());
    }

    @Test
    @DisplayName("updateTag should change the name of an existing tag")
    void updateTag_changesName() {
        tagDao.addTag("Strategy");
        org.criticizer.entity.Tag tag = tagDao.getAllTags().get(0);
        tagDao.updateTag(tag.getId(), "Real-Time Strategy");
        org.criticizer.entity.Tag updatedTag = tagDao.getAllTags().get(0);
        assertEquals("Real-Time Strategy", updatedTag.getName());
        assertEquals(tag.getId(), updatedTag.getId());
    }

    @Test
    @DisplayName("deleteTag should remove a tag")
    void deleteTag_removesTag() {
        tagDao.addTag("Puzzle");
        assertEquals(1, tagDao.getAllTags().size());
        org.criticizer.entity.Tag tag = tagDao.getAllTags().get(0);
        tagDao.deleteTag(tag.getId());
        assertTrue(tagDao.getAllTags().isEmpty());
    }

    @Test
    @DisplayName("getTagsForGame should return correct tags for a specific game")
    void getTagsForGame() {
        tagDao.addTag("FPS");
        tagDao.addTag("Shooter");
        org.criticizer.entity.Tag tag1 = tagDao.getAllTags().get(0);
        org.criticizer.entity.Tag tag2 = tagDao.getAllTags().get(1);

        gameDao.updateGameAndName("Test Game for Tags", "Test Game for Tags", 80, testUserId, List.of(tag1.getId(), tag2.getId()));

        List<org.criticizer.entity.Tag> gameTags = tagDao.getTagsForGame(testGameId);
        assertEquals(2, gameTags.size());
        assertTrue(gameTags.stream().anyMatch(t -> t.getName().equals("FPS")));
        assertTrue(gameTags.stream().anyMatch(t -> t.getName().equals("Shooter")));
    }

    @Test
    @DisplayName("isTagInUse should return true for used tag and false for unused tag")
    void isTagInUse() {
        log.info("TEST: isTagInUse - START");
        tagDao.addTag("Used Tag");
        tagDao.addTag("Unused Tag");
        org.criticizer.entity.Tag usedTag = tagDao.getAllTags().stream().filter(t -> t.getName().equals("Used Tag")).findFirst().get();
        Tag unusedTag = tagDao.getAllTags().stream().filter(t -> t.getName().equals("Unused Tag")).findFirst().get();

        gameDao.updateGameAndName("Test Game for Tags", "Test Game for Tags", 80, testUserId, List.of(usedTag.getId()));
        assertFalse(tagDao.isTagInUse(unusedTag.getId()), "Unused tag should not be in use.");
        assertTrue(tagDao.isTagInUse(usedTag.getId()), "Used tag should be in use.");
        log.info("TEST: isTagInUse - END");
    }
}