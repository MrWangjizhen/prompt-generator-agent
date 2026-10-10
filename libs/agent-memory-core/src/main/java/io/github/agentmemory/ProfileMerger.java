package io.github.agentmemory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户画像合并策略。第一版以 key 为冲突判断依据，高置信度事实优先覆盖低置信度事实。
 */
public final class ProfileMerger {

    private static final int MAX_ITEMS_PER_SECTION = 30;

    private ProfileMerger() {
    }

    public static UserProfile merge(UserProfile current, ProfileUpdate update, int schemaVersion) {
        UserProfile target = current == null ? new UserProfile("unknown", schemaVersion) : current;
        target.setSchemaVersion(schemaVersion);
        target.setFacts(mergeFacts(target.getFacts(), update == null ? List.of() : update.facts()));
        target.setPreferences(mergeFacts(target.getPreferences(), update == null ? List.of() : update.preferences()));
        target.setGoals(mergeFacts(target.getGoals(), update == null ? List.of() : update.goals()));
        target.setConstraints(mergeFacts(target.getConstraints(), update == null ? List.of() : update.constraints()));
        if (target.getCreatedAt() == null) {
            target.setCreatedAt(Instant.now());
        }
        target.setUpdatedAt(Instant.now());
        return target;
    }

    private static List<MemoryFact> mergeFacts(List<MemoryFact> existing, List<MemoryFact> incoming) {
        Map<String, MemoryFact> merged = new LinkedHashMap<>();
        if (existing != null) {
            existing.stream().filter(ProfileMerger::hasValue).forEach(fact -> merged.put(normalizeKey(fact), fact));
        }
        if (incoming != null) {
            for (MemoryFact fact : incoming.stream().filter(ProfileMerger::hasValue).toList()) {
                String key = normalizeKey(fact);
                MemoryFact old = merged.get(key);
                if (old == null || fact.confidence() >= old.confidence()) {
                    merged.put(key, old == null ? normalizeFact(fact) : old.withValue(fact.value(), fact.confidence(), fact.source()));
                }
            }
        }
        return new ArrayList<>(merged.values()).stream().limit(MAX_ITEMS_PER_SECTION).toList();
    }

    private static MemoryFact normalizeFact(MemoryFact fact) {
        String key = fact.key() == null || fact.key().isBlank() ? fact.value() : fact.key();
        return MemoryFact.of(key.trim(), fact.value().trim(), fact.confidence(), fact.source());
    }

    private static String normalizeKey(MemoryFact fact) {
        String key = fact.key() == null || fact.key().isBlank() ? fact.value() : fact.key();
        return key.trim().toLowerCase();
    }

    private static boolean hasValue(MemoryFact fact) {
        return fact != null && fact.value() != null && !fact.value().isBlank();
    }
}

