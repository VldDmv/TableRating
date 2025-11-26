
package org.criticizer.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.criticizer.dao.book.BookDao;
import org.criticizer.dao.genre.GenreDao;
import org.criticizer.dao.helper.DaoFactory;
import org.criticizer.dao.helper.DaoFactoryService;
import org.criticizer.dao.movie.MovieDao;
import org.criticizer.dao.show.ShowDao;
import org.criticizer.dao.user.UserDao;
import org.criticizer.entity.Genre;
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
class GenreDaoServiceTest {

    private static final Logger log = LoggerFactory.getLogger(GenreDaoServiceTest.class);
    private GenreDao genreDao;
    private MovieDao movieDao;
    private BookDao bookDao;
    private UserDao userDao;
    private ShowDao showDao;

    private int testUserId;
    private int testMovieId;
    private int testBookId;
    private int testShowId;

    @BeforeAll
    void setUpAll() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test_genre_db;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        DataSourceProvider.initialize(new HikariDataSource(config));
        log.info("Test DataSource for GenreDao initialized.");

        try (Connection conn = DataSourceProvider.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("RUNSCRIPT FROM 'classpath:schema.sql'");
            log.info("Database schema created successfully for GenreDao tests.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test schema", e);
        }
        DaoFactory daoFactory = new DaoFactoryService();
        this.genreDao = daoFactory.getGenreDao();
        this.movieDao = daoFactory.getMovieDao();
        this.bookDao = daoFactory.getBookDao();
        this.showDao = daoFactory.getShowDao();
        this.userDao = daoFactory.getUserDao();
    }

    @BeforeEach
    void setUp() {
        try (Connection conn = DataSourceProvider.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE;");
            stmt.execute("TRUNCATE TABLE movie_genres RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE book_genres RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE show_genres RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE genre_applicability RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE movies RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE books RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE shows RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE users RESTART IDENTITY;");
            stmt.execute("TRUNCATE TABLE genres RESTART IDENTITY;");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE;");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear tables", e);
        }

        userDao.addUser(new User("genreTestUser", "pass"));
        User user = userDao.findUserByName("genreTestUser");
        testUserId = user.getId();
        testMovieId = movieDao.addMovie("Test Movie for Genres", testUserId, 88, null);
        testBookId = bookDao.addBook("Test Book for Genres", testUserId, 78, null);
        testShowId = showDao.addShow("Test Show for Genres", testUserId, 82, null);
    }

    @AfterAll
    void tearDownAll() {
        DataSourceProvider.close();
        log.info("Test DataSource for GenreDao closed.");
    }

    @Test
    @DisplayName("addGenre and getAllGenres should correctly store and retrieve genres with media types")
    void addAndGetAllGenres() {
        genreDao.addGenre("Sci-Fi", List.of("movie", "book"));
        genreDao.addGenre("Comedy", List.of("shared"));

        List<Genre> genres = genreDao.getAllGenres();
        assertEquals(2, genres.size());

        Genre comedy = genres.get(0);
        assertEquals("Comedy", comedy.getName());
        assertTrue(comedy.getMediaTypes().contains("shared"));

        Genre scifi = genres.get(1);
        assertEquals("Sci-Fi", scifi.getName());
        assertTrue(scifi.getMediaTypes().contains("movie"));
        assertTrue(scifi.getMediaTypes().contains("book"));
    }

    @Test
    @DisplayName("updateGenre should change name and media types")
    void updateGenre() {
        genreDao.addGenre("Horror", List.of("movie"));
        Genre genre = genreDao.getAllGenres().get(0);

        genreDao.updateGenre(genre.getId(), "Psychological Horror", List.of("movie", "book"));

        Genre updatedGenre = genreDao.getAllGenres().get(0);
        assertEquals("Psychological Horror", updatedGenre.getName());
        assertEquals(2, updatedGenre.getMediaTypes().size());
        assertTrue(updatedGenre.getMediaTypes().contains("book"));
    }


    @Test
    @DisplayName("getAvailableGenresFor should return type-specific and shared genres")
    void getAvailableGenresFor() {
        genreDao.addGenre("Biography", List.of("book"));
        genreDao.addGenre("Action", List.of("shared"));
        genreDao.addGenre("Documentary", List.of("movie"));

        List<Genre> movieGenres = genreDao.getAvailableGenresFor("movie");
        assertEquals(2, movieGenres.size());
        assertTrue(movieGenres.stream().anyMatch(g -> g.getName().equals("Action")));
        assertTrue(movieGenres.stream().anyMatch(g -> g.getName().equals("Documentary")));

        List<Genre> bookGenres = genreDao.getAvailableGenresFor("book");
        assertEquals(2, bookGenres.size());
        assertTrue(bookGenres.stream().anyMatch(g -> g.getName().equals("Action")));
        assertTrue(bookGenres.stream().anyMatch(g -> g.getName().equals("Biography")));
    }

    @Test
    @DisplayName("deleteGenre should remove a genre from the database")
    void deleteGenre_succeeds() {
        log.info("TEST: deleteGenre_succeeds - START");
        genreDao.addGenre("To Delete", List.of("shared"));
        Genre genre = genreDao.getAllGenres().stream().filter(g -> g.getName().equals("To Delete")).findFirst().get();
        assertNotNull(genre);

        assertDoesNotThrow(() -> genreDao.deleteGenre(genre.getId()));

        List<Genre> genres = genreDao.getAllGenres();
        assertTrue(genres.stream().noneMatch(g -> g.getName().equals("To Delete")));
        log.info("TEST: deleteGenre_succeeds - END");
    }

    @Test
    @DisplayName("isGenreInUse should return true for linked genres and false for unlinked")
    void isGenreInUse() {
        log.info("TEST: isGenreInUse - START");
        genreDao.addGenre("Movie Genre", List.of("movie"));
        genreDao.addGenre("Book Genre", List.of("book"));
        genreDao.addGenre("Show Genre", List.of("show"));
        genreDao.addGenre("Unused Genre", List.of("shared"));

        Genre movieGenre = genreDao.getAllGenres().stream().filter(g -> g.getName().equals("Movie Genre")).findFirst().get();
        Genre bookGenre = genreDao.getAllGenres().stream().filter(g -> g.getName().equals("Book Genre")).findFirst().get();
        Genre showGenre = genreDao.getAllGenres().stream().filter(g -> g.getName().equals("Show Genre")).findFirst().get();
        Genre unusedGenre = genreDao.getAllGenres().stream().filter(g -> g.getName().equals("Unused Genre")).findFirst().get();

        movieDao.updateMovieAndName("Test Movie for Genres", "Test Movie for Genres", 88, testUserId, List.of(movieGenre.getId()));
        bookDao.updateBookAndName("Test Book for Genres", "Test Book for Genres", 78, testUserId, List.of(bookGenre.getId()));
        showDao.updateShowAndName("Test Show for Genres", "Test Show for Genres", 82, testUserId, List.of(showGenre.getId()));

        assertTrue(genreDao.isGenreInUse(movieGenre.getId()), "Genre used by movie should be marked as in use.");
        assertTrue(genreDao.isGenreInUse(bookGenre.getId()), "Genre used by book should be marked as in use.");
        assertTrue(genreDao.isGenreInUse(showGenre.getId()), "Genre used by show should be marked as in use.");
        assertFalse(genreDao.isGenreInUse(unusedGenre.getId()), "Unused genre should not be marked as in use.");
        log.info("TEST: isGenreInUse - END");
    }

    @Test
    @DisplayName("getGenresFor... methods should return correct genres for each entity type")
    void getGenresForEntities_shouldReturnCorrectGenres() {
        log.info("TEST: getGenresForEntities_shouldReturnCorrectGenres - START");
        genreDao.addGenre("Sci-Fi", List.of("movie"));
        genreDao.addGenre("Drama", List.of("movie", "show"));
        genreDao.addGenre("Memoir", List.of("book"));

        Genre scifi = genreDao.getAllGenres().stream().filter(g -> g.getName().equals("Sci-Fi")).findFirst().get();
        Genre drama = genreDao.getAllGenres().stream().filter(g -> g.getName().equals("Drama")).findFirst().get();
        Genre memoir = genreDao.getAllGenres().stream().filter(g -> g.getName().equals("Memoir")).findFirst().get();

        movieDao.updateMovieAndName("Test Movie for Genres", "Test Movie for Genres", 88, testUserId, List.of(scifi.getId(), drama.getId()));
        bookDao.updateBookAndName("Test Book for Genres", "Test Book for Genres", 78, testUserId, List.of(memoir.getId()));
        showDao.updateShowAndName("Test Show for Genres", "Test Show for Genres", 82, testUserId, List.of(drama.getId()));

        List<Genre> movieGenres = genreDao.getGenresForMovie(testMovieId);
        assertEquals(2, movieGenres.size());
        assertTrue(movieGenres.stream().anyMatch(g -> g.getName().equals("Sci-Fi")));
        assertTrue(movieGenres.stream().anyMatch(g -> g.getName().equals("Drama")));

        List<Genre> bookGenres = genreDao.getGenresForBook(testBookId);
        assertEquals(1, bookGenres.size());
        assertEquals("Memoir", bookGenres.get(0).getName());

        List<Genre> showGenres = genreDao.getGenresForShow(testShowId);
        assertEquals(1, showGenres.size());
        assertEquals("Drama", showGenres.get(0).getName());
        log.info("TEST: getGenresForEntities_shouldReturnCorrectGenres - END");
    }

}