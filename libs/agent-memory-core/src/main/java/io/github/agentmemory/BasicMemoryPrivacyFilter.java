package io.github.agentmemory;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 基础隐私过滤器，覆盖手机号、邮箱、身份证号等常见敏感信息。
 */
public class BasicMemoryPrivacyFilter implements MemoryPrivacyFilter {

    private final boolean enabled;

    private final List<Replacement> replacements;

    public BasicMemoryPrivacyFilter(boolean enabled) {
        this.enabled = enabled;
        this.replacements = List.of(
                new Replacement(Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "[EMAIL]"),
                new Replacement(Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)"), "[PHONE]"),
                new Replacement(Pattern.compile("(?<!\\d)\\d{17}[0-9Xx](?!\\d)"), "[ID_CARD]")
        );
    }

    @Override
    public String filter(String text) {
        if (!enabled || text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        String filtered = text;
        for (Replacement replacement : replacements) {
            filtered = replacement.pattern().matcher(filtered).replaceAll(replacement.value());
        }
        return filtered;
    }

    private record Replacement(Pattern pattern, String value) {
    }
}

