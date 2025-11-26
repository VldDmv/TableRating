package org.criticizer.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import static org.criticizer.constants.AttribConstants.Paths;
import static org.criticizer.constants.AttribConstants.SessionAttributes;

/**
 * Servlet filter that enforces authentication for requests to protected URL patterns.
 * Redirects unauthenticated users to the login page.
 */
@WebFilter("/*")
public class AuthenticationFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/",
            "/auth",
            "/users",
            "/profile",
            "/profile-data",
            "/css",
            "/js",
            "/images",
            "/favicon.ico",
            "/error"
    );

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/css/",
            "/js/",
            "/images/"
    );
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("AuthenticationFilter initialized");
    }

    /**
     * Checks if the user is authenticated by verifying the presence of a user in the session.
     * If authenticated, proceeds with the request; otherwise, redirects to the login page.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String contextPath = httpRequest.getContextPath();
        String requestURI = httpRequest.getRequestURI();
        String relativePath = requestURI.substring(contextPath.length());

        if (isPublicPath(relativePath)) {
            log.trace("Public path accessed: {}", relativePath);
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        boolean isLoggedIn = (session != null &&
                session.getAttribute(SessionAttributes.USER) != null);

        if (isLoggedIn) {
            log.trace("Authenticated access to: {}", relativePath);
            chain.doFilter(request, response);
        } else {
            log.warn("Unauthenticated access attempt to protected resource: {}",
                    relativePath);

            String loginPage = contextPath + Paths.INDEX_JSP;
            httpResponse.sendRedirect(loginPage);
        }
    }

    /**
     * Checking, if path is public.
     */
    private boolean isPublicPath(String relativePath) {
        if (PUBLIC_PATHS.contains(relativePath)) {
            return true;
        }

        for (String prefix : PUBLIC_PREFIXES) {
            if (relativePath.startsWith(prefix)) {
                return true;
            }
        }

        return relativePath.endsWith(".jsp") &&
                (relativePath.equals("/index.jsp") || relativePath.equals("/error.jsp"));
    }

    @Override
    public void destroy() {
        log.info("AuthenticationFilter destroyed");
    }
}
