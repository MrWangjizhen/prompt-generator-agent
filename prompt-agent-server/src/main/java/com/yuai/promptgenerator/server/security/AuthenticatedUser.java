package com.yuai.promptgenerator.server.security;

import java.util.Set;

public record AuthenticatedUser(String id, String username, Set<String> roles) {
}
