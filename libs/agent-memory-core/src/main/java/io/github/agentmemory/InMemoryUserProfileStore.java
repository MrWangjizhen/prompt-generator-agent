package io.github.agentmemory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存用户画像存储，适合单元测试和无外部依赖的本地 demo。
 */
public class InMemoryUserProfileStore implements UserProfileStore {

    private final ConcurrentMap<String, UserProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public Optional<UserProfile> findByUserId(String userId) {
        return Optional.ofNullable(profiles.get(userId));
    }

    @Override
    public UserProfile save(UserProfile profile) {
        profiles.put(profile.getUserId(), profile);
        return profile;
    }

    @Override
    public void deleteByUserId(String userId) {
        profiles.remove(userId);
    }

    @Override
    public void setDisabled(String userId, boolean disabled) {
        UserProfile profile = profiles.computeIfAbsent(userId, key -> new UserProfile(key, 1));
        profile.setDisabled(disabled);
        save(profile);
    }
}

