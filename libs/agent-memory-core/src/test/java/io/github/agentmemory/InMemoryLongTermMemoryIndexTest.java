package io.github.agentmemory;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLongTermMemoryIndexTest {

    @Test
    void shouldPageAndFilterLongTermMemoryMetadata() {
        InMemoryLongTermMemoryStore store = new InMemoryLongTermMemoryStore();
        store.save(memory("1", "user-1", "technical", 5, List.of("Spring AI"), "2026-08-20T00:00:00Z"));
        store.save(memory("2", "user-1", "general", 2, List.of("travel"), "2026-08-21T00:00:00Z"));
        store.save(memory("3", "user-2", "technical", 5, List.of("Spring AI"), "2026-08-22T00:00:00Z"));

        LongTermMemoryQuery query = new LongTermMemoryQuery("user-1", 0, 10,
                Instant.parse("2026-08-19T00:00:00Z"), Instant.parse("2026-08-20T23:59:59Z"),
                "technical", 4, List.of("Spring AI"));

        LongTermMemoryPage page = store.search(query);

        assertEquals(1, page.totalElements());
        assertEquals("1", page.content().getFirst().id());
    }

    @Test
    void shouldDeleteMatchingLongTermMemories() {
        InMemoryLongTermMemoryStore store = new InMemoryLongTermMemoryStore();
        store.save(memory("1", "user-1", "technical", 5, List.of("Spring AI"), "2026-08-20T00:00:00Z"));
        store.save(memory("2", "user-1", "general", 2, List.of("travel"), "2026-08-21T00:00:00Z"));

        store.delete(new LongTermMemoryQuery("user-1", 0, 10, null, null, "technical", null, List.of()));

        assertTrue(store.findById("user-1", "1").isEmpty());
        assertTrue(store.findById("user-1", "2").isPresent());
    }

    private static LongTermMemory memory(String id, String userId, String type, int importance, List<String> topics,
            String createdAt) {
        return new LongTermMemory(id, userId, "chat-1", "content " + id, Instant.parse(createdAt), type, importance,
                topics, 1);
    }
}
