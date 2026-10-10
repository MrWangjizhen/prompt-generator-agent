package com.yuai.promptgenerator.mcp.catalog;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PromptFrameworkCatalog {
    private final List<Framework> frameworks = List.of(
            new Framework("rtf", "RTF", List.of("TEXT", "CODE", "ANALYSIS"),
                    "Role - Task - Format", "明确角色、任务和输出格式，适合简洁通用请求。"),
            new Framework("costar", "CO-STAR", List.of("TEXT", "ANALYSIS"),
                    "Context - Objective - Style - Tone - Audience - Response",
                    "适合内容创作和对受众、语气要求明确的任务。"),
            new Framework("risen", "RISEN", List.of("CODE", "AGENT", "ANALYSIS"),
                    "Role - Instructions - Steps - End goal - Narrowing",
                    "适合多步骤、有终止条件和边界约束的任务。"),
            new Framework("image", "Visual Prompt Canvas", List.of("IMAGE"),
                    "Subject - Style - Composition - Lighting - Color - Camera - Negative constraints",
                    "适合图像生成任务的视觉细节组织。"));

    public List<Framework> list(String taskType) {
        if (taskType == null || taskType.isBlank()) return frameworks;
        String normalized = taskType.toUpperCase(Locale.ROOT);
        return frameworks.stream().filter(value -> value.taskTypes().contains(normalized)).toList();
    }

    public Framework get(String id) {
        return frameworks.stream().filter(value -> value.id().equalsIgnoreCase(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown framework: " + id));
    }

    public LintResult lint(String prompt, String taskType) {
        String value = prompt == null ? "" : prompt;
        java.util.ArrayList<String> warnings = new java.util.ArrayList<>();
        if (value.length() < 80) warnings.add("Prompt is probably too short");
        if (!containsAny(value, "目标", "任务", "goal", "task")) warnings.add("Missing an explicit goal");
        if (!containsAny(value, "输出", "格式", "output", "format")) warnings.add("Missing output format");
        if (!containsAny(value, "必须", "约束", "constraint", "must")) warnings.add("Missing constraints");
        if ("AGENT".equalsIgnoreCase(taskType) && !containsAny(value, "结束", "终止", "stop", "terminate")) {
            warnings.add("Agent prompt is missing a termination condition");
        }
        return new LintResult(Math.max(0, 100 - warnings.size() * 20), List.copyOf(warnings), warnings.isEmpty());
    }

    public String asMarkdown() {
        return frameworks.stream().map(value -> "## " + value.name() + "\n\n- 结构：" + value.structure()
                + "\n- 适用：" + String.join(", ", value.taskTypes()) + "\n- 说明：" + value.description())
                .reduce("# Prompt Framework Catalog\n", (a, b) -> a + "\n\n" + b);
    }

    public String generatorTemplate(String taskType, String goal) {
        Framework framework = list(taskType).stream().findFirst().orElse(frameworks.getFirst());
        return "使用 " + framework.name() + " 框架，为以下目标收集必要信息并生成提示词。\n目标："
                + (goal == null ? "{{goal}}" : goal) + "\n框架结构：" + framework.structure()
                + "\n不要猜测缺失信息；每次只询问一个最关键问题。";
    }

    private boolean containsAny(String value, String... candidates) {
        String lower = value.toLowerCase(Locale.ROOT);
        for (String candidate : candidates) if (lower.contains(candidate.toLowerCase(Locale.ROOT))) return true;
        return false;
    }

    public record Framework(String id, String name, List<String> taskTypes, String structure, String description) { }
    public record LintResult(int score, List<String> warnings, boolean passed) { }
}
