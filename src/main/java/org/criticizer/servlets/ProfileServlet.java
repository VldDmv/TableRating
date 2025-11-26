package org.criticizer.servlets;

import com.google.gson.Gson;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.criticizer.entity.User;
import org.criticizer.exceptions.ApplicationException;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.service.book.BookService;
import org.criticizer.service.game.GameService;
import org.criticizer.service.movie.MovieService;
import org.criticizer.service.show.ShowService;
import org.criticizer.service.user.UserService;
import org.criticizer.servlets.items.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

import static org.criticizer.constants.AttribConstants.*;

/**
 * Servlet for handling user profile page requests and privacy settings updates.
 */
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(ProfileServlet.class);

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

        log.info("ProfileServlet initialized successfully");
    }

    /**
     * Handles GET requests to display a user's profile page.
     * Enforces privacy rules, allowing access only to the owner or if the profile is public.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String context = "GET /profile";

        try {
            // Get and validate username parameter
            String profileUsername = ServletHelper.getRequiredParameter(request, RequestParams.USERNAME);

            // Get profile owner - throws UserNotFoundException if not found
            User profileOwner = userService.getUser(profileUsername);

            // Get current viewer
            User viewer = ServletHelper.getAuthenticatedUser(request);


            // Determine if the viewer is the profile owner
            boolean isOwner = (viewer != null && viewer.getId() == profileOwner.getId());

            // User can view the profile if it's public OR they are the owner
            boolean canView = profileOwner.isProfileIsPublic() || isOwner;

            // Set attributes for JSP
            request.setAttribute(RequestAttributes.PROFILE_OWNER, profileOwner);
            request.setAttribute(RequestAttributes.IS_OWNER_VIEWING, isOwner);
            request.setAttribute(RequestAttributes.CAN_VIEW, canView);

            // Only fetch the user's rated items if the viewer has permission
            if (canView) {
                loadInitialProfileData(request, profileOwner.getId());
            }

            request.setAttribute(RequestAttributes.GSON, gson);

            // Forward to profile page
            RequestDispatcher dispatcher = request.getRequestDispatcher(Paths.PROFILE_TEMPLATE);
            dispatcher.forward(request, response);

        } catch (ApplicationException e) {
            // Custom business exceptions (UserNotFound, etc.)
            ServletHelper.handleError(request, response, e, log, context);
        } catch (Exception e) {
            // Unexpected system errors
            log.error("{}: Unexpected error loading profile", context, e);
            ServletHelper.handleError(request, response,
                    new DatabaseException(context, e), log, context);
        }
    }

    /**
     * Loads initial data for the selected profile tab.
     */
    private void loadInitialProfileData(HttpServletRequest request, int userId) {
        // Get initial tab parameter (or use default)
        String initialTab = ServletHelper.getOptionalParameter(request, RequestParams.TAB);

        // Validate tab is one of the allowed categories
        if (initialTab == null ||
                !Arrays.asList(Categories.GAMES, Categories.MOVIES,
                        Categories.BOOKS, Categories.SHOWS).contains(initialTab)) {
            initialTab = Defaults.DEFAULT_TAB;
        }

        int initialPage = Defaults.PAGE;
        int pageSize = Defaults.PAGE_SIZE_PROFILE;

        // Load data based on selected tab
        Object pageResult = switch (initialTab) {
            case Categories.MOVIES ->
                    movieService.getUserMoviesPage(userId, initialPage, pageSize, null, null, null, null);
            case Categories.BOOKS ->
                    bookService.getUserBooksPage(userId, initialPage, pageSize, null, null, null, null);
            case Categories.SHOWS ->
                    showService.getUserShowsPage(userId, initialPage, pageSize, null, null, null, null);
            default -> gameService.getUserGamesPage(userId, initialPage, pageSize, null, null, null, null);
        };

        request.setAttribute(RequestAttributes.PAGE_RESULT, pageResult);
        request.setAttribute(RequestAttributes.INITIAL_TAB, initialTab);
    }

    /**
     * Handles POST requests from the owner to update their profile's privacy setting.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String context = "POST /profile";

        try {
            // Require authentication - throws UnauthorizedException if not logged in
            User user = ServletHelper.requireAuthentication(request);

            // Get privacy setting parameter
            String privacySetting = ServletHelper.getRequiredParameter(request, RequestParams.PRIVACY);
            boolean newIsPublic = Defaults.PRIVACY_PUBLIC.equals(privacySetting);

            // Update privacy setting in database
            userService.updateUserPrivacy(user.getId(), newIsPublic);

            log.info("User '{}' updated privacy setting to: {}", user.getName(), privacySetting);

            // Update user object in session
            // This ensures the change is reflected immediately without needing to re-login
            user.setProfileIsPublic(newIsPublic);
            request.getSession().setAttribute(SessionAttributes.USER, user);

            // Redirect back to the referring page (or dashboard if no referer)
            ServletHelper.redirectBack(response, request, Paths.DASHBOARD_JSP);

        } catch (ApplicationException e) {
            // Custom business exceptions
            ServletHelper.handleError(request, response, e, log, context);
        } catch (Exception e) {
            // Unexpected system errors
            log.error("{}: Unexpected error updating privacy", context, e);
            ServletHelper.handleError(request, response,
                    new DatabaseException(context, e), log, context);
        }
    }
}