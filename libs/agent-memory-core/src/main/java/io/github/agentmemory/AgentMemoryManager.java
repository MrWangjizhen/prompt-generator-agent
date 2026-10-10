package io.github.agentmemory;

import java.util.List;

/**
 * 记忆管理服务，面向管理 API 提供查看、删除、禁用和导出能力。
 */
public class AgentMemoryManager {

    private final UserProfileStore userProfileStore;

    private final LongTermMemoryStore longTermMemoryStore;

    private final LongTermMemoryIndex longTermMemoryIndex;

    public AgentMemoryManager(UserProfileStore userProfileStore, LongTermMemoryStore longTermMemoryStore) {
        this(userProfileStore, longTermMemoryStore, new UnavailableLongTermMemoryIndex());
    }

    public AgentMemoryManager(UserProfileStore userProfileStore, LongTermMemoryStore longTermMemoryStore,
            LongTermMemoryIndex longTermMemoryIndex) {
        this.userProfileStore = userProfileStore;
        this.longTermMemoryStore = longTermMemoryStore;
        this.longTermMemoryIndex = longTermMemoryIndex;
    }

    public UserProfile getProfile(String userId) {
        return userProfileStore.findByUserId(userId).orElse(UserProfile.empty(userId));
    }

    public List<LongTermMemory> listLongTermMemories(String userId) {
        return exportLongTermMemories(userId);
    }

    public LongTermMemoryPage searchLongTermMemories(LongTermMemoryQuery query) {
        return longTermMemoryIndex.search(query);
    }

    public void deleteLongTermMemory(String userId, String memoryId) {
        longTermMemoryStore.deleteById(userId, memoryId);
        if (longTermMemoryIndex != longTermMemoryStore) {
            longTermMemoryIndex.deleteById(userId, memoryId);
        }
    }

    public void deleteLongTermMemories(LongTermMemoryQuery query) {
        LongTermMemoryQuery normalized = query.normalized();
        for (LongTermMemory memory : exportLongTermMemories(normalized)) {
            longTermMemoryStore.deleteById(normalized.userId(), memory.id());
        }
        if (longTermMemoryIndex != longTermMemoryStore) {
            longTermMemoryIndex.delete(normalized);
        }
    }

    public MemoryExport exportUserMemory(String userId) {
        return new MemoryExport(getProfile(userId), listLongTermMemories(userId));
    }

    public void deleteUserMemory(String userId) {
        userProfileStore.deleteByUserId(userId);
        longTermMemoryStore.deleteByUserId(userId);
        if (longTermMemoryIndex != longTermMemoryStore) {
            longTermMemoryIndex.deleteByUserId(userId);
        }
    }

    public void deleteProfile(String userId) {
        userProfileStore.deleteByUserId(userId);
    }

    public void setDisabled(String userId, boolean disabled) {
        userProfileStore.setDisabled(userId, disabled);
    }

    public record MemoryExport(UserProfile profile, List<LongTermMemory> longTermMemories) {
    }

    private List<LongTermMemory> exportLongTermMemories(String userId) {
        return exportLongTermMemories(LongTermMemoryQuery.forUser(userId));
    }

    private List<LongTermMemory> exportLongTermMemories(LongTermMemoryQuery query) {
        List<LongTermMemory> memories = new java.util.ArrayList<>();
        LongTermMemoryQuery normalized = query.normalized();
        int page = 0;
        LongTermMemoryPage result;
        do {
            LongTermMemoryQuery pageQuery = new LongTermMemoryQuery(normalized.userId(), page, 200,
                    normalized.createdFrom(), normalized.createdTo(), normalized.conversationType(),
                    normalized.minImportance(), normalized.topics());
            result = longTermMemoryIndex.search(pageQuery);
            memories.addAll(result.content());
            page++;
        }
        while (!result.content().isEmpty() && memories.size() < result.totalElements());
        return memories;
    }
}

