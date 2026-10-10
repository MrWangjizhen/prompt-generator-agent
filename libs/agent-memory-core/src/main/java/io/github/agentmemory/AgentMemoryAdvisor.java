package io.github.agentmemory;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * 真正接入 ChatClient 的记忆 Advisor，负责在请求前注入记忆上下文，并在响应后异步写入记忆。
 */
public class AgentMemoryAdvisor implements BaseAdvisor {

    public static final String USER_ID = "agent_memory_user_id";

    public static final String CHAT_ID = "agent_memory_chat_id";

    public static final String CONSENT = "agent_memory_consent";

    private static final String MEMORY_CONTEXT = AgentMemoryAdvisor.class.getName() + ".context";

    private final AgentMemoryService agentMemoryService;

    private final int order;

    public AgentMemoryAdvisor(AgentMemoryService agentMemoryService) {
        this(agentMemoryService, BaseAdvisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER - 100);
    }

    public AgentMemoryAdvisor(AgentMemoryService agentMemoryService, int order) {
        this.agentMemoryService = agentMemoryService;
        this.order = order;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        Map<String, Object> context = request.context();
        String userId = stringParam(context, USER_ID, "default-user");
        String chatId = stringParam(context, CHAT_ID, stringParam(context, ChatMemory.CONVERSATION_ID, userId));
        boolean consent = booleanParam(context, CONSENT, false);
        String userMessage = request.prompt().getUserMessage().getText();

        AgentMemoryContext memoryContext = agentMemoryService.prepare(userId, chatId, userMessage, consent);
        ChatClientRequest.Builder builder = request.mutate()
                .context(MEMORY_CONTEXT, memoryContext);
        if (!memoryContext.promptContext().isBlank()) {
            builder.prompt(request.prompt().augmentSystemMessage(memoryContext.promptContext()));
        }
        return builder.build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        Object value = response.context().get(MEMORY_CONTEXT);
        if (value instanceof AgentMemoryContext memoryContext) {
            agentMemoryService.rememberAfterResponse(
                    memoryContext,
                    memoryContext.userMessage(),
                    assistantText(response)
            );
        }
        return response;
    }

    @Override
    public int getOrder() {
        return order;
    }

    private static String assistantText(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            return "";
        }
        return chatResponse.getResult().getOutput().getText();
    }

    private static String stringParam(Map<String, Object> context, String key, String defaultValue) {
        Object value = context.get(key);
        return value == null || value.toString().isBlank() ? defaultValue : value.toString();
    }

    private static boolean booleanParam(Map<String, Object> context, String key, boolean defaultValue) {
        Object value = context.get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.toString());
    }
}

