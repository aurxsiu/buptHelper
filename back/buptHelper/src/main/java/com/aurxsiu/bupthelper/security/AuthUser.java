package com.aurxsiu.bupthelper.security;

import java.time.Instant;

/**
 * 登录态上下文：由 {@link AuthInterceptor} 解析令牌后放入请求属性。
 */
public record AuthUser(Long userId, String username, String userType, String tokenId, Instant tokenExpiresAt) {
}
