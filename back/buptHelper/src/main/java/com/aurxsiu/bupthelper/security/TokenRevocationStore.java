package com.aurxsiu.bupthelper.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌失效登记（单机内存实现）：
 * 1. 注销时登记该令牌的 jti；
 * 2. 修改密码时递增该用户的令牌版本号，使此前签发的令牌全部作废。
 */
@Component
public class TokenRevocationStore {

    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();
    private final Map<Long, Long> tokenVersions = new ConcurrentHashMap<>();

    public void revokeToken(String tokenId, Instant expiresAt) {
        removeExpiredTokens();
        if (tokenId != null && expiresAt != null) {
            revokedTokens.put(tokenId, expiresAt);
        }
    }

    public boolean isTokenRevoked(String tokenId) {
        removeExpiredTokens();
        return tokenId != null && revokedTokens.containsKey(tokenId);
    }

    public long currentVersion(Long userId) {
        Long version = userId == null ? null : tokenVersions.get(userId);
        return version == null ? 0L : version;
    }

    /**
     * 递增用户令牌版本，令此前签发的令牌全部失效。
     */
    public void bumpVersion(Long userId) {
        if (userId != null) {
            tokenVersions.merge(userId, 1L, Long::sum);
        }
    }

    private void removeExpiredTokens() {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
