package org.criticizer.servlets;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.criticizer.entity.User;
import org.criticizer.exceptions.ApplicationException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.exceptions.validation.PasswordMismatchException;
import org.criticizer.service.user.UserService;
import org.criticizer.servlets.items.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.criticizer.constants.AttribConstants.*;

/**
 * Servlet for handling user authentication (login, logout, registration).
 */
@WebServlet("/auth")
public class AuthServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(AuthServlet.class);
    private UserService userService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);


        this.userService = ServletHelper.getService(
                config.getServletContext(),
                ServiceNames.USER_SERVICE,
                UserService.class
        );
        log.info("AuthServlet initialized successfully");
    }

    /**
     * Handles POST requests for login, logout, or registration actions
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        try {

            String action = ServletHelper.getRequiredParameter(request, RequestParams.ACTION);

            switch (action) {
                case Actions.LOGIN -> handleLogin(request, response);
                case Actions.LOGOUT -> handleLogout(request, response);
                case Actions.REGISTER -> handleRegister(request, response);
                default -> {
                    log.warn("Unknown action '{}' received", action);
                    throw new InvalidInputException("action", "unknown action type");
                }
            }

        } catch (ApplicationException e) {

            log.info("Authentication error: {}", e.getMessage());
            ServletHelper.setFlashError(request.getSession(), e.getUserMessage());
            ServletHelper.redirectTo(response, request, Paths.INDEX_JSP);

        } catch (Exception e) {

            log.error("Unexpected error in authentication", e);
            ServletHelper.handleError(request, response, e, log, "Authentication");
        }
    }

    /**
     * Handles user login by validating credentials and setting session attributes
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String name = ServletHelper.getRequiredParameter(request, RequestParams.NAME);
        String password = ServletHelper.getRequiredParameter(request, RequestParams.PASSWORD);

        User user = userService.authenticate(name, password);

        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
            log.debug("Old session invalidated for security (Session Fixation prevention)");
        }

        HttpSession newSession = request.getSession(true);

        try {
            request.changeSessionId();
            log.debug("Session ID changed after authentication");
        } catch (IllegalStateException e) {
            log.warn("Failed to change session ID (Servlet < 3.1?): {}", e.getMessage());
        }

        newSession.setMaxInactiveInterval(30 * 60);

        if (request.isSecure()) {
            newSession.setAttribute("__secure__", true);
        }

        ServletHelper.setAuthenticatedUser(newSession, user);
        newSession.setAttribute(SessionAttributes.CSRF_TOKEN, generateSecureCsrfToken());

        ServletHelper.redirectTo(response, request, Paths.DASHBOARD_JSP);
    }


    private String generateSecureCsrfToken() {
        try {
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        } catch (NoSuchAlgorithmException e) {
            log.warn("SecureRandom not available, falling back to UUID", e);
            return UUID.randomUUID().toString();
        }
    }
    /**
     * Handles user logout by invalidating the session
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {
            User user = (User) session.getAttribute(SessionAttributes.USER);
            if (user != null) {
                log.info("User '{}' logged out", user.getName());
            }
            session.invalidate();
        }

        ServletHelper.redirectTo(response, request, Paths.INDEX_JSP);
    }

    /**
     * Handles user registration by creating a new user account
     */
    private void handleRegister(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String name = ServletHelper.getRequiredParameter(request, RequestParams.NAME);
        String password = ServletHelper.getRequiredParameter(request, RequestParams.PASSWORD);
        String confirmPassword = ServletHelper.getRequiredParameter(request, RequestParams.CONFIRM_PASSWORD);

        if (!password.equals(confirmPassword)) {
            throw new PasswordMismatchException();
        }


        userService.registerUser(name, password);

        log.info("New user '{}' registered successfully", name);

        ServletHelper.setFlashSuccess(request.getSession(),
                "Registration successful! Please log in.");

        ServletHelper.redirectTo(response, request, Paths.INDEX_JSP);
    }
}