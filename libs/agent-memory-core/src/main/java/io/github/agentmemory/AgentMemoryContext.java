package io.github.agentmemory;

import java.util.List;

/**
 * 单次对话请求对应的记忆上下文，供 Advisor 的 before/after 阶段共享。
 */
public record AgentMemoryContext(
        String userId,
        String chatId,
        String userMessage,
        boolean consentGranted,
        UserProfile userProfile,
        MemoryDecision decision,
        List<LongTermMemory> longTermMemories,
        String promptContext
) {
}

