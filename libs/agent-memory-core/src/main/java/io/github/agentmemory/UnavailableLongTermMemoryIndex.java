package io.github.agentmemory;

import java.util.Optional;

/**
 * 显式表示未配置长期记忆索引。
 */
public class UnavailableLongTermMemoryIndex implements LongTermMemoryIndex {

    private static final String MESSAGE = "Long-term memory metadata index is not available. "
            + "Configure agent.memory.long-term.index-store or provide a LongTermMemoryIndex bean.";

    @Override
    public void save(LongTermMemory memory) {
    }

    @Override
    public LongTermMemoryPage search(LongTermMemoryQuery query) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public Optional<LongTermMemory> findById(String userId, String memoryId) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public void deleteById(String userId, String memoryId) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public void delete(LongTermMemoryQuery query) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public void deleteByUserId(String userId) {
    }
}
