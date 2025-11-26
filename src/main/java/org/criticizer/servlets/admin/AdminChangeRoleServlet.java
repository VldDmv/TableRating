package org.criticizer.servlets.admin;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.exceptions.ApplicationException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.service.user.UserService;
import org.criticizer.servlets.items.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.criticizer.constants.AttribConstants.*;

/**
 * Servlet for handling user role changes by administrators.
 */
@WebServlet("/admin/changeRole")
public class AdminChangeRoleServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminChangeRoleServlet.class);
    private UserService userService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.userService = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.USER_SERVICE,
                UserService.class
        );

        log.info("AdminChangeRoleServlet initialized successfully");
    }

    /**
     * Handles POST requests to change a user's role.
     * Only administrators can perform this action.
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.debug("AdminChangeRoleServlet received POST request");
        HttpSession session = request.getSession();
        String context = "Admin Change Role";

        try {
            // Require authentication
            User initiator = ServletHelper.requireAuthentication(request);

            // Get and validate parameters
            String userIdStr = ServletHelper.getRequiredParameter(request, RequestParams.USER_ID);
            String newRoleStr = ServletHelper.getRequiredParameter(request, RequestParams.NEW_ROLE);

            int targetUserId = ServletHelper.parseIntParamRequired(userIdStr, RequestParams.USER_ID);

            // Parse role enum
            Role newRole;
            try {
                newRole = Role.valueOf(newRoleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidInputException(RequestParams.NEW_ROLE, "Invalid role name");
            }

            log.info("Role change attempt: targetUserId={}, newRole={}, initiator={}",
                    targetUserId, newRole, initiator.getName());

            userService.changeUserRole(targetUserId, newRole, initiator);

            log.info("Role successfully changed for user ID {} to {}", targetUserId, newRole);
            ServletHelper.setFlashSuccess(session, "User role updated successfully!");

        } catch (ApplicationException e) {
            // All custom business exceptions
            log.warn("{}: {}", context, e.getMessage());
            ServletHelper.setFlashError(session, e.getUserMessage());
        } catch (Exception e) {
            // Unexpected system errors
            log.error("{}: Unexpected error changing user role", context, e);
            ServletHelper.setFlashError(session, "An unexpected error occurred. Please try again.");
        }

        ServletHelper.redirectTo(response, request, Paths.ADMIN_USERS_SERVLET);
    }
}