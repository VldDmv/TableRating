package org.criticizer.servlets.items;

import com.google.gson.Gson;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.criticizer.entity.User;
import org.criticizer.exceptions.ApplicationException;
import org.criticizer.exceptions.ServiceNotAvailableException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.exceptions.security.InsufficientPermissionsException;
import org.criticizer.exceptions.security.UnauthorizedException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.exceptions.validation.MissingParameterException;
import org.criticizer.exceptions.validation.ValidationException;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;

import static org.criticizer.constants.AttribConstants.*;

/**
 * Common utility methods for all servlets.
 * Provides centralized handling for authentication, parameter parsing,
 * JSON responses, error handling, and service retrieval.
 */
public class ServletHelper {

    private static final Gson gson = new Gson();

    // ============= Authentication & Session =============

    /**
     * Gets the authenticated user from the session.
     * Returns null if no session exists or user is not authenticated.
     */
    public static User getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (session != null) ? (User) session.getAttribute(SessionAttributes.USER) : null;
    }

    /**
     * Requires authentication; throws UnauthorizedException if user is not logged in.
     * Use this method when a user MUST be authenticated to proceed.
     */
    public static User requireAuthentication(HttpServletRequest request) {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            throw new UnauthorizedException();
        }
        return user;
    }

    /**
     * Sets the authenticated user in the session.
     */
    public static void setAuthenticatedUser(HttpSession session, User user) {
        session.setAttribute(SessionAttributes.USER, user);
    }

    // ============= Request Type Detection =============

    /**
     * Checks if the request is an AJAX request.
     * Looks for X-Requested-With header or custom AJAX header.
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        String xRequestedWith = request.getHeader(Headers.X_REQUESTED_WITH);
        String xRequestedWithAjax = request.getHeader(Headers.X_REQUESTED_WITH_AJAX);

        return Headers.XML_HTTP_REQUEST.equals(xRequestedWith) ||
                "true".equals(xRequestedWithAjax);
    }

    // ============= Parameter Parsing =============

    /**
     * Safely parses an integer parameter with a default fallback value.
     * Returns defaultValue if parameter is null, empty, or invalid.
     * Also ensures the parsed value is positive.
     */
    public static int parseIntParam(String param, int defaultValue, Logger log) {
        if (param == null || param.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(param.trim());
            return value > 0 ? value : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("Invalid parameter format: '{}'. Using default: {}", param, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Parses a required integer parameter.
     * Throws MissingParameterException if missing or InvalidInputException if not a valid number.
     */
    public static int parseIntParamRequired(String param, String paramName) {
        if (param == null || param.trim().isEmpty()) {
            throw new MissingParameterException(paramName);
        }
        try {
            return Integer.parseInt(param.trim());
        } catch (NumberFormatException e) {
            throw new InvalidInputException(paramName, "must be a valid number");
        }
    }

    /**
     * Gets a required string parameter.
     * Throws MissingParameterException if parameter is null or empty.
     */
    public static String getRequiredParameter(HttpServletRequest request, String paramName) {
        String value = request.getParameter(paramName);
        if (value == null || value.trim().isEmpty()) {
            throw new MissingParameterException(paramName);
        }
        return value.trim();
    }

    /**
     * Gets an optional string parameter.
     * Returns null if parameter is missing or empty.
     */
    public static String getOptionalParameter(HttpServletRequest request, String paramName) {
        String value = request.getParameter(paramName);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }

    // ============= JSON Responses =============

    /**
     * Sends a JSON response with the specified status code and data.
     */
    public static void sendJsonResponse(HttpServletResponse response, int status, Object data)
            throws IOException {
        response.setStatus(status);
        response.setContentType(ContentTypes.JSON);
        response.setCharacterEncoding(Encodings.UTF_8);
        response.getWriter().write(gson.toJson(data));
    }

    /**
     * Sends a JSON error response.
     * Creates a JSON object with success=false and the error message.
     */
    public static void sendJsonError(HttpServletResponse response, int status, String message)
            throws IOException {
        sendJsonResponse(response, status,
                Map.of("success", false, "message", message));
    }

    /**
     * Sends a JSON success response.
     * Creates a JSON object with success=true and the provided data.
     */
    public static void sendJsonSuccess(HttpServletResponse response, Object data)
            throws IOException {
        sendJsonResponse(response, HttpServletResponse.SC_OK,
                Map.of("success", true, "data", data));
    }

    // ============= Flash Messages =============

    /**
     * Sets a success flash message in the session.
     * Flash messages are displayed once and then removed.
     */
    public static void setFlashSuccess(HttpSession session, String message) {
        session.setAttribute(SessionAttributes.FLASH_SUCCESS, message);
    }

    /**
     * Sets an error flash message in the session.
     */
    public static void setFlashError(HttpSession session, String message) {
        session.setAttribute(SessionAttributes.FLASH_ERROR, message);
    }

    // ============= Error Handling =============

    /**
     * Centralized error handling for all servlets.
     * Determines the exception type and sends the appropriate response
     * This method automatically:
     * - Maps exception types to HTTP status codes
     * - Logs errors with appropriate severity levels
     * - Sends safe user messages (never exposes technical details)
     * - Handles both AJAX and non-AJAX requests differently
     */
    public static void handleError(HttpServletRequest request, HttpServletResponse response,
                                   Exception e, Logger log, String context)
            throws IOException, ServletException {

        String userMessage;
        int statusCode;

        // Determine exception type and appropriate response
        if (e instanceof ApplicationException appEx) {
            userMessage = appEx.getUserMessage();

            // Map exception type to HTTP status code
            if (e instanceof UnauthorizedException) {
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;
                log.warn("{}: Unauthorized access - {}", context, e.getMessage());
            } else if (e instanceof InsufficientPermissionsException) {
                statusCode = HttpServletResponse.SC_FORBIDDEN;
                log.warn("{}: Forbidden - {}", context, e.getMessage());
            } else if (e instanceof ResourceNotFoundException) {
                statusCode = HttpServletResponse.SC_NOT_FOUND;
                log.info("{}: Resource not found - {}", context, e.getMessage());
            } else if (e instanceof ValidationException) {
                statusCode = HttpServletResponse.SC_BAD_REQUEST;
                log.info("{}: Validation error - {}", context, e.getMessage());
            } else {
                // Other application exceptions (business logic errors)
                statusCode = HttpServletResponse.SC_BAD_REQUEST;
                log.warn("{}: Application error - {}", context, e.getMessage());
            }
        } else {
            // Unexpected system errors
            userMessage = "An unexpected server error occurred. Please try again.";
            statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            log.error("{}: Unexpected error", context, e);
        }

        // Send response based on request type
        if (isAjaxRequest(request)) {
            sendJsonError(response, statusCode, userMessage);
        } else {
            handleNonAjaxError(request, response, userMessage, statusCode);
        }
    }

    /**
     * Handles errors for regular (non-AJAX) requests.
     * Routes the error to appropriate destinations:
     * - 401: Redirect to login with flash message
     * - 400: Redirect back with flash message
     * - Others: Forward to error page
     */
    private static void handleNonAjaxError(HttpServletRequest request,
                                           HttpServletResponse response,
                                           String userMessage,
                                           int statusCode)
            throws IOException, ServletException {

        HttpSession session = request.getSession();

        // Unauthorized - redirect to login
        if (statusCode == HttpServletResponse.SC_UNAUTHORIZED) {
            setFlashError(session, userMessage);
            redirectTo(response, request, Paths.INDEX_JSP);
            return;
        }

        // Bad request (validation errors) - redirect back with message
        if (statusCode == HttpServletResponse.SC_BAD_REQUEST) {
            setFlashError(session, userMessage);
            redirectBack(response, request, Paths.DASHBOARD_JSP);
            return;
        }

        // Other errors (404, 500, etc.) - show error page
        request.setAttribute(RequestAttributes.ERROR_MESSAGE, userMessage);
        request.setAttribute(RequestAttributes.ERROR_CODE, statusCode);

        RequestDispatcher dispatcher = request.getRequestDispatcher(Paths.ERROR_PAGE);
        if (dispatcher != null) {
            dispatcher.forward(request, response);
        } else {
            // Fallback if error page doesn't exist
            response.sendError(statusCode, userMessage);
        }
    }

    // ============= Redirect Helpers =============

    /**
     * Performs a redirect with context path automatically prepended.
     */
    public static void redirectTo(HttpServletResponse response,
                                  HttpServletRequest request,
                                  String path) throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    /**
     * Redirects back to the referring page (HTTP Referer header).
     * Falls back to defaultPath if no referer is available.
     */
    public static void redirectBack(HttpServletResponse response,
                                    HttpServletRequest request,
                                    String defaultPath) throws IOException {
        String referer = request.getHeader(Headers.REFERER);
        if (referer != null && !referer.isEmpty()) {
            response.sendRedirect(referer);
        } else {
            redirectTo(response, request, defaultPath);
        }
    }

    // ============= Service Retrieval =============

    /**
     * Gets a service from ServletContext with type checking.
     * Throws ServiceNotAvailableException if service is not found or has wrong type.
     * This is the main method to use in init() methods.
     */
    public static <T> T getService(ServletContext context, String serviceName, Class<T> serviceClass) {
        Object service = context.getAttribute(serviceName);
        if (service == null) {
            throw new ServiceNotAvailableException(serviceName);
        }
        if (!serviceClass.isInstance(service)) {
            throw new ServiceNotAvailableException(serviceName + " (wrong type)");
        }
        return serviceClass.cast(service);
    }

    /**
     * Convenience overload that extracts ServletContext from HttpServletRequest.
     * Useful in doGet/doPost methods where you have request but not config.
     */
    public static <T> T getService(HttpServletRequest request, String serviceName, Class<T> serviceClass) {
        return getService(request.getServletContext(), serviceName, serviceClass);
    }
}