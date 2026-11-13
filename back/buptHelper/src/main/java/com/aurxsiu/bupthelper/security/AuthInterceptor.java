package com.aurxsiu.bupthelper.security;

import com.aurxsiu.bupthelper.common.ApiCode;
import com.aurxsiu.bupthelper.common.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

/**
 * 校验 Authorization 请求头中的 JWT，并把登录用户放入请求属性。
 * 需要登录的接口通过 {@code @RequestAttribute(AuthInterceptor.ATTR_USER)} 获取当前用户。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER = "bupthelper.authUser";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TokenRevocationStore revocationStore;

    public AuthInterceptor(JwtTokenProvider tokenProvider, TokenRevocationStore revocationStore) {
        this.tokenProvider = tokenProvider;
        this.revocationStore = revocationStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "请先登录");
        }

        Claims claims = tokenProvider.parse(token);
        Long userId = parseUserId(claims.getSubject());
        Instant expiresAt = claims.getExpiration() == null ? null : claims.getExpiration().toInstant();
        String tokenId = claims.getId();

        if (revocationStore.isTokenRevoked(tokenId)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (parseTokenVersion(claims) != revocationStore.currentVersion(userId)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }

        request.setAttribute(ATTR_USER, new AuthUser(userId, claims.get("username", String.class),
                claims.get("type", String.class), tokenId, expiresAt));
        return true;
    }

    private Long parseUserId(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "请先登录");
        }
    }

    private long parseTokenVersion(Claims claims) {
        Object version = claims.get("ver");
        return version instanceof Number number ? number.longValue() : 0L;
    }

    private String resolveToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        String value = authorization.trim();
        if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String token = value.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return value;
    }
}
