package io.github.agentmemory;

/**
 * 记忆写入前的隐私过滤器。
 */
public interface MemoryPrivacyFilter {

    String filter(String text);
}

