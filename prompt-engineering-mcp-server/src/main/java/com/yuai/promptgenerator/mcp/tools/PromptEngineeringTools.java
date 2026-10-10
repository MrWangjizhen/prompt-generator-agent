package com.yuai.promptgenerator.mcp.tools;

import java.util.List;

import com.yuai.promptgenerator.mcp.catalog.PromptFrameworkCatalog;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class PromptEngineeringTools {
    private final PromptFrameworkCatalog catalog;
    public PromptEngineeringTools(PromptFrameworkCatalog catalog) { this.catalog = catalog; }

    @Tool(description = "List prompt-engineering frameworks suitable for a task type")
    public List<PromptFrameworkCatalog.Framework> listPromptFrameworks(
            @ToolParam(description = "TEXT, CODE, ANALYSIS, IMAGE, or AGENT") String taskType) {
        return catalog.list(taskType);
    }

    @Tool(description = "Get a prompt-engineering framework by id")
    public PromptFrameworkCatalog.Framework getPromptFramework(
            @ToolParam(description = "Framework id such as rtf, costar, risen, or image") String id) {
        return catalog.get(id);
    }

    @Tool(description = "Statically inspect a prompt for clarity, structure, constraints, and termination conditions")
    public PromptFrameworkCatalog.LintResult lintPrompt(
            @ToolParam(description = "Prompt text") String prompt,
            @ToolParam(description = "TEXT, CODE, ANALYSIS, IMAGE, or AGENT") String taskType) {
        return catalog.lint(prompt, taskType);
    }
}
