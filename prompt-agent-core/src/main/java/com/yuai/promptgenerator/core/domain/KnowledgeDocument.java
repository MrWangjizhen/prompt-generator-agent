package com.yuai.promptgenerator.core.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("knowledge_documents")
public class KnowledgeDocument {
    @Id
    private String id;
    private String filename;
    private String contentType;
    private long size;
    @Indexed(unique = true)
    private String checksum;
    private String ossKey;
    private KnowledgeStatus status = KnowledgeStatus.PENDING;
    private int version = 1;
    private String error;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public String getOssKey() { return ossKey; }
    public void setOssKey(String ossKey) { this.ossKey = ossKey; }
    public KnowledgeStatus getStatus() { return status; }
    public void setStatus(KnowledgeStatus status) { this.status = status; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
