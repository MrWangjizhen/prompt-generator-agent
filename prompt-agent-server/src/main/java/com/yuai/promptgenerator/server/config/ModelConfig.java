package com.yuai.promptgenerator.server.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
public class ModelConfig {
    @Bean("primaryChatModel")
    @Primary
    @Profile("!prod")
    ChatModel localChatModel(@Qualifier("ollamaChatModel") ChatModel model) {
        return model;
    }

    @Bean("primaryEmbeddingModel")
    @Primary
    @Profile("!prod")
    EmbeddingModel localEmbeddingModel(@Qualifier("ollamaEmbeddingModel") EmbeddingModel model) {
        return model;
    }

    @Bean("primaryChatModel")
    @Primary
    @Profile("prod")
    ChatModel productionChatModel(@Qualifier("dashscopeChatModel") ChatModel model) {
        return model;
    }

    @Bean("primaryEmbeddingModel")
    @Primary
    @Profile("prod")
    EmbeddingModel productionEmbeddingModel(@Qualifier("dashscopeEmbeddingModel") EmbeddingModel model) {
        return model;
    }
}
