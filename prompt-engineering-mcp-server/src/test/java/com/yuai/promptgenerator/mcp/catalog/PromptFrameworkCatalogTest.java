package com.yuai.promptgenerator.mcp.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PromptFrameworkCatalogTest {
    private final PromptFrameworkCatalog catalog = new PromptFrameworkCatalog();

    @Test
    void filtersFrameworksByTaskType() {
        assertThat(catalog.list("IMAGE")).extracting(PromptFrameworkCatalog.Framework::id).containsExactly("image");
    }

    @Test
    void lintsAgentTerminationCondition() {
        assertThat(catalog.lint("目标是执行任务，必须按 JSON 格式输出", "AGENT").warnings())
                .contains("Agent prompt is missing a termination condition");
    }
}
