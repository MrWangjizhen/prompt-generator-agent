package com.yuai.promptgenerator.server.service;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuai.promptgenerator.core.domain.ConversationTurn;
import com.yuai.promptgenerator.core.domain.PromptCandidate;
import com.yuai.promptgenerator.core.domain.PromptSession;
import com.yuai.promptgenerator.core.domain.SessionStatus;
import com.yuai.promptgenerator.core.domain.TaskType;
import com.yuai.promptgenerator.core.repository.PromptSessionRepository;
import com.yuai.promptgenerator.core.service.GuidedRequirementEngine;
import com.yuai.promptgenerator.server.api.PromptDtos.RequirementSnapshot;
import com.yuai.promptgenerator.server.api.PromptDtos.SessionDetailView;
import com.yuai.promptgenerator.server.api.PromptDtos.SessionSummaryView;
import io.github.agentmemory.AgentMemoryContext;
import io.github.agentmemory.AgentMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PromptSessionService {
    private static final Logger log = LoggerFactory.getLogger(PromptSessionService.class);
    private final PromptSessionRepository sessions;
    private final PromptModelGateway model;
    private final KnowledgeRetrievalService knowledge;
    private final AgentMemoryService memory;
    private final GuidedRequirementEngine engine = new GuidedRequirementEngine();
    private final ObjectMapper objectMapper;

    public PromptSessionService(PromptSessionRepository sessions, PromptModelGateway model,
            KnowledgeRetrievalService knowledge, AgentMemoryService memory, ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.model = model;
        this.knowledge = knowledge;
        this.memory = memory;
        this.objectMapper = objectMapper;
    }

    public SessionDetailView create(String userId, TaskType taskType, boolean consent) {
        PromptSession session = new PromptSession(userId, taskType, consent);
        session.addTurn(ConversationTurn.assistant("先告诉我：你最终希望模型完成什么任务？"));
        return detail(sessions.save(session));
    }

    public List<SessionSummaryView> list(String userId) {
        return sessions.findTop50ByUserIdOrderByUpdatedAtDesc(userId).stream().map(this::summaryView).toList();
    }

    public SessionDetailView get(String userId, String id) { return detail(requireOwned(userId, id)); }

    public TurnOutcome turn(String userId, String id, String message, Long expectedVersion) {
        PromptSession session = requireOwned(userId, id);
        checkVersion(session, expectedVersion);
        if (session.getStatus() == SessionStatus.GENERATING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "提示词正在生成，请稍候");
        }
        if (session.getStatus() == SessionStatus.COMPLETED) {
            session.setStatus(SessionStatus.DISCOVERING);
            session.setCandidates(List.of());
            session.setSelectedCandidateId(null);
        }
        var retrieval = knowledge.retrieve(message);
        AgentMemoryContext memoryContext = prepareMemory(session, message);
        var extraction = model.extract(session.getTaskType(), session.getRequirements(), message,
                memoryContext == null ? "" : memoryContext.promptContext(), retrieval.context());
        session.getRequirements().merge(extraction.common(), extraction.taskSpecific(), extraction.skipped());
        session.setDiscoveryTurns(session.getDiscoveryTurns() + 1);
        session.addTurn(ConversationTurn.user(message));
        var next = engine.next(session.getRequirements(), session.getTaskType(), session.getDiscoveryTurns());
        String assistant = next.readyForConfirmation() ? summary(session, next.missingFields()) : next.question();
        session.addTurn(ConversationTurn.assistant(assistant));
        session.setStatus(next.readyForConfirmation() ? SessionStatus.AWAITING_CONFIRMATION : SessionStatus.DISCOVERING);
        PromptSession saved = save(session);
        boolean committed = commitMemory(memoryContext, message, assistant);
        return new TurnOutcome(detail(saved), new RequirementSnapshot(saved.getRequirements(), next.missingFields(),
                next.readyForConfirmation(), next.assumptionsRequired(), saved.getVersion()), assistant,
                retrieval.sources(), committed);
    }

    public GenerateOutcome confirm(String userId, String id, Long expectedVersion,
            PromptModelGateway.GenerationListener listener) {
        PromptSession session = requireOwned(userId, id);
        checkVersion(session, expectedVersion);
        if (session.getStatus() != SessionStatus.AWAITING_CONFIRMATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "需求尚未进入确认状态");
        }
        session.setStatus(SessionStatus.GENERATING);
        session = save(session);
        String memoryMessage = "用户已确认需求并要求生成三种提示词";
        AgentMemoryContext memoryContext = prepareMemory(session, memoryMessage);
        var retrieval = knowledge.retrieve(json(session.getRequirements()));
        try {
            List<PromptCandidate> candidates = model.generate(session.getTaskType(), session.getRequirements(),
                    memoryContext == null ? "" : memoryContext.promptContext(), retrieval.context(), listener);
            session.setCandidates(candidates);
            session.setStatus(SessionStatus.COMPLETED);
            session.addTurn(ConversationTurn.user(memoryMessage));
            session.addTurn(ConversationTurn.assistant("已生成简洁版、标准版和严格版提示词。"));
            PromptSession saved = save(session);
            boolean committed = commitMemory(memoryContext, memoryMessage,
                    candidates.stream().map(PromptCandidate::content).reduce((a, b) -> a + "\n\n" + b).orElse(""));
            return new GenerateOutcome(detail(saved), retrieval.sources(), committed);
        }
        catch (RuntimeException exception) {
            session.setStatus(SessionStatus.AWAITING_CONFIRMATION);
            try { save(session); }
            catch (RuntimeException resetFailure) {
                log.warn("Failed to reset session {} after generation error: {}", id, resetFailure.getMessage());
            }
            throw exception;
        }
    }

    public GenerateOutcome confirm(String userId, String id, Long expectedVersion) {
        return confirm(userId, id, expectedVersion, PromptModelGateway.GenerationListener.noop());
    }

    public SessionDetailView select(String userId, String id, String candidateId, Long expectedVersion) {
        PromptSession session = requireOwned(userId, id);
        checkVersion(session, expectedVersion);
        boolean exists = session.getCandidates().stream().anyMatch(value -> value.id().equals(candidateId));
        if (!exists) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "候选提示词不存在");
        session.setSelectedCandidateId(candidateId);
        session.touch();
        return detail(save(session));
    }

    public ExportedPrompt export(String userId, String id, String format) {
        PromptSession session = requireOwned(userId, id);
        PromptCandidate selected = session.getCandidates().stream()
                .filter(value -> value.id().equals(session.getSelectedCandidateId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "请先选择一个候选提示词"));
        if ("json".equalsIgnoreCase(format)) return new ExportedPrompt("application/json", json(selected));
        String markdown = "# " + selected.title() + "\n\n" + selected.content() + "\n\n## 使用取舍\n\n"
                + selected.tradeoff() + "\n\n## 变量\n\n" + selected.variables().entrySet().stream()
                .map(entry -> "- `{{" + entry.getKey() + "}}`: " + entry.getValue())
                .reduce((a, b) -> a + "\n" + b).orElse("无");
        return new ExportedPrompt("text/markdown;charset=UTF-8", markdown);
    }

    public void delete(String userId, String id) { sessions.delete(requireOwned(userId, id)); }

    private AgentMemoryContext prepareMemory(PromptSession session, String message) {
        try { return memory.prepare(session.getUserId(), session.getId(), message, session.isMemoryConsent()); }
        catch (RuntimeException exception) {
            log.warn("Memory preparation skipped for session {}: {}", session.getId(), exception.getMessage());
            return null;
        }
    }

    private boolean commitMemory(AgentMemoryContext context, String user, String assistant) {
        if (context == null || !context.consentGranted()) return false;
        try { memory.rememberSynchronously(context, user, assistant); return true; }
        catch (RuntimeException exception) { log.warn("Memory commit failed: {}", exception.getMessage()); return false; }
    }

    private PromptSession requireOwned(String userId, String id) {
        return sessions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在"));
    }

    private void checkVersion(PromptSession session, Long expected) {
        if (expected != null && !expected.equals(session.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "会话版本冲突，请刷新后重试");
        }
    }

    private PromptSession save(PromptSession session) {
        try { session.touch(); return sessions.save(session); }
        catch (OptimisticLockingFailureException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "会话已被其他请求更新");
        }
    }

    private String summary(PromptSession session, List<String> missing) {
        return "需求信息已足够。请确认以下需求摘要；如需修改可继续发送说明，否则点击“确认并生成”。\n"
                + json(session.getRequirements())
                + (missing.isEmpty() ? "" : "\n尚未明确、将按默认假设处理：" + String.join(", ", missing));
    }

    private String json(Object value) {
        try { return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value); }
        catch (JsonProcessingException e) { return "{}"; }
    }

    private SessionDetailView detail(PromptSession value) {
        return new SessionDetailView(value.getId(), value.getTaskType(), value.getStatus(), value.getRequirements(),
                List.copyOf(value.getTurns()), value.getCandidates(), value.getSelectedCandidateId(), value.isMemoryConsent(),
                value.getDiscoveryTurns(), value.getCreatedAt(), value.getUpdatedAt(), value.getVersion());
    }

    private SessionSummaryView summaryView(PromptSession value) {
        return new SessionSummaryView(value.getId(), value.getTaskType(), value.getStatus(),
                value.getSelectedCandidateId(), value.getCreatedAt(), value.getUpdatedAt(), value.getVersion());
    }

    public record TurnOutcome(SessionDetailView session, RequirementSnapshot snapshot, String assistantMessage,
                              List<KnowledgeRetrievalService.Source> knowledgeSources, boolean memoryCommitted) { }
    public record GenerateOutcome(SessionDetailView session, List<KnowledgeRetrievalService.Source> knowledgeSources,
                                  boolean memoryCommitted) { }
    public record ExportedPrompt(String contentType, String content) { }
}
