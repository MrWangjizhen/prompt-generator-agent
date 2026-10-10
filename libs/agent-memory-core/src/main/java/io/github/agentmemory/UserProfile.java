package io.github.agentmemory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户画像。Starter 不绑定具体数据库，持久化由 UserProfileStore 适配。
 */
public class UserProfile {

    private String userId;

    private int schemaVersion;

    private List<MemoryFact> facts = new ArrayList<>();

    private List<MemoryFact> preferences = new ArrayList<>();

    private List<MemoryFact> goals = new ArrayList<>();

    private List<MemoryFact> constraints = new ArrayList<>();

    private boolean disabled;

    private Instant createdAt;

    private Instant updatedAt;

    public UserProfile() {
    }

    public UserProfile(String userId, int schemaVersion) {
        this.userId = userId;
        this.schemaVersion = schemaVersion;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static UserProfile empty(String userId) {
        return new UserProfile(userId, 1);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public List<MemoryFact> getFacts() {
        return facts;
    }

    public void setFacts(List<MemoryFact> facts) {
        this.facts = copy(facts);
    }

    public List<MemoryFact> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<MemoryFact> preferences) {
        this.preferences = copy(preferences);
    }

    public List<MemoryFact> getGoals() {
        return goals;
    }

    public void setGoals(List<MemoryFact> goals) {
        this.goals = copy(goals);
    }

    public List<MemoryFact> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<MemoryFact> constraints) {
        this.constraints = copy(constraints);
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    private static List<MemoryFact> copy(List<MemoryFact> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}

