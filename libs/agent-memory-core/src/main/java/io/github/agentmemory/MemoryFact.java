package io.github.agentmemory;

import java.time.Instant;

/**
 * 画像中的单条事实，带置信度和来源，便于冲突处理和审计。
 */
public record MemoryFact(
        String key,
        String value,
        double confidence,
        String source,
        Instant createdAt,
        Instant updatedAt
) {

    public static MemoryFact of(String key, String value, double confidence, String source) {
        Instant now = Instant.now();
        return new MemoryFact(key, value, normalizeConfidence(confidence), source == null ? "" : source, now, now);
    }

    public MemoryFact withValue(String newValue, double newConfidence, String newSource) {
        return new MemoryFact(key, newValue, normalizeConfidence(newConfidence), newSource == null ? source : newSource,
                createdAt, Instant.now());
    }

    private static double normalizeConfidence(double confidence) {
        return Math.max(0.0, Math.min(1.0, confidence));
    }
}

