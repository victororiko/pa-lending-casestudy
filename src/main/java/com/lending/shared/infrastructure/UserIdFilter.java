package com.lending.shared.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Extracts the X-User-Id header from incoming requests and populates the UserContext.
 * Per ADR-0008, authentication is stubbed — all endpoints are open,
 * and created_by is populated from this header.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserIdFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final UserContext userContext;

    public UserIdFilter(UserContext userContext) {
        this.userContext = userContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String userId = request.getHeader(USER_ID_HEADER);
            userContext.setUserId(userId);
            filterChain.doFilter(request, response);
        } finally {
            userContext.clear();
        }
    }
}
