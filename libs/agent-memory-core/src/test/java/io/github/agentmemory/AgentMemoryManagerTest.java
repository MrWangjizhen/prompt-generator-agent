package io.github.agentmemory;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentMemoryManagerTest {

    @Test
    void shouldExportAndDeleteLongTermMemoriesThroughIndex() {
        InMemoryUserProfileStore profileStore = new InMemoryUserProfileStore();
        InMemoryLongTermMemoryStore longTermStore = new InMemoryLongTermMemoryStore();
        AgentMemoryManager manager = new AgentMemoryManager(profileStore, longTermStore, longTermStore);
        longTermStore.save(memory("1"));
        longTermStore.save(memory("2"));

        assertEquals(2, manager.exportUserMemory("user-1").longTermMemories().size());

        manager.deleteLongTermMemory("user-1", "1");

        assertTrue(longTermStore.findById("user-1", "1").isEmpty());
        assertEquals(1, manager.listLongTermMemories("user-1").size());
    }

    private static LongTermMemory memory(String id) {
        return new LongTermMemory(id, "user-1", "chat-1", "content " + id, Instant.parse("2026-08-20T00:00:00Z"),
                "technical", 5, List.of("Spring AI"), 1);
    }
}
