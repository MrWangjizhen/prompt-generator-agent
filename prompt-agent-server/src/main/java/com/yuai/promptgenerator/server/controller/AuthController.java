package com.yuai.promptgenerator.server.controller;

import com.yuai.promptgenerator.server.api.AuthDtos.AuthResponse;
import com.yuai.promptgenerator.server.api.AuthDtos.Credentials;
import com.yuai.promptgenerator.server.api.AuthDtos.UserView;
import com.yuai.promptgenerator.server.config.AppProperties;
import com.yuai.promptgenerator.server.security.CurrentUser;
import com.yuai.promptgenerator.server.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "prompt_refresh";
    private final AuthService authService;
    private final AppProperties.Jwt jwtProperties;

    public AuthController(AuthService authService, AppProperties properties) {
        this.authService = authService;
        this.jwtProperties = properties.getJwt();
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody Credentials request, HttpServletRequest servletRequest) {
        return response(authService.register(request.username(), request.password()), secure(servletRequest));
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody Credentials request, HttpServletRequest servletRequest) {
        return response(authService.login(request.username(), request.password()), secure(servletRequest));
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_COOKIE) String token,
            HttpServletRequest servletRequest) {
        return response(authService.refresh(token), secure(servletRequest));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String token,
            HttpServletRequest servletRequest) {
        authService.logout(token);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie(secure(servletRequest)).toString()).build();
    }

    @GetMapping("/me")
    UserView me(Authentication authentication) {
        var principal = CurrentUser.require(authentication);
        var user = authService.requireUser(principal.id());
        return new UserView(user.getId(), user.getUsername(), user.getRoles(), user.isMemoryConsent());
    }

    private ResponseEntity<AuthResponse> response(AuthService.TokenPair pair, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, pair.refreshToken())
                .httpOnly(true).secure(secure).sameSite("Lax").path("/api/v1/auth")
                .maxAge(jwtProperties.getRefreshTtl()).build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(authService.response(pair.user(), pair.accessToken()));
    }

    private ResponseCookie clearCookie(boolean secure) {
        return ResponseCookie.from(REFRESH_COOKIE, "").httpOnly(true).secure(secure).sameSite("Lax")
                .path("/api/v1/auth").maxAge(0).build();
    }

    private boolean secure(HttpServletRequest request) {
        return jwtProperties.isCookieSecure() || request.isSecure();
    }
}
