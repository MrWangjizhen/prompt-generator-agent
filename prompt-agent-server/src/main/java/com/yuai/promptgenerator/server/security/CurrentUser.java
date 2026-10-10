package com.yuai.promptgenerator.server.security;

import org.springframework.security.core.Authentication;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static AuthenticatedUser require(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return user;
    }
}
