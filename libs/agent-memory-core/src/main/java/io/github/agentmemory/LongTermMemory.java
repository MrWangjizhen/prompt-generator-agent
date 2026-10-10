package io.github.agentmemory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 长期记忆存储对象，所有实现都必须按 userId 隔离。
 */
public record LongTermMemory(
        String id,
        String userId,
        String chatId,
        String content,
        Instant createdAt,
        String conversationType,
        int importance,
        List<String> topics,
        int schemaVersion
) {

    public static LongTermMemory from(String userId, String chatId, ExtractedLongTermMemory memory, int schemaVersion) {
        return new LongTermMemory(UUID.randomUUID().toString(), userId, chatId, memory.content().trim(), Instant.now(),
                defaultText(memory.conversationType(), "general"), Math.max(1, Math.min(5, memory.importance())),
                memory.topics() == null ? List.of() : memory.topics(), schemaVersion);
    }

    private static String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}

