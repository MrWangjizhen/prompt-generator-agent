package com.yuai.promptgenerator.core.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RequirementProfile {

    private Map<String, Object> common = new LinkedHashMap<>();
    private Map<String, Object> taskSpecific = new LinkedHashMap<>();
    private Set<String> skipped = new LinkedHashSet<>();

    public Map<String, Object> getCommon() {
        return common;
    }

    public void setCommon(Map<String, Object> common) {
        this.common = copy(common);
    }

    public Map<String, Object> getTaskSpecific() {
        return taskSpecific;
    }

    public void setTaskSpecific(Map<String, Object> taskSpecific) {
        this.taskSpecific = copy(taskSpecific);
    }

    public Set<String> getSkipped() {
        return skipped;
    }

    public void setSkipped(Set<String> skipped) {
        this.skipped = skipped == null ? new LinkedHashSet<>() : new LinkedHashSet<>(skipped);
    }

    public void merge(Map<String, Object> commonPatch, Map<String, Object> taskPatch, Collection<String> skippedFields) {
        mergeInto(common, commonPatch);
        mergeInto(taskSpecific, taskPatch);
        if (skippedFields != null) {
            skippedFields.stream().filter(RequirementProfile::hasText).forEach(skipped::add);
        }
        common.keySet().forEach(skipped::remove);
        taskSpecific.keySet().forEach(skipped::remove);
    }

    public List<String> missingFields(TaskType taskType) {
        List<String> required = new ArrayList<>(List.of(
                "goal", "context", "audience", "input", "outputFormat", "constraints", "successCriteria"));
        required.addAll(switch (taskType) {
            case TEXT -> List.of("tone");
            case CODE -> List.of("language", "runtime", "acceptanceTests");
            case ANALYSIS -> List.of("method", "evidenceStandard");
            case IMAGE -> List.of("subject", "style", "composition", "aspectRatio");
            case AGENT -> List.of("role", "allowedTools", "terminationCondition", "safetyBoundaries");
        });
        return required.stream().filter(field -> !isPresent(field) && !skipped.contains(field)).toList();
    }

    public boolean isPresent(String field) {
        return meaningful(common.get(field)) || meaningful(taskSpecific.get(field));
    }

    private static boolean meaningful(Object value) {
        if (value == null) return false;
        if (value instanceof String string) return !string.isBlank();
        if (value instanceof Collection<?> collection) return !collection.isEmpty();
        if (value instanceof Map<?, ?> map) return !map.isEmpty();
        return true;
    }

    private static void mergeInto(Map<String, Object> target, Map<String, Object> patch) {
        if (patch == null) return;
        patch.forEach((key, value) -> {
            if (!hasText(key)) return;
            if (meaningful(value)) target.put(key, value);
            else target.remove(key);
        });
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
