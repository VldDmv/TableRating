package org.criticizer.servlets;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.DatabaseException;
import org.criticizer.service.user.UserService;
import org.criticizer.servlets.admin.AdminChangeRoleServlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.criticizer.constants.AttribConstants.*;
import static org.mockito.Mockito.*;

class AdminChangeRoleServletTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private ServletContext servletContext;

    @Mock
    private ServletConfig servletConfig;

    @Mock
    private UserService userService;

    private AdminChangeRoleServlet servlet;

    private User initiator;

    @BeforeEach
    void setUp() throws ServletException {
        MockitoAnnotations.openMocks(this);
        servlet = new AdminChangeRoleServlet();

        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getAttribute(ServiceNames.USER_SERVICE)).thenReturn(userService);
        servlet.init(servletConfig);

        initiator = new User(1, "admin", "pass", Role.ADMIN, false);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionAttributes.USER)).thenReturn(initiator);
        when(request.getContextPath()).thenReturn("/app");
    }

    @Test
    void doPost_ValidRoleChange_ShouldChangeRoleAndRedirect() throws ServletException, IOException {
        when(request.getParameter(RequestParams.USER_ID)).thenReturn("2");
        when(request.getParameter(RequestParams.NEW_ROLE)).thenReturn("ADMIN");

        servlet.doPost(request, response);

        verify(userService).changeUserRole(2, Role.ADMIN, initiator);
        verify(session).setAttribute(eq(SessionAttributes.FLASH_SUCCESS), anyString());
        verify(response).sendRedirect("/app" + Paths.ADMIN_USERS_SERVLET);
    }

    @Test
    void doPost_NoInitiator_ShouldSetErrorAndRedirect() throws ServletException, IOException {
        when(session.getAttribute(SessionAttributes.USER)).thenReturn(null);
        when(request.getParameter(RequestParams.USER_ID)).thenReturn("2");
        when(request.getParameter(RequestParams.NEW_ROLE)).thenReturn("ADMIN");

        servlet.doPost(request, response);

        verify(userService, never()).changeUserRole(anyInt(), any(), any());
        verify(session).setAttribute(eq(SessionAttributes.FLASH_ERROR), anyString());
        verify(response).sendRedirect("/app" + Paths.ADMIN_USERS_SERVLET);
    }

    @Test
    void doPost_MissingUserId_ShouldSetErrorAndRedirect() throws ServletException, IOException {
        when(request.getParameter(RequestParams.USER_ID)).thenReturn(null);
        when(request.getParameter(RequestParams.NEW_ROLE)).thenReturn("ADMIN");

        servlet.doPost(request, response);

        verify(userService, never()).changeUserRole(anyInt(), any(), any());
        verify(session).setAttribute(eq(SessionAttributes.FLASH_ERROR), anyString());
        verify(response).sendRedirect("/app" + Paths.ADMIN_USERS_SERVLET);
    }

    @Test
    void doPost_InvalidUserId_ShouldSetErrorAndRedirect() throws ServletException, IOException {
        when(request.getParameter(RequestParams.USER_ID)).thenReturn("invalid");
        when(request.getParameter(RequestParams.NEW_ROLE)).thenReturn("ADMIN");

        servlet.doPost(request, response);

        verify(userService, never()).changeUserRole(anyInt(), any(), any());
        verify(session).setAttribute(eq(SessionAttributes.FLASH_ERROR), anyString());
        verify(response).sendRedirect("/app" + Paths.ADMIN_USERS_SERVLET);
    }

    @Test
    void doPost_InvalidRole_ShouldSetErrorAndRedirect() throws ServletException, IOException {
        when(request.getParameter(RequestParams.USER_ID)).thenReturn("2");
        when(request.getParameter(RequestParams.NEW_ROLE)).thenReturn("INVALID_ROLE");

        servlet.doPost(request, response);

        verify(userService, never()).changeUserRole(anyInt(), any(), any());
        verify(session).setAttribute(eq(SessionAttributes.FLASH_ERROR), anyString());
        verify(response).sendRedirect("/app" + Paths.ADMIN_USERS_SERVLET);
    }

    @Test
    void doPost_ServiceException_ShouldSetErrorAndRedirect() throws ServletException, IOException {
        when(request.getParameter(RequestParams.USER_ID)).thenReturn("2");
        when(request.getParameter(RequestParams.NEW_ROLE)).thenReturn("ADMIN");
        doThrow(new DatabaseException("Database error", new RuntimeException()))
                .when(userService).changeUserRole(2, Role.ADMIN, initiator);

        servlet.doPost(request, response);

        verify(userService).changeUserRole(2, Role.ADMIN, initiator);
        verify(session).setAttribute(eq(SessionAttributes.FLASH_ERROR), anyString());
        verify(response).sendRedirect("/app" + Paths.ADMIN_USERS_SERVLET);
    }
}