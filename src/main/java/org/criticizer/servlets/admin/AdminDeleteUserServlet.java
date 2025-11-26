package org.criticizer.servlets.admin;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.criticizer.entity.User;
import org.criticizer.exceptions.ApplicationException;
import org.criticizer.service.user.UserService;
import org.criticizer.servlets.items.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.criticizer.constants.AttribConstants.*;

/**
 * Servlet for handling user deletion by administrators.
 */
@WebServlet("/admin/deleteUser")
public class AdminDeleteUserServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminDeleteUserServlet.class);
    private UserService userService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.userService = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.USER_SERVICE,
                UserService.class
        );

        log.info("AdminDeleteUserServlet initialized successfully");
    }

    /**
     * Handles POST requests to delete a user and their associated data.
     * Only administrators can perform this action.
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.debug("AdminDeleteUserServlet received POST request");
        HttpSession session = request.getSession();
        String context = "Admin Delete User";

        try {
            // Require authentication
            User initiator = ServletHelper.requireAuthentication(request);

            // Get and validate user ID parameter
            String userIdStr = ServletHelper.getRequiredParameter(request, RequestParams.USER_ID);
            int targetUserId = ServletHelper.parseIntParamRequired(userIdStr, RequestParams.USER_ID);

            log.info("User deletion attempt: targetUserId={}, initiator={}",
                    targetUserId, initiator.getName());

            // Delete user (may throw InsufficientPermissionsException or OperationNotPermittedException)
            userService.deleteUser(targetUserId, initiator);

            log.info("User ID {} successfully deleted by admin {}", targetUserId, initiator.getName());
            ServletHelper.setFlashSuccess(session, "User successfully deleted!");

        } catch (ApplicationException e) {
            // All custom business exceptions
            log.warn("{}: {}", context, e.getMessage());
            ServletHelper.setFlashError(session, e.getUserMessage());
        } catch (Exception e) {
            // Unexpected system errors
            log.error("{}: Unexpected error deleting user", context, e);
            ServletHelper.setFlashError(session, "An unexpected error occurred. Please try again.");
        }

        ServletHelper.redirectTo(response, request, Paths.ADMIN_USERS_SERVLET);
    }
}