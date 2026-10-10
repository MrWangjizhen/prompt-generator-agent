package com.yuai.promptgenerator.server.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.yuai.promptgenerator.core.domain.KnowledgeDocument;
import com.yuai.promptgenerator.core.domain.KnowledgeJob;
import com.yuai.promptgenerator.core.domain.KnowledgeStatus;
import com.yuai.promptgenerator.core.repository.KnowledgeDocumentRepository;
import com.yuai.promptgenerator.core.repository.KnowledgeJobRepository;
import com.yuai.promptgenerator.server.config.AppProperties;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KnowledgeAdminService {
    private static final Set<String> EXTENSIONS = Set.of("md", "markdown", "txt", "pdf");
    private final KnowledgeDocumentRepository documents;
    private final KnowledgeJobRepository jobs;
    private final ObjectStorage storage;
    private final RagWorkerInvoker worker;
    private final VectorStore vectorStore;
    private final AppProperties.Rag properties;

    public KnowledgeAdminService(KnowledgeDocumentRepository documents, KnowledgeJobRepository jobs,
            ObjectStorage storage, RagWorkerInvoker worker,
            @Qualifier("knowledgeVectorStore") VectorStore vectorStore, AppProperties properties) {
        this.documents = documents;
        this.jobs = jobs;
        this.storage = storage;
        this.worker = worker;
        this.vectorStore = vectorStore;
        this.properties = properties.getRag();
    }

    public UploadResult upload(MultipartFile file) {
        if (!storage.available()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OSS 尚未配置");
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件为空");
        if (file.getSize() > properties.getMaxUploadBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "文件超过 20 MB 限制");
        }
        String filename = sanitize(file.getOriginalFilename());
        String extension = extension(filename);
        if (!EXTENSIONS.contains(extension)) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "仅支持 Markdown、TXT 和 PDF");
        try {
            byte[] bytes = file.getBytes();
            validateMagic(extension, bytes);
            String checksum = sha256(bytes);
            var duplicate = documents.findByChecksum(checksum);
            if (duplicate.isPresent() && duplicate.get().getStatus() != KnowledgeStatus.DELETED) {
                return new UploadResult(duplicate.get(), null, true, false);
            }
            KnowledgeDocument document = new KnowledgeDocument();
            document.setFilename(filename);
            document.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            document.setSize(bytes.length);
            document.setChecksum(checksum);
            document.setOssKey("knowledge/" + UUID.randomUUID() + "/" + filename);
            storage.put(document.getOssKey(), new ByteArrayInputStream(bytes), bytes.length, document.getContentType());
            document = documents.save(document);
            KnowledgeJob job = new KnowledgeJob();
            job.setDocumentId(document.getId());
            job = jobs.save(job);
            boolean triggered = worker.invoke(job.getId());
            return new UploadResult(document, job, false, triggered);
        }
        catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法读取上传文件", exception);
        }
    }

    public List<KnowledgeDocument> list() { return documents.findAll(); }

    public KnowledgeDocument get(String id) {
        return documents.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文档不存在"));
    }

    public KnowledgeJob retry(String id) {
        KnowledgeDocument document = get(id);
        if (document.getStatus() == KnowledgeStatus.DELETED) throw new ResponseStatusException(HttpStatus.CONFLICT, "文档已删除");
        document.setStatus(KnowledgeStatus.PENDING);
        document.setError(null);
        document.setUpdatedAt(Instant.now());
        documents.save(document);
        KnowledgeJob job = new KnowledgeJob();
        job.setDocumentId(id);
        job = jobs.save(job);
        worker.invoke(job.getId());
        return job;
    }

    public KnowledgeJob job(String id) {
        return jobs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在"));
    }

    public void delete(String id) {
        KnowledgeDocument document = get(id);
        vectorStore.delete(new FilterExpressionBuilder().eq("documentId", id).build());
        if (storage.available()) storage.delete(document.getOssKey());
        document.setStatus(KnowledgeStatus.DELETED);
        document.setUpdatedAt(Instant.now());
        documents.save(document);
    }

    private String sanitize(String value) {
        String filename = value == null ? "document.txt" : value.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._\\-\u4e00-\u9fa5]", "_");
        if (filename.isBlank()) return "document.txt";
        return filename.substring(0, Math.min(filename.length(), 160));
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void validateMagic(String extension, byte[] bytes) {
        if ("pdf".equals(extension)) {
            String header = new String(bytes, 0, Math.min(bytes.length, 5), StandardCharsets.US_ASCII);
            if (!"%PDF-".equals(header)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF 文件头无效");
        }
    }

    private String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public record UploadResult(KnowledgeDocument document, KnowledgeJob job, boolean duplicate, boolean triggered) { }
}
