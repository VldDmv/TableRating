package org.criticizer.servlets.items;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.criticizer.entity.Show;
import org.criticizer.service.genre.GenreService;
import org.criticizer.service.show.ShowService;
import org.criticizer.service.user.UserPageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.criticizer.constants.AttribConstants.*;

/**
 * Servlet for managing TV Shows.
 */
@WebServlet("/shows")
public class ShowsServlet extends AbstractCategoryServlet<Show, ShowService> {
    private static final Logger log = LoggerFactory.getLogger(ShowsServlet.class);

    public ShowsServlet() {
        super(log);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Get ShowService using ServletHelper
        this.service = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.SHOW_SERVICE,
                ShowService.class
        );

        log.info("ShowsServlet initialized successfully");
    }

    @Override
    protected String getCategoryName() {
        return Categories.SHOWS;
    }

    @Override
    protected String getEntityNameSingular() {
        return EntityNames.SHOW_SINGULAR;
    }

    @Override
    protected String getEntityNamePlural() {
        return EntityNames.SHOW_PLURAL;
    }

    @Override
    protected Map<String, String> getParamNames() {
        return Map.of(
                "addItemName", "showName",
                "addItemScore", "showScore",
                "addItemTagIds", "showGenreIds",
                "removeItem", "removeShow",
                "toggleItemStatus", "toggleShowStatus",
                "oldItemName", "oldShowName",
                "updatedItemName", "updatedShowName",
                "updatedItemScore", "updatedShowScore",
                "updatedItemTagIds", "updatedShowGenreIds"
        );
    }

    @Override
    protected String getAddFormId() {
        return "add-show-form";
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
                genreService.getAvailableGenresFor("show"));
    }

    @Override
    protected UserPageResult<Show> getPage(int userId, int page, int pageSize,
                                           Integer genreId, String searchTerm,
                                           String sortBy, String sortOrder) {
        return service.getUserShowsPage(userId, page, pageSize, genreId, searchTerm, sortBy, sortOrder);
    }

    @Override
    protected void addItem(String name, int userId, int score, List<Integer> genreIds) {
        service.addShow(name, userId, score, genreIds);
    }

    @Override
    protected void removeItem(String name, int userId) {
        service.removeShow(name, userId);
    }

    @Override
    protected void updateItem(String oldName, String newName, int newScore,
                              int userId, List<Integer> genreIds) {
        service.updateShowAndName(oldName, newName, newScore, userId, genreIds);
    }

    @Override
    protected boolean getItemStatus(String name, int userId) {
        return service.getShowStatus(name, userId);
    }

    @Override
    protected void toggleItemStatus(String name, int userId) {
        service.toggleShowStatus(name, userId);
    }
}