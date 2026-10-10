package io.github.agentmemory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 将画像和长期记忆格式化成可注入系统提示词的上下文。
 */
public class MemoryContextBuilder {

    public String build(UserProfile userProfile, List<LongTermMemory> longTermMemories) {
        String profile = formatProfile(userProfile);
        String longTerm = formatLongTerm(longTermMemories);
        if (profile.isBlank() && longTerm.isBlank()) {
            return "";
        }
        return """

                【用户画像】
                %s

                【相关长期记忆】
                %s

                使用要求：
                - 用户画像是稳定偏好和长期事实，只在相关时用于个性化回答。
                - 长期记忆来自历史检索，只在和当前问题相关时使用。
                - 不要编造未出现在当前问题、用户画像或长期记忆里的事实。
                """.formatted(defaultText(profile), defaultText(longTerm));
    }

    private static String formatProfile(UserProfile profile) {
        if (profile == null) {
            return "";
        }
        return """
                事实：%s
                偏好：%s
                目标：%s
                限制：%s
                """.formatted(formatFacts(profile.getFacts()), formatFacts(profile.getPreferences()),
                formatFacts(profile.getGoals()), formatFacts(profile.getConstraints())).trim();
    }

    private static String formatFacts(List<MemoryFact> facts) {
        if (facts == null || facts.isEmpty()) {
            return "无";
        }
        return facts.stream().map(MemoryFact::value).collect(Collectors.joining("；"));
    }

    private static String formatLongTerm(List<LongTermMemory> memories) {
        if (memories == null || memories.isEmpty()) {
            return "";
        }
        return memories.stream()
                .map(memory -> "- [%s | %s | importance=%d] %s".formatted(memory.createdAt(),
                        memory.conversationType(), memory.importance(), memory.content()))
                .collect(Collectors.joining("\n"));
    }

    private static String defaultText(String value) {
        return value == null || value.isBlank() ? "无" : value;
    }
}

