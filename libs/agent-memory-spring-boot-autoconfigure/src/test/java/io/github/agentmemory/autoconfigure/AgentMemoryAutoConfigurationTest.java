package io.github.agentmemory.autoconfigure;

import java.util.List;

import io.github.agentmemory.AgentMemoryAdvisor;
import io.github.agentmemory.AgentMemoryManager;
import io.github.agentmemory.AgentMemoryService;
import io.github.agentmemory.ExtractedLongTermMemory;
import io.github.agentmemory.InMemoryLongTermMemoryStore;
import io.github.agentmemory.InMemoryUserProfileStore;
import io.github.agentmemory.LongTermMemory;
import io.github.agentmemory.LongTermMemoryIndex;
import io.github.agentmemory.LongTermMemoryStore;
import io.github.agentmemory.MemoryDecision;
import io.github.agentmemory.MemoryExtractor;
import io.github.agentmemory.MemoryRouter;
import io.github.agentmemory.ProfileUpdate;
import io.github.agentmemory.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AgentMemoryAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgentMemoryAutoConfiguration.class));

    @Test
    void shouldBackOffWhenMemoryIsDisabled() {
        contextRunner
                .withPropertyValues("agent.memory.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AgentMemoryManager.class));
    }

    @Test
    void shouldCreateManageableStoresWithoutChatModel() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(InMemoryUserProfileStore.class);
            assertThat(context).hasSingleBean(InMemoryLongTermMemoryStore.class);
            assertThat(context).hasSingleBean(LongTermMemoryIndex.class);
            assertThat(context).hasSingleBean(AgentMemoryManager.class);
            assertThat(context).doesNotHaveBean(AgentMemoryService.class);
            assertThat(context).doesNotHaveBean(AgentMemoryAdvisor.class);
        });
    }

    @Test
    void shouldCreateManagerForCustomStoreWithoutIndex() {
        contextRunner
                .withBean(LongTermMemoryStore.class, StubLongTermMemoryStore::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentMemoryManager.class);
                    assertThat(context).doesNotHaveBean(LongTermMemoryIndex.class);
                });
    }

    @Test
    void shouldCreateAdvisorWhenDecisionBeansAreProvided() {
        contextRunner
                .withBean(MemoryRouter.class, () -> (userMessage, profile) -> MemoryDecision.none())
                .withBean(MemoryExtractor.class, StubMemoryExtractor::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentMemoryService.class);
                    assertThat(context).hasSingleBean(AgentMemoryAdvisor.class);
                });
    }

    /**
     * 测试用抽取器，避免自动装配测试依赖真实大模型。
     */
    static class StubMemoryExtractor implements MemoryExtractor {

        @Override
        public ProfileUpdate extractProfileUpdate(String userMessage, String assistantMessage, UserProfile currentProfile) {
            return ProfileUpdate.empty();
        }

        @Override
        public ExtractedLongTermMemory extractLongTermMemory(String userMessage, String assistantMessage,
                MemoryDecision decision) {
            return new ExtractedLongTermMemory("", "general", 1, List.of());
        }
    }

    static class StubLongTermMemoryStore implements LongTermMemoryStore {

        @Override
        public List<LongTermMemory> search(String userId, String query, int topK, double similarityThreshold) {
            return List.of();
        }

        @Override
        public List<LongTermMemory> listByUserId(String userId) {
            return List.of();
        }

        @Override
        public void save(LongTermMemory memory) {
        }

        @Override
        public void deleteByUserId(String userId) {
        }
    }
}

