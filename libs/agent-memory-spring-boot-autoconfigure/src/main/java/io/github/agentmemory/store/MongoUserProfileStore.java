package io.github.agentmemory.store;

import java.util.Optional;

import io.github.agentmemory.UserProfile;
import io.github.agentmemory.UserProfileStore;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * 基于 MongoTemplate 的用户画像存储适配器，避免 core 模块直接依赖 MongoDB。
 */
public class MongoUserProfileStore implements UserProfileStore {

    private static final String COLLECTION = "agent_memory_user_profiles";

    private final MongoOperations mongoOperations;

    public MongoUserProfileStore(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public Optional<UserProfile> findByUserId(String userId) {
        return Optional.ofNullable(mongoOperations.findOne(Query.query(Criteria.where("userId").is(userId)),
                UserProfile.class, COLLECTION));
    }

    @Override
    public UserProfile save(UserProfile profile) {
        return mongoOperations.save(profile, COLLECTION);
    }

    @Override
    public void deleteByUserId(String userId) {
        mongoOperations.remove(Query.query(Criteria.where("userId").is(userId)), COLLECTION);
    }

    @Override
    public void setDisabled(String userId, boolean disabled) {
        UserProfile profile = findByUserId(userId).orElseGet(() -> new UserProfile(userId, 1));
        profile.setDisabled(disabled);
        save(profile);
    }
}

