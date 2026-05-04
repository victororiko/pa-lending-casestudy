package com.lending.shared.infrastructure;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Holds the user identity for the current request, populated from the X-User-Id header.
 * Falls back to "system" if the header is not provided.
 */
@Component
@RequestScope
public class UserContext {

    private String userId = "system";

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId != null && !userId.isBlank() ? userId : "system";
    }
}
