package org.criticizer.servlets.admin;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.criticizer.entity.AdminStats;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.service.dashboard.DashboardService;
import org.criticizer.servlets.items.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.criticizer.constants.AttribConstants.*;

/**
 * Servlet for displaying the admin dashboard with statistics.
 */
@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardServlet.class);
    private DashboardService dashboardService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.dashboardService = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.DASHBOARD_SERVICE,
                DashboardService.class
        );

        log.info("AdminDashboardServlet initialized successfully");
    }

    /**
     * Handles GET requests to display the admin dashboard with statistics.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String context = "Admin Dashboard GET";

        try {
            // Get admin statistics
            AdminStats stats = dashboardService.getAdminDashboardStats();

            // Set attribute for JSP
            request.setAttribute(RequestAttributes.STATS, stats);

            // Forward to admin dashboard page
            RequestDispatcher dispatcher = request.getRequestDispatcher(Paths.ADMIN_DASHBOARD);
            dispatcher.forward(request, response);

        } catch (Exception e) {
            // Any error loading dashboard data
            log.error("{}: Error loading admin dashboard", context, e);
            ServletHelper.handleError(request, response,
                    new DatabaseException(context, e), log, context);
        }
    }
}
