package io.github.agentmemory;

import java.util.List;

/**
 * 模型抽取出的一条长期记忆。
 */
public record ExtractedLongTermMemory(
        String content,
        String conversationType,
        int importance,
        List<String> topics
) {

    public boolean hasContent() {
        return content != null && !content.isBlank();
    }
}

