package com.yuai.promptgenerator.core.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("prompt_sessions")
@CompoundIndex(name = "owner_updated_idx", def = "{'userId': 1, 'updatedAt': -1}")
public class PromptSession {

    @Id
    private String id;
    private String userId;
    private TaskType taskType;
    private SessionStatus status = SessionStatus.DISCOVERING;
    private RequirementProfile requirements = new RequirementProfile();
    private List<ConversationTurn> turns = new ArrayList<>();
    private List<PromptCandidate> candidates = new ArrayList<>();
    private String selectedCandidateId;
    private boolean memoryConsent;
    private int discoveryTurns;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    @Version
    private Long version;

    public PromptSession() {
    }

    public PromptSession(String userId, TaskType taskType, boolean memoryConsent) {
        this.userId = userId;
        this.taskType = taskType;
        this.memoryConsent = memoryConsent;
    }

    public void addTurn(ConversationTurn turn) {
        turns.add(turn);
        updatedAt = Instant.now();
    }

    public void touch() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public RequirementProfile getRequirements() { return requirements; }
    public void setRequirements(RequirementProfile requirements) { this.requirements = requirements; }
    public List<ConversationTurn> getTurns() { return turns; }
    public void setTurns(List<ConversationTurn> turns) { this.turns = turns == null ? new ArrayList<>() : turns; }
    public List<PromptCandidate> getCandidates() { return candidates; }
    public void setCandidates(List<PromptCandidate> candidates) { this.candidates = candidates == null ? new ArrayList<>() : candidates; }
    public String getSelectedCandidateId() { return selectedCandidateId; }
    public void setSelectedCandidateId(String selectedCandidateId) { this.selectedCandidateId = selectedCandidateId; }
    public boolean isMemoryConsent() { return memoryConsent; }
    public void setMemoryConsent(boolean memoryConsent) { this.memoryConsent = memoryConsent; }
    public int getDiscoveryTurns() { return discoveryTurns; }
    public void setDiscoveryTurns(int discoveryTurns) { this.discoveryTurns = discoveryTurns; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
