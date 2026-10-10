package com.yuai.promptgenerator.core.service;

import java.util.ArrayList;
import java.util.List;

public class PromptLintService {

    public LintResult lint(String prompt) {
        String value = prompt == null ? "" : prompt.trim();
        List<String> warnings = new ArrayList<>();
        if (value.length() < 80) warnings.add("提示词过短，可能缺少背景或约束。");
        if (!containsAny(value, "目标", "任务", "请")) warnings.add("缺少明确任务目标。");
        if (!containsAny(value, "输出", "格式", "返回")) warnings.add("缺少输出格式说明。");
        if (!containsAny(value, "标准", "检查", "确保", "必须")) warnings.add("缺少质量或验收标准。");
        if (value.contains("忽略之前") || value.contains("绕过安全")) warnings.add("包含潜在的越权或提示注入表达。");
        int score = Math.max(0, 100 - warnings.size() * 20);
        return new LintResult(score, List.copyOf(warnings), warnings.isEmpty());
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    public record LintResult(int score, List<String> warnings, boolean passed) {
    }
}
