package com.yuai.promptgenerator.server.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yuai.promptgenerator.core.domain.AppUser;
import com.yuai.promptgenerator.server.api.AuthDtos.Credentials;
import com.yuai.promptgenerator.server.config.AppProperties;
import com.yuai.promptgenerator.server.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

class AuthControllerTest {
    @Test
    void productionCookieIsSecureHttpOnlyAndSameSiteProtected() {
        AuthService service = mock(AuthService.class);
        AppProperties properties = new AppProperties();
        properties.getJwt().setCookieSecure(true);
        AppUser user = new AppUser();
        user.setId("user-1");
        user.setUsername("alice");
        when(service.login("alice", "a-strong-password"))
                .thenReturn(new AuthService.TokenPair("access", "refresh", user));

        var response = new AuthController(service, properties).login(
                new Credentials("alice", "a-strong-password"), new MockHttpServletRequest());

        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("HttpOnly", "Secure", "SameSite=Lax", "Path=/api/v1/auth");
    }
}
