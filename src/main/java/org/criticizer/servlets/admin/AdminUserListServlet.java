package org.criticizer.servlets.admin;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.service.user.UserPageResult;
import org.criticizer.service.user.UserService;
import org.criticizer.servlets.items.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.criticizer.constants.AttribConstants.*;

/**
 * Servlet for displaying a paginated list of users for administrators.
 */
@WebServlet("/admin/users")
public class AdminUserListServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminUserListServlet.class);
    private UserService userService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.userService = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.USER_SERVICE,
                UserService.class
        );

        log.info("AdminUserListServlet initialized successfully");
    }

    /**
     * Handles GET requests to display a paginated list of users
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.debug("AdminUserListServlet received GET request");
        String context = "Admin User List GET";

        try {
            // Parse pagination and search parameters
            int currentPage = ServletHelper.parseIntParam(
                    request.getParameter(RequestParams.PAGE),
                    Defaults.PAGE,
                    log
            );
            int pageSize = ServletHelper.parseIntParam(
                    request.getParameter(RequestParams.PAGE_SIZE),
                    Defaults.PAGE_SIZE_ADMIN,
                    log
            );
            String searchTerm = ServletHelper.getOptionalParameter(request, RequestParams.SEARCH);

            // Get paginated user list (publicOnly = false for admin view)
            UserPageResult<User> pageResult = userService.getUsersPage(
                    searchTerm,
                    currentPage,
                    pageSize,
                    false
            );

            int totalPages = pageResult.getTotalPages();

            // Edge case: if requested page > total pages, redirect to last page
            if (currentPage > totalPages && totalPages > 0) {
                currentPage = totalPages;
                pageResult = userService.getUsersPage(searchTerm, currentPage, pageSize, false);
            }

            // Set attributes for JSP
            request.setAttribute(RequestAttributes.USER_LIST, pageResult.getItems());
            request.setAttribute(RequestAttributes.CURRENT_PAGE, currentPage);
            request.setAttribute(RequestAttributes.TOTAL_PAGES, totalPages);
            request.setAttribute(RequestAttributes.SEARCH_TERM, searchTerm);
            request.setAttribute(RequestAttributes.PAGE_SIZE, pageSize);

            // Forward to JSP
            RequestDispatcher dispatcher = request.getRequestDispatcher(Paths.ADMIN_USER_LIST);
            dispatcher.forward(request, response);

        } catch (Exception e) {
            // Any error loading user list
            log.error("{}: Error loading user list", context, e);
            ServletHelper.handleError(request, response,
                    new DatabaseException(context, e), log, context);
        }
    }
}