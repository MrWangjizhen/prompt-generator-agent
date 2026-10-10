package io.github.agentmemory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryDecisionTest {

    @Test
    void shouldNormalizeMissingAndOutOfRangeValues() {
        MemoryDecision decision = new MemoryDecision(true, true, true, "", 8,
                List.of(" Java ", "", "RAG", "Java"));

        MemoryDecision normalized = decision.normalized();

        assertEquals("general", normalized.conversationType());
        assertEquals(5, normalized.importance());
        assertEquals(List.of("Java", "RAG"), normalized.topics());
    }
}

