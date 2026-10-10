package io.github.agentmemory;

import java.util.List;

/**
 * 长期记忆分页结果。
 */
public record LongTermMemoryPage(
        List<LongTermMemory> content,
        int page,
        int size,
        long totalElements
) {
}
