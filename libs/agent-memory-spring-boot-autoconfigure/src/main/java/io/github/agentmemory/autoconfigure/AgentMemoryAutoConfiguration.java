package io.github.agentmemory.autoconfigure;

import java.util.concurrent.Executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.agentmemory.AgentMemoryAdvisor;
import io.github.agentmemory.AgentMemoryManager;
import io.github.agentmemory.AgentMemoryProperties;
import io.github.agentmemory.AgentMemoryService;
import io.github.agentmemory.BasicMemoryPrivacyFilter;
import io.github.agentmemory.DefaultMemoryExtractor;
import io.github.agentmemory.DefaultMemoryRouter;
import io.github.agentmemory.InMemoryLongTermMemoryStore;
import io.github.agentmemory.InMemoryUserProfileStore;
import io.github.agentmemory.LongTermMemoryIndex;
import io.github.agentmemory.LongTermMemoryStore;
import io.github.agentmemory.MemoryContextBuilder;
import io.github.agentmemory.MemoryExtractor;
import io.github.agentmemory.MemoryPrivacyFilter;
import io.github.agentmemory.MemoryRouter;
import io.github.agentmemory.UnavailableLongTermMemoryIndex;
import io.github.agentmemory.UserProfileStore;
import io.github.agentmemory.store.MongoLongTermMemoryIndex;
import io.github.agentmemory.store.MongoUserProfileStore;
import io.github.agentmemory.store.QdrantLongTermMemoryStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Agent Memory starter 自动装配入口。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "agent.memory", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentMemoryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "agent.memory")
    public AgentMemoryProperties agentMemoryProperties() {
        return new AgentMemoryProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public Executor agentMemoryTaskExecutor(AgentMemoryProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getAsync().getCorePoolSize());
        executor.setMaxPoolSize(properties.getAsync().getMaxPoolSize());
        executor.setQueueCapacity(properties.getAsync().getQueueCapacity());
        executor.setThreadNamePrefix("agent-memory-");
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    public MemoryPrivacyFilter memoryPrivacyFilter(AgentMemoryProperties properties) {
        return new BasicMemoryPrivacyFilter(properties.getPrivacy().isSensitiveFilterEnabled());
    }

    @Bean
    @ConditionalOnMissingBean
    public MemoryContextBuilder memoryContextBuilder() {
        return new MemoryContextBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public MeterRegistry agentMemoryMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "agent.memory.profile", name = "store", havingValue = "in-memory", matchIfMissing = true)
    public UserProfileStore inMemoryUserProfileStore() {
        return new InMemoryUserProfileStore();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MongoOperations.class)
    @ConditionalOnBean(MongoOperations.class)
    @ConditionalOnProperty(prefix = "agent.memory.profile", name = "store", havingValue = "mongodb")
    public UserProfileStore mongoUserProfileStore(MongoOperations mongoOperations) {
        return new MongoUserProfileStore(mongoOperations);
    }

    @Bean
    @ConditionalOnMissingBean(LongTermMemoryStore.class)
    @ConditionalOnProperty(prefix = "agent.memory.long-term", name = "store", havingValue = "in-memory", matchIfMissing = true)
    public InMemoryLongTermMemoryStore inMemoryLongTermMemoryStore() {
        return new InMemoryLongTermMemoryStore();
    }

    @Bean
    @ConditionalOnMissingBean(LongTermMemoryStore.class)
    @ConditionalOnBean(VectorStore.class)
    @ConditionalOnProperty(prefix = "agent.memory.long-term", name = "store", havingValue = "qdrant")
    public QdrantLongTermMemoryStore qdrantLongTermMemoryStore(ObjectProvider<VectorStore> vectorStores,
            BeanFactory beanFactory, AgentMemoryProperties properties) {
        String beanName = properties.getLongTerm().getVectorStoreBeanName();
        VectorStore vectorStore = beanName == null || beanName.isBlank()
                ? vectorStores.getObject()
                : beanFactory.getBean(beanName, VectorStore.class);
        return new QdrantLongTermMemoryStore(vectorStore);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(LongTermMemoryStore.class)
    @ConditionalOnExpression("'${agent.memory.long-term.store:in-memory}' == 'qdrant' "
            + "&& '${agent.memory.long-term.index-store:in-memory}' == 'in-memory'")
    public LongTermMemoryIndex inMemoryLongTermMemoryIndex(LongTermMemoryStore longTermMemoryStore) {
        if (longTermMemoryStore instanceof LongTermMemoryIndex index) {
            return index;
        }
        return new InMemoryLongTermMemoryStore();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MongoOperations.class)
    @ConditionalOnBean(MongoOperations.class)
    @ConditionalOnProperty(prefix = "agent.memory.long-term", name = "index-store", havingValue = "mongodb")
    public LongTermMemoryIndex mongoLongTermMemoryIndex(MongoOperations mongoOperations) {
        return new MongoLongTermMemoryIndex(mongoOperations);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ChatModel.class)
    public MemoryRouter memoryRouter(ObjectProvider<ChatModel> chatModels, BeanFactory beanFactory,
            AgentMemoryProperties properties, ObjectMapper objectMapper) {
        return new DefaultMemoryRouter(selectModel(chatModels, beanFactory, properties.getRouter().getModelBeanName()),
                objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ChatModel.class)
    public MemoryExtractor memoryExtractor(ObjectProvider<ChatModel> chatModels, BeanFactory beanFactory,
            AgentMemoryProperties properties, ObjectMapper objectMapper) {
        return new DefaultMemoryExtractor(selectModel(chatModels, beanFactory, properties.getExtractor().getModelBeanName()),
                objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({ MemoryRouter.class, MemoryExtractor.class })
    public AgentMemoryService agentMemoryService(UserProfileStore userProfileStore,
            LongTermMemoryStore longTermMemoryStore, MemoryRouter memoryRouter, MemoryExtractor memoryExtractor,
            ObjectProvider<LongTermMemoryIndex> longTermMemoryIndexes, MemoryContextBuilder memoryContextBuilder, MemoryPrivacyFilter privacyFilter,
            AgentMemoryProperties properties, Executor agentMemoryTaskExecutor, MeterRegistry agentMemoryMeterRegistry) {
        return new AgentMemoryService(userProfileStore, longTermMemoryStore,
                selectLongTermMemoryIndex(longTermMemoryStore, longTermMemoryIndexes), memoryRouter, memoryExtractor,
                memoryContextBuilder, privacyFilter, properties, agentMemoryTaskExecutor, agentMemoryMeterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AgentMemoryService.class)
    public AgentMemoryAdvisor agentMemoryAdvisor(AgentMemoryService agentMemoryService) {
        return new AgentMemoryAdvisor(agentMemoryService);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentMemoryManager agentMemoryManager(UserProfileStore userProfileStore, LongTermMemoryStore longTermMemoryStore,
            ObjectProvider<LongTermMemoryIndex> longTermMemoryIndexes) {
        return new AgentMemoryManager(userProfileStore, longTermMemoryStore,
                selectLongTermMemoryIndex(longTermMemoryStore, longTermMemoryIndexes));
    }

    private static ChatModel selectModel(ObjectProvider<ChatModel> chatModels, BeanFactory beanFactory,
            String modelBeanName) {
        if (modelBeanName != null && !modelBeanName.isBlank()) {
            return beanFactory.getBean(modelBeanName, ChatModel.class);
        }
        return chatModels.getObject();
    }

    private static LongTermMemoryIndex selectLongTermMemoryIndex(LongTermMemoryStore longTermMemoryStore,
            ObjectProvider<LongTermMemoryIndex> longTermMemoryIndexes) {
        if (longTermMemoryStore instanceof LongTermMemoryIndex index) {
            return index;
        }
        return longTermMemoryIndexes.orderedStream()
                .filter(index -> !(index instanceof UnavailableLongTermMemoryIndex))
                .findFirst()
                .orElseGet(() -> longTermMemoryIndexes.getIfAvailable(UnavailableLongTermMemoryIndex::new));
    }
}

