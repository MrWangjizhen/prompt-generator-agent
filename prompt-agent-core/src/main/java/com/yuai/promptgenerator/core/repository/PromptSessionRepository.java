package com.yuai.promptgenerator.core.repository;

import java.util.List;
import java.util.Optional;

import com.yuai.promptgenerator.core.domain.PromptSession;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PromptSessionRepository extends MongoRepository<PromptSession, String> {
    Optional<PromptSession> findByIdAndUserId(String id, String userId);
    List<PromptSession> findTop50ByUserIdOrderByUpdatedAtDesc(String userId);
}
