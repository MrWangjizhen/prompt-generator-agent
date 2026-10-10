package io.github.agentmemory;

import java.util.Optional;

/**
 * 长期记忆元数据索引，负责管理查询、导出和精确删除。
 */
public interface LongTermMemoryIndex {

    void save(LongTermMemory memory);

    LongTermMemoryPage search(LongTermMemoryQuery query);

    Optional<LongTermMemory> findById(String userId, String memoryId);

    void deleteById(String userId, String memoryId);

    void delete(LongTermMemoryQuery query);

    void deleteByUserId(String userId);
}
