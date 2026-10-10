package com.yuai.promptgenerator.worker.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.yuai.promptgenerator.core.domain.KnowledgeJob;
import com.yuai.promptgenerator.worker.config.WorkerProperties;
import com.yuai.promptgenerator.worker.service.RagIndexService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class RagWorkerController {
    private final RagIndexService service;
    private final WorkerProperties properties;
    public RagWorkerController(RagIndexService service, WorkerProperties properties) {
        this.service = service; this.properties = properties;
    }

    @PostMapping("/internal/rag/jobs/{id}")
    KnowledgeJob process(@PathVariable String id,
            @RequestHeader(value = "X-Worker-Token", required = false) String token) {
        byte[] actual = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(actual,
                properties.getWorkerToken().getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid worker token");
        }
        return service.process(id);
    }
}
