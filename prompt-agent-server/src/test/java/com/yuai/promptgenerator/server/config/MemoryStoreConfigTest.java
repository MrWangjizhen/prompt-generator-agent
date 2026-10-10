package com.yuai.promptgenerator.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.agentmemory.store.MongoLongTermMemoryIndex;
import io.github.agentmemory.store.MongoUserProfileStore;
import io.github.agentmemory.store.QdrantLongTermMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.mongodb.core.MongoOperations;

class MemoryStoreConfigTest {

    private final MemoryStoreConfig config = new MemoryStoreConfig();

    @Test
    void providesMongoProfileStore() {
        assertThat(config.userProfileStore(mock(MongoOperations.class)))
                .isInstanceOf(MongoUserProfileStore.class);
    }

    @Test
    void providesQdrantLongTermStore() {
        assertThat(config.longTermMemoryStore(mock(VectorStore.class)))
                .isInstanceOf(QdrantLongTermMemoryStore.class);
    }

    @Test
    void providesMongoLongTermIndex() {
        assertThat(config.longTermMemoryIndex(mock(MongoOperations.class)))
                .isInstanceOf(MongoLongTermMemoryIndex.class);
    }
}
