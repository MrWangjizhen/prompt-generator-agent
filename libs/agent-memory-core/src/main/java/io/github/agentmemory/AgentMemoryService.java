package io.github.agentmemory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 记忆总编排服务：读取画像、路由、检索、构建上下文，并在回复后异步保存记忆。
 */
public class AgentMemoryService {

    private static final Logger log = LoggerFactory.getLogger(AgentMemoryService.class);

    private final UserProfileStore userProfileStore;

    private final LongTermMemoryStore longTermMemoryStore;

    private final LongTermMemoryIndex longTermMemoryIndex;

    private final MemoryRouter memoryRouter;

    private final MemoryExtractor memoryExtractor;

    private final MemoryContextBuilder memoryContextBuilder;

    private final MemoryPrivacyFilter privacyFilter;

    private final AgentMemoryProperties properties;

    private final Executor executor;

    private final MeterRegistry meterRegistry;

    public AgentMemoryService(UserProfileStore userProfileStore, LongTermMemoryStore longTermMemoryStore,
            MemoryRouter memoryRouter, MemoryExtractor memoryExtractor, MemoryContextBuilder memoryContextBuilder,
            MemoryPrivacyFilter privacyFilter, AgentMemoryProperties properties, Executor executor,
            MeterRegistry meterRegistry) {
        this(userProfileStore, longTermMemoryStore, new UnavailableLongTermMemoryIndex(), memoryRouter, memoryExtractor,
                memoryContextBuilder, privacyFilter, properties, executor, meterRegistry);
    }

    public AgentMemoryService(UserProfileStore userProfileStore, LongTermMemoryStore longTermMemoryStore,
            LongTermMemoryIndex longTermMemoryIndex, MemoryRouter memoryRouter, MemoryExtractor memoryExtractor,
            MemoryContextBuilder memoryContextBuilder, MemoryPrivacyFilter privacyFilter,
            AgentMemoryProperties properties, Executor executor, MeterRegistry meterRegistry) {
        this.userProfileStore = userProfileStore;
        this.longTermMemoryStore = longTermMemoryStore;
        this.longTermMemoryIndex = longTermMemoryIndex;
        this.memoryRouter = memoryRouter;
        this.memoryExtractor = memoryExtractor;
        this.memoryContextBuilder = memoryContextBuilder;
        this.privacyFilter = privacyFilter;
        this.properties = properties;
        this.executor = executor;
        this.meterRegistry = meterRegistry;
    }

    public AgentMemoryContext prepare(String userId, String chatId, String userMessage, boolean consentGranted) {
        if (!properties.isEnabled() || userProfileStore.isDisabled(userId)
                || (properties.getPrivacy().isRequireConsent() && !consentGranted)) {
            count("agent.memory.skipped");
            return new AgentMemoryContext(userId, chatId, userMessage, consentGranted, UserProfile.empty(userId),
                    MemoryDecision.none(), List.of(), "");
        }
        UserProfile userProfile = userProfileStore.getOrCreate(userId, properties.getSchema().getVersion());
        MemoryDecision decision = memoryRouter.decide(userMessage, userProfile).normalized();
        List<LongTermMemory> memories = decision.retrieveLongTermMemory()
                ? longTermMemoryStore.search(userId, userMessage, properties.getLongTerm().getTopK(),
                        properties.getLongTerm().getSimilarityThreshold())
                : List.of();
        count("agent.memory.prepare");
        return new AgentMemoryContext(userId, chatId, userMessage, consentGranted, userProfile, decision, memories,
                memoryContextBuilder.build(userProfile, memories));
    }

    public void rememberAfterResponse(AgentMemoryContext context, String userMessage, String assistantResponse) {
        if (context == null || context.decision() == null || !context.consentGranted()) {
            return;
        }
        if (properties.getCommitMode() == AgentMemoryProperties.CommitMode.SYNC) {
            rememberSynchronously(context, userMessage, assistantResponse);
            return;
        }
        CompletableFuture.runAsync(() -> rememberSynchronously(context, userMessage, assistantResponse), executor);
    }

    /**
     * Completes extraction and persistence in the caller thread. Serverless applications should use this mode before
     * the request-complete event so the platform cannot freeze the executor before the write finishes.
     */
    public void rememberSynchronously(AgentMemoryContext context, String userMessage, String assistantResponse) {
        if (context == null || context.decision() == null || !context.consentGranted()) {
            return;
        }
        try {
            String filteredUserMessage = privacyFilter.filter(userMessage);
            String filteredAssistantResponse = privacyFilter.filter(assistantResponse);
            if (context.decision().updateUserProfile()) {
                ProfileUpdate update = memoryExtractor.extractProfileUpdate(filteredUserMessage,
                        filteredAssistantResponse, context.userProfile());
                userProfileStore.save(ProfileMerger.merge(context.userProfile(), update, properties.getSchema().getVersion()));
                count("agent.memory.profile.updated");
            }
            if (context.decision().saveLongTermMemory()) {
                ExtractedLongTermMemory extracted = memoryExtractor.extractLongTermMemory(filteredUserMessage,
                        filteredAssistantResponse, context.decision());
                if (extracted.hasContent()) {
                    LongTermMemory memory = LongTermMemory.from(context.userId(), context.chatId(), extracted,
                            properties.getSchema().getVersion());
                    longTermMemoryStore.save(memory);
                    if (longTermMemoryIndex != longTermMemoryStore) {
                        longTermMemoryIndex.save(memory);
                    }
                    count("agent.memory.long-term.saved");
                }
            }
        }
        catch (RuntimeException exception) {
            count("agent.memory.error");
            log.warn("Memory update failed for userId={}, chatId={}", context.userId(), context.chatId(), exception);
            if (properties.getCommitMode() == AgentMemoryProperties.CommitMode.SYNC) {
                throw exception;
            }
        }
    }

    private void count(String metricName) {
        if (properties.getObservation().isEnabled()) {
            meterRegistry.counter(metricName).increment();
        }
    }
}

