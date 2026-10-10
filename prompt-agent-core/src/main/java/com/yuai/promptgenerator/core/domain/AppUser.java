package com.yuai.promptgenerator.core.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("app_users")
public class AppUser {
    @Id
    private String id;
    @Indexed(unique = true)
    private String username;
    private String passwordHash;
    private Set<String> roles = new LinkedHashSet<>(Set.of("ROLE_USER"));
    private boolean enabled = true;
    private boolean memoryConsent;
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isMemoryConsent() { return memoryConsent; }
    public void setMemoryConsent(boolean memoryConsent) { this.memoryConsent = memoryConsent; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
