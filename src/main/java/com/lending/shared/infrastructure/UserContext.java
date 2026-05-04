package com.lending.shared.infrastructure;

import org.springframework.stereotype.Component;

/**
 * Holds the user identity for the current request, populated from the X-User-Id header.
 * Falls back to "system" if the header is not provided.
 * Uses ThreadLocal to work correctly in servlet filters (before DispatcherServlet).
 */
@Component
public class UserContext {

    private static final ThreadLocal<String> currentUser = ThreadLocal.withInitial(() -> "system");

    public String getUserId() {
        return currentUser.get();
    }

    public void setUserId(String userId) {
        currentUser.set(userId != null && !userId.isBlank() ? userId : "system");
    }

    public void clear() {
        currentUser.remove();
    }
}
