package com.yuai.promptgenerator.core.domain;

import java.util.List;
import java.util.Map;

public record PromptCandidate(
        String id,
        String variant,
        String title,
        String content,
        String tradeoff,
        Map<String, String> variables,
        List<String> qualityChecks) {
}
