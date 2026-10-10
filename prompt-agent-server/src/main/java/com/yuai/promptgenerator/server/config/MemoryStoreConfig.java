package com.yuai.promptgenerator.server.config;

import io.github.agentmemory.LongTermMemoryIndex;
import io.github.agentmemory.LongTermMemoryStore;
import io.github.agentmemory.UserProfileStore;
import io.github.agentmemory.store.MongoLongTermMemoryIndex;
import io.github.agentmemory.store.MongoUserProfileStore;
import io.github.agentmemory.store.QdrantLongTermMemoryStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;

@Configuration(proxyBeanMethods = false)
public class MemoryStoreConfig {

    @Bean
    UserProfileStore userProfileStore(MongoOperations mongoOperations) {
        return new MongoUserProfileStore(mongoOperations);
    }

    @Bean
    LongTermMemoryStore longTermMemoryStore(
            @Qualifier("agentMemoryVectorStore") VectorStore agentMemoryVectorStore) {
        return new QdrantLongTermMemoryStore(agentMemoryVectorStore);
    }

    @Bean
    LongTermMemoryIndex longTermMemoryIndex(MongoOperations mongoOperations) {
        return new MongoLongTermMemoryIndex(mongoOperations);
    }
}
