package io.github.agentmemory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 基于 ChatModel 的默认记忆抽取器。
 */
public class DefaultMemoryExtractor implements MemoryExtractor {

    private static final Logger log = LoggerFactory.getLogger(DefaultMemoryExtractor.class);

    private static final String PROFILE_SYSTEM_PROMPT = """
            你是用户画像抽取器。请从本轮对话中抽取稳定、长期有用的用户信息。
            只输出 JSON，不要输出解释。
            JSON 字段：
            {
              "facts": [{"key":"字段名","value":"稳定事实","confidence":0.8,"source":"来源摘要"}],
              "preferences": [{"key":"字段名","value":"稳定偏好","confidence":0.8,"source":"来源摘要"}],
              "goals": [{"key":"字段名","value":"长期目标","confidence":0.8,"source":"来源摘要"}],
              "constraints": [{"key":"字段名","value":"长期限制","confidence":0.8,"source":"来源摘要"}]
            }
            不要抽取临时情绪、一次性任务、猜测信息。无法确认时返回空数组。
            """;

    private static final String LONG_TERM_SYSTEM_PROMPT = """
            你是长期记忆抽取器。请从本轮用户输入和助手回复中提取一条未来可能复用的记忆。
            只输出 JSON，不要输出解释。
            JSON 字段：
            {
              "content": "一条清晰、可独立理解的长期记忆；没有价值时为空字符串",
              "conversationType": "general|preference|project|task|personal|technical",
              "importance": 1-5,
              "topics": ["最多5个主题"]
            }
            """;

    private final ChatClient chatClient;

    private final ObjectMapper objectMapper;

    public DefaultMemoryExtractor(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public ProfileUpdate extractProfileUpdate(String userMessage, String assistantResponse, UserProfile currentProfile) {
        try {
            String response = chatClient.prompt()
                    .system(PROFILE_SYSTEM_PROMPT)
                    .user(buildTurnText(userMessage, assistantResponse, currentProfile))
                    .call()
                    .content();
            return objectMapper.readValue(JsonOutputParser.extractJsonObject(response), ProfileUpdate.class);
        }
        catch (Exception exception) {
            log.warn("Profile extraction failed, skip profile update", exception);
            return ProfileUpdate.empty();
        }
    }

    @Override
    public ExtractedLongTermMemory extractLongTermMemory(String userMessage, String assistantResponse,
            MemoryDecision decision) {
        try {
            String response = chatClient.prompt()
                    .system(LONG_TERM_SYSTEM_PROMPT)
                    .user("""
                            路由判断：%s

                            用户输入：
                            %s

                            助手回复：
                            %s
                            """.formatted(decision, userMessage, assistantResponse))
                    .call()
                    .content();
            return objectMapper.readValue(JsonOutputParser.extractJsonObject(response), ExtractedLongTermMemory.class);
        }
        catch (Exception exception) {
            log.warn("Long-term memory extraction failed, skip memory write", exception);
            return new ExtractedLongTermMemory("", "general", 1, java.util.List.of());
        }
    }

    private static String buildTurnText(String userMessage, String assistantResponse, UserProfile currentProfile) {
        return """
                当前用户画像：
                facts=%s
                preferences=%s
                goals=%s
                constraints=%s

                用户输入：
                %s

                助手回复：
                %s
                """.formatted(
                currentProfile == null ? java.util.List.of() : currentProfile.getFacts(),
                currentProfile == null ? java.util.List.of() : currentProfile.getPreferences(),
                currentProfile == null ? java.util.List.of() : currentProfile.getGoals(),
                currentProfile == null ? java.util.List.of() : currentProfile.getConstraints(),
                userMessage,
                assistantResponse
        );
    }
}

