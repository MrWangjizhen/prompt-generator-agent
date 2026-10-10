package io.github.agentmemory;

import java.util.List;

/**
 * 将向量存储和元数据索引组合为一个长期记忆存储。
 */
public class IndexedLongTermMemoryStore implements LongTermMemoryStore {

    private final LongTermMemoryStore vectorStore;

    private final LongTermMemoryIndex memoryIndex;

    public IndexedLongTermMemoryStore(LongTermMemoryStore vectorStore, LongTermMemoryIndex memoryIndex) {
        this.vectorStore = vectorStore;
        this.memoryIndex = memoryIndex;
    }

    @Override
    public List<LongTermMemory> search(String userId, String query, int topK, double similarityThreshold) {
        return vectorStore.search(userId, query, topK, similarityThreshold);
    }

    @Override
    public List<LongTermMemory> listByUserId(String userId) {
        return memoryIndex.search(new LongTermMemoryQuery(userId, 0, 200, null, null, null, null, List.of()))
                .content();
    }

    @Override
    public void save(LongTermMemory memory) {
        vectorStore.save(memory);
        memoryIndex.save(memory);
    }

    @Override
    public void deleteByUserId(String userId) {
        vectorStore.deleteByUserId(userId);
        memoryIndex.deleteByUserId(userId);
    }

    @Override
    public void deleteById(String userId, String memoryId) {
        vectorStore.deleteById(userId, memoryId);
        memoryIndex.deleteById(userId, memoryId);
    }
}
