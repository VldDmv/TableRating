
package org.criticizer.servlets.items;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.criticizer.entity.Movie;
import org.criticizer.service.genre.GenreService;
import org.criticizer.service.movie.MovieService;
import org.criticizer.service.user.UserPageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.criticizer.constants.AttribConstants.*;

/**
 * Servlet for managing Movies.
 */
@WebServlet("/movies")
public class MoviesServlet extends AbstractCategoryServlet<Movie, MovieService> {
    private static final Logger log = LoggerFactory.getLogger(MoviesServlet.class);

    public MoviesServlet() {
        super(log);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Get MovieService using ServletHelper
        this.service = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.MOVIE_SERVICE,
                MovieService.class
        );

        log.info("MoviesServlet initialized successfully");
    }

    @Override
    protected String getCategoryName() {
        return Categories.MOVIES;
    }

    @Override
    protected String getEntityNameSingular() {
        return EntityNames.MOVIE_SINGULAR;
    }

    @Override
    protected String getEntityNamePlural() {
        return EntityNames.MOVIE_PLURAL;
    }

    @Override
    protected Map<String, String> getParamNames() {
        return Map.of(
                "addItemName", "movieName",
                "addItemScore", "movieScore",
                "addItemTagIds", "movieGenreIds",
                "removeItem", "removeMovie",
                "toggleItemStatus", "toggleMovieStatus",
                "oldItemName", "oldMovieName",
                "updatedItemName", "updatedMovieName",
                "updatedItemScore", "updatedMovieScore",
                "updatedItemTagIds", "updatedMovieGenreIds"
        );
    }

    @Override
    protected String getAddFormId() {
        return "add-movie-form";
    }

    @Override
    protected void setAssociations(HttpServletRequest request) {
        // Get GenreService safely using ServletHelper
        GenreService genreService = ServletHelper.getService(
                request,
                ServiceNames.GENRE_SERVICE,
                GenreService.class
        );

        // Set genres for the JSP
        request.setAttribute(RequestAttributes.ALL_GENRES,
                genreService.getAvailableGenresFor("movie"));
    }

    @Override
    protected UserPageResult<Movie> getPage(int userId, int page, int pageSize,
                                            Integer genreId, String searchTerm,
                                            String sortBy, String sortOrder) {
        return service.getUserMoviesPage(userId, page, pageSize, genreId, searchTerm, sortBy, sortOrder);
    }

    @Override
    protected void addItem(String name, int userId, int score, List<Integer> genreIds) {
        service.addMovie(name, userId, score, genreIds);
    }

    @Override
    protected void removeItem(String name, int userId) {
        service.removeMovie(name, userId);
    }

    @Override
    protected void updateItem(String oldName, String newName, int newScore,
                              int userId, List<Integer> genreIds) {
        service.updateMovieAndName(oldName, newName, newScore, userId, genreIds);
    }

    @Override
    protected boolean getItemStatus(String name, int userId) {
        return service.getMovieStatus(name, userId);
    }

    @Override
    protected void toggleItemStatus(String name, int userId) {
        service.toggleMovieStatus(name, userId);
    }
}