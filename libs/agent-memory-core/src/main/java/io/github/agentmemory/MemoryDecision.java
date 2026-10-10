package io.github.agentmemory;

import java.util.List;

/**
 * 本轮记忆路由决策。
 */
public record MemoryDecision(
        boolean retrieveLongTermMemory,
        boolean updateUserProfile,
        boolean saveLongTermMemory,
        String conversationType,
        int importance,
        List<String> topics
) {

    public static MemoryDecision none() {
        return new MemoryDecision(false, false, false, "general", 1, List.of());
    }

    public MemoryDecision normalized() {
        String type = hasText(conversationType) ? conversationType.trim() : "general";
        int normalizedImportance = Math.max(1, Math.min(5, importance));
        List<String> normalizedTopics = topics == null ? List.of() : topics.stream()
                .filter(MemoryDecision::hasText)
                .map(String::trim)
                .distinct()
                .limit(5)
                .toList();
        return new MemoryDecision(retrieveLongTermMemory, updateUserProfile, saveLongTermMemory, type,
                normalizedImportance, normalizedTopics);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

