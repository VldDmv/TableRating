package org.criticizer.servlets.items;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.criticizer.entity.User;
import org.criticizer.exceptions.ApplicationException;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.service.user.UserPageResult;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.criticizer.constants.AttribConstants.*;

/**
 * Abstract base servlet for category handling (Games, Movies, Books, Shows).
 *
 * @param <T> Entity type (Game, Book, Movie, Show)
 * @param <S> Service type (GameService, BookService, etc.)
 */
public abstract class AbstractCategoryServlet<T, S> extends HttpServlet {
    private static final int MAX_ASSOCIATION_IDS = 100;
    protected final Logger log;
    protected final Gson gson = new Gson();
    protected S service;

    protected AbstractCategoryServlet(Logger log) {
        this.log = log;
    }

    // ============= Template Methods  =============

    protected abstract String getCategoryName();
    protected abstract String getEntityNameSingular();
    protected abstract String getEntityNamePlural();
    protected abstract Map<String, String> getParamNames();
    protected abstract String getAddFormId();
    protected abstract void setAssociations(HttpServletRequest request);

    protected abstract UserPageResult<T> getPage(int userId, int page, int pageSize,
                                                 Integer filterId, String searchTerm,
                                                 String sortBy, String sortOrder);

    protected abstract void addItem(String name, int userId, int score, List<Integer> associationIds);
    protected abstract void removeItem(String name, int userId);
    protected abstract void updateItem(String oldName, String newName, int newScore,
                                       int userId, List<Integer> associationIds);
    protected abstract boolean getItemStatus(String name, int userId);
    protected abstract void toggleItemStatus(String name, int userId);

    // ============= GET Handler =============

