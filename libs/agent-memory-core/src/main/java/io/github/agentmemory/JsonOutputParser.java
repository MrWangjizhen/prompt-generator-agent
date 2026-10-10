package io.github.agentmemory;

/**
 * 从模型输出中提取 JSON 对象，兼容 Markdown 代码块包裹。
 */
final class JsonOutputParser {

    private JsonOutputParser() {
    }

    static String extractJsonObject(String content) {
        if (content == null) {
            return "{}";
        }
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                text = text.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return start >= 0 && end >= start ? text.substring(start, end + 1) : "{}";
    }
}

