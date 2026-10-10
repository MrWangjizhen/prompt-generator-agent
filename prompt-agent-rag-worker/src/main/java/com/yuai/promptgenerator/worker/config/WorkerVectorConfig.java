package com.yuai.promptgenerator.worker.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerVectorConfig {
    @Bean(destroyMethod = "close")
    QdrantClient qdrantClient(WorkerProperties properties) {
        var value = properties.getQdrant();
        var builder = QdrantGrpcClient.newBuilder(value.getHost(), value.getPort(), value.isTls());
        if (value.getApiKey() != null && !value.getApiKey().isBlank()) builder.withApiKey(value.getApiKey());
        return new QdrantClient(builder.build());
    }

    @Bean("knowledgeVectorStore")
    VectorStore knowledgeVectorStore(QdrantClient client,
            @Qualifier("primaryEmbeddingModel") EmbeddingModel embeddingModel, WorkerProperties properties) {
        return QdrantVectorStore.builder(client, embeddingModel)
                .collectionName(properties.getQdrant().getKnowledgeCollection()).initializeSchema(false).build();
    }
}
