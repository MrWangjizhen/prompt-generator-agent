package io.github.agentmemory;

import java.util.Optional;

/**
 * 用户画像存储接口，starter 通过不同实现适配内存、MongoDB 或其他非关系型数据库。
 */
public interface UserProfileStore {

    Optional<UserProfile> findByUserId(String userId);

    UserProfile save(UserProfile profile);

    void deleteByUserId(String userId);

    void setDisabled(String userId, boolean disabled);

    default boolean isDisabled(String userId) {
        return findByUserId(userId).map(UserProfile::isDisabled).orElse(false);
    }

    default UserProfile getOrCreate(String userId, int schemaVersion) {
        return findByUserId(userId).orElseGet(() -> save(new UserProfile(userId, schemaVersion)));
    }
}

