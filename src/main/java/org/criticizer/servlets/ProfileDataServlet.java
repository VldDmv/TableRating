package org.criticizer.servlets;

import com.google.gson.Gson;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.criticizer.entity.User;
import org.criticizer.exceptions.ApplicationException;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.service.book.BookService;
import org.criticizer.service.game.GameService;
import org.criticizer.service.movie.MovieService;
import org.criticizer.service.show.ShowService;
import org.criticizer.service.user.UserService;
import org.criticizer.servlets.items.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.criticizer.constants.AttribConstants.*;

/**
 * A dedicated AJAX servlet for retrieving paginated user data for profile pages.
 */
@WebServlet("/profile-data")
public class ProfileDataServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(ProfileDataServlet.class);

    private UserService userService;
    private GameService gameService;
    private MovieService movieService;
    private BookService bookService;
    private ShowService showService;
    private final Gson gson = new Gson();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Get all required services using ServletHelper
        this.userService = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.USER_SERVICE,
                UserService.class
        );
        this.gameService = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.GAME_SERVICE,
                GameService.class
        );
        this.movieService = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.MOVIE_SERVICE,
                MovieService.class
        );
        this.bookService = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.BOOK_SERVICE,
                BookService.class
        );
        this.showService = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.SHOW_SERVICE,
                ShowService.class
        );

        log.info("ProfileDataServlet initialized successfully");
    }

    /**
     * Handles GET requests to retrieve paginated data for a user's profile in a specific category.
     * Returns the data in JSON format.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String context = "GET /profile-data";

        try {
            // Get and validate required parameters
            String username = ServletHelper.getRequiredParameter(request, RequestParams.USER_PARAM);
            String category = ServletHelper.getRequiredParameter(request, RequestParams.CATEGORY);

            // Parse page number (with default)
            int page = ServletHelper.parseIntParam(
                    request.getParameter(RequestParams.PAGE),
                    Defaults.PAGE,
                    log
            );
            int pageSize = Defaults.PAGE_SIZE_PROFILE;

            // Get user - throws UserNotFoundException if not found
            User profileOwner = userService.getUser(username);
            int userId = profileOwner.getId();

            // Get page data based on category
            Object pageResult = switch (category) {
                case Categories.GAMES -> gameService.getUserGamesPage(userId, page, pageSize, null, null, null, null);
                case Categories.MOVIES ->
                        movieService.getUserMoviesPage(userId, page, pageSize, null, null, null, null);
                case Categories.BOOKS -> bookService.getUserBooksPage(userId, page, pageSize, null, null, null, null);
                case Categories.SHOWS -> showService.getUserShowsPage(userId, page, pageSize, null, null, null, null);
                default -> throw new InvalidInputException("category", "must be games, movies, books, or shows");
            };

            // Send JSON response with the data
            ServletHelper.sendJsonResponse(response, HttpServletResponse.SC_OK, pageResult);

        } catch (ApplicationException e) {
            // All custom business exceptions (UserNotFound, InvalidInput, etc.)
            ServletHelper.handleError(request, response, e, log, context);
        } catch (Exception e) {
            // Unexpected system errors
            log.error("{}: Unexpected error loading profile data", context, e);
            ServletHelper.handleError(request, response,
                    new DatabaseException(context, e), log, context);
        }
    }
}