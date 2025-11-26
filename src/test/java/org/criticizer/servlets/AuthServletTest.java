package org.criticizer.servlets;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.criticizer.entity.User;
import org.criticizer.exceptions.security.InvalidCredentialsException;
import org.criticizer.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;



@ExtendWith(MockitoExtension.class)
class AuthServletTest {

    @Mock
    private UserService userServiceMock;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession oldSession;
    @Mock
    private HttpSession newSession;
    @Mock
    private ServletConfig servletConfig;
    @Mock
    private ServletContext servletContext;

    @InjectMocks
    private AuthServlet authServlet;

    @BeforeEach
    void setUp() throws Exception {
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getAttribute("userService")).thenReturn(userServiceMock);
        authServlet.init(servletConfig);
    }

    @Test
    @DisplayName("handleLogin should succeed with valid credentials")
    void handleLogin_withValidCredentials_shouldRedirectToDashboard() throws Exception {
        String name = "testuser";
        String password = "password123";
        User user = new User(name, "hashed_password");

        when(request.getParameter("action")).thenReturn("login");
        when(request.getParameter("name")).thenReturn(name);
        when(request.getParameter("password")).thenReturn(password);
        when(userServiceMock.authenticate(name, password)).thenReturn(user);
        when(request.getContextPath()).thenReturn("/app");

        when(request.getSession(false)).thenReturn(oldSession);
        when(request.getSession(true)).thenReturn(newSession);

        doNothing().when(newSession).setAttribute(anyString(), any());
        doNothing().when(newSession).setMaxInactiveInterval(anyInt());

        authServlet.doPost(request, response);

        verify(oldSession, times(1)).invalidate();

        verify(request, times(1)).getSession(true);

        verify(newSession, times(1)).setAttribute(eq("user"), any(User.class));
        verify(newSession, times(1)).setAttribute(eq("_csrfToken"), anyString());
        verify(newSession, times(1)).setMaxInactiveInterval(30 * 60);

        verify(response).sendRedirect("/app/jsp/dashboard.jsp");
    }

    @Test
    @DisplayName("handleLogin should succeed even without old session")
    void handleLogin_withoutOldSession_shouldCreateNewSession() throws Exception {
        String name = "testuser";
        String password = "password123";
        User user = new User(name, "hashed_password");

        when(request.getParameter("action")).thenReturn("login");
        when(request.getParameter("name")).thenReturn(name);
        when(request.getParameter("password")).thenReturn(password);
        when(userServiceMock.authenticate(name, password)).thenReturn(user);
        when(request.getContextPath()).thenReturn("/app");

        when(request.getSession(false)).thenReturn(null);
        when(request.getSession(true)).thenReturn(newSession);

        doNothing().when(newSession).setAttribute(anyString(), any());
        doNothing().when(newSession).setMaxInactiveInterval(anyInt());

        authServlet.doPost(request, response);


        verify(oldSession, never()).invalidate();


        verify(request, times(1)).getSession(true);
        verify(newSession, times(1)).setAttribute(eq("user"), any(User.class));
        verify(newSession, times(1)).setAttribute(eq("_csrfToken"), anyString());

        verify(response).sendRedirect("/app/jsp/dashboard.jsp");
    }

    @Test
    @DisplayName("handleLogin should fail with invalid credentials")
    void handleLogin_withInvalidCredentials_shouldRedirectToIndex() throws Exception {
        when(request.getParameter("action")).thenReturn("login");
        when(request.getParameter("name")).thenReturn("wronguser");
        when(request.getParameter("password")).thenReturn("wrongpass");
        when(userServiceMock.authenticate("wronguser", "wrongpass"))
                .thenThrow(new InvalidCredentialsException("Invalid username or password."));

        when(request.getSession()).thenReturn(newSession);
        when(request.getContextPath()).thenReturn("/app");

        authServlet.doPost(request, response);

        verify(newSession).setAttribute("flashErrorMessage", "Invalid username or password");
        verify(response).sendRedirect("/app/jsp/index.jsp");

        verify(oldSession, never()).invalidate();
    }

    @Test
    @DisplayName("handleRegister should succeed for a new user")
    void handleRegister_withNewUser_shouldRedirectToIndexWithSuccessMessage() throws Exception {
        String name = "newuser";
        String password = "password123";

        when(request.getParameter("action")).thenReturn("register");
        when(request.getParameter("name")).thenReturn(name);
        when(request.getParameter("password")).thenReturn(password);
        when(request.getParameter("confirmPassword")).thenReturn(password);
        when(request.getContextPath()).thenReturn("/app");
        when(request.getSession()).thenReturn(newSession);

        authServlet.doPost(request, response);

        verify(userServiceMock).registerUser(name, password);
        verify(newSession).setAttribute("flashSuccessMessage", "Registration successful! Please log in.");
        verify(response).sendRedirect("/app/jsp/index.jsp");
    }

    @Test
    @DisplayName("handleRegister should fail if passwords do not match")
    void handleRegister_withMismatchedPasswords_shouldRedirectToIndexWithErrorMessage() throws Exception {
        when(request.getParameter("action")).thenReturn("register");
        when(request.getParameter("name")).thenReturn("someuser");
        when(request.getParameter("password")).thenReturn("password123");
        when(request.getParameter("confirmPassword")).thenReturn("password456");
        when(request.getContextPath()).thenReturn("/app");
        when(request.getSession()).thenReturn(newSession);

        authServlet.doPost(request, response);

        verify(userServiceMock, never()).registerUser(anyString(), anyString());
        verify(newSession).setAttribute("flashErrorMessage", "Passwords do not match");
        verify(response).sendRedirect("/app/jsp/index.jsp");
    }

    @Test
    @DisplayName("handleLogout should invalidate session and redirect")
    void handleLogout_shouldInvalidateSessionAndRedirect() throws Exception {
        when(request.getParameter("action")).thenReturn("logout");
        when(request.getContextPath()).thenReturn("/app");
        when(request.getSession(false)).thenReturn(oldSession);

        authServlet.doPost(request, response);

        verify(oldSession).invalidate();
        verify(response).sendRedirect("/app/jsp/index.jsp");
    }
}