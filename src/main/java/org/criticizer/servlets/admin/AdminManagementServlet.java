package org.criticizer.servlets.admin;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.criticizer.exceptions.ApplicationException;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.exceptions.validation.MissingParameterException;
import org.criticizer.service.genre.GenreService;
import org.criticizer.service.tag.TagService;
import org.criticizer.servlets.items.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.criticizer.constants.AttribConstants.*;

/**
 * Servlet for managing both Tags and Genres by administrators.
 */
@WebServlet("/admin/management")
public class AdminManagementServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminManagementServlet.class);

    private TagService tagService;
    private GenreService genreService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Get both services using ServletHelper
        this.tagService = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.TAG_SERVICE,
                TagService.class
        );
        this.genreService = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.GENRE_SERVICE,
                GenreService.class
        );

        log.info("AdminManagementServlet initialized successfully");
    }

    /**
     * Handles GET requests to display the management page for tags or genres.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String context = "Admin Management GET";

        try {
            // Get and validate type parameter
            String type = ServletHelper.getRequiredParameter(request, RequestParams.TYPE);

            // Load appropriate data based on type
            if (ManagementTypes.TAGS.equals(type)) {
                request.setAttribute(RequestAttributes.ITEMS, tagService.getAllTags());
                request.setAttribute(RequestAttributes.ITEM_TYPE, EntityNames.TAG);
            } else if (ManagementTypes.GENRES.equals(type)) {
                request.setAttribute(RequestAttributes.ITEMS, genreService.getAllGenres());
                request.setAttribute(RequestAttributes.ITEM_TYPE, EntityNames.GENRE);
            } else {
                throw new InvalidInputException(RequestParams.TYPE,
                        "must be 'tags' or 'genres'");
            }

            request.setAttribute(RequestAttributes.TYPE, type);

            // Forward to management page
            RequestDispatcher dispatcher = request.getRequestDispatcher(Paths.ADMIN_MANAGEMENT);
            dispatcher.forward(request, response);

        } catch (ApplicationException e) {
            // Custom business exceptions
            ServletHelper.handleError(request, response, e, log, context);
        } catch (Exception e) {
            // Unexpected system errors
            log.error("{}: Unexpected error loading management page", context, e);
            ServletHelper.handleError(request, response,
                    new DatabaseException(context, e), log, context);
        }
    }

    /**
     * Handles POST requests to add, update, or delete tags or genres.
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String type = request.getParameter(RequestParams.TYPE);
        String action = request.getParameter(RequestParams.ACTION);
        String context = "Admin Management POST (type=" + type + ", action=" + action + ")";

        try {
            // Validate type parameter
            if ((!ManagementTypes.TAGS.equals(type) && !ManagementTypes.GENRES.equals(type))) {
                throw new InvalidInputException(RequestParams.TYPE,
                        "must be 'tags' or 'genres'");
            }

            // Validate action parameter
            if (action == null || action.trim().isEmpty()) {
                throw new MissingParameterException(RequestParams.ACTION);
            }

            // Perform action based on type
            if (ManagementTypes.TAGS.equals(type)) {
                handleTagAction(request, action);
            } else { // genres
                handleGenreAction(request, action);
            }

            ServletHelper.setFlashSuccess(request.getSession(), "Operation successful!");
            log.info("{}: Operation completed successfully", context);

        } catch (ApplicationException e) {
            // Custom business exceptions
            log.warn("{}: {}", context, e.getMessage());
            ServletHelper.setFlashError(request.getSession(), e.getUserMessage());
        } catch (Exception e) {
            // Unexpected system errors
            log.error("{}: Unexpected error", context, e);
            ServletHelper.setFlashError(request.getSession(),
                    "Operation failed. Please try again.");
        }

        // Redirect back to management page with type parameter
        String redirectUrl = request.getContextPath() +
                "/admin/management?" + RequestParams.TYPE + "=" + type;
        response.sendRedirect(redirectUrl);
    }

    /**
     * Handles tag-related actions (add, update, delete).
     */
    private void handleTagAction(HttpServletRequest request, String action) {


        switch (action) {
            case Actions.ADD -> {
                String name = ServletHelper.getRequiredParameter(request, RequestParams.NAME);
                tagService.createTag(name);
                log.info("Created tag: {}", name);
            }
            case Actions.UPDATE -> {
                String name = ServletHelper.getRequiredParameter(request, RequestParams.NAME);
                int tagId = ServletHelper.parseIntParamRequired(
                        request.getParameter(RequestParams.ID),
                        RequestParams.ID
                );
                tagService.editTag(tagId, name);
                log.info("Updated tag ID {}: {}", tagId, name);
            }
            case Actions.DELETE -> {
                int tagId = ServletHelper.parseIntParamRequired(
                        request.getParameter(RequestParams.ID),
                        RequestParams.ID
                );
                tagService.removeTag(tagId);
                log.info("Deleted tag ID: {}", tagId);
            }
            default -> throw new InvalidInputException(RequestParams.ACTION,
                    "unknown action for tags");
        }
    }

    /**
     * Handles genre-related actions (add, update, delete).
     */
    private void handleGenreAction(HttpServletRequest request, String action) {
        String name = ServletHelper.getRequiredParameter(request, RequestParams.NAME);

        switch (action) {
            case Actions.ADD -> {
                List<String> mediaTypes = getMediaTypes(request);
                genreService.createGenre(name, mediaTypes);
                log.info("Created genre: {} with media types: {}", name, mediaTypes);
            }
            case Actions.UPDATE -> {
                int genreId = ServletHelper.parseIntParamRequired(
                        request.getParameter(RequestParams.ID),
                        RequestParams.ID
                );
                List<String> mediaTypes = getMediaTypes(request);
                genreService.editGenre(genreId, name, mediaTypes);
                log.info("Updated genre ID {}: {} with media types: {}",
                        genreId, name, mediaTypes);
            }
            case Actions.DELETE -> {
                int genreId = ServletHelper.parseIntParamRequired(
                        request.getParameter(RequestParams.ID),
                        RequestParams.ID
                );
                genreService.removeGenre(genreId);
                log.info("Deleted genre ID: {}", genreId);
            }
            default -> throw new InvalidInputException(RequestParams.ACTION,
                    "unknown action for genres");
        }
    }

    /**
     * Gets list of media types from request parameters.
     * Returns empty list if no media types are selected.
     */
    private List<String> getMediaTypes(HttpServletRequest request) {
        String[] mediaTypes = request.getParameterValues(RequestParams.MEDIA_TYPES);
        return (mediaTypes != null) ? Arrays.asList(mediaTypes) : Collections.emptyList();
    }
}