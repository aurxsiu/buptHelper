package com.aurxsiu.bupthelper.auth.support;

import com.aurxsiu.bupthelper.common.ApiCode;
import com.aurxsiu.bupthelper.common.BusinessException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败计数（单机内存实现）：同一用户名连续失败 5 次锁定 10 分钟。
 */
@Component
public class LoginAttemptTracker {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /**
     * 账号处于锁定期时抛出 42901。
     */
    public void checkNotLocked(String username) {
        if (username == null) {
            return;
        }
        Attempt attempt = attempts.get(username);
        if (attempt == null) {
            return;
        }
        long remainingMinutes = attempt.remainingLockMinutes();
        if (remainingMinutes > 0) {
            throw new BusinessException(ApiCode.LOGIN_LOCKED,
                    "登录失败次数过多，请 " + remainingMinutes + " 分钟后重试");
        }
        if (attempt.isLockExpired()) {
            attempts.remove(username, attempt);
        }
    }

    /**
     * 记录一次失败，返回是否已因此次失败而锁定。
     */
    public boolean recordFailure(String username) {
        if (username == null) {
            return false;
        }
        Instant now = Instant.now();
        Attempt attempt = attempts.compute(username, (key, current) -> {
            Attempt value = current == null ? new Attempt() : current;
            value.failures++;
            if (value.failures >= MAX_FAILURES) {
                value.failures = 0;
                value.lockUntil = now.plus(LOCK_DURATION);
            }
            return value;
        });
        return attempt.isLocked(now);
    }

    public void reset(String username) {
        if (username != null) {
            attempts.remove(username);
        }
    }

    private static final class Attempt {

        private int failures;
        private Instant lockUntil;

        boolean isLocked(Instant now) {
            return lockUntil != null && lockUntil.isAfter(now);
        }

        boolean isLockExpired() {
            return lockUntil != null && !lockUntil.isAfter(Instant.now());
        }

        long remainingLockMinutes() {
            if (lockUntil == null) {
                return 0;
            }
            Duration remaining = Duration.between(Instant.now(), lockUntil);
            if (remaining.isNegative() || remaining.isZero()) {
                return 0;
            }
            return Math.max(1, (remaining.getSeconds() + 59) / 60);
        }
    }
}
