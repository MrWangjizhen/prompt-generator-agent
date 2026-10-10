package io.github.agentmemory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryContextBuilderTest {

    @Test
    void shouldBuildContextWithProfileAndLongTermMemories() {
        UserProfile profile = new UserProfile("user-1", 1);
        profile.setPreferences(List.of(MemoryFact.of("style", "喜欢简洁回答", 0.9, "test")));
        profile.setGoals(List.of(MemoryFact.of("project", "构建 Spring AI Agent", 0.9, "test")));
        LongTermMemory memory = LongTermMemory.from("user-1", "chat-1",
                new ExtractedLongTermMemory("用户决定使用 MongoDB 存用户画像。", "technical", 4, List.of("MongoDB")), 1);

        String context = new MemoryContextBuilder().build(profile, List.of(memory));

        assertTrue(context.contains("【用户画像】"));
        assertTrue(context.contains("喜欢简洁回答"));
        assertTrue(context.contains("【相关长期记忆】"));
        assertTrue(context.contains("MongoDB"));
    }
}

