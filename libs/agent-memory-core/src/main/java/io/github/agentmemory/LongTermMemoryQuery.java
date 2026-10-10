package io.github.agentmemory;

import java.time.Instant;
import java.util.List;

/**
 * 长期记忆管理查询条件。
 */
public record LongTermMemoryQuery(
        String userId,
        int page,
        int size,
        Instant createdFrom,
        Instant createdTo,
        String conversationType,
        Integer minImportance,
        List<String> topics
) {

    private static final int DEFAULT_SIZE = 20;

    private static final int MAX_SIZE = 200;

    public static LongTermMemoryQuery forUser(String userId) {
        return new LongTermMemoryQuery(userId, 0, DEFAULT_SIZE, null, null, null, null, List.of()).normalized();
    }

    public LongTermMemoryQuery normalized() {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        int normalizedPage = Math.max(0, page);
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String normalizedType = conversationType == null || conversationType.isBlank() ? null : conversationType.trim();
        Integer normalizedImportance = minImportance == null ? null : Math.max(1, Math.min(5, minImportance));
        List<String> normalizedTopics = topics == null ? List.of() : topics.stream()
                .filter(topic -> topic != null && !topic.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        return new LongTermMemoryQuery(userId.trim(), normalizedPage, normalizedSize, createdFrom, createdTo,
                normalizedType, normalizedImportance, normalizedTopics);
    }
}
