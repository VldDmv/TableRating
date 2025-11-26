package org.criticizer.servlets;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.criticizer.entity.AdminStats;
import org.criticizer.servlets.admin.AdminDashboardServlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.criticizer.service.dashboard.DashboardService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServletTest {

    @Mock
    private DashboardService dashboardServiceMock;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ServletConfig servletConfig;
    @Mock
    private ServletContext servletContext;
    @Mock
    private RequestDispatcher requestDispatcher;

    @InjectMocks
    private AdminDashboardServlet adminDashboardServlet;

    @BeforeEach
    void setUp() throws Exception {
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getAttribute("dashboardService")).thenReturn(dashboardServiceMock);
        adminDashboardServlet.init(servletConfig);
    }

    @Test
    @DisplayName("doGet should fetch stats and forward to admin dashboard JSP")
    void doGet_shouldFetchStatsAndForward() throws Exception {
        AdminStats mockStats = new AdminStats();
        when(dashboardServiceMock.getAdminDashboardStats()).thenReturn(mockStats);
        when(request.getRequestDispatcher("/WEB-INF/admin/adminDashboard.jsp")).thenReturn(requestDispatcher);

        adminDashboardServlet.doGet(request, response);

        verify(dashboardServiceMock, times(1)).getAdminDashboardStats();

        verify(request).setAttribute("stats", mockStats);

        verify(requestDispatcher).forward(request, response);
    }
}