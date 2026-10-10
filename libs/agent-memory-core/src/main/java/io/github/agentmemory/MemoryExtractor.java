package io.github.agentmemory;

/**
 * 从本轮对话中抽取画像增量和长期记忆。
 */
public interface MemoryExtractor {

    ProfileUpdate extractProfileUpdate(String userMessage, String assistantResponse, UserProfile currentProfile);

    ExtractedLongTermMemory extractLongTermMemory(String userMessage, String assistantResponse, MemoryDecision decision);
}

