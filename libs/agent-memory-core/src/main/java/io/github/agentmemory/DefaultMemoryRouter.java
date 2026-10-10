package io.github.agentmemory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 基于 ChatModel 的默认记忆路由器。
 */
public class DefaultMemoryRouter implements MemoryRouter {

    private static final Logger log = LoggerFactory.getLogger(DefaultMemoryRouter.class);

    private static final String SYSTEM_PROMPT = """
            你是 Agent 记忆路由器。请判断当前用户消息是否需要读取长期记忆、是否包含应该更新用户画像的信息、是否值得写入长期记忆。
            只输出 JSON，不要输出解释。
            JSON 字段：
            {
              "retrieveLongTermMemory": boolean,
              "updateUserProfile": boolean,
              "saveLongTermMemory": boolean,
              "conversationType": "general|preference|project|task|personal|technical",
              "importance": 1-5,
              "topics": ["最多5个主题"]
            }
            """;

    private final ChatClient chatClient;

    private final ObjectMapper objectMapper;

    public DefaultMemoryRouter(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatClient = ChatClient.builder(chatModel).defaultSystem(SYSTEM_PROMPT).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public MemoryDecision decide(String userMessage, UserProfile userProfile) {
        try {
            String response = chatClient.prompt()
                    .user("""
                            用户画像：
                            %s

                            当前用户消息：
                            %s
                            """.formatted(formatProfile(userProfile), userMessage))
                    .call()
                    .content();
            return objectMapper.readValue(JsonOutputParser.extractJsonObject(response), MemoryDecision.class).normalized();
        }
        catch (Exception exception) {
            log.warn("Memory router failed, fallback to no-op decision", exception);
            return MemoryDecision.none();
        }
    }

    private static String formatProfile(UserProfile profile) {
        if (profile == null) {
            return "无";
        }
        return """
                facts=%s
                preferences=%s
                goals=%s
                constraints=%s
                """.formatted(profile.getFacts(), profile.getPreferences(), profile.getGoals(), profile.getConstraints());
    }
}

