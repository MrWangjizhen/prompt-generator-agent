package com.yuai.promptgenerator.core.repository;

import java.util.Optional;

import com.yuai.promptgenerator.core.domain.AppUser;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppUserRepository extends MongoRepository<AppUser, String> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
