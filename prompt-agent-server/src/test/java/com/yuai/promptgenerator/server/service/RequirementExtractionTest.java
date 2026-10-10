package com.yuai.promptgenerator.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.yuai.promptgenerator.core.domain.TaskType;
import org.junit.jupiter.api.Test;

class RequirementExtractionTest {

    @Test
    void keepsOnlyFieldsSupportedByTaskType() {
        var raw = new PromptModelGateway.RequirementExtraction(
                Map.of("goal", "build an API", "invented", "discard me"),
                Map.of("language", "Java", "aspectRatio", "16:9"),
                List.of("runtime", "subject", "runtime"));

        var normalized = raw.normalized(TaskType.CODE);

        assertThat(normalized.common()).containsExactly(Map.entry("goal", "build an API"));
        assertThat(normalized.taskSpecific()).containsExactly(Map.entry("language", "Java"));
        assertThat(normalized.skipped()).containsExactly("runtime");
        assertThat(normalized.hasContent()).isTrue();
    }

    @Test
    void dropsBlankValuesAndHandlesNulls() {
        var raw = new PromptModelGateway.RequirementExtraction(Map.of("goal", ""), null, null);

        assertThat(raw.normalized(TaskType.TEXT).hasContent()).isFalse();
    }
}
