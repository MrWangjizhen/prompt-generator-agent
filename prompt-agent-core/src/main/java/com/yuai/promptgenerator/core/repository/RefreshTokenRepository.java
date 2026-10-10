package com.yuai.promptgenerator.core.repository;

import java.util.Optional;

import com.yuai.promptgenerator.core.domain.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);
    void deleteByUserId(String userId);
}
