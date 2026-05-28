package org.criticizer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RateLimitFilter Tests")
class RateLimitFilterTest {

    private static final int CAPACITY = 10;

    private RateLimitFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
    }

    private void post(String path) {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn(path);
    }

    @Test
    @DisplayName("Non-protected path passes through without consuming the bucket")
    void nonProtectedPasses() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/games");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(contains("ratelimit"));
    }

    @Test
    @DisplayName("Form login within the limit passes through")
    void formLoginWithinLimitPasses() throws Exception {
        post("/login");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Form login over the limit redirects to /index with a ratelimit flag")
    void formLoginOverLimitRedirects() throws Exception {
        post("/login");
        when(request.getContextPath()).thenReturn("");

        for (int i = 0; i < CAPACITY; i++) {
            filter.doFilterInternal(request, response, chain);
        }
        // One more request exceeds the bucket.
        filter.doFilterInternal(request, response, chain);

        verify(chain, times(CAPACITY)).doFilter(request, response);
        verify(response).sendRedirect("/index?ratelimit=true");
    }

    @Test
    @DisplayName("API login over the limit returns 429 JSON")
    void apiLoginOverLimitReturns429() throws Exception {
        post("/api/auth/login");
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        for (int i = 0; i < CAPACITY; i++) {
            filter.doFilterInternal(request, response, chain);
        }
        filter.doFilterInternal(request, response, chain);

        verify(chain, times(CAPACITY)).doFilter(request, response);
        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "60");
        assertThat(body.toString()).contains("Too many requests");
    }
}
