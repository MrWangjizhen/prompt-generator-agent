package io.github.agentmemory;

import java.util.List;

/**
 * 本轮对话抽取出的用户画像增量。
 */
public record ProfileUpdate(
        List<MemoryFact> facts,
        List<MemoryFact> preferences,
        List<MemoryFact> goals,
        List<MemoryFact> constraints
) {

    public static ProfileUpdate empty() {
        return new ProfileUpdate(List.of(), List.of(), List.of(), List.of());
    }
}

