package com.yuai.promptgenerator.worker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yuai.promptgenerator.core.domain.KnowledgeJob;
import com.yuai.promptgenerator.worker.config.WorkerProperties;
import com.yuai.promptgenerator.worker.service.RagIndexService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RagWorkerControllerTest {

    private final RagIndexService service = mock(RagIndexService.class);
    private final WorkerProperties properties = new WorkerProperties();
    private final RagWorkerController controller = new RagWorkerController(service, properties);

    @Test
    void rejectsMissingOrWrongTokenAsUnauthorized() {
        assertUnauthorized(null);
        assertUnauthorized("wrong-token");
    }

    @Test
    void acceptsConfiguredToken() {
        KnowledgeJob job = new KnowledgeJob();
        when(service.process("job-1")).thenReturn(job);

        assertThat(controller.process("job-1", "dev-worker-token")).isSameAs(job);
        verify(service).process("job-1");
    }

    private void assertUnauthorized(String token) {
        assertThatThrownBy(() -> controller.process("job-1", token))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
