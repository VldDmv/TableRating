package org.criticizer.listener;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.criticizer.dao.book.BookDao;
import org.criticizer.dao.game.GameDao;
import org.criticizer.dao.genre.GenreDao;
import org.criticizer.dao.helper.DaoFactory;
import org.criticizer.dao.helper.DaoFactoryService;
import org.criticizer.dao.helper.DaoHelperService;
import org.criticizer.dao.movie.MovieDao;
import org.criticizer.dao.show.ShowDao;
import org.criticizer.dao.tag.TagDao;
import org.criticizer.dao.user.UserDao;
import org.criticizer.service.book.BookService;
import org.criticizer.service.book.BookServiceImpl;
import org.criticizer.service.dashboard.DashboardService;
import org.criticizer.service.dashboard.DashboardServiceImpl;
import org.criticizer.service.game.GameService;
import org.criticizer.service.game.GameServiceImpl;
import org.criticizer.service.genre.GenreService;
import org.criticizer.service.genre.GenreServiceImpl;
import org.criticizer.service.helper.MediaDelService;
import org.criticizer.service.helper.MediaDelServiceImpl;
import org.criticizer.service.helper.ServiceValidator;
import org.criticizer.service.movie.MovieService;
import org.criticizer.service.movie.MovieServiceImpl;
import org.criticizer.service.show.ShowService;
import org.criticizer.service.show.ShowServiceImpl;
import org.criticizer.service.tag.TagService;
import org.criticizer.service.tag.TagServiceImpl;
import org.criticizer.service.user.UserService;
import org.criticizer.service.user.UserServiceImpl;
import org.criticizer.util.ConfigLoader;
import org.criticizer.util.DataSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.criticizer.constants.AttribConstants.ServiceNames;

/**
 * Servlet context listener that initializes and destroys the application context.
 * Sets up the HikariCP data source and registers services in the servlet context.
 */
@WebListener
public class AppContextListener implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    /**
     * Initializes the application context by configuring the HikariCP data source
     * and registering services.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("Initializing application context...");

        // --- Database Configuration ---
        HikariConfig config = new HikariConfig();
        String url = ConfigLoader.getDbUrl();
        String user = ConfigLoader.getDbUser();
        String password = ConfigLoader.getDbPassword();

        if (url == null || user == null || password == null) {
            log.error("FATAL: Database configuration properties are not set!");
            throw new RuntimeException("Database configuration not found!");
        }

        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // --- HikariCP Pool Tuning ---
        config.setMaximumPoolSize(10);      // Max number of connections in pool
        config.setMinimumIdle(2);           // Min number of idle connections
        config.setConnectionTimeout(30000); // Max wait time for connection (ms)
        config.setIdleTimeout(600000);      // Max idle time in pool (ms)
        config.setMaxLifetime(1800000);     // Max lifetime of connection (ms)
        config.setConnectionTestQuery("SELECT 1"); // Query to test connections

        try {
            HikariDataSource ds = new HikariDataSource(config);
            DataSourceProvider.initialize(ds);
            log.info("HikariCP DataSource initialized successfully");

            // --- DAO Layer Initialization ---
            DaoFactory daoFactory = new DaoFactoryService();
            DaoHelperService daoHelper = new DaoHelperService();
            UserDao userDao = daoFactory.getUserDao();
            GameDao gameDao = daoFactory.getGameDao();
            BookDao bookDao = daoFactory.getBookDao();
            MovieDao movieDao = daoFactory.getMovieDao();
            ShowDao showDao = daoFactory.getShowDao();
            TagDao tagDao = daoFactory.getTagDao();
            GenreDao genreDao = daoFactory.getGenreDao();

            // --- Service Layer Initialization ---
            ServiceValidator serviceValidator = new ServiceValidator(
                    LoggerFactory.getLogger(ServiceValidator.class)
            );

            MediaDelService mediaDelService = new MediaDelServiceImpl(
                    gameDao, showDao, movieDao, bookDao, daoHelper
            );

            UserService userService = new UserServiceImpl(
                    userDao, mediaDelService, serviceValidator, daoHelper
            );

            DashboardService dashboardService = new DashboardServiceImpl(userDao);

            GameService gameService = new GameServiceImpl(gameDao, serviceValidator);
            BookService bookService = new BookServiceImpl(bookDao, serviceValidator);
            MovieService movieService = new MovieServiceImpl(movieDao, serviceValidator);
            ShowService showService = new ShowServiceImpl(showDao, serviceValidator);
            TagService tagService = new TagServiceImpl(tagDao);
            GenreService genreService = new GenreServiceImpl(genreDao);

            ServletContext servletContext = sce.getServletContext();
            servletContext.setAttribute(ServiceNames.USER_SERVICE, userService);
            servletContext.setAttribute(ServiceNames.GAME_SERVICE, gameService);
            servletContext.setAttribute(ServiceNames.SHOW_SERVICE, showService);
            servletContext.setAttribute(ServiceNames.MOVIE_SERVICE, movieService);
            servletContext.setAttribute(ServiceNames.BOOK_SERVICE, bookService);
            servletContext.setAttribute(ServiceNames.TAG_SERVICE, tagService);
            servletContext.setAttribute(ServiceNames.GENRE_SERVICE, genreService);
            servletContext.setAttribute(ServiceNames.DASHBOARD_SERVICE, dashboardService);

            log.info("All services initialized and registered in ServletContext");

        } catch (Exception e) {
            log.error("FATAL: Failed to initialize application context!", e);
            throw new RuntimeException("Failed to initialize application context", e);
        }
    }

    /**
     * Cleans up the application context by closing the HikariCP data source.
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Destroying application context...");
        DataSourceProvider.close();
        log.info("Application context destroyed");
    }
}