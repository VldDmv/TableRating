package org.criticizer.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.criticizer.constants.AttribConstants.SessionAttributes;

/**
 * Servlet filter that restricts access to "/admin/*" URLs to users with the ADMIN role.
 * Returns a 403 Forbidden error for unauthorized users.
 */
@WebFilter("/admin/*")
public class AdminAuthorizationFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthorizationFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("AdminAuthorizationFilter initialized");
    }

    /**
     * Checks if the user has the ADMIN role.
     * If authorized, proceeds with the request; otherwise, returns a 403 Forbidden error.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        User user = null;
        boolean isAdmin = false;

        if (session != null) {
            user = (User) session.getAttribute(SessionAttributes.USER);
            if (user != null && user.getRole() == Role.ADMIN) {
                isAdmin = true;
            }
        }

        String requestURI = httpRequest.getRequestURI();

        // Access is only granted if the user is explicitly identified as an ADMIN
        if (isAdmin) {
            log.debug("Admin access GRANTED for user '{}' to URI: {}", user.getName(), requestURI);
            chain.doFilter(request, response);
        } else {
            log.warn("Admin access DENIED for user '{}' (or anonymous) to URI: {}",
                    (user != null ? user.getName() + "[" + user.getRole() + "]" : "anonymous"),
                    requestURI);
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "You do not have permission to access this resource.");
        }
    }

    @Override
    public void destroy() {
        log.info("AdminAuthorizationFilter destroyed");
    }
}
