package io.github.agentmemory.store;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.agentmemory.LongTermMemory;
import io.github.agentmemory.LongTermMemoryStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 基于 Spring AI VectorStore 的长期记忆适配器，默认用于 Qdrant。
 */
public class QdrantLongTermMemoryStore implements LongTermMemoryStore {

    private final VectorStore vectorStore;

    public QdrantLongTermMemoryStore(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<LongTermMemory> search(String userId, String query, int topK, double similarityThreshold) {
        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .filterExpression(filter.eq("userId", userId).build())
                .build();
        return vectorStore.similaritySearch(request).stream().map(QdrantLongTermMemoryStore::fromDocument).toList();
    }

    @Override
    public List<LongTermMemory> listByUserId(String userId) {
        throw new UnsupportedOperationException("Spring AI VectorStore does not expose full listing by metadata. "
                + "Use a LongTermMemoryStore implementation with a metadata index for export/list APIs.");
    }

    @Override
    public void save(LongTermMemory memory) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("userId", memory.userId());
        metadata.put("chatId", memory.chatId());
        metadata.put("createdAt", memory.createdAt().toString());
        metadata.put("conversationType", memory.conversationType());
        metadata.put("importance", memory.importance());
        metadata.put("topics", memory.topics());
        metadata.put("schemaVersion", memory.schemaVersion());
        vectorStore.add(List.of(new Document(memory.id(), memory.content(), metadata)));
    }

    @Override
    public void deleteByUserId(String userId) {
        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        vectorStore.delete(filter.eq("userId", userId).build());
    }

    @Override
    public void deleteById(String userId, String memoryId) {
        vectorStore.delete(List.of(memoryId));
    }

    private static LongTermMemory fromDocument(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new LongTermMemory(
                document.getId(),
                stringValue(metadata.get("userId")),
                stringValue(metadata.get("chatId")),
                document.getText(),
                instantValue(metadata.get("createdAt")),
                defaultText(stringValue(metadata.get("conversationType")), "general"),
                intValue(metadata.get("importance"), 1),
                topics(metadata.get("topics")),
                intValue(metadata.get("schemaVersion"), 1)
        );
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static Instant instantValue(Object value) {
        try {
            return value == null ? Instant.EPOCH : Instant.parse(value.toString());
        }
        catch (RuntimeException exception) {
            return Instant.EPOCH;
        }
    }

    private static int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? defaultValue : Integer.parseInt(value.toString());
        }
        catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> topics(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}

