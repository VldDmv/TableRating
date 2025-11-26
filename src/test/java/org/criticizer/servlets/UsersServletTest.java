package org.criticizer.servlets;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.service.user.UserPageResult;
import org.criticizer.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;

import static org.criticizer.constants.AttribConstants.*;
import static org.mockito.Mockito.*;

public class UsersServletTest {
    @Mock
    private HttpSession session;
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
    @Mock
    private UserService userService;

    @InjectMocks
    private UsersServlet usersServlet;

    @BeforeEach
    void setUp() throws ServletException {
        MockitoAnnotations.openMocks(this);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getAttribute(ServiceNames.USER_SERVICE)).thenReturn(userService);
        usersServlet.init(servletConfig);
    }

    @Test
    void testDoGet_DefaultPage_ForwardsCorrectly() throws ServletException, IOException {
        UserPageResult<User> mockPageResult = new UserPageResult<>(new ArrayList<>(), 0, 1, Defaults.PAGE_SIZE_PUBLIC);
        when(userService.getUsersPage(null, Defaults.PAGE, Defaults.PAGE_SIZE_PUBLIC, true))
                .thenReturn(mockPageResult);
        when(request.getRequestDispatcher(Paths.USERS_JSP)).thenReturn(requestDispatcher);

        usersServlet.doGet(request, response);

        verify(request).setAttribute(RequestAttributes.USER_LIST, mockPageResult.getItems());
        verify(request).setAttribute(RequestAttributes.CURRENT_PAGE, 1);
        verify(request).setAttribute(RequestAttributes.TOTAL_PAGES, 0);
        verify(request).setAttribute(RequestAttributes.SEARCH_TERM, null);
        verify(requestDispatcher).forward(request, response);
    }

    @Test
    void testDoGet_WithPageAndSearchParams_ForwardsCorrectly() throws ServletException, IOException {
        when(request.getParameter(RequestParams.PAGE)).thenReturn("3");
        when(request.getParameter(RequestParams.SEARCH)).thenReturn("test");
        UserPageResult<User> mockPageResult = new UserPageResult<>(new ArrayList<>(), 50, 3, Defaults.PAGE_SIZE_PUBLIC);
        when(userService.getUsersPage("test", 3, Defaults.PAGE_SIZE_PUBLIC, true))
                .thenReturn(mockPageResult);
        when(request.getRequestDispatcher(Paths.USERS_JSP)).thenReturn(requestDispatcher);

        usersServlet.doGet(request, response);

        verify(request).setAttribute(RequestAttributes.CURRENT_PAGE, 3);
        verify(request).setAttribute(RequestAttributes.TOTAL_PAGES, 3);
        verify(request).setAttribute(RequestAttributes.SEARCH_TERM, "test");
        verify(requestDispatcher).forward(request, response);
    }

    @Test
    void testDoGet_ServiceThrowsException_HandlesError() throws ServletException, IOException {
        when(userService.getUsersPage(any(), anyInt(), anyInt(), anyBoolean()))
                .thenThrow(new DatabaseException("DB Error", new RuntimeException()));

        when(request.getSession()).thenReturn(session);

        when(request.getContextPath()).thenReturn("/app");

        usersServlet.doGet(request, response);

        verify(session).setAttribute(eq("flashErrorMessage"), anyString());

        verify(response).sendRedirect(eq("/app/jsp/dashboard.jsp"));

        verify(response, never()).sendError(anyInt(), anyString());

    }
}