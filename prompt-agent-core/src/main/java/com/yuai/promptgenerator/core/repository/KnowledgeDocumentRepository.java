package com.yuai.promptgenerator.core.repository;

import java.util.Optional;

import com.yuai.promptgenerator.core.domain.KnowledgeDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface KnowledgeDocumentRepository extends MongoRepository<KnowledgeDocument, String> {
    Optional<KnowledgeDocument> findByChecksum(String checksum);
}
