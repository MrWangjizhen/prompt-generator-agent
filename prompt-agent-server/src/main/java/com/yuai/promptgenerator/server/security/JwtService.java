package com.yuai.promptgenerator.server.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.yuai.promptgenerator.core.domain.AppUser;
import com.yuai.promptgenerator.server.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final AppProperties.Jwt properties;
    private final SecretKey key;

    public JwtService(AppProperties properties) {
        this.properties = properties.getJwt();
        String secret = this.properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.getIssuer())
                .subject(user.getId())
                .claim("username", user.getUsername())
                .claim("roles", user.getRoles())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAccessTtl())))
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer(properties.getIssuer()).build()
                .parseSignedClaims(token).getPayload();
        Object rolesClaim = claims.get("roles");
        Set<String> roles = new LinkedHashSet<>();
        if (rolesClaim instanceof List<?> list) list.forEach(role -> roles.add(role.toString()));
        return new AuthenticatedUser(claims.getSubject(), claims.get("username", String.class), Set.copyOf(roles));
    }
}
