package io.github.agentmemory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存长期记忆存储。检索采用简单关键词匹配，不替代生产向量库。
 */
public class InMemoryLongTermMemoryStore implements LongTermMemoryStore, LongTermMemoryIndex {

    private final List<LongTermMemory> memories = new CopyOnWriteArrayList<>();

    @Override
    public List<LongTermMemory> search(String userId, String query, int topK, double similarityThreshold) {
        String normalizedQuery = query == null ? "" : query.toLowerCase();
        return memories.stream()
                .filter(memory -> memory.userId().equals(userId))
                .filter(memory -> normalizedQuery.isBlank()
                        || memory.content().toLowerCase().contains(normalizedQuery)
                        || memory.topics().stream().anyMatch(topic -> normalizedQuery.contains(topic.toLowerCase())))
                .sorted(Comparator.comparing(LongTermMemory::createdAt).reversed())
                .limit(topK)
                .toList();
    }

    @Override
    public List<LongTermMemory> listByUserId(String userId) {
        return memories.stream()
                .filter(memory -> memory.userId().equals(userId))
                .sorted(Comparator.comparing(LongTermMemory::createdAt).reversed())
                .toList();
    }

    @Override
    public void save(LongTermMemory memory) {
        memories.add(memory);
    }

    @Override
    public void deleteByUserId(String userId) {
        memories.removeIf(memory -> memory.userId().equals(userId));
    }

    @Override
    public LongTermMemoryPage search(LongTermMemoryQuery query) {
        LongTermMemoryQuery normalized = query.normalized();
        List<LongTermMemory> filtered = memories.stream()
                .filter(memory -> memory.userId().equals(normalized.userId()))
                .filter(memory -> normalized.createdFrom() == null
                        || !memory.createdAt().isBefore(normalized.createdFrom()))
                .filter(memory -> normalized.createdTo() == null
                        || !memory.createdAt().isAfter(normalized.createdTo()))
                .filter(memory -> normalized.conversationType() == null
                        || memory.conversationType().equals(normalized.conversationType()))
                .filter(memory -> normalized.minImportance() == null
                        || memory.importance() >= normalized.minImportance())
                .filter(memory -> normalized.topics().isEmpty()
                        || memory.topics().stream().anyMatch(normalized.topics()::contains))
                .sorted(Comparator.comparing(LongTermMemory::createdAt).reversed())
                .toList();
        int fromIndex = Math.min(normalized.page() * normalized.size(), filtered.size());
        int toIndex = Math.min(fromIndex + normalized.size(), filtered.size());
        return new LongTermMemoryPage(filtered.subList(fromIndex, toIndex), normalized.page(), normalized.size(),
                filtered.size());
    }

    @Override
    public Optional<LongTermMemory> findById(String userId, String memoryId) {
        return memories.stream()
                .filter(memory -> memory.userId().equals(userId))
                .filter(memory -> memory.id().equals(memoryId))
                .findFirst();
    }

    @Override
    public void deleteById(String userId, String memoryId) {
        memories.removeIf(memory -> memory.userId().equals(userId) && memory.id().equals(memoryId));
    }

    @Override
    public void delete(LongTermMemoryQuery query) {
        LongTermMemoryQuery normalized = query.normalized();
        memories.removeIf(memory -> memory.userId().equals(normalized.userId())
                && (normalized.createdFrom() == null || !memory.createdAt().isBefore(normalized.createdFrom()))
                && (normalized.createdTo() == null || !memory.createdAt().isAfter(normalized.createdTo()))
                && (normalized.conversationType() == null
                        || memory.conversationType().equals(normalized.conversationType()))
                && (normalized.minImportance() == null || memory.importance() >= normalized.minImportance())
                && (normalized.topics().isEmpty()
                        || memory.topics().stream().anyMatch(normalized.topics()::contains)));
    }
}

