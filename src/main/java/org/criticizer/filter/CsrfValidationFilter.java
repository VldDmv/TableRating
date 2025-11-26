package org.criticizer.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.Set;

import static org.criticizer.constants.AttribConstants.SessionAttributes;

/**
 * CSRF protection for all authenticated POST requests.
 * Excludes public endpoints (login, register, static resources).
 */
@WebFilter(urlPatterns = {"/*"},
        dispatcherTypes = {DispatcherType.REQUEST})
public class CsrfValidationFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(CsrfValidationFilter.class);
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth",           // Login/Register/Logout
            "/users",
            "/profile-data",
            "/css",
            "/js",
            "/images",
            "/favicon.ico"
    );

    private static final String CSRF_PARAM_NAME = "_csrf";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("CsrfValidationFilter initialized - protecting all POST requests");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod().toUpperCase();

        if (!method.equals("POST") && !method.equals("PUT") &&
                !method.equals("DELETE") && !method.equals("PATCH")) {
            chain.doFilter(request, response);
            return;
        }

        String path = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String relativePath = path.substring(contextPath.length());

        if (isPublicPath(relativePath)) {
            log.trace("CSRF check skipped for public path: {}", relativePath);
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        String sessionToken = (session != null)
                ? (String) session.getAttribute(SessionAttributes.CSRF_TOKEN)
                : null;

        String requestToken = httpRequest.getParameter(CSRF_PARAM_NAME);

        if (requestToken == null) {
            requestToken = httpRequest.getHeader("X-CSRF-Token");
        }

        if (constantTimeEquals(sessionToken, requestToken)) {
            log.trace("CSRF token valid for {}", relativePath);
            chain.doFilter(request, response);
        } else {
            log.warn("CSRF VALIDATION FAILED for {} {} (session={}, request={})",
                    method, relativePath,
                    sessionToken != null ? "present" : "null",
                    requestToken != null ? "present" : "null");

            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Invalid CSRF Token detected.");
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return Objects.equals(a, b);
        }

        if (a.length() != b.length()) {
            return false;
        }

        try {
            return MessageDigest.isEqual(
                    a.getBytes(StandardCharsets.UTF_8),
                    b.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error comparing CSRF tokens", e);
            return false;
        }
    }

    private boolean isPublicPath(String relativePath) {
        for (String publicPath : PUBLIC_PATHS) {
            if (relativePath.equals(publicPath) || relativePath.startsWith(publicPath + "/")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        log.info("CsrfValidationFilter destroyed");
    }
}