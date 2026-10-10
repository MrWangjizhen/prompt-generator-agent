package io.github.agentmemory;

/**
 * 判断本轮对话是否需要读/写记忆。
 */
public interface MemoryRouter {

    MemoryDecision decide(String userMessage, UserProfile userProfile);
}

