package org.criticizer.servlets;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.criticizer.constants.AttribConstants.*;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.service.user.UserPageResult;
import org.criticizer.service.user.UserService;
import org.criticizer.servlets.items.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Servlet for displaying a paginated list of all public users.
 */
@WebServlet("/users")
public class UsersServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(UsersServlet.class);
    private UserService userService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.userService = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.USER_SERVICE,
                UserService.class
        );
    }

    /**
     * Handles GET requests to display a paginated list of public users
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.debug("Public UsersServlet received GET request.");

        try {

            String pageParam = request.getParameter(RequestParams.PAGE);
            String searchTerm = request.getParameter(RequestParams.SEARCH);

            int currentPage = ServletHelper.parseIntParam(pageParam, Defaults.PAGE, log);
            String effectiveSearchTerm = (searchTerm != null) ? searchTerm.trim() : null;

            UserPageResult<User> pageResult = userService.getUsersPage(
                    effectiveSearchTerm, currentPage, Defaults.PAGE_SIZE_PUBLIC, true);

            request.setAttribute(RequestAttributes.USER_LIST, pageResult.getItems());
            request.setAttribute(RequestAttributes.CURRENT_PAGE, pageResult.getCurrentPage());
            request.setAttribute(RequestAttributes.TOTAL_PAGES, pageResult.getTotalPages());
            request.setAttribute(RequestAttributes.SEARCH_TERM, effectiveSearchTerm);

            RequestDispatcher dispatcher = request.getRequestDispatcher(Paths.USERS_JSP);
            dispatcher.forward(request, response);

        } catch (Exception e) {

            ServletHelper.handleError(request, response,
                    new DatabaseException("getUsersPage", e), log, "UsersServlet GET");
        }
    }
}