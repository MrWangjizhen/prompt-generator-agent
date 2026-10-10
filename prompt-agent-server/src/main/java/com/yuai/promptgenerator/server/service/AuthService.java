package com.yuai.promptgenerator.server.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;

import com.yuai.promptgenerator.core.domain.AppUser;
import com.yuai.promptgenerator.core.domain.RefreshToken;
import com.yuai.promptgenerator.core.repository.AppUserRepository;
import com.yuai.promptgenerator.core.repository.RefreshTokenRepository;
import com.yuai.promptgenerator.server.api.AuthDtos.AuthResponse;
import com.yuai.promptgenerator.server.api.AuthDtos.UserView;
import com.yuai.promptgenerator.server.config.AppProperties;
import com.yuai.promptgenerator.server.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties properties;
    private final SecureRandom random = new SecureRandom();

    public AuthService(AppUserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder,
            JwtService jwtService, AppProperties properties) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    public TokenPair register(String username, String password) {
        String normalized = username.trim().toLowerCase();
        if (users.existsByUsername(normalized)) throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        AppUser user = new AppUser();
        user.setUsername(normalized);
        user.setPasswordHash(passwordEncoder.encode(password));
        return issue(users.save(user));
    }

    public TokenPair login(String username, String password) {
        AppUser user = users.findByUsername(username.trim().toLowerCase())
                .filter(AppUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return issue(user);
    }

    public TokenPair refresh(String rawToken) {
        RefreshToken token = refreshTokens.findByTokenHashAndRevokedFalse(hash(rawToken))
                .filter(value -> value.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "刷新令牌无效"));
        token.setRevoked(true);
        refreshTokens.save(token);
        AppUser user = users.findById(token.getUserId())
                .filter(AppUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不可用"));
        return issue(user);
    }

    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokens.findByTokenHashAndRevokedFalse(hash(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokens.save(token);
        });
    }

    public AppUser requireUser(String userId) {
        return users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    public AuthResponse response(AppUser user, String accessToken) {
        return new AuthResponse(accessToken, properties.getJwt().getAccessTtl().toSeconds(),
                new UserView(user.getId(), user.getUsername(), Set.copyOf(user.getRoles()), user.isMemoryConsent()));
    }

    private TokenPair issue(AppUser user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(rawRefresh));
        token.setExpiresAt(Instant.now().plus(properties.getJwt().getRefreshTtl()));
        refreshTokens.save(token);
        return new TokenPair(jwtService.createAccessToken(user), rawRefresh, user);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record TokenPair(String accessToken, String refreshToken, AppUser user) {
    }
}
