package com.yuai.promptgenerator.worker.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

import com.yuai.promptgenerator.core.domain.KnowledgeJob;
import com.yuai.promptgenerator.core.domain.KnowledgeStatus;
import com.yuai.promptgenerator.core.repository.KnowledgeDocumentRepository;
import com.yuai.promptgenerator.core.repository.KnowledgeJobRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RagIndexService {
    private final KnowledgeJobRepository jobs;
    private final KnowledgeDocumentRepository documents;
    private final WorkerObjectStorage storage;
    private final VectorStore vectorStore;
    private final KnowledgeChunker chunker = new KnowledgeChunker();

    public RagIndexService(KnowledgeJobRepository jobs, KnowledgeDocumentRepository documents,
            WorkerObjectStorage storage, @Qualifier("knowledgeVectorStore") VectorStore vectorStore) {
        this.jobs = jobs;
        this.documents = documents;
        this.storage = storage;
        this.vectorStore = vectorStore;
    }

    public KnowledgeJob process(String jobId) {
        KnowledgeJob job = jobs.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在"));
        if (job.getStatus() == KnowledgeStatus.SUCCEEDED) return job;
        var document = documents.findById(job.getDocumentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文档不存在"));
        job.setStatus(KnowledgeStatus.RUNNING);
        job.setAttempts(job.getAttempts() + 1);
        job.setUpdatedAt(Instant.now());
        jobs.save(job);
        document.setStatus(KnowledgeStatus.RUNNING);
        document.setUpdatedAt(Instant.now());
        documents.save(document);
        try (InputStream input = storage.get(document.getOssKey())) {
            byte[] bytes = input.readAllBytes();
            String text = extract(document.getFilename(), bytes);
            var chunks = chunker.split(document, text);
            if (chunks.isEmpty()) throw new IllegalArgumentException("文档没有可索引文本");
            vectorStore.delete(new FilterExpressionBuilder().eq("documentId", document.getId()).build());
            vectorStore.add(chunks);
            job.setStatus(KnowledgeStatus.SUCCEEDED);
            job.setError(null);
            document.setStatus(KnowledgeStatus.SUCCEEDED);
            document.setError(null);
        }
        catch (Exception exception) {
            job.setStatus(KnowledgeStatus.FAILED);
            job.setError(limit(exception.getMessage()));
            document.setStatus(KnowledgeStatus.FAILED);
            document.setError(limit(exception.getMessage()));
        }
        job.setUpdatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        documents.save(document);
        return jobs.save(job);
    }

    private String extract(String filename, byte[] bytes) throws IOException {
        if (filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            try (var pdf = Loader.loadPDF(bytes)) { return new PDFTextStripper().getText(pdf); }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String limit(String value) {
        String message = value == null ? "Unknown indexing error" : value;
        return message.substring(0, Math.min(message.length(), 1000));
    }
}
