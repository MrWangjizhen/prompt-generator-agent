package com.yuai.promptgenerator.server.config;

import java.util.LinkedHashSet;
import java.util.Set;

import com.yuai.promptgenerator.core.domain.AppUser;
import com.yuai.promptgenerator.core.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;

    public AdminBootstrap(AppUserRepository users, PasswordEncoder encoder,
            @Value("${BOOTSTRAP_ADMIN_USERNAME:}") String username,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username == null || username.isBlank() || password == null || password.length() < 10) return;
        users.findByUsername(username.trim().toLowerCase()).orElseGet(() -> {
            AppUser admin = new AppUser();
            admin.setUsername(username.trim().toLowerCase());
            admin.setPasswordHash(encoder.encode(password));
            admin.setRoles(new LinkedHashSet<>(Set.of("ROLE_USER", "ROLE_ADMIN")));
            return users.save(admin);
        });
    }
}
