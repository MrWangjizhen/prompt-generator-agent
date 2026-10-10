package com.yuai.promptgenerator.server.tools;

import com.yuai.promptgenerator.core.service.PromptLintService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class PromptInspectionTool {
    private final PromptLintService lintService;
    public PromptInspectionTool(PromptLintService lintService) { this.lintService = lintService; }

    @Tool(description = "Inspect a prompt for missing goals, output format, quality criteria, and unsafe instructions")
    public PromptLintService.LintResult inspectPrompt(@ToolParam(description = "Prompt text") String prompt) {
        return lintService.lint(prompt);
    }
}
