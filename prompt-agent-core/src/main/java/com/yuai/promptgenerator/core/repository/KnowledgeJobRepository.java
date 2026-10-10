package com.yuai.promptgenerator.core.repository;

import java.util.List;

import com.yuai.promptgenerator.core.domain.KnowledgeJob;
import com.yuai.promptgenerator.core.domain.KnowledgeStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface KnowledgeJobRepository extends MongoRepository<KnowledgeJob, String> {
    List<KnowledgeJob> findTop20ByStatusOrderByCreatedAt(KnowledgeStatus status);
}
