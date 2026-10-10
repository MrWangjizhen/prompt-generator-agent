package com.yuai.promptgenerator.server.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import com.yuai.promptgenerator.server.api.PromptDtos.ConfirmRequest;
import com.yuai.promptgenerator.server.api.PromptDtos.CreateSessionRequest;
import com.yuai.promptgenerator.server.api.PromptDtos.SelectCandidateRequest;
import com.yuai.promptgenerator.server.api.PromptDtos.SessionDetailView;
import com.yuai.promptgenerator.server.api.PromptDtos.SessionSummaryView;
import com.yuai.promptgenerator.server.api.PromptDtos.TurnRequest;
import com.yuai.promptgenerator.server.security.CurrentUser;
import com.yuai.promptgenerator.server.service.PromptModelGateway;
import com.yuai.promptgenerator.server.service.PromptSessionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1/prompt-sessions")
public class PromptSessionController {
    private final PromptSessionService service;
    private final Executor executor;

    public PromptSessionController(PromptSessionService service, @Qualifier("sseExecutor") Executor executor) {
        this.service = service;
        this.executor = executor;
    }

    @PostMapping
    SessionDetailView create(@Valid @RequestBody CreateSessionRequest request, Authentication authentication) {
        return service.create(CurrentUser.require(authentication).id(), request.taskType(),
                Boolean.TRUE.equals(request.memoryConsent()));
    }

    @GetMapping
    List<SessionSummaryView> list(Authentication authentication) {
        return service.list(CurrentUser.require(authentication).id());
    }

    @GetMapping("/{id}")
    SessionDetailView get(@PathVariable String id, Authentication authentication) {
        return service.get(CurrentUser.require(authentication).id(), id);
    }

    @PostMapping(value = "/{id}/turns/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter turn(@PathVariable String id, @Valid @RequestBody TurnRequest request, Authentication authentication) {
        String userId = CurrentUser.require(authentication).id();
        return emit(emitter -> {
            var outcome = service.turn(userId, id, request.message(), request.expectedVersion());
            send(emitter, "snapshot", outcome.snapshot());
            send(emitter, outcome.snapshot().awaitingConfirmation() ? "confirmation_required" : "question",
                    Map.of("message", outcome.assistantMessage()));
            send(emitter, "knowledge_sources", outcome.knowledgeSources());
            send(emitter, "memory_committed", Map.of("committed", outcome.memoryCommitted()));
            send(emitter, "done", outcome.session());
        });
    }

    @PostMapping(value = "/{id}/confirm/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter confirm(@PathVariable String id, @Valid @RequestBody ConfirmRequest request,
            Authentication authentication) {
        String userId = CurrentUser.require(authentication).id();
        return emit(emitter -> {
            send(emitter, "generation_started", Map.of("sessionId", id));
            var outcome = service.confirm(userId, id, request.expectedVersion(), new PromptModelGateway.GenerationListener() {
                public void onDelta(PromptModelGateway.GenerationDelta delta) {
                    sendUnchecked(emitter, "generation_delta", delta);
                }
                public void onCandidate(com.yuai.promptgenerator.core.domain.PromptCandidate candidate) {
                    sendUnchecked(emitter, "candidate", candidate);
                }
            });
            send(emitter, "candidates", outcome.session().candidates());
            send(emitter, "knowledge_sources", outcome.knowledgeSources());
            send(emitter, "memory_committed", Map.of("committed", outcome.memoryCommitted()));
            send(emitter, "done", outcome.session());
        });
    }

    @PostMapping("/{id}/selection")
    SessionDetailView select(@PathVariable String id, @Valid @RequestBody SelectCandidateRequest request,
            Authentication authentication) {
        return service.select(CurrentUser.require(authentication).id(), id, request.candidateId(),
                request.expectedVersion());
    }

    @GetMapping("/{id}/export")
    ResponseEntity<byte[]> export(@PathVariable String id, @RequestParam(defaultValue = "markdown") String format,
            Authentication authentication) {
        var exported = service.export(CurrentUser.require(authentication).id(), id, format);
        String extension = "json".equalsIgnoreCase(format) ? "json" : "md";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("prompt." + extension).build().toString())
                .contentType(MediaType.parseMediaType(exported.contentType()))
                .body(exported.content().getBytes(StandardCharsets.UTF_8));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
        service.delete(CurrentUser.require(authentication).id(), id);
        return ResponseEntity.noContent().build();
    }

    private SseEmitter emit(SseWork work) {
        SseEmitter emitter = new SseEmitter(180_000L);
        executor.execute(() -> {
            try { work.run(emitter); emitter.complete(); }
            catch (Exception exception) {
                try { send(emitter, "error", Map.of("message", safeMessage(exception))); emitter.complete(); }
                catch (IOException ignored) { emitter.completeWithError(exception); }
            }
        });
        return emitter;
    }

    private void send(SseEmitter emitter, String name, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
    }

    private void sendUnchecked(SseEmitter emitter, String name, Object data) {
        try { send(emitter, name, data); }
        catch (IOException exception) { throw new IllegalStateException("SSE client disconnected", exception); }
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? "请求处理失败" : exception.getMessage();
    }

    @FunctionalInterface
    private interface SseWork { void run(SseEmitter emitter) throws Exception; }
}
