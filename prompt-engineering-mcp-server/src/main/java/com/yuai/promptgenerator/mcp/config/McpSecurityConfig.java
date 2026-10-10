package com.yuai.promptgenerator.mcp.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@Profile("!stdio")
public class McpSecurityConfig {
    @Bean
    SecurityFilterChain mcpSecurity(HttpSecurity http,
            @Value("${MCP_SERVER_TOKEN:}") String expectedToken) throws Exception {
        OncePerRequestFilter tokenFilter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                if (expectedToken == null || expectedToken.isBlank() || request.getRequestURI().startsWith("/actuator/health")) {
                    chain.doFilter(request, response);
                    return;
                }
                String actual = request.getHeader("X-MCP-Token");
                if (actual != null && MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),
                        expectedToken.getBytes(StandardCharsets.UTF_8))) {
                    chain.doFilter(request, response);
                    return;
                }
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        };
        return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class).build();
    }
}