    /**
     * Handles GET requests - either AJAX data requests or initial page load.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String context = "GET /" + getCategoryName();

        try {
            // Require authentication
            User user = ServletHelper.requireAuthentication(request);

            if (ServletHelper.isAjaxRequest(request)) {
                handleAjaxDataRequest(request, response, user);
            } else {
                handleInitialPageLoad(request, response, user);
            }

        } catch (ApplicationException e) {
            // Custom business exceptions
            ServletHelper.handleError(request, response, e, log, context);
        } catch (Exception e) {
            // Unexpected system errors
            log.error("{}: Unexpected error", context, e);
            ServletHelper.handleError(request, response,
                    new DatabaseException(context, e), log, context);
        }
    }

    /**
     * Handles AJAX requests for paginated/filtered/sorted data.
     */
    private void handleAjaxDataRequest(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {

        // Parse pagination parameters
        int page = ServletHelper.parseIntParam(
                request.getParameter(RequestParams.PAGE),
                Defaults.PAGE,
                log
        );
        int pageSize = ServletHelper.parseIntParam(
                request.getParameter(RequestParams.ROWS),
                Defaults.PAGE_SIZE_ITEMS,
                log
        );

        // Parse filter ID (tag or genre)
        String filterIdParam = request.getParameter(RequestParams.TAG_ID);
        if (filterIdParam == null) {
            filterIdParam = request.getParameter(RequestParams.GENRE_ID);
        }

        Integer filterId = parseFilterId(filterIdParam);

        // Parse search and sort parameters
        String searchTerm = ServletHelper.getOptionalParameter(request, RequestParams.SEARCH);
        String sortBy = ServletHelper.getOptionalParameter(request, RequestParams.SORT_BY);
        String sortOrder = ServletHelper.getOptionalParameter(request, RequestParams.SORT_ORDER);

        // Set defaults if not provided
        if (sortBy == null) sortBy = Defaults.DEFAULT_SORT_BY;
        if (sortOrder == null) sortOrder = Defaults.DEFAULT_SORT_ORDER;

        // Fetch data
        UserPageResult<T> pageResult = getPage(
                user.getId(), page, pageSize,
                filterId, searchTerm, sortBy, sortOrder
        );

        // Send JSON response
        ServletHelper.sendJsonSuccess(response, pageResult);
    }

    /**
     * Handles initial page load - renders JSP with initial data.
     */
    private void handleInitialPageLoad(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {

        // Fetch initial data with defaults
        UserPageResult<T> pageResult = getPage(
                user.getId(),
                Defaults.PAGE,
                Defaults.PAGE_SIZE_ITEMS,
                null, null,
                Defaults.DEFAULT_SORT_BY,
                Defaults.DEFAULT_SORT_ORDER
        );

        // Set attributes for JSP
        request.setAttribute(RequestAttributes.PAGE_RESULT, pageResult);
        request.setAttribute(RequestAttributes.ENTITY_TYPE, getCategoryName());
        request.setAttribute(RequestAttributes.ENTITY_NAME_SINGULAR, getEntityNameSingular());
        request.setAttribute(RequestAttributes.ENTITY_NAME_PLURAL, getEntityNamePlural());
        request.setAttribute(RequestAttributes.PARAM_NAMES, getParamNames());
        request.setAttribute(RequestAttributes.ADD_FORM_ID, getAddFormId());
        request.setAttribute(RequestAttributes.GSON, gson);

        // Set tags or genres (implemented by subclasses)
        setAssociations(request);

        // Forward to JSP template
        request.getRequestDispatcher(Paths.LIST_RATINGS_TEMPLATE)
                .forward(request, response);
    }

    // ============= POST Handler =============

    /**
     * Handles POST requests for CRUD operations (add, remove, update, toggle).
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String context = "POST /" + getCategoryName();

        try {
            // Require authentication
            User user = ServletHelper.requireAuthentication(request);

            boolean isAjax = ServletHelper.isAjaxRequest(request);
            Map<String, String> params = getParamNames();

            // Determine which operation to perform based on parameters
            if (request.getParameter(params.get("addItemName")) != null) {
                handleAdd(request, response, user.getId());
            } else if (request.getParameter(params.get("removeItem")) != null) {
                handleRemove(request, response, user.getId());
            } else if (request.getParameter(params.get("oldItemName")) != null) {
                handleUpdate(request, response, user.getId());
                return; // AJAX response already sent
            } else if (request.getParameter(params.get("toggleItemStatus")) != null) {
                handleToggle(request, response, user.getId());
                return; // AJAX response already sent
            } else {
                throw new InvalidInputException("action", "Unknown action");
            }

            // For non-AJAX requests, redirect back to the category page
            if (!isAjax) {
                ServletHelper.redirectTo(response, request, "/" + getCategoryName());
            }

        } catch (ApplicationException e) {
            // Custom business exceptions
            ServletHelper.handleError(request, response, e, log, context);
        } catch (Exception e) {
            // Unexpected system errors
            log.error("{}: Unexpected error", context, e);
            ServletHelper.handleError(request, response,
                    new DatabaseException(context, e), log, context);
        }
    }

    // ============= CRUD Operation Handlers =============

    /**
     * Handles adding a new item.
     */
    private void handleAdd(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException {

        Map<String, String> params = getParamNames();

        // Get and validate required parameters
        String name = ServletHelper.getRequiredParameter(request, params.get("addItemName"));
        String scoreParam = ServletHelper.getRequiredParameter(request, params.get("addItemScore"));

        // Parse score
        int score = ServletHelper.parseIntParamRequired(scoreParam, "score");

        // Parse association IDs (tags or genres)
        List<Integer> associationIds = parseIds(
                request.getParameterValues(params.get("addItemTagIds"))
        );

        // Add item via service
        addItem(name, userId, score, associationIds);

        log.info("Added {} '{}' for user {}", getEntityNameSingular(), name, userId);
        ServletHelper.setFlashSuccess(request.getSession(),
                getEntityNameSingular() + " added successfully.");
    }

    /**
     * Handles removing an existing item.
     */
    private void handleRemove(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException {

        Map<String, String> params = getParamNames();
        String name = ServletHelper.getRequiredParameter(request, params.get("removeItem"));

        // Remove item via service
        removeItem(name, userId);

        log.info("Removed {} '{}' for user {}", getEntityNameSingular(), name, userId);
        ServletHelper.setFlashSuccess(request.getSession(),
                getEntityNameSingular() + " removed successfully.");
    }

    /**
     * Handles updating an existing item (AJAX only).
     */
    private void handleUpdate(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException {

        Map<String, String> params = getParamNames();

        // Get and validate parameters
        String oldName = ServletHelper.getRequiredParameter(request, params.get("oldItemName"));
        String newName = ServletHelper.getRequiredParameter(request, params.get("updatedItemName"));
        String scoreParam = ServletHelper.getRequiredParameter(request, params.get("updatedItemScore"));


        int newScore = ServletHelper.parseIntParamRequired(scoreParam, "score");


        List<Integer> associationIds = parseIds(
                request.getParameterValues(params.get("updatedItemTagIds"))
        );

        // Update item via service
        updateItem(oldName, newName, newScore, userId, associationIds);

        // Send JSON success response
        ServletHelper.sendJsonSuccess(response, Map.of("message", "Updated successfully"));

        log.info("Updated {} '{}' for user {}", getEntityNameSingular(), oldName, userId);
    }

    /**
     * Handles toggling item status (AJAX only).
     */
    private void handleToggle(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException {

        Map<String, String> params = getParamNames();
        String name = ServletHelper.getRequiredParameter(request, params.get("toggleItemStatus"));

        // Toggle status via service
        toggleItemStatus(name, userId);

        // Get new status
        boolean newStatus = getItemStatus(name, userId);

        // Send JSON response with new status
        ServletHelper.sendJsonSuccess(response,
                Map.of("newStatus", newStatus, "itemName", name));

        log.debug("Toggled status for {} '{}' to {}", getEntityNameSingular(), name, newStatus);
    }

    // ============= Helper Methods =============

    /**
     * Parses an array of ID strings into a list of integers.
     * Returns empty list if input is null or empty.
     * Throws InvalidInputException if any ID is not a valid number.
     */
    private List<Integer> parseIds(String[] idsStr) {
        if (idsStr == null || idsStr.length == 0) {
            return Collections.emptyList();
        }
        if (idsStr.length > MAX_ASSOCIATION_IDS) {
            log.warn("Too many association IDs provided: {} (max {})",
                    idsStr.length, MAX_ASSOCIATION_IDS);
            throw new InvalidInputException(
                    "associationIds",
                    "too many IDs provided (max " + MAX_ASSOCIATION_IDS + ", got " + idsStr.length + ")"
            );
        }

        try {
            return Arrays.stream(idsStr)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.warn("Invalid ID format in array: {}", (Object) idsStr);
            throw new InvalidInputException("associationIds", "must be valid numbers");
        }
    }
    /**
     * Parses filter ID parameter (tag or genre ID).
     * Returns null if parameter is missing, empty, or "all".
     * Returns null (with warning) if format is invalid.
     */
    private Integer parseFilterId(String filterIdParam) {
        if (filterIdParam == null || filterIdParam.trim().isEmpty() ||
                "all".equalsIgnoreCase(filterIdParam)) {
            return null;
        }

        try {
            return Integer.parseInt(filterIdParam.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid filter ID format: '{}', ignoring filter", filterIdParam);
            return null;
        }
    }
}