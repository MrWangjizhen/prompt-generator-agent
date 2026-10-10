package com.yuai.promptgenerator.server.api;

import java.time.Instant;
import java.util.List;

import com.yuai.promptgenerator.core.domain.ConversationTurn;
import com.yuai.promptgenerator.core.domain.PromptCandidate;
import com.yuai.promptgenerator.core.domain.RequirementProfile;
import com.yuai.promptgenerator.core.domain.SessionStatus;
import com.yuai.promptgenerator.core.domain.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public final class PromptDtos {
    private PromptDtos() {
    }

    public record CreateSessionRequest(@NotNull TaskType taskType, Boolean memoryConsent) {
    }

    public record TurnRequest(@NotBlank String message, @PositiveOrZero Long expectedVersion) {
    }

    public record ConfirmRequest(@PositiveOrZero Long expectedVersion) {
    }

    public record SelectCandidateRequest(@NotBlank String candidateId, @PositiveOrZero Long expectedVersion) {
    }

    public record SessionSummaryView(String id, TaskType taskType, SessionStatus status, String selectedCandidateId,
                                     Instant createdAt, Instant updatedAt, Long version) {
    }

    public record SessionDetailView(String id, TaskType taskType, SessionStatus status,
                                    RequirementProfile requirements, List<ConversationTurn> turns,
                                    List<PromptCandidate> candidates, String selectedCandidateId,
                                    boolean memoryConsent, int discoveryTurns, Instant createdAt,
                                    Instant updatedAt, Long version) {
    }

    public record SseEvent(String type, Object data) {
    }

    public record RequirementSnapshot(RequirementProfile requirements, List<String> missingFields,
                                      boolean awaitingConfirmation, boolean assumptionsRequired, Long version) {
    }
}
