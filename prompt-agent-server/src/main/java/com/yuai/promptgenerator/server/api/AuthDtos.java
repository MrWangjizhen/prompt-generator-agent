package com.yuai.promptgenerator.server.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record Credentials(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{3,32}") String username,
            @NotBlank @Size(min = 10, max = 128) String password) {
    }

    public record AuthResponse(String accessToken, long expiresInSeconds, UserView user) {
    }

    public record UserView(String id, String username, java.util.Set<String> roles, boolean memoryConsent) {
    }
}
