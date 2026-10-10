package com.yuai.promptgenerator.server.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuai.promptgenerator.core.domain.AppUser;
import com.yuai.promptgenerator.server.config.AppProperties;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void createsUniqueTokensAndRestoresPrincipal() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("test-secret-that-is-at-least-32-bytes-long");
        JwtService service = new JwtService(properties);
        AppUser user = new AppUser();
        user.setId("user-1");
        user.setUsername("tester");

        String first = service.createAccessToken(user);
        String second = service.createAccessToken(user);

        assertThat(second).isNotEqualTo(first);
        AuthenticatedUser principal = service.parse(first);
        assertThat(principal.id()).isEqualTo("user-1");
        assertThat(principal.username()).isEqualTo("tester");
        assertThat(principal.roles()).containsExactly("ROLE_USER");
    }
}
