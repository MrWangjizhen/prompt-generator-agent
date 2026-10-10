package com.yuai.promptgenerator.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.yuai.promptgenerator.core.domain.RequirementProfile;
import com.yuai.promptgenerator.core.domain.TaskType;
import org.junit.jupiter.api.Test;

class GuidedRequirementEngineTest {

    private final GuidedRequirementEngine engine = new GuidedRequirementEngine();

    @Test
    void asksForFirstMissingField() {
        var next = engine.next(new RequirementProfile(), TaskType.TEXT, 0);
        assertThat(next.readyForConfirmation()).isFalse();
        assertThat(next.question()).contains("最终希望");
        assertThat(next.missingFields().getFirst()).isEqualTo("goal");
    }

    @Test
    void becomesReadyWhenRequiredFieldsArePresent() {
        RequirementProfile profile = new RequirementProfile();
        profile.merge(Map.of(
                "goal", "写产品文案", "context", "新品发布", "audience", "开发者",
                "input", "产品参数", "outputFormat", "Markdown", "constraints", List.of("500字"),
                "successCriteria", List.of("准确", "清晰"), "tone", "专业"), Map.of(), List.of());
        assertThat(engine.next(profile, TaskType.TEXT, 8).readyForConfirmation()).isTrue();
    }

    @Test
    void stopsAfterTwelveTurnsWithExplicitAssumptions() {
        var next = engine.next(new RequirementProfile(), TaskType.AGENT, 12);
        assertThat(next.readyForConfirmation()).isTrue();
        assertThat(next.assumptionsRequired()).isTrue();
        assertThat(next.missingFields()).isNotEmpty();
    }
}
