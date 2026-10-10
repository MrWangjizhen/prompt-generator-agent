package io.github.agentmemory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileMergerTest {

    @Test
    void shouldKeepHigherConfidenceFactWhenKeysConflict() {
        UserProfile profile = new UserProfile("user-1", 1);
        profile.setPreferences(List.of(MemoryFact.of("style", "喜欢详细回答", 0.9, "old")));
        ProfileUpdate update = new ProfileUpdate(List.of(),
                List.of(MemoryFact.of("style", "喜欢简洁回答", 0.6, "new")), List.of(), List.of());

        UserProfile merged = ProfileMerger.merge(profile, update, 1);

        assertEquals("喜欢详细回答", merged.getPreferences().getFirst().value());
    }

    @Test
    void shouldOverwriteLowerConfidenceFactWhenKeysConflict() {
        UserProfile profile = new UserProfile("user-1", 1);
        profile.setPreferences(List.of(MemoryFact.of("style", "喜欢详细回答", 0.4, "old")));
        ProfileUpdate update = new ProfileUpdate(List.of(),
                List.of(MemoryFact.of("style", "喜欢简洁回答", 0.8, "new")), List.of(), List.of());

        UserProfile merged = ProfileMerger.merge(profile, update, 1);

        assertEquals("喜欢简洁回答", merged.getPreferences().getFirst().value());
    }
}

