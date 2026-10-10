package com.yuai.promptgenerator.server.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class VectorStoreConfig {
    @Bean(destroyMethod = "close")
    QdrantClient qdrantClient(AppProperties properties) {
        AppProperties.Qdrant qdrant = properties.getQdrant();
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(qdrant.getHost(), qdrant.getPort(), qdrant.isTls());
        if (qdrant.getApiKey() != null && !qdrant.getApiKey().isBlank()) builder.withApiKey(qdrant.getApiKey());
        return new QdrantClient(builder.build());
    }

    @Bean("agentMemoryVectorStore")
    @Primary
    VectorStore agentMemoryVectorStore(QdrantClient client,
            @Qualifier("primaryEmbeddingModel") EmbeddingModel embeddingModel, AppProperties properties) {
        return QdrantVectorStore.builder(client, embeddingModel)
                .collectionName(properties.getQdrant().getMemoryCollection())
                .initializeSchema(true)
                .build();
    }

    @Bean("knowledgeVectorStore")
    VectorStore knowledgeVectorStore(QdrantClient client,
            @Qualifier("primaryEmbeddingModel") EmbeddingModel embeddingModel, AppProperties properties) {
        return QdrantVectorStore.builder(client, embeddingModel)
                .collectionName(properties.getQdrant().getKnowledgeCollection())
                .initializeSchema(true)
                .build();
    }
}
