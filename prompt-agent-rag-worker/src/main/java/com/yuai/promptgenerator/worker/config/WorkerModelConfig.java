package com.yuai.promptgenerator.worker.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
public class WorkerModelConfig {
    @Bean("primaryEmbeddingModel") @Primary @Profile("!prod")
    EmbeddingModel local(@Qualifier("ollamaEmbeddingModel") EmbeddingModel model) { return model; }
    @Bean("primaryEmbeddingModel") @Primary @Profile("prod")
    EmbeddingModel prod(@Qualifier("dashscopeEmbeddingModel") EmbeddingModel model) { return model; }
}
