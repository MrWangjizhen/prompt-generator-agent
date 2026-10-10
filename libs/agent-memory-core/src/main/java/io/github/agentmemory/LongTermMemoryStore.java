package io.github.agentmemory;

import java.util.List;

/**
 * 长期记忆存储接口，隐藏向量库细节。
 */
public interface LongTermMemoryStore {

    List<LongTermMemory> search(String userId, String query, int topK, double similarityThreshold);

    List<LongTermMemory> listByUserId(String userId);

    void save(LongTermMemory memory);

    void deleteByUserId(String userId);

    default void deleteById(String userId, String memoryId) {
        throw new UnsupportedOperationException("Deleting a long-term memory by id is not supported by this store.");
    }
}

