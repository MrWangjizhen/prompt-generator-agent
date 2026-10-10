package com.yuai.promptgenerator.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuai.promptgenerator.core.domain.PromptSession;
import com.yuai.promptgenerator.core.domain.SessionStatus;
import com.yuai.promptgenerator.core.domain.TaskType;
import com.yuai.promptgenerator.core.repository.PromptSessionRepository;
import io.github.agentmemory.AgentMemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PromptSessionServiceTest {
    @Mock PromptSessionRepository sessions;
    @Mock PromptModelGateway model;
    @Mock KnowledgeRetrievalService knowledge;
    @Mock AgentMemoryService memory;
    private PromptSessionService service;

    @BeforeEach
    void setUp() {
        service = new PromptSessionService(sessions, model, knowledge, memory, new ObjectMapper());
    }

    @Test
    void createsSessionWithPersistedInitialQuestion() {
        when(sessions.save(any(PromptSession.class))).thenAnswer(invocation -> {
            PromptSession value = invocation.getArgument(0);
            if (value.getId() == null) value.setId("session-1");
            if (value.getVersion() == null) value.setVersion(0L);
            else value.setVersion(value.getVersion() + 1);
            return value;
        });
        var result = service.create("user-1", TaskType.TEXT, true);

        assertThat(result.id()).isEqualTo("session-1");
        assertThat(result.memoryConsent()).isTrue();
        assertThat(result.turns()).singleElement().satisfies(turn -> {
            assertThat(turn.role()).isEqualTo("assistant");
            assertThat(turn.content()).contains("最终希望模型完成什么任务");
        });
    }

    @Test
    void rejectsChangesWhileGenerationIsRunning() {
        PromptSession session = new PromptSession("user-1", TaskType.CODE, false);
        session.setId("session-1");
        session.setVersion(3L);
        session.setStatus(SessionStatus.GENERATING);
        when(sessions.findByIdAndUserId("session-1", "user-1")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.turn("user-1", "session-1", "继续修改", 3L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verifyNoInteractions(model, knowledge, memory);
    }
}
